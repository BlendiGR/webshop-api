package com.example.webshop.repository;

import com.example.webshop.entity.StatusChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusChangeRepository extends JpaRepository<StatusChange, Integer> {

    List<StatusChange> findByOrderIdOrderById(Integer orderId);
}
