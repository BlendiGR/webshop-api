package com.example.webshop.dto;

import com.example.webshop.entity.OrderStatus;
import com.example.webshop.entity.OrderSummary;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryDto(Integer orderId, LocalDateTime orderDate, OrderStatus status, long totalQuantity,
                              BigDecimal totalAmount) {

    public static OrderSummaryDto from(OrderSummary o) {
        return new OrderSummaryDto(o.getOrderId(), o.getOrderDate(), o.getStatus(), o.getTotalQuantity(),
                o.getTotalAmount());
    }
}
