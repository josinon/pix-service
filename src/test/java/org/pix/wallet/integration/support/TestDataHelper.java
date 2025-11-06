package org.pix.wallet.integration.support;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small helper to reduce duplication in integration tests.
 * Uses very lightweight JSON extraction via regex consistent with existing tests.
 */
public class TestDataHelper {

    private final TestRestTemplate rest;
    private final int port;

    public TestDataHelper(TestRestTemplate rest, int port) {
        this.rest = rest;
        this.port = port;
    }

    private String url(String p) { return "http://localhost:" + port + p; }

    public String createWallet() {
        ResponseEntity<String> resp = rest.postForEntity(url("/wallets"), null, String.class);
        return extract(resp.getBody(), "id");
    }

    public void deposit(String walletId, BigDecimal amount) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Idempotency-Key", "dep-" + UUID.randomUUID());
        HttpEntity<String> req = new HttpEntity<>("{\"amount\":" + amount + "}", h);
        rest.postForEntity(url("/wallets/" + walletId + "/deposit"), req, String.class);
    }

    public String createRandomPixKey(String walletId) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>("{\"type\":\"RANDOM\"}", h);
        ResponseEntity<String> resp = rest.postForEntity(url("/wallets/" + walletId + "/pix-keys"), req, String.class);
        if (resp.getStatusCode().value() != 201) {
            throw new IllegalStateException("Pix key creation failed: status=" + resp.getStatusCode().value() + " body=" + resp.getBody());
        }
        String value = extract(resp.getBody(), "value");
        if (value == null) {
            throw new IllegalStateException("Could not extract pix key value from body=" + resp.getBody());
        }
        return value;
    }

    public String startPixTransfer(String fromWalletId, String toPixKey, BigDecimal amount, String idempotencyKey) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Idempotency-Key", idempotencyKey);
        String body = "{\"fromWalletId\":\"" + fromWalletId + "\",\"toPixKey\":\"" + toPixKey + "\",\"amount\":" + amount + "}";
        ResponseEntity<String> resp = rest.postForEntity(url("/pix/transfers"), new HttpEntity<>(body, h), String.class);
        if (resp.getStatusCode().value() != 201) {
            throw new IllegalStateException("Transfer creation failed: status=" + resp.getStatusCode().value() + " body=" + resp.getBody());
        }
        String id = extract(resp.getBody(), "endToEndId");
        if (id == null) {
            throw new IllegalStateException("endToEndId not found in response body=" + resp.getBody());
        }
        return id;
    }

    public ResponseEntity<String> startPixTransferRaw(String fromWalletId, String toPixKey, BigDecimal amount, String idempotencyKey) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) h.set("Idempotency-Key", idempotencyKey);
        String body = "{\"fromWalletId\":\"" + fromWalletId + "\",\"toPixKey\":\"" + toPixKey + "\",\"amount\":" + amount + "}";
        return rest.postForEntity(url("/pix/transfers"), new HttpEntity<>(body, h), String.class);
    }

    public ResponseEntity<String> sendWebhook(String endToEndId, String eventId, String eventType) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"endToEndId\":\"" + endToEndId + "\",\"eventId\":\"" + eventId + "\",\"eventType\":\"" + eventType + "\",\"occurredAt\":\"" + java.time.Instant.now().toString() + "\"}";
        return rest.postForEntity(url("/pix/webhook"), new HttpEntity<>(body, h), String.class);
    }

    public void confirmPixTransfer(String endToEndId) {
        String eventId = "evt-confirm-" + UUID.randomUUID();
        ResponseEntity<String> resp = sendWebhook(endToEndId, eventId, "CONFIRMED");
        if (resp.getStatusCode().value() != 200) {
            throw new IllegalStateException("Webhook confirmation failed: status=" + resp.getStatusCode().value());
        }
    }

    public void rejectPixTransfer(String endToEndId) {
        String eventId = "evt-reject-" + UUID.randomUUID();
        ResponseEntity<String> resp = sendWebhook(endToEndId, eventId, "REJECTED");
        if (resp.getStatusCode().value() != 200) {
            throw new IllegalStateException("Webhook rejection failed: status=" + resp.getStatusCode().value());
        }
    }

    public static String extract(String json, String field) {
        if (json == null) return null;
        // Robust JSON field capture (simple, not a full parser). Allows whitespace/newlines between tokens.
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
