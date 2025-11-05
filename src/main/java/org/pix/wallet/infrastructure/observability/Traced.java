package org.pix.wallet.infrastructure.observability;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation para adicionar spans customizados em métodos.
 * 
 * <p>Quando um método é anotado com @Traced, um span será criado automaticamente
 * via AspectJ, permitindo rastreamento detalhado da execução no distributed tracing.</p>
 * 
 * <p><b>Uso:</b></p>
 * <pre>
 * {@code
 * @Traced(operation = "pix.transfer.create")
 * public Transfer createTransfer(TransferCommand command) {
 *     // lógica de negócio
 * }
 * }
 * </pre>
 * 
 * <p><b>Visualização no Grafana/Tempo:</b></p>
 * <ul>
 *   <li>Cada método anotado aparecerá como um span separado</li>
 *   <li>O span terá o nome especificado em {@code operation}</li>
 *   <li>Exceções serão marcadas como erros no span</li>
 *   <li>Duração do método será capturada automaticamente</li>
 * </ul>
 * 
 * @see TracingAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Traced {
    
    /**
     * Nome da operação para o span.
     * 
     * <p>Convenção de nomenclatura:</p>
     * <ul>
     *   <li>Use lowercase com pontos: {@code pix.transfer.create}</li>
     *   <li>Prefixo com domínio: {@code pix.webhook.process}</li>
     *   <li>Seja descritivo: {@code pix.transfer.validate.balance}</li>
     * </ul>
     * 
     * @return nome da operação
     */
    String operation();
    
    /**
     * Descrição opcional da operação.
     * 
     * @return descrição da operação
     */
    String description() default "";
}
