package org.pix.wallet.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.pix.wallet.application.port.out.WebhookInboxRepositoryPort;
import org.pix.wallet.infrastructure.persistence.entity.WebhookInboxEntity;
import org.pix.wallet.infrastructure.persistence.repository.WebhookInboxJpaRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebhookInboxRepositoryAdapter implements WebhookInboxRepositoryPort {

    private final WebhookInboxJpaRepository webhookInboxJpaRepository;

    @Override
    public boolean existsByEventId(String eventId) {
        return webhookInboxJpaRepository.existsByEventId(eventId);
    }

    @Override
    public void save(WebhookEvent event) {
        WebhookInboxEntity entity = WebhookInboxEntity.builder()
            .id(event.id())
            .endToEndId(event.endToEndId())
            .eventId(event.eventId())
            .eventType(event.eventType())
            .eventTime(event.occurredAt())
            .payloadHash(String.valueOf(event.eventId().hashCode()))
            .receivedAt(event.processedAt())
            .processed(true)
            .build();
        
        webhookInboxJpaRepository.save(entity);
    }
}
