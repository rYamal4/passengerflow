package io.github.ryamal4.passengerflow;

import org.testcontainers.containers.PostgreSQLContainer;

@SuppressWarnings("resource")
public class PostgresTestContainer {
    public static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    static {
        INSTANCE.start();
    }
}