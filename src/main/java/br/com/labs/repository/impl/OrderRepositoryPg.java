package br.com.labs.repository.impl;

import br.com.labs.model.Order;
import br.com.labs.model.OrderItem;
import br.com.labs.repository.OrderRepository;
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

public class OrderRepositoryPg implements OrderRepository {

    private final Pool pool;

    public OrderRepositoryPg(Pool pool) {
        this.pool = pool;
    }

    @Override
    public Future<Order> save(Order order) {
        String sql = """
            INSERT INTO orders (user_id, status, total, created_at, updated_at)
            VALUES ($1, $2, $3, $4, $5)
            RETURNING id, user_id, status, total, created_at, updated_at
            """;

        LocalDateTime now = LocalDateTime.now();

        return pool.preparedQuery(sql)
                .execute(Tuple.of(
                        order.getUserId(),
                        order.getStatus().name(),
                        order.getTotal(),
                        now,
                        now
                ))
                .map(rows -> mapRow(rows.iterator().next()));
    }

    @Override
    public Future<Optional<Order>> findById(UUID id) {
        String sql = "SELECT * FROM orders WHERE id = $1";

        return pool.preparedQuery(sql)
                .execute(Tuple.of(id))
                .compose(rows -> {
                    if (rows.rowCount() == 0) {
                        return Future.succeededFuture(Optional.empty());
                    }
                    Order order = mapRow(rows.iterator().next());
                    return findItemsByOrderId(order.getId())
                            .map(items -> {
                                order.setItems(items);
                                return Optional.of(order);
                            });
                });
    }

    @Override
    public Future<List<Order>> findByUserId(UUID userId, int page, int size) {
        String sql = "SELECT * FROM orders WHERE user_id = $1 ORDER BY created_at DESC LIMIT $2 OFFSET $3";
        int offset = page * size;

        return pool.preparedQuery(sql)
                .execute(Tuple.of(userId, size, offset))
                .map(this::mapRows);
    }

    @Override
    public Future<Long> countByUserId(UUID userId) {
        String sql = "SELECT COUNT(*) FROM orders WHERE user_id = $1";

        return pool.preparedQuery(sql)
                .execute(Tuple.of(userId))
                .map(rows -> rows.iterator().next().getLong(0));
    }

    @Override
    public Future<List<OrderItem>> findItemsByOrderId(UUID orderId) {
        String sql = "SELECT * FROM order_items WHERE order_id = $1";

        return pool.preparedQuery(sql)
                .execute(Tuple.of(orderId))
                .map(this::mapItemRows);
    }

    @Override
    public Future<OrderItem> saveItem(OrderItem item) {
        String sql = """
            INSERT INTO order_items (order_id, product_id, quantity, unit_price)
            VALUES ($1, $2, $3, $4)
            RETURNING id, order_id, product_id, quantity, unit_price
            """;

        return pool.preparedQuery(sql)
                .execute(Tuple.of(
                        item.getOrderId(),
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .map(rows -> mapItemRow(rows.iterator().next()));
    }

    private Order mapRow(Row row) {
        return Order.builder()
                .id(row.getUUID("id"))
                .userId(row.getUUID("user_id"))
                .status(Order.OrderStatus.valueOf(row.getString("status")))
                .total(row.getBigDecimal("total"))
                .createdAt(row.getLocalDateTime("created_at"))
                .updatedAt(row.getLocalDateTime("updated_at"))
                .build();
    }

    private List<Order> mapRows(RowSet<Row> rows) {
        List<Order> orders = new ArrayList<>();
        for (Row row : rows) {
            orders.add(mapRow(row));
        }
        return orders;
    }

    private OrderItem mapItemRow(Row row) {
        return OrderItem.builder()
                .id(row.getUUID("id"))
                .orderId(row.getUUID("order_id"))
                .productId(row.getUUID("product_id"))
                .quantity(row.getInteger("quantity"))
                .unitPrice(row.getBigDecimal("unit_price"))
                .build();
    }

    private List<OrderItem> mapItemRows(RowSet<Row> rows) {
        List<OrderItem> items = new ArrayList<>();
        for (Row row : rows) {
            items.add(mapItemRow(row));
        }
        return items;
    }
}
