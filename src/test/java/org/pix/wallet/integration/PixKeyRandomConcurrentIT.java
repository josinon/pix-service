package org.pix.wallet.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PixKeyRandomConcurrentIT {

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;

    @Test
    void randomKeysAreUniqueInParallel() {
        // cria wallet
        var walletResp = rest.postForEntity("http://localhost:" + port + "/wallets", null, String.class);
        String walletId = walletResp.getBody().replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        int N = 8;
        ExecutorService pool = Executors.newFixedThreadPool(4);
        Set<String> values = ConcurrentHashMap.newKeySet();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < N; i++) {
            futures.add(pool.submit(() -> {
              HttpHeaders headers = new HttpHeaders();
              headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
              HttpEntity<String> req = new HttpEntity<>("{\"type\":\"RANDOM\"}", headers);

                var r = rest.postForEntity("http://localhost:" + port + "/wallets/" + walletId + "/pix-keys",
                        req, String.class);
                assertEquals(201, r.getStatusCode().value());
                String val = r.getBody().replaceAll(".*\"value\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                values.add(val);
            }));
        }
        futures.forEach(f -> { try { f.get(); } catch (Exception e) { fail(e); }});
        pool.shutdown();
        assertEquals(N, values.size());
    }
}