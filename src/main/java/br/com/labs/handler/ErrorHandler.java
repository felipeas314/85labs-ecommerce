package br.com.labs.handler;

import br.com.labs.dto.response.ApiResponse;
import br.com.labs.exception.InsufficientStockException;
import br.com.labs.exception.NotFoundException;
import br.com.labs.exception.UnauthorizedException;
import br.com.labs.exception.ValidationException;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    public void handle(RoutingContext ctx) {
        Throwable failure = ctx.failure();
        int statusCode = ctx.statusCode();

        if (failure != null) {
            logger.error("Request failed", failure);

            if (failure instanceof ValidationException) {
                statusCode = 400;
            } else if (failure instanceof UnauthorizedException) {
                statusCode = 401;
            } else if (failure instanceof NotFoundException) {
                statusCode = 404;
            } else if (failure instanceof InsufficientStockException) {
                statusCode = 409; // Conflict - resource state conflict
            } else {
                statusCode = 500;
            }

            String message = failure.getMessage();
            if (statusCode == 500) {
                message = "Internal server error";
            }

            ctx.response()
                    .setStatusCode(statusCode)
                    .putHeader("content-type", "application/json")
                    .end(Json.encode(ApiResponse.error(message)));
        } else {
            // No failure object, use status code
            String message = switch (statusCode) {
                case 400 -> "Bad request";
                case 401 -> "Unauthorized";
                case 403 -> "Forbidden";
                case 404 -> "Not found";
                case 405 -> "Method not allowed";
                default -> "Internal server error";
            };

            ctx.response()
                    .setStatusCode(statusCode > 0 ? statusCode : 500)
                    .putHeader("content-type", "application/json")
                    .end(Json.encode(ApiResponse.error(message)));
        }
    }
}
