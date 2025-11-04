package org.pix.wallet.infrastructure.persistence.repository;

import org.pix.wallet.infrastructure.persistence.entity.WebhookInboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WebhookInboxJpaRepository extends JpaRepository<WebhookInboxEntity, UUID> {
    boolean existsByEventId(String eventId);
}
