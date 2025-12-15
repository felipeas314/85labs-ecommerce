package br.com.labs.service;

import br.com.labs.dto.request.LoginRequest;
import br.com.labs.dto.request.RegisterRequest;
import br.com.labs.dto.response.TokenResponse;
import br.com.labs.model.User;
import io.vertx.core.Future;

public interface AuthService {

    Future<User> register(RegisterRequest request);

    Future<TokenResponse> login(LoginRequest request);
}
