package org.pix.wallet.infrastructure.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.pix.wallet.infrastructure.persistence.repository.LedgerEntryJpaRepository;
import org.pix.wallet.infrastructure.persistence.repository.WalletJpaRepository;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LedgerEntryRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.4-alpine")
            .withReuse(false)
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/testdata");
    }

    @Autowired
    LedgerEntryJpaRepository ledgerJpa;

    @Autowired
    WalletJpaRepository walletJpa;

    @Test
    void appendAndIdempotencyExists() {
        // precisa wallet existir por FK
        UUID wid = UUID.fromString("7f9d34e0-8b2a-4d0d-aad1-12f3c9d5e6b2");

        var adapter = new LedgerEntryRepositoryAdapter(ledgerJpa, walletJpa);
        String key = "idem-1";
        adapter.deposit(wid.toString(), new BigDecimal("10.00"), key);
        ledgerJpa.flush();
        assertTrue(adapter.existsByIdempotencyKey(key));
        assertFalse(adapter.existsByIdempotencyKey("other"));
    }
}
