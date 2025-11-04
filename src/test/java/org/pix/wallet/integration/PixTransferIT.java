package org.pix.wallet.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pix.wallet.config.IntegrationTest;
import org.pix.wallet.integration.support.TestDataHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.pix.wallet.application.port.out.TransferRepositoryPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@IntegrationTest
class PixTransferIT {

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @Autowired TransferRepositoryPort transferRepositoryPort;
    TestDataHelper helper;

    @BeforeEach
    void setup() { helper = new TestDataHelper(rest, port); }

    @Test
    void validation_missingHeader() {
        String w1 = helper.createWallet();
        String w2 = helper.createWallet();
        helper.deposit(w1, BigDecimal.valueOf(200));
        String pixKey = helper.createRandomPixKey(w2);
        ResponseEntity<String> resp = helper.startPixTransferRaw(w1, pixKey, BigDecimal.valueOf(10), null);
        // When the required header is absent Spring returns 400 (MissingRequestHeaderException)
        assertEquals(400, resp.getStatusCode().value(), "Expected 400 Bad Request for missing Idempotency-Key header");
    }

    @Test
    void validation_invalidAmount() {
        String w1 = helper.createWallet();
        String w2 = helper.createWallet();
        helper.deposit(w1, BigDecimal.valueOf(50));
        String pixKey = helper.createRandomPixKey(w2);
        ResponseEntity<String> resp = helper.startPixTransferRaw(w1, pixKey, BigDecimal.ZERO, "idem-zero");
        assertTrue(resp.getStatusCode().value() >= 400 && resp.getStatusCode().value() < 500);
    }



}
