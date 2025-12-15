package br.com.labs.router;

import br.com.labs.handler.ProductHandler;
import br.com.labs.security.JwtProvider;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class ProductRouter {

    public static Router create(Vertx vertx, ProductHandler productHandler, JwtProvider jwtProvider) {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        // All routes are protected by JWT
        router.route().handler(jwtProvider.createAuthHandler());

        // GET /api/v1/products
        router.get("/").handler(productHandler::findAll);

        // GET /api/v1/products/:id
        router.get("/:id").handler(productHandler::findById);

        // POST /api/v1/products
        router.post("/").handler(productHandler::create);

        // PUT /api/v1/products/:id
        router.put("/:id").handler(productHandler::update);

        // DELETE /api/v1/products/:id
        router.delete("/:id").handler(productHandler::delete);

        return router;
    }
}
