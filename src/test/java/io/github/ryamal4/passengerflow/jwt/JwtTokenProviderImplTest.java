package io.github.ryamal4.passengerflow.jwt;

import io.github.ryamal4.passengerflow.enums.TokenType;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderImplTest {

    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(
            Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded()
    );
    private static final String TEST_USERNAME = "testuser";

    @InjectMocks
    private JwtTokenProviderImpl jwtTokenProvider;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        userDetails = new User(TEST_USERNAME, "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void testGenerateAccessTokenCreatesValidToken() {
        var extraClaims = Map.<String, Object>of("role", "ROLE_USER");

        var token = jwtTokenProvider.generateAccessToken(extraClaims, 5, ChronoUnit.MINUTES, userDetails);

        assertThat(token).isNotNull();
        assertThat(token.getType()).isEqualTo(TokenType.ACCESS);
        assertThat(token.getValue()).isNotBlank();
        assertThat(token.isDisabled()).isFalse();
        assertThat(token.getExpiryDate()).isAfter(LocalDateTime.now());
    }

    @Test
    void testGenerateAccessTokenSetsCorrectExpiry() {
        var extraClaims = Map.<String, Object>of("role", "ROLE_USER");
        var beforeGeneration = LocalDateTime.now();

        var token = jwtTokenProvider.generateAccessToken(extraClaims, 10, ChronoUnit.MINUTES, userDetails);

        var expectedExpiryMin = beforeGeneration.plusMinutes(10);
        var expectedExpiryMax = beforeGeneration.plusMinutes(11);
        assertThat(token.getExpiryDate()).isBetween(expectedExpiryMin, expectedExpiryMax);
    }

    @Test
    void testGenerateRefreshTokenCreatesValidToken() {
        var token = jwtTokenProvider.generateRefreshToken(7, ChronoUnit.DAYS, userDetails);

        assertThat(token).isNotNull();
        assertThat(token.getType()).isEqualTo(TokenType.REFRESH);
        assertThat(token.getValue()).isNotBlank();
        assertThat(token.isDisabled()).isFalse();
        assertThat(token.getExpiryDate()).isAfter(LocalDateTime.now());
    }

    @Test
    void testGenerateRefreshTokenSetsCorrectExpiry() {
        var beforeGeneration = LocalDateTime.now();

        var token = jwtTokenProvider.generateRefreshToken(7, ChronoUnit.DAYS, userDetails);

        var expectedExpiryMin = beforeGeneration.plusDays(7);
        var expectedExpiryMax = beforeGeneration.plusDays(7).plusMinutes(1);
        assertThat(token.getExpiryDate()).isBetween(expectedExpiryMin, expectedExpiryMax);
    }

    @Test
    void testValidateTokenReturnsTrueForValidToken() {
        var token = jwtTokenProvider.generateAccessToken(
                Map.of(), 5, ChronoUnit.MINUTES, userDetails);

        var isValid = jwtTokenProvider.validateToken(token.getValue());

        assertThat(isValid).isTrue();
    }

    @Test
    void testValidateTokenReturnsFalseForExpiredToken() {
        var expiredToken = createExpiredToken();

        var isValid = jwtTokenProvider.validateToken(expiredToken);

        assertThat(isValid).isFalse();
    }

    @Test
    void testValidateTokenReturnsFalseForNullToken() {
        var isValid = jwtTokenProvider.validateToken(null);

        assertThat(isValid).isFalse();
    }

    @Test
    void testValidateTokenReturnsFalseForMalformedToken() {
        var isValid = jwtTokenProvider.validateToken("invalid.token.value");

        assertThat(isValid).isFalse();
    }

    @Test
    void testValidateTokenReturnsFalseForTokenWithWrongSignature() {
        var differentSecret = Base64.getEncoder().encodeToString(
                Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded()
        );
        var tokenWithDifferentKey = Jwts.builder()
                .setSubject(TEST_USERNAME)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 300000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(differentSecret)),
                        SignatureAlgorithm.HS256)
                .compact();

        var isValid = jwtTokenProvider.validateToken(tokenWithDifferentKey);

        assertThat(isValid).isFalse();
    }

    @Test
    void testGetUsernameFromTokenReturnsCorrectUsername() {
        var token = jwtTokenProvider.generateAccessToken(
                Map.of(), 5, ChronoUnit.MINUTES, userDetails);

        var username = jwtTokenProvider.getUsernameFromToken(token.getValue());

        assertThat(username).isEqualTo(TEST_USERNAME);
    }

    @Test
    void testGetExpiryDateFromTokenReturnsCorrectDate() {
        var beforeGeneration = LocalDateTime.now();
        var token = jwtTokenProvider.generateAccessToken(
                Map.of(), 5, ChronoUnit.MINUTES, userDetails);

        var expiryDate = jwtTokenProvider.getExpiryDateFromToken(token.getValue());

        var expectedMin = beforeGeneration.plusMinutes(4).plusSeconds(58);
        var expectedMax = beforeGeneration.plusMinutes(6);
        assertThat(expiryDate).isBetween(expectedMin, expectedMax);
    }

    @Test
    void testGenerateAccessTokenIncludesExtraClaims() {
        var extraClaims = Map.<String, Object>of("role", "ROLE_ADMIN", "customClaim", "customValue");

        var token = jwtTokenProvider.generateAccessToken(extraClaims, 5, ChronoUnit.MINUTES, userDetails);

        assertThat(jwtTokenProvider.validateToken(token.getValue())).isTrue();
    }

    private String createExpiredToken() {
        var key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        return Jwts.builder()
                .setSubject(TEST_USERNAME)
                .setIssuedAt(new Date(System.currentTimeMillis() - 600000))
                .setExpiration(new Date(System.currentTimeMillis() - 300000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
