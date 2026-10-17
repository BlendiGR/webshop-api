package com.example.webshop.controller;

import com.example.webshop.dto.*;
import com.example.webshop.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    public CustomerDto byEmail(@RequestParam String email) {
        return service.findByEmail(email);
    }

    @GetMapping("/{id}")
    public CustomerDto get(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDto create(@RequestBody CustomerRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public CustomerDto update(@PathVariable Integer id, @RequestBody CustomerRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }

    @GetMapping("/{id}/orders")
    public PageDto<OrderSummaryDto> orders(@PathVariable Integer id,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return service.orders(id, page, size);
    }

    @GetMapping("/{id}/addresses")
    public List<AddressDto> addresses(@PathVariable Integer id) {
        return service.addresses(id);
    }

    @PostMapping("/{id}/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressDto addAddress(@PathVariable Integer id, @RequestBody AddressRequest req) {
        return service.addAddress(id, req);
    }

    @PutMapping("/{id}/addresses/{addressId}")
    public AddressDto updateAddress(@PathVariable Integer id, @PathVariable Integer addressId,
                                    @RequestBody AddressRequest req) {
        return service.updateAddress(id, addressId, req);
    }

    @DeleteMapping("/{id}/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@PathVariable Integer id, @PathVariable Integer addressId) {
        service.deleteAddress(id, addressId);
    }
}
