package br.com.labs.model;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItem {

    private UUID id;
    private UUID orderId;
    private UUID productId;
    private int quantity;
    private BigDecimal unitPrice;

    public OrderItem() {}

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        if (unitPrice == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final OrderItem item = new OrderItem();

        public Builder id(UUID id) {
            item.id = id;
            return this;
        }

        public Builder orderId(UUID orderId) {
            item.orderId = orderId;
            return this;
        }

        public Builder productId(UUID productId) {
            item.productId = productId;
            return this;
        }

        public Builder quantity(int quantity) {
            item.quantity = quantity;
            return this;
        }

        public Builder unitPrice(BigDecimal unitPrice) {
            item.unitPrice = unitPrice;
            return this;
        }

        public OrderItem build() {
            return item;
        }
    }
}
