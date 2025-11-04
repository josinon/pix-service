package org.pix.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.pix.wallet.domain.model.enums.OperationType;

@Entity
@Table(name = "ledger_entry", indexes = {
    @Index(name = "ix_ledger_wallet_time", columnList = "wallet_id,effective_at"),
    @Index(name = "ix_ledger_wallet", columnList = "wallet_id"),
    @Index(name = "ix_ledger_request", columnList = "wallet_id,request_id", unique = true)
    // ATENÇÃO: idempotência condicional (wallet_id, request_id) WHERE request_id IS NOT NULL
    // precisa ser índice parcial feito via Flyway para comportar NULL corretamente.
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerEntryEntity {

  @Id
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "wallet_id", nullable = false)
  private WalletEntity wallet;

  @Enumerated(EnumType.STRING)
  @Column(name = "operation_type", nullable = false, length = 24)
  private OperationType operationType;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount; // positivo = crédito; negativo = débito

  @Column(name = "effective_at", nullable = false)
  private Instant effectiveAt;

  @Column(nullable = false)
  private Instant createdAt;

  // para idempotência
  @Column(name = "idempotency_key")
  private String idempotencyKey;

  @PrePersist
  void prePersist() {
    if (createdAt == null) createdAt = Instant.now();
    if (effectiveAt == null) effectiveAt = createdAt;
  }
}
