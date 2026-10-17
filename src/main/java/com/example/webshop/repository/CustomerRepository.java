package com.example.webshop.repository;

import com.example.webshop.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    // uses index idx_email
    Optional<Customer> findFirstByEmail(String email);
}
