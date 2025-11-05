package org.pix.wallet.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serviço centralizado para gerenciamento de métricas customizadas da aplicação.
 * 
 * <p>Este serviço utiliza o <b>Micrometer</b> para instrumentar a aplicação com métricas
 * que são expostas no endpoint <code>/actuator/prometheus</code> e coletadas pelo Prometheus.</p>
 * 
 * <h3>Categorias de Métricas:</h3>
 * <ul>
 *   <li><b>Contadores (Counters):</b> Contagem acumulada de eventos (sempre crescente)</li>
 *   <li><b>Timers:</b> Medição de latência e duração de operações</li>
 *   <li><b>Gauges:</b> Valores que podem subir e descer (ex: transferências pendentes)</li>
 * </ul>
 * 
 * <h3>Métricas PIX Transfer:</h3>
 * <ul>
 *   <li><code>pix.transfers.created</code> - Total de transferências criadas</li>
 *   <li><code>pix.transfers.confirmed</code> - Total de transferências confirmadas</li>
 *   <li><code>pix.transfers.rejected</code> - Total de transferências rejeitadas</li>
 *   <li><code>pix.transfers.pending</code> - Transferências aguardando confirmação (gauge)</li>
 *   <li><code>pix.transfer.creation.time</code> - Latência de criação de transferências</li>
 *   <li><code>pix.transfer.end_to_end.time</code> - Tempo total (criação → confirmação)</li>
 * </ul>
 * 
 * <h3>Métricas Webhook:</h3>
 * <ul>
 *   <li><code>pix.webhooks.received</code> - Total de webhooks recebidos</li>
 *   <li><code>pix.webhooks.duplicated</code> - Webhooks duplicados detectados</li>
 *   <li><code>pix.webhook.processing.time</code> - Latência de processamento de webhooks</li>
 *   <li><code>pix.webhooks.by_type</code> - Webhooks por tipo (CONFIRMED, REJECTED, etc)</li>
 * </ul>
 * 
 * <h3>Métricas Wallet:</h3>
 * <ul>
 *   <li><code>pix.wallets.created</code> - Total de carteiras criadas</li>
 *   <li><code>pix.wallets.active</code> - Carteiras ativas (gauge)</li>
 *   <li><code>pix.pixkeys.registered</code> - Total de chaves PIX registradas</li>
 * </ul>
 * 
 * <h3>Uso em Dashboards:</h3>
 * <p>Estas métricas são projetadas para alimentar dashboards Grafana que mostram:</p>
 * <ul>
 *   <li>Taxa de sucesso de transferências (confirmed / created)</li>
 *   <li>Latências (p50, p95, p99) de operações críticas</li>
 *   <li>Funil de conversão (created → pending → confirmed)</li>
 *   <li>Detecção de anomalias (picos de rejeições, duplicações)</li>
 * </ul>
 * 
 * <h3>Exemplo de Query Prometheus:</h3>
 * <pre>{@code
 * # Taxa de transferências criadas (req/s)
 * rate(pix_transfers_created_total[5m])
 * 
 * # Taxa de sucesso
 * rate(pix_transfers_confirmed_total[5m]) / rate(pix_transfers_created_total[5m])
 * 
 * # Latência p95 de criação
 * histogram_quantile(0.95, rate(pix_transfer_creation_time_bucket[5m]))
 * }</pre>
 * 
 * @author PIX Wallet Team
 * @see io.micrometer.core.instrument.MeterRegistry
 * @see io.micrometer.core.instrument.Counter
 * @see io.micrometer.core.instrument.Timer
 * @since 1.0.0
 */
@Slf4j
@Service
public class MetricsService {
    
    private final MeterRegistry registry;
    
    // ========== PIX Transfer Metrics ==========
    
    /** Contador total de transferências PIX criadas */
    private final Counter transfersCreated;
    
    /** Contador total de transferências PIX confirmadas */
    private final Counter transfersConfirmed;
    
    /** Contador total de transferências PIX rejeitadas */
    private final Counter transfersRejected;
    
    /** Timer para medir latência de criação de transferências */
    private final Timer transferCreationTime;
    
    /** Timer para medir tempo end-to-end (criação → confirmação) */
    private final Timer transferEndToEndTime;
    
    /** Gauge com número atual de transferências pendentes */
    private final AtomicInteger pendingTransfers = new AtomicInteger(0);
    
    // ========== Webhook Metrics ==========
    
    /** Contador total de webhooks recebidos */
    private final Counter webhooksReceived;
    
    /** Contador de webhooks duplicados detectados */
    private final Counter webhooksDuplicated;
    
    /** Timer para medir latência de processamento de webhooks */
    private final Timer webhookProcessingTime;
    
    // ========== Wallet Metrics ==========
    
    /** Contador total de carteiras criadas */
    private final Counter walletsCreated;
    
    /** Contador total de chaves PIX registradas */
    private final Counter pixKeysRegistered;
    
    /** Gauge com número atual de carteiras ativas */
    private final AtomicInteger activeWallets = new AtomicInteger(0);
    
    // ========== Deposit/Withdraw Metrics ==========
    
    /** Contador total de depósitos realizados */
    private final Counter depositsCompleted;
    
    /** Contador total de saques realizados */
    private final Counter withdrawalsCompleted;
    
    /**
     * Construtor que inicializa todas as métricas no MeterRegistry.
     * 
     * @param registry o MeterRegistry do Micrometer (injetado automaticamente)
     */
    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
        
        log.info("Initializing custom metrics service...");
        
        // ========== PIX Transfer Counters ==========
        
        this.transfersCreated = Counter.builder("pix.transfers.created")
            .description("Total number of PIX transfers created")
            .tag("type", "transfer")
            .tag("status", "created")
            .register(registry);
        
        this.transfersConfirmed = Counter.builder("pix.transfers.confirmed")
            .description("Total number of PIX transfers confirmed via webhook")
            .tag("type", "transfer")
            .tag("status", "confirmed")
            .register(registry);
        
        this.transfersRejected = Counter.builder("pix.transfers.rejected")
            .description("Total number of PIX transfers rejected via webhook")
            .tag("type", "transfer")
            .tag("status", "rejected")
            .register(registry);
        
        // ========== PIX Transfer Timers ==========
        
        this.transferCreationTime = Timer.builder("pix.transfer.creation.time")
            .description("Time taken to create a PIX transfer (validation + persistence)")
            .publishPercentiles(0.5, 0.95, 0.99) // p50, p95, p99
            .publishPercentileHistogram()
            .register(registry);
        
        this.transferEndToEndTime = Timer.builder("pix.transfer.end_to_end.time")
            .description("Total time from transfer creation to webhook confirmation")
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram()
            .register(registry);
        
        // ========== PIX Transfer Gauges ==========
        
        Gauge.builder("pix.transfers.pending", pendingTransfers, AtomicInteger::get)
            .description("Current number of PIX transfers in PENDING status")
            .tag("status", "pending")
            .register(registry);
        
        // ========== Webhook Counters ==========
        
        this.webhooksReceived = Counter.builder("pix.webhooks.received")
            .description("Total number of webhooks received")
            .tag("type", "webhook")
            .register(registry);
        
        this.webhooksDuplicated = Counter.builder("pix.webhooks.duplicated")
            .description("Number of duplicate webhook events detected (idempotency)")
            .tag("type", "webhook")
            .tag("reason", "duplicate")
            .register(registry);
        
        // ========== Webhook Timers ==========
        
        this.webhookProcessingTime = Timer.builder("pix.webhook.processing.time")
            .description("Time taken to process a webhook event")
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram()
            .register(registry);
        
        // ========== Wallet Counters ==========
        
        this.walletsCreated = Counter.builder("pix.wallets.created")
            .description("Total number of wallets created")
            .tag("type", "wallet")
            .register(registry);
        
        this.pixKeysRegistered = Counter.builder("pix.pixkeys.registered")
            .description("Total number of PIX keys registered")
            .tag("type", "pixkey")
            .register(registry);
        
        // ========== Wallet Gauges ==========
        
        Gauge.builder("pix.wallets.active", activeWallets, AtomicInteger::get)
            .description("Current number of active wallets")
            .tag("status", "active")
            .register(registry);
        
        // ========== Deposit/Withdraw Counters ==========
        
        this.depositsCompleted = Counter.builder("pix.deposits.completed")
            .description("Total number of deposits completed")
            .tag("type", "deposit")
            .register(registry);
        
        this.withdrawalsCompleted = Counter.builder("pix.withdrawals.completed")
            .description("Total number of withdrawals completed")
            .tag("type", "withdrawal")
            .register(registry);
        
        log.info("Custom metrics initialized successfully");
    }
    
    // ========== PIX Transfer Methods ==========
    
    /**
     * Registra a criação de uma transferência PIX.
     * Incrementa o contador de transferências criadas e o gauge de pendentes.
     */
    public void recordTransferCreated() {
        transfersCreated.increment();
        pendingTransfers.incrementAndGet();
        log.trace("Metric recorded: transfer created (pending: {})", pendingTransfers.get());
    }
    
    /**
     * Registra a confirmação de uma transferência PIX via webhook.
     * 
     * @param endToEndDuration duração total desde criação até confirmação
     */
    public void recordTransferConfirmed(Duration endToEndDuration) {
        transfersConfirmed.increment();
        pendingTransfers.decrementAndGet();
        transferEndToEndTime.record(endToEndDuration);
        
        log.trace("Metric recorded: transfer confirmed (pending: {}, e2e: {}ms)", 
                  pendingTransfers.get(), endToEndDuration.toMillis());
    }
    
    /**
     * Registra a rejeição de uma transferência PIX via webhook.
     */
    public void recordTransferRejected() {
        transfersRejected.increment();
        pendingTransfers.decrementAndGet();
        log.trace("Metric recorded: transfer rejected (pending: {})", pendingTransfers.get());
    }
    
    /**
     * Inicia medição de tempo para criação de transferência.
     * 
     * @return amostra de timer para ser parada posteriormente
     */
    public Timer.Sample startTransferCreation() {
        return Timer.start(registry);
    }
    
    /**
     * Finaliza medição de tempo de criação de transferência.
     * 
     * @param sample a amostra iniciada com {@link #startTransferCreation()}
     */
    public void recordTransferCreation(Timer.Sample sample) {
        sample.stop(transferCreationTime);
    }
    
    /**
     * Registra erro na criação de transferência.
     * 
     * @param sample a amostra de timer
     * @param errorType tipo do erro (ex: insufficient_balance, validation_error)
     */
    public void recordTransferCreationError(Timer.Sample sample, String errorType) {
        Timer errorTimer = Timer.builder("pix.transfer.creation.time")
            .description("Time taken to create a PIX transfer (with error)")
            .tag("status", "error")
            .tag("error_type", errorType)
            .register(registry);
        
        sample.stop(errorTimer);
        log.trace("Metric recorded: transfer creation error (type: {})", errorType);
    }
    
    // ========== Webhook Methods ==========
    
    /**
     * Registra recebimento de um webhook.
     * 
     * @param eventType tipo do evento (CONFIRMED, REJECTED, PENDING)
     */
    public void recordWebhookReceived(String eventType) {
        webhooksReceived.increment();
        
        // Contador adicional por tipo de evento
        registry.counter("pix.webhooks.by_type", 
                        "eventType", eventType.toUpperCase())
                .increment();
        
        log.trace("Metric recorded: webhook received (type: {})", eventType);
    }
    
    /**
     * Registra detecção de webhook duplicado (idempotência).
     */
    public void recordWebhookDuplicated() {
        webhooksDuplicated.increment();
        log.trace("Metric recorded: webhook duplicated");
    }
    
    /**
     * Inicia medição de tempo de processamento de webhook.
     * 
     * @return amostra de timer
     */
    public Timer.Sample startWebhookProcessing() {
        return Timer.start(registry);
    }
    
    /**
     * Finaliza medição de tempo de processamento de webhook.
     * 
     * @param sample a amostra iniciada
     */
    public void recordWebhookProcessing(Timer.Sample sample) {
        sample.stop(webhookProcessingTime);
    }
    
    /**
     * Registra erro no processamento de webhook.
     * 
     * @param sample a amostra de timer
     * @param errorType tipo do erro (ex: transfer_not_found, validation_error)
     */
    public void recordWebhookProcessingError(Timer.Sample sample, String errorType) {
        Timer errorTimer = Timer.builder("pix.webhook.processing.time")
            .description("Time taken to process webhook (with error)")
            .tag("status", "error")
            .tag("error_type", errorType)
            .register(registry);
        
        sample.stop(errorTimer);
        log.trace("Metric recorded: webhook processing error (type: {})", errorType);
    }
    
    // ========== Wallet Methods ==========
    
    /**
     * Registra criação de uma carteira.
     */
    public void recordWalletCreated() {
        walletsCreated.increment();
        activeWallets.incrementAndGet();
        log.trace("Metric recorded: wallet created (active: {})", activeWallets.get());
    }
    
    /**
     * Registra registro de uma chave PIX.
     * 
     * @param keyType tipo da chave (CPF, EMAIL, PHONE, RANDOM)
     */
    public void recordPixKeyRegistered(String keyType) {
        pixKeysRegistered.increment();
        
        // Contador adicional por tipo de chave
        registry.counter("pix.pixkeys.by_type",
                        "keyType", keyType.toUpperCase())
                .increment();
        
        log.trace("Metric recorded: PIX key registered (type: {})", keyType);
    }
    
    // ========== Deposit/Withdraw Methods ==========
    
    /**
     * Registra conclusão de um depósito.
     */
    public void recordDepositCompleted() {
        depositsCompleted.increment();
        log.trace("Metric recorded: deposit completed");
    }
    
    /**
     * Registra conclusão de um saque.
     */
    public void recordWithdrawalCompleted() {
        withdrawalsCompleted.increment();
        log.trace("Metric recorded: withdrawal completed");
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Retorna o número atual de transferências pendentes.
     * 
     * @return número de transferências em status PENDING
     */
    public int getPendingTransfersCount() {
        return pendingTransfers.get();
    }
    
    /**
     * Retorna o número atual de carteiras ativas.
     * 
     * @return número de carteiras ativas
     */
    public int getActiveWalletsCount() {
        return activeWallets.get();
    }
}
