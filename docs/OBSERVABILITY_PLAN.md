# Plano de Observabilidade - PIX Wallet

## 📋 Visão Geral

Este documento apresenta um plano estruturado para implementar **observabilidade completa** no sistema PIX Wallet, com foco especial no rastreamento de fluxos assíncronos de transferências (solicitação → processamento → webhook → confirmação).

---

## 🎯 Objetivos

### Principais
1. **Rastreabilidade End-to-End** de transferências PIX através de Correlation IDs
2. **Logs Estruturados (JSON)** para facilitar queries e análise
3. **Métricas Customizadas** para monitorar SLAs e performance
4. **Distributed Tracing** para visualizar fluxos assíncronos
5. **Dashboards** para visualização em tempo real

### Desafios Específicos
- ✅ Correlacionar requisição inicial com webhook assíncrono
- ✅ Rastrear mudanças de status de transferências
- ✅ Identificar gargalos e latências
- ✅ Detectar falhas e retentativas
- ✅ Monitorar duplicação (idempotência)

---

## 🏗️ Arquitetura de Observabilidade

```
┌─────────────────────────────────────────────────────────────┐
│                     Application Layer                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │   Logs   │  │ Metrics  │  │  Traces  │  │   MDC    │   │
│  │  (SLF4J) │  │(Micrometer)│ │ (OTEL)   │  │(Context) │   │
│  └─────┬────┘  └─────┬────┘  └─────┬────┘  └─────┬────┘   │
└────────┼─────────────┼─────────────┼─────────────┼─────────┘
         │             │             │             │
         ▼             ▼             ▼             ▼
┌─────────────────────────────────────────────────────────────┐
│              OpenTelemetry Collector (OTEL)                 │
│  • Recebe traces, métricas e logs                           │
│  • Processa e enriquece dados                               │
│  • Exporta para backends específicos                        │
└────────┬─────────────┬─────────────┬────────────────────────┘
         │             │             │
         ▼             ▼             ▼
┌──────────────┐ ┌──────────┐ ┌──────────────┐
│  Tempo       │ │Prometheus│ │  Loki        │
│  (Traces)    │ │(Metrics) │ │  (Logs)      │
└──────┬───────┘ └────┬─────┘ └──────┬───────┘
       │              │              │
       └──────────────┼──────────────┘
                      ▼
              ┌─────────────┐
              │   Grafana   │
              │ (Dashboards)│
              └─────────────┘
```

---

## 📦 Fase 1: Dependências e Configurações Base

### 1.1. Adicionar Dependências ao `pom.xml`

```xml
<!-- Logs Estruturados (JSON) -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>

<!-- Micrometer Tracing (já tem, mas verificar) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<!-- Propagação de Trace Context -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<!-- AOP para métricas customizadas -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 1.2. Atualizar `docker-compose.yml` - Adicionar Loki

```yaml
loki:
  image: grafana/loki:2.9.3
  container_name: pixwallet-loki
  restart: unless-stopped
  command: -config.file=/etc/loki/local-config.yaml
  ports:
    - "3100:3100"
  volumes:
    - loki-data:/loki
  networks: [app-net]

# Adicionar volume
volumes:
  loki-data:
```

### 1.3. Configurar Application Properties

**`application.yml`**
```yaml
spring:
  application:
    name: pixwallet

# Logging
logging:
  level:
    org.pix.wallet: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active:local}
    distribution:
      percentiles-histogram:
        http.server.requests: true
  
  # Tracing
  tracing:
    sampling:
      probability: 1.0  # 100% em dev/staging, 0.1 em prod
  
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
```

---

## 📝 Fase 2: Logs Estruturados + MDC (Correlation)

### 2.1. Criar Logback Configuration (JSON)

**Arquivo:** `src/main/resources/logback-spring.xml`

**Características:**
- ✅ Logs em JSON para facilitar parsing
- ✅ Inclui MDC (Mapped Diagnostic Context) para correlation
- ✅ Trace ID e Span ID do OpenTelemetry
- ✅ Campos customizados (walletId, transferId, endToEndId)

### 2.2. Criar Filter para Correlation ID

**Classe:** `infrastructure/config/CorrelationIdFilter.java`

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) {
        try {
            String correlationId = getOrGenerateCorrelationId(request);
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
    
    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }
}
```

### 2.3. Criar Utility para MDC Context

**Classe:** `infrastructure/observability/ObservabilityContext.java`

```java
@Slf4j
public class ObservabilityContext {
    
    public static void setTransferId(UUID transferId) {
        MDC.put("transferId", transferId.toString());
    }
    
    public static void setEndToEndId(String endToEndId) {
        MDC.put("endToEndId", endToEndId);
    }
    
    public static void setWalletId(UUID walletId) {
        MDC.put("walletId", walletId.toString());
    }
    
    public static void setEventId(String eventId) {
        MDC.put("eventId", eventId);
    }
    
    public static void setOperation(String operation) {
        MDC.put("operation", operation);
    }
    
    public static void clear() {
        MDC.remove("transferId");
        MDC.remove("endToEndId");
        MDC.remove("walletId");
        MDC.remove("eventId");
        MDC.remove("operation");
    }
}
```

### 2.4. Logs Estruturados nos Services

**Exemplo em `PixTransferService.java`:**

```java
@Slf4j
@Service
public class PixTransferService {
    
    public PixTransferResponse createTransfer(...) {
        ObservabilityContext.setOperation("PIX_TRANSFER_CREATE");
        ObservabilityContext.setWalletId(fromWalletId);
        
        log.info("Initiating PIX transfer", 
            kv("fromWallet", fromWalletId),
            kv("toPixKey", toPixKey),
            kv("amount", amount));
        
        try {
            // Lógica de transferência...
            String endToEndId = generateEndToEndId();
            ObservabilityContext.setEndToEndId(endToEndId);
            
            log.info("PIX transfer created successfully",
                kv("endToEndId", endToEndId),
                kv("status", "PENDING"));
            
            return response;
        } catch (Exception e) {
            log.error("Failed to create PIX transfer", e);
            throw e;
        } finally {
            ObservabilityContext.clear();
        }
    }
}
```

**Exemplo em `PixWebhookService.java`:**

```java
@Slf4j
@Service
public class PixWebhookService {
    
    public void processWebhook(Command command) {
        ObservabilityContext.setOperation("PIX_WEBHOOK_PROCESS");
        ObservabilityContext.setEndToEndId(command.endToEndId());
        ObservabilityContext.setEventId(command.eventId());
        
        log.info("Processing PIX webhook",
            kv("eventType", command.eventType()),
            kv("occurredAt", command.occurredAt()));
        
        try {
            // Buscar transferência original
            Transfer transfer = findTransfer(command.endToEndId());
            ObservabilityContext.setTransferId(transfer.getId());
            
            log.info("Transfer found for webhook",
                kv("transferId", transfer.getId()),
                kv("currentStatus", transfer.getStatus()));
            
            // Processar evento...
            
        } finally {
            ObservabilityContext.clear();
        }
    }
}
```

---

## 📊 Fase 3: Métricas Customizadas (Micrometer)

### 3.1. Criar Serviço de Métricas

**Classe:** `infrastructure/observability/MetricsService.java`

```java
@Service
public class MetricsService {
    
    private final MeterRegistry registry;
    
    // Contadores
    private final Counter transfersCreated;
    private final Counter transfersConfirmed;
    private final Counter transfersRejected;
    private final Counter webhooksReceived;
    private final Counter webhooksDuplicated;
    
    // Timers
    private final Timer transferCreationTime;
    private final Timer webhookProcessingTime;
    private final Timer transferEndToEndTime;
    
    // Gauges
    private final AtomicInteger pendingTransfers = new AtomicInteger(0);
    
    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
        
        // Inicializar métricas
        this.transfersCreated = Counter.builder("pix.transfers.created")
            .description("Total PIX transfers created")
            .tag("type", "transfer")
            .register(registry);
        
        this.transfersConfirmed = Counter.builder("pix.transfers.confirmed")
            .description("Total PIX transfers confirmed")
            .register(registry);
        
        this.transfersRejected = Counter.builder("pix.transfers.rejected")
            .description("Total PIX transfers rejected")
            .register(registry);
        
        this.webhooksReceived = Counter.builder("pix.webhooks.received")
            .description("Total webhooks received")
            .register(registry);
        
        this.webhooksDuplicated = Counter.builder("pix.webhooks.duplicated")
            .description("Duplicate webhook events detected")
            .register(registry);
        
        this.transferCreationTime = Timer.builder("pix.transfer.creation.time")
            .description("Time to create a transfer")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
        
        this.webhookProcessingTime = Timer.builder("pix.webhook.processing.time")
            .description("Time to process webhook")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
        
        this.transferEndToEndTime = Timer.builder("pix.transfer.end_to_end.time")
            .description("Time from transfer creation to confirmation")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
        
        // Gauge para transferências pendentes
        Gauge.builder("pix.transfers.pending", pendingTransfers, AtomicInteger::get)
            .description("Current pending transfers")
            .register(registry);
    }
    
    public void recordTransferCreated() {
        transfersCreated.increment();
        pendingTransfers.incrementAndGet();
    }
    
    public void recordTransferConfirmed(Duration endToEndDuration) {
        transfersConfirmed.increment();
        pendingTransfers.decrementAndGet();
        transferEndToEndTime.record(endToEndDuration);
    }
    
    public void recordTransferRejected() {
        transfersRejected.increment();
        pendingTransfers.decrementAndGet();
    }
    
    public void recordWebhookReceived(String eventType) {
        webhooksReceived.increment();
        registry.counter("pix.webhooks.by_type", "eventType", eventType).increment();
    }
    
    public void recordWebhookDuplicated() {
        webhooksDuplicated.increment();
    }
    
    public Timer.Sample startTransferCreation() {
        return Timer.start(registry);
    }
    
    public void recordTransferCreation(Timer.Sample sample) {
        sample.stop(transferCreationTime);
    }
    
    public Timer.Sample startWebhookProcessing() {
        return Timer.start(registry);
    }
    
    public void recordWebhookProcessing(Timer.Sample sample) {
        sample.stop(webhookProcessingTime);
    }
}
```

### 3.2. Integrar Métricas nos Services

**`PixTransferService.java`:**

```java
@Service
public class PixTransferService {
    
    private final MetricsService metricsService;
    
    public PixTransferResponse createTransfer(...) {
        Timer.Sample sample = metricsService.startTransferCreation();
        
        try {
            // Criar transferência...
            
            metricsService.recordTransferCreated();
            metricsService.recordTransferCreation(sample);
            
            return response;
        } catch (Exception e) {
            sample.stop(registry.timer("pix.transfer.creation.time", "status", "error"));
            throw e;
        }
    }
}
```

**`PixWebhookService.java`:**

```java
@Service
public class PixWebhookService {
    
    private final MetricsService metricsService;
    
    public void processWebhook(Command command) {
        Timer.Sample sample = metricsService.startWebhookProcessing();
        
        metricsService.recordWebhookReceived(command.eventType());
        
        // Verificar duplicação
        if (webhookInbox.existsByEventId(command.eventId())) {
            metricsService.recordWebhookDuplicated();
            log.warn("Duplicate webhook detected", kv("eventId", command.eventId()));
            return;
        }
        
        try {
            Transfer transfer = findTransfer(command.endToEndId());
            
            if ("CONFIRMED".equals(command.eventType())) {
                Duration endToEndDuration = Duration.between(
                    transfer.getCreatedAt(), 
                    command.occurredAt()
                );
                
                metricsService.recordTransferConfirmed(endToEndDuration);
                
                log.info("Transfer end-to-end completed",
                    kv("duration_ms", endToEndDuration.toMillis()));
            } else if ("REJECTED".equals(command.eventType())) {
                metricsService.recordTransferRejected();
            }
            
            metricsService.recordWebhookProcessing(sample);
            
        } catch (Exception e) {
            sample.stop(registry.timer("pix.webhook.processing.time", "status", "error"));
            throw e;
        }
    }
}
```

---

## 🔍 Fase 4: Distributed Tracing (OpenTelemetry)

### 4.1. Criar Aspect para Tracing Automático

**Classe:** `infrastructure/observability/TracingAspect.java`

```java
@Aspect
@Component
@Slf4j
public class TracingAspect {
    
    private final Tracer tracer;
    
    public TracingAspect(Tracer tracer) {
        this.tracer = tracer;
    }
    
    @Around("@annotation(traced)")
    public Object traceMethod(ProceedingJoinPoint joinPoint, Traced traced) throws Throwable {
        String spanName = traced.value().isEmpty() 
            ? joinPoint.getSignature().getName() 
            : traced.value();
        
        Span span = tracer.spanBuilder(spanName)
            .setSpanKind(traced.kind())
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            // Adicionar atributos do MDC ao span
            addMdcAttributesToSpan(span);
            
            Object result = joinPoint.proceed();
            
            span.setStatus(StatusCode.OK);
            return result;
            
        } catch (Throwable t) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage());
            throw t;
        } finally {
            span.end();
        }
    }
    
    private void addMdcAttributesToSpan(Span span) {
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        if (mdc != null) {
            mdc.forEach((key, value) -> 
                span.setAttribute(key, value));
        }
    }
}
```

### 4.2. Criar Anotação `@Traced`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Traced {
    String value() default "";
    SpanKind kind() default SpanKind.INTERNAL;
}
```

### 4.3. Usar `@Traced` nos Services

```java
@Service
public class PixTransferService {
    
    @Traced(value = "pix.transfer.create", kind = SpanKind.SERVER)
    public PixTransferResponse createTransfer(...) {
        // Implementação...
    }
}

@Service
public class PixWebhookService {
    
    @Traced(value = "pix.webhook.process", kind = SpanKind.SERVER)
    public void processWebhook(Command command) {
        // Implementação...
    }
    
    @Traced(value = "pix.transfer.apply", kind = SpanKind.INTERNAL)
    private void applyTransferToWallets(Transfer transfer) {
        // Implementação...
    }
}
```

---

## 📈 Fase 5: Dashboards Grafana

### 5.1. Dashboard de Transferências PIX

**Métricas principais:**
- Taxa de criação de transferências (req/s)
- Taxa de confirmação vs rejeição
- Latência p50, p95, p99 (criação + webhook)
- Tempo end-to-end (criação → confirmação)
- Transferências pendentes (gauge)
- Taxa de duplicação de webhooks

**Query Prometheus exemplo:**
```promql
# Taxa de transferências criadas (5min)
rate(pix_transfers_created_total[5m])

# Latência p95 de criação
histogram_quantile(0.95, rate(pix_transfer_creation_time_bucket[5m]))

# Tempo end-to-end médio
rate(pix_transfer_end_to_end_time_sum[5m]) / 
rate(pix_transfer_end_to_end_time_count[5m])

# Transferências pendentes
pix_transfers_pending

# Taxa de webhooks duplicados
rate(pix_webhooks_duplicated_total[5m])
```

### 5.2. Dashboard de Logs Correlacionados

**Panels:**
1. **Log Stream** - Filtrado por correlationId
2. **Error Rate** - Logs de erro por operação
3. **Transfer Journey** - Logs de uma transferência específica (endToEndId)

**LogQL (Loki) exemplo:**
```logql
# Todos os logs de uma transferência
{app="pixwallet"} |= `endToEndId` |= "E123ABC..."

# Logs de erro em webhooks
{app="pixwallet"} | json | operation="PIX_WEBHOOK_PROCESS" | level="ERROR"

# Rastrear jornada completa
{app="pixwallet"} | json | correlationId="abc-123-def"
```

### 5.3. Dashboard de Traces

**Visualizações:**
- Flamegraph de spans
- Dependências entre serviços
- Latências por operação
- Taxa de erro por span

---

## 🎯 Fase 6: Alerts e SLOs

### 6.1. Alertas Prometheus

**Arquivo:** `docker/prometheus/alerts.yml`

```yaml
groups:
  - name: pix_transfers
    interval: 30s
    rules:
      - alert: HighTransferErrorRate
        expr: |
          rate(pix_transfer_creation_time_count{status="error"}[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Alta taxa de erro em transferências"
          description: "{{ $value }} erros/s nos últimos 5min"
      
      - alert: HighWebhookLatency
        expr: |
          histogram_quantile(0.95, 
            rate(pix_webhook_processing_time_bucket[5m])) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Latência alta no processamento de webhooks"
          description: "P95 = {{ $value }}s"
      
      - alert: TooManyPendingTransfers
        expr: pix_transfers_pending > 100
        for: 10m
        labels:
          severity: critical
        annotations:
          summary: "Muitas transferências pendentes"
          description: "{{ $value }} transferências aguardando confirmação"
      
      - alert: HighWebhookDuplicationRate
        expr: |
          rate(pix_webhooks_duplicated_total[5m]) > 0.05
        for: 3m
        labels:
          severity: warning
        annotations:
          summary: "Alta taxa de webhooks duplicados"
          description: "{{ $value }} duplicações/s"
```

### 6.2. SLOs (Service Level Objectives)

```yaml
# Transfer Creation SLO: 99.9% de sucesso
- record: slo:pix_transfer_creation:success_rate:5m
  expr: |
    sum(rate(pix_transfer_creation_time_count{status!="error"}[5m])) /
    sum(rate(pix_transfer_creation_time_count[5m]))

# Webhook Processing SLO: 99.5% em < 1s
- record: slo:pix_webhook_processing:latency:5m
  expr: |
    histogram_quantile(0.995, 
      rate(pix_webhook_processing_time_bucket[5m])) < 1
```

---

## 🔧 Fase 7: Queries Úteis para Debugging

### 7.1. Rastrear Transferência Específica

**Logs (Loki):**
```logql
{app="pixwallet"} | json | endToEndId="E123ABC456"
```

**Traces (Tempo/Jaeger):**
```
endToEndId = "E123ABC456"
```

**Métricas (Prometheus):**
```promql
pix_transfer_end_to_end_time{endToEndId="E123ABC456"}
```

### 7.2. Encontrar Transferências Lentas

```logql
{app="pixwallet"} 
| json 
| operation="PIX_WEBHOOK_PROCESS" 
| duration_ms > 2000
```

### 7.3. Analisar Falhas de Webhook

```logql
{app="pixwallet"} 
| json 
| operation="PIX_WEBHOOK_PROCESS" 
| level="ERROR"
| line_format "{{.timestamp}} [{{.endToEndId}}] {{.message}}"
```

---

## 📊 Fase 8: Exemplo de Fluxo Completo Rastreado

### Cenário: Transferência PIX de R$ 100,00

**1. Criação da Transferência (POST /pix/transfers)**
```json
// Log estruturado
{
  "timestamp": "2025-11-04T21:30:00.123Z",
  "level": "INFO",
  "correlationId": "corr-abc-123",
  "transferId": "transfer-uuid-456",
  "endToEndId": "E123ABC456",
  "walletId": "wallet-789",
  "operation": "PIX_TRANSFER_CREATE",
  "message": "Initiating PIX transfer",
  "fromWallet": "wallet-789",
  "toPixKey": "12345678901",
  "amount": 100.00,
  "trace_id": "trace-xyz-999",
  "span_id": "span-001"
}
```

**Trace:**
```
[pix.transfer.create] 250ms
  ├─ [validate.amount] 5ms
  ├─ [find.destination.wallet] 50ms
  ├─ [generate.end_to_end_id] 2ms
  └─ [save.transfer] 193ms
```

**Métrica:**
```
pix_transfers_created_total{} +1
pix_transfer_creation_time{} 0.250
pix_transfers_pending{} 1
```

---

**2. Recebimento do Webhook (POST /pix/webhook)**
```json
// Log estruturado
{
  "timestamp": "2025-11-04T21:30:02.456Z",
  "level": "INFO",
  "correlationId": "corr-webhook-999",  // Novo correlation ID
  "endToEndId": "E123ABC456",            // LINK com transferência!
  "eventId": "evt-confirm-123",
  "operation": "PIX_WEBHOOK_PROCESS",
  "message": "Processing PIX webhook",
  "eventType": "CONFIRMED",
  "trace_id": "trace-webhook-888",
  "span_id": "span-002"
}
```

**Trace:**
```
[pix.webhook.process] 180ms
  ├─ [find.transfer] 45ms
  ├─ [pix.transfer.apply] 100ms
  │   ├─ [debit.wallet] 40ms
  │   └─ [credit.wallet] 60ms
  └─ [update.status] 35ms
```

**Métrica:**
```
pix_webhooks_received_total{eventType="CONFIRMED"} +1
pix_webhook_processing_time{} 0.180
pix_transfers_confirmed_total{} +1
pix_transfers_pending{} 0
pix_transfer_end_to_end_time{} 2.333  // 2.333s total
```

---

**3. Query Grafana para visualizar jornada completa:**

```promql
# Latência total da transferência
histogram_quantile(0.95, 
  rate(pix_transfer_end_to_end_time_bucket[5m]))
```

```logql
# Todos os logs relacionados
{app="pixwallet"} | json | endToEndId="E123ABC456"
```

**Resultado esperado:**
```
2025-11-04 21:30:00.123 [INFO] [corr-abc-123] Initiating PIX transfer
2025-11-04 21:30:00.373 [INFO] [corr-abc-123] PIX transfer created successfully
2025-11-04 21:30:02.456 [INFO] [corr-webhook-999] Processing PIX webhook
2025-11-04 21:30:02.556 [INFO] [corr-webhook-999] Transfer found for webhook
2025-11-04 21:30:02.636 [INFO] [corr-webhook-999] Transfer end-to-end completed (duration: 2333ms)
```

---

## 🚀 Ordem de Implementação Recomendada

### Sprint 1 (Fundação)
- [x] ~~Fase 1: Dependências~~
- [ ] Fase 2.1-2.3: Logs estruturados + MDC + Correlation Filter
- [ ] Testar: Verificar logs JSON no console

### Sprint 2 (Métricas)
- [ ] Fase 3.1: Criar MetricsService
- [ ] Fase 3.2: Integrar métricas em PixTransferService
- [ ] Fase 3.2: Integrar métricas em PixWebhookService
- [ ] Testar: Verificar métricas no `/actuator/prometheus`

### Sprint 3 (Tracing)
- [ ] Fase 4.1-4.3: Aspect + @Traced
- [ ] Adicionar @Traced nos services principais
- [ ] Testar: Verificar traces no Tempo via Grafana

### Sprint 4 (Agregação)
- [ ] Adicionar Loki ao docker-compose
- [ ] Configurar Grafana datasources
- [ ] Fase 5: Criar dashboards

### Sprint 5 (Alertas)
- [ ] Fase 6: Configurar alertas
- [ ] Testar cenários de falha
- [ ] Ajustar thresholds

---

## ✅ Checklist de Validação

### Logs
- [ ] Logs em formato JSON
- [ ] Correlation ID presente em todos os logs
- [ ] MDC com endToEndId, transferId, walletId
- [ ] Trace ID e Span ID integrados
- [ ] Níveis de log apropriados (INFO, WARN, ERROR)

### Métricas
- [ ] Contador de transferências criadas
- [ ] Contador de transferências confirmadas/rejeitadas
- [ ] Timer de latência de criação
- [ ] Timer de latência de webhook
- [ ] Timer end-to-end
- [ ] Gauge de transferências pendentes

### Tracing
- [ ] Spans criados para operações principais
- [ ] Atributos do MDC propagados para spans
- [ ] Traces visualizáveis no Grafana
- [ ] Flamegraph mostrando hierarquia

### Dashboards
- [ ] Dashboard de transferências
- [ ] Dashboard de logs correlacionados
- [ ] Dashboard de traces
- [ ] Painéis com latências p50/p95/p99

### Alertas
- [ ] Alerta de alta taxa de erro
- [ ] Alerta de latência alta
- [ ] Alerta de transferências pendentes
- [ ] SLOs configurados

---

## 📚 Referências

- [OpenTelemetry Best Practices](https://opentelemetry.io/docs/concepts/observability-primer/)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Logback JSON Encoder](https://github.com/logfellow/logstash-logback-encoder)
- [Grafana Loki](https://grafana.com/docs/loki/latest/)
- [Prometheus Alerting](https://prometheus.io/docs/alerting/latest/overview/)

---

**Status:** 🚧 Em implementação  
**Última atualização:** Novembro 2025  
**Próxima revisão:** Após Sprint 1
