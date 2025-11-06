package org.pix.wallet.application.service;

import java.time.Instant;
import java.util.UUID;

import org.pix.wallet.application.port.in.CreateWalletUseCase;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.domain.model.enums.WalletStatus;
import org.pix.wallet.infrastructure.observability.MetricsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class WalletService implements CreateWalletUseCase {

    private final WalletRepositoryPort walletRepository;
    private final MetricsService metricsService;

    public WalletService(WalletRepositoryPort walletRepository, MetricsService metricsService) {
        this.walletRepository = walletRepository;
        this.metricsService = metricsService;
    }

    @Override
    public Wallet create() {
        Wallet wallet = Wallet.builder()
        .id(UUID.randomUUID())
        .status(WalletStatus.ACTIVE)
        .createdAt(Instant.now())
        .build();
        
        Wallet savedWallet = walletRepository.save(wallet);
        
        metricsService.recordWalletCreated();
        
        return savedWallet;
    }


}
