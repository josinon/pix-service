package org.pix.wallet.presentation.api;

import org.junit.jupiter.api.Test;
import org.pix.wallet.application.port.in.DepositUseCase;
import org.pix.wallet.application.port.in.DepositUseCase.Command;
import org.pix.wallet.application.port.in.DepositUseCase.Result;
import org.pix.wallet.application.port.in.CreateWalletUseCase;
import org.pix.wallet.application.port.in.GetBalanceUseCase;
import org.pix.wallet.application.port.in.WithdrawUseCase;
import org.pix.wallet.application.port.in.CreatePixKeyUseCase;
import org.pix.wallet.application.service.WalletOperationValidator;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
class WalletControllerDepositTest {

    @Autowired MockMvc mvc;

    @MockBean CreateWalletUseCase createWallet;
    @MockBean GetBalanceUseCase getBalance;
    @MockBean CreatePixKeyUseCase createPixKeyUseCase;
    @MockBean DepositUseCase depositFunds;
    @MockBean WithdrawUseCase withdrawFunds;
    @MockBean WalletOperationValidator walletOperationValidator;

    @Test
    void depositReturnsOk() throws Exception {
        UUID wid = UUID.randomUUID();
        when(depositFunds.execute(any(Command.class)))
                .thenReturn(new Result(wid, UUID.randomUUID().toString()));

        mvc.perform(post("/wallets/" + wid + "/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "k-test")
                .content("{\"amount\":50}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.walletId").value(wid.toString()));   
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