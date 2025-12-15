package br.com.labs.router;

import br.com.labs.handler.AuthHandler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class AuthRouter {

    public static Router create(Vertx vertx, AuthHandler authHandler) {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        // POST /api/v1/auth/register
        router.post("/register").handler(authHandler::register);

        // POST /api/v1/auth/login
        router.post("/login").handler(authHandler::login);

        return router;
    }
}
