package br.com.labs.integration;

import br.com.labs.exception.InsufficientStockException;
import br.com.labs.model.Product;
import br.com.labs.repository.ProductRepository;
import br.com.labs.repository.impl.ProductRepositoryPg;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test demonstrating optimistic locking for stock management.
 *
 * This test creates a product with limited stock and simulates multiple
 * concurrent requests trying to decrement the same stock, proving that
 * optimistic locking prevents overselling.
 */
@Testcontainers
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ecommerce_test")
            .withUsername("test")
            .withPassword("test");

    private static PgPool pool;
    private static ProductRepository productRepository;

    @BeforeAll
    static void setupDatabase(Vertx vertx, VertxTestContext ctx) {
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(postgres.getFirstMappedPort())
                .setHost(postgres.getHost())
                .setDatabase(postgres.getDatabaseName())
                .setUser(postgres.getUsername())
                .setPassword(postgres.getPassword());

        PoolOptions poolOptions = new PoolOptions().setMaxSize(10);
        pool = PgPool.pool(vertx, connectOptions, poolOptions);
        productRepository = new ProductRepositoryPg(pool);

        // Create products table
        String createTable = """
            CREATE TABLE IF NOT EXISTS products (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                name VARCHAR(255) NOT NULL,
                description TEXT,
                code VARCHAR(50) UNIQUE NOT NULL,
                price DECIMAL(10,2) NOT NULL,
                stock INT NOT NULL DEFAULT 0,
                version INT NOT NULL DEFAULT 1,
                category_id UUID,
                created_at TIMESTAMP DEFAULT NOW(),
                updated_at TIMESTAMP DEFAULT NOW()
            )
            """;

        pool.query(createTable)
                .execute()
                .onSuccess(r -> ctx.completeNow())
                .onFailure(ctx::failNow);
    }

    @AfterAll
    static void cleanup() {
        if (pool != null) {
            pool.close();
        }
    }

    @BeforeEach
    void clearTable(VertxTestContext ctx) {
        pool.query("DELETE FROM products")
                .execute()
                .onSuccess(r -> ctx.completeNow())
                .onFailure(ctx::failNow);
    }

    @Test
    @Order(1)
    @DisplayName("Single decrement should work correctly")
    void singleDecrementShouldWork(VertxTestContext ctx) {
        // Create product with stock = 5
        Product product = Product.builder()
                .name("Test Product")
                .code("TEST-001")
                .price(new BigDecimal("99.99"))
                .stock(5)
                .build();

        productRepository.save(product)
                .compose(saved -> {
                    assertThat(saved.getStock()).isEqualTo(5);
                    assertThat(saved.getVersion()).isEqualTo(1);

                    // Decrement stock by 2
                    return productRepository.decrementStock(saved.getId(), 2, saved.getVersion());
                })
                .compose(result -> {
                    assertThat(result).isPresent();
                    assertThat(result.get().getStock()).isEqualTo(3);
                    assertThat(result.get().getVersion()).isEqualTo(2);
                    ctx.completeNow();
                    return Future.succeededFuture();
                })
                .onFailure(ctx::failNow);
    }

    @Test
    @Order(2)
    @DisplayName("Decrement with wrong version should fail")
    void decrementWithWrongVersionShouldFail(VertxTestContext ctx) {
        Product product = Product.builder()
                .name("Test Product")
                .code("TEST-002")
                .price(new BigDecimal("99.99"))
                .stock(5)
                .build();

        productRepository.save(product)
                .compose(saved -> {
                    // Try to decrement with wrong version (0 instead of 1)
                    return productRepository.decrementStock(saved.getId(), 1, 0);
                })
                .compose(result -> {
                    // Should return empty because version mismatch
                    assertThat(result).isEmpty();
                    ctx.completeNow();
                    return Future.succeededFuture();
                })
                .onFailure(ctx::failNow);
    }

    @Test
    @Order(3)
    @DisplayName("Decrement with insufficient stock should fail")
    void decrementWithInsufficientStockShouldFail(VertxTestContext ctx) {
        Product product = Product.builder()
                .name("Test Product")
                .code("TEST-003")
                .price(new BigDecimal("99.99"))
                .stock(2)
                .build();

        productRepository.save(product)
                .compose(saved -> {
                    // Try to decrement 5 when only 2 in stock
                    return productRepository.decrementStock(saved.getId(), 5, saved.getVersion());
                })
                .compose(result -> {
                    // Should return empty because insufficient stock
                    assertThat(result).isEmpty();
                    ctx.completeNow();
                    return Future.succeededFuture();
                })
                .onFailure(ctx::failNow);
    }

    @Test
    @Order(4)
    @DisplayName("10 concurrent requests for 1 item in stock - only 1 should succeed")
    void concurrentRequestsForLimitedStock(Vertx vertx, VertxTestContext ctx) throws InterruptedException {
        final int CONCURRENT_REQUESTS = 10;
        final int INITIAL_STOCK = 1;

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(CONCURRENT_REQUESTS);

        // Create product with stock = 1
        Product product = Product.builder()
                .name("Limited Edition Item")
                .code("LIMITED-001")
                .price(new BigDecimal("999.99"))
                .stock(INITIAL_STOCK)
                .build();

        productRepository.save(product)
                .onSuccess(saved -> {
                    UUID productId = saved.getId();
                    int initialVersion = saved.getVersion();

                    System.out.println("\n========================================");
                    System.out.println("Starting concurrency test");
                    System.out.println("Product ID: " + productId);
                    System.out.println("Initial Stock: " + INITIAL_STOCK);
                    System.out.println("Concurrent Requests: " + CONCURRENT_REQUESTS);
                    System.out.println("========================================\n");

                    // Launch concurrent requests
                    for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                        final int requestNum = i + 1;

                        vertx.executeBlocking(() -> {
                            try {
                                // Wait for all threads to be ready
                                startLatch.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            return null;
                        }).onComplete(ready -> {
                            // All threads start simultaneously
                            productRepository.decrementStock(productId, 1, initialVersion)
                                    .onSuccess(result -> {
                                        if (result.isPresent()) {
                                            int count = successCount.incrementAndGet();
                                            System.out.println("Request #" + requestNum + ": SUCCESS (total: " + count + ")");
                                        } else {
                                            int count = failureCount.incrementAndGet();
                                            System.out.println("Request #" + requestNum + ": CONFLICT - version mismatch (total failures: " + count + ")");
                                        }
                                        completionLatch.countDown();
                                    })
                                    .onFailure(err -> {
                                        failureCount.incrementAndGet();
                                        System.out.println("Request #" + requestNum + ": ERROR - " + err.getMessage());
                                        completionLatch.countDown();
                                    });
                        });
                    }

                    // Release all threads at once
                    startLatch.countDown();
                })
                .onFailure(ctx::failNow);

        // Wait for all requests to complete
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // Give a moment for final counts to settle
        Thread.sleep(100);

        // Verify results
        System.out.println("\n========================================");
        System.out.println("RESULTS");
        System.out.println("========================================");
        System.out.println("Successful decrements: " + successCount.get());
        System.out.println("Failed (conflict): " + failureCount.get());
        System.out.println("========================================\n");

        // Only 1 request should succeed because we only have 1 in stock
        // and all requests use the same initial version
        assertThat(successCount.get())
                .as("Only one request should succeed with optimistic locking")
                .isEqualTo(1);

        assertThat(failureCount.get())
                .as("All other requests should fail due to version mismatch")
                .isEqualTo(CONCURRENT_REQUESTS - 1);

        ctx.completeNow();
    }

    @Test
    @Order(5)
    @DisplayName("Multiple items - concurrent requests should not oversell")
    void concurrentRequestsShouldNotOversell(Vertx vertx, VertxTestContext ctx) throws InterruptedException {
        final int CONCURRENT_REQUESTS = 20;
        final int INITIAL_STOCK = 5;

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(CONCURRENT_REQUESTS);

        // Create product with stock = 5
        Product product = Product.builder()
                .name("Popular Item")
                .code("POPULAR-001")
                .price(new BigDecimal("49.99"))
                .stock(INITIAL_STOCK)
                .build();

        productRepository.save(product)
                .onSuccess(saved -> {
                    UUID productId = saved.getId();

                    System.out.println("\n========================================");
                    System.out.println("Starting oversell prevention test");
                    System.out.println("Product ID: " + productId);
                    System.out.println("Initial Stock: " + INITIAL_STOCK);
                    System.out.println("Concurrent Requests: " + CONCURRENT_REQUESTS);
                    System.out.println("========================================\n");

                    // Launch concurrent requests - each will fetch fresh data and retry
                    for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                        final int requestNum = i + 1;

                        vertx.executeBlocking(() -> {
                            try {
                                startLatch.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            return null;
                        }).onComplete(ready -> {
                            // Simulate real-world: fetch product, then try to decrement
                            productRepository.findById(productId)
                                    .compose(optProduct -> {
                                        if (optProduct.isEmpty()) {
                                            return Future.failedFuture(new RuntimeException("Product not found"));
                                        }
                                        Product p = optProduct.get();
                                        if (p.getStock() < 1) {
                                            return Future.failedFuture(new InsufficientStockException(productId, 1, 0));
                                        }
                                        return productRepository.decrementStock(productId, 1, p.getVersion());
                                    })
                                    .onSuccess(result -> {
                                        if (result != null && result.isPresent()) {
                                            int count = successCount.incrementAndGet();
                                            System.out.println("Request #" + requestNum + ": SUCCESS - stock now: " + result.get().getStock());
                                        } else {
                                            int count = failureCount.incrementAndGet();
                                            System.out.println("Request #" + requestNum + ": CONFLICT");
                                        }
                                        completionLatch.countDown();
                                    })
                                    .onFailure(err -> {
                                        failureCount.incrementAndGet();
                                        System.out.println("Request #" + requestNum + ": REJECTED - " + err.getMessage());
                                        completionLatch.countDown();
                                    });
                        });
                    }

                    startLatch.countDown();
                })
                .onFailure(ctx::failNow);

        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        Thread.sleep(100);

        // Verify final stock
        productRepository.findByCode("POPULAR-001")
                .onSuccess(result -> {
                    assertThat(result).isPresent();
                    Product finalProduct = result.get();

                    System.out.println("\n========================================");
                    System.out.println("FINAL RESULTS");
                    System.out.println("========================================");
                    System.out.println("Successful purchases: " + successCount.get());
                    System.out.println("Failed attempts: " + failureCount.get());
                    System.out.println("Final stock: " + finalProduct.getStock());
                    System.out.println("Final version: " + finalProduct.getVersion());
                    System.out.println("========================================\n");

                    // Stock should never go negative
                    assertThat(finalProduct.getStock())
                            .as("Stock should never go negative")
                            .isGreaterThanOrEqualTo(0);

                    // Success count should equal items sold
                    assertThat(successCount.get())
                            .as("Success count should match items sold")
                            .isEqualTo(INITIAL_STOCK - finalProduct.getStock());

                    // Version should increment with each successful decrement
                    assertThat(finalProduct.getVersion())
                            .as("Version should track successful updates")
                            .isEqualTo(1 + successCount.get());

                    ctx.completeNow();
                })
                .onFailure(ctx::failNow);
    }
}
