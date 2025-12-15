package br.com.labs.handler;

import br.com.labs.dto.request.LoginRequest;
import br.com.labs.dto.request.RegisterRequest;
import br.com.labs.dto.response.ApiResponse;
import br.com.labs.dto.response.TokenResponse;
import br.com.labs.exception.ValidationException;
import br.com.labs.service.AuthService;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class AuthHandler {

    private final AuthService authService;

    public AuthHandler(AuthService authService) {
        this.authService = authService;
    }

    public void register(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new ValidationException("Request body is required"));
            return;
        }

        RegisterRequest request = new RegisterRequest();
        request.setEmail(body.getString("email"));
        request.setPassword(body.getString("password"));
        request.setName(body.getString("name"));

        // Validate request
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            ctx.fail(new ValidationException("Email is required"));
            return;
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            ctx.fail(new ValidationException("Password is required"));
            return;
        }
        if (request.getName() == null || request.getName().isBlank()) {
            ctx.fail(new ValidationException("Name is required"));
            return;
        }

        authService.register(request)
                .onSuccess(tokenResponse -> ctx.response()
                        .setStatusCode(201)
                        .putHeader("content-type", "application/json")
                        .end(Json.encode(ApiResponse.success(tokenResponse))))
                .onFailure(ctx::fail);
    }

    public void login(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.fail(new ValidationException("Request body is required"));
            return;
        }

        LoginRequest request = new LoginRequest();
        request.setEmail(body.getString("email"));
        request.setPassword(body.getString("password"));

        // Validate request
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            ctx.fail(new ValidationException("Email is required"));
            return;
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            ctx.fail(new ValidationException("Password is required"));
            return;
        }

        authService.login(request)
                .onSuccess(tokenResponse -> ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(Json.encode(ApiResponse.success(tokenResponse))))
                .onFailure(ctx::fail);
    }
}
