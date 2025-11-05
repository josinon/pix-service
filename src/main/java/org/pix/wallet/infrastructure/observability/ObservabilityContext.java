package org.pix.wallet.infrastructure.observability;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utilitário para gerenciar contexto de observabilidade via MDC (Mapped Diagnostic Context).
 * 
 * <p>Esta classe fornece métodos para adicionar informações de contexto de negócio
 * ao MDC do SLF4J, permitindo que logs estruturados incluam automaticamente dados
 * relevantes como IDs de transferências, wallets, eventos PIX, etc.</p>
 * 
 * <h3>Uso Típico:</h3>
 * <pre>{@code
 * // No início de uma operação
 * ObservabilityContext.setOperation("PIX_TRANSFER_CREATE");
 * ObservabilityContext.setWalletId(walletId);
 * 
 * try {
 *     // Lógica de negócio...
 *     log.info("Creating transfer"); 
 *     // Output JSON incluirá: operation, walletId, correlationId (do filter)
 * } finally {
 *     ObservabilityContext.clear(); // Importante: limpar ao final
 * }
 * }</pre>
 * 
 * <h3>Campos Disponíveis:</h3>
 * <ul>
 *   <li><b>operation</b> - Nome da operação (ex: PIX_TRANSFER_CREATE, PIX_WEBHOOK_PROCESS)</li>
 *   <li><b>transferId</b> - UUID da transferência PIX</li>
 *   <li><b>endToEndId</b> - ID E2E da transação PIX (formato BACEN)</li>
 *   <li><b>walletId</b> - UUID da carteira envolvida</li>
 *   <li><b>eventId</b> - ID do evento de webhook</li>
 *   <li><b>userId</b> - ID do usuário (quando disponível)</li>
 * </ul>
 * 
 * <h3>Integração com Tracing:</h3>
 * <p>Os campos do MDC são automaticamente propagados para spans do OpenTelemetry
 * quando o {@link org.pix.wallet.infrastructure.observability.TracingAspect} está ativo.</p>
 * 
 * <h3>Thread Safety:</h3>
 * <p>MDC é thread-safe e mantém contexto por thread. Em ambientes assíncronos,
 * use {@code MDC.getCopyOfContextMap()} e {@code MDC.setContextMap()} para
 * propagar contexto entre threads.</p>
 * 
 * @author PIX Wallet Team
 * @see org.slf4j.MDC
 * @see CorrelationIdFilter
 * @since 1.0.0
 */
@Slf4j
public final class ObservabilityContext {
    
    // Chaves MDC
    private static final String OPERATION_KEY = "operation";
    private static final String TRANSFER_ID_KEY = "transferId";
    private static final String END_TO_END_ID_KEY = "endToEndId";
    private static final String WALLET_ID_KEY = "walletId";
    private static final String EVENT_ID_KEY = "eventId";
    private static final String USER_ID_KEY = "userId";
    
    /**
     * Construtor privado para prevenir instanciação.
     * Esta é uma classe utilitária com métodos estáticos apenas.
     */
    private ObservabilityContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Define o nome da operação sendo executada.
     * 
     * <p>Convenção de nomenclatura: Use snake_case em MAIÚSCULAS</p>
     * <p>Exemplos:</p>
     * <ul>
     *   <li>PIX_TRANSFER_CREATE</li>
     *   <li>PIX_WEBHOOK_PROCESS</li>
     *   <li>WALLET_CREATE</li>
     *   <li>PIX_KEY_REGISTER</li>
     *   <li>DEPOSIT</li>
     *   <li>WITHDRAW</li>
     * </ul>
     * 
     * @param operation nome da operação
     */
    public static void setOperation(String operation) {
        if (operation != null && !operation.isBlank()) {
            MDC.put(OPERATION_KEY, operation);
            log.trace("MDC: operation = {}", operation);
        }
    }
    
    /**
     * Define o ID da transferência PIX.
     * 
     * @param transferId UUID da transferência
     */
    public static void setTransferId(UUID transferId) {
        if (transferId != null) {
            MDC.put(TRANSFER_ID_KEY, transferId.toString());
            log.trace("MDC: transferId = {}", transferId);
        }
    }
    
    /**
     * Define o End-to-End ID da transação PIX.
     * 
     * <p>O E2EId é o identificador único da transação PIX no formato do BACEN,
     * usado para rastrear a transação desde a origem até o destino.</p>
     * 
     * <p>Formato típico: E[ISPB][AAAAMMDD][HHMMSS][SEQUENCIAL]</p>
     * <p>Exemplo: E123456782025110421300012345678</p>
     * 
     * @param endToEndId identificador E2E da transação PIX
     */
    public static void setEndToEndId(String endToEndId) {
        if (endToEndId != null && !endToEndId.isBlank()) {
            MDC.put(END_TO_END_ID_KEY, endToEndId);
            log.trace("MDC: endToEndId = {}", endToEndId);
        }
    }
    
    /**
     * Define o ID da carteira (wallet).
     * 
     * @param walletId UUID da carteira
     */
    public static void setWalletId(UUID walletId) {
        if (walletId != null) {
            MDC.put(WALLET_ID_KEY, walletId.toString());
            log.trace("MDC: walletId = {}", walletId);
        }
    }
    
    /**
     * Define o ID do evento de webhook.
     * 
     * <p>Usado para rastrear eventos assíncronos recebidos via webhook,
     * garantindo idempotência e rastreabilidade.</p>
     * 
     * @param eventId identificador único do evento
     */
    public static void setEventId(String eventId) {
        if (eventId != null && !eventId.isBlank()) {
            MDC.put(EVENT_ID_KEY, eventId);
            log.trace("MDC: eventId = {}", eventId);
        }
    }
    
    /**
     * Define o ID do usuário.
     * 
     * @param userId identificador do usuário
     */
    public static void setUserId(String userId) {
        if (userId != null && !userId.isBlank()) {
            MDC.put(USER_ID_KEY, userId);
            log.trace("MDC: userId = {}", userId);
        }
    }
    
    /**
     * Remove o campo 'operation' do MDC.
     */
    public static void clearOperation() {
        MDC.remove(OPERATION_KEY);
    }
    
    /**
     * Remove o campo 'transferId' do MDC.
     */
    public static void clearTransferId() {
        MDC.remove(TRANSFER_ID_KEY);
    }
    
    /**
     * Remove o campo 'endToEndId' do MDC.
     */
    public static void clearEndToEndId() {
        MDC.remove(END_TO_END_ID_KEY);
    }
    
    /**
     * Remove o campo 'walletId' do MDC.
     */
    public static void clearWalletId() {
        MDC.remove(WALLET_ID_KEY);
    }
    
    /**
     * Remove o campo 'eventId' do MDC.
     */
    public static void clearEventId() {
        MDC.remove(EVENT_ID_KEY);
    }
    
    /**
     * Remove o campo 'userId' do MDC.
     */
    public static void clearUserId() {
        MDC.remove(USER_ID_KEY);
    }
    
    /**
     * Remove TODOS os campos de contexto de observabilidade do MDC.
     * 
     * <p><b>Importante:</b> Este método NÃO remove o correlationId, que é
     * gerenciado pelo {@link CorrelationIdFilter} e deve permanecer durante
     * toda a requisição HTTP.</p>
     * 
     * <p>Use este método em blocos {@code finally} para garantir limpeza:</p>
     * <pre>{@code
     * try {
     *     ObservabilityContext.setOperation("PIX_TRANSFER");
     *     // ... lógica ...
     * } finally {
     *     ObservabilityContext.clear();
     * }
     * }</pre>
     */
    public static void clear() {
        MDC.remove(OPERATION_KEY);
        MDC.remove(TRANSFER_ID_KEY);
        MDC.remove(END_TO_END_ID_KEY);
        MDC.remove(WALLET_ID_KEY);
        MDC.remove(EVENT_ID_KEY);
        MDC.remove(USER_ID_KEY);
        log.trace("MDC: observability context cleared");
    }
    
    /**
     * Obtém o nome da operação atual do MDC.
     * 
     * @return o nome da operação ou null se não definida
     */
    public static String getOperation() {
        return MDC.get(OPERATION_KEY);
    }
    
    /**
     * Obtém o ID da transferência atual do MDC.
     * 
     * @return o transferId como String ou null se não definido
     */
    public static String getTransferId() {
        return MDC.get(TRANSFER_ID_KEY);
    }
    
    /**
     * Obtém o End-to-End ID atual do MDC.
     * 
     * @return o endToEndId ou null se não definido
     */
    public static String getEndToEndId() {
        return MDC.get(END_TO_END_ID_KEY);
    }
    
    /**
     * Obtém o ID da wallet atual do MDC.
     * 
     * @return o walletId como String ou null se não definido
     */
    public static String getWalletId() {
        return MDC.get(WALLET_ID_KEY);
    }
    
    /**
     * Obtém o ID do evento atual do MDC.
     * 
     * @return o eventId ou null se não definido
     */
    public static String getEventId() {
        return MDC.get(EVENT_ID_KEY);
    }
    
    /**
     * Obtém o ID do usuário atual do MDC.
     * 
     * @return o userId ou null se não definido
     */
    public static String getUserId() {
        return MDC.get(USER_ID_KEY);
    }
}
