package br.com.labs.repository;

import br.com.labs.model.Product;
import io.vertx.core.Future;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    Future<Product> save(Product product);

    Future<Product> update(Product product);

    Future<Optional<Product>> findById(UUID id);

    Future<Optional<Product>> findByCode(String code);

    Future<List<Product>> findAll(int page, int size);

    Future<List<Product>> findByCategory(UUID categoryId, int page, int size);

    Future<Long> count();

    Future<Boolean> delete(UUID id);

    Future<Boolean> existsByCode(String code);

    /**
     * Decrements the stock of a product atomically using optimistic locking.
     * Uses version field to detect concurrent modifications.
     *
     * @param productId the product ID
     * @param quantity the quantity to decrement
     * @param expectedVersion the expected version for optimistic locking
     * @return Future with the updated Product if successful, or empty if version mismatch or insufficient stock
     */
    Future<Optional<Product>> decrementStock(UUID productId, int quantity, int expectedVersion);
}
