package br.com.labs.config;

import io.vertx.core.json.JsonObject;

public class AppConfig {

    private final JsonObject config;

    public AppConfig(JsonObject config) {
        this.config = config;
    }

    // Server config
    public int getServerPort() {
        return config.getJsonObject("server", new JsonObject()).getInteger("port", 8080);
    }

    public String getServerHost() {
        return config.getJsonObject("server", new JsonObject()).getString("host", "0.0.0.0");
    }

    // Database config
    public String getDbHost() {
        return config.getJsonObject("database", new JsonObject()).getString("host", "localhost");
    }

    public int getDbPort() {
        return config.getJsonObject("database", new JsonObject()).getInteger("port", 5432);
    }

    public String getDbName() {
        return config.getJsonObject("database", new JsonObject()).getString("database", "ecommerce");
    }

    public String getDbUser() {
        return config.getJsonObject("database", new JsonObject()).getString("user", "ecommerce");
    }

    public String getDbPassword() {
        return config.getJsonObject("database", new JsonObject()).getString("password", "ecommerce123");
    }

    public int getDbMaxPoolSize() {
        return config.getJsonObject("database", new JsonObject()).getInteger("maxPoolSize", 10);
    }

    public String getJdbcUrl() {
        return String.format("jdbc:postgresql://%s:%d/%s", getDbHost(), getDbPort(), getDbName());
    }

    // JWT config
    public String getJwtSecret() {
        return config.getJsonObject("jwt", new JsonObject())
                .getString("secret", "your-super-secret-key-change-in-production-min-256-bits");
    }

    public String getJwtIssuer() {
        return config.getJsonObject("jwt", new JsonObject()).getString("issuer", "85labs-ecommerce");
    }

    public int getJwtExpirationMinutes() {
        return config.getJsonObject("jwt", new JsonObject()).getInteger("expirationMinutes", 60);
    }

    public JsonObject getRawConfig() {
        return config;
    }
}
