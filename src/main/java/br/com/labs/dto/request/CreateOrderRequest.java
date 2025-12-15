package br.com.labs.dto.request;

import java.util.List;
import java.util.UUID;

public class CreateOrderRequest {

    private List<OrderItemRequest> items;

    public CreateOrderRequest() {}

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    public static class OrderItemRequest {
        private UUID productId;
        private int quantity;

        public OrderItemRequest() {}

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
    }
}
