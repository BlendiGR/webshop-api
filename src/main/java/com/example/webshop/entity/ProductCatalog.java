package com.example.webshop.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Entity
@Immutable
@Table(name = "v_product_catalog")
public class ProductCatalog {

    @Id
    @Column(name = "product_id")
    private Integer productId;

    private String name;
    private String description;

    @Column(name = "list_price")
    private BigDecimal listPrice;

    @Column(name = "effective_price")
    private BigDecimal effectivePrice;

    @Column(name = "on_campaign")
    private boolean onCampaign;

    @Column(name = "in_stock")
    private boolean inStock;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "supplier_name")
    private String supplierName;

    public Integer getProductId() { return productId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getListPrice() { return listPrice; }
    public BigDecimal getEffectivePrice() { return effectivePrice; }
    public boolean isOnCampaign() { return onCampaign; }
    public boolean isInStock() { return inStock; }
    public Integer getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getSupplierName() { return supplierName; }
}
