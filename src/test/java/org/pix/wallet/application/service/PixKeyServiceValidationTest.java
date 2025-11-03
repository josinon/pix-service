package org.pix.wallet.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pix.wallet.application.port.in.CreatePixKeyUseCase.CreatePixKeyCommand;
import org.pix.wallet.application.port.out.PixKeyRepositoryPort;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.domain.model.enums.WalletStatus;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class PixKeyServiceValidationTest {

    WalletRepositoryPort walletPort = mock(WalletRepositoryPort.class);
    PixKeyRepositoryPort pixPort = mock(PixKeyRepositoryPort.class);
    PixKeyService service = new PixKeyService(walletPort, pixPort);
    UUID walletId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(walletPort.findById(walletId))
                .thenReturn(Optional.of(Wallet.builder()
                        .id(walletId)
                        .status(WalletStatus.ACTIVE)
                        .createdAt(OffsetDateTime.now())
                        .build()));
        when(pixPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void cpfValido() {
        var r = service.execute(walletId, new CreatePixKeyCommand("CPF", "12345678901"));
        assertEquals("CPF", r.type());
        assertEquals("12345678901", r.value());
    }

    @Test
    void cpfInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.execute(walletId, new CreatePixKeyCommand("CPF", "123")));
    }

    @Test
    void emailValido() {
        var r = service.execute(walletId, new CreatePixKeyCommand("EMAIL", "user@test.com"));
        assertEquals("EMAIL", r.type());
    }

    @Test
    void emailInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.execute(walletId, new CreatePixKeyCommand("EMAIL", "user@test")));
    }

    @Test
    void phoneValido() {
        var r = service.execute(walletId, new CreatePixKeyCommand("PHONE", "+5511999999999"));
        assertEquals("PHONE", r.type());
    }

    @Test
    void phoneInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.execute(walletId, new CreatePixKeyCommand("PHONE", "5511999999999"))); // falta "+"
    }

    @Test
    void tipoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.execute(walletId, new CreatePixKeyCommand("XYZ", "abc")));
    }
}
