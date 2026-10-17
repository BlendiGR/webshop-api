package com.example.webshop.dto;

import com.example.webshop.entity.ProductCatalog;

import java.math.BigDecimal;

public record ProductDto(Integer id, String name, String description, BigDecimal price, BigDecimal listPrice,
                         boolean onCampaign, boolean inStock, Integer categoryId, String categoryName,
                         String supplierName) {

    public static ProductDto from(ProductCatalog p) {
        return new ProductDto(p.getProductId(), p.getName(), p.getDescription(), p.getEffectivePrice(),
                p.getListPrice(), p.isOnCampaign(), p.isInStock(), p.getCategoryId(), p.getCategoryName(),
                p.getSupplierName());
    }
}
