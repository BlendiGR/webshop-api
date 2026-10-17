package com.example.webshop.service;

import com.example.webshop.dto.*;
import com.example.webshop.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductCatalogRepository catalog;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final CampaignPriceRepository campaigns;

    public ProductService(ProductCatalogRepository catalog, ProductRepository products, CategoryRepository categories,
                          CampaignPriceRepository campaigns) {
        this.catalog = catalog;
        this.products = products;
        this.categories = categories;
        this.campaigns = campaigns;
    }

    public PageDto<ProductDto> search(Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock,
                                      String name, String sort, int page, int size) {
        Sort order = switch (sort) {
            case "price" -> Sort.by("effectivePrice");
            case "price_desc" -> Sort.by("effectivePrice").descending();
            case "name" -> Sort.by("name");
            default -> Sort.by("productId");
        };
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), order);
        var spec = ProductSpecs.filter(categoryId, minPrice, maxPrice, inStock, name);
        return PageDto.of(catalog.findAll(spec, pageRequest), ProductDto::from);
    }

    public List<ProductDto> fullTextSearch(String q) {
        if (q == null || q.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search text is required");
        }
        return catalog.fullTextSearch(q).stream().map(ProductDto::from).toList();
    }

    public ProductDetailDto findById(Integer id) {
        ProductDto product = catalog.findById(id).map(ProductDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + id + " not found"));
        List<ProductDetailDto.Campaign> list = campaigns
                .findByProductIdAndValidToAfterOrderByValidFrom(id, LocalDate.now()).stream()
                .map(c -> new ProductDetailDto.Campaign(c.getPrice(), c.getValidFrom(), c.getValidTo()))
                .toList();
        return new ProductDetailDto(product, list);
    }

    public List<PriceHistoryDto> priceHistory(Integer id) {
        List<PriceHistoryDto> rows = products.priceHistory(id).stream()
                .map(r -> new PriceHistoryDto(r.getName(), r.getPrice(), r.getValidFrom(), r.getValidTo()))
                .toList();
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + id + " not found");
        }
        return rows;
    }

    public List<CategoryDto> categories() {
        return categories.findAll(Sort.by("name")).stream().map(CategoryDto::from).toList();
    }

    public PageDto<ProductDto> categoryProducts(Integer categoryId, int page, int size) {
        if (!categories.existsById(categoryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category " + categoryId + " not found");
        }
        return search(categoryId, null, null, null, null, "price", page, size);
    }
}
