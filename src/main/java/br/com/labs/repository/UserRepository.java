package br.com.labs.repository;

import br.com.labs.model.User;
import io.vertx.core.Future;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Future<User> save(User user);

    Future<Optional<User>> findById(UUID id);

    Future<Optional<User>> findByEmail(String email);

    Future<Boolean> existsByEmail(String email);
}
