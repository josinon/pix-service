package org.pix.wallet.domain.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.pix.wallet.domain.exception.InvalidTransferStatusTransitionException;
import org.pix.wallet.domain.model.enums.TransferStatus;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TransferStatusTransitionValidator Tests")
class TransferStatusTransitionValidatorTest {

    private final TransferStatusTransitionValidator validator = new TransferStatusTransitionValidator();

    @Test
    @DisplayName("Should allow PENDING -> CONFIRMED")
    void shouldAllowPendingToConfirmed() {
        assertThatCode(() -> validator.validate(TransferStatus.PENDING, TransferStatus.CONFIRMED))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow PENDING -> REJECTED")
    void shouldAllowPendingToRejected() {
        assertThatCode(() -> validator.validate(TransferStatus.PENDING, TransferStatus.REJECTED))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow idempotent PENDING -> PENDING")
    void shouldAllowPendingToPending() {
        assertThatCode(() -> validator.validate(TransferStatus.PENDING, TransferStatus.PENDING))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject CONFIRMED -> REJECTED")
    void shouldRejectConfirmedToRejected() {
        assertThatThrownBy(() -> validator.validate(TransferStatus.CONFIRMED, TransferStatus.REJECTED))
            .isInstanceOf(InvalidTransferStatusTransitionException.class)
            .hasMessageContaining("CONFIRMED -> REJECTED");
    }

    @Test
    @DisplayName("Should reject REJECTED -> CONFIRMED")
    void shouldRejectRejectedToConfirmed() {
        assertThatThrownBy(() -> validator.validate(TransferStatus.REJECTED, TransferStatus.CONFIRMED))
            .isInstanceOf(InvalidTransferStatusTransitionException.class)
            .hasMessageContaining("REJECTED -> CONFIRMED");
    }

    @Test
    @DisplayName("Should reject CONFIRMED -> PENDING")
    void shouldRejectConfirmedToPending() {
        assertThatThrownBy(() -> validator.validate(TransferStatus.CONFIRMED, TransferStatus.PENDING))
            .isInstanceOf(InvalidTransferStatusTransitionException.class)
            .hasMessageContaining("CONFIRMED -> PENDING");
    }

    @Test
    @DisplayName("Should reject REJECTED -> PENDING")
    void shouldRejectRejectedToPending() {
        assertThatThrownBy(() -> validator.validate(TransferStatus.REJECTED, TransferStatus.PENDING))
            .isInstanceOf(InvalidTransferStatusTransitionException.class)
            .hasMessageContaining("REJECTED -> PENDING");
    }
}
