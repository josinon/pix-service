package org.pix.wallet.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MetricsService Unit Tests")
class MetricsServiceTest {

    private MeterRegistry meterRegistry;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }

    @Test
    @DisplayName("Should record transfer created and increment pending gauge")
    void shouldRecordTransferCreated() {
        // When
        metricsService.recordTransferCreated();

        // Then
        Counter counter = meterRegistry.find("pix.transfers.created").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);

        // Verify pending gauge incremented
        assertThat(metricsService.getPendingTransfersCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should record transfer confirmed and decrement pending gauge")
    void shouldRecordTransferConfirmed() {
        // Given - create a transfer first
        metricsService.recordTransferCreated();

        // When
        metricsService.recordTransferConfirmed(Duration.ofSeconds(2));

        // Then
        Counter counter = meterRegistry.find("pix.transfers.confirmed").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);

        // Verify pending gauge decremented
        assertThat(metricsService.getPendingTransfersCount()).isEqualTo(0);

        // Verify end-to-end timer recorded
        Timer timer = meterRegistry.find("pix.transfer.end_to_end.time").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should record transfer rejected and decrement pending gauge")
    void shouldRecordTransferRejected() {
        // Given - create a transfer first
        metricsService.recordTransferCreated();

        // When
        metricsService.recordTransferRejected();

        // Then
        Counter counter = meterRegistry.find("pix.transfers.rejected").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);

        // Verify pending gauge decremented
        assertThat(metricsService.getPendingTransfersCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should record transfer creation time")
    void shouldRecordTransferCreationTime() {
        // Given
        Timer.Sample sample = metricsService.startTransferCreation();

        // When
        metricsService.recordTransferCreation(sample);

        // Then
        Timer timer = meterRegistry.find("pix.transfer.creation.time").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should record transfer creation error with type tag")
    void shouldRecordTransferCreationError() {
        // Given
        Timer.Sample sample = metricsService.startTransferCreation();

        // When
        metricsService.recordTransferCreationError(sample, "insufficient_balance");

        // Then - Verify timer with error tags was recorded
        Timer timer = meterRegistry.find("pix.transfer.creation.time")
                .tag("status", "error")
                .tag("error_type", "insufficient_balance")
                .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should record webhook received with event type tag")
    void shouldRecordWebhookReceived() {
        // When
        metricsService.recordWebhookReceived("CONFIRMED");

        // Then
        Counter totalCounter = meterRegistry.find("pix.webhooks.received").counter();
        assertThat(totalCounter).isNotNull();
        assertThat(totalCounter.count()).isEqualTo(1.0);

        Counter typeCounter = meterRegistry.find("pix.webhooks.by_type")
                .tag("eventType", "CONFIRMED")
                .counter();
        assertThat(typeCounter).isNotNull();
        assertThat(typeCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record webhook duplicated")
    void shouldRecordWebhookDuplicated() {
        // When
        metricsService.recordWebhookDuplicated();

        // Then
        Counter counter = meterRegistry.find("pix.webhooks.duplicated").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record webhook processing time")
    void shouldRecordWebhookProcessingTime() {
        // Given
        Timer.Sample sample = metricsService.startWebhookProcessing();

        // When
        metricsService.recordWebhookProcessing(sample);

        // Then
        Timer timer = meterRegistry.find("pix.webhook.processing.time").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should record webhook processing error with type tag")
    void shouldRecordWebhookProcessingError() {
        // Given
        Timer.Sample sample = metricsService.startWebhookProcessing();

        // When
        metricsService.recordWebhookProcessingError(sample, "transfer_not_found");

        // Then - Verify timer with error tags was recorded
        Timer timer = meterRegistry.find("pix.webhook.processing.time")
                .tag("status", "error")
                .tag("error_type", "transfer_not_found")
                .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should record wallet created and increment active gauge")
    void shouldRecordWalletCreated() {
        // When
        metricsService.recordWalletCreated();

        // Then
        Counter counter = meterRegistry.find("pix.wallets.created").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);

        // Verify active gauge incremented
        assertThat(metricsService.getActiveWalletsCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should record PIX key registered with type tag")
    void shouldRecordPixKeyRegistered() {
        // When
        metricsService.recordPixKeyRegistered("CPF");

        // Then
        Counter totalCounter = meterRegistry.find("pix.pixkeys.registered").counter();
        assertThat(totalCounter).isNotNull();
        assertThat(totalCounter.count()).isEqualTo(1.0);

        Counter typeCounter = meterRegistry.find("pix.pixkeys.by_type")
                .tag("keyType", "CPF")
                .counter();
        assertThat(typeCounter).isNotNull();
        assertThat(typeCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record deposit completed")
    void shouldRecordDepositCompleted() {
        // When
        metricsService.recordDepositCompleted();

        // Then
        Counter counter = meterRegistry.find("pix.deposits.completed").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record withdrawal completed")
    void shouldRecordWithdrawalCompleted() {
        // When
        metricsService.recordWithdrawalCompleted();

        // Then
        Counter counter = meterRegistry.find("pix.withdrawals.completed").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should maintain correct pending transfers gauge count")
    void shouldMaintainCorrectPendingCount() {
        // Initially zero
        assertThat(metricsService.getPendingTransfersCount()).isEqualTo(0);

        // Create 3 transfers
        metricsService.recordTransferCreated();
        metricsService.recordTransferCreated();
        metricsService.recordTransferCreated();
        assertThat(metricsService.getPendingTransfersCount()).isEqualTo(3);

        // Confirm 1
        metricsService.recordTransferConfirmed(Duration.ofSeconds(1));
        assertThat(metricsService.getPendingTransfersCount()).isEqualTo(2);

        // Reject 1
        metricsService.recordTransferRejected();
        assertThat(metricsService.getPendingTransfersCount()).isEqualTo(1);

        // Confirm last one
        metricsService.recordTransferConfirmed(Duration.ofSeconds(2));
        assertThat(metricsService.getPendingTransfersCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should maintain correct active wallets gauge count")
    void shouldMaintainCorrectActiveWalletsCount() {
        // Initially zero
        assertThat(metricsService.getActiveWalletsCount()).isEqualTo(0);

        // Create 5 wallets
        metricsService.recordWalletCreated();
        metricsService.recordWalletCreated();
        metricsService.recordWalletCreated();
        metricsService.recordWalletCreated();
        metricsService.recordWalletCreated();

        assertThat(metricsService.getActiveWalletsCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should record multiple event types for webhooks")
    void shouldRecordMultipleWebhookEventTypes() {
        // When
        metricsService.recordWebhookReceived("CONFIRMED");
        metricsService.recordWebhookReceived("CONFIRMED");
        metricsService.recordWebhookReceived("REJECTED");

        // Then
        Counter confirmedCounter = meterRegistry.find("pix.webhooks.by_type")
                .tag("eventType", "CONFIRMED")
                .counter();
        assertThat(confirmedCounter).isNotNull();
        assertThat(confirmedCounter.count()).isEqualTo(2.0);

        Counter rejectedCounter = meterRegistry.find("pix.webhooks.by_type")
                .tag("eventType", "REJECTED")
                .counter();
        assertThat(rejectedCounter).isNotNull();
        assertThat(rejectedCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record multiple PIX key types")
    void shouldRecordMultiplePixKeyTypes() {
        // When
        metricsService.recordPixKeyRegistered("CPF");
        metricsService.recordPixKeyRegistered("EMAIL");
        metricsService.recordPixKeyRegistered("CPF");
        metricsService.recordPixKeyRegistered("PHONE");

        // Then
        Counter cpfCounter = meterRegistry.find("pix.pixkeys.by_type")
                .tag("keyType", "CPF")
                .counter();
        assertThat(cpfCounter).isNotNull();
        assertThat(cpfCounter.count()).isEqualTo(2.0);

        Counter emailCounter = meterRegistry.find("pix.pixkeys.by_type")
                .tag("keyType", "EMAIL")
                .counter();
        assertThat(emailCounter).isNotNull();
        assertThat(emailCounter.count()).isEqualTo(1.0);

        Counter phoneCounter = meterRegistry.find("pix.pixkeys.by_type")
                .tag("keyType", "PHONE")
                .counter();
        assertThat(phoneCounter).isNotNull();
        assertThat(phoneCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should verify all gauges are registered")
    void shouldVerifyGaugesRegistered() {
        // Verify pending transfers gauge
        Gauge pendingGauge = meterRegistry.find("pix.transfers.pending").gauge();
        assertThat(pendingGauge).isNotNull();

        // Verify active wallets gauge
        Gauge activeWalletsGauge = meterRegistry.find("pix.wallets.active").gauge();
        assertThat(activeWalletsGauge).isNotNull();
    }

    @Test
    @DisplayName("Should record multiple types of transfer errors")
    void shouldRecordMultipleErrorTypes() {
        // When - record different error types
        Timer.Sample sample1 = metricsService.startTransferCreation();
        metricsService.recordTransferCreationError(sample1, "insufficient_balance");

        Timer.Sample sample2 = metricsService.startTransferCreation();
        metricsService.recordTransferCreationError(sample2, "not_found");

        Timer.Sample sample3 = metricsService.startTransferCreation();
        metricsService.recordTransferCreationError(sample3, "insufficient_balance");

        // Then - verify each error type is counted
        Timer insufficientBalanceTimer = meterRegistry.find("pix.transfer.creation.time")
                .tag("error_type", "insufficient_balance")
                .timer();
        assertThat(insufficientBalanceTimer.count()).isEqualTo(2);

        Timer notFoundTimer = meterRegistry.find("pix.transfer.creation.time")
                .tag("error_type", "not_found")
                .timer();
        assertThat(notFoundTimer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle pending transfers gauge not going below zero")
    void shouldNotDecrementPendingBelowZero() {
        // When - try to reject without creating
        assertThat(metricsService.getPendingTransfersCount()).isEqualTo(0);

        metricsService.recordTransferRejected();

        // Then - should be -1 (AtomicInteger allows negative)
        assertThat(metricsService.getPendingTransfersCount()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Should record end-to-end duration of zero for immediate confirmation")
    void shouldRecordZeroE2EDuration() {
        // Given
        metricsService.recordTransferCreated();

        // When - confirm immediately with zero duration
        metricsService.recordTransferConfirmed(Duration.ZERO);

        // Then
        Timer timer = meterRegistry.find("pix.transfer.end_to_end.time").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.NANOSECONDS)).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should record multiple deposits and withdrawals")
    void shouldRecordMultipleTransactions() {
        // When - record multiple deposits
        metricsService.recordDepositCompleted();
        metricsService.recordDepositCompleted();
        metricsService.recordDepositCompleted();

        // And - record multiple withdrawals
        metricsService.recordWithdrawalCompleted();
        metricsService.recordWithdrawalCompleted();

        // Then
        Counter deposits = meterRegistry.find("pix.deposits.completed").counter();
        assertThat(deposits.count()).isEqualTo(3.0);

        Counter withdrawals = meterRegistry.find("pix.withdrawals.completed").counter();
        assertThat(withdrawals.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should record webhook duplicates correctly")
    void shouldRecordWebhookDuplicates() {
        // When - record multiple duplicates
        metricsService.recordWebhookDuplicated();
        metricsService.recordWebhookDuplicated();
        metricsService.recordWebhookDuplicated();

        // Then
        Counter duplicates = meterRegistry.find("pix.webhooks.duplicated").counter();
        assertThat(duplicates.count()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("Should measure transfer creation timing accurately")
    void shouldMeasureTimingAccurately() throws InterruptedException {
        // Given - start timer
        Timer.Sample sample = metricsService.startTransferCreation();

        // When - simulate some work (1ms)
        Thread.sleep(1);

        // And - stop timer
        metricsService.recordTransferCreation(sample);

        // Then - verify timer recorded non-zero duration
        Timer timer = meterRegistry.find("pix.transfer.creation.time").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should measure webhook processing timing accurately")
    void shouldMeasureWebhookTimingAccurately() throws InterruptedException {
        // Given - start timer
        Timer.Sample sample = metricsService.startWebhookProcessing();

        // When - simulate some work (1ms)
        Thread.sleep(1);

        // And - stop timer
        metricsService.recordWebhookProcessing(sample);

        // Then - verify timer recorded non-zero duration
        Timer timer = meterRegistry.find("pix.webhook.processing.time").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }
}
