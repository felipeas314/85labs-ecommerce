package br.com.labs.verticle;

import br.com.labs.config.AppConfig;
import br.com.labs.handler.*;
import br.com.labs.repository.impl.*;
import br.com.labs.router.*;
import br.com.labs.security.JwtProvider;
import br.com.labs.security.PasswordEncoder;
import br.com.labs.service.impl.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class HttpServerVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);

    private PgPool pgPool;

    @Override
    public void start(Promise<Void> startPromise) {
        AppConfig appConfig = new AppConfig(config());

        // Initialize PostgreSQL connection pool
        pgPool = createPgPool(appConfig);

        // Initialize components
        PasswordEncoder passwordEncoder = new PasswordEncoder();
        JwtProvider jwtProvider = new JwtProvider(vertx, appConfig);

        // Initialize repositories
        UserRepositoryPg userRepository = new UserRepositoryPg(pgPool);
        CategoryRepositoryPg categoryRepository = new CategoryRepositoryPg(pgPool);
        ProductRepositoryPg productRepository = new ProductRepositoryPg(pgPool);
        OrderRepositoryPg orderRepository = new OrderRepositoryPg(pgPool);

        // Initialize services
        AuthServiceImpl authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtProvider);
        CategoryServiceImpl categoryService = new CategoryServiceImpl(categoryRepository);
        ProductServiceImpl productService = new ProductServiceImpl(productRepository, categoryRepository);
        OrderServiceImpl orderService = new OrderServiceImpl(orderRepository, productRepository);

        // Initialize handlers
        AuthHandler authHandler = new AuthHandler(authService);
        CategoryHandler categoryHandler = new CategoryHandler(categoryService);
        ProductHandler productHandler = new ProductHandler(productService);
        OrderHandler orderHandler = new OrderHandler(orderService);
        ErrorHandler errorHandler = new ErrorHandler();

        // Create main router
        Router router = Router.router(vertx);

        // Global handlers
        router.route().handler(BodyHandler.create());
        router.route().handler(createCorsHandler());

        // Health check
        router.get("/health").handler(ctx ->
                ctx.response()
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                                .put("status", "UP")
                                .put("timestamp", System.currentTimeMillis())
                                .encode())
        );

        // OpenAPI spec endpoint
        router.get("/openapi.yaml").handler(ctx ->
                vertx.fileSystem().readFile("openapi.yaml")
                        .onSuccess(buffer -> ctx.response()
                                .putHeader("content-type", "application/x-yaml")
                                .end(buffer))
                        .onFailure(err -> ctx.response()
                                .setStatusCode(404)
                                .end("OpenAPI spec not found"))
        );

        router.get("/openapi.json").handler(ctx ->
                vertx.fileSystem().readFile("openapi.yaml")
                        .onSuccess(buffer -> {
                            // Return YAML as-is, clients can convert if needed
                            ctx.response()
                                    .putHeader("content-type", "application/x-yaml")
                                    .end(buffer);
                        })
                        .onFailure(err -> ctx.response()
                                .setStatusCode(404)
                                .end("OpenAPI spec not found"))
        );

        // Swagger UI redirect
        router.get("/swagger-ui").handler(ctx ->
                ctx.response()
                        .putHeader("content-type", "text/html")
                        .end(getSwaggerUIHtml())
        );

        router.get("/docs").handler(ctx ->
                ctx.response()
                        .putHeader("content-type", "text/html")
                        .end(getSwaggerUIHtml())
        );

        // API routes
        Router apiRouter = Router.router(vertx);

        // Mount sub-routers
        apiRouter.route("/auth/*").subRouter(AuthRouter.create(vertx, authHandler));
        apiRouter.route("/categories/*").subRouter(CategoryRouter.create(vertx, categoryHandler, jwtProvider));
        apiRouter.route("/products/*").subRouter(ProductRouter.create(vertx, productHandler, jwtProvider));
        apiRouter.route("/orders/*").subRouter(OrderRouter.create(vertx, orderHandler, jwtProvider));

        router.route("/api/v1/*").subRouter(apiRouter);

        // Error handler
        router.route().failureHandler(errorHandler::handle);

        // Start HTTP server
        int port = appConfig.getServerPort();
        String host = appConfig.getServerHost();

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port, host)
                .onSuccess(server -> {
                    logger.info("HTTP server started on {}:{}", host, server.actualPort());
                    startPromise.complete();
                })
                .onFailure(err -> {
                    logger.error("Failed to start HTTP server", err);
                    startPromise.fail(err);
                });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        if (pgPool != null) {
            pgPool.close()
                    .onSuccess(v -> {
                        logger.info("PostgreSQL connection pool closed");
                        stopPromise.complete();
                    })
                    .onFailure(stopPromise::fail);
        } else {
            stopPromise.complete();
        }
    }

    private PgPool createPgPool(AppConfig config) {
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setHost(config.getDbHost())
                .setPort(config.getDbPort())
                .setDatabase(config.getDbName())
                .setUser(config.getDbUser())
                .setPassword(config.getDbPassword());

        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(config.getDbMaxPoolSize());

        return PgPool.pool(vertx, connectOptions, poolOptions);
    }

    private CorsHandler createCorsHandler() {
        Set<String> allowedHeaders = new HashSet<>();
        allowedHeaders.add("Content-Type");
        allowedHeaders.add("Authorization");
        allowedHeaders.add("Accept");

        Set<io.vertx.core.http.HttpMethod> allowedMethods = new HashSet<>();
        allowedMethods.add(io.vertx.core.http.HttpMethod.GET);
        allowedMethods.add(io.vertx.core.http.HttpMethod.POST);
        allowedMethods.add(io.vertx.core.http.HttpMethod.PUT);
        allowedMethods.add(io.vertx.core.http.HttpMethod.DELETE);
        allowedMethods.add(io.vertx.core.http.HttpMethod.OPTIONS);

        return CorsHandler.create()
                .addOrigin("*")
                .allowedHeaders(allowedHeaders)
                .allowedMethods(allowedMethods);
    }

    private String getSwaggerUIHtml() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>85Labs E-commerce API - Swagger UI</title>
                    <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@5.10.3/swagger-ui.css">
                    <style>
                        body { margin: 0; padding: 0; }
                        .swagger-ui .topbar { display: none; }
                    </style>
                </head>
                <body>
                    <div id="swagger-ui"></div>
                    <script src="https://unpkg.com/swagger-ui-dist@5.10.3/swagger-ui-bundle.js"></script>
                    <script src="https://unpkg.com/swagger-ui-dist@5.10.3/swagger-ui-standalone-preset.js"></script>
                    <script>
                        window.onload = function() {
                            SwaggerUIBundle({
                                url: "/openapi.yaml",
                                dom_id: '#swagger-ui',
                                presets: [
                                    SwaggerUIBundle.presets.apis,
                                    SwaggerUIStandalonePreset
                                ],
                                layout: "StandaloneLayout"
                            });
                        };
                    </script>
                </body>
                </html>
                """;
    }
}
