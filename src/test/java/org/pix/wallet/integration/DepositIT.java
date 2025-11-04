package org.pix.wallet.integration;

import org.junit.jupiter.api.Test;
import org.pix.wallet.config.IntegrationTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

@IntegrationTest
class DepositIT {

    @LocalServerPort
    int port;

    @org.springframework.beans.factory.annotation.Autowired
    TestRestTemplate rest;

    private String url(String p) { return "http://localhost:" + port + p; }

    @Test
    void depositFlow() {
        var walletResp = rest.postForEntity(url("/wallets"), null, String.class);
        assertEquals(201, walletResp.getStatusCode().value());
        String walletId = walletResp.getBody().replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        var idempotencyKey = UUID.randomUUID().toString();
        h.set("Idempotency-Key", idempotencyKey);
        var r1 = rest.postForEntity(url("/wallets/" + walletId + "/deposit"),
                new HttpEntity<>("{\"amount\":100}", h), String.class);
        assertEquals(200, r1.getStatusCode().value());
    }

    @Test
    void depositIdempotentReplay() {
        var walletResp = rest.postForEntity(url("/wallets"), null, String.class);
        String walletId = walletResp.getBody().replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Idempotency-Key", "dep-replay");
        rest.postForEntity(url("/wallets/" + walletId + "/deposit"),
                new HttpEntity<>("{\"amount\":50}", h), String.class);
        var r2 = rest.postForEntity(url("/wallets/" + walletId + "/deposit"),
                new HttpEntity<>("{\"amount\":50}", h), String.class);
        assertEquals(200, r2.getStatusCode().value());
    }

    @Test
    void depositInvalidAmount() {
        var walletResp = rest.postForEntity(url("/wallets"), null, String.class);
        String walletId = walletResp.getBody().replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Idempotency-Key", "dep-bad");
        var r = rest.postForEntity(url("/wallets/" + walletId + "/deposit"),
                new HttpEntity<>("{\"amount\":0}", h), String.class);
        assertEquals(400, r.getStatusCode().value());
    }
}
