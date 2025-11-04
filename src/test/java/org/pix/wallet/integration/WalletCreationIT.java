package org.pix.wallet.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.pix.wallet.config.IntegrationTest;
import org.pix.wallet.infrastructure.persistence.repository.WalletJpaRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@IntegrationTest
class WalletCreationIT {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    WalletJpaRepository repo;

    @Test
    void createsWalletAndPersists() {
        ResponseEntity<String> resp = rest.postForEntity("http://localhost:" + port + "/wallets", null, String.class);
        assertEquals(201, resp.getStatusCode().value());
        assertTrue(resp.getHeaders().getLocation() != null);

        // Extrair ID do JSON simples {"id":"..."}
        String body = resp.getBody();
        assertNotNull(body);
        String idStr = body.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        UUID id = UUID.fromString(idStr);

        assertTrue(repo.findById(id).isPresent());
        var entity = repo.findById(id).get();
        assertNotNull(entity.getCreatedAt());
        assertEquals("ACTIVE", entity.getStatus().name());
    }
}