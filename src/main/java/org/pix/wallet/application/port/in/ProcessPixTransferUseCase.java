package org.pix.wallet.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProcessPixTransferUseCase {
    
    Result execute(Command command);
    
    record Command(
        UUID fromWalletId,
        String toPixKey,
        BigDecimal amount,
        String idempotencyKey
    ) {}
    
    record Result(
        String endToEndId,
        String status
    ) {}
}
