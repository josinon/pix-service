package org.pix.wallet.infrastructure.persistence.repository;

import org.pix.wallet.infrastructure.persistence.entity.BalanceSnapshotEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BalanceSnapshotJpaRepository extends JpaRepository<BalanceSnapshotEntity, Long> {

  // Snapshot mais próximo (<= T) para compor saldo histórico
  Optional<BalanceSnapshotEntity> findTopByWalletIdAndAsOfLessThanEqualOrderByAsOfDesc(UUID walletId, Instant asOf);

  // Gerar/consultar snapshots em janelas
  Page<BalanceSnapshotEntity> findByWalletIdAndAsOfBetweenOrderByAsOfDesc(UUID walletId, Instant from, Instant to, Pageable pageable);
}
