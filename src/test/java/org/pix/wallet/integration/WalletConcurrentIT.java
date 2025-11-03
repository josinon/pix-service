package org.pix.wallet.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WalletConcurrentIT {

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;

    @Test
    void parallelCreatesProduceDistinctIds() throws Exception {
        int n = 10;
        ExecutorService pool = Executors.newFixedThreadPool(5);
        Set<String> ids = ConcurrentHashMap.newKeySet();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(pool.submit(() -> {
                var r = rest.postForEntity("http://localhost:" + port + "/wallets", null, String.class);
                assertEquals(201, r.getStatusCode().value());
                String body = r.getBody();
                String id = body.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                ids.add(id);
            }));
        }
        for (var f : futures) f.get();
        pool.shutdown();

        assertEquals(n, ids.size());
    }
}