package org.pix.wallet.infrastructure.persistence.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.pix.wallet.domain.exception.InsufficientFundsException;
import org.pix.wallet.domain.model.enums.OperationType;
import org.pix.wallet.infrastructure.persistence.entity.LedgerEntryEntity;
import org.pix.wallet.infrastructure.persistence.entity.WalletEntity;
import org.pix.wallet.infrastructure.persistence.repository.LedgerEntryJpaRepository;
import org.pix.wallet.infrastructure.persistence.repository.WalletJpaRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("LedgerEntryRepositoryAdapter - Reserve/Unreserve Tests")
class LedgerEntryRepositoryAdapterReserveTest {

    private LedgerEntryRepositoryAdapter adapter;
    private LedgerEntryJpaRepository ledgerRepo;
    private WalletJpaRepository walletRepo;

    private UUID walletId;
    private WalletEntity walletEntity;

    @BeforeEach
    void setUp() {
        ledgerRepo = mock(LedgerEntryJpaRepository.class);
        walletRepo = mock(WalletJpaRepository.class);
        adapter = new LedgerEntryRepositoryAdapter(ledgerRepo, walletRepo);

        walletId = UUID.randomUUID();
        walletEntity = WalletEntity.builder()
            .id(walletId)
            .build();
    }

    @Test
    @DisplayName("Should reserve funds successfully when available balance is sufficient")
    void shouldReserveFundsSuccessfully() {
        // Given
        BigDecimal availableBalance = new BigDecimal("500.00");
        BigDecimal reserveAmount = new BigDecimal("100.00");
        String idempotencyKey = "reserve-123";

        when(walletRepo.findById(walletId)).thenReturn(Optional.of(walletEntity));
        when(ledgerRepo.findAvailableBalance(walletId)).thenReturn(Optional.of(availableBalance));
        when(ledgerRepo.save(any(LedgerEntryEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When
        String result = adapter.reserve(walletId.toString(), reserveAmount, idempotencyKey);

        // Then
        assertThat(result).isNotNull();
        verify(ledgerRepo).findAvailableBalance(walletId);
        verify(ledgerRepo).save(argThat(entry -> 
            entry.getOperationType() == OperationType.RESERVED &&
            entry.getAmount().compareTo(reserveAmount) == 0 &&
            entry.getIdempotencyKey().equals(idempotencyKey)
        ));
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException when available balance is less than reserve amount")
    void shouldThrowExceptionWhenInsufficientFunds() {
        // Given
        BigDecimal availableBalance = new BigDecimal("50.00");
        BigDecimal reserveAmount = new BigDecimal("100.00");
        String idempotencyKey = "reserve-456";

        when(walletRepo.findById(walletId)).thenReturn(Optional.of(walletEntity));
        when(ledgerRepo.findAvailableBalance(walletId)).thenReturn(Optional.of(availableBalance));

        // When/Then
        assertThatThrownBy(() -> adapter.reserve(walletId.toString(), reserveAmount, idempotencyKey))
            .isInstanceOf(InsufficientFundsException.class);

        verify(ledgerRepo, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when wallet not found for reserve")
    void shouldThrowExceptionWhenWalletNotFoundForReserve() {
        // Given
        BigDecimal reserveAmount = new BigDecimal("100.00");
        String idempotencyKey = "reserve-789";

        when(walletRepo.findById(walletId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> adapter.reserve(walletId.toString(), reserveAmount, idempotencyKey))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Wallet not found");

        verify(ledgerRepo, never()).findAvailableBalance(any());
        verify(ledgerRepo, never()).save(any());
    }

    @Test
    @DisplayName("Should unreserve funds successfully")
    void shouldUnreserveFundsSuccessfully() {
        // Given
        BigDecimal unreserveAmount = new BigDecimal("100.00");
        String idempotencyKey = "unreserve-123";

        when(walletRepo.findById(walletId)).thenReturn(Optional.of(walletEntity));
        when(ledgerRepo.save(any(LedgerEntryEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When
        String result = adapter.unreserve(walletId.toString(), unreserveAmount, idempotencyKey);

        // Then
        assertThat(result).isNotNull();
        verify(ledgerRepo).save(argThat(entry -> 
            entry.getOperationType() == OperationType.UNRESERVED &&
            entry.getAmount().compareTo(unreserveAmount) == 0 &&
            entry.getIdempotencyKey().equals(idempotencyKey)
        ));
    }

    @Test
    @DisplayName("Should throw exception when wallet not found for unreserve")
    void shouldThrowExceptionWhenWalletNotFoundForUnreserve() {
        // Given
        BigDecimal unreserveAmount = new BigDecimal("100.00");
        String idempotencyKey = "unreserve-456";

        when(walletRepo.findById(walletId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> adapter.unreserve(walletId.toString(), unreserveAmount, idempotencyKey))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Wallet not found");

        verify(ledgerRepo, never()).save(any());
    }

    @Test
    @DisplayName("Should get available balance successfully")
    void shouldGetAvailableBalanceSuccessfully() {
        // Given
        BigDecimal expectedBalance = new BigDecimal("350.00");
        when(ledgerRepo.findAvailableBalance(walletId)).thenReturn(Optional.of(expectedBalance));

        // When
        Optional<BigDecimal> result = adapter.getAvailableBalance(walletId.toString());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(expectedBalance);
        verify(ledgerRepo).findAvailableBalance(walletId);
    }

    @Test
    @DisplayName("Should return empty when no available balance found")
    void shouldReturnEmptyWhenNoBalanceFound() {
        // Given
        when(ledgerRepo.findAvailableBalance(walletId)).thenReturn(Optional.empty());

        // When
        Optional<BigDecimal> result = adapter.getAvailableBalance(walletId.toString());

        // Then
        assertThat(result).isEmpty();
        verify(ledgerRepo).findAvailableBalance(walletId);
    }

    @Test
    @DisplayName("Should handle reserve with exactly available balance")
    void shouldHandleReserveWithExactBalance() {
        // Given
        BigDecimal availableBalance = new BigDecimal("100.00");
        BigDecimal reserveAmount = new BigDecimal("100.00");
        String idempotencyKey = "reserve-exact";

        when(walletRepo.findById(walletId)).thenReturn(Optional.of(walletEntity));
        when(ledgerRepo.findAvailableBalance(walletId)).thenReturn(Optional.of(availableBalance));
        when(ledgerRepo.save(any(LedgerEntryEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When
        String result = adapter.reserve(walletId.toString(), reserveAmount, idempotencyKey);

        // Then
        assertThat(result).isNotNull();
        verify(ledgerRepo).save(any(LedgerEntryEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when reserve amount is 0.01 more than available")
    void shouldThrowExceptionWhenReserveSlightlyOverAvailable() {
        // Given
        BigDecimal availableBalance = new BigDecimal("99.99");
        BigDecimal reserveAmount = new BigDecimal("100.00");
        String idempotencyKey = "reserve-over";

        when(walletRepo.findById(walletId)).thenReturn(Optional.of(walletEntity));
        when(ledgerRepo.findAvailableBalance(walletId)).thenReturn(Optional.of(availableBalance));

        // When/Then
        assertThatThrownBy(() -> adapter.reserve(walletId.toString(), reserveAmount, idempotencyKey))
            .isInstanceOf(InsufficientFundsException.class);

        verify(ledgerRepo, never()).save(any());
    }
}
