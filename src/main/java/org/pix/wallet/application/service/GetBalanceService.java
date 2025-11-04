package org.pix.wallet.application.service;

import java.math.BigDecimal;

import org.pix.wallet.application.port.in.GetBalanceUseCase;
import org.pix.wallet.application.port.out.LedgerEntryRepositoryPort;
import org.pix.wallet.application.port.out.WalletRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class GetBalanceService implements GetBalanceUseCase {

  private final WalletRepositoryPort walletPort;
    private final LedgerEntryRepositoryPort ledgerPort;

    public GetBalanceService(WalletRepositoryPort walletPort,
                          LedgerEntryRepositoryPort ledgerPort) {
        this.walletPort = walletPort;
        this.ledgerPort = ledgerPort;
    }
    
  @Override
  public Result execute(Command command) {
    var wallet = walletPort.findById(command.walletId())
        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

    if (command.at() != null) {
      var balanceAsOf = ledgerPort.getBalanceAsOf(wallet.id().toString(), command.at())
          .orElse(BigDecimal.ZERO);
      return new Result(wallet.id(), balanceAsOf);
    }

    var balance = ledgerPort.getCurrentBalance(wallet.id().toString()).orElse(BigDecimal.ZERO);
    return new Result(wallet.id(), balance);
  }
  
}
