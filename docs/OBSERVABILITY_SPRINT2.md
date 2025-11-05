# Sprint 2: Métricas Customizadas - Resumo de Implementação

## 📊 Objetivo

Implementar métricas customizadas usando Micrometer para monitorar o fluxo assíncrono de transferências PIX, permitindo rastreamento de performance, detecção de anomalias e troubleshooting em produção.

---

## ✅ Tarefas Completadas

### 1. MetricsService - Serviço Centralizado de Métricas

**Arquivo**: `src/main/java/org/pix/wallet/infrastructure/observability/MetricsService.java`

**Descrição**: Serviço centralizado para gerenciamento de todas as métricas customizadas do sistema.

**Métricas Implementadas** (15 métricas):

#### Transferências PIX (6 métricas)
- ✅ `pix.transfers.created` (Counter) - Total de transferências criadas
- ✅ `pix.transfers.confirmed` (Counter) - Total de transferências confirmadas
- ✅ `pix.transfers.rejected` (Counter) - Total de transferências rejeitadas
- ✅ `pix.transfers.pending` (Gauge) - Número atual de transferências pendentes
- ✅ `pix.transfer.creation.time` (Timer) - Latência de criação (p50/p95/p99)
- ✅ `pix.transfer.end_to_end.time` (Timer) - Tempo total do fluxo (criação → confirmação)

#### Webhooks (4 métricas)
- ✅ `pix.webhooks.received` (Counter) - Total de webhooks recebidos
- ✅ `pix.webhooks.duplicated` (Counter) - Webhooks duplicados (idempotência)
- ✅ `pix.webhooks.by_type` (Counter) - Webhooks por tipo de evento (CONFIRMED/REJECTED)
- ✅ `pix.webhook.processing.time` (Timer) - Latência de processamento

#### Carteiras e Chaves PIX (4 métricas)
- ✅ `pix.wallets.created` (Counter) - Total de carteiras criadas
- ✅ `pix.wallets.active` (Gauge) - Número de carteiras ativas
- ✅ `pix.pixkeys.registered` (Counter) - Total de chaves PIX registradas
- ✅ `pix.pixkeys.by_type` (Counter) - Chaves por tipo (CPF/EMAIL/PHONE/RANDOM)

#### Transações (2 métricas)
- ✅ `pix.deposits.completed` (Counter) - Total de depósitos completados
- ✅ `pix.withdrawals.completed` (Counter) - Total de saques completados

**Recursos Adicionais**:
- ✅ Error tracking com tags `error_type` para categorização detalhada
- ✅ Timer.Sample pattern para medição precisa de duração
- ✅ AtomicInteger para gauges thread-safe
- ✅ JavaDoc completo com exemplos de queries Prometheus

---

### 2. Instrumentação de Serviços

#### ✅ PixTransferService
**Arquivo**: `src/main/java/org/pix/wallet/application/service/PixTransferService.java`

**Métricas adicionadas**:
- Timer de criação com `Timer.Sample`
- Counter de transferências criadas
- Incremento de gauge de pendentes
- Error tracking com categorização (6 tipos de erro)

**Tipos de erro rastreados**:
- `insufficient_balance` - Saldo insuficiente
- `not_found` - Wallet/PIX key não encontrada
- `same_wallet` - Transferência para mesma carteira
- `duplicate` - Idempotency key duplicada
- `validation_error` - Erro de validação
- `business_error` - Outras regras de negócio

---

#### ✅ PixWebhookService
**Arquivo**: `src/main/java/org/pix/wallet/application/service/PixWebhookService.java`

**Métricas adicionadas**:
- Timer de processamento de webhook
- Counter de webhooks recebidos (por tipo)
- Counter de webhooks duplicados
- Counter de confirmações/rejeições
- Timer end-to-end quando webhook confirma transferência
- Error tracking com categorização (4 tipos)

**Tipos de erro rastreados**:
- `transfer_not_found` - Webhook órfão
- `validation_error` - Payload inválido
- `concurrent_modification` - Conflito de concorrência
- `business_error` - Outras regras

---

#### ✅ WalletService
**Arquivo**: `src/main/java/org/pix/wallet/application/service/WalletService.java`

**Métricas adicionadas**:
- Counter de carteiras criadas
- Incremento de gauge de carteiras ativas

---

#### ✅ PixKeyService
**Arquivo**: `src/main/java/org/pix/wallet/application/service/PixKeyService.java`

**Métricas adicionadas**:
- Counter de chaves PIX registradas
- Counter por tipo de chave (CPF/EMAIL/PHONE/RANDOM)

---

#### ✅ DepositService
**Arquivo**: `src/main/java/org/pix/wallet/application/service/DepositService.java`

**Métricas adicionadas**:
- Counter de depósitos completados

---

#### ✅ WithdrawService
**Arquivo**: `src/main/java/org/pix/wallet/application/service/WithdrawService.java`

**Métricas adicionadas**:
- Counter de saques completados

---

### 3. Documentação Completa

#### ✅ METRICS_GUIDE.md
**Arquivo**: `docs/METRICS_GUIDE.md`

**Conteúdo**:
- ✅ Descrição detalhada de todas as 15 métricas
- ✅ Valor de negócio de cada métrica
- ✅ Exemplos de queries Prometheus para cada métrica
- ✅ Recomendações de alertas com thresholds
- ✅ 4 cenários de troubleshooting detalhados
- ✅ 4 dashboards recomendados com painéis específicos
- ✅ Guia de acesso às métricas (Prometheus endpoint, UI, Grafana)
- ✅ Resumo executivo com métricas críticas
- ✅ Boas práticas de monitoramento

---

## 🎯 Valor Entregue

### Monitoramento em Tempo Real
- **Gauge de transferências pendentes**: Indicador crítico de saúde do fluxo assíncrono
- **Gauge de carteiras ativas**: Monitoramento da base de usuários
- **Counters incrementais**: Rastreamento de volume total de operações

### Performance e SLA
- **Timers com percentis (p50/p95/p99)**: Identificação de degradação de performance
- **Timer end-to-end**: Métrica de SLA do usuário (tempo total da transferência)
- **Latência de criação**: Tempo de resposta do endpoint

### Qualidade e Confiabilidade
- **Error tracking categorizado**: Identificação de causas raiz de falhas
- **Idempotência tracking**: Monitoramento de webhooks duplicados
- **Taxa de sucesso**: Confirmadas vs rejeitadas

### Capacidade e Negócio
- **Crescimento de usuários**: Carteiras e chaves PIX criadas
- **Fluxo financeiro**: Depósitos e saques completados
- **Distribuição de chaves**: Por tipo (CPF/EMAIL/PHONE/RANDOM)

---

## 📊 Exemplos de Uso

### 1. Monitorar Saúde do Sistema
```promql
# Transferências pendentes (deve ser baixo)
pix_transfers_pending

# Taxa de sucesso (deve ser > 95%)
pix_transfers_confirmed_total / pix_transfers_created_total
```

### 2. Detectar Problemas de Performance
```promql
# P95 de latência de criação (deve ser < 500ms)
histogram_quantile(0.95, rate(pix_transfer_creation_time_seconds_bucket[5m]))

# P95 end-to-end (deve ser < 5s)
histogram_quantile(0.95, rate(pix_transfer_end_to_end_time_seconds_bucket[5m]))
```

### 3. Identificar Causas de Erro
```promql
# Top 3 tipos de erro mais comuns
topk(3, sum by (error_type) (rate(pix_transfer_creation_errors_total[10m])))

# Erros de webhook por tipo
sum by (error_type) (rate(pix_webhook_processing_errors_total[5m]))
```

### 4. Monitorar Integração com Provider PIX
```promql
# Webhooks recebidos vs transferências criadas (deve ser ~1:1)
rate(pix_webhooks_received_total[5m]) / rate(pix_transfers_created_total[5m])

# Taxa de duplicação (deve ser < 10%)
pix_webhooks_duplicated_total / pix_webhooks_received_total
```

---

## 🚨 Alertas Críticos Recomendados

### 1. Transferências Pendentes Acumulando
```yaml
- alert: PendingTransfersBacklogCritical
  expr: pix_transfers_pending > 500
  for: 5m
  severity: critical
```

### 2. Taxa de Confirmação Baixa
```yaml
- alert: LowConfirmationRate
  expr: rate(pix_transfers_confirmed_total[10m]) / rate(pix_transfers_created_total[10m]) < 0.95
  for: 5m
  severity: critical
```

### 3. Webhooks Não Chegando
```yaml
- alert: NoWebhooksReceived
  expr: rate(pix_webhooks_received_total[5m]) == 0
  for: 10m
  severity: critical
```

### 4. Performance Degradada
```yaml
- alert: VerySlowTransferCreation
  expr: histogram_quantile(0.99, rate(pix_transfer_creation_time_seconds_bucket[5m])) > 2
  for: 3m
  severity: critical
```

---

## 🧪 Como Testar

### 1. Verificar Métricas no Prometheus Endpoint
```bash
# Ver todas as métricas PIX
curl http://localhost:8080/actuator/prometheus | grep pix

# Ver métricas específicas
curl http://localhost:8080/actuator/prometheus | grep pix_transfers_pending
curl http://localhost:8080/actuator/prometheus | grep pix_transfer_creation_time
```

### 2. Executar Fluxo Completo
```bash
# Usar script de teste do Sprint 1
./test-observability.sh

# Verificar incremento das métricas
curl http://localhost:8080/actuator/prometheus | grep pix_transfers_created
curl http://localhost:8080/actuator/prometheus | grep pix_webhooks_received
```

### 3. Prometheus UI
```
http://localhost:9090
```

Queries de exemplo:
- `pix_transfers_pending`
- `rate(pix_transfers_created_total[1m])`
- `histogram_quantile(0.95, rate(pix_transfer_creation_time_seconds_bucket[5m]))`

### 4. Grafana
```
http://localhost:3000
```

Criar painéis com as queries do METRICS_GUIDE.md

---

## 📈 Próximos Passos

### Sprint 3: Distributed Tracing
- [ ] Implementar TracingAspect com annotation @Traced
- [ ] Integrar com Tempo (já configurado no docker-compose)
- [ ] Adicionar trace IDs aos logs estruturados
- [ ] Criar spans customizados para operações críticas

### Melhorias Futuras (Sprint 2)
- [ ] Adicionar métricas de JVM (heap, threads, GC)
- [ ] Criar dashboards Grafana pré-configurados
- [ ] Implementar alertas no Alertmanager
- [ ] Adicionar métricas de banco de dados (query time, connection pool)

---

## 📚 Arquivos Criados/Modificados

### Novos Arquivos
1. ✅ `src/main/java/org/pix/wallet/infrastructure/observability/MetricsService.java` (400+ linhas)
2. ✅ `docs/METRICS_GUIDE.md` (completo com 15 métricas documentadas)
3. ✅ `docs/OBSERVABILITY_SPRINT2.md` (este arquivo)

### Arquivos Modificados
1. ✅ `src/main/java/org/pix/wallet/application/service/PixTransferService.java`
2. ✅ `src/main/java/org/pix/wallet/application/service/PixWebhookService.java`
3. ✅ `src/main/java/org/pix/wallet/application/service/WalletService.java`
4. ✅ `src/main/java/org/pix/wallet/application/service/PixKeyService.java`
5. ✅ `src/main/java/org/pix/wallet/application/service/DepositService.java`
6. ✅ `src/main/java/org/pix/wallet/application/service/WithdrawService.java`

---

## 🎓 Aprendizados e Boas Práticas

### 1. Padrão Timer.Sample
```java
Timer.Sample timer = metricsService.startTransferCreation();
try {
    // Operação
    metricsService.recordTransferCreation(timer); // Success
} catch (Exception e) {
    metricsService.recordTransferCreationError(timer, errorType); // Error
    throw e;
}
```

### 2. Gauges com AtomicInteger
```java
private final AtomicInteger pendingTransfers = new AtomicInteger(0);

Gauge.builder("pix.transfers.pending", pendingTransfers, AtomicInteger::get)
    .description("Current number of pending PIX transfers")
    .register(meterRegistry);
```

### 3. Error Categorization
```java
private String determineErrorType(Exception ex) {
    if (ex instanceof InsufficientBalanceException) return "insufficient_balance";
    if (ex instanceof WalletNotFoundException) return "not_found";
    // ...
}
```

### 4. Percentis em Timers
```java
Timer.builder("pix.transfer.creation.time")
    .publishPercentiles(0.5, 0.95, 0.99) // p50, p95, p99
    .publishPercentileHistogram()
    .register(meterRegistry);
```

---

## ✅ Critérios de Aceitação - TODOS CUMPRIDOS

- [x] MetricsService centralizado criado
- [x] Métricas de transferências PIX (criadas, confirmadas, rejeitadas, pendentes)
- [x] Métricas de performance (timers com percentis)
- [x] Métricas de webhooks (recebidos, duplicados, por tipo)
- [x] Métricas de carteiras e chaves PIX
- [x] Métricas de transações (depósitos, saques)
- [x] Error tracking categorizado
- [x] Documentação completa com queries Prometheus
- [x] Recomendações de alertas
- [x] Cenários de troubleshooting
- [x] Dashboards recomendados
- [x] Todos os serviços instrumentados

---

**Status**: ✅ **SPRINT 2 CONCLUÍDO**  
**Data**: Sprint 2 - Métricas Customizadas  
**Próximo Sprint**: Sprint 3 - Distributed Tracing
