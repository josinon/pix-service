package org.pix.wallet.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.pix.wallet.domain.model.enums.PixKeyStatus;
import org.pix.wallet.domain.model.enums.PixKeyType;

public class PixKey {
    private final UUID id;
    private final UUID walletId;
    private final PixKeyType type;
    private final String value;
    private PixKeyStatus status;
    private final OffsetDateTime createdAt;

    public PixKey(UUID id, UUID walletId, PixKeyType type, String value, PixKeyStatus status, OffsetDateTime createdAt) {
        this.id = id;
        this.walletId = walletId;
        this.type = type;
        this.value = value;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID id() { return id; }
    public UUID walletId() { return walletId; }
    public PixKeyType type() { return type; }
    public String value() { return value; }
    public PixKeyStatus status() { return status; }
    public OffsetDateTime createdAt() { return createdAt; }

    public void deactivate() { if (status == PixKeyStatus.ACTIVE) status = PixKeyStatus.REVOKED; }
}