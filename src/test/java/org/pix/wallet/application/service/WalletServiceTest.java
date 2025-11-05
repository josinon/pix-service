package org.pix.wallet.application.service;

import org.junit.jupiter.api.Test;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.domain.model.enums.WalletStatus;
import org.pix.wallet.infrastructure.observability.MetricsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    WalletRepositoryPort walletPort = mock(WalletRepositoryPort.class);
    MetricsService metricsService = mock(MetricsService.class);
    WalletService service = new WalletService(walletPort, metricsService);

    @Test
    void createsWalletActive() {
        when(walletPort.save(any())).thenAnswer(a -> a.getArgument(0));
        Wallet w = service.create();
        assertNotNull(w.id());
        assertEquals(WalletStatus.ACTIVE, w.status());
    }

    // @Test
    // void getBalanceWalletNotFoundThrows() {
    //     UUID id = UUID.randomUUID();
    //     when(walletPort.findById(id)).thenReturn(Optional.empty());
    //     assertThrows(IllegalArgumentException.class, () -> service.getBalance(id));
    // }
}