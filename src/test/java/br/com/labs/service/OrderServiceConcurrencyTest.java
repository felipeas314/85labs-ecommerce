package br.com.labs.service;

import br.com.labs.dto.request.CreateOrderRequest;
import br.com.labs.exception.InsufficientStockException;
import br.com.labs.model.Order;
import br.com.labs.model.OrderItem;
import br.com.labs.model.Product;
import br.com.labs.repository.OrderRepository;
import br.com.labs.repository.ProductRepository;
import br.com.labs.service.impl.OrderServiceImpl;
import io.vertx.core.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests demonstrating optimistic locking behavior in OrderService.
 * Uses mocks to simulate concurrent access scenarios without a database.
 */
@ExtendWith(MockitoExtension.class)
public class OrderServiceConcurrencyTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    private OrderService orderService;

    private UUID productId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, productRepository);
        productId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should succeed when stock is available and version matches")
    void shouldSucceedWhenStockAvailable() throws Exception {
        // Arrange
        Product product = createProduct(5, 1);

        when(productRepository.findById(productId))
                .thenReturn(Future.succeededFuture(Optional.of(product)));

        when(productRepository.decrementStock(eq(productId), eq(1), eq(1)))
                .thenReturn(Future.succeededFuture(Optional.of(createProduct(4, 2))));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> {
                    Order order = inv.getArgument(0);
                    order.setId(UUID.randomUUID());
                    return Future.succeededFuture(order);
                });

        when(orderRepository.saveItem(any(OrderItem.class)))
                .thenAnswer(inv -> {
                    OrderItem item = inv.getArgument(0);
                    item.setId(UUID.randomUUID());
                    return Future.succeededFuture(item);
                });

        CreateOrderRequest request = createOrderRequest(1);

        // Act
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger(0);

        orderService.create(userId, request)
                .onSuccess(order -> {
                    success.incrementAndGet();
                    latch.countDown();
                })
                .onFailure(err -> latch.countDown());

        latch.await(5, TimeUnit.SECONDS);

        // Assert
        assertThat(success.get()).isEqualTo(1);
        verify(productRepository).decrementStock(productId, 1, 1);
    }

    @Test
    @DisplayName("Should fail when stock is insufficient")
    void shouldFailWhenStockInsufficient() throws Exception {
        // Arrange
        Product product = createProduct(1, 1); // Only 1 in stock

        when(productRepository.findById(productId))
                .thenReturn(Future.succeededFuture(Optional.of(product)));

        CreateOrderRequest request = createOrderRequest(5); // Trying to buy 5

        // Act
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger(0);
        String[] errorMessage = new String[1];

        orderService.create(userId, request)
                .onSuccess(order -> latch.countDown())
                .onFailure(err -> {
                    failures.incrementAndGet();
                    errorMessage[0] = err.getMessage();
                    latch.countDown();
                });

        latch.await(5, TimeUnit.SECONDS);

        // Assert
        assertThat(failures.get()).isEqualTo(1);
        assertThat(errorMessage[0]).contains("Insufficient stock");
    }

    @Test
    @DisplayName("Should retry on optimistic lock conflict and eventually succeed")
    void shouldRetryOnOptimisticLockConflict() throws Exception {
        // Arrange
        Product productV1 = createProduct(5, 1);
        Product productV2 = createProduct(4, 2); // Someone else already bought one
        Product productV3 = createProduct(3, 3); // After our successful purchase

        // First call returns product with version 1
        when(productRepository.findById(productId))
                .thenReturn(Future.succeededFuture(Optional.of(productV1)))
                .thenReturn(Future.succeededFuture(Optional.of(productV2)));

        // First decrement fails (version mismatch), second succeeds
        when(productRepository.decrementStock(eq(productId), eq(1), eq(1)))
                .thenReturn(Future.succeededFuture(Optional.empty())); // Conflict!

        when(productRepository.decrementStock(eq(productId), eq(1), eq(2)))
                .thenReturn(Future.succeededFuture(Optional.of(productV3))); // Success!

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> {
                    Order order = inv.getArgument(0);
                    order.setId(UUID.randomUUID());
                    return Future.succeededFuture(order);
                });

        when(orderRepository.saveItem(any(OrderItem.class)))
                .thenAnswer(inv -> {
                    OrderItem item = inv.getArgument(0);
                    item.setId(UUID.randomUUID());
                    return Future.succeededFuture(item);
                });

        CreateOrderRequest request = createOrderRequest(1);

        // Act
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger(0);

        orderService.create(userId, request)
                .onSuccess(order -> {
                    success.incrementAndGet();
                    latch.countDown();
                })
                .onFailure(err -> {
                    System.out.println("Failed: " + err.getMessage());
                    latch.countDown();
                });

        latch.await(5, TimeUnit.SECONDS);

        // Assert
        assertThat(success.get()).isEqualTo(1);

        // Verify retry happened
        verify(productRepository).decrementStock(productId, 1, 1); // First attempt
        verify(productRepository).decrementStock(productId, 1, 2); // Retry with new version
    }

    @Test
    @DisplayName("Should fail after max retries when stock depleted during retries")
    void shouldFailAfterMaxRetries() throws Exception {
        // Arrange
        Product productV1 = createProduct(1, 1);
        Product productV2 = createProduct(0, 2); // Stock depleted!

        when(productRepository.findById(productId))
                .thenReturn(Future.succeededFuture(Optional.of(productV1)))
                .thenReturn(Future.succeededFuture(Optional.of(productV2)));

        // First decrement fails due to conflict
        when(productRepository.decrementStock(eq(productId), eq(1), eq(1)))
                .thenReturn(Future.succeededFuture(Optional.empty()));

        CreateOrderRequest request = createOrderRequest(1);

        // Act
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger(0);

        orderService.create(userId, request)
                .onSuccess(order -> latch.countDown())
                .onFailure(err -> {
                    failures.incrementAndGet();
                    assertThat(err).isInstanceOf(InsufficientStockException.class);
                    latch.countDown();
                });

        latch.await(5, TimeUnit.SECONDS);

        // Assert
        assertThat(failures.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Simulated concurrent requests - demonstrates race condition handling")
    void simulatedConcurrentRequests() throws Exception {
        // This test simulates what happens with concurrent requests:
        // - 5 requests arrive "simultaneously"
        // - Only 1 item in stock
        // - Only 1 should succeed, 4 should fail

        final int CONCURRENT_REQUESTS = 5;
        final int INITIAL_STOCK = 1;

        AtomicInteger currentStock = new AtomicInteger(INITIAL_STOCK);
        AtomicInteger currentVersion = new AtomicInteger(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Mock findById to return current stock state
        when(productRepository.findById(productId))
                .thenAnswer(inv -> Future.succeededFuture(
                        Optional.of(createProduct(currentStock.get(), currentVersion.get()))));

        // Mock decrementStock to simulate optimistic locking
        when(productRepository.decrementStock(eq(productId), eq(1), anyInt()))
                .thenAnswer((Answer<Future<Optional<Product>>>) inv -> {
                    int expectedVersion = inv.getArgument(2);

                    synchronized (currentVersion) {
                        if (currentVersion.get() != expectedVersion) {
                            // Version mismatch - conflict!
                            return Future.succeededFuture(Optional.empty());
                        }

                        if (currentStock.get() < 1) {
                            // Insufficient stock
                            return Future.succeededFuture(Optional.empty());
                        }

                        // Success! Decrement stock and increment version
                        int newStock = currentStock.decrementAndGet();
                        int newVersion = currentVersion.incrementAndGet();

                        return Future.succeededFuture(Optional.of(createProduct(newStock, newVersion)));
                    }
                });

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> {
                    Order order = inv.getArgument(0);
                    order.setId(UUID.randomUUID());
                    return Future.succeededFuture(order);
                });

        when(orderRepository.saveItem(any(OrderItem.class)))
                .thenAnswer(inv -> {
                    OrderItem item = inv.getArgument(0);
                    item.setId(UUID.randomUUID());
                    return Future.succeededFuture(item);
                });

        // Execute concurrent requests
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(CONCURRENT_REQUESTS);

        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            final int requestNum = i + 1;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    CreateOrderRequest request = createOrderRequest(1);
                    CountDownLatch requestLatch = new CountDownLatch(1);

                    orderService.create(userId, request)
                            .onSuccess(order -> {
                                successCount.incrementAndGet();
                                System.out.println("Request #" + requestNum + ": SUCCESS");
                                requestLatch.countDown();
                            })
                            .onFailure(err -> {
                                failureCount.incrementAndGet();
                                System.out.println("Request #" + requestNum + ": FAILED - " + err.getClass().getSimpleName());
                                requestLatch.countDown();
                            });

                    requestLatch.await(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all requests simultaneously
        startLatch.countDown();

        // Wait for completion
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert results
        System.out.println("\n========================================");
        System.out.println("CONCURRENCY TEST RESULTS");
        System.out.println("========================================");
        System.out.println("Initial Stock: " + INITIAL_STOCK);
        System.out.println("Concurrent Requests: " + CONCURRENT_REQUESTS);
        System.out.println("Successful Orders: " + successCount.get());
        System.out.println("Failed Attempts: " + failureCount.get());
        System.out.println("Final Stock: " + currentStock.get());
        System.out.println("Final Version: " + currentVersion.get());
        System.out.println("========================================\n");

        assertThat(completed).isTrue();
        assertThat(currentStock.get()).as("Stock should not go negative").isGreaterThanOrEqualTo(0);
        assertThat(successCount.get() + currentStock.get())
                .as("Success count + remaining stock should equal initial stock")
                .isEqualTo(INITIAL_STOCK);
    }

    // Helper methods
    private Product createProduct(int stock, int version) {
        return Product.builder()
                .id(productId)
                .name("Test Product")
                .code("TEST-001")
                .price(new BigDecimal("99.99"))
                .stock(stock)
                .version(version)
                .build();
    }

    private CreateOrderRequest createOrderRequest(int quantity) {
        CreateOrderRequest request = new CreateOrderRequest();
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        request.setItems(List.of(item));
        return request;
    }
}
