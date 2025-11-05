package org.pix.wallet.domain.exception;

import java.math.BigDecimal;

/**
 * Domain exception thrown when a withdrawal or transfer is attempted with insufficient funds.
 */
public class InsufficientFundsException extends IllegalArgumentException {
    private final BigDecimal available;
    private final BigDecimal requested;

    public InsufficientFundsException(BigDecimal available, BigDecimal requested) {
        super("Insufficient balance. Available: " + available + ", Requested: " + requested);
        this.available = available;
        this.requested = requested;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getRequested() {
        return requested;
    }
}
