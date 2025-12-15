package br.com.labs.service.impl;

import br.com.labs.dto.request.CreateOrderRequest;
import br.com.labs.dto.response.PageResponse;
import br.com.labs.exception.InsufficientStockException;
import br.com.labs.exception.NotFoundException;
import br.com.labs.exception.ValidationException;
import br.com.labs.model.Order;
import br.com.labs.model.OrderItem;
import br.com.labs.model.Product;
import br.com.labs.repository.OrderRepository;
import br.com.labs.repository.ProductRepository;
import br.com.labs.service.OrderService;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderServiceImpl(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Future<Order> create(UUID userId, CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return Future.failedFuture(new ValidationException("Order must have at least one item"));
        }

        // Validate all items and calculate total
        List<Future<ProductWithQuantity>> productFutures = new ArrayList<>();

        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            if (item.getQuantity() <= 0) {
                return Future.failedFuture(new ValidationException("Item quantity must be greater than 0"));
            }

            Future<ProductWithQuantity> productFuture = productRepository.findById(item.getProductId())
                    .compose(optional -> {
                        if (optional.isEmpty()) {
                            return Future.failedFuture(new NotFoundException("Product", item.getProductId()));
                        }
                        return Future.succeededFuture(new ProductWithQuantity(optional.get(), item.getQuantity()));
                    });

            productFutures.add(productFuture);
        }

        return CompositeFuture.all(new ArrayList<>(productFutures))
                .compose(cf -> {
                    List<ProductWithQuantity> productsWithQty = new ArrayList<>();
                    BigDecimal total = BigDecimal.ZERO;

                    for (int i = 0; i < cf.size(); i++) {
                        ProductWithQuantity pwq = cf.resultAt(i);
                        productsWithQty.add(pwq);
                        total = total.add(pwq.product.getPrice().multiply(BigDecimal.valueOf(pwq.quantity)));
                    }

                    // Validate stock availability before creating order
                    for (ProductWithQuantity pwq : productsWithQty) {
                        if (pwq.product.getStock() < pwq.quantity) {
                            return Future.failedFuture(new InsufficientStockException(
                                    pwq.product.getId(),
                                    pwq.quantity,
                                    pwq.product.getStock()));
                        }
                    }

                    final BigDecimal finalTotal = total;
                    final List<ProductWithQuantity> finalProducts = productsWithQty;

                    // Decrement stock for all products with optimistic locking
                    return decrementStockForAllProducts(finalProducts)
                            .compose(updatedProducts -> {
                                // Create order after stock is reserved
                                Order order = Order.builder()
                                        .userId(userId)
                                        .status(Order.OrderStatus.PENDING)
                                        .total(finalTotal)
                                        .build();

                                return orderRepository.save(order)
                                        .compose(savedOrder -> {
                                            // Save order items
                                            List<Future<OrderItem>> itemFutures = new ArrayList<>();

                                            for (ProductWithQuantity pwq : updatedProducts) {
                                                OrderItem orderItem = OrderItem.builder()
                                                        .orderId(savedOrder.getId())
                                                        .productId(pwq.product.getId())
                                                        .quantity(pwq.quantity)
                                                        .unitPrice(pwq.product.getPrice())
                                                        .build();

                                                itemFutures.add(orderRepository.saveItem(orderItem));
                                            }

                                            return CompositeFuture.all(new ArrayList<>(itemFutures))
                                                    .map(itemsCf -> {
                                                        List<OrderItem> items = new ArrayList<>();
                                                        for (int i = 0; i < itemsCf.size(); i++) {
                                                            items.add(itemsCf.resultAt(i));
                                                        }
                                                        savedOrder.setItems(items);
                                                        return savedOrder;
                                                    });
                                        });
                            });
                });
    }

    /**
     * Decrements stock for all products in the order using optimistic locking.
     * Processes each product sequentially to avoid partial updates on failure.
     */
    private Future<List<ProductWithQuantity>> decrementStockForAllProducts(List<ProductWithQuantity> products) {
        return decrementStockSequentially(products, 0, new ArrayList<>());
    }

    private Future<List<ProductWithQuantity>> decrementStockSequentially(
            List<ProductWithQuantity> products,
            int index,
            List<ProductWithQuantity> updatedProducts) {

        if (index >= products.size()) {
            return Future.succeededFuture(updatedProducts);
        }

        ProductWithQuantity pwq = products.get(index);
        return decrementStockWithRetry(pwq.product.getId(), pwq.quantity, pwq.product.getVersion(), 0)
                .compose(updatedProduct -> {
                    updatedProducts.add(new ProductWithQuantity(updatedProduct, pwq.quantity));
                    return decrementStockSequentially(products, index + 1, updatedProducts);
                });
    }

    /**
     * Attempts to decrement stock with retry on optimistic locking failure.
     * On conflict, re-fetches the product and tries again up to MAX_RETRY_ATTEMPTS times.
     */
    private Future<Product> decrementStockWithRetry(UUID productId, int quantity, int expectedVersion, int attempt) {
        if (attempt >= MAX_RETRY_ATTEMPTS) {
            logger.warn("Max retry attempts ({}) reached for product {}", MAX_RETRY_ATTEMPTS, productId);
            return Future.failedFuture(new InsufficientStockException(productId));
        }

        return productRepository.decrementStock(productId, quantity, expectedVersion)
                .compose(optional -> {
                    if (optional.isPresent()) {
                        logger.debug("Stock decremented for product {} (attempt {})", productId, attempt + 1);
                        return Future.succeededFuture(optional.get());
                    }

                    // Optimistic locking failed - retry with fresh data
                    logger.info("Optimistic lock conflict for product {}, retrying (attempt {})",
                            productId, attempt + 1);

                    return productRepository.findById(productId)
                            .compose(freshProduct -> {
                                if (freshProduct.isEmpty()) {
                                    return Future.failedFuture(new NotFoundException("Product", productId));
                                }

                                Product product = freshProduct.get();

                                // Check if there's enough stock after refresh
                                if (product.getStock() < quantity) {
                                    return Future.failedFuture(new InsufficientStockException(
                                            productId, quantity, product.getStock()));
                                }

                                // Retry with new version
                                return decrementStockWithRetry(productId, quantity, product.getVersion(), attempt + 1);
                            });
                });
    }

    @Override
    public Future<Order> findById(UUID id, UUID userId) {
        return orderRepository.findById(id)
                .compose(optional -> {
                    if (optional.isEmpty()) {
                        return Future.failedFuture(new NotFoundException("Order", id));
                    }

                    Order order = optional.get();

                    // Check if order belongs to user
                    if (!order.getUserId().equals(userId)) {
                        return Future.failedFuture(new NotFoundException("Order", id));
                    }

                    return Future.succeededFuture(order);
                });
    }

    @Override
    public Future<PageResponse<Order>> findByUserId(UUID userId, int page, int size) {
        return orderRepository.countByUserId(userId)
                .compose(total -> orderRepository.findByUserId(userId, page, size)
                        .map(orders -> new PageResponse<>(orders, page, size, total)));
    }

    private record ProductWithQuantity(Product product, int quantity) {}
}
