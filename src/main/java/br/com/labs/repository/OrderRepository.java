package br.com.labs.repository;

import br.com.labs.model.Order;
import br.com.labs.model.OrderItem;
import io.vertx.core.Future;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Future<Order> save(Order order);

    Future<Optional<Order>> findById(UUID id);

    Future<List<Order>> findByUserId(UUID userId, int page, int size);

    Future<Long> countByUserId(UUID userId);

    Future<List<OrderItem>> findItemsByOrderId(UUID orderId);

    Future<OrderItem> saveItem(OrderItem item);
}
