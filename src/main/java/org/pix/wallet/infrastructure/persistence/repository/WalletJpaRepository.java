package org.pix.wallet.infrastructure.persistence.repository;

import org.pix.wallet.infrastructure.persistence.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface WalletJpaRepository extends JpaRepository<WalletEntity, UUID> {
}