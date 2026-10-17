package com.example.webshop.dto;

import java.util.List;

public record OrderRequest(Integer customerId, Integer addressId, List<Item> items) {

    public record Item(Integer productId, Integer quantity) {
    }
}
