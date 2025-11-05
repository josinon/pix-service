# PIX Wallet Service
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16.4-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)
![Coverage](https://img.shields.io/badge/Coverage-70%25-green)

Serviço de carteira digital PIX construído em **Spring Boot** seguindo princípios de **Clean Architecture** e **Hexagonal Architecture**, com foco em: consistência financeira, segurança contra condições de corrida, idempotência em múltiplos pontos, rastreabilidade end-to-end e escalabilidade futura.

---

## 📋 Índice

1. Visão Geral
2. Arquitetura
3. Decisões Arquiteturais Principais
4. Tradeoffs e Riscos
5. Concorrência & Race Conditions
6. Modelo Financeiro & Ledger
7. Observabilidade (Resumo)
8. Testes & Qualidade
9. Execução & Ambientes
10. Roadmap de Escalabilidade
11. Futuras Melhorias
12. API
13. Estrutura do Projeto
14. Licença & Autor

---

## 🎯 1. Visão Geral

O sistema provê:
- Criação de carteiras e chaves PIX
- Depósitos, saques e transferências com idempotência
- Ledger append-only auditável
- Confirmação assíncrona de transferências via webhook
- Observabilidade completa (métricas, logs estruturados, tracing, dashboards)

Pilares atuais:
- Confiabilidade operacional
- Auditabilidade detalhada
- Base para escala horizontal (>100k carteiras) com evolução progressiva

---

## 🏗️ 2. Arquitetura

Camadas (Ports & Adapters):
```
Presentation → Application → Domain → Infrastructure
```
Principais características:
- Separation of concerns clara
- Dependências direcionadas para o domínio
- DTOs externos isolados
- Validadores de domínio ricos (PixKeyValidator / TransferValidator)
- Idempotência aplicada em serviços críticos e no processamento de webhook

Validação em 3 níveis:
1. Presentation (Bean Validation) – forma & presença
2. Application – idempotência, coordenação e regras agregadas
3. Domain – integridade semântica (formatos PIX, limites, regras de evento)

---

## 🧭 3. Decisões Arquiteturais Principais

| Decisão | Motivação | Resultado |
|---------|-----------|-----------|
| Ledger append-only | Auditoria e reconstrução histórica fácil | Consulta de saldo via agregação SUM |
| Webhook assíncrono para confirmação | Reduz acoplamento e permite integração externa simulada | Necessário correlacionar por endToEndId |
| Idempotência multi-nível (depósito, saque, transferência, webhook) | Evitar duplicação em reenvios ou concorrência | Requisições repetidas retornam estado idempotente |
| Optimistic locking em transferências | Simplicidade e menor bloqueio | Possível contenda em alta concorrência, mitigada por version checks |
| Validação estratificada | Garantir erro rápido no nível correto | Mensagens consistentes e testes focados |
| Logs estruturados + MDC | Correlação entre requisição e evento assíncrono | Investigação rápida em Loki/Grafana |
| Métricas personalizadas | Observação de SLAs de negócio | Dashboards e alertas orientados a outcomes |

---

## ⚖️ 4. Tradeoffs e Riscos

| Tema | Benefícios | Riscos / Custos | Mitigação Atual | Próxima Mitigação |
|------|------------|-----------------|------------------|------------------|
| Append-only ledger | Auditoria forte; histórico completo | Query de saldo pode ficar lenta | Índices + agregação por walletId | Materialização incremental / snapshots |
| Webhook assíncrono | Desacoplamento; ecossistema realista | Latência end-to-end; correlação | endToEndId + métricas | Retries/backoff + dead-letter |
| Optimistic locking | Menos bloqueio; simples | Write skew em alta concorrência | Validação de saldo defensiva | Stored procedure atômica / advisory lock |
| Idempotência local | Evita duplicações | Janela entre débito/crédito | Chaves separadas | Chave composta / atomic ledger apply |
| Saldo via SUM | Evita estado derivado precoce | Escalabilidade limitada | Índices + possíveis caches | CQRS read model |
| Amostragem de falhas | Reduz ruído | Perda de eventos raros | Limite configurável | Export manual completo |

---

## 🔄 5. Concorrência & Race Conditions

Riscos atuais:
1. Overspending em saques simultâneos
2. Dupla aplicação de transferência em reprocessamento de webhook
3. Contenda em atualização de status

Mitigações implementadas:
- Idempotência por operação e por webhook (eventId / endToEndId)
- Verificação de saldo em serviço + checagem defensiva no adaptador
- Optimistic locking em Transfer (version)
- Filtro de duplicidade de evento

Roadmap de robustez:
- Stored procedure atômica de débito
- Unificação de idempotência apply (debitar + creditar + status)
- Advisory locks por walletId em alto volume
- Backoff e retry para falhas transitórias de confirmação

---

## 💰 6. Modelo Financeiro & Ledger

- Ledger append-only (DEBIT/CREDIT)
- Saldo atual = SUM(entries) por walletId
- Transfer PENDING → CONFIRMED/REJECTED via webhook
- Idempotência garante não duplicação de depósito/saque

Evoluções planejadas:
- Tabela materializada de saldo (wallet_balance)
- Reconciliação periódica
- Suporte futuro a múltiplas moedas

---

## 🔍 7. Observabilidade (Guia)

Esta seção documenta de forma estática como a observabilidade foi implementada e como utilizá‑la em cenários comuns de diagnóstico. Não reflete cronologia de implementação, e sim o estado atual da solução.

### 7.1 Visão Geral dos Pilares
| Pilar | Implementação | Valor | Como acessar |
|-------|---------------|-------|--------------|
| Logs Estruturados | Logback + Logstash Encoder (JSON) + MDC | Correlação e análise rápida | Grafana Explore (Loki) |
| Métricas | Micrometer + Actuator + Prometheus | SLAs e saúde de negócio | /actuator/prometheus + Grafana |
| Tracing Distribuído | OpenTelemetry SDK + OTEL Collector + Tempo | Latência e fluxo end-to-end | Grafana Explore (Tempo) |
| Dashboards | Grafana (provisionados) | Visualização consolidada | http://localhost:3000 | 
| Alertas & SLO | Prometheus + Alertmanager | Detecção proativa | http://localhost:9093 |

Arquitetura resumida:
```
App (Logs / Métricas / Traces) → OTEL Collector → { Prometheus, Loki, Tempo } → Grafana / Alertmanager
```

### 7.2 Componentes Implementados
- MDC com chaves: correlationId, operation, walletId, endToEndId, transferId, eventId, userId
- Métricas de negócio e técnica (pix.transfers.*, pix.webhooks.*, pix.transfers.pending, timers de criação e end-to-end)
- Spans atualmente instrumentados: pix.transfer.create, pix.webhook.process (não há span pix.transfer.apply ainda)
- Dashboards: Transfers Overview, Correlation, Operational Health, Alerts & SLOs
- Alertas: erro elevado, latência, pendências excessivas, duplicação de webhooks
- Observação: atributos de negócio (endToEndId, walletId, transferId) NÃO estão presentes nos spans – apenas nos logs. Correlação de trace → negócio deve usar trace_id dos logs.

### 7.3 Endpoints & Portas
| Serviço | URL | Uso |
|---------|-----|-----|
| Aplicação | http://localhost:8080 | API + Actuator |
| Prometheus | http://localhost:9090 | Queries métricas |
| Grafana | http://localhost:3000 (admin/admin) | Dashboards / Explore |
| Alertmanager | http://localhost:9093 | Alertas ativos |
| Loki (via Grafana) | Explore → Data Source Loki | Logs JSON |
| Tempo (via Grafana) | Explore → Data Source Tempo | Traces |

### 7.4 Acesso Rápido
```bash
# Métricas PIX
curl -s http://localhost:8080/actuator/prometheus | grep pix

# Health
curl -s http://localhost:8080/actuator/health
```

### 7.5 Padrões de Log (MDC)
Exemplo:
```json
{
  "timestamp":"2025-11-05T10:15:00Z",
  "level":"INFO",
  "operation":"PIX_TRANSFER_CREATE",
  "correlationId":"corr-123",
  "endToEndId":"EABC123XYZ",
  "walletId":"wallet-uuid",
  "transferId":"transfer-uuid",
  "message":"PIX transfer created successfully",
  "trace_id":"...",
  "span_id":"..."
}
```

### 7.6 Métricas Principais
| Métrica | Tipo | Descrição | Uso Operacional |
|---------|------|-----------|-----------------|
| pix.transfers.created | Counter | Total de transferências criadas | Volume / carga |
| pix.transfers.confirmed | Counter | Transferências confirmadas | Taxa de sucesso |
| pix.transfers.rejected | Counter | Transferências rejeitadas | Qualidade / falhas externas |
| pix.transfers.pending | Gauge | Em estado PENDING | Fila / atraso |
| pix.transfer.creation.time | Timer | Latência de criação | Performance endpoint |
| pix.transfer.end_to_end.time | Timer | Criação → confirmação | SLA de fluxo |
| pix.webhooks.received | Counter | Webhooks totais | Atividade externa |
| pix.webhooks.duplicated | Counter | Eventos duplicados | Idempotência / ruído |
| pix.webhook.processing.time | Timer | Latência processamento webhook | Performance assíncrona |

Nota: Erros de criação/processamento são marcados via timers com tag status="error" e error_type, não há counters dedicados de erro.

Queries úteis:
```promql
histogram_quantile(0.95, rate(pix_transfer_end_to_end_time_seconds_bucket[5m]))
pix_transfers_pending
rate(pix_webhooks_duplicated_total[5m])
```

### 7.7 Tracing
Spans principais:
- pix.transfer.create (SERVER)
- pix.webhook.process (SERVER)

Limitações atuais:
- Atributos de negócio (endToEndId, walletId, transferId, eventId) não são adicionados aos spans – apenas logs possuem esses campos via MDC.
- Para correlacionar uma transferência a um trace: capture trace_id em um log (com endToEndId presente) e busque o trace por trace_id no Tempo.

Como consultar:
1. Grafana → Explore → Tempo
2. Filtro por `name="pix.transfer.create"` ou `name="pix.webhook.process"`
3. Use trace_id obtido nos logs para foco em uma jornada específica.

### 7.8 Casos de Uso Comuns de Observabilidade

1. Jornada completa de uma transferência
  - Query logs: `{app="pixwallet"} | json | endToEndId="EABC123XYZ"`
  - Métrica SLA: `histogram_quantile(0.95, rate(pix_transfer_end_to_end_time_seconds_bucket[5m]))`
  - Trace: buscar `endToEndId` em Tempo.

2. Diagnosticar falha de webhook
  - Logs: `{app="pixwallet"} | json | operation="PIX_WEBHOOK_PROCESS" | level="ERROR"`
  - Métrica duplicação: `rate(pix_webhooks_duplicated_total[5m])`
  - Verificar latência: `histogram_quantile(0.95, rate(pix_webhook_processing_time_seconds_bucket[5m]))`

3. Investigar lentidão na criação de transferência
  - Métrica: `histogram_quantile(0.95, rate(pix_transfer_creation_time_seconds_bucket[5m]))`
  - Trace: span `pix.transfer.create` (ver sub-spans de persistência)
  - Logs: procurar WARN/ERROR junto ao correlationId.

4. Detectar duplicações
  - Métrica: `rate(pix_webhooks_duplicated_total[10m]) > 0.05`
  - Dashboard Correlation → filtrar por endToEndId específico e validar apenas um apply.

5. Verificar saúde geral
  - Dashboard Operational Health.
  - Prometheus: erros HTTP `rate(http_server_requests_seconds_count{status="5xx"}[5m])`
  - Heap / pool conexões se disponível via Actuator.

### 7.9 Alertas Essenciais (Prometheus)
| Alerta | Objetivo | Exemplo Expr |
|--------|----------|--------------|
| HighTransferErrorRate | Detectar falhas em criação | rate(pix_transfer_creation_time_seconds_count{status="error"}[5m]) / rate(pix_transfer_creation_time_seconds_count[5m]) > 0.10 |
| HighWebhookLatency | Latência de processamento | histogram_quantile(0.95, rate(pix_webhook_processing_time_seconds_bucket[5m])) > 2 |
| TooManyPendingTransfers | Atraso de confirmação | pix_transfers_pending > 100 |
| HighWebhookDuplicationRate | Problema de idempotência upstream | (rate(pix_webhooks_duplicated_total[5m]) / rate(pix_webhooks_received_total[5m])) > 0.30 |

### 7.10 Fluxo de Investigação Recomendado
1. Sintoma (ex: alta latência) → consultar dashboard
2. Confirmar em métricas Prometheus (quantificar impacto)
3. Capturar trace_id em um log relacionado (usa correlationId / endToEndId no log)
4. Buscar trace no Tempo pelo trace_id
5. Correlacionar spans e logs (mesmo trace_id)
6. Validar se alerta disparou / threshold adequado
7. Registrar causa e ação em playbook interno (futuro)

### 7.11 Referências
- [Plano de Observabilidade](docs/OBSERVABILITY_PLAN.md)
- [Guia de Métricas](docs/METRICS_GUIDE.md)
- [Guia de Tracing](docs/TRACING_GUIDE.md)

---

---

## 🧪 8. Testes & Qualidade

Pirâmide:
- Unit (validadores, serviços)
- Integration (Testcontainers)
- Concurrency / Load (k6 scripts externos)

Comandos principais:
```bash
mvn test
mvn verify
open target/site/jacoco/index.html
```

Cobertura alvo ≥70% (validadores ≈96%).
Próximos passos: chaos tests, automação de carga em CI, teste de reconciliação.

---

## 🚀 9. Execução & Ambientes

Infra:
```bash
docker-compose up -d
```
Aplicação:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
Ou Docker:
```bash
docker build -t pix-wallet:latest .
docker-compose up app
```

Variáveis relevantes: `WEBHOOK_TIMESTAMP_SKEW_MS` para testes de confirmação.

---

## 📈 10. Roadmap de Escalabilidade

1. Índices avançados (walletId, endToEndId)
2. Materialização de saldo + atomicidade
3. Particionamento / sharding
4. CQRS read model
5. Event streaming (Kafka) para transfer & webhook
6. Rate limiting adaptativo
7. Arquivamento de ledger antigo

---

## 🔮 11. Futuras Melhorias

- Stored procedure atômica (débitos)
- Idempotência consolidada na aplicação de transferência
- Retry/backoff inteligente para webhooks
- Reconciliação automática de saldo
- Auto-scaling baseado em SLA end-to-end
- Materialização incremental + reparo
- Tracing mais granular em validação
- Testes de caos (latência DB, falha de rede)

---

## 📚 12. API (Resumo)

```
POST /api/v1/wallet
GET  /api/v1/wallet/{id}/balance
POST /api/v1/wallet/{id}/deposit
POST /api/v1/wallet/{id}/withdraw
POST /api/v1/pix/transfers
POST /api/v1/pix/webhook
```
Swagger: http://localhost:8080/swagger-ui.html | OpenAPI: http://localhost:8080/v3/api-docs
Actuator: http://localhost:8080/actuator/health, http://localhost:8080/actuator/prometheus

---

## 📂 13. Estrutura do Projeto

```
pix-service/
  docs/
  docker/
  src/main/java/org/pix/wallet/
    presentation/
    application/
    domain/
    infrastructure/
  src/test/java/org/pix/wallet/
```

---

## 📝 14. Licença & Autor

Licença: MIT
Autor: **Josino Neto** (github.com/josinon)

---

<!-- Conteúdo legado removido -->
