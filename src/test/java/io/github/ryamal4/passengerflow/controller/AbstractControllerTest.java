package io.github.ryamal4.passengerflow.controller;

import io.github.ryamal4.passengerflow.jwt.JwtAuthEntryPoint;
import io.github.ryamal4.passengerflow.jwt.JwtAuthFilter;
import io.github.ryamal4.passengerflow.jwt.JwtTokenProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class AbstractControllerTest {

    @MockitoBean
    protected JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    protected JwtAuthEntryPoint jwtAuthEntryPoint;

    @MockitoBean
    protected JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    protected UserDetailsService userDetailsService;
}
