package br.com.labs.handler;

import br.com.labs.dto.request.CreateOrderRequest;
import br.com.labs.dto.response.ApiResponse;
import br.com.labs.exception.ValidationException;
import br.com.labs.service.OrderService;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderHandler {

    private final OrderService orderService;

    public OrderHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    public void create(RoutingContext ctx) {
        UUID userId = getUserIdFromContext(ctx);
        if (userId == null) {
            ctx.fail(new ValidationException("User ID not found in token"));
            return;
        }

        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new ValidationException("Request body is required"));
            return;
        }

        CreateOrderRequest request = new CreateOrderRequest();

        JsonArray itemsArray = body.getJsonArray("items");
        if (itemsArray != null) {
            List<CreateOrderRequest.OrderItemRequest> items = new ArrayList<>();
            for (int i = 0; i < itemsArray.size(); i++) {
                JsonObject itemObj = itemsArray.getJsonObject(i);
                CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest();

                String productIdStr = itemObj.getString("productId");
                if (productIdStr != null) {
                    try {
                        itemRequest.setProductId(UUID.fromString(productIdStr));
                    } catch (IllegalArgumentException e) {
                        ctx.fail(new ValidationException("Invalid product ID format at index " + i));
                        return;
                    }
                }

                itemRequest.setQuantity(itemObj.getInteger("quantity", 0));
                items.add(itemRequest);
            }
            request.setItems(items);
        }

        orderService.create(userId, request)
                .onSuccess(order -> ctx.response()
                        .setStatusCode(201)
                        .putHeader("content-type", "application/json")
                        .end(Json.encode(ApiResponse.success(order))))
                .onFailure(ctx::fail);
    }

    public void findById(RoutingContext ctx) {
        UUID userId = getUserIdFromContext(ctx);
        if (userId == null) {
            ctx.fail(new ValidationException("User ID not found in token"));
            return;
        }

        String idParam = ctx.pathParam("id");

        UUID id;
        try {
            id = UUID.fromString(idParam);
        } catch (IllegalArgumentException e) {
            ctx.fail(new ValidationException("Invalid order ID format"));
            return;
        }

        orderService.findById(id, userId)
                .onSuccess(order -> ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(Json.encode(ApiResponse.success(order))))
                .onFailure(ctx::fail);
    }

    public void findByUser(RoutingContext ctx) {
        UUID userId = getUserIdFromContext(ctx);
        if (userId == null) {
            ctx.fail(new ValidationException("User ID not found in token"));
            return;
        }

        int page = parseIntParam(ctx.queryParam("page").isEmpty() ? "0" : ctx.queryParam("page").get(0), 0);
        int size = parseIntParam(ctx.queryParam("size").isEmpty() ? "10" : ctx.queryParam("size").get(0), 10);

        if (size > 100) {
            size = 100;
        }

        orderService.findByUserId(userId, page, size)
                .onSuccess(pageResponse -> ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(Json.encode(ApiResponse.success(pageResponse))))
                .onFailure(ctx::fail);
    }

    private UUID getUserIdFromContext(RoutingContext ctx) {
        try {
            String sub = ctx.user().principal().getString("sub");
            return UUID.fromString(sub);
        } catch (Exception e) {
            return null;
        }
    }

    private int parseIntParam(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
