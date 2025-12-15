package br.com.labs.service.impl;

import br.com.labs.dto.request.CreateCategoryRequest;
import br.com.labs.exception.NotFoundException;
import br.com.labs.exception.ValidationException;
import br.com.labs.model.Category;
import br.com.labs.repository.CategoryRepository;
import br.com.labs.service.CategoryService;
import io.vertx.core.Future;

import java.util.List;
import java.util.UUID;

public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Future<Category> create(CreateCategoryRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            return Future.failedFuture(new ValidationException("Category name is required"));
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        return categoryRepository.save(category);
    }

    @Override
    public Future<Category> findById(UUID id) {
        return categoryRepository.findById(id)
                .compose(optional -> {
                    if (optional.isEmpty()) {
                        return Future.failedFuture(new NotFoundException("Category", id));
                    }
                    return Future.succeededFuture(optional.get());
                });
    }

    @Override
    public Future<List<Category>> findAll() {
        return categoryRepository.findAll();
    }
}
