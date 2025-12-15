package br.com.labs.service;

import br.com.labs.dto.request.CreateOrderRequest;
import br.com.labs.dto.response.PageResponse;
import br.com.labs.model.Order;
import io.vertx.core.Future;

import java.util.UUID;

public interface OrderService {

    Future<Order> create(UUID userId, CreateOrderRequest request);

    Future<Order> findById(UUID id, UUID userId);

    Future<PageResponse<Order>> findByUserId(UUID userId, int page, int size);
}
