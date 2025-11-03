package org.pix.wallet.infrastructure.persistence.repository;

import org.pix.wallet.infrastructure.persistence.entity.WebhookInboxEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookInboxJpaRepository extends JpaRepository<WebhookInboxEntity, Long> {
  boolean existsByEndToEndIdAndPayloadHash(String endToEndId, String payloadHash);

  // Útil para depuração/ordenamento de eventos recebidos
  Optional<WebhookInboxEntity> findTopByEndToEndIdOrderByEventTimeDesc(String endToEndId);

  Page<WebhookInboxEntity> findByEndToEndIdOrderByEventTimeDesc(String endToEndId, Pageable pageable);
}
