package org.pix.wallet.presentation.api;

import org.junit.jupiter.api.Test;
import org.pix.wallet.application.port.in.DepositFundsUseCase;
import org.pix.wallet.application.port.in.DepositFundsUseCase.Command;
import org.pix.wallet.application.port.in.DepositFundsUseCase.Result;
import org.pix.wallet.application.port.in.CreateWalletUseCase;
import org.pix.wallet.application.port.in.GetBalanceUseCase;
import org.pix.wallet.application.port.in.CreatePixKeyUseCase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
class WalletControllerDepositTest {

    @Autowired MockMvc mvc;

    @MockitoBean CreateWalletUseCase createWallet;
    @MockitoBean GetBalanceUseCase getBalance;
    @MockitoBean CreatePixKeyUseCase createPixKeyUseCase;
    @MockitoBean DepositFundsUseCase depositFunds;

    @Test
    void depositReturnsOk() throws Exception {
        UUID wid = UUID.randomUUID();
        when(depositFunds.execute(any(Command.class)))
                .thenReturn(new Result(wid, new BigDecimal("0"), new BigDecimal("50"), new BigDecimal("50")));

        mvc.perform(post("/wallets/" + wid + "/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "k-test")
                .content("{\"amount\":50}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.walletId").value(wid.toString()))
           .andExpect(jsonPath("$.amount").value(50));
    }

    @Test
    void missingIdempotencyKeyReturns400() throws Exception {
        UUID wid = UUID.randomUUID();
        mvc.perform(post("/wallets/" + wid + "/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":50}"))
           .andExpect(status().isBadRequest());
    }
}