package org.pix.wallet.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.pix.wallet.infrastructure.persistence.repository.WalletJpaRepository;
import org.pix.wallet.config.IntegrationTest;
import org.pix.wallet.infrastructure.persistence.repository.PixKeyJpaRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@IntegrationTest
class WalletAndPixKeyIT {

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @Autowired WalletJpaRepository walletRepo;
    @Autowired PixKeyJpaRepository pixRepo;

    private String url(String p) { return "http://localhost:" + port + p; }

    @Test
    void createWalletAndPixKeyFlow() {
        var walletResp = rest.postForEntity(url("/wallets"), null, String.class);
        assertEquals(201, walletResp.getStatusCode().value());
        String walletId = walletResp.getBody().replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        assertTrue(walletRepo.findById(UUID.fromString(walletId)).isPresent());

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        var pixResp = rest.postForEntity(url("/wallets/" + walletId + "/pix-keys"),
                new HttpEntity<>("{\"type\":\"RANDOM\"}", h), String.class);
        assertEquals(201, pixResp.getStatusCode().value());
        String keyValue = pixResp.getBody().replaceAll(".*\"value\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        assertFalse(keyValue.isBlank());
    }

    @Test
    void invalidTypeReturns400() {
        var walletResp = rest.postForEntity(url("/wallets"), null, String.class);
        String walletId = walletResp.getBody().replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        var r = rest.postForEntity(url("/wallets/" + walletId + "/pix-keys"),
                new HttpEntity<>("{\"type\":\"XXX\",\"value\":\"abc\"}", h), String.class);
        assertEquals(400, r.getStatusCode().value());
    }
}
