package com.example.webshop.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

// link table with extra columns so it needs its own entity and a composite key
@Entity
@Table(name = "orderitems")
public class OrderItem {

    @EmbeddedId
    private Key id = new Key();

    @MapsId("orderId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private int quantity;

    // the trigger sets the same price again in the database so it can not be faked
    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    protected OrderItem() {
    }

    OrderItem(CustomerOrder order, Product product, int quantity, BigDecimal unitPrice) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }

    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "order_id")
        private Integer orderId;

        @Column(name = "product_id")
        private Integer productId;

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(orderId, k.orderId)
                    && Objects.equals(productId, k.productId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(orderId, productId);
        }
    }
}
