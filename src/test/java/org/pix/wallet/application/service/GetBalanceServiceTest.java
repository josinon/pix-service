package org.pix.wallet.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pix.wallet.application.port.in.GetBalanceUseCase;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.pix.wallet.domain.model.Wallet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetBalanceService Unit Tests")
class GetBalanceServiceTest {

    @Mock
    private WalletRepositoryPort walletPort;

    @Mock
    private LedgerEntryRepositoryPort ledgerPort;

    @InjectMocks
    private GetBalanceService getBalanceService;

    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        wallet = Wallet.builder()
                .id(walletId)
                .build();
    }

    @Test
    @DisplayName("Should get current balance successfully")
    void shouldGetCurrentBalanceSuccessfully() {
        // Arrange
        BigDecimal expectedBalance = new BigDecimal("500.00");
        var command = new GetBalanceUseCase.Command(walletId, null);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.getCurrentBalance(walletId.toString())).thenReturn(Optional.of(expectedBalance));

        // Act
        GetBalanceUseCase.Result result = getBalanceService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.walletId()).isEqualTo(walletId);
        assertThat(result.balance()).isEqualByComparingTo(expectedBalance);

        verify(walletPort).findById(walletId);
        verify(ledgerPort).getCurrentBalance(walletId.toString());
        verify(ledgerPort, never()).getBalanceAsOf(any(), any());
    }

    @Test
    @DisplayName("Should return zero when current balance is empty")
    void shouldReturnZeroWhenCurrentBalanceIsEmpty() {
        // Arrange
        var command = new GetBalanceUseCase.Command(walletId, null);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.getCurrentBalance(walletId.toString())).thenReturn(Optional.empty());

        // Act
        GetBalanceUseCase.Result result = getBalanceService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.walletId()).isEqualTo(walletId);
        assertThat(result.balance()).isEqualByComparingTo(BigDecimal.ZERO);

        verify(walletPort).findById(walletId);
        verify(ledgerPort).getCurrentBalance(walletId.toString());
    }

    @Test
    @DisplayName("Should get balance as of specific timestamp")
    void shouldGetBalanceAsOfSpecificTimestamp() {
        // Arrange
        Instant timestamp = Instant.parse("2025-11-01T10:00:00Z");
        BigDecimal expectedBalance = new BigDecimal("250.50");
        var command = new GetBalanceUseCase.Command(walletId, timestamp);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.getBalanceAsOf(walletId.toString(), timestamp)).thenReturn(Optional.of(expectedBalance));

        // Act
        GetBalanceUseCase.Result result = getBalanceService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.walletId()).isEqualTo(walletId);
        assertThat(result.balance()).isEqualByComparingTo(expectedBalance);

        verify(walletPort).findById(walletId);
        verify(ledgerPort).getBalanceAsOf(walletId.toString(), timestamp);
        verify(ledgerPort, never()).getCurrentBalance(any());
    }

    @Test
    @DisplayName("Should return zero when balance as of is empty")
    void shouldReturnZeroWhenBalanceAsOfIsEmpty() {
        // Arrange
        Instant timestamp = Instant.parse("2025-11-01T10:00:00Z");
        var command = new GetBalanceUseCase.Command(walletId, timestamp);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.getBalanceAsOf(walletId.toString(), timestamp)).thenReturn(Optional.empty());

        // Act
        GetBalanceUseCase.Result result = getBalanceService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.walletId()).isEqualTo(walletId);
        assertThat(result.balance()).isEqualByComparingTo(BigDecimal.ZERO);

        verify(walletPort).findById(walletId);
        verify(ledgerPort).getBalanceAsOf(walletId.toString(), timestamp);
    }

    @Test
    @DisplayName("Should throw exception when wallet not found")
    void shouldThrowExceptionWhenWalletNotFound() {
        // Arrange
        var command = new GetBalanceUseCase.Command(walletId, null);

        when(walletPort.findById(walletId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getBalanceService.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Wallet not found");

        verify(walletPort).findById(walletId);
        verify(ledgerPort, never()).getCurrentBalance(any());
        verify(ledgerPort, never()).getBalanceAsOf(any(), any());
    }

    @Test
    @DisplayName("Should handle negative balances correctly")
    void shouldHandleNegativeBalancesCorrectly() {
        // Arrange
        BigDecimal negativeBalance = new BigDecimal("-100.00");
        var command = new GetBalanceUseCase.Command(walletId, null);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.getCurrentBalance(walletId.toString())).thenReturn(Optional.of(negativeBalance));

        // Act
        GetBalanceUseCase.Result result = getBalanceService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.balance()).isEqualByComparingTo(negativeBalance);
    }

    @Test
    @DisplayName("Should handle large balances correctly")
    void shouldHandleLargeBalancesCorrectly() {
        // Arrange
        BigDecimal largeBalance = new BigDecimal("999999999.99");
        var command = new GetBalanceUseCase.Command(walletId, null);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.getCurrentBalance(walletId.toString())).thenReturn(Optional.of(largeBalance));

        // Act
        GetBalanceUseCase.Result result = getBalanceService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.balance()).isEqualByComparingTo(largeBalance);
    }

    @Test
    @DisplayName("Should handle very small balances correctly")
    void shouldHandleVerySmallBalancesCorrectly() {
        // Arrange
        BigDecimal smallBalance = new BigDecimal("0.01");
        var command = new GetBalanceUseCase.Command(walletId, null);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.getCurrentBalance(walletId.toString())).thenReturn(Optional.of(smallBalance));

        // Act
        GetBalanceUseCase.Result result = getBalanceService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.balance()).isEqualByComparingTo(smallBalance);
    }

    @Test
    @DisplayName("Should get balance at past timestamp")
    void shouldGetBalanceAtPastTimestamp() {
        // Arrange
        Instant pastTimestamp = Instant.now().minusSeconds(86400); // 1 day ago
        BigDecimal pastBalance = new BigDecimal("300.00");
        var command = new GetBalanceUseCase.Command(walletId, pastTimestamp);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.getBalanceAsOf(walletId.toString(), pastTimestamp)).thenReturn(Optional.of(pastBalance));

        // Act
        GetBalanceUseCase.Result result = getBalanceService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.balance()).isEqualByComparingTo(pastBalance);
        verify(ledgerPort).getBalanceAsOf(walletId.toString(), pastTimestamp);
    }

    @Test
    @DisplayName("Should get balance at future timestamp")
    void shouldGetBalanceAtFutureTimestamp() {
        // Arrange
        Instant futureTimestamp = Instant.now().plusSeconds(86400); // 1 day ahead
        var command = new GetBalanceUseCase.Command(walletId, futureTimestamp);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.getBalanceAsOf(walletId.toString(), futureTimestamp)).thenReturn(Optional.empty());

        // Act
        GetBalanceUseCase.Result result = getBalanceService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.balance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(ledgerPort).getBalanceAsOf(walletId.toString(), futureTimestamp);
    }

    @Test
    @DisplayName("Should call correct repository method based on timestamp presence")
    void shouldCallCorrectRepositoryMethodBasedOnTimestamp() {
        // Arrange - without timestamp
        var commandWithoutTimestamp = new GetBalanceUseCase.Command(walletId, null);
        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.getCurrentBalance(walletId.toString())).thenReturn(Optional.of(BigDecimal.TEN));

        // Act
        getBalanceService.execute(commandWithoutTimestamp);

        // Assert
        verify(ledgerPort).getCurrentBalance(walletId.toString());
        verify(ledgerPort, never()).getBalanceAsOf(any(), any());

        // Reset mocks
        reset(walletPort, ledgerPort);

        // Arrange - with timestamp
        Instant timestamp = Instant.now();
        var commandWithTimestamp = new GetBalanceUseCase.Command(walletId, timestamp);
        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.getBalanceAsOf(walletId.toString(), timestamp)).thenReturn(Optional.of(BigDecimal.TEN));

        // Act
        getBalanceService.execute(commandWithTimestamp);

        // Assert
        verify(ledgerPort).getBalanceAsOf(walletId.toString(), timestamp);
        verify(ledgerPort, never()).getCurrentBalance(any());
    }

    @Test
    @DisplayName("Should handle balance with many decimal places")
    void shouldHandleBalanceWithManyDecimalPlaces() {
        // Arrange
        BigDecimal preciseBalance = new BigDecimal("123.456789");
        var command = new GetBalanceUseCase.Command(walletId, null);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.getCurrentBalance(walletId.toString())).thenReturn(Optional.of(preciseBalance));

        // Act
        GetBalanceUseCase.Result result = getBalanceService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.balance()).isEqualByComparingTo(preciseBalance);
    }

    @Test
    @DisplayName("Should verify wallet existence before querying balance")
    void shouldVerifyWalletExistenceBeforeQueryingBalance() {
        // Arrange
        var command = new GetBalanceUseCase.Command(walletId, null);
        when(walletPort.findById(walletId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getBalanceService.execute(command))
                .isInstanceOf(IllegalArgumentException.class);

        // Verifica que o ledger não foi consultado
        verify(ledgerPort, never()).getCurrentBalance(any());
        verify(ledgerPort, never()).getBalanceAsOf(any(), any());
    }
}