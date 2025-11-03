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

@Service
public class PixKeyService implements CreatePixKeyUseCase {

    private final WalletRepositoryPort walletPort;
    private final PixKeyRepositoryPort pixKeyPort;

    public PixKeyService(WalletRepositoryPort walletPort, PixKeyRepositoryPort pixKeyPort) {
        this.walletPort = walletPort;
        this.pixKeyPort = pixKeyPort;
    }

    @Override
    @Transactional
    public CreatePixKeyResult execute(UUID walletId, CreatePixKeyCommand command) {
        Wallet wallet = walletPort.findById(walletId)
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        PixKeyType type = PixKeyType.valueOf(command.type().toUpperCase());
        String value = normalizeAndGenerate(type, command.value());

        validate(type, value);
        if (pixKeyPort.existsByValue(value)) {
            throw new IllegalArgumentException("Pix key already exists");
        }

        PixKey key = new PixKey(UUID.randomUUID(), wallet.id(), type, value, PixKeyStatus.ACTIVE, OffsetDateTime.now());
        PixKey saved = pixKeyPort.save(key);
        return new CreatePixKeyResult(saved.id(), saved.type().name(), saved.value(), saved.status().name());
    }

    private String normalizeAndGenerate(PixKeyType type, String value) {
        if (type == PixKeyType.RANDOM) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return value == null ? "" : value.trim();
    }

    private void validate(PixKeyType type, String value) {
        switch (type) {
            case CPF -> {
                if (!value.matches("\\d{11}")) throw new IllegalArgumentException("Invalid CPF");
            }
            case EMAIL -> {
                if (!value.matches(".+@.+\\..+")) throw new IllegalArgumentException("Invalid email");
            }
            case PHONE -> {
                if (!value.matches("\\+\\d{11,14}")) throw new IllegalArgumentException("Invalid phone");
            }
            case RANDOM -> { /* generated */ }
            default -> throw new IllegalArgumentException("Unsupported type");
        }
    }
}
