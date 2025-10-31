package io.github.ryamal4.passengerflow.service.auth;

import io.github.ryamal4.passengerflow.dto.LoginRequest;
import io.github.ryamal4.passengerflow.dto.LoginResponse;
import io.github.ryamal4.passengerflow.dto.UserLoggedDto;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<LoginResponse> login(LoginRequest loginRequest, String accessToken, String refreshToken);

    ResponseEntity<LoginResponse> refresh(String refreshToken);

    ResponseEntity<LoginResponse> logout(String accessToken, String refreshToken);

    UserLoggedDto getUserLoggedInfo();
}