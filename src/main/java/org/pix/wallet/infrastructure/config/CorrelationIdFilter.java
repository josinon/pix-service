package org.pix.wallet.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter que gerencia Correlation IDs para rastreamento de requisições.
 * 
 * <p>Este filter é executado com prioridade máxima (HIGHEST_PRECEDENCE) para
 * garantir que o Correlation ID esteja disponível para todos os outros
 * componentes da aplicação.</p>
 * 
 * <h3>Funcionamento:</h3>
 * <ul>
 *   <li>Extrai o Correlation ID do header <b>X-Correlation-ID</b> da requisição</li>
 *   <li>Se não presente, gera um novo UUID</li>
 *   <li>Adiciona o ID ao MDC (Mapped Diagnostic Context) para logs</li>
 *   <li>Adiciona o ID ao header da resposta para o cliente</li>
 *   <li>Limpa o MDC após o processamento para evitar vazamento de contexto</li>
 * </ul>
 * 
 * <h3>Uso em Logs:</h3>
 * <pre>{@code
 * // O correlationId estará automaticamente disponível em todos os logs
 * log.info("Processing transfer"); 
 * // Output: {"correlationId": "abc-123", "message": "Processing transfer"}
 * }</pre>
 * 
 * <h3>Propagação:</h3>
 * <p>O Correlation ID é retornado no header da resposta, permitindo que
 * clientes externos (webhooks, APIs) propaguem o mesmo ID em requisições
 * subsequentes.</p>
 * 
 * @author PIX Wallet Team
 * @see org.slf4j.MDC
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    /**
     * Nome do header HTTP que contém o Correlation ID.
     * <p>Header padrão: <b>X-Correlation-ID</b></p>
     */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    
    /**
     * Chave usada no MDC para armazenar o Correlation ID.
     * <p>Esta chave estará disponível em todos os logs estruturados.</p>
     */
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    /**
     * Processa cada requisição HTTP adicionando o Correlation ID ao contexto.
     * 
     * @param request a requisição HTTP recebida
     * @param response a resposta HTTP a ser enviada
     * @param filterChain a cadeia de filtros a ser executada
     * @throws ServletException se ocorrer erro no processamento
     * @throws IOException se ocorrer erro de I/O
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            // Obter ou gerar Correlation ID
            String correlationId = getOrGenerateCorrelationId(request);
            
            // Adicionar ao MDC para logs
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            
            // Adicionar ao header da resposta
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            log.debug("Request received with correlationId: {}", correlationId);
            
            // Continuar processamento
            filterChain.doFilter(request, response);
            
        } finally {
            // Limpar MDC para evitar vazamento entre threads
            MDC.clear();
        }
    }
    
    /**
     * Obtém o Correlation ID do header da requisição ou gera um novo.
     * 
     * <p>Se o cliente enviou o header <b>X-Correlation-ID</b>, reutiliza o valor.
     * Caso contrário, gera um novo UUID.</p>
     * 
     * @param request a requisição HTTP
     * @return o Correlation ID (existente ou novo)
     */
    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.trace("Generated new correlationId: {}", correlationId);
        } else {
            log.trace("Using existing correlationId from header: {}", correlationId);
        }
        
        return correlationId;
    }
}
