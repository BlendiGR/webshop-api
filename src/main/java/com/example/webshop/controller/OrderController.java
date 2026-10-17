package com.example.webshop.controller;

import com.example.webshop.dto.OrderDto;
import com.example.webshop.dto.OrderRequest;
import com.example.webshop.dto.StatusChangeDto;
import com.example.webshop.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto create(@RequestBody OrderRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public OrderDto get(@PathVariable Integer id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/history")
    public List<StatusChangeDto> history(@PathVariable Integer id) {
        return service.statusHistory(id);
    }

    @PostMapping("/{id}/pay")
    public OrderDto pay(@PathVariable Integer id) {
        return service.pay(id);
    }

    @PostMapping("/{id}/cancel")
    public OrderDto cancel(@PathVariable Integer id) {
        return service.cancel(id);
    }
}
