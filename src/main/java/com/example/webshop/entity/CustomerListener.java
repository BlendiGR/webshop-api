package com.example.webshop.entity;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class CustomerListener {

    // same format every time so the email index lookup always matches
    @PrePersist
    @PreUpdate
    public void normalize(Customer c) {
        c.setEmail(c.getEmail().trim().toLowerCase());
        c.setFirstName(c.getFirstName().trim());
        c.setLastName(c.getLastName().trim());
    }
}
