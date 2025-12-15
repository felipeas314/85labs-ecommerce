package br.com.labs.repository.impl;

import br.com.labs.model.Category;
import br.com.labs.repository.CategoryRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CategoryRepositoryPg implements CategoryRepository {

    private final Pool pool;

    public CategoryRepositoryPg(Pool pool) {
        this.pool = pool;
    }

    @Override
    public Future<Category> save(Category category) {
        String sql = """
            INSERT INTO categories (name, description, created_at)
            VALUES ($1, $2, $3)
            RETURNING id, name, description, created_at
            """;

        LocalDateTime now = LocalDateTime.now();

        return pool.preparedQuery(sql)
                .execute(Tuple.of(
                        category.getName(),
                        category.getDescription(),
                        now
                ))
                .map(rows -> mapRow(rows.iterator().next()));
    }

    @Override
    public Future<Optional<Category>> findById(UUID id) {
        String sql = "SELECT * FROM categories WHERE id = $1";

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
    public Future<List<Category>> findAll() {
        String sql = "SELECT * FROM categories ORDER BY name";

        return pool.query(sql)
                .execute()
                .map(this::mapRows);
    }

    @Override
    public Future<Boolean> existsById(UUID id) {
        String sql = "SELECT EXISTS(SELECT 1 FROM categories WHERE id = $1)";

        return pool.preparedQuery(sql)
                .execute(Tuple.of(id))
                .map(rows -> rows.iterator().next().getBoolean(0));
    }

    private Category mapRow(Row row) {
        return Category.builder()
                .id(row.getUUID("id"))
                .name(row.getString("name"))
                .description(row.getString("description"))
                .createdAt(row.getLocalDateTime("created_at"))
                .build();
    }

    private List<Category> mapRows(RowSet<Row> rows) {
        List<Category> categories = new ArrayList<>();
        for (Row row : rows) {
            categories.add(mapRow(row));
        }
        return categories;
    }
}
