# Guia de Distributed Tracing - PIX Wallet

## 📋 Sumário

1. [Visão Geral](#visão-geral)
2. [Arquitetura](#arquitetura)
3. [Configuração](#configuração)
4. [Annotation @Traced](#annotation-traced)
5. [Visualização no Grafana](#visualização-no-grafana)
6. [Queries no Tempo](#queries-no-tempo)
7. [Correlação com Logs](#correlação-com-logs)
8. [Troubleshooting](#troubleshooting)
9. [Boas Práticas](#boas-práticas)

---

## 🎯 Visão Geral

O **Distributed Tracing** permite rastrear requisições através de todo o fluxo da aplicação, incluindo operações assíncronas. Isso é especialmente importante no PIX Wallet, onde transferências PIX seguem um fluxo:

```
1. Cliente solicita transferência → 
2. Sistema valida e cria transfer (PENDING) → 
3. Webhook externo confirma/rejeita (CONFIRMED/REJECTED) →
4. Sistema atualiza transfer e ledger
```

### Benefícios

- ✅ **Rastreamento end-to-end**: Acompanhe toda a jornada de uma transferência PIX
- ✅ **Identificação de gargalos**: Visualize quanto tempo cada operação leva
- ✅ **Correlação automática**: trace_id vincula logs, métricas e spans
- ✅ **Debug facilitado**: Veja exatamente onde ocorreu um erro
- ✅ **Análise de performance**: Identifique operações lentas no trace timeline

---

## 🏗️ Arquitetura

### Stack de Tracing

```
┌─────────────────────────────────────────────────────────────┐
│                    PIX Wallet Application                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ @Traced Annotation → TracingAspect (AOP)             │   │
│  │         ↓                                            │   │
│  │ Micrometer Observation API                           │   │
│  │         ↓                                            │   │
│  │ Micrometer Tracing Bridge (OTel)                     │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                           ↓ OTLP (HTTP/4318)
┌─────────────────────────────────────────────────────────────┐
│              OpenTelemetry Collector (Docker)               │
│  • Recebe spans via OTLP                                    │
│  • Processa e exporta para Tempo                            │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                   Grafana Tempo (Storage)                   │
│  • Armazena traces                                          │
│  • Permite queries por trace_id, service, operation         │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                  Grafana (Visualização)                     │
│  • Explore traces no Tempo                                  │
│  • Correlacione com logs (Loki) e métricas (Prometheus)     │
└─────────────────────────────────────────────────────────────┘
```

### Componentes

| Componente | Responsabilidade |
|-----------|------------------|
| **@Traced** | Annotation para marcar métodos que devem gerar spans |
| **TracingAspect** | Intercepta métodos @Traced via AOP e cria spans |
| **Micrometer Observation** | API unificada para observabilidade |
| **OpenTelemetry** | Padrão para instrumentação e exportação de traces |
| **OTLP Collector** | Recebe spans via OTLP (porta 4318) |
| **Tempo** | Backend de armazenamento de traces |
| **Grafana** | Frontend para visualização de traces |

---

## ⚙️ Configuração

### 1. Dependências Maven (já incluídas)

```xml
<!-- Micrometer Tracing Bridge para OpenTelemetry -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<!-- OpenTelemetry Exporter OTLP -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>

<!-- Spring Boot AOP (para @Traced) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 2. Configuração do application.yml

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling (desenvolvimento)
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
      compression: gzip
```

**⚠️ Nota para Produção:** Ajustar `probability` para 0.1 (10%) ou menos para reduzir overhead.

### 3. Logback (trace_id e span_id nos logs)

O `logback-spring.xml` já está configurado para incluir automaticamente os IDs de rastreamento:

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <!-- Outros campos MDC -->
    <includeMdcKeyName>trace_id</includeMdcKeyName>
    <includeMdcKeyName>span_id</includeMdcKeyName>
</encoder>
```

**Resultado:** Todo log JSON incluirá `trace_id` e `span_id` automaticamente quando houver um trace ativo.

---

## 🎯 Annotation @Traced

### Como Usar

A annotation `@Traced` marca métodos para rastreamento automático:

```java
@Service
public class PixTransferService {
    
    @Traced(operation = "pix.transfer.create", description = "Create PIX transfer")
    @Transactional
    public Result execute(Command command) {
        // O TracingAspect criará um span automaticamente
        // com nome "pix.transfer.create"
        return result;
    }
}
```

### Parâmetros

| Parâmetro | Obrigatório | Descrição | Exemplo |
|-----------|-------------|-----------|---------|
| `operation` | ✅ Sim | Nome da operação (lowercase com pontos) | `"pix.transfer.create"` |
| `description` | ❌ Não | Descrição opcional | `"Create PIX transfer"` |

### Convenções de Nomenclatura

Use **lowercase com pontos** para nomes de operações:

✅ **Bom:**
- `pix.transfer.create`
- `pix.webhook.process`
- `wallet.create`
- `pix.key.register`

❌ **Evite:**
- `PixTransferCreate` (CamelCase)
- `PIX_TRANSFER_CREATE` (snake_case em maiúsculas)
- `create-transfer` (kebab-case)

### Métodos Anotados

Atualmente, os seguintes métodos estão instrumentados:

| Service | Método | Operation Name |
|---------|--------|---------------|
| `PixTransferService` | `execute()` | `pix.transfer.create` |
| `PixWebhookService` | `execute()` | `pix.webhook.process` |

### Metadados Adicionados Automaticamente

O `TracingAspect` adiciona automaticamente:

| Tag | Descrição | Exemplo |
|-----|-----------|---------|
| `class` | Nome da classe | `PixTransferService` |
| `method` | Nome do método | `execute` |
| `parameter_types` | Tipos dos parâmetros | `Command` |
| `description` | Descrição (se fornecida) | `Create PIX transfer` |

---

## 📊 Visualização no Grafana

### Acessar o Grafana

1. Abra o navegador em http://localhost:3000
2. Login: `admin` / Senha: `admin`
3. Navegue para **Explore** > Selecione **Tempo** como data source

### Buscar Traces

#### Por Trace ID (mais preciso)

```
# Copie o trace_id de um log e pesquise diretamente
trace_id: 1a2b3c4d5e6f7g8h9i0j
```

**Exemplo de log JSON:**
```json
{
  "timestamp": "2025-11-04T23:15:42.123Z",
  "level": "INFO",
  "message": "Initiating PIX transfer",
  "trace_id": "1a2b3c4d5e6f7g8h9i0j",
  "span_id": "abc123def456",
  "endToEndId": "E123456782025110423154212345678"
}
```

#### Por Service Name

```
service.name = "pixwallet"
```

#### Por Operation Name

```
name = "pix.transfer.create"
```

#### Combinando Filtros

```
service.name = "pixwallet" AND name = "pix.webhook.process" AND status = error
```

### Timeline de um Trace

Ao abrir um trace, você verá:

```
┌─────────────────────────────────────────────────────────────┐
│ Trace: 1a2b3c4d5e6f7g8h9i0j                                 │
│ Duration: 245ms                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ pix.transfer.create [PixTransferService]      ████████ 245ms│
│ └─ database.query.select                     ██ 45ms        │
│ └─ database.query.insert                     ██ 38ms        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Detalhes do Span:**
- Nome da operação
- Duração total
- Tags (class, method, parameters)
- Logs de eventos (se houver)
- Exceções (se ocorreram erros)

---

## 🔍 Queries no Tempo

### Exemplos de Queries TraceQL

#### 1. Transferências PIX lentas (> 1 segundo)

```traceql
{name="pix.transfer.create" && duration > 1s}
```

#### 2. Webhooks com erro

```traceql
{name="pix.webhook.process" && status=error}
```

#### 3. Traces de um endToEndId específico

Como o `endToEndId` está no MDC, ele pode aparecer em tags:

```traceql
{resource.endToEndId="E123456782025110423154212345678"}
```

#### 4. Todas operações de um trace_id

```traceql
{trace_id="1a2b3c4d5e6f7g8h9i0j"}
```

#### 5. Operações de um usuário específico

```traceql
{resource.userId="user-123"}
```

### Análise de Performance

#### P95 de duração de transferências

No **Metrics Explorer** do Grafana:

```promql
histogram_quantile(0.95, 
  sum(rate(traces_spanmetrics_duration_bucket{name="pix.transfer.create"}[5m])) by (le)
)
```

---

## 🔗 Correlação com Logs

### Logs → Traces

**Cenário:** Você tem um log de erro e quer ver o trace completo.

1. No log JSON, copie o `trace_id`:
   ```json
   {
     "level": "ERROR",
     "message": "Transfer validation failed",
     "trace_id": "abc123def456xyz789"
   }
   ```

2. No Grafana Explore:
   - Data source: **Tempo**
   - Query: `trace_id: abc123def456xyz789`
   - Veja o trace timeline completo

### Traces → Logs

**Cenário:** Você vê um span com erro e quer os logs detalhados.

1. No Grafana, clique no span com erro
2. Copie o `trace_id` do span
3. Mude para data source **Loki** (quando implementado)
4. Query: `{app="pixwallet"} | json | trace_id="abc123def456xyz789"`
5. Veja todos os logs relacionados ao trace

---

## 🐛 Troubleshooting

### Problema 1: Traces não aparecem no Grafana

**Diagnóstico:**

1. Verificar se o OTLP Collector está rodando:
   ```bash
   docker ps | grep otel-collector
   ```

2. Verificar logs do coletor:
   ```bash
   docker logs otel-collector
   ```

3. Verificar conectividade:
   ```bash
   curl -I http://localhost:4318/v1/traces
   # Deve retornar 405 Method Not Allowed (normal)
   ```

4. Verificar se o Tempo está recebendo:
   ```bash
   docker logs tempo
   ```

**Solução:**
- Reiniciar containers: `docker-compose restart otel-collector tempo`
- Verificar `application.yml` → endpoint correto

---

### Problema 2: trace_id não aparece nos logs

**Diagnóstico:**

1. Verificar se há um trace ativo:
   - Traces são criados apenas para requisições HTTP ou métodos @Traced
   - Em testes unitários, não há trace por padrão

2. Verificar `logback-spring.xml`:
   ```xml
   <includeMdcKeyName>trace_id</includeMdcKeyName>
   <includeMdcKeyName>span_id</includeMdcKeyName>
   ```

**Solução:**
- Para testes de integração, usar `@SpringBootTest` para criar contexto completo
- Para testes unitários, mockar o tracing ou não esperar trace_id

---

### Problema 3: Span não criado para método @Traced

**Diagnóstico:**

1. Verificar se o AspectJ está habilitado:
   - `spring-boot-starter-aop` deve estar no classpath
   - `@EnableAspectJAutoProxy` não é necessário (Spring Boot auto-configura)

2. Verificar se o método é público:
   - AOP só funciona em métodos públicos

3. Verificar logs:
   ```
   DEBUG TracingAspect - Starting span: pix.transfer.create
   ```

**Solução:**
- Aumentar log level: `logging.level.org.pix.wallet.infrastructure.observability=DEBUG`
- Verificar se o método está sendo chamado via proxy Spring (não `this.metodo()`)

---

### Problema 4: Muitos traces estão degradando performance

**Diagnóstico:**

Sampling em 100% pode gerar overhead em produção.

**Solução:**

Ajustar sampling no `application.yml`:

```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # 10% de amostragem
```

**Boas Práticas:**
- Desenvolvimento: 100% (`1.0`)
- Staging: 50% (`0.5`)
- Produção: 10% (`0.1`) ou menos

---

## ✅ Boas Práticas

### 1. Nomeie Operações de Forma Consistente

Use padrão hierárquico:

```
<domínio>.<recurso>.<ação>

Exemplos:
✅ pix.transfer.create
✅ pix.transfer.confirm
✅ pix.webhook.process
✅ wallet.create
✅ pix.key.register
```

### 2. Não Anote Todos os Métodos

Anote apenas **métodos críticos** de negócio:

✅ **Anote:**
- Casos de uso principais
- Operações assíncronas
- Integrações externas
- Operações lentas conhecidas

❌ **Evite anotar:**
- Getters/setters
- Métodos privados
- Validações simples
- Métodos chamados milhares de vezes

### 3. Use Descrições Informativas

```java
@Traced(
    operation = "pix.transfer.validate.balance",
    description = "Validate if wallet has sufficient balance"
)
```

### 4. Combine Tracing com MDC

Enriqueça spans com contexto de negócio via `ObservabilityContext`:

```java
@Traced(operation = "pix.transfer.create")
public Result execute(Command command) {
    ObservabilityContext.setOperation("PIX_TRANSFER_CREATE");
    ObservabilityContext.setEndToEndId(command.endToEndId());
    ObservabilityContext.setWalletId(walletId);
    
    // O trace_id será propagado automaticamente para os logs
    log.info("Creating transfer");  
    // Output JSON terá: trace_id, span_id, operation, endToEndId, walletId
}
```

### 5. Monitore Latências

Configure alertas no Prometheus para operações lentas:

```yaml
- alert: PixTransferSlow
  expr: |
    histogram_quantile(0.95,
      sum(rate(traces_spanmetrics_duration_bucket{name="pix.transfer.create"}[5m])) by (le)
    ) > 1
  for: 5m
  annotations:
    summary: "PIX transfers estão lentos (P95 > 1s)"
```

### 6. Correlacione Traces, Logs e Métricas

Fluxo de debug ideal:

1. **Alerta de métrica** → "Taxa de erro em `pix_transfers_rejected_total` aumentou"
2. **Buscar traces com erro** → `{name="pix.transfer.create" && status=error}`
3. **Ver timeline do trace** → Identificar span lento ou com erro
4. **Ir para logs** → Copiar `trace_id` e buscar logs detalhados
5. **Análise de causa raiz** → Logs mostram validação que falhou

---

## 📖 Recursos Adicionais

- [Micrometer Tracing Documentation](https://micrometer.io/docs/tracing)
- [OpenTelemetry Specification](https://opentelemetry.io/docs/specs/otel/)
- [Grafana Tempo Documentation](https://grafana.com/docs/tempo/latest/)
- [TraceQL Query Language](https://grafana.com/docs/tempo/latest/traceql/)

---

## 🎯 Próximos Passos (Sprint 4)

- [ ] Integrar Loki para centralização de logs
- [ ] Criar dashboards combinando traces, logs e métricas
- [ ] Configurar alertas baseados em traces
- [ ] Adicionar trace de chamadas HTTP externas (se houver)

---

**Atualizado em:** 2025-11-04  
**Versão:** 1.0 (Sprint 3)
