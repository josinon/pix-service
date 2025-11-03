package org.pix.wallet.infrastructure.persistence.repository;

import org.pix.wallet.infrastructure.persistence.entity.WalletBalanceEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletBalanceJpaRepository extends JpaRepository<WalletBalanceEntity, UUID> {
  Optional<WalletBalanceEntity> findByWalletId(UUID walletId);

  // Para saque/transferência: bloqueia a linha do saldo (garante não-negativo sob concorrência)
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
  @Query("select wb from WalletBalanceEntity wb where wb.id = :walletId")
  Optional<WalletBalanceEntity> lockByWalletId(@Param("walletId") UUID walletId);
}