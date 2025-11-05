package org.pix.wallet.application.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.pix.wallet.application.port.in.CreatePixKeyUseCase;
import org.pix.wallet.application.port.out.PixKeyRepositoryPort;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.pix.wallet.domain.model.PixKey;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.domain.model.enums.PixKeyStatus;
import org.pix.wallet.domain.model.enums.PixKeyType;
import org.pix.wallet.domain.validator.PixKeyValidator;

@Service
public class PixKeyService implements CreatePixKeyUseCase {

    private final WalletRepositoryPort walletPort;
    private final PixKeyRepositoryPort pixKeyPort;
    private final PixKeyValidator pixKeyValidator;

    public PixKeyService(
            WalletRepositoryPort walletPort, 
            PixKeyRepositoryPort pixKeyPort,
            PixKeyValidator pixKeyValidator) {
        this.walletPort = walletPort;
        this.pixKeyPort = pixKeyPort;
        this.pixKeyValidator = pixKeyValidator;
    }

    @Override
    @Transactional
    public CreatePixKeyResult execute(UUID walletId, CreatePixKeyCommand command) {
        Wallet wallet = walletPort.findById(walletId)
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        PixKeyType type = PixKeyType.valueOf(command.type().toUpperCase());
        String value = pixKeyValidator.normalizeAndGenerate(type, command.value());

        pixKeyValidator.validate(type, value);
        
        PixKey key = new PixKey(UUID.randomUUID(), wallet.id(), type, value, PixKeyStatus.ACTIVE, OffsetDateTime.now());
        PixKey saved = pixKeyPort.save(key);
        return new CreatePixKeyResult(saved.id(), saved.type().name(), saved.value(), saved.status().name());
    }
}
