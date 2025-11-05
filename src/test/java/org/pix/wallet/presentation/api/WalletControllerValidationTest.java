package org.pix.wallet.presentation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.pix.wallet.application.port.in.CreatePixKeyUseCase;
import org.pix.wallet.application.port.in.CreateWalletUseCase;
import org.pix.wallet.application.port.in.DepositUseCase;
import org.pix.wallet.application.port.in.GetBalanceUseCase;
import org.pix.wallet.application.port.in.WithdrawUseCase;
import org.pix.wallet.presentation.dto.DepositRequest;
import org.pix.wallet.presentation.dto.WithdrawRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@DisplayName("WalletController - Validation Tests")
class WalletControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateWalletUseCase createWalletUseCase;

    @MockBean
    private CreatePixKeyUseCase createPixKeyUseCase;

    @MockBean
    private DepositUseCase depositUseCase;

    @MockBean
    private WithdrawUseCase withdrawUseCase;

    @MockBean
    private GetBalanceUseCase getBalanceUseCase;

    @Test
    @DisplayName("Should return 400 when Idempotency-Key header is missing on deposit")
    void shouldReturn400WhenIdempotencyKeyMissingOnDeposit() throws Exception {
        UUID walletId = UUID.randomUUID();
        DepositRequest request = new DepositRequest(BigDecimal.valueOf(100));

        mockMvc.perform(post("/wallets/{walletId}/deposit", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Idempotency-Key")));
    }

    @Test
    @DisplayName("Should return 400 when Idempotency-Key header is missing on withdraw")
    void shouldReturn400WhenIdempotencyKeyMissingOnWithdraw() throws Exception {
        UUID walletId = UUID.randomUUID();
        WithdrawRequest request = new WithdrawRequest(BigDecimal.valueOf(50));

        mockMvc.perform(post("/wallets/{walletId}/withdraw", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Idempotency-Key")));
    }

    @Test
    @DisplayName("Should return 400 when deposit amount is null")
    void shouldReturn400WhenDepositAmountIsNull() throws Exception {
        UUID walletId = UUID.randomUUID();
        DepositRequest request = new DepositRequest(null);

        mockMvc.perform(post("/wallets/{walletId}/deposit", walletId)
                        .header("Idempotency-Key", "key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("amount")));
    }

    @Test
    @DisplayName("Should return 400 when deposit amount is zero")
    void shouldReturn400WhenDepositAmountIsZero() throws Exception {
        UUID walletId = UUID.randomUUID();
        DepositRequest request = new DepositRequest(BigDecimal.ZERO);

        mockMvc.perform(post("/wallets/{walletId}/deposit", walletId)
                        .header("Idempotency-Key", "key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("amount")));
    }

    @Test
    @DisplayName("Should return 400 when deposit amount is negative")
    void shouldReturn400WhenDepositAmountIsNegative() throws Exception {
        UUID walletId = UUID.randomUUID();
        DepositRequest request = new DepositRequest(BigDecimal.valueOf(-50));

        mockMvc.perform(post("/wallets/{walletId}/deposit", walletId)
                        .header("Idempotency-Key", "key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("amount")));
    }

    @Test
    @DisplayName("Should return 400 when withdraw amount is null")
    void shouldReturn400WhenWithdrawAmountIsNull() throws Exception {
        UUID walletId = UUID.randomUUID();
        WithdrawRequest request = new WithdrawRequest(null);

        mockMvc.perform(post("/wallets/{walletId}/withdraw", walletId)
                        .header("Idempotency-Key", "key-456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("amount")));
    }

    @Test
    @DisplayName("Should return 400 when withdraw amount is zero")
    void shouldReturn400WhenWithdrawAmountIsZero() throws Exception {
        UUID walletId = UUID.randomUUID();
        WithdrawRequest request = new WithdrawRequest(BigDecimal.ZERO);

        mockMvc.perform(post("/wallets/{walletId}/withdraw", walletId)
                        .header("Idempotency-Key", "key-456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("amount")));
    }

    @Test
    @DisplayName("Should return 400 when withdraw amount is negative")
    void shouldReturn400WhenWithdrawAmountIsNegative() throws Exception {
        UUID walletId = UUID.randomUUID();
        WithdrawRequest request = new WithdrawRequest(BigDecimal.valueOf(-100));

        mockMvc.perform(post("/wallets/{walletId}/withdraw", walletId)
                        .header("Idempotency-Key", "key-456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("amount")));
    }
}
