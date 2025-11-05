package org.pix.wallet.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pix.wallet.application.port.out.TransferRepositoryPort;
import org.pix.wallet.config.IntegrationTest;
import org.pix.wallet.integration.support.TestDataHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@IntegrationTest
class PixWebhookIT {

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @Autowired TransferRepositoryPort transferRepositoryPort;
    TestDataHelper helper;

    @BeforeEach
    void setup() { helper = new TestDataHelper(rest, port); }

    private String createPendingTransfer() {
        String from = helper.createWallet();
        String toWallet = helper.createWallet();
        helper.deposit(from, BigDecimal.valueOf(400));
        String pixKey = helper.createRandomPixKey(toWallet);
        String endToEnd = helper.startPixTransfer(from, pixKey, BigDecimal.valueOf(75), "idem-" + UUID.randomUUID());
        assertNotNull(endToEnd);
        var transfer = transferRepositoryPort.findByEndToEndId(endToEnd).orElseThrow();
        assertEquals("PENDING", transfer.status());
        return endToEnd;
    }

    @Test
    void confirmWebhookUpdatesStatus() {
        String endToEnd = createPendingTransfer();
        String eventId = "evt-confirm-" + UUID.randomUUID();
        ResponseEntity<String> resp = helper.sendWebhook(endToEnd, eventId, "CONFIRMED");
        assertEquals(200, resp.getStatusCode().value());
        var updated = transferRepositoryPort.findByEndToEndId(endToEnd).orElseThrow();
        assertEquals("CONFIRMED", updated.status());
    }

    @Test
    void duplicateEventIdIsIgnored() {
        String endToEnd = createPendingTransfer();
        String eventId = "evt-dupe-" + UUID.randomUUID();
        ResponseEntity<String> first = helper.sendWebhook(endToEnd, eventId, "CONFIRMED");
        ResponseEntity<String> second = helper.sendWebhook(endToEnd, eventId, "CONFIRMED");
        assertEquals(200, first.getStatusCode().value());
        assertEquals(200, second.getStatusCode().value());
        var tr = transferRepositoryPort.findByEndToEndId(endToEnd).orElseThrow();
        assertEquals("CONFIRMED", tr.status());
    }

    @Test
    void rejectThenConfirmTransitions() {
        String endToEnd = createPendingTransfer();
        String rejectEventId = "evt-reject-" + UUID.randomUUID();
        ResponseEntity<String> rejectResp = helper.sendWebhook(endToEnd, rejectEventId, "REJECTED");
        assertEquals(200, rejectResp.getStatusCode().value());
        var rejected = transferRepositoryPort.findByEndToEndId(endToEnd).orElseThrow();
        assertEquals("REJECTED", rejected.status());

        String confirmEventId = "evt-confirm2-" + UUID.randomUUID();
        ResponseEntity<String> confirmResp = helper.sendWebhook(endToEnd, confirmEventId, "CONFIRMED");
        // After business rule change, REJECTED -> CONFIRMED is invalid and should return 409
        assertEquals(409, confirmResp.getStatusCode().value());
        var stillRejected = transferRepositoryPort.findByEndToEndId(endToEnd).orElseThrow();
        assertEquals("REJECTED", stillRejected.status());
    }

    @Test
    void concurrency_duplicateEventProcessedOnce() {
        String endToEnd = createPendingTransfer();
        String eventId = "evt-concurrent-" + UUID.randomUUID();
        int threads = 5;
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> helper.sendWebhook(endToEnd, eventId, "CONFIRMED")));
        }
        futures.forEach(f -> { try { f.get(); } catch (Exception e) { fail(e); } });
        pool.shutdown();
        var tr = transferRepositoryPort.findByEndToEndId(endToEnd).orElseThrow();
        assertEquals("CONFIRMED", tr.status());
    }
}
