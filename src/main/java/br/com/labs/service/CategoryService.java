package br.com.labs.service;

import br.com.labs.dto.request.CreateCategoryRequest;
import br.com.labs.model.Category;
import io.vertx.core.Future;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    Future<Category> create(CreateCategoryRequest request);

    Future<Category> findById(UUID id);

    Future<List<Category>> findAll();
}
