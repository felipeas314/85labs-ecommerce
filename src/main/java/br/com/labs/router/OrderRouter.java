package br.com.labs.router;

import br.com.labs.handler.OrderHandler;
import br.com.labs.security.JwtProvider;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class OrderRouter {

    public static Router create(Vertx vertx, OrderHandler orderHandler, JwtProvider jwtProvider) {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        // All routes are protected by JWT
        router.route().handler(jwtProvider.createAuthHandler());

        // GET /api/v1/orders
        router.get("/").handler(orderHandler::findByUser);

        // GET /api/v1/orders/:id
        router.get("/:id").handler(orderHandler::findById);

        // POST /api/v1/orders
        router.post("/").handler(orderHandler::create);

        return router;
    }
}
