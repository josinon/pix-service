package org.pix.wallet.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.domain.exception.InsufficientFundsException;
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
    public String deposit(String walletId, BigDecimal amount, String idempotencyKey) {

        WalletEntity wallet = walletRepo.findById(UUID.fromString(walletId))
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        LedgerEntryEntity e = new LedgerEntryEntity();
        e.setId(UUID.randomUUID());
        e.setWallet(wallet);
        e.setOperationType(OperationType.DEPOSIT);
        e.setAmount(amount);
        e.setCreatedAt(Instant.now());
        e.setIdempotencyKey(idempotencyKey);
        repo.save(e);
        return e.getId().toString();
    }

    @Override
    public String withdraw(String walletId, BigDecimal amount, String idempotencyKey) {
        WalletEntity wallet = walletRepo.findById(UUID.fromString(walletId))
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        BigDecimal current = repo.findAvailableBalance(wallet.getId())
            .orElse(BigDecimal.ZERO);
        if (current.compareTo(amount) < 0) {
            throw new InsufficientFundsException(current, amount);
        }

        LedgerEntryEntity e = new LedgerEntryEntity();
        e.setId(UUID.randomUUID());
        e.setWallet(wallet);
        e.setOperationType(OperationType.WITHDRAW);
        e.setAmount(amount);
        e.setCreatedAt(Instant.now());
        e.setIdempotencyKey(idempotencyKey);
        repo.save(e);
        return e.getId().toString();
    }

    @Override
    public Optional<BigDecimal> getBalanceAsOf(String walletId, Instant asOf) {
        return repo.findHistoricalBalance(UUID.fromString(walletId), asOf);
    }

    @Override
    public Optional<BigDecimal> getCurrentBalance(String walletId) {
        return repo.findCurrentBalanceByWalletId(UUID.fromString(walletId));
    }

    @Override
    public String reserve(String walletId, BigDecimal amount, String idempotencyKey) {
        WalletEntity wallet = walletRepo.findById(UUID.fromString(walletId))
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        BigDecimal available = repo.findAvailableBalance(wallet.getId())
            .orElse(BigDecimal.ZERO);
        if (available.compareTo(amount) < 0) {
            throw new InsufficientFundsException(available, amount);
        }

        LedgerEntryEntity e = new LedgerEntryEntity();
        e.setId(UUID.randomUUID());
        e.setWallet(wallet);
        e.setOperationType(OperationType.RESERVED);
        e.setAmount(amount);
        e.setCreatedAt(Instant.now());
        e.setIdempotencyKey(idempotencyKey);
        repo.save(e);
        return e.getId().toString();
    }

    @Override
    public String unreserve(String walletId, BigDecimal amount, String idempotencyKey) {
        WalletEntity wallet = walletRepo.findById(UUID.fromString(walletId))
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        LedgerEntryEntity e = new LedgerEntryEntity();
        e.setId(UUID.randomUUID());
        e.setWallet(wallet);
        e.setOperationType(OperationType.UNRESERVED);
        e.setAmount(amount);
        e.setCreatedAt(Instant.now());
        e.setIdempotencyKey(idempotencyKey);
        repo.save(e);
        return e.getId().toString();
    }

    @Override
    public Optional<BigDecimal> getAvailableBalance(String walletId) {
        return repo.findAvailableBalance(UUID.fromString(walletId));
    }
}