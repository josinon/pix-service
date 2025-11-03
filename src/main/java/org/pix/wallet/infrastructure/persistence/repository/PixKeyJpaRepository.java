package org.pix.wallet.infrastructure.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.pix.wallet.infrastructure.persistence.entity.PixKeyEntity;

public interface PixKeyJpaRepository extends JpaRepository<PixKeyEntity, UUID> {
    boolean existsByValue(String value);
}