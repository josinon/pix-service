package org.pix.wallet.application.service;

import org.junit.jupiter.api.Test;
import org.pix.wallet.application.port.in.CreatePixKeyUseCase.CreatePixKeyCommand;
import org.pix.wallet.application.port.out.PixKeyRepositoryPort;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.domain.model.enums.WalletStatus;
import org.pix.wallet.domain.validator.PixKeyValidator;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PixKeyServiceTest {

    WalletRepositoryPort walletPort = mock(WalletRepositoryPort.class);
    PixKeyRepositoryPort pixPort = mock(PixKeyRepositoryPort.class);
    PixKeyValidator pixKeyValidator = new PixKeyValidator(); // Use real validator
    PixKeyService service = new PixKeyService(walletPort, pixPort, pixKeyValidator);

    @Test
    void createsRandomKey() {
        UUID wid = UUID.randomUUID();
        when(walletPort.findById(wid)).thenReturn(Optional.of( Wallet.builder()
                .id(wid)
                .status(WalletStatus.ACTIVE)
                .createdAt(Instant.now())
                .build()));
  
        when(pixPort.save(any())).thenAnswer(a -> a.getArgument(0));

        var res = service.execute(wid, new CreatePixKeyCommand("RANDOM", null));
        assertEquals("RANDOM", res.type());
        assertNotNull(res.value());
        assertTrue(res.value().length() >= 16);
    }

    @Test
    void duplicateValueThrows() {
        UUID wid = UUID.randomUUID();
        when(walletPort.findById(wid)).thenReturn(Optional.of(Wallet.builder()
                .id(wid)
                .status(WalletStatus.ACTIVE)
                .createdAt(Instant.now())
                .build()));
        when(pixPort.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(DataIntegrityViolationException.class,
                () -> service.execute(wid, new CreatePixKeyCommand("CPF", "12345678901")));
    }

    
}