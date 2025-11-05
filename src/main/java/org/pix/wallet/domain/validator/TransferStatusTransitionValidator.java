package org.pix.wallet.domain.validator;

import org.pix.wallet.domain.exception.InvalidTransferStatusTransitionException;
import org.pix.wallet.domain.model.enums.TransferStatus;
import org.springframework.stereotype.Component;

/**
 * Validates permissible status transitions for a transfer.
 */
@Component
public class TransferStatusTransitionValidator {

    public void validate(TransferStatus current, TransferStatus target) {
        if (!current.canTransitionTo(target)) {
            throw new InvalidTransferStatusTransitionException(current, target);
        }
    }
}
