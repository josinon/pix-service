package org.pix.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_inbox", indexes = {
    @Index(name = "ix_webhook_e2e_time", columnList = "end_to_end_id, event_time"),
    @Index(name = "uq_webhook_inbox_e2e", columnList = "end_to_end_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookInboxEntity {

    @Id
    private UUID id;

    @Column(name = "end_to_end_id", nullable = false, unique = true)
    private String endToEndId;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

}
