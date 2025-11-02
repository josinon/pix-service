package org.pix.wallet.integration;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthEndpointIT {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    void liveness() {
        ResponseEntity<String> r = rest.getForEntity("http://localhost:" + port + "/actuator/health/liveness", String.class);
        assert r.getStatusCode().is2xxSuccessful();
    }
}