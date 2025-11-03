package org.pix.wallet.presentation.api;

import java.util.UUID;

import org.pix.wallet.application.port.in.CreatePixKeyUseCase;
import org.pix.wallet.application.port.in.CreateWalletUseCase;
import org.pix.wallet.application.port.in.GetBalanceUseCase;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.presentation.dto.CreatePixKeyRequest;
import org.pix.wallet.presentation.dto.CreatePixKeyResponse;
import org.pix.wallet.presentation.dto.CreateWalletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final CreateWalletUseCase createWallet;
    private final GetBalanceUseCase getBalance;
    private final CreatePixKeyUseCase createPixKeyUseCase;

    public WalletController(CreateWalletUseCase createWallet, GetBalanceUseCase getBalance, CreatePixKeyUseCase createPixKeyUseCase) {
        this.createWallet = createWallet;
        this.getBalance = getBalance;
        this.createPixKeyUseCase = createPixKeyUseCase;
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

    @PostMapping("/{id}/pix-keys")
    public ResponseEntity<CreatePixKeyResponse> createPixKey(
            @PathVariable("id") UUID walletId,
            @Valid @RequestBody CreatePixKeyRequest body) {

        var result = createPixKeyUseCase.execute(
            walletId,
            new CreatePixKeyUseCase.CreatePixKeyCommand(body.type(), body.value())
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new CreatePixKeyResponse(result.id(), result.type(), result.value(), result.status()));
    }
}
