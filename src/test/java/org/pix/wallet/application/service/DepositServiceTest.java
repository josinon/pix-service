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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DepositServiceTest {

    WalletRepositoryPort walletPort = mock(WalletRepositoryPort.class);
    WalletBalanceRepositoryPort balancePort = mock(WalletBalanceRepositoryPort.class);
    LedgerEntryRepositoryPort ledgerPort = mock(LedgerEntryRepositoryPort.class);
    DepositService service = new DepositService(walletPort, balancePort, ledgerPort);

    UUID wid = UUID.randomUUID();

    private Wallet wallet() {
        return Wallet.builder().id(wid).status(WalletStatus.ACTIVE).createdAt(Instant.now()).build();
    }

    @Test
    void depositSuccess() {
        when(walletPort.findById(wid)).thenReturn(Optional.of(wallet()));
        when(ledgerPort.existsByIdempotencyKey("k1")).thenReturn(false);
        when(balancePort.findCurrentBalance(wid)).thenReturn(Optional.of(BigDecimal.ZERO));
        when(ledgerPort.appendDeposit(eq(wid), any(), eq("k1"))).thenReturn(UUID.randomUUID());
        when(balancePort.incrementBalance(wid, new BigDecimal("25.00"))).thenReturn(new BigDecimal("25.00"));

        var r = service.execute(new DepositFundsUseCase.Command(wid, new BigDecimal("25.00"), "k1"));
        assertEquals(new BigDecimal("0"), r.previousBalance());
        assertEquals(new BigDecimal("25.00"), r.amount());
        assertEquals(new BigDecimal("25.00"), r.newBalance());
    }

    @Test
    void depositIdempotentReplay() {
        when(walletPort.findById(wid)).thenReturn(Optional.of(wallet()));
        when(ledgerPort.existsByIdempotencyKey("k1")).thenReturn(true);
        when(balancePort.findCurrentBalance(wid)).thenReturn(Optional.of(new BigDecimal("100.00")));

        var r = service.execute(new DepositFundsUseCase.Command(wid, new BigDecimal("25.00"), "k1"));
        assertEquals(new BigDecimal("100.00"), r.previousBalance());
        assertEquals(BigDecimal.ZERO, r.amount());
        assertEquals(new BigDecimal("100.00"), r.newBalance());
    }

    @Test
    void walletNotFound() {
        when(walletPort.findById(wid)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new DepositFundsUseCase.Command(wid, new BigDecimal("10"), "k1")));
    }

    @Test
    void amountInvalidZero() {
        when(walletPort.findById(wid)).thenReturn(Optional.of(wallet()));
        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new DepositFundsUseCase.Command(wid, new BigDecimal("0"), "k1")));
    }

    @Test
    void missingIdempotencyKey() {
        when(walletPort.findById(wid)).thenReturn(Optional.of(wallet()));
        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new DepositFundsUseCase.Command(wid, new BigDecimal("10"), "")));
    }
}
