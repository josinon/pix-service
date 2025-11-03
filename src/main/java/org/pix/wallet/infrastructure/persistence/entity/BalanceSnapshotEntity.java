package org.pix.wallet.infrastructure.persistence.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "balance_snapshot", uniqueConstraints = {
    @UniqueConstraint(name = "uq_snapshot_wallet_time", columnNames = {"wallet_id","as_of"})
}, indexes = {
    @Index(name = "ix_snapshot_wallet_time", columnList = "wallet_id,as_of")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BalanceSnapshotEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "wallet_id", nullable = false)
  private WalletEntity wallet;

  @Column(name = "as_of", nullable = false)
  private Instant asOf;

  @Column(name = "balance_cents", nullable = false)
  private long balanceCents;

  @Column(nullable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    if (createdAt == null) createdAt = Instant.now();
  }
}