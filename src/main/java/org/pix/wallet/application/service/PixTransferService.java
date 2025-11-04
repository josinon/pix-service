package org.pix.wallet.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pix.wallet.application.port.in.ProcessPixTransferUseCase;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.application.port.out.PixKeyRepositoryPort;
import org.pix.wallet.application.port.out.TransferRepositoryPort;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.pix.wallet.domain.model.PixKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PixTransferService implements ProcessPixTransferUseCase {

    private final WalletRepositoryPort walletRepositoryPort;
    private final PixKeyRepositoryPort pixKeyRepositoryPort;
    private final TransferRepositoryPort transferRepositoryPort;
    private final LedgerEntryRepositoryPort ledgerEntryRepositoryPort;
    
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Result execute(Command command) {
        log.info("Processing PIX transfer - fromWallet: {}, toPixKey: {}, amount: {}, idempotencyKey: {}", 
                 command.fromWalletId(), command.toPixKey(), command.amount(), command.idempotencyKey());
        
        // 1. Validations
        validateCommand(command);
        
        // 2. Check idempotency - if already processed, return existing result
        if (transferRepositoryPort.existsByIdempotencyKey(command.idempotencyKey())) {
            log.info("Transfer already processed with idempotency key: {}", command.idempotencyKey());
            var existingTransfer = transferRepositoryPort.findByIdempotencyKey(command.idempotencyKey())
                .orElseThrow(() -> new IllegalStateException("Transfer exists but not found"));
            return new Result(existingTransfer.endToEndId(), existingTransfer.status());
        }
        
        // 3. Validate source wallet exists
        walletRepositoryPort.findById(UUID.fromString(command.fromWalletId()))
            .orElseThrow(() -> new IllegalArgumentException("Source wallet not found: " + command.fromWalletId()));
        
        // 4. Validate source wallet has sufficient balance
        BigDecimal currentBalance = ledgerEntryRepositoryPort.getCurrentBalance(command.fromWalletId())
            .orElse(BigDecimal.ZERO);
        
        if (currentBalance.compareTo(command.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Available: " + currentBalance + ", Required: " + command.amount());
        }
        

        // 8. Generate unique endToEndId (E + 32 chars)
        String endToEndId = generateEndToEndId();
        
        // 9. Create transfer record with PENDING status
        var transferCommand = new TransferRepositoryPort.TransferCommand(
            endToEndId,
            command.fromWalletId(),
            command.toPixKey(),
            command.amount(),
            "BRL",
            "PENDING",
            command.idempotencyKey()
        );
        
        TransferRepositoryPort.TransferResult transfer = transferRepositoryPort.save(transferCommand);
        
        log.info("PIX transfer created - endToEndId: {}, status: {}, fromWallet: {}, toWallet: {}", 
                 transfer.endToEndId(), transfer.status(), transfer.fromWalletId(), transfer.toWalletId());
        
        // Note: Actual debit/credit will be done when webhook confirms the transfer
        // This follows the eventual consistency pattern for PIX transfers
        
        return new Result(transfer.endToEndId(), transfer.status());
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
