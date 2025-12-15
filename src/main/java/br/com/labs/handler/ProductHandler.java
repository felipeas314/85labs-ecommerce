package br.com.labs.handler;

import br.com.labs.dto.request.CreateProductRequest;
import br.com.labs.dto.request.UpdateProductRequest;
import br.com.labs.dto.response.ApiResponse;
import br.com.labs.exception.ValidationException;
import br.com.labs.service.ProductService;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductHandler {

    private final ProductService productService;

    public ProductHandler(ProductService productService) {
        this.productService = productService;
    }

    public void create(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new ValidationException("Request body is required"));
            return;
        }

        CreateProductRequest request = new CreateProductRequest();
        request.setName(body.getString("name"));
        request.setDescription(body.getString("description"));
        request.setCode(body.getString("code"));
        request.setPrice(body.getDouble("price") != null ? BigDecimal.valueOf(body.getDouble("price")) : null);

        String categoryIdStr = body.getString("categoryId");
        if (categoryIdStr != null) {
            try {
                request.setCategoryId(UUID.fromString(categoryIdStr));
            } catch (IllegalArgumentException e) {
                ctx.fail(new ValidationException("Invalid category ID format"));
                return;
            }
        }

        productService.create(request)
                .onSuccess(product -> ctx.response()
                        .setStatusCode(201)
                        .putHeader("content-type", "application/json")
                        .end(Json.encode(ApiResponse.success(product))))
                .onFailure(ctx::fail);
    }

    public void findById(RoutingContext ctx) {
        String idParam = ctx.pathParam("id");

        UUID id;
        try {
            id = UUID.fromString(idParam);
        } catch (IllegalArgumentException e) {
            ctx.fail(new ValidationException("Invalid product ID format"));
            return;
        }

        productService.findById(id)
                .onSuccess(product -> ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(Json.encode(ApiResponse.success(product))))
                .onFailure(ctx::fail);
    }

    public void findAll(RoutingContext ctx) {
        int page = parseIntParam(ctx.queryParam("page").isEmpty() ? "0" : ctx.queryParam("page").get(0), 0);
        int size = parseIntParam(ctx.queryParam("size").isEmpty() ? "10" : ctx.queryParam("size").get(0), 10);

        if (size > 100) {
            size = 100;
        }

        productService.findAll(page, size)
                .onSuccess(pageResponse -> ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(Json.encode(ApiResponse.success(pageResponse))))
                .onFailure(ctx::fail);
    }

    public void update(RoutingContext ctx) {
        String idParam = ctx.pathParam("id");

        UUID id;
        try {
            id = UUID.fromString(idParam);
        } catch (IllegalArgumentException e) {
            ctx.fail(new ValidationException("Invalid product ID format"));
            return;
        }

        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new ValidationException("Request body is required"));
            return;
        }

        UpdateProductRequest request = new UpdateProductRequest();
        request.setName(body.getString("name"));
        request.setDescription(body.getString("description"));
        request.setPrice(body.getDouble("price") != null ? BigDecimal.valueOf(body.getDouble("price")) : null);

        String categoryIdStr = body.getString("categoryId");
        if (categoryIdStr != null) {
            try {
                request.setCategoryId(UUID.fromString(categoryIdStr));
            } catch (IllegalArgumentException e) {
                ctx.fail(new ValidationException("Invalid category ID format"));
                return;
            }
        }

        productService.update(id, request)
                .onSuccess(product -> ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(Json.encode(ApiResponse.success(product))))
                .onFailure(ctx::fail);
    }

    public void delete(RoutingContext ctx) {
        String idParam = ctx.pathParam("id");

        UUID id;
        try {
            id = UUID.fromString(idParam);
        } catch (IllegalArgumentException e) {
            ctx.fail(new ValidationException("Invalid product ID format"));
            return;
        }

        productService.delete(id)
                .onSuccess(v -> ctx.response()
                        .setStatusCode(204)
                        .end())
                .onFailure(ctx::fail);
    }

    private int parseIntParam(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
