package io.github.ryamal4.passengerflow.dto;

public record LoginResponse(
        boolean isLogged,
        String roles
) {
}
