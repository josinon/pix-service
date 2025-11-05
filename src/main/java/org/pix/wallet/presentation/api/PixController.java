package org.pix.wallet.presentation.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pix.wallet.application.port.in.ProcessPixTransferUseCase;
import org.pix.wallet.application.port.in.ProcessPixWebhookUseCase;
import org.pix.wallet.presentation.dto.PixTransferRequest;
import org.pix.wallet.presentation.dto.PixTransferResponse;
import org.pix.wallet.presentation.dto.PixWebhookRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/pix")
@RequiredArgsConstructor
public class PixController {

    private final ProcessPixTransferUseCase processPixTransferUseCase;
    private final ProcessPixWebhookUseCase processPixWebhookUseCase;

    @PostMapping("/transfers")
    public ResponseEntity<PixTransferResponse> createTransfer(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody PixTransferRequest request) {
        
        log.info("Creating PIX transfer from wallet {} to PIX key {} with idempotency key {}", 
                 request.fromWalletId(), request.toPixKey(), idempotencyKey);
        
        var command = new ProcessPixTransferUseCase.Command(
            request.fromWalletId().toString(),
            request.toPixKey(),
            request.amount(),
            idempotencyKey
        );
        
        ProcessPixTransferUseCase.Result result = processPixTransferUseCase.execute(command);
        
        PixTransferResponse response = PixTransferResponse.builder()
            .endToEndId(result.endToEndId())
            .status(result.status())
            .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveWebhook(@Valid @RequestBody PixWebhookRequest request) {
        
        log.info("Received PIX webhook - endToEndId: {}, eventId: {}, eventType: {}", 
                 request.endToEndId(), request.eventId(), request.eventType());
        
        var command = new ProcessPixWebhookUseCase.Command(
            request.endToEndId(),
            request.eventId(),
            request.eventType(),
            request.occurredAt()
        );
        
        processPixWebhookUseCase.execute(command);
        
        return ResponseEntity.ok().build();
    }
}
