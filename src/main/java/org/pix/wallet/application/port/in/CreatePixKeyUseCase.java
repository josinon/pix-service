package org.pix.wallet.application.port.in;

import java.util.UUID;

public interface CreatePixKeyUseCase {
    CreatePixKeyResult execute(UUID walletId, CreatePixKeyCommand command);

    record CreatePixKeyCommand(String type, String value) { }
    record CreatePixKeyResult(UUID id, String type, String value, String status) { }
}
