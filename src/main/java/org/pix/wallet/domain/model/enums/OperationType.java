package org.pix.wallet.domain.model.enums;

public enum OperationType { 
    DEPOSIT,      // Adiciona fundos à carteira
    WITHDRAW,     // Remove fundos da carteira
    PIX_OUT,      // Transferência PIX enviada (legacy - será removido)
    PIX_IN,       // Transferência PIX recebida (legacy - será removido)
    ADJUSTMENT,   // Ajuste manual
    RESERVED,     // Bloqueia fundos temporariamente (transferência PENDING)
    UNRESERVED    // Libera fundos bloqueados (transferência CONFIRMED/REJECTED)
}