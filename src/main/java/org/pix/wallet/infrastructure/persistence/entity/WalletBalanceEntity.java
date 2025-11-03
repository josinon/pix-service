package org.pix.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_balance")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletBalanceEntity {

  @Id
  @Column(name = "wallet_id")
  private UUID id; // mesmo valor do Wallet.id

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "wallet_id")
  private WalletEntity wallet;

  @Column(name = "balance_cents", nullable = false)
  private long balanceCents;

  @Column(nullable = false)
  private Instant updatedAt;

  @PrePersist
  @PreUpdate
  void touch() {
    if (updatedAt == null) updatedAt = Instant.now();
    else updatedAt = Instant.now();
  }
}
