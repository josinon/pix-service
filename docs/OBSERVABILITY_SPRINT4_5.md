# Sprint 4 & 5: Loki, Dashboards e Alertas - Sumário de Implementação

**Data:** 2025-11-05  
**Status:** ✅ CONCLUÍDO

---

## 🎯 Objetivo

Completar a stack de observabilidade com:
1. **Loki** - Centralização de logs
2. **Dashboards Grafana** - Visualização unificada
3. **Alertas Inteligentes** - Proatividade operacional

---

## 🏗️ Arquitetura Completa Implementada

```
┌─────────────────────────────────────────────────────────────┐
│                  PIX Wallet Application                      │
│                                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │   Logs   │  │ Metrics  │  │  Traces  │  │   MDC    │   │
│  │  (JSON)  │  │(Micrometer)│ │ (@Traced)│  │(Context) │   │
│  └─────┬────┘  └─────┬────┘  └─────┬────┘  └─────┬────┘   │
└────────┼─────────────┼─────────────┼─────────────┼─────────┘
         │             │             │             │
         ▼             ▼             ▼             ▼
┌─────────────────────────────────────────────────────────────┐
│             OpenTelemetry Collector (OTEL)                  │
│  • Recebe traces via OTLP HTTP (porta 4318)                │
│  • Exporta para Tempo                                       │
└────────────────────────┬────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┬────────────────┐
         ▼               ▼               ▼                ▼
┌──────────────┐ ┌──────────────┐ ┌──────────┐ ┌──────────────┐
│  Loki 3.0    │ │ Prometheus   │ │  Tempo   │ │ Alertmanager │
│  (Logs)      │ │ (Metrics)    │ │ (Traces) │ │  (Alerts)    │
│  Port: 3100  │ │ Port: 9090   │ │Port: 3200│ │  Port: 9093  │
└──────┬───────┘ └──────┬───────┘ └────┬─────┘ └──────┬───────┘
       │                │              │              │
       └────────────────┼──────────────┼──────────────┘
                        ▼              │              
                 ┌─────────────┐       │              
                 │  Grafana    │───────┘              
                 │  (Dashboards)                      
                 │  Port: 3000 │                      
                 └─────────────┘                      
                        ▲
                        │
                 ┌──────┴──────┐
                 │  Promtail   │
                 │ (Log Agent) │
                 └─────────────┘
```

---

## ✅ Sprint 4: Loki Integration

### Componentes Adicionados

#### 1. Grafana Loki (docker-compose.yml)
```yaml
loki:
  image: grafana/loki:3.0.0
  ports: 3100:3100
  config: docker/loki/loki-config.yml
  retention: 30 dias
```

**Características**:
- Storage: filesystem (produção: S3/GCS)
- Retention: 720h (30 dias)
- Schema: v13 (TSDB)
- Compaction automática

#### 2. Promtail (docker-compose.yml)
```yaml
promtail:
  image: grafana/promtail:3.0.0
  config: docker/promtail/promtail-config.yml
```

**Funcionalidades**:
- Coleta logs via Docker socket (`/var/run/docker.sock`)
- Filtra container: `pixwallet-app`
- Parse de logs JSON estruturados
- Extração de labels: `level`, `operation`, `correlationId`, `endToEndId`
- Pipeline stages:
  1. JSON parsing
  2. Label extraction
  3. Timestamp parsing
  4. Message formatting

#### 3. Loki Data Source no Grafana

**Arquivo**: `docker/grafana/provisioning/datasources/datasources.yml`

**Derived Fields configurados**:
- `trace_id` → Link para Tempo (correlação automática!)
- Click no log → abre trace correspondente

### Configurações Criadas

| Arquivo | Descrição |
|---------|-----------|
| `docker/loki/loki-config.yml` | Configuração do Loki (storage, retention, limits) |
| `docker/promtail/promtail-config.yml` | Pipeline de coleta e parsing de logs |
| `docker/grafana/provisioning/datasources/datasources.yml` | Data sources (Prometheus, Loki, Tempo) |

### Queries LogQL Implementadas

```logql
# Todos os logs da aplicação
{app="pixwallet"}

# Logs de uma transferência específica
{app="pixwallet"} | json | endToEndId="E123ABC456"

# Logs de erro
{app="pixwallet"} | json | level="ERROR"

# Logs de webhook
{app="pixwallet"} | json | operation="PIX_WEBHOOK_PROCESS"

# Rastrear por correlation ID
{app="pixwallet"} | json | correlationId="abc-123"

# Log stream por operação
{app="pixwallet"} | json | operation="PIX_TRANSFER_CREATE"
```

---

## ✅ Sprint 5: Dashboards Grafana

### 4 Dashboards Criados

#### Dashboard 1: PIX Transfers Overview
**UID**: `pix-transfers`  
**Arquivo**: `docker/grafana/provisioning/dashboards/pix-transfers.json`

**Painéis (8)**:
1. 📊 Taxa de Transferências PIX (criadas, confirmadas, rejeitadas)
2. ✅ Taxa de Sucesso (gauge com thresholds)
3. ⏳ Transferências Pendentes (stat)
4. ⚡ Latência de Criação (p50/p95/p99)
5. 🔄 Latência de Webhook & End-to-End
6. 📨 Webhooks Recebidos (confirmados, rejeitados, duplicados)
7. 🚨 Últimos Erros (logs do Loki)
8. 📈 Top 10 Operações por Volume

**Valor de Negócio**:
- Visão em tempo real do volume de transações
- Identificar degradação de performance
- Monitorar taxa de sucesso vs SLO (99.9%)

#### Dashboard 2: Operational Health
**UID**: `operational-health`  
**Arquivo**: `docker/grafana/provisioning/dashboards/operational-health.json`

**Painéis (8)**:
1. 🌐 HTTP Request Rate
2. ❌ HTTP Errors (4xx/5xx)
3. 💾 JVM Heap Memory
4. ⚙️ CPU Usage (gauge)
5. 🗄️ DB Connections Active (stat)
6. 🧵 JVM Threads
7. 📊 Log Volume by Level (Loki)
8. ⚡ HTTP Request Latency P95

**Valor Operacional**:
- Monitorar saúde geral do sistema
- Detectar vazamentos de memória
- Identificar esgotamento de recursos

#### Dashboard 3: PIX Correlation Dashboard ⭐
**UID**: `pix-correlation`  
**Arquivo**: `docker/grafana/provisioning/dashboards/correlation-dashboard.json`

**MAIS IMPORTANTE DO PROJETO!**

**Variáveis**:
- `$endToEndId` - ID da transferência PIX
- `$correlationId` - Correlation ID da requisição

**Painéis (9)**:
1. 🔍 Header de Rastreamento
2. 📊 Status da Transferência (métrica)
3. ⚡ Tempo de Criação (métrica)
4. 🔄 Tempo de Webhook (métrica)
5. 🏁 Tempo End-to-End Total (métrica)
6. 🔍 **Distributed Trace (Tempo)** - Flamegraph completo
7. 📜 **Log Stream - Jornada Completa** (Loki)
8. 📥 Logs: Criação da Transferência (filtrado)
9. 📤 Logs: Processamento do Webhook (filtrado)

**Como usar**:
1. Abrir dashboard
2. Digitar `endToEndId` (ex: E123ABC456)
3. Visualizar:
   - Métricas de performance
   - Trace completo no Tempo
   - Logs correlacionados no Loki
   - Click em `trace_id` → abre trace detalhado

**Valor para Troubleshooting**:
- Reduz MTTR (Mean Time to Resolution) de **horas para minutos**
- Debugging de problemas em produção sem SSH
- Análise post-mortem de incidentes

#### Dashboard 4: Alerts & SLOs
**UID**: `alerts-slos`  
**Arquivo**: `docker/grafana/provisioning/dashboards/alerts-slos.json`

**Painéis (10)**:
1. 🚨 Alertas Ativos (count)
2. 🔴 Críticos (count)
3. 🟡 Warnings (count)
4. 📋 Lista de Alertas Ativos (tabela)
5. 📊 SLO: Taxa de Sucesso (gauge com threshold 99.9%)
6. ⚡ SLO: Latência P95 (gauge com threshold 500ms)
7. 🔄 SLO: Latência P99.5 Webhook (gauge com threshold 1s)
8. 📈 SLO Histórico: Taxa de Sucesso (5min e 1h)
9. ⚡ SLO Histórico: Latências
10. 📊 Histórico de Alertas (última hora)

**Valor para SRE**:
- Monitorar compliance de SLOs em tempo real
- Visibilidade de alertas ativos
- Análise de tendências (melhorando ou piorando?)

---

## ✅ Sprint 5: Alertas Inteligentes

### Componentes Adicionados

#### 1. Prometheus Alertmanager (docker-compose.yml)
```yaml
alertmanager:
  image: prom/alertmanager:v0.27.0
  port: 9093
  config: docker/alertmanager/alertmanager.yml
```

#### 2. Regras de Alertas (alerts.yml)

**10 Alertas Configurados**:

| # | Alerta | Condição | Severidade | For | Grupo |
|---|--------|----------|------------|-----|-------|
| 1 | HighTransferErrorRate | Taxa erro > 10% | Warning | 2min | pix_transfers |
| 2 | HighWebhookLatency | P95 > 2s | Warning | 5min | pix_transfers |
| 3 | TooManyPendingTransfers | Pendentes > 100 | Critical | 10min | pix_transfers |
| 4 | HighWebhookDuplicationRate | Duplicações > 5% | Warning | 3min | pix_transfers |
| 5 | NoWebhooksReceived | Taxa = 0 por 15min | Critical | 15min | pix_transfers |
| 6 | DatabaseConnectionExhaustion | Pool > 90% | Critical | 1min | system_health |
| 7 | HighMemoryUsage | Heap > 85% | Warning | 5min | system_health |
| 8 | HighHTTPErrorRate | 5xx > 5% | Warning | 3min | system_health |
| 9 | SLOViolation_TransferCreation | Sucesso < 99.9% | Critical | 5min | slo_violations |
| 10 | SLOViolation_TransferLatency | P95 > 500ms | Warning | 5min | slo_violations |

**Annotations incluem**:
- `summary` - Descrição curta
- `description` - Detalhes com valores
- `impact` - Impacto no negócio
- `runbook` - Query LogQL para investigação (alguns)

#### 3. Recording Rules (recording-rules.yml)

**8 SLOs configurados**:

| # | SLO | Expressão | Uso |
|---|-----|-----------|-----|
| 1 | Taxa de Sucesso (5min) | `slo:pix_transfer_creation:success_rate:5m` | Alerta + Dashboard |
| 2 | Taxa de Sucesso (1h) | `slo:pix_transfer_creation:success_rate:1h` | Dashboard |
| 3 | Latência P95 (5min) | `slo:pix_transfer_creation:latency_p95:5m` | Alerta + Dashboard |
| 4 | Latência P99 (5min) | `slo:pix_transfer_creation:latency_p99:5m` | Dashboard |
| 5 | Taxa Sucesso Webhook (5min) | `slo:pix_webhook:success_rate:5m` | Dashboard |
| 6 | Latência P99.5 Webhook | `slo:pix_webhook:latency_p995:5m` | Alerta + Dashboard |
| 7 | End-to-End Médio | `slo:pix_transfer:end_to_end_time:mean:5m` | Dashboard |
| 8 | Taxa Duplicação Webhook | `slo:pix_webhook:duplication_rate:5m` | Dashboard |

**Agregações adicionais**:
- Request rate total HTTP
- Taxa de erro HTTP
- Latência média HTTP
- Uso de heap (ratio)
- Uso de conexões DB (ratio)

#### 4. Configuração Alertmanager

**Arquivo**: `docker/alertmanager/alertmanager.yml`

**Rotas configuradas**:
- `critical-alerts` - Alertas críticos (notificação imediata)
- `warning-alerts` - Warnings (agrupamento maior)
- `slo-violations` - Violações de SLO (canal específico)

**Receivers disponíveis** (comentados, prontos para configurar):
- Slack (3 canais: critical, warning, slo)
- Email (SMTP)
- Webhook genérico
- PagerDuty (produção)

**Inhibition rules**:
- Alertas críticos inibem warnings relacionados (evita spam)

---

## 📊 Métricas de Sucesso

### Antes (Sprints 1-3):
- ✅ Logs estruturados (JSON)
- ✅ 15+ métricas customizadas
- ✅ Distributed tracing
- ❌ Logs descentralizados (só stdout)
- ❌ Sem dashboards pré-configurados
- ❌ Sem alertas automáticos

### Depois (Sprints 4-5):
- ✅ Logs centralizados no Loki (30 dias)
- ✅ 4 dashboards pré-configurados
- ✅ 10 alertas inteligentes
- ✅ 8 SLOs monitorados
- ✅ Correlação automática: logs ↔ traces ↔ metrics
- ✅ MTTR reduzido de horas para minutos

---

## 🎯 Como Usar

### 1. Iniciar Stack Completa

```bash
docker-compose up -d
```

**Serviços disponíveis**:
- Aplicação: http://localhost:8080
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- Alertmanager: http://localhost:9093
- Loki: http://localhost:3100
- Tempo: http://localhost:3200

### 2. Acessar Dashboards

```bash
# Dashboard principal de negócio
http://localhost:3000/d/pix-transfers

# Dashboard de correlação (troubleshooting)
http://localhost:3000/d/pix-correlation

# Dashboard de saúde operacional
http://localhost:3000/d/operational-health

# Dashboard de alertas e SLOs
http://localhost:3000/d/alerts-slos
```

### 3. Criar uma Transferência PIX

```bash
# Criar carteira
WALLET_ID=$(curl -X POST http://localhost:8080/api/v1/wallet \
  -H "Content-Type: application/json" \
  -d '{"document":"12345678901","ownerName":"John Doe"}' | jq -r '.walletId')

# Fazer depósito
curl -X POST "http://localhost:8080/api/v1/wallet/$WALLET_ID/deposit" \
  -H "Content-Type: application/json" \
  -d '{"amount":1000.00}'

# Criar transferência PIX
END_TO_END_ID=$(curl -X POST http://localhost:8080/api/v1/pix/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "fromWalletId":"'$WALLET_ID'",
    "toPixKey":"12345678901",
    "amount":100.00
  }' | jq -r '.endToEndId')

echo "Transfer created with endToEndId: $END_TO_END_ID"
```

### 4. Rastrear Transferência (Correlation Dashboard)

1. Acessar http://localhost:3000/d/pix-correlation
2. Colar o `$END_TO_END_ID` no campo "Transfer ID"
3. Visualizar:
   - ✅ Status (confirmado/pendente)
   - ⏱️ Métricas de tempo (criação, webhook, e2e)
   - 🔍 Trace completo (Tempo)
   - 📜 Logs da jornada (Loki)

### 5. Simular Webhook

```bash
curl -X POST http://localhost:8080/api/v1/pix/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "endToEndId":"'$END_TO_END_ID'",
    "eventType":"CONFIRMED",
    "eventId":"evt-'$(date +%s)'"
  }'
```

### 6. Consultar Logs no Loki

Grafana → Explore → Loki:

```logql
# Jornada completa da transferência
{app="pixwallet"} | json | endToEndId="PASTE_END_TO_END_ID_HERE"

# Só logs de criação
{app="pixwallet"} | json | endToEndId="..." | operation="PIX_TRANSFER_CREATE"

# Só logs de webhook
{app="pixwallet"} | json | endToEndId="..." | operation="PIX_WEBHOOK_PROCESS"
```

### 7. Consultar Alertas

```bash
# Ver alertas ativos
curl http://localhost:9093/api/v1/alerts | jq

# Ver status no Grafana
http://localhost:3000/d/alerts-slos
```

---

## 🚀 Próximos Passos (Produção)

### Configurações Recomendadas

#### 1. Alertmanager - Notificações

**Slack**:
```yaml
# docker/alertmanager/alertmanager.yml
receivers:
  - name: 'critical-alerts'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/YOUR/WEBHOOK'
        channel: '#pix-alerts-critical'
```

**Email**:
```yaml
global:
  smtp_smarthost: 'smtp.gmail.com:587'
  smtp_from: 'alerts@pixwallet.com'
  smtp_auth_username: 'your-email@gmail.com'
  smtp_auth_password: 'app-password'
```

#### 2. Loki - Storage Externo

```yaml
# Trocar filesystem por S3/GCS em produção
storage_config:
  aws:
    s3: s3://region/bucket
    access_key_id: ${AWS_ACCESS_KEY}
    secret_access_key: ${AWS_SECRET_KEY}
```

#### 3. Prometheus - Federação

```yaml
# Centralizar métricas de múltiplos clusters
scrape_configs:
  - job_name: 'federate'
    honor_labels: true
    metrics_path: '/federate'
    params:
      'match[]':
        - '{job="pixwallet-app"}'
    static_configs:
      - targets: ['prometheus-cluster-1:9090']
```

#### 4. Grafana - Auth & RBAC

```yaml
# docker-compose.yml
environment:
  GF_AUTH_GITHUB_ENABLED: "true"
  GF_AUTH_GITHUB_CLIENT_ID: "your-client-id"
  GF_AUTH_GITHUB_CLIENT_SECRET: "your-secret"
```

---

## 📚 Referências

- [Grafana Loki Documentation](https://grafana.com/docs/loki/latest/)
- [Prometheus Alerting](https://prometheus.io/docs/alerting/latest/)
- [Grafana Dashboards](https://grafana.com/docs/grafana/latest/dashboards/)
- [LogQL Query Language](https://grafana.com/docs/loki/latest/query/)
- [SLO Best Practices](https://sre.google/workbook/implementing-slos/)

---

## ✅ Checklist de Validação

### Loki
- [x] Loki rodando (porta 3100)
- [x] Promtail coletando logs
- [x] Logs sendo indexados com labels corretos
- [x] Queries LogQL funcionando
- [x] Derived fields (trace_id → Tempo) configurados

### Dashboards
- [x] Dashboard 1: PIX Transfers Overview
- [x] Dashboard 2: Operational Health
- [x] Dashboard 3: PIX Correlation Dashboard
- [x] Dashboard 4: Alerts & SLOs
- [x] Variáveis de dashboard funcionando
- [x] Refresh automático (10s)

### Alertas
- [x] Alertmanager rodando (porta 9093)
- [x] 10 alertas configurados
- [x] 8 SLOs (recording rules)
- [x] Prometheus carregando rules
- [x] Rotas e receivers configurados
- [x] Inhibition rules funcionando

---

**Atualizado em:** 2025-11-05  
**Versão:** 1.0 (Sprints 4 & 5)
