package org.pix.wallet.application.service;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pix.wallet.application.port.in.ProcessPixTransferUseCase;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.application.port.out.PixKeyRepositoryPort;
import org.pix.wallet.application.port.out.TransferRepositoryPort;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.pix.wallet.domain.model.PixKey;
import org.pix.wallet.infrastructure.observability.ObservabilityContext;
import org.pix.wallet.infrastructure.observability.MetricsService;
import org.pix.wallet.infrastructure.observability.Traced;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Service
@RequiredArgsConstructor
public class PixTransferService implements ProcessPixTransferUseCase {

    private final WalletRepositoryPort walletRepositoryPort;
    private final PixKeyRepositoryPort pixKeyRepositoryPort;
    private final TransferRepositoryPort transferRepositoryPort;
    private final LedgerEntryRepositoryPort ledgerEntryRepositoryPort;
    private final MetricsService metricsService;
    private final FundsValidator fundsValidator;
    
    @Override
    @Traced(operation = "pix.transfer.create", description = "Create PIX transfer")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Result execute(Command command) {
        ObservabilityContext.setOperation("PIX_TRANSFER_CREATE");
        ObservabilityContext.setWalletId(UUID.fromString(command.fromWalletId()));
        
        Timer.Sample metricsTimer = metricsService.startTransferCreation();
        
        try {
            log.info("Initiating PIX transfer", 
                     kv("fromWallet", command.fromWalletId()),
                     kv("toPixKey", command.toPixKey()),
                     kv("amount", command.amount()),
                     kv("idempotencyKey", command.idempotencyKey()));
            
            validateCommand(command);
            
            if (transferRepositoryPort.existsByIdempotencyKey(command.idempotencyKey())) {
                log.info("Transfer already processed (idempotency check)", 
                         kv("idempotencyKey", command.idempotencyKey()),
                         kv("reason", "duplicate_request"));
                
                var existingTransfer = transferRepositoryPort.findByIdempotencyKey(command.idempotencyKey())
                    .orElseThrow(() -> new IllegalStateException("Transfer exists but not found"));
                
                ObservabilityContext.setEndToEndId(existingTransfer.endToEndId());
                
                log.debug("Returning existing transfer", 
                          kv("endToEndId", existingTransfer.endToEndId()),
                          kv("status", existingTransfer.status()));
                
                return new Result(existingTransfer.endToEndId(), existingTransfer.status());
            }
            
            var sourceWallet = walletRepositoryPort.findById(UUID.fromString(command.fromWalletId()))
                .orElseThrow(() -> {
                    log.error("Source wallet not found", 
                              kv("walletId", command.fromWalletId()),
                              kv("errorType", "wallet_not_found"));
                    return new IllegalArgumentException("Source wallet not found: " + command.fromWalletId());
                });
            
            log.debug("Source wallet validated", 
                      kv("walletId", sourceWallet.id()),
                      kv("walletStatus", "active"));
            
            PixKey pixKey = pixKeyRepositoryPort.findByValueAndActive(command.toPixKey())
                .orElseThrow(() -> {
                    log.error("PIX key not found or inactive", 
                              kv("pixKey", command.toPixKey()),
                              kv("errorType", "pix_key_not_found"));
                    return new IllegalArgumentException("PIX key not found or inactive: " + command.toPixKey());
                });
            
            log.debug("PIX key resolved", 
                      kv("pixKey", command.toPixKey()),
                      kv("pixKeyType", pixKey.type()),
                      kv("destinationWallet", pixKey.walletId()));
            
            var destinationWallet = walletRepositoryPort.findById(pixKey.walletId())
                .orElseThrow(() -> {
                    log.error("Destination wallet not found for PIX key", 
                              kv("pixKey", command.toPixKey()),
                              kv("walletId", pixKey.walletId()),
                              kv("errorType", "destination_wallet_not_found"));
                    return new IllegalArgumentException("Destination wallet not found for PIX key: " + command.toPixKey());
                });
            
            if (sourceWallet.id().equals(destinationWallet.id())) {
                log.error("Attempt to transfer to same wallet", 
                          kv("walletId", sourceWallet.id()),
                          kv("errorType", "same_wallet_transfer"));
                throw new IllegalArgumentException("Cannot transfer to the same wallet");
            }
            
            log.debug("Destination wallet validated", 
                      kv("destinationWallet", destinationWallet.id()),
                      kv("differentWallets", true));
            
            BigDecimal availableBalance = fundsValidator.ensureSufficientFunds(UUID.fromString(command.fromWalletId()), command.amount());
            
            log.debug("Balance validated", 
                      kv("availableBalance", availableBalance),
                      kv("transferAmount", command.amount()),
                      kv("remainingAfterReserve", availableBalance.subtract(command.amount())));
            
            String endToEndId = generateEndToEndId();
            ObservabilityContext.setEndToEndId(endToEndId);
            
            log.debug("Generated End-to-End ID", 
                      kv("endToEndId", endToEndId));
            
            String reserveIdempotencyKey = endToEndId + "-reserve";
            ledgerEntryRepositoryPort.reserve(
                command.fromWalletId(),
                command.amount(),
                reserveIdempotencyKey
            );
            
            log.info("Funds reserved for transfer", 
                     kv("endToEndId", endToEndId),
                     kv("amount", command.amount()),
                     kv("walletId", command.fromWalletId()));
            
            var transferCommand = new TransferRepositoryPort.TransferCommand(
                endToEndId,
                command.fromWalletId(),
                destinationWallet.id().toString(),
                command.amount(),
                "BRL",
                "PENDING",
                command.idempotencyKey()
            );
            
            TransferRepositoryPort.TransferResult transfer = transferRepositoryPort.save(transferCommand);
            
            metricsService.recordTransferCreated();
            metricsService.recordTransferCreation(metricsTimer);
            
            log.info("PIX transfer created successfully", 
                     kv("endToEndId", transfer.endToEndId()),
                     kv("status", transfer.status()),
                     kv("fromWallet", transfer.fromWalletId()),
                     kv("toWallet", transfer.toWalletId()),
                     kv("amount", transfer.amount()),
                     kv("currency", transfer.currency()),
                     kv("fundsReserved", true));
            
            return new Result(transfer.endToEndId(), transfer.status());
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            String errorType = determineErrorType(e);
            metricsService.recordTransferCreationError(metricsTimer, errorType);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating PIX transfer", 
                      kv("errorType", "unexpected_error"),
                      kv("errorMessage", e.getMessage()),
                      e);
            metricsService.recordTransferCreationError(metricsTimer, "unexpected_error");
            throw new RuntimeException("Failed to create PIX transfer", e);
        } finally {
            ObservabilityContext.clear();
        }
    }
    
    private String determineErrorType(Exception e) {
        String message = e.getMessage();
        if (message == null) return "unknown";
        
        if (message.contains("Insufficient balance")) return "insufficient_balance";
        if (message.contains("not found")) return "not_found";
        if (message.contains("same wallet")) return "same_wallet";
        if (message.contains("already processed")) return "duplicate";
        if (message.contains("required")) return "validation_error";
        
        return "business_error";
    }
    
    private void validateCommand(Command command) {
        if (command.amount() == null || command.amount().signum() <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }
        
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key required");
        }
        
        if (command.toPixKey() == null || command.toPixKey().isBlank()) {
            throw new IllegalArgumentException("PIX key is required");
        }
        
        if (command.fromWalletId() == null) {
            throw new IllegalArgumentException("Source wallet ID is required");
        }
    }
    
    private String generateEndToEndId() {
        // Format: E + 32 alphanumeric characters
        // In production, this should follow BACEN's standard format
        return "E" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
