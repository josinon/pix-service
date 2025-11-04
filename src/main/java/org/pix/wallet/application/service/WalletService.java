package org.pix.wallet.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.pix.wallet.application.port.in.CreateWalletUseCase;
import org.pix.wallet.application.port.in.GetBalanceUseCase;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.domain.model.enums.WalletStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class WalletService implements CreateWalletUseCase, GetBalanceUseCase {

    private final WalletRepositoryPort walletRepository;

    public WalletService(WalletRepositoryPort walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public Wallet create() {
        Wallet wallet = Wallet.builder()
        .id(UUID.randomUUID())
        .status(WalletStatus.ACTIVE)
        .createdAt(Instant.now())
        .build();
        return walletRepository.save(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID walletId) {
        return BigDecimal.ZERO;
    }
}
