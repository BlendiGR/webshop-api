package com.example.webshop.dto;

import com.example.webshop.entity.Category;

public record CategoryDto(Integer id, String name, String description) {

    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getDescription());
    }
}
