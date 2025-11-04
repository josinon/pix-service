package org.pix.wallet.presentation.api;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.pix.wallet.application.port.in.CreatePixKeyUseCase;
import org.pix.wallet.application.port.in.CreateWalletUseCase;
import org.pix.wallet.application.port.in.DepositUseCase;
import org.pix.wallet.application.port.in.GetBalanceUseCase;
import org.pix.wallet.application.port.in.WithdrawUseCase;
import org.pix.wallet.domain.model.Wallet;
import org.pix.wallet.presentation.dto.CreatePixKeyRequest;
import org.pix.wallet.presentation.dto.CreatePixKeyResponse;
import org.pix.wallet.presentation.dto.CreateWalletResponse;
import org.pix.wallet.presentation.dto.DepositRequest;
import org.pix.wallet.presentation.dto.DepositResponse;
import org.pix.wallet.presentation.dto.WithdrawRequest;
import org.pix.wallet.presentation.dto.WithdrawResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;


@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final CreateWalletUseCase createWallet;
    private final GetBalanceUseCase getBalance;
    private final CreatePixKeyUseCase createPixKeyUseCase;
    private final DepositUseCase depositUseCase;
    private final WithdrawUseCase withdrawUseCase;

    public WalletController(CreateWalletUseCase createWallet, GetBalanceUseCase getBalance, CreatePixKeyUseCase createPixKeyUseCase, DepositUseCase depositUseCase, WithdrawUseCase withdrawUseCase) {
        this.createWallet = createWallet;
        this.getBalance = getBalance;
        this.createPixKeyUseCase = createPixKeyUseCase;
        this.depositUseCase = depositUseCase;
        this.withdrawUseCase = withdrawUseCase;
    }

    @PostMapping
    public ResponseEntity<CreateWalletResponse> create() {
        Wallet w = createWallet.create();
        return ResponseEntity
            .created(URI.create("/wallets/"+w.id()))
            .body(new CreateWalletResponse(w.id()));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<?> balance(@PathVariable UUID id, @PathParam("at") Instant at) {
        return ResponseEntity.ok(getBalance.execute(new GetBalanceUseCase.Command(id, at)));
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

    @PostMapping("/{id}/deposit")
    public ResponseEntity<DepositResponse> deposit(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody DepositRequest body) {
        
        System.out.println("Deposit called with id=" + id + ", key=" + key + ", amount=" + body.amount());
        var r = depositUseCase.execute(new DepositUseCase.Command(id, body.amount(), key));
        return ResponseEntity.ok(new DepositResponse(r.walletId(), r.idempotencyKey()));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdraw(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody WithdrawRequest body) {
        var r = withdrawUseCase.execute(new WithdrawUseCase.Command(id, body.amount(), key));
        return ResponseEntity.ok(new WithdrawResponse(r.walletId(), r.idempotencyKey()));
    }
}
