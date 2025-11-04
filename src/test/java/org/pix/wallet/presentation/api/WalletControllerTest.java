package org.pix.wallet.presentation.api;

import org.junit.jupiter.api.Test;
import org.pix.wallet.application.port.in.CreateWalletUseCase;
import org.pix.wallet.application.port.in.DepositFundsUseCase;
import org.pix.wallet.application.port.in.GetBalanceUseCase;
import org.pix.wallet.application.port.in.CreatePixKeyUseCase;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.domain.model.enums.WalletStatus;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean CreateWalletUseCase createWallet;
    @MockitoBean GetBalanceUseCase getBalance;
    @MockitoBean CreatePixKeyUseCase createPixKeyUseCase;
    @MockitoBean DepositFundsUseCase depositFunds;

    @Test
    void createWalletReturns201AndId() throws Exception {
        UUID id = UUID.randomUUID();
        when(createWallet.create()).thenReturn(Wallet.builder()
                .id(id)
                .status(WalletStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());

        mvc.perform(post("/wallets"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/wallets/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void createPixKey201() throws Exception {
        UUID wid = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();
        when(createPixKeyUseCase.execute(eq(wid), any()))
                .thenReturn(new CreatePixKeyUseCase.CreatePixKeyResult(keyId, "CPF", "12345678901", "ACTIVE"));

        mvc.perform(post("/wallets/" + wid + "/pix-keys")
                .contentType("application/json")
                .content("""
                  {"type":"CPF","value":"12345678901"}
                """))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").value(keyId.toString()))
           .andExpect(jsonPath("$.type").value("CPF"))
           .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
