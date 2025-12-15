package br.com.labs;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting 85Labs E-commerce API...");

        VertxOptions options = new VertxOptions();

        Vertx vertx = Vertx.vertx(options);

        vertx.deployVerticle(new MainVerticle())
                .onSuccess(id -> logger.info("MainVerticle deployed successfully with id: {}", id))
                .onFailure(err -> {
                    logger.error("Failed to deploy MainVerticle", err);
                    System.exit(1);
                });
    }
}
