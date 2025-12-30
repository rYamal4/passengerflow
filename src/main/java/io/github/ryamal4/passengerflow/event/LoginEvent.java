package io.github.ryamal4.passengerflow.event;

import io.github.ryamal4.passengerflow.model.User;

import java.time.LocalDateTime;

public record LoginEvent(User user, LocalDateTime timestamp) {
}
