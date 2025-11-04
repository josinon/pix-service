package org.pix.wallet.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration
public class TestContainersConfig {

    @Bean
    @ServiceConnection  // ← Injeta automaticamente datasource
    PostgreSQLContainer<?> postgres() {  // ← NÃO static = novo por contexto
        return new PostgreSQLContainer<>("postgres:16.4-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(false);
    }

}
