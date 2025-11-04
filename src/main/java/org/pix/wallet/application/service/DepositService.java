package org.pix.wallet.application.service;


import org.pix.wallet.application.port.in.DepositUseCase;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.domain.model.Wallet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositService implements DepositUseCase {

    private final WalletOperationValidator validator;
    private final LedgerEntryRepositoryPort ledgerPort;

    public DepositService(WalletOperationValidator validator,
                          LedgerEntryRepositoryPort ledgerPort) {
        this.validator = validator;
        this.ledgerPort = ledgerPort;
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

        // Execute deposit
        ledgerPort.deposit(wallet.id().toString(), command.amount(), command.idempotencyKey());

        return new Result(wallet.id(), command.idempotencyKey());
    }
}