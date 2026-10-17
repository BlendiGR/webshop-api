package com.example.webshop.dto;

import java.util.List;

public record CustomerDto(Integer id, String firstName, String lastName, String email, String phone,
                          int loyaltyPoints, List<AddressDto> addresses) {
}
