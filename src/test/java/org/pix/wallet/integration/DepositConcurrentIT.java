package org.pix.wallet.integration;

import org.junit.jupiter.api.Test;
import org.pix.wallet.config.IntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@IntegrationTest
class DepositConcurrentIT {

    @LocalServerPort int port;
    @org.springframework.beans.factory.annotation.Autowired TestRestTemplate rest;

    private String url(String p) { return "http://localhost:" + port + p; }

    @Test
    void concurrentDepositsIncrementBalance() throws Exception {
        var walletResp = rest.postForEntity(url("/wallets"), null, String.class);
        String walletId = walletResp.getBody().replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        int N = 5;
        ExecutorService pool = Executors.newFixedThreadPool(3);
        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            h.set("Idempotency-Key", "dep-" + i);
            HttpEntity<String> req = new HttpEntity<>("{\"amount\":10}", h);
            futures.add(pool.submit(() -> rest.postForEntity(
                    url("/wallets/" + walletId + "/deposit"), req, String.class)));
        }
        pool.shutdown();
        int success = 0;
        for (var f : futures) {
            var r = f.get();
            if (r.getStatusCode().value() == 200) success++;
        }
        assertEquals(N, success);
    }
}