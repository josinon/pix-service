package org.pix.wallet.infrastructure.persistence.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.pix.wallet.domain.model.enums.WalletStatus;
@Data @Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "wallet")
public class WalletEntity {
  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private WalletStatus status = WalletStatus.ACTIVE;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Version
  private Integer version;

}