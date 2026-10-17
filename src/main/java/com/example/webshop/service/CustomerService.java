package com.example.webshop.service;

import com.example.webshop.dto.*;
import com.example.webshop.entity.Address;
import com.example.webshop.entity.Customer;
import com.example.webshop.entity.CustomerProfile;
import com.example.webshop.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customers;
    private final AddressRepository addresses;
    private final CustomerProfileRepository profiles;
    private final OrderRepository orders;
    private final OrderSummaryRepository orderSummaries;

    public CustomerService(CustomerRepository customers, AddressRepository addresses,
                           CustomerProfileRepository profiles, OrderRepository orders,
                           OrderSummaryRepository orderSummaries) {
        this.customers = customers;
        this.addresses = addresses;
        this.profiles = profiles;
        this.orders = orders;
        this.orderSummaries = orderSummaries;
    }

    @Transactional(readOnly = true)
    public CustomerDto findById(Integer id) {
        return toDto(get(id));
    }

    @Transactional(readOnly = true)
    public CustomerDto findByEmail(String email) {
        Customer c = customers.findFirstByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No customer with email " + email));
        return toDto(c);
    }

    @Transactional
    public CustomerDto create(CustomerRequest req) {
        validate(req);
        if (customers.findFirstByEmail(req.email().trim().toLowerCase()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        Customer c = new Customer();
        copy(req, c);
        return toDto(customers.save(c));
    }

    @Transactional
    public CustomerDto update(Integer id, CustomerRequest req) {
        validate(req);
        Customer c = get(id);
        copy(req, c);
        customers.flush();
        return toDto(c);
    }

    // customers with orders are anonymized because the orders must stay
    @Transactional
    public void delete(Integer id) {
        Customer c = get(id);
        List<Address> list = addresses.findByCustomerIdOrderById(id);
        if (orders.existsByCustomerId(id)) {
            c.setFirstName("Deleted");
            c.setLastName("Customer");
            c.setEmail("deleted-" + id + "@example.invalid");
            c.setPhone(null);
            list.forEach(a -> {
                a.setStreetAddress("Deleted");
                a.setPostalCode(null);
            });
        } else {
            addresses.deleteAll(list);
            customers.delete(c);
        }
    }

    @Transactional(readOnly = true)
    public PageDto<OrderSummaryDto> orders(Integer id, int page, int size) {
        get(id);
        var result = orderSummaries.findByCustomerIdOrderByOrderDateDesc(id, PageRequest.of(page, Math.min(size, 100)));
        return PageDto.of(result, OrderSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public List<AddressDto> addresses(Integer id) {
        get(id);
        return addresses.findByCustomerIdOrderById(id).stream().map(AddressDto::from).toList();
    }

    @Transactional
    public AddressDto addAddress(Integer id, AddressRequest req) {
        validate(req);
        Address a = new Address();
        a.setCustomer(get(id));
        copy(req, a);
        return AddressDto.from(addresses.save(a));
    }

    @Transactional
    public AddressDto updateAddress(Integer id, Integer addressId, AddressRequest req) {
        validate(req);
        Address a = getAddress(id, addressId);
        copy(req, a);
        return AddressDto.from(a);
    }

    @Transactional
    public void deleteAddress(Integer id, Integer addressId) {
        Address a = getAddress(id, addressId);
        if (orders.existsByShippingAddressId(addressId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Address is used in an order and can not be deleted");
        }
        addresses.delete(a);
    }

    Customer get(Integer id) {
        return customers.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer " + id + " not found"));
    }

    Address getAddress(Integer customerId, Integer addressId) {
        return addresses.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address " + addressId + " not found"));
    }

    private CustomerDto toDto(Customer c) {
        int points = profiles.findById(c.getId()).map(CustomerProfile::getLoyaltyPoints).orElse(0);
        List<AddressDto> list = addresses.findByCustomerIdOrderById(c.getId()).stream().map(AddressDto::from).toList();
        return new CustomerDto(c.getId(), c.getFirstName(), c.getLastName(), c.getEmail(), c.getPhone(), points, list);
    }

    private void validate(CustomerRequest req) {
        if (isBlank(req.firstName()) || isBlank(req.lastName()) || isBlank(req.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "firstName, lastName and email are required");
        }
        if (!req.email().trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is not valid");
        }
    }

    private void validate(AddressRequest req) {
        if (isBlank(req.streetAddress()) || isBlank(req.city())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "streetAddress and city are required");
        }
    }

    private void copy(CustomerRequest req, Customer c) {
        c.setFirstName(req.firstName());
        c.setLastName(req.lastName());
        c.setEmail(req.email());
        c.setPhone(req.phone());
    }

    private void copy(AddressRequest req, Address a) {
        a.setStreetAddress(req.streetAddress());
        a.setPostalCode(req.postalCode());
        a.setCity(req.city());
        a.setCountry(req.country());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
