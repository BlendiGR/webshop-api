package com.example.webshop.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

// shares the primary key with customers, written only by a trigger
@Entity
@Immutable
@Table(name = "customer_profiles")
public class CustomerProfile {

    @Id
    @Column(name = "customer_id")
    private Integer customerId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "loyalty_points")
    private int loyaltyPoints;

    public int getLoyaltyPoints() { return loyaltyPoints; }
}
