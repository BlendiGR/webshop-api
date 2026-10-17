package com.example.webshop.dto;

import com.example.webshop.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(Integer orderId, Integer customerId, LocalDateTime orderDate, LocalDateTime deliveryDate,
                       OrderStatus status, AddressDto shippingAddress, List<Line> items, BigDecimal totalAmount) {

    public record Line(Integer productId, String productName, int quantity, BigDecimal unitPrice,
                       BigDecimal lineTotal) {
    }
}
