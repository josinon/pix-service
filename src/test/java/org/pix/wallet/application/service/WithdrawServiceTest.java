package org.pix.wallet.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pix.wallet.application.port.in.WithdrawUseCase;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.pix.wallet.domain.model.Wallet;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawService Unit Tests")
class WithdrawServiceTest {

    @Mock
    private WalletRepositoryPort walletPort;

    @Mock
    private LedgerEntryRepositoryPort ledgerPort;

    @InjectMocks
    private WithdrawService withdrawService;

    private UUID walletId;
    private Wallet wallet;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        wallet = Wallet.builder()
                .id(walletId)
                .build();
        idempotencyKey = "idp-" + UUID.randomUUID();
    }

    @Test
    @DisplayName("Should execute withdraw successfully")
    void shouldExecuteWithdrawSuccessfully() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        var command = new WithdrawUseCase.Command(walletId, amount, idempotencyKey);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);

        // Act
        WithdrawUseCase.Result result = withdrawService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.walletId()).isEqualTo(walletId);
        assertThat(result.idempotencyKey()).isEqualTo(idempotencyKey);

        verify(walletPort).findById(walletId);
        verify(ledgerPort).existsByIdempotencyKey(idempotencyKey);
        verify(ledgerPort).withdraw(walletId.toString(), amount, idempotencyKey);
    }

    @Test
    @DisplayName("Should throw exception when amount is null")
    void shouldThrowExceptionWhenAmountIsNull() {
        // Arrange
        var command = new WithdrawUseCase.Command(walletId, null, idempotencyKey);

        // Act & Assert
        assertThatThrownBy(() -> withdrawService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be > 0");

        verify(walletPort, never()).findById(any());
        verify(ledgerPort, never()).withdraw(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when amount is zero")
    void shouldThrowExceptionWhenAmountIsZero() {
        // Arrange
        var command = new WithdrawUseCase.Command(walletId, BigDecimal.ZERO, idempotencyKey);

        // Act & Assert
        assertThatThrownBy(() -> withdrawService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be > 0");

        verify(walletPort, never()).findById(any());
        verify(ledgerPort, never()).withdraw(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when amount is negative")
    void shouldThrowExceptionWhenAmountIsNegative() {
        // Arrange
        var command = new WithdrawUseCase.Command(walletId, new BigDecimal("-50.00"), idempotencyKey);

        // Act & Assert
        assertThatThrownBy(() -> withdrawService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be > 0");

        verify(walletPort, never()).findById(any());
        verify(ledgerPort, never()).withdraw(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when idempotency key is null")
    void shouldThrowExceptionWhenIdempotencyKeyIsNull() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        var command = new WithdrawUseCase.Command(walletId, amount, null);

        // Act & Assert
        assertThatThrownBy(() -> withdrawService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Idempotency-Key required");

        verify(walletPort, never()).findById(any());
        verify(ledgerPort, never()).withdraw(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when idempotency key is blank")
    void shouldThrowExceptionWhenIdempotencyKeyIsBlank() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        var command = new WithdrawUseCase.Command(walletId, amount, "   ");

        // Act & Assert
        assertThatThrownBy(() -> withdrawService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Idempotency-Key required");

        verify(walletPort, never()).findById(any());
        verify(ledgerPort, never()).withdraw(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when idempotency key is empty")
    void shouldThrowExceptionWhenIdempotencyKeyIsEmpty() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        var command = new WithdrawUseCase.Command(walletId, amount, "");

        // Act & Assert
        assertThatThrownBy(() -> withdrawService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Idempotency-Key required");

        verify(walletPort, never()).findById(any());
        verify(ledgerPort, never()).withdraw(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when wallet not found")
    void shouldThrowExceptionWhenWalletNotFound() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        var command = new WithdrawUseCase.Command(walletId, amount, idempotencyKey);

        when(walletPort.findById(walletId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> withdrawService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Wallet not found");

        verify(walletPort).findById(walletId);
        verify(ledgerPort, never()).existsByIdempotencyKey(any());
        verify(ledgerPort, never()).withdraw(any(), any(), any());
    }

    @Test
    @DisplayName("Should return wallet id when idempotency key already exists")
    void shouldReturnWalletIdWhenIdempotencyKeyAlreadyExists() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        var command = new WithdrawUseCase.Command(walletId, amount, idempotencyKey);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.existsByIdempotencyKey(idempotencyKey)).thenReturn(true);

        // Act
        WithdrawUseCase.Result result = withdrawService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.walletId()).isEqualTo(walletId);
        assertThat(result.idempotencyKey()).isEqualTo(idempotencyKey);

        verify(walletPort).findById(walletId);
        verify(ledgerPort).existsByIdempotencyKey(idempotencyKey);
        verify(ledgerPort, never()).withdraw(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle decimal amounts correctly")
    void shouldHandleDecimalAmountsCorrectly() {
        // Arrange
        BigDecimal amount = new BigDecimal("99.99");
        var command = new WithdrawUseCase.Command(walletId, amount, idempotencyKey);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);

        // Act
        WithdrawUseCase.Result result = withdrawService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.walletId()).isEqualTo(walletId);

        verify(ledgerPort).withdraw(walletId.toString(), amount, idempotencyKey);
    }

    @Test
    @DisplayName("Should handle very small positive amounts")
    void shouldHandleVerySmallPositiveAmounts() {
        // Arrange
        BigDecimal amount = new BigDecimal("0.01");
        var command = new WithdrawUseCase.Command(walletId, amount, idempotencyKey);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);

        // Act
        WithdrawUseCase.Result result = withdrawService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.walletId()).isEqualTo(walletId);

        verify(ledgerPort).withdraw(walletId.toString(), amount, idempotencyKey);
    }

    @Test
    @DisplayName("Should handle large amounts")
    void shouldHandleLargeAmounts() {
        // Arrange
        BigDecimal amount = new BigDecimal("999999.99");
        var command = new WithdrawUseCase.Command(walletId, amount, idempotencyKey);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);

        // Act
        WithdrawUseCase.Result result = withdrawService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.walletId()).isEqualTo(walletId);

        verify(ledgerPort).withdraw(walletId.toString(), amount, idempotencyKey);
    }

    @Test
    @DisplayName("Should validate all parameters in correct order")
    void shouldValidateAllParametersInCorrectOrder() {
        // Arrange - null amount should be checked first
        var command = new WithdrawUseCase.Command(walletId, null, null);

        // Act & Assert
        assertThatThrownBy(() -> withdrawService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be > 0");
    }

    @Test
    @DisplayName("Should call withdraw with exact parameters")
    void shouldCallWithdrawWithExactParameters() {
        // Arrange
        BigDecimal amount = new BigDecimal("250.50");
        var command = new WithdrawUseCase.Command(walletId, amount, idempotencyKey);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);

        // Act
        withdrawService.execute(command);

        // Assert
        verify(ledgerPort).withdraw(
                eq(walletId.toString()),
                eq(amount),
                eq(idempotencyKey)
        );
    }

}