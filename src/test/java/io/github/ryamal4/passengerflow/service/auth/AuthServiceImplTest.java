package io.github.ryamal4.passengerflow.service.auth;

import io.github.ryamal4.passengerflow.dto.LoginRequest;
import io.github.ryamal4.passengerflow.enums.TokenType;
import io.github.ryamal4.passengerflow.exception.AppException;
import io.github.ryamal4.passengerflow.jwt.JwtTokenProviderImpl;
import io.github.ryamal4.passengerflow.model.Role;
import io.github.ryamal4.passengerflow.model.Token;
import io.github.ryamal4.passengerflow.model.User;
import io.github.ryamal4.passengerflow.repository.TokenRepository;
import io.github.ryamal4.passengerflow.repository.UserRepository;
import io.github.ryamal4.passengerflow.util.CookieUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password";
    private static final String ROLE_USER = "USER";

    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private JwtTokenProviderImpl tokenProvider;
    @Mock
    private CookieUtil cookieUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private Authentication authentication;
    @Mock
    private HttpCookie httpCookie;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private Token accessToken;
    private Token refreshToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenDurationMinute", 5L);
        ReflectionTestUtils.setField(authService, "accessTokenDurationSecond", 300L);
        ReflectionTestUtils.setField(authService, "refreshTokenDurationDay", 7L);
        ReflectionTestUtils.setField(authService, "refreshTokenDurationSecond", 604800L);

        Role testRole = Role.builder()
                .id(1L)
                .name(ROLE_USER)
                .permissions(Set.of())
                .build();

        testUser = User.builder()
                .id(1L)
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .role(testRole)
                .tokens(new HashSet<>())
                .build();

        accessToken = new Token(TokenType.ACCESS, "access-token-value",
                LocalDateTime.now().plusMinutes(5), false, testUser);
        refreshToken = new Token(TokenType.REFRESH, "refresh-token-value",
                LocalDateTime.now().plusDays(7), false, testUser);
    }

    @Test
    void testLoginSuccessWithNoExistingTokens() {
        var loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(tokenProvider.validateToken(null)).thenReturn(false);
        when(tokenProvider.generateAccessToken(anyMap(), anyLong(), any(ChronoUnit.class), eq(testUser)))
                .thenReturn(accessToken);
        when(tokenProvider.generateRefreshToken(anyLong(), any(ChronoUnit.class), eq(testUser)))
                .thenReturn(refreshToken);
        when(cookieUtil.createAccessTokenCookie(anyString(), anyLong())).thenReturn(httpCookie);
        when(cookieUtil.createRefreshTokenCookie(anyString(), anyLong())).thenReturn(httpCookie);

        var response = authService.login(loginRequest, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isLogged()).isTrue();
        assertThat(response.getBody().roles()).isEqualTo(ROLE_USER);
        verify(tokenRepository).saveAll(anyList());
    }

    @Test
    void testLoginUserNotFoundThrowsException() {
        var loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest, null, null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void testRefreshSuccessWithValidToken() {
        var validRefreshToken = "valid-refresh-token";
        when(tokenProvider.validateToken(validRefreshToken)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(validRefreshToken)).thenReturn(TEST_USERNAME);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateAccessToken(anyMap(), anyLong(), any(ChronoUnit.class), eq(testUser)))
                .thenReturn(accessToken);
        when(cookieUtil.createAccessTokenCookie(anyString(), anyLong())).thenReturn(httpCookie);

        var response = authService.refresh(validRefreshToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isLogged()).isTrue();
        verify(tokenProvider).generateAccessToken(anyMap(), anyLong(), any(ChronoUnit.class), eq(testUser));
    }

    @Test
    void testRefreshWithInvalidTokenThrowsException() {
        var invalidRefreshToken = "invalid-refresh-token";
        when(tokenProvider.validateToken(invalidRefreshToken)).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(invalidRefreshToken))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    void testRefreshUserNotFoundThrowsException() {
        var validRefreshToken = "valid-refresh-token";
        when(tokenProvider.validateToken(validRefreshToken)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(validRefreshToken)).thenReturn(TEST_USERNAME);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(validRefreshToken))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void testLogoutSuccess() {
        var validAccessToken = "valid-access-token";
        var validRefreshToken = "valid-refresh-token";
        when(tokenProvider.getUsernameFromToken(validAccessToken)).thenReturn(TEST_USERNAME);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(cookieUtil.deleteAccessTokenCookie()).thenReturn(httpCookie);
        when(cookieUtil.deleteRefreshTokenCookie()).thenReturn(httpCookie);

        var response = authService.logout(validAccessToken, validRefreshToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isLogged()).isFalse();
        assertThat(response.getBody().roles()).isNull();
        verify(cookieUtil).deleteAccessTokenCookie();
        verify(cookieUtil).deleteRefreshTokenCookie();
    }

    @Test
    void testLogoutUserNotFoundThrowsException() {
        var validAccessToken = "valid-access-token";
        when(tokenProvider.getUsernameFromToken(validAccessToken)).thenReturn(TEST_USERNAME);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout(validAccessToken, "refresh"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetUserLoggedInfoSuccess() {
        var securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(TEST_USERNAME);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        var result = authService.getUserLoggedInfo();

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo(TEST_USERNAME);
        assertThat(result.role()).isEqualTo(ROLE_USER);
    }

    @Test
    void testGetUserLoggedInfoUserNotFoundThrowsException() {
        var securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(TEST_USERNAME);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserLoggedInfo())
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void testLoginRevokesExistingTokens() {
        var existingToken = new Token(TokenType.ACCESS, "old-token",
                LocalDateTime.now().plusMinutes(5), false, testUser);
        testUser.setTokens(Set.of(existingToken));

        var loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(tokenProvider.validateToken(null)).thenReturn(false);
        when(tokenProvider.generateAccessToken(anyMap(), anyLong(), any(ChronoUnit.class), eq(testUser)))
                .thenReturn(accessToken);
        when(tokenProvider.generateRefreshToken(anyLong(), any(ChronoUnit.class), eq(testUser)))
                .thenReturn(refreshToken);
        when(cookieUtil.createAccessTokenCookie(anyString(), anyLong())).thenReturn(httpCookie);
        when(cookieUtil.createRefreshTokenCookie(anyString(), anyLong())).thenReturn(httpCookie);

        authService.login(loginRequest, null, null);

        verify(tokenRepository).save(argThat(Token::isDisabled));
    }

    @Test
    void testLoginWithValidRefreshTokenOnlyRegeneratesAccessToken() {
        var loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
        var existingRefreshToken = "existing-refresh-token";
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(tokenProvider.validateToken(null)).thenReturn(false);
        when(tokenProvider.validateToken(existingRefreshToken)).thenReturn(true);
        when(tokenProvider.generateAccessToken(anyMap(), anyLong(), any(ChronoUnit.class), eq(testUser)))
                .thenReturn(accessToken);
        when(cookieUtil.createAccessTokenCookie(anyString(), anyLong())).thenReturn(httpCookie);

        var response = authService.login(loginRequest, null, existingRefreshToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(tokenProvider).generateAccessToken(anyMap(), anyLong(), any(ChronoUnit.class), eq(testUser));
        verify(tokenProvider, never()).generateRefreshToken(anyLong(), any(ChronoUnit.class), any());
    }
}
