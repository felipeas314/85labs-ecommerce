package br.com.labs.router;

import br.com.labs.handler.CategoryHandler;
import br.com.labs.security.JwtProvider;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class CategoryRouter {

    public static Router create(Vertx vertx, CategoryHandler categoryHandler, JwtProvider jwtProvider) {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        // All routes are protected by JWT
        router.route().handler(jwtProvider.createAuthHandler());

        // GET /api/v1/categories
        router.get("/").handler(categoryHandler::findAll);

        // GET /api/v1/categories/:id
        router.get("/:id").handler(categoryHandler::findById);

        // POST /api/v1/categories
        router.post("/").handler(categoryHandler::create);

        return router;
    }
}
