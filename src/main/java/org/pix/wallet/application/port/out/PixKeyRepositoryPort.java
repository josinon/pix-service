package org.pix.wallet.application.port.out;

import org.pix.wallet.domain.model.PixKey;
import java.util.Optional;

public interface PixKeyRepositoryPort {
    PixKey save(PixKey key);
    boolean existsByValue(String value);
    Optional<PixKey> findByValueAndActive(String value);
}
