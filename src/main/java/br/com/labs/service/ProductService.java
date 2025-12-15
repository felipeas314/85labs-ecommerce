package br.com.labs.service;

import br.com.labs.dto.request.CreateProductRequest;
import br.com.labs.dto.request.UpdateProductRequest;
import br.com.labs.dto.response.PageResponse;
import br.com.labs.model.Product;
import io.vertx.core.Future;

import java.util.UUID;

public interface ProductService {

    Future<Product> create(CreateProductRequest request);

    Future<Product> update(UUID id, UpdateProductRequest request);

    Future<Product> findById(UUID id);

    Future<PageResponse<Product>> findAll(int page, int size);

    Future<Boolean> delete(UUID id);
}
