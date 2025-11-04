package org.pix.wallet.application.service;

import java.math.BigDecimal;

import org.pix.wallet.application.port.in.DepositFundsUseCase;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.application.port.out.WalletBalanceRepositoryPort;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DepositService implements DepositFundsUseCase {

    private static final Logger log = LoggerFactory.getLogger(DepositService.class);

    private final WalletRepositoryPort walletPort;
    private final WalletBalanceRepositoryPort balancePort;
    private final LedgerEntryRepositoryPort ledgerPort;

    public DepositService(WalletRepositoryPort walletPort,
                          WalletBalanceRepositoryPort balancePort,
                          LedgerEntryRepositoryPort ledgerPort) {
        this.walletPort = walletPort;
        this.balancePort = balancePort;
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
            var current = balancePort.findCurrentBalance(wallet.id()).orElse(BigDecimal.ZERO);
            return new Result(wallet.id(), current, BigDecimal.ZERO, current);
        }

        BigDecimal previous = balancePort.findCurrentBalance(wallet.id()).orElse(BigDecimal.ZERO);
        ledgerPort.appendDeposit(wallet.id(), command.amount(), command.idempotencyKey());
        BigDecimal newBalance = balancePort.incrementBalance(wallet.id(), command.amount());

        return new Result(wallet.id(), previous, command.amount(), newBalance);
    }
}