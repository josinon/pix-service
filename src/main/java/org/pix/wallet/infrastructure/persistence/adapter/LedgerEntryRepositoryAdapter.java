package org.pix.wallet.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.domain.model.enums.OperationType;
import org.pix.wallet.infrastructure.persistence.entity.LedgerEntryEntity;
import org.pix.wallet.infrastructure.persistence.entity.WalletEntity;
import org.pix.wallet.infrastructure.persistence.repository.LedgerEntryJpaRepository;
import org.pix.wallet.infrastructure.persistence.repository.WalletJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class LedgerEntryRepositoryAdapter implements LedgerEntryRepositoryPort {

    private final LedgerEntryJpaRepository repo;
    private final WalletJpaRepository walletRepo;

    public LedgerEntryRepositoryAdapter(LedgerEntryJpaRepository repo, WalletJpaRepository walletRepo) {
        this.repo = repo;
        this.walletRepo = walletRepo;
    }

    @Override
    public boolean existsByIdempotencyKey(String key) {
        return repo.existsByIdempotencyKey(key);
    }

    @Override
    public UUID deposit(UUID walletId, BigDecimal amount, String idempotencyKey) {

        WalletEntity wallet = walletRepo.findById(walletId)
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        LedgerEntryEntity e = new LedgerEntryEntity();
        e.setId(UUID.randomUUID());
        e.setWallet(wallet);
        e.setOperationType(OperationType.DEPOSIT);
        e.setAmount(amount);
        e.setCreatedAt(Instant.now());
        e.setIdempotencyKey(idempotencyKey);
        repo.save(e);
        return e.getId();
    }

    @Override
    public UUID withdraw(UUID walletId, BigDecimal amount, String idempotencyKey) {
        WalletEntity wallet = walletRepo.findById(walletId)
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        LedgerEntryEntity e = new LedgerEntryEntity();
        e.setId(UUID.randomUUID());
        e.setWallet(wallet);
        e.setOperationType(OperationType.WITHDRAW);
        e.setAmount(amount);
        e.setCreatedAt(Instant.now());
        e.setIdempotencyKey(idempotencyKey);
        repo.save(e);
        return e.getId();
    }

    @Override
    public Optional<BigDecimal> getBalanceAsOf(UUID walletId, Instant asOf) {
        return repo.findHistoricalBalance(walletId, asOf);
    }

    @Override
    public Optional<BigDecimal> getCurrentBalance(UUID walletId) {
        return repo.findCurrentBalanceByWalletId(walletId);
    }
}