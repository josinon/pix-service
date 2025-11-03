package org.pix.wallet.infrastructure.persistence.repository;

import org.pix.wallet.infrastructure.persistence.entity.LedgerEntryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryEntity, Long> {
  // Paginação por carteira + ordenação temporal (suporta consulta histórica)
  Page<LedgerEntryEntity> findByWalletIdOrderByEffectiveAtAsc(UUID walletId, Pageable pageable);
  Page<LedgerEntryEntity> findByWalletIdAndEffectiveAtBetweenOrderByEffectiveAtAsc(UUID walletId, Instant from, Instant to, Pageable pageable);

  // Último lançamento (às vezes útil para debugging/explicabilidade)
  Optional<LedgerEntryEntity> findTopByWalletIdOrderByEffectiveAtDesc(UUID walletId);

  // Idempotência (depósitos/saques não-PIX)
  Optional<LedgerEntryEntity> findByWalletIdAndRequestId(UUID walletId, UUID requestId);

  // Soma de lançamentos em intervalo (para saldo histórico Δ)
  @Query("""
         select coalesce(sum(le.amountCents),0)
         from LedgerEntryEntity le
         where le.wallet.id = :walletId
           and le.effectiveAt > :fromTs
           and le.effectiveAt <= :toTs
         """)
  long sumDeltaInInterval(@Param("walletId") UUID walletId,
                          @Param("fromTs") Instant fromExclusive,
                          @Param("toTs") Instant toInclusive);
}
