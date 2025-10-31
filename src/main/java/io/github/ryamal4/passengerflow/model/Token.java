package io.github.ryamal4.passengerflow.model;

import io.github.ryamal4.passengerflow.enums.TokenType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "tokens")
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private TokenType type;
    private String value;
    private LocalDateTime expiryDate;
    private boolean disabled;
    @ManyToOne
    private User user;

    public Token(TokenType type, String value, LocalDateTime expiryDate, boolean disabled, User user) {
        this.disabled = disabled;
        this.expiryDate = expiryDate;
        this.type = type;
        this.user = user;
        this.value = value;
    }
}
