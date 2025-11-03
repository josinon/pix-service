package org.pix.wallet.application.port.in;

import org.pix.wallet.domain.model.Wallet;

public interface CreateWalletUseCase {
    Wallet create();
}