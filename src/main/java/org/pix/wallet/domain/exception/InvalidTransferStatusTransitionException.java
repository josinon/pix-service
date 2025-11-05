package org.pix.wallet.domain.exception;

import org.pix.wallet.domain.model.enums.TransferStatus;

/**
 * Thrown when an invalid transfer status transition is attempted.
 */
public class InvalidTransferStatusTransitionException extends IllegalStateException {
    private final TransferStatus from;
    private final TransferStatus to;

    public InvalidTransferStatusTransitionException(TransferStatus from, TransferStatus to) {
        super("Invalid transfer status transition: " + from + " -> " + to);
        this.from = from;
        this.to = to;
    }

    public TransferStatus getFrom() { return from; }
    public TransferStatus getTo() { return to; }
}
