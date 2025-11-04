package org.pix.wallet.application.service;

import org.junit.jupiter.api.Test;
import org.pix.wallet.application.port.in.DepositFundsUseCase;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.application.port.out.WalletBalanceRepositoryPort;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.domain.model.enums.WalletStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DepositServiceConcurrencyTest {

    WalletRepositoryPort walletPort = mock(WalletRepositoryPort.class);
    WalletBalanceRepositoryPort balancePort = mock(WalletBalanceRepositoryPort.class);
    LedgerEntryRepositoryPort ledgerPort = mock(LedgerEntryRepositoryPort.class);
    DepositService service = new DepositService(walletPort, balancePort, ledgerPort);

    @Test
    void concurrentDifferentIdempotencyKeys() throws Exception {
        UUID wid = UUID.randomUUID();
        when(walletPort.findById(wid)).thenReturn(Optional.of(Wallet.builder().id(wid).status(WalletStatus.ACTIVE).createdAt(Instant.now()).build()));
        when(balancePort.findCurrentBalance(wid))
                .thenReturn(Optional.of(BigDecimal.ZERO)) // primeira leitura
                .thenReturn(Optional.of(new BigDecimal("10"))) // segunda
                .thenReturn(Optional.of(new BigDecimal("20"))); // terceira
        when(ledgerPort.existsByIdempotencyKey(any())).thenReturn(false);
        when(ledgerPort.appendDeposit(eq(wid), any(), any())).thenReturn(UUID.randomUUID());
        when(balancePort.incrementBalance(eq(wid), eq(new BigDecimal("10"))))
                .thenReturn(new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30"));

        ExecutorService pool = Executors.newFixedThreadPool(3);
        Callable<BigDecimal> task = () -> service.execute(new DepositFundsUseCase.Command(
                wid, new BigDecimal("10"), UUID.randomUUID().toString())).newBalance();
        var futures = pool.invokeAll(
                java.util.List.of(task, task, task));
        pool.shutdown();
        var balances = new java.util.HashSet<BigDecimal>();
        for (var f : futures) balances.add(f.get());
        assertTrue(balances.contains(new BigDecimal("10")));
        assertTrue(balances.contains(new BigDecimal("20")));
        assertTrue(balances.contains(new BigDecimal("30")));
    }
}