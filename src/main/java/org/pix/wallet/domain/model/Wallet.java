package org.pix.wallet.domain.model;

import java.time.Instant;
import java.util.UUID;

import org.pix.wallet.domain.model.enums.WalletStatus;

import lombok.Builder;

@Builder
public class Wallet {
    private final UUID id;
    private Instant createdAt;
    private WalletStatus status;

    public UUID id() { return id; }
    public WalletStatus status() { return status; }
    public Instant createdAt() { return createdAt; }

}
