package br.com.labs.service.impl;

import br.com.labs.dto.request.LoginRequest;
import br.com.labs.dto.request.RegisterRequest;
import br.com.labs.dto.response.TokenResponse;
import br.com.labs.exception.UnauthorizedException;
import br.com.labs.exception.ValidationException;
import br.com.labs.model.User;
import br.com.labs.repository.UserRepository;
import br.com.labs.security.JwtProvider;
import br.com.labs.security.PasswordEncoder;
import br.com.labs.service.AuthService;
import io.vertx.core.Future;

public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @Override
    public Future<User> register(RegisterRequest request) {
        // Validate request
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return Future.failedFuture(new ValidationException("Email is required"));
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return Future.failedFuture(new ValidationException("Password must be at least 6 characters"));
        }
        if (request.getName() == null || request.getName().isBlank()) {
            return Future.failedFuture(new ValidationException("Name is required"));
        }

        // Check if email already exists
        return userRepository.existsByEmail(request.getEmail())
                .compose(exists -> {
                    if (exists) {
                        return Future.failedFuture(new ValidationException("Email already registered"));
                    }

                    // Create user
                    User user = User.builder()
                            .email(request.getEmail())
                            .passwordHash(passwordEncoder.encode(request.getPassword()))
                            .name(request.getName())
                            .build();

                    return userRepository.save(user);
                });
    }

    @Override
    public Future<TokenResponse> login(LoginRequest request) {
        // Validate request
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return Future.failedFuture(new ValidationException("Email is required"));
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return Future.failedFuture(new ValidationException("Password is required"));
        }

        return userRepository.findByEmail(request.getEmail())
                .compose(optionalUser -> {
                    if (optionalUser.isEmpty()) {
                        return Future.failedFuture(new UnauthorizedException("Invalid credentials"));
                    }

                    User user = optionalUser.get();

                    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                        return Future.failedFuture(new UnauthorizedException("Invalid credentials"));
                    }

                    String token = jwtProvider.generateToken(user.getId(), user.getEmail());
                    TokenResponse response = new TokenResponse(token, 3600); // 1 hour

                    return Future.succeededFuture(response);
                });
    }
}
