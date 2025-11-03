package org.pix.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "webhook_inbox", uniqueConstraints = {
    @UniqueConstraint(name = "uq_webhook_e2e_hash", columnNames = {"end_to_end_id","payload_hash"})
}, indexes = {
    @Index(name = "ix_webhook_e2e_time", columnList = "end_to_end_id,event_time DESC")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebhookInboxEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "end_to_end_id", nullable = false)
  private String endToEndId;

  @Column(name = "event_type", nullable = false, length = 16)
  private String eventType; // "CONFIRMED" | "REJECTED" (pode virar enum se preferir)

  @Column(name = "event_time", nullable = false)
  private Instant eventTime;

  @Column(name = "payload_hash", nullable = false)
  private String payloadHash;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  @Column(nullable = false)
  private boolean processed = false;

  // opcional: referenciar Transfer por end_to_end_id (FK lógica)
  // @ManyToOne(fetch = FetchType.LAZY)
  // @JoinColumn(name = "end_to_end_id", referencedColumnName = "end_to_end_id", insertable = false, updatable = false)
  // private TransferEntity transfer;

  @PrePersist
  void prePersist() {
    if (receivedAt == null) receivedAt = Instant.now();
    if (eventTime == null) eventTime = receivedAt;
  }
}
