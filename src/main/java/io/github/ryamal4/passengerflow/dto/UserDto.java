package io.github.ryamal4.passengerflow.dto;

import java.io.Serializable;
import java.util.Set;

public record UserDto(
        Long id,
        String username,
        String password,
        String role,
        Set<String> permissions
) implements Serializable {
}