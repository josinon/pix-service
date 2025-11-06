package org.pix.wallet.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.config.IntegrationTest;
import org.pix.wallet.domain.model.enums.OperationType;
import org.pix.wallet.infrastructure.persistence.repository.LedgerEntryJpaRepository;
import org.pix.wallet.integration.support.TestDataHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@DisplayName("Transfer Fund Reservation - Integration Tests")
class TransferReservationIT {

    @LocalServerPort
    int port;
    
    @Autowired
    TestRestTemplate rest;

    @Autowired
    private LedgerEntryRepositoryPort ledgerEntryRepositoryPort;

    @Autowired
    private LedgerEntryJpaRepository ledgerEntryJpaRepository;
    
    private TestDataHelper helper;

    private String walletA;
    private String walletB;
    private String pixKeyB;

    @BeforeEach
    void setUp() {
        helper = new TestDataHelper(rest, port);
        walletA = helper.createWallet();
        walletB = helper.createWallet();
        pixKeyB = helper.createRandomPixKey(walletB);
        
        // Setup initial balances
        helper.deposit(walletA, new BigDecimal("1000.00"));
        helper.deposit(walletB, new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("Should reserve funds when transfer is created and unreserve when CONFIRMED")
    @Transactional
    void shouldReserveFundsAndUnreserveOnConfirmed() {
        // Given - Initial balance: 1000.00
        BigDecimal initialBalance = ledgerEntryRepositoryPort.getCurrentBalance(walletA).orElse(BigDecimal.ZERO);
        BigDecimal initialAvailable = ledgerEntryRepositoryPort.getAvailableBalance(walletA).orElse(BigDecimal.ZERO);
        
        assertThat(initialBalance).isEqualByComparingTo("1000.00");
        assertThat(initialAvailable).isEqualByComparingTo("1000.00");

        BigDecimal transferAmount = new BigDecimal("300.00");

        // When - Create transfer
        String endToEndId = helper.startPixTransfer(walletA, pixKeyB, transferAmount, "idem-" + UUID.randomUUID());

        // Then - Funds should be reserved
        BigDecimal balanceAfterReserve = ledgerEntryRepositoryPort.getCurrentBalance(walletA).orElse(BigDecimal.ZERO);
        BigDecimal availableAfterReserve = ledgerEntryRepositoryPort.getAvailableBalance(walletA).orElse(BigDecimal.ZERO);
        
        assertThat(balanceAfterReserve).isEqualByComparingTo("1000.00"); // Real balance unchanged
        assertThat(availableAfterReserve).isEqualByComparingTo("700.00"); // 1000 - 300 reserved
        
        // Verify RESERVED entry exists
        long reservedCount = ledgerEntryJpaRepository.findAll().stream()
            .filter(e -> e.getOperationType() == OperationType.RESERVED)
            .filter(e -> e.getWallet().getId().toString().equals(walletA))
            .count();
        assertThat(reservedCount).isEqualTo(1);

        // When - Confirm transfer via webhook
        helper.confirmPixTransfer(endToEndId);

        // Then - Funds should be debited and unreserved
        BigDecimal finalBalance = ledgerEntryRepositoryPort.getCurrentBalance(walletA).orElse(BigDecimal.ZERO);
        BigDecimal finalAvailable = ledgerEntryRepositoryPort.getAvailableBalance(walletA).orElse(BigDecimal.ZERO);
        
        assertThat(finalBalance).isEqualByComparingTo("700.00"); // 1000 - 300 transferred
        assertThat(finalAvailable).isEqualByComparingTo("700.00"); // No more reservations
        
        // Verify UNRESERVED entry exists
        long unreservedCount = ledgerEntryJpaRepository.findAll().stream()
            .filter(e -> e.getOperationType() == OperationType.UNRESERVED)
            .filter(e -> e.getWallet().getId().toString().equals(walletA))
            .count();
        assertThat(unreservedCount).isEqualTo(1);
        
        // Verify destination wallet received funds
        BigDecimal destBalance = ledgerEntryRepositoryPort.getCurrentBalance(walletB).orElse(BigDecimal.ZERO);
        assertThat(destBalance).isEqualByComparingTo("800.00"); // 500 + 300
    }

    @Test
    @DisplayName("Should reserve funds when transfer is created and unreserve when REJECTED")
    @Transactional
    void shouldReserveFundsAndUnreserveOnRejected() {
        // Given - Initial balance: 1000.00
        BigDecimal initialBalance = ledgerEntryRepositoryPort.getCurrentBalance(walletA).orElse(BigDecimal.ZERO);
        assertThat(initialBalance).isEqualByComparingTo("1000.00");

        BigDecimal transferAmount = new BigDecimal("400.00");

        // When - Create transfer
        String endToEndId = helper.startPixTransfer(walletA, pixKeyB, transferAmount, "idem-" + UUID.randomUUID());

        // Then - Funds should be reserved
        BigDecimal availableAfterReserve = ledgerEntryRepositoryPort.getAvailableBalance(walletA).orElse(BigDecimal.ZERO);
        assertThat(availableAfterReserve).isEqualByComparingTo("600.00"); // 1000 - 400 reserved

        // When - Reject transfer via webhook
        helper.rejectPixTransfer(endToEndId);

        // Then - Funds should be unreserved and returned
        BigDecimal finalBalance = ledgerEntryRepositoryPort.getCurrentBalance(walletA).orElse(BigDecimal.ZERO);
        BigDecimal finalAvailable = ledgerEntryRepositoryPort.getAvailableBalance(walletA).orElse(BigDecimal.ZERO);
        
        assertThat(finalBalance).isEqualByComparingTo("1000.00"); // Unchanged - no debit
        assertThat(finalAvailable).isEqualByComparingTo("1000.00"); // Fully available again
        
        // Verify destination wallet did NOT receive funds
        BigDecimal destBalance = ledgerEntryRepositoryPort.getCurrentBalance(walletB).orElse(BigDecimal.ZERO);
        assertThat(destBalance).isEqualByComparingTo("500.00"); // Unchanged
    }

    @Test
    @DisplayName("Should prevent second transfer when funds are reserved by first")
    void shouldPreventSecondTransferWhenFundsReserved() {
        // Given - Initial balance: 1000.00
        BigDecimal transferAmount1 = new BigDecimal("700.00");
        BigDecimal transferAmount2 = new BigDecimal("400.00");

        // When - Create first transfer (reserves 700)
        helper.startPixTransfer(walletA, pixKeyB, transferAmount1, "idem-first-" + UUID.randomUUID());

        // Then - Available balance should be 300.00
        BigDecimal availableAfterFirst = ledgerEntryRepositoryPort.getAvailableBalance(walletA).orElse(BigDecimal.ZERO);
        assertThat(availableAfterFirst).isEqualByComparingTo("300.00");

        // When/Then - Second transfer should fail (needs 400, only 300 available)
        try {
            helper.startPixTransfer(walletA, pixKeyB, transferAmount2, "idem-second-" + UUID.randomUUID());
            assertThat(false).as("Should have thrown InsufficientFundsException").isTrue();
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("Insufficient");
        }
    }

    @Test
    @DisplayName("Should allow second transfer after first is confirmed")
    void shouldAllowSecondTransferAfterFirstConfirmed() {
        // Given - Initial balance: 1000.00
        BigDecimal transferAmount1 = new BigDecimal("400.00");
        BigDecimal transferAmount2 = new BigDecimal("300.00");

        // When - Create and confirm first transfer
        String endToEndId1 = helper.startPixTransfer(walletA, pixKeyB, transferAmount1, "idem-first-" + UUID.randomUUID());
        helper.confirmPixTransfer(endToEndId1);

        // Then - Balance should be 600.00, all available
        BigDecimal balanceAfterFirst = ledgerEntryRepositoryPort.getCurrentBalance(walletA).orElse(BigDecimal.ZERO);
        BigDecimal availableAfterFirst = ledgerEntryRepositoryPort.getAvailableBalance(walletA).orElse(BigDecimal.ZERO);
        
        assertThat(balanceAfterFirst).isEqualByComparingTo("600.00");
        assertThat(availableAfterFirst).isEqualByComparingTo("600.00");

        // When - Create second transfer
        String endToEndId2 = helper.startPixTransfer(walletA, pixKeyB, transferAmount2, "idem-second-" + UUID.randomUUID());

        // Then - Should succeed, available balance should be 300.00
        BigDecimal availableAfterSecond = ledgerEntryRepositoryPort.getAvailableBalance(walletA).orElse(BigDecimal.ZERO);
        assertThat(availableAfterSecond).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("Should allow second transfer after first is rejected")
    void shouldAllowSecondTransferAfterFirstRejected() {
        // Given - Initial balance: 1000.00
        BigDecimal transferAmount1 = new BigDecimal("700.00");
        BigDecimal transferAmount2 = new BigDecimal("600.00");

        // When - Create and reject first transfer
        String endToEndId1 = helper.startPixTransfer(walletA, pixKeyB, transferAmount1, "idem-first-" + UUID.randomUUID());
        helper.rejectPixTransfer(endToEndId1);

        // Then - Balance should be 1000.00, all available
        BigDecimal balanceAfterReject = ledgerEntryRepositoryPort.getCurrentBalance(walletA).orElse(BigDecimal.ZERO);
        BigDecimal availableAfterReject = ledgerEntryRepositoryPort.getAvailableBalance(walletA).orElse(BigDecimal.ZERO);
        
        assertThat(balanceAfterReject).isEqualByComparingTo("1000.00");
        assertThat(availableAfterReject).isEqualByComparingTo("1000.00");

        // When - Create second transfer
        String endToEndId2 = helper.startPixTransfer(walletA, pixKeyB, transferAmount2, "idem-second-" + UUID.randomUUID());

        // Then - Should succeed, available balance should be 400.00
        BigDecimal availableAfterSecond = ledgerEntryRepositoryPort.getAvailableBalance(walletA).orElse(BigDecimal.ZERO);
        assertThat(availableAfterSecond).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("Should handle multiple concurrent reservations correctly")
    void shouldHandleMultipleConcurrentReservations() {
        // Given - Initial balance: 1000.00
        BigDecimal amount1 = new BigDecimal("200.00");
        BigDecimal amount2 = new BigDecimal("300.00");
        BigDecimal amount3 = new BigDecimal("400.00");

        // When - Create three transfers (all PENDING)
        helper.startPixTransfer(walletA, pixKeyB, amount1, "idem-1-" + UUID.randomUUID());
        helper.startPixTransfer(walletA, pixKeyB, amount2, "idem-2-" + UUID.randomUUID());
        helper.startPixTransfer(walletA, pixKeyB, amount3, "idem-3-" + UUID.randomUUID());

        // Then - Available should be 100.00 (1000 - 200 - 300 - 400)
        BigDecimal available = ledgerEntryRepositoryPort.getAvailableBalance(walletA).orElse(BigDecimal.ZERO);
        assertThat(available).isEqualByComparingTo("100.00");
        
        // Real balance should still be 1000.00
        BigDecimal balance = ledgerEntryRepositoryPort.getCurrentBalance(walletA).orElse(BigDecimal.ZERO);
        assertThat(balance).isEqualByComparingTo("1000.00");
        
        // Verify 3 RESERVED entries
        long reservedCount = ledgerEntryJpaRepository.findAll().stream()
            .filter(e -> e.getOperationType() == OperationType.RESERVED)
            .filter(e -> e.getWallet().getId().toString().equals(walletA))
            .count();
        assertThat(reservedCount).isEqualTo(3);
    }
}
