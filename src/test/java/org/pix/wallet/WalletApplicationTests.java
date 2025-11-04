package org.pix.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles(value = "test")
@Testcontainers
class WalletApplicationTests {

	@Container
	static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16.4")
                .withDatabaseName("pixwallet-test")
                .withUsername("pix")
                .withPassword("pixpass");

@DynamicPropertySource
static void registerProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);

    // Garantir que Flyway use o mesmo container
    registry.add("spring.flyway.url", postgres::getJdbcUrl);
    registry.add("spring.flyway.user", postgres::getUsername);
    registry.add("spring.flyway.password", postgres::getPassword);
    registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/testdata");
}

@Autowired
DataSource dataSource;

@Test
void contextLoads() {
    // Se o contexto não subir, o teste falha automaticamente
}

@Test
void dataSourceNotNull() {
    assertNotNull(dataSource);
}

@Test
void flywayMigrationsApplied() throws SQLException {
    try (Connection c = dataSource.getConnection();
         PreparedStatement ps = c.prepareStatement("select count(*) from flyway_schema_history");
         ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next());
        int count = rs.getInt(1);
        assertTrue(count >= 1, "Esperado pelo menos 1 migração aplicada, obtido: " + count);
    }
}

@Test
void simpleSelectWorks() throws SQLException {
    try (Connection c = dataSource.getConnection();
         PreparedStatement ps = c.prepareStatement("select 1");
         ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
    }
}
}
