package br.com.labs.repository.impl;

import br.com.labs.model.Product;
import br.com.labs.repository.ProductRepository;
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

public class ProductRepositoryPg implements ProductRepository {

    private final Pool pool;

    public ProductRepositoryPg(Pool pool) {
        this.pool = pool;
    }

    @Override
    public Future<Product> save(Product product) {
        String sql = """
            INSERT INTO products (name, description, code, price, stock, version, category_id, created_at, updated_at)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
            RETURNING *
            """;

        LocalDateTime now = LocalDateTime.now();
        Integer stock = product.getStock() != null ? product.getStock() : 0;

        return pool.preparedQuery(sql)
                .execute(Tuple.of(
                        product.getName(),
                        product.getDescription(),
                        product.getCode(),
                        product.getPrice(),
                        stock,
                        1, // Initial version
                        product.getCategoryId(),
                        now,
                        now
                ))
                .map(rows -> mapRow(rows.iterator().next()));
    }

    @Override
    public Future<Product> update(Product product) {
        String sql = """
            UPDATE products
            SET name = $1, description = $2, price = $3, stock = $4, category_id = $5, updated_at = $6
            WHERE id = $7
            RETURNING *
            """;

        return pool.preparedQuery(sql)
                .execute(Tuple.of(
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        product.getStock(),
                        product.getCategoryId(),
                        LocalDateTime.now(),
                        product.getId()
                ))
                .map(rows -> mapRow(rows.iterator().next()));
    }

    @Override
    public Future<Optional<Product>> findById(UUID id) {
        String sql = "SELECT * FROM products WHERE id = $1";

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
    public Future<Optional<Product>> findByCode(String code) {
        String sql = "SELECT * FROM products WHERE code = $1";

        return pool.preparedQuery(sql)
                .execute(Tuple.of(code))
                .map(rows -> {
                    if (rows.rowCount() == 0) {
                        return Optional.empty();
                    }
                    return Optional.of(mapRow(rows.iterator().next()));
                });
    }

    @Override
    public Future<List<Product>> findAll(int page, int size) {
        String sql = "SELECT * FROM products ORDER BY created_at DESC LIMIT $1 OFFSET $2";
        int offset = page * size;

        return pool.preparedQuery(sql)
                .execute(Tuple.of(size, offset))
                .map(this::mapRows);
    }

    @Override
    public Future<List<Product>> findByCategory(UUID categoryId, int page, int size) {
        String sql = "SELECT * FROM products WHERE category_id = $1 ORDER BY created_at DESC LIMIT $2 OFFSET $3";
        int offset = page * size;

        return pool.preparedQuery(sql)
                .execute(Tuple.of(categoryId, size, offset))
                .map(this::mapRows);
    }

    @Override
    public Future<Long> count() {
        String sql = "SELECT COUNT(*) FROM products";

        return pool.query(sql)
                .execute()
                .map(rows -> rows.iterator().next().getLong(0));
    }

    @Override
    public Future<Boolean> delete(UUID id) {
        String sql = "DELETE FROM products WHERE id = $1";

        return pool.preparedQuery(sql)
                .execute(Tuple.of(id))
                .map(rows -> rows.rowCount() > 0);
    }

    @Override
    public Future<Boolean> existsByCode(String code) {
        String sql = "SELECT EXISTS(SELECT 1 FROM products WHERE code = $1)";

        return pool.preparedQuery(sql)
                .execute(Tuple.of(code))
                .map(rows -> rows.iterator().next().getBoolean(0));
    }

    @Override
    public Future<Optional<Product>> decrementStock(UUID productId, int quantity, int expectedVersion) {
        // Atomic UPDATE with optimistic locking
        // Only succeeds if: version matches AND stock >= quantity
        String sql = """
            UPDATE products
            SET stock = stock - $1,
                version = version + 1,
                updated_at = $2
            WHERE id = $3
              AND version = $4
              AND stock >= $1
            RETURNING *
            """;

        return pool.preparedQuery(sql)
                .execute(Tuple.of(
                        quantity,
                        LocalDateTime.now(),
                        productId,
                        expectedVersion
                ))
                .map(rows -> {
                    if (rows.rowCount() == 0) {
                        // Either version mismatch (concurrent modification) or insufficient stock
                        return Optional.empty();
                    }
                    return Optional.of(mapRow(rows.iterator().next()));
                });
    }

    private Product mapRow(Row row) {
        return Product.builder()
                .id(row.getUUID("id"))
                .name(row.getString("name"))
                .description(row.getString("description"))
                .code(row.getString("code"))
                .price(row.getBigDecimal("price"))
                .stock(row.getInteger("stock"))
                .version(row.getInteger("version"))
                .categoryId(row.getUUID("category_id"))
                .createdAt(row.getLocalDateTime("created_at"))
                .updatedAt(row.getLocalDateTime("updated_at"))
                .build();
    }

    private List<Product> mapRows(RowSet<Row> rows) {
        List<Product> products = new ArrayList<>();
        for (Row row : rows) {
            products.add(mapRow(row));
        }
        return products;
    }
}
