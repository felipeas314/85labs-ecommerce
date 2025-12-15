package br.com.labs.security;

import br.com.labs.config.AppConfig;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.handler.JWTAuthHandler;

import java.util.UUID;

public class JwtProvider {

    private final JWTAuth jwtAuth;
    private final int expirationMinutes;
    private final String issuer;

    public JwtProvider(Vertx vertx, AppConfig config) {
        this.expirationMinutes = config.getJwtExpirationMinutes();
        this.issuer = config.getJwtIssuer();

        JWTAuthOptions jwtAuthOptions = new JWTAuthOptions()
                .addPubSecKey(new PubSecKeyOptions()
                        .setAlgorithm("HS256")
                        .setBuffer(config.getJwtSecret()));

        this.jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);
    }

    public String generateToken(UUID userId, String email) {
        JsonObject claims = new JsonObject()
                .put("sub", userId.toString())
                .put("email", email)
                .put("iss", issuer);

        JWTOptions options = new JWTOptions()
                .setExpiresInMinutes(expirationMinutes)
                .setIssuer(issuer);

        return jwtAuth.generateToken(claims, options);
    }

    public JWTAuth getJwtAuth() {
        return jwtAuth;
    }

    public JWTAuthHandler createAuthHandler() {
        return JWTAuthHandler.create(jwtAuth);
    }

    public io.vertx.core.Future<io.vertx.ext.auth.User> authenticate(String token) {
        return jwtAuth.authenticate(new TokenCredentials(token));
    }
}
