package com.example.webshop.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceHistoryDto(String name, BigDecimal price, LocalDateTime validFrom, LocalDateTime validTo) {
}
