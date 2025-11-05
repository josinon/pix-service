# Guia de Métricas - PIX Wallet Service

## 📊 Visão Geral

Este documento descreve todas as métricas customizadas implementadas no PIX Wallet Service, explicando o propósito de cada uma, como utilizá-las para monitoramento e troubleshooting, e fornecendo exemplos de queries Prometheus e recomendações de alertas.

## 🎯 Objetivos das Métricas

As métricas foram projetadas para:
- **Monitorar a saúde** do fluxo assíncrono de transferências PIX
- **Identificar gargalos** de performance
- **Detectar anomalias** e padrões de erro
- **Rastrear métricas de negócio** (taxa de sucesso, volume de transações)
- **Facilitar troubleshooting** em produção
- **Suportar planejamento de capacidade**

---

## 📈 Categorias de Métricas

### 1️⃣ Métricas de Transferências PIX

#### `pix.transfers.created` (Counter)
**Tipo**: Counter  
**Descrição**: Total de transferências PIX criadas no sistema (status PENDING inicial).

**Valor de Negócio**:
- Monitorar volume total de transferências solicitadas
- Identificar picos de demanda
- Baseline para calcular taxa de conversão (criadas → confirmadas)

**Query Prometheus**:
```promql
# Total de transferências criadas
pix_transfers_created_total

# Taxa de criação por minuto (últimos 5 min)
rate(pix_transfers_created_total[5m]) * 60

# Total nas últimas 24h
increase(pix_transfers_created_total[24h])
```

**Alertas Recomendados**:
```yaml
# Alerta: Queda abrupta no volume de transferências
- alert: TransferVolumeDropped
  expr: rate(pix_transfers_created_total[5m]) < 0.1 * rate(pix_transfers_created_total[1h] offset 1h)
  for: 10m
  labels:
    severity: warning
  annotations:
    summary: "Volume de transferências caiu 90% comparado à hora anterior"
```

---

#### `pix.transfers.confirmed` (Counter)
**Tipo**: Counter  
**Descrição**: Total de transferências confirmadas via webhook.

**Valor de Negócio**:
- Medir taxa de sucesso das transferências
- Calcular SLA de confirmação
- Identificar problemas de integração com webhook provider

**Query Prometheus**:
```promql
# Total de transferências confirmadas
pix_transfers_confirmed_total

# Taxa de sucesso (confirmadas / criadas)
pix_transfers_confirmed_total / pix_transfers_created_total

# Taxa de confirmação por minuto
rate(pix_transfers_confirmed_total[5m]) * 60
```

**Alertas Recomendados**:
```yaml
# Alerta: Taxa de sucesso baixa
- alert: LowConfirmationRate
  expr: |
    (
      rate(pix_transfers_confirmed_total[10m]) / 
      rate(pix_transfers_created_total[10m])
    ) < 0.95
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "Taxa de confirmação abaixo de 95%"
```

---

#### `pix.transfers.rejected` (Counter)
**Tipo**: Counter  
**Descrição**: Total de transferências rejeitadas via webhook.

**Valor de Negócio**:
- Identificar problemas sistêmicos causando rejeições
- Monitorar qualidade das validações pré-transferência
- Alertar sobre aumento anormal de rejeições

**Query Prometheus**:
```promql
# Total de transferências rejeitadas
pix_transfers_rejected_total

# Taxa de rejeição
pix_transfers_rejected_total / pix_transfers_created_total

# Comparar rejeições vs confirmações
rate(pix_transfers_rejected_total[5m]) / rate(pix_transfers_confirmed_total[5m])
```

**Alertas Recomendados**:
```yaml
# Alerta: Taxa de rejeição alta
- alert: HighRejectionRate
  expr: |
    (
      rate(pix_transfers_rejected_total[10m]) / 
      rate(pix_transfers_created_total[10m])
    ) > 0.05
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Taxa de rejeição acima de 5%"
```

---

#### `pix.transfers.pending` (Gauge)
**Tipo**: Gauge  
**Descrição**: Número atual de transferências em estado PENDING (aguardando webhook).

**Valor de Negócio**:
- **Indicador crítico de saúde** do fluxo assíncrono
- Detectar atrasos no processamento de webhooks
- Monitorar backlog de transferências pendentes
- Identificar problemas de integração com provider

**Query Prometheus**:
```promql
# Transferências pendentes no momento
pix_transfers_pending

# Variação nas últimas 5 minutos
delta(pix_transfers_pending[5m])

# Média de pendentes nas últimas 2 horas
avg_over_time(pix_transfers_pending[2h])
```

**Alertas Recomendados**:
```yaml
# Alerta: Acúmulo de transferências pendentes
- alert: PendingTransfersAccumulating
  expr: pix_transfers_pending > 100
  for: 10m
  labels:
    severity: warning
  annotations:
    summary: "Mais de 100 transferências pendentes por 10+ minutos"

# Alerta CRÍTICO: Backlog muito alto
- alert: PendingTransfersBacklogCritical
  expr: pix_transfers_pending > 500
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "Backlog crítico: 500+ transferências pendentes"
```

---

#### `pix.transfer.creation.time` (Timer)
**Tipo**: Timer (Histogram)  
**Descrição**: Latência da criação de transferências PIX (desde request até persist).

**Percentis**: p50, p95, p99  
**Buckets**: 10ms, 50ms, 100ms, 200ms, 500ms, 1s, 2s, 5s

**Valor de Negócio**:
- Identificar degradação de performance no endpoint de criação
- Monitorar tempo de resposta para usuários
- Detectar lentidão em validações ou acesso ao banco

**Query Prometheus**:
```promql
# Latência média
rate(pix_transfer_creation_time_seconds_sum[5m]) / 
rate(pix_transfer_creation_time_seconds_count[5m])

# Percentil 95 (95% das requisições abaixo deste tempo)
histogram_quantile(0.95, 
  rate(pix_transfer_creation_time_seconds_bucket[5m])
)

# Percentil 99 (worst case para quase todos os usuários)
histogram_quantile(0.99, 
  rate(pix_transfer_creation_time_seconds_bucket[5m])
)

# Contagem total de criações
rate(pix_transfer_creation_time_seconds_count[5m]) * 60
```

**Alertas Recomendados**:
```yaml
# Alerta: P95 acima de 500ms
- alert: SlowTransferCreation
  expr: |
    histogram_quantile(0.95, 
      rate(pix_transfer_creation_time_seconds_bucket[5m])
    ) > 0.5
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "P95 de criação de transferências acima de 500ms"

# Alerta: P99 acima de 2s
- alert: VerySlowTransferCreation
  expr: |
    histogram_quantile(0.99, 
      rate(pix_transfer_creation_time_seconds_bucket[5m])
    ) > 2
  for: 3m
  labels:
    severity: critical
  annotations:
    summary: "P99 de criação de transferências acima de 2s"
```

---

#### `pix.transfer.end_to_end.time` (Timer)
**Tipo**: Timer (Histogram)  
**Descrição**: Tempo total do fluxo completo de transferência (criação → confirmação via webhook).

**Valor de Negócio**:
- **Métrica de SLA principal**: mede experiência real do usuário
- Identifica atrasos no processamento externo (provider PIX)
- Detecta problemas de latência na entrega de webhooks

**Query Prometheus**:
```promql
# Tempo médio end-to-end
rate(pix_transfer_end_to_end_time_seconds_sum[5m]) / 
rate(pix_transfer_end_to_end_time_seconds_count[5m])

# P95 do tempo total
histogram_quantile(0.95, 
  rate(pix_transfer_end_to_end_time_seconds_bucket[5m])
)

# P99 do tempo total
histogram_quantile(0.99, 
  rate(pix_transfer_end_to_end_time_seconds_bucket[5m])
)
```

**Alertas Recomendados**:
```yaml
# Alerta: SLA end-to-end violado (P95 > 5s)
- alert: TransferSLAViolation
  expr: |
    histogram_quantile(0.95, 
      rate(pix_transfer_end_to_end_time_seconds_bucket[10m])
    ) > 5
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "95% das transferências levando mais de 5s para confirmar"
```

---

#### `pix.transfer.creation.errors` (Counter)
**Tipo**: Counter  
**Tags**: `error_type`

**Descrição**: Erros durante criação de transferências, categorizados por tipo.

**Tipos de Erro**:
- `insufficient_balance` - Saldo insuficiente
- `not_found` - Wallet ou PIX key não encontrada
- `same_wallet` - Tentativa de transferir para mesma carteira
- `duplicate` - Idempotency key duplicada
- `validation_error` - Erro de validação (formato, valores)
- `business_error` - Outras regras de negócio

**Valor de Negócio**:
- Identificar principais causas de falha
- Priorizar melhorias em validações client-side
- Detectar problemas específicos (ex: muitos insufficient_balance)

**Query Prometheus**:
```promql
# Total de erros por tipo
sum by (error_type) (pix_transfer_creation_errors_total)

# Taxa de erros por tipo
rate(pix_transfer_creation_errors_total[5m]) * 60

# Tipo de erro mais comum
topk(3, sum by (error_type) (
  rate(pix_transfer_creation_errors_total[10m])
))

# Taxa de erro geral
sum(rate(pix_transfer_creation_errors_total[5m])) / 
rate(pix_transfer_creation_time_seconds_count[5m])
```

**Alertas Recomendados**:
```yaml
# Alerta: Muitos erros de saldo insuficiente
- alert: HighInsufficientBalanceErrors
  expr: |
    rate(pix_transfer_creation_errors_total{error_type="insufficient_balance"}[5m]) > 10
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Alta taxa de erros de saldo insuficiente"

# Alerta: Taxa geral de erro alta
- alert: HighTransferCreationErrorRate
  expr: |
    sum(rate(pix_transfer_creation_errors_total[5m])) / 
    rate(pix_transfer_creation_time_seconds_count[5m]) > 0.1
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "Taxa de erro de criação acima de 10%"
```

---

### 2️⃣ Métricas de Webhooks

#### `pix.webhooks.received` (Counter)
**Tipo**: Counter  
**Descrição**: Total de webhooks recebidos do provider PIX.

**Valor de Negócio**:
- Monitorar conectividade com provider PIX
- Detectar ausência de webhooks (possível falha)
- Comparar volume esperado vs recebido

**Query Prometheus**:
```promql
# Total de webhooks recebidos
pix_webhooks_received_total

# Taxa de recebimento por minuto
rate(pix_webhooks_received_total[5m]) * 60

# Comparar com transferências criadas (deveria ser ~1:1)
pix_webhooks_received_total / pix_transfers_created_total
```

**Alertas Recomendados**:
```yaml
# Alerta: Webhooks não estão chegando
- alert: NoWebhooksReceived
  expr: rate(pix_webhooks_received_total[5m]) == 0
  for: 10m
  labels:
    severity: critical
  annotations:
    summary: "Nenhum webhook recebido nos últimos 10 minutos"
```

---

#### `pix.webhooks.duplicated` (Counter)
**Tipo**: Counter  
**Descrição**: Webhooks duplicados detectados pela chave de idempotência.

**Valor de Negócio**:
- Monitorar retries do provider PIX
- Validar efetividade da idempotência
- Detectar "retry storms" (muitos retries anormais)

**Query Prometheus**:
```promql
# Total de duplicatas
pix_webhooks_duplicated_total

# Taxa de duplicação
pix_webhooks_duplicated_total / pix_webhooks_received_total

# Taxa de duplicatas por minuto
rate(pix_webhooks_duplicated_total[5m]) * 60
```

**Alertas Recomendados**:
```yaml
# Alerta: Alta taxa de webhooks duplicados
- alert: HighWebhookDuplicationRate
  expr: |
    (
      rate(pix_webhooks_duplicated_total[10m]) / 
      rate(pix_webhooks_received_total[10m])
    ) > 0.3
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Mais de 30% dos webhooks são duplicados - possível retry storm"
```

---

#### `pix.webhooks.by_type` (Counter)
**Tipo**: Counter  
**Tags**: `event_type`

**Descrição**: Webhooks categorizados por tipo de evento.

**Tipos de Evento**:
- `CONFIRMED` - Transferência confirmada
- `REJECTED` - Transferência rejeitada
- `PENDING` - Status intermediário (se aplicável)

**Valor de Negócio**:
- Verificar distribuição de confirmações vs rejeições
- Identificar padrões anormais (ex: muitas rejeições repentinas)

**Query Prometheus**:
```promql
# Webhooks por tipo
sum by (event_type) (pix_webhooks_by_type_total)

# Taxa por tipo
rate(pix_webhooks_by_type_total[5m]) * 60

# Proporção REJECTED vs CONFIRMED
rate(pix_webhooks_by_type_total{event_type="REJECTED"}[5m]) / 
rate(pix_webhooks_by_type_total{event_type="CONFIRMED"}[5m])
```

---

#### `pix.webhook.processing.time` (Timer)
**Tipo**: Timer (Histogram)  
**Descrição**: Latência do processamento de webhooks (desde recepção até conclusão).

**Valor de Negócio**:
- Monitorar performance do endpoint de webhook
- Identificar lentidão no processamento assíncrono
- Garantir que webhooks sejam processados rapidamente (importante para idempotência)

**Query Prometheus**:
```promql
# Latência média de processamento
rate(pix_webhook_processing_time_seconds_sum[5m]) / 
rate(pix_webhook_processing_time_seconds_count[5m])

# P95 de processamento
histogram_quantile(0.95, 
  rate(pix_webhook_processing_time_seconds_bucket[5m])
)

# P99 de processamento
histogram_quantile(0.99, 
  rate(pix_webhook_processing_time_seconds_bucket[5m])
)
```

**Alertas Recomendados**:
```yaml
# Alerta: Processamento lento de webhooks
- alert: SlowWebhookProcessing
  expr: |
    histogram_quantile(0.95, 
      rate(pix_webhook_processing_time_seconds_bucket[5m])
    ) > 1
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "P95 de processamento de webhooks acima de 1s"
```

---

#### `pix.webhook.processing.errors` (Counter)
**Tipo**: Counter  
**Tags**: `error_type`

**Descrição**: Erros durante processamento de webhooks, categorizados.

**Tipos de Erro**:
- `transfer_not_found` - Transferência não encontrada (webhook órfão)
- `validation_error` - Erro de validação do payload
- `concurrent_modification` - Conflito de concorrência
- `business_error` - Outras regras de negócio

**Valor de Negócio**:
- Identificar webhooks órfãos (problema de sincronização)
- Detectar problemas de concorrência
- Monitorar saúde da integração

**Query Prometheus**:
```promql
# Erros por tipo
sum by (error_type) (pix_webhook_processing_errors_total)

# Taxa de erros por tipo
rate(pix_webhook_processing_errors_total[5m]) * 60

# Taxa de erro geral
sum(rate(pix_webhook_processing_errors_total[5m])) / 
rate(pix_webhook_processing_time_seconds_count[5m])
```

**Alertas Recomendados**:
```yaml
# Alerta: Muitos webhooks órfãos
- alert: OrphanWebhooks
  expr: |
    rate(pix_webhook_processing_errors_total{error_type="transfer_not_found"}[5m]) > 1
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "Webhooks chegando sem transferência correspondente"
```

---

### 3️⃣ Métricas de Carteiras e Chaves PIX

#### `pix.wallets.created` (Counter)
**Tipo**: Counter  
**Descrição**: Total de carteiras criadas no sistema.

**Valor de Negócio**:
- Monitorar crescimento de usuários
- Planejamento de capacidade
- Métrica de adoção do produto

**Query Prometheus**:
```promql
# Total de carteiras criadas
pix_wallets_created_total

# Taxa de criação por dia
increase(pix_wallets_created_total[1d])

# Crescimento semanal
increase(pix_wallets_created_total[7d])
```

---

#### `pix.wallets.active` (Gauge)
**Tipo**: Gauge  
**Descrição**: Número atual de carteiras ativas no sistema.

**Valor de Negócio**:
- Monitorar base ativa de usuários
- Detectar inativações em massa (possível bug)

**Query Prometheus**:
```promql
# Total de carteiras ativas
pix_wallets_active

# Variação nas últimas 24h
delta(pix_wallets_active[24h])
```

---

#### `pix.pixkeys.registered` (Counter)
**Tipo**: Counter  
**Descrição**: Total de chaves PIX registradas.

**Valor de Negócio**:
- Monitorar engajamento dos usuários
- Medir adoção de funcionalidade

**Query Prometheus**:
```promql
# Total de chaves PIX registradas
pix_pixkeys_registered_total

# Taxa de registro por dia
increase(pix_pixkeys_registered_total[1d])
```

---

#### `pix.pixkeys.by_type` (Counter)
**Tipo**: Counter  
**Tags**: `key_type`

**Descrição**: Chaves PIX categorizadas por tipo.

**Tipos de Chave**:
- `CPF` - Chave baseada em CPF
- `EMAIL` - Chave baseada em e-mail
- `PHONE` - Chave baseada em telefone
- `RANDOM` - Chave aleatória

**Valor de Negócio**:
- Entender preferências dos usuários
- Identificar padrões de uso por tipo de chave

**Query Prometheus**:
```promql
# Chaves por tipo
sum by (key_type) (pix_pixkeys_by_type_total)

# Tipo mais popular
topk(1, sum by (key_type) (pix_pixkeys_by_type_total))

# Distribuição percentual
(
  sum by (key_type) (pix_pixkeys_by_type_total) / 
  sum(pix_pixkeys_by_type_total)
) * 100
```

---

### 4️⃣ Métricas de Transações

#### `pix.deposits.completed` (Counter)
**Tipo**: Counter  
**Descrição**: Total de depósitos completados nas carteiras.

**Valor de Negócio**:
- Monitorar volume de entrada de dinheiro
- Identificar padrões de uso
- Métricas financeiras

**Query Prometheus**:
```promql
# Total de depósitos
pix_deposits_completed_total

# Taxa de depósitos por hora
rate(pix_deposits_completed_total[1h]) * 3600

# Total nas últimas 24h
increase(pix_deposits_completed_total[24h])
```

---

#### `pix.withdrawals.completed` (Counter)
**Tipo**: Counter  
**Descrição**: Total de saques completados das carteiras.

**Valor de Negócio**:
- Monitorar volume de saída de dinheiro
- Balancear com depósitos para entender fluxo de caixa

**Query Prometheus**:
```promql
# Total de saques
pix_withdrawals_completed_total

# Taxa de saques por hora
rate(pix_withdrawals_completed_total[1h]) * 3600

# Comparar depósitos vs saques
pix_deposits_completed_total / pix_withdrawals_completed_total
```

---

## 🔍 Cenários de Troubleshooting

### Cenário 1: Transferências Pendentes Acumulando

**Sintoma**: `pix.transfers.pending` crescendo constantemente

**Investigação**:
```promql
# Verificar se webhooks estão chegando
rate(pix_webhooks_received_total[5m])

# Comparar criações vs confirmações
rate(pix_transfers_created_total[5m]) - rate(pix_transfers_confirmed_total[5m])

# Verificar erros no processamento de webhooks
sum(rate(pix_webhook_processing_errors_total[5m])) by (error_type)
```

**Possíveis Causas**:
- Webhooks não estão sendo entregues (problema no provider)
- Erros no processamento de webhooks
- Lentidão no processamento assíncrono

---

### Cenário 2: Taxa de Rejeição Alta

**Sintoma**: `pix.transfers.rejected` aumentando

**Investigação**:
```promql
# Taxa de rejeição
pix_transfers_rejected_total / pix_transfers_created_total

# Ver erros de criação por tipo
sum by (error_type) (rate(pix_transfer_creation_errors_total[10m]))

# Verificar se validações estão falhando
rate(pix_transfer_creation_errors_total{error_type="validation_error"}[5m])
```

**Possíveis Causas**:
- Validações client-side insuficientes
- Problemas no provider PIX (rejeitando mais transferências)
- Mudança em regras de negócio

---

### Cenário 3: Performance Degradada

**Sintoma**: P95/P99 de `pix.transfer.creation.time` aumentando

**Investigação**:
```promql
# P95 atual vs 1 hora atrás
histogram_quantile(0.95, rate(pix_transfer_creation_time_seconds_bucket[5m]))
vs
histogram_quantile(0.95, rate(pix_transfer_creation_time_seconds_bucket[5m] offset 1h))

# Verificar volume de requisições
rate(pix_transfer_creation_time_seconds_count[5m]) * 60

# Ver distribuição de latência
histogram_quantile(0.50, rate(pix_transfer_creation_time_seconds_bucket[5m])) # P50
histogram_quantile(0.95, rate(pix_transfer_creation_time_seconds_bucket[5m])) # P95
histogram_quantile(0.99, rate(pix_transfer_creation_time_seconds_bucket[5m])) # P99
```

**Possíveis Causas**:
- Aumento de carga (mais requisições)
- Lentidão no banco de dados
- Contenção de recursos (CPU, memória)

---

### Cenário 4: Webhooks Duplicados em Excesso

**Sintoma**: `pix.webhooks.duplicated` muito alto

**Investigação**:
```promql
# Taxa de duplicação
pix_webhooks_duplicated_total / pix_webhooks_received_total

# Ver se está piorando
rate(pix_webhooks_duplicated_total[5m])

# Verificar se há lentidão no processamento
histogram_quantile(0.95, rate(pix_webhook_processing_time_seconds_bucket[5m]))
```

**Possíveis Causas**:
- Provider PIX fazendo muitos retries (possível timeout)
- Processamento de webhook muito lento (provider retrying antes de completar)
- Configuração de timeout inadequada

---

## 📊 Dashboards Recomendados

### Dashboard 1: Visão Geral de Transferências

**Painéis**:
1. **Taxa de Criação** - `rate(pix_transfers_created_total[5m]) * 60`
2. **Transferências Pendentes** - `pix_transfers_pending` (Gauge)
3. **Taxa de Sucesso** - `pix_transfers_confirmed_total / pix_transfers_created_total`
4. **Distribuição de Status**:
   - Confirmadas: `pix_transfers_confirmed_total`
   - Rejeitadas: `pix_transfers_rejected_total`
   - Pendentes: `pix_transfers_pending`

---

### Dashboard 2: Performance

**Painéis**:
1. **Latência de Criação (P50/P95/P99)**:
```promql
histogram_quantile(0.50, rate(pix_transfer_creation_time_seconds_bucket[5m]))
histogram_quantile(0.95, rate(pix_transfer_creation_time_seconds_bucket[5m]))
histogram_quantile(0.99, rate(pix_transfer_creation_time_seconds_bucket[5m]))
```

2. **Latência End-to-End (P95)**:
```promql
histogram_quantile(0.95, rate(pix_transfer_end_to_end_time_seconds_bucket[5m]))
```

3. **Latência de Webhook (P95)**:
```promql
histogram_quantile(0.95, rate(pix_webhook_processing_time_seconds_bucket[5m]))
```

---

### Dashboard 3: Erros e Anomalias

**Painéis**:
1. **Erros de Criação por Tipo**:
```promql
sum by (error_type) (rate(pix_transfer_creation_errors_total[5m]))
```

2. **Erros de Webhook por Tipo**:
```promql
sum by (error_type) (rate(pix_webhook_processing_errors_total[5m]))
```

3. **Taxa de Duplicação de Webhooks**:
```promql
rate(pix_webhooks_duplicated_total[5m]) / rate(pix_webhooks_received_total[5m])
```

---

### Dashboard 4: Métricas de Negócio

**Painéis**:
1. **Crescimento de Usuários** - `increase(pix_wallets_created_total[1d])`
2. **Carteiras Ativas** - `pix_wallets_active`
3. **Volume de Transações** - `increase(pix_deposits_completed_total[24h])` + `increase(pix_withdrawals_completed_total[24h])`
4. **Chaves PIX por Tipo** - `sum by (key_type) (pix_pixkeys_by_type_total)`

---

## 🚀 Como Acessar as Métricas

### 1. Prometheus Endpoint
```bash
# Ver todas as métricas PIX
curl http://localhost:8080/actuator/prometheus | grep pix

# Filtrar métricas específicas
curl http://localhost:8080/actuator/prometheus | grep pix_transfers
```

### 2. Prometheus UI
```
http://localhost:9090
```

Exemplos de queries na UI:
- `pix_transfers_pending` - Ver gauge de pendentes
- `rate(pix_transfers_created_total[5m])` - Taxa de criação
- `histogram_quantile(0.95, rate(pix_transfer_creation_time_seconds_bucket[5m]))` - P95 de latência

### 3. Grafana
```
http://localhost:3000
```

Datasource já configurado: **Prometheus**

---

## 📝 Resumo Executivo

### Métricas Críticas (Monitorar 24/7)
1. ✅ `pix.transfers.pending` - **Principal indicador de saúde**
2. ✅ Taxa de confirmação - `pix_transfers_confirmed_total / pix_transfers_created_total`
3. ✅ P95 de latência end-to-end - Métrica de SLA
4. ✅ Taxa de erro de criação - Detectar problemas sistêmicos

### Métricas de Performance
- `pix.transfer.creation.time` (p50/p95/p99)
- `pix.webhook.processing.time` (p50/p95/p99)
- `pix.transfer.end_to_end.time` (p95/p99)

### Métricas de Negócio
- `pix.transfers.created` - Volume total
- `pix.transfers.confirmed` - Sucesso
- `pix.wallets.active` - Base de usuários
- `pix.deposits.completed` + `pix.withdrawals.completed` - Fluxo financeiro

### Métricas de Qualidade
- `pix.transfer.creation.errors` (por tipo)
- `pix.webhook.processing.errors` (por tipo)
- `pix.webhooks.duplicated` - Efetividade de idempotência

---

## 🎓 Boas Práticas

1. **Use percentis (p95, p99) em vez de médias** para latência - médias escondem outliers
2. **Combine métricas** para insights mais profundos (ex: taxa de sucesso = confirmadas / criadas)
3. **Configure alertas progressivos** - warning → critical
4. **Monitore tendências**, não apenas valores absolutos
5. **Correlacione métricas com logs estruturados** usando correlation_id
6. **Revise thresholds de alertas regularmente** com base em dados históricos

---

## 📚 Referências

- [Micrometer Documentation](https://micrometer.io/docs)
- [Prometheus Query Examples](https://prometheus.io/docs/prometheus/latest/querying/examples/)
- [Grafana Dashboard Best Practices](https://grafana.com/docs/grafana/latest/best-practices/)

---

**Última Atualização**: Sprint 2 - Métricas Customizadas  
**Contato**: Time de Observability PIX Wallet

---
## 🔧 Apêndice: Estado Atual vs Seção Principal

Este apêndice ajusta divergências entre a descrição originalmente planejada e o código hoje:

| Aspecto | Documentação original | Implementação atual | Ação recomendada futura |
|---------|-----------------------|---------------------|-------------------------|
| Counters de erro (`pix.transfer.creation.errors`, `pix.webhook.processing.errors`) | Existentes e usados em queries | NÃO existem; erros registrados via timers com tags `status=error` e `error_type` | Criar counters dedicados para simplificar queries e alertas |
| Tags de tipo em webhooks | `event_type` | `eventType` | Padronizar para snake_case ou ajustar dashboards para camelCase |
| Tags de tipo em pixkeys | `key_type` | `keyType` | Idem acima |
| Queries de erro (transfer/webhook) | Usam *_errors_total | Devem usar `*_time_seconds_count{status="error"}` | Atualizar dashboards/alertas |
| Métricas de qualidade listadas | Incluem counters de erro | Devem referenciar timers com status=error | Atualizar documentação principal (feito parcialmente no README) |

### Exemplos Corrigidos de Queries de Erro
```promql
# Transferências - erros por tipo
sum by (error_type) (rate(pix_transfer_creation_time_seconds_count{status="error"}[5m]))

# Webhooks - erros por tipo
sum by (error_type) (rate(pix_webhook_processing_time_seconds_count{status="error"}[5m]))
```

### Nota
Mantivemos o corpo principal para referência histórica; utilize este apêndice para qualquer automação ou criação de dashboards até os ajustes de código serem implementados.
