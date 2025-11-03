package org.pix.wallet.application.port.out;

import org.pix.wallet.domain.model.PixKey;

public interface PixKeyRepositoryPort {
    PixKey save(PixKey key);
    boolean existsByValue(String value);
}
