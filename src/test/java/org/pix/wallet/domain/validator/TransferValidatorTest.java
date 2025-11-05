package org.pix.wallet.domain.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for TransferValidator to ensure transfer validation rules
 * are correctly enforced.
 */
@DisplayName("TransferValidator Unit Tests")
class TransferValidatorTest {

    private final TransferValidator validator = new TransferValidator();

    @Test
    @DisplayName("Should validate valid transfer amount")
    void shouldValidateValidAmount() {
        // Given
        BigDecimal validAmount = new BigDecimal("100.50");

        // When/Then - should not throw
        validator.validateAmount(validAmount);
    }

    @Test
    @DisplayName("Should reject null amount")
    void shouldRejectNullAmount() {
        // When/Then
        assertThatThrownBy(() -> validator.validateAmount(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Transfer amount is required");
    }

    @Test
    @DisplayName("Should reject zero amount")
    void shouldRejectZeroAmount() {
        // Given
        BigDecimal zero = BigDecimal.ZERO;

        // When/Then
        assertThatThrownBy(() -> validator.validateAmount(zero))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be greater than zero");
    }

    @Test
    @DisplayName("Should reject negative amount")
    void shouldRejectNegativeAmount() {
        // Given
        BigDecimal negative = new BigDecimal("-50.00");

        // When/Then
        assertThatThrownBy(() -> validator.validateAmount(negative))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be greater than zero");
    }

    @Test
    @DisplayName("Should reject amount exceeding maximum limit")
    void shouldRejectAmountExceedingLimit() {
        // Given
        BigDecimal tooLarge = new BigDecimal("100001.00"); // > 100,000

        // When/Then
        assertThatThrownBy(() -> validator.validateAmount(tooLarge))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exceeds maximum limit");
    }

    @Test
    @DisplayName("Should accept amount at maximum limit")
    void shouldAcceptAmountAtMaximumLimit() {
        // Given
        BigDecimal atLimit = new BigDecimal("100000.00");

        // When/Then - should not throw
        validator.validateAmount(atLimit);
    }

    @Test
    @DisplayName("Should validate different wallets")
    void shouldValidateDifferentWallets() {
        // Given
        UUID wallet1 = UUID.randomUUID();
        UUID wallet2 = UUID.randomUUID();

        // When/Then - should not throw
        validator.validateDifferentWallets(wallet1, wallet2);
    }

    @Test
    @DisplayName("Should reject same source and destination wallet")
    void shouldRejectSameWallets() {
        // Given
        UUID sameWallet = UUID.randomUUID();

        // When/Then
        assertThatThrownBy(() -> validator.validateDifferentWallets(sameWallet, sameWallet))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot transfer to the same wallet");
    }

    @Test
    @DisplayName("Should reject null source wallet")
    void shouldRejectNullSourceWallet() {
        // Given
        UUID destination = UUID.randomUUID();

        // When/Then
        assertThatThrownBy(() -> validator.validateDifferentWallets(null, destination))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Source wallet ID is required");
    }

    @Test
    @DisplayName("Should reject null destination wallet")
    void shouldRejectNullDestinationWallet() {
        // Given
        UUID source = UUID.randomUUID();

        // When/Then
        assertThatThrownBy(() -> validator.validateDifferentWallets(source, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Destination wallet ID is required");
    }

    @Test
    @DisplayName("Should validate valid webhook event")
    void shouldValidateValidWebhookEvent() {
        // Given
        String endToEndId = "E12345678901234567890123456789012";
        String eventId = "evt-123";
        String eventType = "CONFIRMED";
        Instant occurredAt = Instant.now().minus(1, ChronoUnit.HOURS);

        // When/Then - should not throw
        validator.validateWebhookEvent(endToEndId, eventId, eventType, occurredAt);
    }

    @Test
    @DisplayName("Should reject webhook event with null endToEndId")
    void shouldRejectNullEndToEndId() {
        // When/Then
        assertThatThrownBy(() -> validator.validateWebhookEvent(null, "evt-123", "CONFIRMED", Instant.now()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("End to end ID is required");
    }

    @Test
    @DisplayName("Should reject webhook event with blank endToEndId")
    void shouldRejectBlankEndToEndId() {
        // When/Then
        assertThatThrownBy(() -> validator.validateWebhookEvent("   ", "evt-123", "CONFIRMED", Instant.now()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("End to end ID is required");
    }

    @Test
    @DisplayName("Should reject webhook event with null eventId")
    void shouldRejectNullEventId() {
        // When/Then
        assertThatThrownBy(() -> validator.validateWebhookEvent("E123", null, "CONFIRMED", Instant.now()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Event ID is required");
    }

    @Test
    @DisplayName("Should reject webhook event with null eventType")
    void shouldRejectNullEventType() {
        // When/Then
        assertThatThrownBy(() -> validator.validateWebhookEvent("E123", "evt-123", null, Instant.now()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Event type is required");
    }

    @Test
    @DisplayName("Should reject webhook event with null timestamp")
    void shouldRejectNullTimestamp() {
        // When/Then
        assertThatThrownBy(() -> validator.validateWebhookEvent("E123", "evt-123", "CONFIRMED", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Occurred at timestamp is required");
    }

    @Test
    @DisplayName("Should reject webhook event with future timestamp")
    void shouldRejectFutureTimestamp() {
        // Given
        Instant future = Instant.now().plus(1, ChronoUnit.HOURS);

        // When/Then
        assertThatThrownBy(() -> validator.validateWebhookEvent("E123", "evt-123", "CONFIRMED", future))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be in the future");
    }

    @Test
    @DisplayName("Should validate and normalize CONFIRMED event type")
    void shouldValidateConfirmedEventType() {
        // When
        String normalized = validator.validateAndNormalizeEventType("confirmed");

        // Then
        assertThat(normalized).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("Should validate and normalize REJECTED event type")
    void shouldValidateRejectedEventType() {
        // When
        String normalized = validator.validateAndNormalizeEventType("rejected");

        // Then
        assertThat(normalized).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("Should validate and normalize PENDING event type")
    void shouldValidatePendingEventType() {
        // When
        String normalized = validator.validateAndNormalizeEventType("pending");

        // Then
        assertThat(normalized).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Should reject unsupported event type")
    void shouldRejectUnsupportedEventType() {
        // When/Then
        assertThatThrownBy(() -> validator.validateAndNormalizeEventType("INVALID"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported event type");
    }

    @Test
    @DisplayName("Should reject null event type")
    void shouldRejectNullEventTypeInNormalize() {
        // When/Then
        assertThatThrownBy(() -> validator.validateAndNormalizeEventType(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Event type is required");
    }

    @Test
    @DisplayName("Should reject blank event type")
    void shouldRejectBlankEventType() {
        // When/Then
        assertThatThrownBy(() -> validator.validateAndNormalizeEventType("  "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Event type is required");
    }
}
