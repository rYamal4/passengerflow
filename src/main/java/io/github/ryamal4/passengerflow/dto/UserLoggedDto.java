package io.github.ryamal4.passengerflow.dto;

import java.util.Set;

public record UserLoggedDto(String username, String role, Set<String> permissions) {
}