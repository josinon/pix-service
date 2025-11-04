package org.pix.wallet.application.service;


import org.pix.wallet.application.port.in.DepositUseCase;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositService implements DepositUseCase {

    private final WalletRepositoryPort walletPort;
    private final LedgerEntryRepositoryPort ledgerPort;

    public DepositService(WalletRepositoryPort walletPort,
                          LedgerEntryRepositoryPort ledgerPort) {
        this.walletPort = walletPort;
        this.ledgerPort = ledgerPort;
    }

   @Override
    @Transactional
    public Result execute(Command command) {
        if (command.amount() == null || command.amount().signum() <= 0)
            throw new IllegalArgumentException("Amount must be > 0");
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank())
            throw new IllegalArgumentException("Idempotency-Key required");

        var wallet = walletPort.findById(command.walletId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (ledgerPort.existsByIdempotencyKey(command.idempotencyKey())) {
            return new Result(wallet.id(), command.idempotencyKey());
        }

        ledgerPort.deposit(wallet.id().toString(), command.amount(), command.idempotencyKey());

        return new Result(wallet.id(), command.idempotencyKey());
    }
}