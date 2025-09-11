package io.github.ryamal4.passengerflow;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractTestContainerTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestContainer.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTestContainer.INSTANCE::getUsername);
        registry.add("spring.datasource.password", PostgresTestContainer.INSTANCE::getPassword);
    }
}