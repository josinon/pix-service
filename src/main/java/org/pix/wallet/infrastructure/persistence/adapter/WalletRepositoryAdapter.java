package org.pix.wallet.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.infrastructure.persistence.entity.WalletEntity;
import org.pix.wallet.infrastructure.persistence.repository.WalletJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class WalletRepositoryAdapter implements WalletRepositoryPort {

    private final WalletJpaRepository jpa;

    public WalletRepositoryAdapter(WalletJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Wallet save(Wallet wallet) {
        WalletEntity entity = WalletEntity.builder()
                .id(wallet.id())
                .status(wallet.status())
                .createdAt(wallet.createdAt())
                .build();
        jpa.save(entity);
        return wallet;
    }

    @Override
    public Optional<Wallet> findById(UUID id) {
        return jpa.findById(id)
                .map(e -> Wallet.builder()
                        .id(e.getId())
                        .build());
    }
}