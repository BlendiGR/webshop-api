package com.example.webshop.repository;

import com.example.webshop.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItem.Key> {

    // join fetch loads the products in the same query, no n+1 problem
    @Query("SELECT i FROM OrderItem i JOIN FETCH i.product WHERE i.order.id = :orderId")
    List<OrderItem> findByOrderId(Integer orderId);
}
