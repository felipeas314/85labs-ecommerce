package br.com.labs;

import br.com.labs.config.AppConfig;
import br.com.labs.verticle.HttpServerVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Promise<Void> startPromise) {
        loadConfig()
                .compose(config -> {
                    AppConfig appConfig = new AppConfig(config);

                    // Run database migrations
                    runMigrations(appConfig);

                    // Deploy HTTP Server Verticle
                    DeploymentOptions options = new DeploymentOptions().setConfig(config);
                    return vertx.deployVerticle(new HttpServerVerticle(), options);
                })
                .onSuccess(id -> {
                    logger.info("All verticles deployed successfully");
                    startPromise.complete();
                })
                .onFailure(err -> {
                    logger.error("Failed to start application", err);
                    startPromise.fail(err);
                });
    }

    private io.vertx.core.Future<JsonObject> loadConfig() {
        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setFormat("json")
                .setConfig(new JsonObject().put("path", "application.json"));

        ConfigStoreOptions envStore = new ConfigStoreOptions()
                .setType("env");

        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(fileStore)
                .addStore(envStore);

        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

        return retriever.getConfig();
    }

    private void runMigrations(AppConfig config) {
        logger.info("Running database migrations...");

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(config.getJdbcUrl(), config.getDbUser(), config.getDbPassword())
                    .locations("classpath:db/migration")
                    .load();

            int migrationsApplied = flyway.migrate().migrationsExecuted;
            logger.info("Database migrations completed. {} migrations applied.", migrationsApplied);
        } catch (Exception e) {
            logger.error("Failed to run database migrations", e);
            throw new RuntimeException("Database migration failed", e);
        }
    }
}
