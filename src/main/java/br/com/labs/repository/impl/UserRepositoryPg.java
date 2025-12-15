package br.com.labs.repository.impl;

import br.com.labs.model.User;
import br.com.labs.repository.UserRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class UserRepositoryPg implements UserRepository {

    private final Pool pool;

    public UserRepositoryPg(Pool pool) {
        this.pool = pool;
    }

    @Override
    public Future<User> save(User user) {
        String sql = """
            INSERT INTO users (email, password_hash, name, created_at, updated_at)
            VALUES ($1, $2, $3, $4, $5)
            RETURNING id, email, password_hash, name, created_at, updated_at
            """;

        LocalDateTime now = LocalDateTime.now();

        return pool.preparedQuery(sql)
                .execute(Tuple.of(
                        user.getEmail(),
                        user.getPasswordHash(),
                        user.getName(),
                        now,
                        now
                ))
                .map(rows -> {
                    Row row = rows.iterator().next();
                    return mapRow(row);
                });
    }

    @Override
    public Future<Optional<User>> findById(UUID id) {
        String sql = "SELECT * FROM users WHERE id = $1";

        return pool.preparedQuery(sql)
                .execute(Tuple.of(id))
                .map(rows -> {
                    if (rows.rowCount() == 0) {
                        return Optional.empty();
                    }
                    return Optional.of(mapRow(rows.iterator().next()));
                });
    }

    @Override
    public Future<Optional<User>> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = $1";

        return pool.preparedQuery(sql)
                .execute(Tuple.of(email))
                .map(rows -> {
                    if (rows.rowCount() == 0) {
                        return Optional.empty();
                    }
                    return Optional.of(mapRow(rows.iterator().next()));
                });
    }

    @Override
    public Future<Boolean> existsByEmail(String email) {
        String sql = "SELECT EXISTS(SELECT 1 FROM users WHERE email = $1)";

        return pool.preparedQuery(sql)
                .execute(Tuple.of(email))
                .map(rows -> rows.iterator().next().getBoolean(0));
    }

    private User mapRow(Row row) {
        return User.builder()
                .id(row.getUUID("id"))
                .email(row.getString("email"))
                .passwordHash(row.getString("password_hash"))
                .name(row.getString("name"))
                .createdAt(row.getLocalDateTime("created_at"))
                .updatedAt(row.getLocalDateTime("updated_at"))
                .build();
    }
}
