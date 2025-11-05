package org.pix.wallet.presentation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.pix.wallet.application.port.in.ProcessPixTransferUseCase;
import org.pix.wallet.application.port.in.ProcessPixWebhookUseCase;
import org.pix.wallet.presentation.dto.PixTransferRequest;
import org.pix.wallet.presentation.dto.PixWebhookRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PixController.class)
@DisplayName("PixController - Validation Tests")
class PixControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockBean
        private ProcessPixTransferUseCase processPixTransferUseCase;

        @MockBean
        private ProcessPixWebhookUseCase processPixWebhookUseCase;

    @Test
    @DisplayName("Should return 400 when Idempotency-Key header is missing on PIX transfer")
    void shouldReturn400WhenIdempotencyKeyMissingOnPixTransfer() throws Exception {
        PixTransferRequest request = new PixTransferRequest(
                UUID.randomUUID(),
                "12345678901",
                BigDecimal.valueOf(100)
        );

        mockMvc.perform(post("/pix/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Idempotency-Key")));
    }

    @Test
    @DisplayName("Should return 400 when PIX transfer amount is null")
    void shouldReturn400WhenPixTransferAmountIsNull() throws Exception {
        PixTransferRequest request = new PixTransferRequest(
                UUID.randomUUID(),
                "12345678901",
                null
        );

        mockMvc.perform(post("/pix/transfers")
                        .header("Idempotency-Key", "key-transfer-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("amount")));
    }

    @Test
    @DisplayName("Should return 400 when PIX transfer amount is zero")
    void shouldReturn400WhenPixTransferAmountIsZero() throws Exception {
        PixTransferRequest request = new PixTransferRequest(
                UUID.randomUUID(),
                "12345678901",
                BigDecimal.ZERO
        );

        mockMvc.perform(post("/pix/transfers")
                        .header("Idempotency-Key", "key-transfer-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("amount")));
    }

    @Test
    @DisplayName("Should return 400 when PIX transfer amount is negative")
    void shouldReturn400WhenPixTransferAmountIsNegative() throws Exception {
        PixTransferRequest request = new PixTransferRequest(
                UUID.randomUUID(),
                "12345678901",
                BigDecimal.valueOf(-50)
        );

        mockMvc.perform(post("/pix/transfers")
                        .header("Idempotency-Key", "key-transfer-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("amount")));
    }

    @Test
    @DisplayName("Should return 400 when PIX transfer destinationPixKey is null")
    void shouldReturn400WhenPixTransferDestinationPixKeyIsNull() throws Exception {
        PixTransferRequest request = new PixTransferRequest(
                UUID.randomUUID(),
                null,
                BigDecimal.valueOf(100)
        );

        mockMvc.perform(post("/pix/transfers")
                        .header("Idempotency-Key", "key-transfer-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("PIX key")));
    }

    @Test
    @DisplayName("Should return 400 when PIX transfer destinationPixKey is blank")
    void shouldReturn400WhenPixTransferDestinationPixKeyIsBlank() throws Exception {
        PixTransferRequest request = new PixTransferRequest(
                UUID.randomUUID(),
                "   ",
                BigDecimal.valueOf(100)
        );

        mockMvc.perform(post("/pix/transfers")
                        .header("Idempotency-Key", "key-transfer-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("PIX key")));
    }

    @Test
    @DisplayName("Should return 400 when webhook endToEndId is null")
    void shouldReturn400WhenWebhookEndToEndIdIsNull() throws Exception {
        PixWebhookRequest request = new PixWebhookRequest(
                null,
                "evt-123",
                "CONFIRMED",
                Instant.now()
        );

        mockMvc.perform(post("/pix/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("End to end")));
    }

    @Test
    @DisplayName("Should return 400 when webhook eventId is null")
    void shouldReturn400WhenWebhookEventIdIsNull() throws Exception {
        PixWebhookRequest request = new PixWebhookRequest(
                "E12345678901234567890123456789012",
                null,
                "CONFIRMED",
                Instant.now()
        );

        mockMvc.perform(post("/pix/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("event")));
    }

    @Test
    @DisplayName("Should return 400 when webhook eventType is null")
    void shouldReturn400WhenWebhookEventTypeIsNull() throws Exception {
        PixWebhookRequest request = new PixWebhookRequest(
                "E12345678901234567890123456789012",
                "evt-123",
                null,
                Instant.now()
        );

        mockMvc.perform(post("/pix/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("event")));
    }

    @Test
    @DisplayName("Should return 400 when webhook occurredAt is null")
    void shouldReturn400WhenWebhookOccurredAtIsNull() throws Exception {
        PixWebhookRequest request = new PixWebhookRequest(
                "E12345678901234567890123456789012",
                "evt-123",
                "CONFIRMED",
                null
        );

        mockMvc.perform(post("/pix/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("occurred")));
    }
}

