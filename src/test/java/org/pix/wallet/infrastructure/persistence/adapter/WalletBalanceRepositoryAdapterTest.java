package org.pix.wallet.infrastructure.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.pix.wallet.infrastructure.persistence.repository.WalletBalanceJpaRepository;
import org.pix.wallet.infrastructure.persistence.repository.WalletJpaRepository;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WalletBalanceRepositoryAdapterTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.4-alpine")
            .withReuse(false)
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        postgres.start();
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/testdata");
    }

    @Autowired
    WalletBalanceJpaRepository balanceJpa;

    @Autowired
    WalletJpaRepository walletJpa;

    @Test
    void incrementCreatesAndUpdates() {
        var adapter = new WalletBalanceRepositoryAdapter(balanceJpa, walletJpa);
        UUID wid = UUID.fromString("a3c8b6d2-5f2e-4c1a-9e47-0b6d0c9e4f71");

        var b1 = adapter.incrementBalance(wid, new BigDecimal("50"));
        var b2 = adapter.incrementBalance(wid, new BigDecimal("25"));
        assertEquals(new BigDecimal("50.00"), b1);
        assertEquals(new BigDecimal("75.00"), b2);
    }
}