package org.pix.wallet.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pix.wallet.application.port.in.ProcessPixTransferUseCase;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.application.port.out.PixKeyRepositoryPort;
import org.pix.wallet.application.port.out.TransferRepositoryPort;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.pix.wallet.domain.model.PixKey;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.domain.model.enums.PixKeyStatus;
import org.pix.wallet.domain.model.enums.PixKeyType;
import org.pix.wallet.domain.model.enums.WalletStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PixTransferService Unit Tests")
public class PixTransferServiceTest {

    @Mock
    private WalletRepositoryPort walletRepositoryPort;

    @Mock
    private PixKeyRepositoryPort pixKeyRepositoryPort;

    @Mock
    private TransferRepositoryPort transferRepositoryPort;

    @Mock
    private LedgerEntryRepositoryPort ledgerEntryRepositoryPort;

    @Mock
    private org.pix.wallet.infrastructure.observability.MetricsService metricsService;

    @InjectMocks
    private PixTransferService pixTransferService;

    private UUID fromWalletId;
    private UUID toWalletId;
    private String pixKey;
    private String idempotencyKey;
    private BigDecimal amount;

    @BeforeEach
    void setUp() {
        fromWalletId = UUID.randomUUID();
        toWalletId = UUID.randomUUID();
        pixKey = "12345678901";
        idempotencyKey = "idp-" + UUID.randomUUID();
        amount = new BigDecimal("100.00");
    }



    @Test
    @DisplayName("Should create PIX transfer successfully")
    void shouldCreatePixTransferSuccessfully() {
        // Arrange
        var command = new ProcessPixTransferUseCase.Command(fromWalletId.toString(), pixKey, amount, idempotencyKey);

        Wallet fromWallet = Wallet.builder()
                .id(fromWalletId)
                .status(WalletStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        Wallet toWallet = Wallet.builder()
                .id(toWalletId)
                .status(WalletStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        PixKey pixKeyEntity = new PixKey(
                UUID.randomUUID(),
                toWalletId,
                PixKeyType.CPF,
                pixKey,
                PixKeyStatus.ACTIVE,
                OffsetDateTime.now()
        );

        when(transferRepositoryPort.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(walletRepositoryPort.findById(fromWalletId)).thenReturn(Optional.of(fromWallet));
        when(pixKeyRepositoryPort.findByValueAndActive(pixKey)).thenReturn(Optional.of(pixKeyEntity));
        when(walletRepositoryPort.findById(toWalletId)).thenReturn(Optional.of(toWallet));
        when(ledgerEntryRepositoryPort.getCurrentBalance(fromWalletId.toString())).thenReturn(Optional.of(new BigDecimal("500.00")));

        TransferRepositoryPort.TransferResult transferResult = new TransferRepositoryPort.TransferResult(
                UUID.randomUUID(),
                "E12345678901234567890123456789AB",
                fromWalletId.toString(),
                toWalletId.toString(),
                amount,
                "BRL",
                "PENDING",
                0
        );

        when(transferRepositoryPort.save(any())).thenReturn(transferResult);

        // Act
        ProcessPixTransferUseCase.Result result = pixTransferService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.endToEndId()).startsWith("E");
        assertThat(result.status()).isEqualTo("PENDING");

        verify(transferRepositoryPort).existsByIdempotencyKey(idempotencyKey);
        verify(walletRepositoryPort).findById(fromWalletId);
        verify(pixKeyRepositoryPort).findByValueAndActive(pixKey);
        verify(walletRepositoryPort).findById(toWalletId);
        verify(ledgerEntryRepositoryPort).getCurrentBalance(fromWalletId.toString());
        verify(transferRepositoryPort).save(any());
    }

    @Test
    @DisplayName("Should return existing transfer when idempotency key already exists")
    void shouldReturnExistingTransferWhenIdempotencyKeyExists() {
        // Arrange
        var command = new ProcessPixTransferUseCase.Command(fromWalletId.toString(), pixKey, amount, idempotencyKey);

        TransferRepositoryPort.TransferResult existingTransfer = new TransferRepositoryPort.TransferResult(
                UUID.randomUUID(),
                "E12345678901234567890123456789AB",
                fromWalletId.toString(),
                toWalletId.toString(),
                amount,
                "BRL",
                "CONFIRMED",
                1
        );

        when(transferRepositoryPort.existsByIdempotencyKey(idempotencyKey)).thenReturn(true);
        when(transferRepositoryPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingTransfer));

        // Act
        ProcessPixTransferUseCase.Result result = pixTransferService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.endToEndId()).isEqualTo("E12345678901234567890123456789AB");
        assertThat(result.status()).isEqualTo("CONFIRMED");

        verify(transferRepositoryPort).existsByIdempotencyKey(idempotencyKey);
        verify(transferRepositoryPort).findByIdempotencyKey(idempotencyKey);
        verify(transferRepositoryPort, never()).save(any());
        verifyNoInteractions(walletRepositoryPort, ledgerEntryRepositoryPort, pixKeyRepositoryPort);
    }

        @Test
        @DisplayName("Should throw IllegalState when idempotent transfer record is missing")
        void shouldThrowIllegalStateWhenIdempotentTransferMissing() {
                // Arrange
                var command = new ProcessPixTransferUseCase.Command(fromWalletId.toString(), pixKey, amount, idempotencyKey);

                when(transferRepositoryPort.existsByIdempotencyKey(idempotencyKey)).thenReturn(true);
                when(transferRepositoryPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> pixTransferService.execute(command))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage("Transfer exists but not found");

                verify(transferRepositoryPort).existsByIdempotencyKey(idempotencyKey);
                verify(transferRepositoryPort).findByIdempotencyKey(idempotencyKey);
                verify(transferRepositoryPort, never()).save(any());
                verifyNoInteractions(walletRepositoryPort, ledgerEntryRepositoryPort, pixKeyRepositoryPort);
        }

    @Test
    @DisplayName("Should throw exception when amount is null")
    void shouldThrowExceptionWhenAmountIsNull() {
        // Arrange
        var command = new ProcessPixTransferUseCase.Command(fromWalletId.toString(), pixKey, null, idempotencyKey);

        // Act & Assert
        assertThatThrownBy(() -> pixTransferService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be > 0");

        verify(transferRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when amount is zero")
    void shouldThrowExceptionWhenAmountIsZero() {
        // Arrange
        var command = new ProcessPixTransferUseCase.Command(fromWalletId.toString(), pixKey, BigDecimal.ZERO, idempotencyKey);

        // Act & Assert
        assertThatThrownBy(() -> pixTransferService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be > 0");

        verify(transferRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when amount is negative")
    void shouldThrowExceptionWhenAmountIsNegative() {
        // Arrange
        var command = new ProcessPixTransferUseCase.Command(fromWalletId.toString(), pixKey, new BigDecimal("-50.00"), idempotencyKey);

        // Act & Assert
        assertThatThrownBy(() -> pixTransferService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be > 0");

        verify(transferRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when idempotency key is null")
    void shouldThrowExceptionWhenIdempotencyKeyIsNull() {
        // Arrange
        var command = new ProcessPixTransferUseCase.Command(fromWalletId.toString(), pixKey, amount, null);

        // Act & Assert
        assertThatThrownBy(() -> pixTransferService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Idempotency-Key required");

        verify(transferRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when idempotency key is blank")
    void shouldThrowExceptionWhenIdempotencyKeyIsBlank() {
        // Arrange
        var command = new ProcessPixTransferUseCase.Command(fromWalletId.toString(), pixKey, amount, "   ");

        // Act & Assert
        assertThatThrownBy(() -> pixTransferService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Idempotency-Key required");

        verify(transferRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when PIX key is null")
    void shouldThrowExceptionWhenPixKeyIsNull() {
        // Arrange
        var command = new ProcessPixTransferUseCase.Command(fromWalletId.toString(), null, amount, idempotencyKey);

        // Act & Assert
        assertThatThrownBy(() -> pixTransferService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PIX key is required");

        verify(transferRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when from wallet not found")
    void shouldThrowExceptionWhenFromWalletNotFound() {
        // Arrange
        var command = new ProcessPixTransferUseCase.Command(fromWalletId.toString(), pixKey, amount, idempotencyKey);

        when(transferRepositoryPort.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(walletRepositoryPort.findById(fromWalletId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> pixTransferService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source wallet not found");

        verify(transferRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when insufficient balance")
    void shouldThrowExceptionWhenInsufficientBalance() {
        // Arrange
        var command = new ProcessPixTransferUseCase.Command(fromWalletId.toString(), pixKey, amount, idempotencyKey);

        Wallet fromWallet = Wallet.builder()
                .id(fromWalletId)
                .status(WalletStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
        
        Wallet toWallet = Wallet.builder()
                .id(toWalletId)
                .status(WalletStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        PixKey pixKeyEntity = new PixKey(
                UUID.randomUUID(),
                toWalletId,
                PixKeyType.CPF,
                pixKey,
                PixKeyStatus.ACTIVE,
                OffsetDateTime.now()
        );

        when(transferRepositoryPort.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(walletRepositoryPort.findById(fromWalletId)).thenReturn(Optional.of(fromWallet));
        when(pixKeyRepositoryPort.findByValueAndActive(pixKey)).thenReturn(Optional.of(pixKeyEntity));
        when(walletRepositoryPort.findById(toWalletId)).thenReturn(Optional.of(toWallet));
        when(ledgerEntryRepositoryPort.getCurrentBalance(fromWalletId.toString())).thenReturn(Optional.of(new BigDecimal("50.00")));

        // Act & Assert
        assertThatThrownBy(() -> pixTransferService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");

        verify(transferRepositoryPort, never()).save(any());
    }

}
