package org.pix.wallet.application.service;

import org.pix.wallet.application.port.in.WithdrawUseCase;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.infrastructure.observability.MetricsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WithdrawService implements WithdrawUseCase {

    private final WalletOperationValidator validator;
    private final LedgerEntryRepositoryPort ledgerPort;
    private final MetricsService metricsService;

    public WithdrawService(WalletOperationValidator validator,
                          LedgerEntryRepositoryPort ledgerPort,
                          MetricsService metricsService) {
        this.validator = validator;
        this.ledgerPort = ledgerPort;
        this.metricsService = metricsService;
    }

   @Override
    @Transactional
    public Result execute(Command command) {
        // Validations using centralized validator
        validator.validateAmount(command.amount());
        validator.validateIdempotencyKey(command.idempotencyKey());
        
        Wallet wallet = validator.validateAndGetActiveWallet(command.walletId());

        // Check idempotency
        if (ledgerPort.existsByIdempotencyKey(command.idempotencyKey())) {
            return new Result(wallet.id(), command.idempotencyKey());
        }

        // Execute withdraw
        ledgerPort.withdraw(wallet.id().toString(), command.amount(), command.idempotencyKey());
        
        metricsService.recordWithdrawalCompleted();

        return new Result(wallet.id(), command.idempotencyKey());
    }
  
}
