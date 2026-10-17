package com.example.webshop.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProductDetailDto(ProductDto product, List<Campaign> campaigns) {

    public record Campaign(BigDecimal price, LocalDate validFrom, LocalDate validTo) {
    }
}
