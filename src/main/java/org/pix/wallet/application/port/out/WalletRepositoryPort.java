package org.pix.wallet.application.port.out;

import java.util.Optional;
import java.util.UUID;

import org.pix.wallet.domain.model.Wallet;

public interface WalletRepositoryPort {
    Wallet save(Wallet wallet);
    Optional<Wallet> findById(UUID id);
}
