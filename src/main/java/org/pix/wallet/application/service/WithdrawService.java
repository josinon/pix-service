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
    private final FundsValidator fundsValidator;
    private final MetricsService metricsService;

    public WithdrawService(WalletOperationValidator validator,
                           LedgerEntryRepositoryPort ledgerPort,
                           MetricsService metricsService,
                           FundsValidator fundsValidator) {
        this.validator = validator;
        this.ledgerPort = ledgerPort;
        this.metricsService = metricsService;
        this.fundsValidator = fundsValidator;
    }

   @Override
    @Transactional
    public Result execute(Command command) {
        // Validations using centralized validator
        validator.validateAmount(command.amount());
        validator.validateIdempotencyKey(command.idempotencyKey());
        
        Wallet wallet = validator.validateAndGetActiveWallet(command.walletId());

        // Idempotency shortcut
        if (ledgerPort.existsByIdempotencyKey(command.idempotencyKey())) {
            return new Result(wallet.id(), command.idempotencyKey());
        }

        // Business rule: no overdraft
    // Ensure sufficient funds (returns current balance for potential future metrics/logs)
    fundsValidator.ensureSufficientFunds(wallet.id(), command.amount());

        // Execute withdraw (will persist ledger entry)
        ledgerPort.withdraw(wallet.id().toString(), command.amount(), command.idempotencyKey());
        
        metricsService.recordWithdrawalCompleted();

        return new Result(wallet.id(), command.idempotencyKey());
    }
  
}
