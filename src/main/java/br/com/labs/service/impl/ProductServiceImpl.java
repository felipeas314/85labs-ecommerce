package br.com.labs.service.impl;

import br.com.labs.dto.request.CreateProductRequest;
import br.com.labs.dto.request.UpdateProductRequest;
import br.com.labs.dto.response.PageResponse;
import br.com.labs.exception.NotFoundException;
import br.com.labs.exception.ValidationException;
import br.com.labs.model.Product;
import br.com.labs.repository.CategoryRepository;
import br.com.labs.repository.ProductRepository;
import br.com.labs.service.ProductService;
import io.vertx.core.Future;

import java.util.UUID;

public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Future<Product> create(CreateProductRequest request) {
        // Validate request
        if (request.getName() == null || request.getName().isBlank()) {
            return Future.failedFuture(new ValidationException("Product name is required"));
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            return Future.failedFuture(new ValidationException("Product code is required"));
        }
        if (request.getPrice() == null || request.getPrice().doubleValue() <= 0) {
            return Future.failedFuture(new ValidationException("Product price must be greater than 0"));
        }

        // Check if code already exists
        return productRepository.existsByCode(request.getCode())
                .compose(exists -> {
                    if (exists) {
                        return Future.failedFuture(new ValidationException("Product code already exists"));
                    }

                    // Validate category if provided
                    if (request.getCategoryId() != null) {
                        return categoryRepository.existsById(request.getCategoryId())
                                .compose(categoryExists -> {
                                    if (!categoryExists) {
                                        return Future.failedFuture(new NotFoundException("Category", request.getCategoryId()));
                                    }
                                    return saveProduct(request);
                                });
                    }

                    return saveProduct(request);
                });
    }

    private Future<Product> saveProduct(CreateProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .code(request.getCode())
                .price(request.getPrice())
                .stock(request.getStock() != null ? request.getStock() : 0)
                .categoryId(request.getCategoryId())
                .build();

        return productRepository.save(product);
    }

    @Override
    public Future<Product> update(UUID id, UpdateProductRequest request) {
        return productRepository.findById(id)
                .compose(optional -> {
                    if (optional.isEmpty()) {
                        return Future.failedFuture(new NotFoundException("Product", id));
                    }

                    Product product = optional.get();

                    if (request.getName() != null && !request.getName().isBlank()) {
                        product.setName(request.getName());
                    }
                    if (request.getDescription() != null) {
                        product.setDescription(request.getDescription());
                    }
                    if (request.getPrice() != null) {
                        if (request.getPrice().doubleValue() <= 0) {
                            return Future.failedFuture(new ValidationException("Price must be greater than 0"));
                        }
                        product.setPrice(request.getPrice());
                    }
                    if (request.getStock() != null) {
                        if (request.getStock() < 0) {
                            return Future.failedFuture(new ValidationException("Stock cannot be negative"));
                        }
                        product.setStock(request.getStock());
                    }
                    if (request.getCategoryId() != null) {
                        return categoryRepository.existsById(request.getCategoryId())
                                .compose(exists -> {
                                    if (!exists) {
                                        return Future.failedFuture(new NotFoundException("Category", request.getCategoryId()));
                                    }
                                    product.setCategoryId(request.getCategoryId());
                                    return productRepository.update(product);
                                });
                    }

                    return productRepository.update(product);
                });
    }

    @Override
    public Future<Product> findById(UUID id) {
        return productRepository.findById(id)
                .compose(optional -> {
                    if (optional.isEmpty()) {
                        return Future.failedFuture(new NotFoundException("Product", id));
                    }
                    return Future.succeededFuture(optional.get());
                });
    }

    @Override
    public Future<PageResponse<Product>> findAll(int page, int size) {
        return productRepository.count()
                .compose(total -> productRepository.findAll(page, size)
                        .map(products -> new PageResponse<>(products, page, size, total)));
    }

    @Override
    public Future<Boolean> delete(UUID id) {
        return productRepository.findById(id)
                .compose(optional -> {
                    if (optional.isEmpty()) {
                        return Future.failedFuture(new NotFoundException("Product", id));
                    }
                    return productRepository.delete(id);
                });
    }
}
