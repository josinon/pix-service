package org.pix.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "webhook_inbox", indexes = {
    @Index(name = "ix_webhook_e2e_time", columnList = "end_to_end_id, event_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookInboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "end_to_end_id", nullable = false)
    private String endToEndId;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "payload_hash", nullable = false)
    private String payloadHash;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Builder.Default
    @Column(name = "processed", nullable = false)
    private Boolean processed = false;

    @PrePersist
    void prePersist() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
        if (payloadHash == null) {
            // Use eventId as hash if not provided
            payloadHash = String.valueOf(eventId.hashCode());
        }
    }
}
