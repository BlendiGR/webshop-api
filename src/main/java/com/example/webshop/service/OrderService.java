package com.example.webshop.service;

import com.example.webshop.dto.AddressDto;
import com.example.webshop.dto.OrderDto;
import com.example.webshop.dto.OrderRequest;
import com.example.webshop.dto.StatusChangeDto;
import com.example.webshop.entity.*;
import com.example.webshop.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final ProductRepository products;
    private final AddressRepository addresses;
    private final StatusChangeRepository statusChanges;
    private final CustomerService customerService;

    public OrderService(OrderRepository orders, OrderItemRepository items, ProductRepository products,
                        AddressRepository addresses, StatusChangeRepository statusChanges,
                        CustomerService customerService) {
        this.orders = orders;
        this.items = items;
        this.products = products;
        this.addresses = addresses;
        this.statusChanges = statusChanges;
        this.customerService = customerService;
    }

    // one transaction, if one product is out of stock nothing is saved
    @Transactional
    public OrderDto create(OrderRequest req) {
        if (req.customerId() == null || req.addressId() == null || req.items() == null || req.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customerId, addressId and at least one item are required");
        }
        Customer customer = customerService.get(req.customerId());
        Address address = customerService.getAddress(customer.getId(), req.addressId());

        // treemap merges the same product and keeps ids in order so locks are always taken in the same order
        Map<Integer, Integer> quantities = new TreeMap<>();
        for (OrderRequest.Item item : req.items()) {
            if (item.productId() == null || item.quantity() == null || item.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Every item needs productId and a positive quantity");
            }
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setShippingAddress(address);

        quantities.forEach((productId, qty) -> {
            Product product = products.findById(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + productId + " not found"));
            if (products.reserveStock(productId, qty) == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough stock for product " + productId);
            }
            order.addItem(product, qty, products.effectivePrice(productId));
        });

        return findById(orders.saveAndFlush(order).getId());
    }

    @Transactional(readOnly = true)
    public OrderDto findById(Integer id) {
        CustomerOrder o = get(id);
        List<OrderDto.Line> lines = items.findByOrderId(id).stream()
                .map(i -> new OrderDto.Line(i.getProduct().getId(), i.getProduct().getName(), i.getQuantity(),
                        i.getUnitPrice(), i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();
        BigDecimal total = lines.stream().map(OrderDto.Line::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderDto(o.getId(), o.getCustomer().getId(), o.getOrderDate(), o.getDeliveryDate(), o.getStatus(),
                addressAtOrderTime(o), lines, total);
    }

    @Transactional(readOnly = true)
    public List<StatusChangeDto> statusHistory(Integer id) {
        get(id);
        return statusChanges.findByOrderIdOrderById(id).stream().map(StatusChangeDto::from).toList();
    }

    @Transactional
    public OrderDto pay(Integer id) {
        CustomerOrder o = get(id);
        if (orders.changeStatus(id, OrderStatus.NEW, OrderStatus.PAID) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order can not be paid, status is " + o.getStatus());
        }
        return findById(id);
    }

    // lock the row first so the expire event can not return the same stock twice
    @Transactional
    public OrderDto cancel(Integer id) {
        CustomerOrder o = orders.findForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + id + " not found"));
        OrderStatus status = o.getStatus();
        if (status != OrderStatus.NEW && status != OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order can not be cancelled, status is " + status);
        }
        for (OrderItem item : items.findByOrderId(id)) {
            products.releaseStock(item.getProduct().getId(), item.getQuantity());
        }
        orders.changeStatus(id, status, OrderStatus.CANCELLED);
        return findById(id);
    }

    // shipping address as it was when the order was made, order_date has second precision
    private AddressDto addressAtOrderTime(CustomerOrder o) {
        Address current = o.getShippingAddress();
        if (current == null) {
            return null;
        }
        return addresses.findAsOf(current.getId(), o.getOrderDate().plusSeconds(1))
                .map(row -> AddressDto.from(current.getId(), row))
                .orElse(AddressDto.from(current));
    }

    private CustomerOrder get(Integer id) {
        return orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + id + " not found"));
    }
}
