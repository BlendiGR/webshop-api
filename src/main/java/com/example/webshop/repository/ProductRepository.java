package com.example.webshop.repository;

import com.example.webshop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    // takes stock only if there is enough, returns 0 rows otherwise
    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :qty WHERE p.id = :id AND p.stockQuantity >= :qty")
    int reserveStock(Integer id, int qty);

    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :qty WHERE p.id = :id")
    int releaseStock(Integer id, int qty);

    @Query(value = "SELECT fn_effective_price(:id, CURDATE())", nativeQuery = true)
    BigDecimal effectivePrice(Integer id);

    // system versioning, every version of the product row
    @Query(value = """
            SELECT name, price, row_start AS validFrom, row_end AS validTo
            FROM products FOR SYSTEM_TIME ALL
            WHERE id = :id
            ORDER BY row_start""", nativeQuery = true)
    List<PriceHistoryRow> priceHistory(Integer id);

    interface PriceHistoryRow {
        String getName();
        BigDecimal getPrice();
        LocalDateTime getValidFrom();
        LocalDateTime getValidTo();
    }
}
