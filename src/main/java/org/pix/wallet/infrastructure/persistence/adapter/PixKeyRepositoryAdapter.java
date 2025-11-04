package org.pix.wallet.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;

import java.util.Optional;

import org.pix.wallet.application.port.out.PixKeyRepositoryPort;
import org.pix.wallet.domain.model.PixKey;
import org.pix.wallet.infrastructure.persistence.entity.PixKeyEntity;
import org.pix.wallet.infrastructure.persistence.entity.WalletEntity;
import org.pix.wallet.infrastructure.persistence.repository.PixKeyJpaRepository;
import org.pix.wallet.infrastructure.persistence.repository.WalletJpaRepository;

@Component
public class PixKeyRepositoryAdapter implements PixKeyRepositoryPort {

    private final PixKeyJpaRepository pixKeyJpa;
    private final WalletJpaRepository walletJpa;

    public PixKeyRepositoryAdapter(PixKeyJpaRepository pixKeyJpa, WalletJpaRepository walletJpa) {
        this.pixKeyJpa = pixKeyJpa;
        this.walletJpa = walletJpa;
    }

    @Override
    public PixKey save(PixKey key) {

        Optional<WalletEntity> wallet = walletJpa.findById(key.walletId());

        PixKeyEntity e = PixKeyEntity.builder().id(key.id()).wallet(wallet.orElseThrow()).type(key.type())
        .value(key.value()).status(key.status()).createdAt(key.createdAt()).build();
        
        PixKeyEntity saved = pixKeyJpa.save(e);
        return new PixKey(saved.getId(), saved.getWallet().getId(), saved.getType(),
                saved.getValue(), saved.getStatus(), saved.getCreatedAt());
    }

    @Override
    public boolean existsByValue(String value) {
        return pixKeyJpa.existsByValue(value);
    }

    @Override
    public Optional<PixKey> findByValueAndActive(String value) {
        return pixKeyJpa.findByValueAndStatus(value, org.pix.wallet.domain.model.enums.PixKeyStatus.ACTIVE)
            .map(entity -> new PixKey(
                entity.getId(),
                entity.getWallet().getId(),
                entity.getType(),
                entity.getValue(),
                entity.getStatus(),
                entity.getCreatedAt()
            ));
    }
}
