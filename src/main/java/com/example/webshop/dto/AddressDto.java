package com.example.webshop.dto;

import com.example.webshop.entity.Address;
import com.example.webshop.repository.AddressRepository.AddressRow;

public record AddressDto(Integer id, String streetAddress, String postalCode, String city, String country) {

    public static AddressDto from(Address a) {
        return new AddressDto(a.getId(), a.getStreetAddress(), a.getPostalCode(), a.getCity(), a.getCountry());
    }

    public static AddressDto from(Integer id, AddressRow r) {
        return new AddressDto(id, r.getStreetAddress(), r.getPostalCode(), r.getCity(), r.getCountry());
    }
}
