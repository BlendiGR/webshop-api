package com.example.webshop.repository;

import com.example.webshop.entity.ProductCatalog;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

// criteria api filters, only the given ones are added to the where clause
public class ProductSpecs {

    public static Specification<ProductCatalog> filter(Integer categoryId, BigDecimal minPrice,
                                                       BigDecimal maxPrice, Boolean inStock, String name) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (categoryId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("categoryId"), categoryId));
            }
            if (minPrice != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("effectivePrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("effectivePrice"), maxPrice));
            }
            if (Boolean.TRUE.equals(inStock)) {
                predicates = cb.and(predicates, cb.isTrue(root.get("inStock")));
            }
            if (name != null && !name.isBlank()) {
                predicates = cb.and(predicates, cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            return predicates;
        };
    }
}
