package com.example.webshop.repository;

import com.example.webshop.entity.OrderSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderSummaryRepository extends JpaRepository<OrderSummary, Integer> {

    Page<OrderSummary> findByCustomerIdOrderByOrderDateDesc(Integer customerId, Pageable pageable);
}
