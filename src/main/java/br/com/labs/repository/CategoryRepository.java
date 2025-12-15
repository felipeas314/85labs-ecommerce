package br.com.labs.repository;

import br.com.labs.model.Category;
import io.vertx.core.Future;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {

    Future<Category> save(Category category);

    Future<Optional<Category>> findById(UUID id);

    Future<List<Category>> findAll();

    Future<Boolean> existsById(UUID id);
}
