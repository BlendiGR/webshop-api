package com.example.webshop.controller;

import com.example.webshop.dto.*;
import com.example.webshop.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/products")
    public PageDto<ProductDto> products(@RequestParam(required = false) Integer categoryId,
                                        @RequestParam(required = false) BigDecimal minPrice,
                                        @RequestParam(required = false) BigDecimal maxPrice,
                                        @RequestParam(required = false) Boolean inStock,
                                        @RequestParam(required = false) String name,
                                        @RequestParam(defaultValue = "id") String sort,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return service.search(categoryId, minPrice, maxPrice, inStock, name, sort, page, size);
    }

    @GetMapping("/products/search")
    public List<ProductDto> search(@RequestParam String q) {
        return service.fullTextSearch(q);
    }

    @GetMapping("/products/{id}")
    public ProductDetailDto product(@PathVariable Integer id) {
        return service.findById(id);
    }

    @GetMapping("/products/{id}/history")
    public List<PriceHistoryDto> priceHistory(@PathVariable Integer id) {
        return service.priceHistory(id);
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return service.categories();
    }

    @GetMapping("/categories/{id}/products")
    public PageDto<ProductDto> categoryProducts(@PathVariable Integer id,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return service.categoryProducts(id, page, size);
    }
}
