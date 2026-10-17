package com.example.webshop.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Immutable
@IdClass(CampaignPrice.Key.class)
@Table(name = "product_prices")
public class CampaignPrice {

    @Id
    @Column(name = "product_id")
    private Integer productId;

    @Id
    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    private BigDecimal price;

    public Integer getProductId() { return productId; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public BigDecimal getPrice() { return price; }

    public record Key(Integer productId, LocalDate validFrom) implements Serializable {
    }
}
