package org.pix.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

import org.pix.wallet.domain.model.enums.TransferStatus;

@Entity
@Table(name = "transfer", indexes = {
    @Index(name = "ix_transfer_from", columnList = "from_wallet_id"),
    @Index(name = "ix_transfer_to", columnList = "to_wallet_id"),
    @Index(name = "uq_transfer_e2e", columnList = "end_to_end_id", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransferEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "end_to_end_id", nullable = false, unique = true)
  private String endToEndId;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "from_wallet_id", nullable = false)
  private WalletEntity fromWallet;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "to_wallet_id", nullable = false)
  private WalletEntity toWallet;

  @Column(name = "amount_cents", nullable = false)
  private long amountCents;

  @Column(length = 3, nullable = false)
  private String currency = "BRL";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private TransferStatus status = TransferStatus.PENDING;

  private String reasonCode; // opcional (ex.: INSUFFICIENT_FUNDS)

  @Column(nullable = false)
  private Instant initiatedAt;

  private Instant appliedAt;

  // Usa @Version para controle otimista de concorrência (webhooks fora de ordem)
  @Version
  @Column(nullable = false)
  private int version;

  @Column(nullable = false)
  private Instant updatedAt;

  @PrePersist
  void prePersist() {
    if (initiatedAt == null) initiatedAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }
}
