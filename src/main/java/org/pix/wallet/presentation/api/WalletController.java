package org.pix.wallet.presentation.api;

import java.util.UUID;

import org.pix.wallet.application.port.in.CreateWalletUseCase;
import org.pix.wallet.application.port.in.GetBalanceUseCase;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.presentation.dto.CreateWalletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final CreateWalletUseCase createWallet;
    private final GetBalanceUseCase getBalance;

    public WalletController(CreateWalletUseCase createWallet, GetBalanceUseCase getBalance) {
        this.createWallet = createWallet;
        this.getBalance = getBalance;
    }

    @PostMapping
    public ResponseEntity<CreateWalletResponse> create() {
        Wallet w = createWallet.create();
        return ResponseEntity.ok(new CreateWalletResponse(w.id()));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<?> balance(@PathVariable UUID id) {
        return ResponseEntity.ok(getBalance.getBalance(id));
    }
}
