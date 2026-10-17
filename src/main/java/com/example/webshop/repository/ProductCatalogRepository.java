package com.example.webshop.repository;

import com.example.webshop.entity.ProductCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductCatalogRepository extends JpaRepository<ProductCatalog, Integer>,
        JpaSpecificationExecutor<ProductCatalog> {

    // fulltext index ft_products_name_desc, best match first
    @Query(value = """
            SELECT v.* FROM v_product_catalog v
            JOIN products p ON p.id = v.product_id
            WHERE MATCH(p.name, p.description) AGAINST (:q)
            ORDER BY MATCH(p.name, p.description) AGAINST (:q) DESC
            LIMIT 20""", nativeQuery = true)
    List<ProductCatalog> fullTextSearch(String q);
}
