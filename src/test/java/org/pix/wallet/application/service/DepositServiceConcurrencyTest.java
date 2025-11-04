package org.pix.wallet.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.pix.wallet.application.port.in.DepositUseCase;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.domain.model.enums.WalletStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("DepositService Concurrency Tests")
class DepositServiceConcurrencyTest {

    WalletRepositoryPort walletPort = mock(WalletRepositoryPort.class);
    LedgerEntryRepositoryPort ledgerPort = mock(LedgerEntryRepositoryPort.class);
    DepositService service = new DepositService(walletPort, ledgerPort);

    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        wallet = Wallet.builder()
                .id(walletId)
                .status(WalletStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should process deposit only once when multiple threads use same idempotency key")
    void shouldProcessDepositOnlyOnceWithSameIdempotencyKey() throws InterruptedException, ExecutionException {
        // Arrange
        String idempotencyKey = "idp-" + UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        int threadCount = 10;
        
        AtomicInteger depositCallCount = new AtomicInteger(0);

        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.existsByIdempotencyKey(idempotencyKey))
            .thenReturn(false)  // primeira chamada
            .thenReturn(true);  // demais chamadas

        doAnswer(invocation -> {
            depositCallCount.incrementAndGet();
            return null;
        }).when(ledgerPort).deposit(eq(walletId), eq(amount), eq(idempotencyKey));

        // Act
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<DepositUseCase.Result>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                latch.await(); // todas esperam
                var command = new DepositUseCase.Command(walletId, amount, idempotencyKey);
                return service.execute(command);
            }));
        }

        latch.countDown(); // libera todas de uma vez
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Assert
        for (Future<DepositUseCase.Result> future : futures) {
            assertThat(future.get().walletId()).isEqualTo(walletId);
        }

        // Apenas 1 depósito persistido
        assertThat(depositCallCount.get()).isEqualTo(1);
        verify(ledgerPort, times(1)).deposit(walletId, amount, idempotencyKey);
    }

    @Test
    @DisplayName("Should process all deposits when using different idempotency keys")
    void shouldProcessAllDepositsWithDifferentIdempotencyKeys() throws InterruptedException, ExecutionException {
        // Arrange
        BigDecimal amount = new BigDecimal("50.00");
        int threadCount = 5;
        
        when(walletPort.findById(walletId)).thenReturn(Optional.of(wallet));
        when(ledgerPort.existsByIdempotencyKey(any())).thenReturn(false);

        // Act
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<DepositUseCase.Result>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            String uniqueKey = "idp-" + i;
            futures.add(executor.submit(() -> {
                latch.await();
                var command = new DepositUseCase.Command(walletId, amount, uniqueKey);
                return service.execute(command);
            }));
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Assert
        for (Future<DepositUseCase.Result> future : futures) {
            assertThat(future.get().walletId()).isEqualTo(walletId);
        }

        // Todos os 5 depósitos persistidos
        verify(ledgerPort, times(threadCount)).deposit(eq(walletId), eq(amount), any());
    }
}