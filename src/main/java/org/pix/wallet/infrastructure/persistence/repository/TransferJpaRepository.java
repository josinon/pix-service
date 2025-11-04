package org.pix.wallet.infrastructure.persistence.repository;

import org.pix.wallet.domain.model.enums.TransferStatus;
import org.pix.wallet.infrastructure.persistence.entity.TransferEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferJpaRepository extends JpaRepository<TransferEntity, UUID> {
  Optional<TransferEntity> findByEndToEndId(String endToEndId);
  Optional<TransferEntity> findByIdempotencyKey(String idempotencyKey);

  Page<TransferEntity> findByFromWalletIdOrderByInitiatedAtDesc(UUID walletId, Pageable pageable);
  Page<TransferEntity> findByToWalletIdOrderByInitiatedAtDesc(UUID walletId, Pageable pageable);

  List<TransferEntity> findByStatus(TransferStatus status);

  // Lock otimista já é tratado por @Version em TransferEntity; quando precisar lock pessimista:
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
  @Query("select t from TransferEntity t where t.id = :id")
  Optional<TransferEntity> lockById(@Param("id") UUID id);
}
