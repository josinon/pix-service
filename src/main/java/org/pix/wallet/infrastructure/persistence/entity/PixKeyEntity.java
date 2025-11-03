package org.pix.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.pix.wallet.domain.model.enums.PixKeyStatus;
import org.pix.wallet.domain.model.enums.PixKeyType;


@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Data
@Entity
@Table(name = "pix_key",
  uniqueConstraints = @UniqueConstraint(name = "uq_pix_active", columnNames = {"type","value","status"}))
public class PixKeyEntity {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "wallet_id")
  private WalletEntity wallet;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PixKeyType type;

  @Column(name = "value", nullable = false, length = 255)
  private String value;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PixKeyStatus status = PixKeyStatus.ACTIVE;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name = "revoked_at", nullable = true)
  private OffsetDateTime revokedAt;

}