package org.pix.wallet.infrastructure.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Aspecto AOP para criar spans customizados em métodos anotados com @Traced.
 * 
 * <p>Este aspecto intercepta automaticamente métodos marcados com {@link Traced} e:</p>
 * <ul>
 *   <li>Cria um span com o nome especificado na annotation</li>
 *   <li>Adiciona metadados (classe, método, parâmetros)</li>
 *   <li>Captura exceções e marca como erro no span</li>
 *   <li>Mede a duração da execução automaticamente</li>
 * </ul>
 * 
 * <p><b>Integração com OpenTelemetry:</b></p>
 * <ul>
 *   <li>Spans são exportados via OTLP para o Collector (porta 4318)</li>
 *   <li>Armazenados no Tempo para visualização no Grafana</li>
 *   <li>trace_id e span_id propagados automaticamente</li>
 * </ul>
 * 
 * <p><b>Exemplo de uso:</b></p>
 * <pre>
 * {@code
 * @Service
 * public class PixTransferService {
 *     
 *     @Traced(operation = "pix.transfer.create", description = "Cria transferência PIX")
 *     public Transfer createTransfer(TransferCommand command) {
 *         // O aspecto criará um span com nome "pix.transfer.create"
 *         // e adicionará tags: class, method, parameters
 *         return transfer;
 *     }
 * }
 * }
 * </pre>
 * 
 * @see Traced
 */
@Aspect
@Component
public class TracingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(TracingAspect.class);
    
    private final ObservationRegistry observationRegistry;
    
    public TracingAspect(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }
    
    /**
     * Intercepta métodos anotados com @Traced e cria um span customizado.
     * 
     * @param joinPoint informações sobre o método interceptado
     * @param traced annotation com configurações do span
     * @return resultado do método original
     * @throws Throwable exceções do método original (marcadas como erro no span)
     */
    @Around("@annotation(traced)")
    public Object traceMethod(ProceedingJoinPoint joinPoint, Traced traced) throws Throwable {
        String operationName = traced.operation();
        String description = traced.description();
        
        // Criar observação (span) com nome da operação
        Observation observation = Observation.createNotStarted(operationName, observationRegistry)
                .lowCardinalityKeyValue("class", joinPoint.getTarget().getClass().getSimpleName())
                .lowCardinalityKeyValue("method", joinPoint.getSignature().getName());
        
        // Adicionar descrição se fornecida
        if (!description.isEmpty()) {
            observation.lowCardinalityKeyValue("description", description);
        }
        
        // Adicionar informações sobre parâmetros (apenas tipos para evitar dados sensíveis)
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String parameterTypes = Arrays.stream(signature.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        
        if (!parameterTypes.isEmpty()) {
            observation.lowCardinalityKeyValue("parameter_types", parameterTypes);
        }
        
        logger.debug("Starting span: {} (class={}, method={})", 
                operationName, 
                joinPoint.getTarget().getClass().getSimpleName(), 
                joinPoint.getSignature().getName());
        
        // Executar método dentro do contexto do span
        try {
            observation.start();
            Object result = joinPoint.proceed();
            logger.debug("Span completed successfully: {}", operationName);
            return result;
        } catch (Throwable ex) {
            logger.error("Span completed with error: {} - {}", operationName, ex.getMessage());
            // Marcar span como erro
            observation.error(ex);
            throw ex;
        } finally {
            observation.stop();
        }
    }
}
