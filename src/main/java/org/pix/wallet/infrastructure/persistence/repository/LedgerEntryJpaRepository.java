package org.pix.wallet.infrastructure.persistence.repository;

import java.util.UUID;
import org.pix.wallet.infrastructure.persistence.entity.LedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryEntity, UUID> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}