package org.pix.wallet.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.pix.wallet.application.port.out.TransferRepositoryPort;
import org.pix.wallet.domain.model.enums.TransferStatus;
import org.pix.wallet.infrastructure.persistence.entity.TransferEntity;
import org.pix.wallet.infrastructure.persistence.entity.WalletEntity;
import org.pix.wallet.infrastructure.persistence.repository.TransferJpaRepository;
import org.pix.wallet.infrastructure.persistence.repository.WalletJpaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TransferRepositoryAdapter implements TransferRepositoryPort {

    private final TransferJpaRepository transferJpaRepository;
    private final WalletJpaRepository walletJpaRepository;

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return transferJpaRepository.findByIdempotencyKey(idempotencyKey).isPresent();
    }

    @Override
    public Optional<TransferResult> findByIdempotencyKey(String idempotencyKey) {
        return transferJpaRepository.findByIdempotencyKey(idempotencyKey).map(this::toResult);
    }

    @Override
    public Optional<TransferResult> findByEndToEndId(String endToEndId) {
        return transferJpaRepository.findByEndToEndId(endToEndId)
            .map(this::toResult);
    }

    @Override
    public TransferResult save(TransferCommand command) {
        WalletEntity fromWallet = walletJpaRepository.findById(command.fromWalletId())
            .orElseThrow(() -> new IllegalArgumentException("From wallet not found: " + command.fromWalletId()));
        
        WalletEntity toWallet = walletJpaRepository.findById(command.toWalletId())
            .orElseThrow(() -> new IllegalArgumentException("To wallet not found: " + command.toWalletId()));
        
        TransferEntity entity = TransferEntity.builder()
            .endToEndId(command.endToEndId())
            .idempotencyKey(command.idempotencyKey())
            .fromWallet(fromWallet)
            .toWallet(toWallet)
            .amount(command.amount().multiply(new BigDecimal("100")))
            .currency(command.currency())
            .status(TransferStatus.valueOf(command.status()))
            .version(0)
            .build();
        
        TransferEntity saved = transferJpaRepository.save(entity);
        return toResult(saved);
    }

    @Override
    public void updateStatus(String endToEndId, String status, int currentVersion) {
        TransferEntity entity = transferJpaRepository.findByEndToEndId(endToEndId)
            .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + endToEndId));
        
        if (entity.getVersion() != currentVersion) {
            throw new IllegalStateException("Transfer version mismatch - concurrent modification detected");
        }
        
        entity.setStatus(TransferStatus.valueOf(status));
        transferJpaRepository.save(entity);
    }

    private TransferResult toResult(TransferEntity entity) {
        BigDecimal amount = entity.getAmount().divide(new BigDecimal("100"));
        
        return new TransferResult(
            entity.getId(),
            entity.getEndToEndId(),
            entity.getFromWallet().getId(),
            entity.getToWallet().getId(),
            amount,
            entity.getCurrency(),
            entity.getStatus().name(),
            entity.getVersion()
        );
    }
}
