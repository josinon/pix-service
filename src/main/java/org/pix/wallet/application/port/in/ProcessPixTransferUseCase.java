package org.pix.wallet.application.port.in;

import java.math.BigDecimal;

public interface ProcessPixTransferUseCase {
    
    Result execute(Command command);
    
    record Command(
        String fromWalletId,
        String toPixKey,
        BigDecimal amount,
        String idempotencyKey
    ) {}
    
    record Result(
        String endToEndId,
        String status
    ) {}
}
