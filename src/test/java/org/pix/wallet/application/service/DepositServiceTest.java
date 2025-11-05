package org.pix.wallet.application.service;

import org.junit.jupiter.api.Test;
import org.pix.wallet.application.port.in.DepositUseCase;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.domain.model.enums.WalletStatus;
import org.pix.wallet.infrastructure.observability.MetricsService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DepositServiceTest {

    WalletRepositoryPort walletPort = mock(WalletRepositoryPort.class);
    LedgerEntryRepositoryPort ledgerPort = mock(LedgerEntryRepositoryPort.class);
    MetricsService metricsService = mock(MetricsService.class);
    WalletOperationValidator validator = new WalletOperationValidator(walletPort);
    DepositService service = new DepositService(validator, ledgerPort, metricsService);

    UUID wid = UUID.randomUUID();

    private Wallet wallet() {
        return Wallet.builder().id(wid).status(WalletStatus.ACTIVE).createdAt(Instant.now()).build();
    }

    @Test
    void depositSuccess() {
        var idempotenceKey = "k1";
        when(walletPort.findById(wid)).thenReturn(Optional.of(wallet()));
        when(ledgerPort.existsByIdempotencyKey(idempotenceKey)).thenReturn(false);
        when(ledgerPort.getCurrentBalance(wid.toString())).thenReturn(Optional.of(BigDecimal.ZERO));
        when(ledgerPort.deposit(eq(wid.toString()), eq(new BigDecimal("25.00")), eq(idempotenceKey))).thenReturn(UUID.randomUUID().toString());

        var r = service.execute(new DepositUseCase.Command(wid, new BigDecimal("25.00"), idempotenceKey));
        assertEquals(wid, r.walletId());
        assertEquals(idempotenceKey, r.idempotencyKey());
    }

    @Test
    void depositIdempotentReplay() {
        var idempotenceKey = "k1";
        when(walletPort.findById(wid)).thenReturn(Optional.of(wallet()));
        when(ledgerPort.existsByIdempotencyKey(idempotenceKey)).thenReturn(true);
        when(ledgerPort.getCurrentBalance(wid.toString())).thenReturn(Optional.of(new BigDecimal("100.00")));

        var r = service.execute(new DepositUseCase.Command(wid, new BigDecimal("25.00"), idempotenceKey));
        assertEquals(wid, r.walletId());
        assertEquals(idempotenceKey, r.idempotencyKey());
    }

    @Test
    void walletNotFound() {
        when(walletPort.findById(wid)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new DepositUseCase.Command(wid, new BigDecimal("10"), "k1")));
    }

    @Test
    void amountInvalidZero() {
        when(walletPort.findById(wid)).thenReturn(Optional.of(wallet()));
        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new DepositUseCase.Command(wid, new BigDecimal("0"), "k1")));
    }

    @Test
    void missingIdempotencyKey() {
        when(walletPort.findById(wid)).thenReturn(Optional.of(wallet()));
        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new DepositUseCase.Command(wid, new BigDecimal("10"), "")));
    }
}
