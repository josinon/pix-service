package org.pix.wallet.infrastructure.persistence.repository;

import org.pix.wallet.infrastructure.persistence.entity.WebhookInboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookInboxJpaRepository extends JpaRepository<WebhookInboxEntity, UUID> {
    boolean existsByEventId(String eventId);
}
