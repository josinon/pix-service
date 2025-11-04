package org.pix.wallet.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.pix.wallet.application.port.out.WalletBalanceRepositoryPort;
import org.pix.wallet.infrastructure.persistence.entity.WalletBalanceEntity;
import org.pix.wallet.infrastructure.persistence.repository.WalletBalanceJpaRepository;
import org.pix.wallet.infrastructure.persistence.repository.WalletJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WalletBalanceRepositoryAdapter implements WalletBalanceRepositoryPort {

    private final WalletBalanceJpaRepository repo;
    private final WalletJpaRepository walletRepo;

    public WalletBalanceRepositoryAdapter(WalletBalanceJpaRepository repo, WalletJpaRepository walletRepo) {
        this.repo = repo;
        this.walletRepo = walletRepo;
    }

    @Override
    public Optional<BigDecimal> findCurrentBalance(UUID walletId) {
        return repo.findByWalletId(walletId).map(WalletBalanceEntity::getBalance);
    }

    @Override
    @Transactional
    public BigDecimal incrementBalance(UUID walletId, BigDecimal amount) {
        if (amount.signum() <= 0) throw new IllegalArgumentException("Amount must be positive");
        walletRepo.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        return repo.upsertAndIncrement(walletId, amount);
    }
}