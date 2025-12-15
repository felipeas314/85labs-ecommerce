package br.com.labs.handler;

import br.com.labs.dto.request.CreateCategoryRequest;
import br.com.labs.dto.response.ApiResponse;
import br.com.labs.exception.ValidationException;
import br.com.labs.service.CategoryService;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.UUID;

public class CategoryHandler {

    private final CategoryService categoryService;

    public CategoryHandler(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public void create(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new ValidationException("Request body is required"));
            return;
        }

        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName(body.getString("name"));
        request.setDescription(body.getString("description"));

        categoryService.create(request)
                .onSuccess(category -> ctx.response()
                        .setStatusCode(201)
                        .putHeader("content-type", "application/json")
                        .end(Json.encode(ApiResponse.success(category))))
                .onFailure(ctx::fail);
    }

    public void findById(RoutingContext ctx) {
        String idParam = ctx.pathParam("id");

        UUID id;
        try {
            id = UUID.fromString(idParam);
        } catch (IllegalArgumentException e) {
            ctx.fail(new ValidationException("Invalid category ID format"));
            return;
        }

        categoryService.findById(id)
                .onSuccess(category -> ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(Json.encode(ApiResponse.success(category))))
                .onFailure(ctx::fail);
    }

    public void findAll(RoutingContext ctx) {
        categoryService.findAll()
                .onSuccess(categories -> ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(Json.encode(ApiResponse.success(categories))))
                .onFailure(ctx::fail);
    }
}
