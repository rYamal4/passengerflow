package io.github.ryamal4.passengerflow.jwt;

import io.github.ryamal4.passengerflow.model.Token;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.Map;

public interface JwtTokenProvider {
    Token generateAccessToken(Map<String, Object> extraClaims, long duration, TemporalUnit durationType, UserDetails user);

    Token generateRefreshToken(long duration, TemporalUnit durationType, UserDetails user);

    boolean validateToken(String tokenValue);

    String getUsernameFromToken(String tokenValue);

    LocalDateTime getExpiryDateFromToken(String tokenValue);
}
