# PIX Wallet Service

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16.4-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)
![Coverage](https://img.shields.io/badge/Coverage-70%25+-green)

Sistema de carteira digital PIX desenvolvido com Spring Boot, seguindo princípios de **Clean Architecture** e **Hexagonal Architecture**. O projeto implementa operações bancárias com foco em **escalabilidade**, **concorrência** e **observabilidade**.

---

## 📋 Índice

- [Visão Geral](#-visão-geral)
- [Tecnologias Utilizadas](#-tecnologias-utilizadas)
- [Arquitetura](#-arquitetura)
  - [Arquitetura de Validação](#-arquitetura-de-validação)
- [Funcionalidades](#-funcionalidades)
- [Pré-requisitos](#-pré-requisitos)
- [Como Executar](#-como-executar)
- [Testes](#-testes)
- [Observabilidade](#-observabilidade)
- [API Documentation](#-api-documentation)
- [Estrutura do Projeto](#-estrutura-do-projeto)

---

## 🎯 Visão Geral

O **PIX Wallet Service** é uma aplicação de carteira digital que permite:
- Criação e gerenciamento de carteiras
- Registro e gerenciamento de chaves PIX
- Operações de **depósito** e **saque** com idempotência
- Consulta de saldo atual e histórico
- Transferências PIX entre carteiras

O sistema foi desenvolvido com ênfase em:
- ✅ **Concorrência**: Tratamento de requisições simultâneas
- ✅ **Idempotência**: Evita duplicação de transações
- ✅ **Auditabilidade**: Registro completo de todas as operações
- ✅ **Observabilidade**: Métricas, logs e tracing distribuído

---

## 🚀 Tecnologias Utilizadas

### Backend Core
| Tecnologia | Versão | Propósito |
|------------|--------|-----------|
| **Java** | 17 | Linguagem principal do projeto |
| **Spring Boot** | 3.5.7 | Framework principal para desenvolvimento |
| **Spring Data JPA** | 3.5.7 | Persistência e ORM |
| **PostgreSQL** | 16.4 | Banco de dados relacional principal |
| **Flyway** | - | Versionamento e migração de schema |
| **Lombok** | 1.18.32 | Redução de boilerplate code |

### Validação e Documentação
| Tecnologia | Versão | Propósito |
|------------|--------|-----------|
| **Bean Validation** | 3.x | Validação de dados de entrada |
| **SpringDoc OpenAPI** | 2.5.0 | Documentação automática da API (Swagger) |

### Observabilidade (O11y Stack)
| Tecnologia | Versão | Propósito |
|------------|--------|-----------|
| **Spring Boot Actuator** | 3.5.7 | Endpoints de health check e métricas |
| **Micrometer** | - | Abstração de métricas |
| **Prometheus** | 2.55.0 | Coleta e armazenamento de métricas |
| **Grafana** | 11.2.2 | Visualização de métricas e dashboards |
| **Tempo** | latest | Backend de tracing distribuído |
| **OpenTelemetry Collector** | latest | Coleta e exportação de traces |

### Testes
| Tecnologia | Versão | Propósito |
|------------|--------|-----------|
| **JUnit 5** | 5.x | Framework de testes unitários |
| **Mockito** | 5.x | Mocks para testes unitários |
| **AssertJ** | 3.x | Assertions fluentes |
| **Testcontainers** | 1.20.3 | Testes de integração com containers |
| **H2 Database** | - | Banco em memória para testes rápidos |
| **JaCoCo** | 0.8.11 | Cobertura de código (mínimo 70%) |

### DevOps e Infraestrutura
| Tecnologia | Versão | Propósito |
|------------|--------|-----------|
| **Docker** | - | Containerização da aplicação |
| **Docker Compose** | 3.9 | Orquestração de serviços locais |
| **Maven** | 3.9.6 | Build e gerenciamento de dependências |
| **pgAdmin** | 8 | Interface gráfica para PostgreSQL |

---

## 🏗️ Arquitetura

O projeto segue os princípios de **Clean Architecture** e **Hexagonal Architecture (Ports & Adapters)**, garantindo:
- ✅ Independência de frameworks
- ✅ Testabilidade
- ✅ Separação de responsabilidades
- ✅ Inversão de dependências

### Estrutura de Camadas

```
┌─────────────────────────────────────────────────────────┐
│                  Presentation Layer                      │
│  (Controllers, DTOs, Exception Handlers)                │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                 Application Layer                        │
│     (Use Cases, Services, Port Interfaces)              │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                   Domain Layer                           │
│         (Entities, Value Objects, Enums)                │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│              Infrastructure Layer                        │
│  (JPA Repositories, Adapters, External Services)        │
└─────────────────────────────────────────────────────────┘
```

### Camadas Detalhadas

#### 1. **Presentation Layer** (`presentation/`)
- **Responsabilidade**: Interface HTTP REST
- **Componentes**:
  - `WalletController`: Endpoints da API
  - DTOs: Requests e Responses (Java Records)
  - `GlobalExceptionHandler`: Tratamento centralizado de erros
- **Validação**: Bean Validation (`@NotNull`, `@NotBlank`)

#### 2. **Application Layer** (`application/`)
- **Responsabilidade**: Casos de uso e lógica de aplicação
- **Componentes**:
  - `port.in`: Interfaces de casos de uso (Use Cases)
  - `service`: Implementação dos casos de uso
  - Exemplos: `DepositService`, `WithdrawService`, `GetBalanceService`
- **Validação**: Idempotência e coordenação entre agregados

#### 3. **Domain Layer** (`domain/`)
- **Responsabilidade**: Regras de negócio puras
- **Componentes**:
  - `model`: Entidades de domínio (`Wallet`, `PixKey`)
  - `enums`: Tipos do domínio (`OperationType`, `PixKeyStatus`)
  - **`validator`**: Validadores de regras de negócio (`PixKeyValidator`, `TransferValidator`)
- **Validação**: Formatos PIX, limites de transferência, tipos de evento

#### 4. **Infrastructure Layer** (`infrastructure/`)
- **Responsabilidade**: Detalhes técnicos e frameworks
- **Componentes**:
  - `persistence.entity`: Entidades JPA
  - `persistence.repository`: Repositórios Spring Data
  - `persistence.adapter`: Adaptadores de porta
  - `config`: Configurações (OpenAPI, etc)

### 🔐 Arquitetura de Validação

O projeto implementa **validação em 3 camadas** para garantir qualidade e consistência dos dados:

```
┌─────────────────────────────────────────────────────────┐
│  Presentation: Bean Validation (@NotNull, @NotBlank)   │
│  → Valida sintaxe e presença de campos                 │
└────────────────────┬────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│  Application: WalletOperationValidator                  │
│  → Valida idempotência e coordenação entre agregados   │
└────────────────────┬────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│  Domain: PixKeyValidator + TransferValidator            │
│  → Valida regras de negócio do domínio PIX             │
└─────────────────────────────────────────────────────────┘
```

#### Validadores de Domínio

**PixKeyValidator** - Valida formatos de chaves PIX:
- ✅ CPF: 11 dígitos
- ✅ Email: formato válido, max 120 caracteres
- ✅ Phone: formato internacional `+[11-14 dígitos]`
- ✅ Random: 32 caracteres hexadecimais (UUID sem hífens)

**TransferValidator** - Valida regras de transferência:
- ✅ Valor > R$ 0,00 e ≤ R$ 100.000,00
- ✅ Carteira origem ≠ Carteira destino
- ✅ Timestamp do webhook não pode ser futuro
- ✅ Tipos de evento: `CONFIRMED`, `REJECTED`, `PENDING`

**ValidationConstants** - Centraliza constantes de validação:
- ✅ Padrões regex (CPF, Email, Phone, Random)
- ✅ Limites de valores (max transfer amount)
- ✅ Mensagens de erro consistentes

📖 **Documentação Completa**: [Arquitetura de Validação](docs/VALIDATION_ARCHITECTURE.md)

---

## ⚡ Funcionalidades

### Gestão de Carteiras
- ✅ Criação de carteira com CPF e nome completo
- ✅ Consulta de saldo atual
- ✅ Consulta de saldo em data específica (histórico)

### Chaves PIX
- ✅ Registro de chaves PIX (CPF, Email, Telefone, Aleatória)
- ✅ Validação de unicidade e formato
- ✅ Suporte a múltiplas chaves por carteira
- ✅ Status de chaves (ACTIVE, REVOKED)

### Operações Financeiras
- ✅ **Depósito**: Com idempotência e controle de concorrência
- ✅ **Saque**: Com validação de saldo e idempotência
- ✅ **Transferências**: Entre carteiras via chave PIX
- ✅ **Ledger**: Registro auditável de todas as transações

### Recursos Técnicos
- ✅ **Idempotência**: Chave única por operação evita duplicações
- ✅ **Concorrência**: Locks otimistas e pessimistas
- ✅ **Transações**: ACID completo com Spring @Transactional
- ✅ **Validações**: Bean Validation em todas as entradas
- ✅ **Auditoria**: Timestamps automáticos em todas as entidades

---

## 📦 Pré-requisitos

- **Java 17+** (JDK)
- **Maven 3.6+**
- **Docker & Docker Compose** (para executar infraestrutura completa)
- **Git**

---

## 🏃 Como Executar

### 1️⃣ Clonar o Repositório

```bash
git clone https://github.com/josinon/pix-service.git
cd pix-service
```

### 2️⃣ Executar Infraestrutura com Docker Compose

```bash
# Inicia todos os serviços (DB, Prometheus, Grafana, Tempo, etc)
docker-compose up -d

# Verificar status dos containers
docker-compose ps
```

**Serviços disponíveis após iniciar:**
- **PostgreSQL**: `localhost:5432` (user: `pix`, pass: `pixpass`)
- **pgAdmin**: http://localhost:15432 (email: `admin@example.com`, pass: `admin`)
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (user: `admin`, pass: `admin`)
- **Application**: http://localhost:8080

### 3️⃣ Build da Aplicação

```bash
# Compilar e empacotar (pula testes)
mvn clean package -DskipTests

# Compilar, testar e gerar relatório de cobertura
mvn clean verify
```

### 4️⃣ Executar a Aplicação Localmente

#### Opção A: Via Maven
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

#### Opção B: Via JAR
```bash
java -jar target/wallet-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

#### Opção C: Via Docker (aplicação containerizada)
```bash
# Build da imagem
docker build -t pix-wallet:latest .

# Executar com docker-compose
docker-compose up app
```

---

## 🧪 Testes

O projeto possui **3 níveis de testes** com cobertura mínima de **70%**:

### Executar Testes Unitários

```bash
# Apenas testes unitários (rápidos)
mvn test
```

**Localização**: `src/test/java/**/service/*Test.java`

**Exemplos**:
- `DepositServiceTest`: Valida regras de depósito
- `WithdrawServiceTest`: Valida regras de saque
- `GetBalanceServiceTest`: Valida consultas de saldo
- `DepositServiceConcurrencyTest`: Valida concorrência e idempotência

### Executar Testes de Integração

```bash
# Apenas testes de integração (com Testcontainers)
mvn verify -DskipUTs
```

**Localização**: `src/test/java/**/integration/*IT.java`

**Exemplos**:
- `DepositIT`: Testa endpoint de depósito end-to-end
- `WalletCreationIT`: Testa criação de carteira
- `DepositConcurrentIT`: Testa concorrência em depósitos
- `WalletConcurrentIT`: Testa race conditions

### Executar Todos os Testes + Cobertura

```bash
# Testes unitários + integração + relatório JaCoCo
mvn clean verify
```

**Relatório de Cobertura**: `target/site/jacoco/index.html`

### Verificar Cobertura

```bash
# Abrir relatório no navegador (macOS)
open target/site/jacoco/index.html
```

### Estrutura de Testes

```
src/test/java/
├── application/service/          # Testes unitários de serviços
│   ├── DepositServiceTest
│   ├── WithdrawServiceTest
│   ├── GetBalanceServiceTest
│   └── DepositServiceConcurrencyTest
├── domain/validator/             # Testes de validadores de domínio
│   ├── PixKeyValidatorTest       # 17 testes (96% cobertura)
│   └── TransferValidatorTest     # 23 testes (96% cobertura)
├── integration/                  # Testes de integração (IT)
│   ├── DepositIT
│   ├── WalletCreationIT
│   └── DepositConcurrentIT
├── presentation/api/             # Testes de controllers
│   ├── WalletControllerTest
│   ├── WalletControllerValidationTest
│   └── PixControllerValidationTest
└── config/                       # Configurações de teste
    ├── IntegrationTest           # Anotação customizada
    └── TestContainersConfig      # Config do Testcontainers
```

**Cobertura de Validadores:**
- `PixKeyValidator`: **96%** (17 testes)
- `TransferValidator`: **96%** (23 testes)
- Total: 40 testes unitários de validação

---

## 📊 Observabilidade

O projeto implementa **full observability stack** com os **3 pilares de observabilidade** e foco especial em **rastreamento de fluxos assíncronos PIX**:

### 🎯 Arquitetura de Observabilidade

```
┌─────────────────────────────────────────────────────────────┐
│                     Application Layer                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │   Logs   │  │ Metrics  │  │  Traces  │  │   MDC    │   │
│  │  (JSON)  │  │(Micrometer)│ │ (OTEL)   │  │(Context) │   │
│  └─────┬────┘  └─────┬────┘  └─────┬────┘  └─────┬────┘   │
└────────┼─────────────┼─────────────┼─────────────┼─────────┘
         │             │             │             │
         ▼             ▼             ▼             ▼
         Loki       Prometheus       Tempo       Grafana
```

### 📌 Sprints de Observabilidade

O projeto seguiu um roadmap estruturado de 8 fases para implementação completa de observabilidade:

- ✅ **Sprint 1: Logs Estruturados + Correlation ID** - [CONCLUÍDO]
- ✅ **Sprint 2: Métricas Customizadas** - [CONCLUÍDO]
- ⏳ **Sprint 3: Distributed Tracing** - [Próximo]
- ⏳ **Sprint 4: Loki Integration** - [Planejado]
- ⏳ **Sprint 5: Dashboards e Alertas** - [Planejado]

📖 **Documentação Completa**: 
- Plano Geral: [`docs/OBSERVABILITY_PLAN.md`](docs/OBSERVABILITY_PLAN.md)
- Sprint 1: [`docs/OBSERVABILITY_SPRINT1.md`](docs/OBSERVABILITY_SPRINT1.md)
- Sprint 2: [`docs/OBSERVABILITY_SPRINT2.md`](docs/OBSERVABILITY_SPRINT2.md)
- **Guia de Métricas**: [`docs/METRICS_GUIDE.md`](docs/METRICS_GUIDE.md) ⭐

### 1. 📝 Logs Estruturados (JSON)

**Implementação:** Logback + Logstash Encoder

#### Características:
- ✅ Logs em formato JSON para facilitar parsing e queries
- ✅ Correlation ID automático em todas as requisições HTTP
- ✅ MDC (Mapped Diagnostic Context) com campos de negócio:
  - `correlationId` - ID único da requisição HTTP
  - `operation` - Nome da operação (ex: PIX_TRANSFER_CREATE)
  - `transferId` - UUID da transferência PIX
  - `endToEndId` - ID E2E da transação PIX (**chave para correlação assíncrona**)
  - `walletId` - UUID da carteira
  - `eventId` - ID do evento de webhook
- ✅ Integração com OpenTelemetry: `trace_id` e `span_id` incluídos automaticamente

#### Exemplo de Log JSON:
```json
{
  "timestamp": "2025-11-04T21:30:00.123Z",
  "level": "INFO",
  "correlationId": "abc-123-def",
  "operation": "PIX_TRANSFER_CREATE",
  "walletId": "wallet-uuid-789",
  "endToEndId": "E123ABC456",
  "transferId": "transfer-uuid-456",
  "trace_id": "trace-xyz-999",
  "span_id": "span-001",
  "message": "PIX transfer created successfully",
  "fromWallet": "wallet-uuid-789",
  "toWallet": "wallet-uuid-999",
  "amount": 100.00,
  "status": "PENDING"
}
```

#### Rastreamento de Fluxo Assíncrono PIX:

O sistema permite rastrear toda a jornada de uma transferência PIX desde a criação até a confirmação via webhook:

**1. Criação da Transferência (Síncrona):**
```json
// POST /pix/transfers
{
  "correlationId": "corr-abc-123",
  "operation": "PIX_TRANSFER_CREATE",
  "endToEndId": "E123ABC456",
  "message": "PIX transfer created successfully",
  "status": "PENDING"
}
```

**2. Processamento do Webhook (Assíncrona):**
```json
// POST /pix/webhook (seconds/minutes later)
{
  "correlationId": "corr-webhook-999",  // Diferente (nova requisição)
  "operation": "PIX_WEBHOOK_PROCESS",
  "endToEndId": "E123ABC456",            // MESMO! (correlação)
  "eventId": "evt-confirm-123",
  "message": "PIX webhook processed successfully",
  "finalStatus": "CONFIRMED"
}
```

**Query para rastrear transferência completa (Loki):**
```logql
{app="pixwallet"} | json | endToEndId="E123ABC456"
```

### 2. 📊 Métricas Customizadas (Micrometer + Prometheus)

**✅ Sprint 2 - COMPLETO**

O sistema implementa **15+ métricas customizadas** para monitorar saúde, performance e negócio:

#### 📈 Métricas Implementadas

##### Transferências PIX (6 métricas):
- `pix.transfers.created` (Counter) - Total de transferências criadas
- `pix.transfers.confirmed` (Counter) - Total confirmadas via webhook
- `pix.transfers.rejected` (Counter) - Total rejeitadas
- `pix.transfers.pending` (Gauge) - **Número atual de pendentes** ⚠️
- `pix.transfer.creation.time` (Timer) - Latência de criação (p50/p95/p99)
- `pix.transfer.end_to_end.time` (Timer) - SLA end-to-end (criação → confirmação)

##### Webhooks (4 métricas):
- `pix.webhooks.received` (Counter) - Total de webhooks recebidos
- `pix.webhooks.duplicated` (Counter) - Detecções de idempotência
- `pix.webhooks.by_type` (Counter) - Por tipo de evento (CONFIRMED/REJECTED)
- `pix.webhook.processing.time` (Timer) - Latência de processamento

##### Carteiras e Chaves PIX (4 métricas):
- `pix.wallets.created` (Counter) - Total de carteiras criadas
- `pix.wallets.active` (Gauge) - Carteiras ativas no momento
- `pix.pixkeys.registered` (Counter) - Total de chaves PIX
- `pix.pixkeys.by_type` (Counter) - Por tipo (CPF/EMAIL/PHONE/RANDOM)

##### Transações (2 métricas):
- `pix.deposits.completed` (Counter) - Depósitos completados
- `pix.withdrawals.completed` (Counter) - Saques completados

#### 🔍 Métricas Críticas

**Indicadores de Saúde:**
```promql
# Transferências pendentes (deve ser baixo)
pix_transfers_pending

# Taxa de sucesso (deve ser > 95%)
pix_transfers_confirmed_total / pix_transfers_created_total
```

**Performance (SLA):**
```promql
# P95 de criação (deve ser < 500ms)
histogram_quantile(0.95, rate(pix_transfer_creation_time_seconds_bucket[5m]))

# P95 end-to-end (deve ser < 5s)
histogram_quantile(0.95, rate(pix_transfer_end_to_end_time_seconds_bucket[5m]))
```

**Detecção de Problemas:**
```promql
# Top 3 erros mais comuns
topk(3, sum by (error_type) (rate(pix_transfer_creation_errors_total[10m])))

# Taxa de webhooks duplicados (deve ser < 10%)
pix_webhooks_duplicated_total / pix_webhooks_received_total
```

#### 📊 Acessar Métricas

**Prometheus Endpoint:**
```bash
# Ver todas as métricas PIX
curl http://localhost:8080/actuator/prometheus | grep pix
```

**Prometheus UI:** http://localhost:9090

**Grafana:** http://localhost:3000 (admin/admin)

📖 **Guia Completo de Métricas**: [`docs/METRICS_GUIDE.md`](docs/METRICS_GUIDE.md) - Inclui:
- Descrição detalhada de cada métrica
- Valor de negócio
- Queries Prometheus prontas
- Alertas recomendados (thresholds)
- Cenários de troubleshooting
- Dashboards sugeridos

### 3. 🔍 Distributed Tracing (Tempo + OpenTelemetry)

**Distributed Tracing** para rastreamento de requisições:
- OpenTelemetry Collector captura traces
- Tempo armazena traces
- Grafana visualiza traces (Explore → Tempo)

**Configuração**:
- Sampling: 100% (todas as requisições são rastreadas)
- Endpoint: `http://otel-collector:4318/v1/traces`
- Service Name: `pixwallet`

**Consultar traces no Grafana**:
1. Acessar http://localhost:3000
2. Menu → Explore
3. Data Source → Tempo
4. Query → Search traces

### 📖 Documentação de Observabilidade

Para mais detalhes sobre a implementação de observabilidade:

- **Plano Completo:** [`docs/OBSERVABILITY_PLAN.md`](docs/OBSERVABILITY_PLAN.md)
- **Sprint 1 (Logs):** [`docs/OBSERVABILITY_SPRINT1.md`](docs/OBSERVABILITY_SPRINT1.md)

#### Componentes Implementados:

**Sprint 1 - Logs Estruturados:**

| Componente | Arquivo | Descrição |
|------------|---------|-----------|
| **CorrelationIdFilter** | `infrastructure/config/CorrelationIdFilter.java` | Gera/propaga Correlation IDs via header `X-Correlation-ID` |
| **ObservabilityContext** | `infrastructure/observability/ObservabilityContext.java` | Utilitário MDC para contexto de negócio |
| **Logback Config** | `resources/logback-spring.xml` | Configuração de logs estruturados JSON |

**Sprint 2 - Métricas Customizadas:**

| Componente | Arquivo | Descrição |
|------------|---------|-----------|
| **MetricsService** | `infrastructure/observability/MetricsService.java` | Serviço centralizado com 15+ métricas customizadas |
| **Instrumentação** | Todos os services (Transfer, Webhook, Wallet, PixKey, Deposit, Withdraw) | Métricas integradas em todos os fluxos críticos |

#### Queries Úteis:

**Buscar logs de uma transferência:**
```logql
{app="pixwallet"} | json | endToEndId="E123ABC456"
```

**Buscar erros em webhooks:**
```logql
{app="pixwallet"} | json | operation="PIX_WEBHOOK_PROCESS" | level="ERROR"
```

**Buscar requisições duplicadas:**
```logql
{app="pixwallet"} | json | reason="duplicate_request"
```

---

## 📚 API Documentation

A documentação da API é gerada automaticamente com **SpringDoc OpenAPI 3**.

### Swagger UI

**URL**: http://localhost:8080/swagger-ui.html

### OpenAPI JSON

**URL**: http://localhost:8080/v3/api-docs

### Principais Endpoints

#### Carteiras
```http
POST   /api/v1/wallet              # Criar carteira
GET    /api/v1/wallet/{id}/balance # Obter saldo
POST   /api/v1/wallet/{id}/deposit # Realizar depósito
POST   /api/v1/wallet/{id}/withdraw # Realizar saque
```

#### Chaves PIX
```http
POST   /api/v1/wallet/{walletId}/pix-key # Criar chave PIX
```

#### Health Check
```http
GET    /actuator/health           # Status da aplicação
GET    /actuator/prometheus       # Métricas para Prometheus
```

---

## 📂 Estrutura do Projeto

```
pix-service/
├── src/
│   ├── main/
│   │   ├── java/org/pix/wallet/
│   │   │   ├── WalletApplication.java           # Main class
│   │   │   ├── application/                     # Camada de aplicação
│   │   │   │   ├── port/
│   │   │   │   │   └── in/                      # Use Cases (interfaces)
│   │   │   │   └── service/                     # Implementação dos Use Cases
│   │   │   ├── domain/                          # Camada de domínio
│   │   │   │   ├── model/                       # Entidades e Value Objects
│   │   │   │   └── validator/                   # 🆕 Validadores de regras de negócio
│   │   │   │       ├── PixKeyValidator.java
│   │   │   │       ├── TransferValidator.java
│   │   │   │       └── ValidationConstants.java
│   │   │   ├── infrastructure/                  # Camada de infraestrutura
│   │   │   │   ├── config/                      # Configurações
│   │   │   │   └── persistence/                 # JPA, Repositories, Adapters
│   │   │   └── presentation/                    # Camada de apresentação
│   │   │       ├── api/                         # Controllers REST
│   │   │       └── dto/                         # Request/Response DTOs (Records)
│   │   └── resources/
│   │       ├── application.yml                  # Config principal
│   │       ├── application-local.yml            # Config ambiente local
│   │       ├── application-test.yml             # Config ambiente de teste
│   │       └── db/migration/                    # Scripts Flyway
│   │           └── V1__init_schema.sql
│   └── test/
│       └── java/org/pix/wallet/
│           ├── application/service/             # Testes unitários
│           ├── domain/validator/                # 🆕 Testes de validadores (40 testes)
│           ├── integration/                     # Testes de integração
│           ├── presentation/api/                # Testes de controllers
│           └── config/                          # Configs de teste
├── docs/                                        # 🆕 Documentação
│   └── VALIDATION_ARCHITECTURE.md              # Arquitetura de validação
├── docker/                                      # Configurações Docker
│   ├── grafana/provisioning/                   # Datasources e dashboards
│   ├── otel/collector-config.yml               # OpenTelemetry config
│   ├── prometheus/prometheus.yml               # Prometheus config
│   └── tempo/tempo.yaml                        # Tempo config
├── docker-compose.yml                          # Orquestração de serviços
├── Dockerfile                                  # Build da aplicação
├── pom.xml                                     # Dependências Maven
└── README.md                                   # Este arquivo
```

---

## 🔧 Configurações de Ambiente

### Variáveis de Ambiente (docker-compose)

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `POSTGRES_DB` | `pixwallet` | Nome do banco de dados |
| `POSTGRES_USER` | `pix` | Usuário do PostgreSQL |
| `POSTGRES_PASSWORD` | `pixpass` | Senha do PostgreSQL |
| `DB_PORT` | `5432` | Porta do PostgreSQL |
| `APP_PORT` | `8080` | Porta da aplicação |
| `GRAFANA_PORT` | `3000` | Porta do Grafana |
| `PROMETHEUS_PORT` | `9090` | Porta do Prometheus |

### Profiles Spring

- **`local`**: Desenvolvimento local (db externo)
- **`test`**: Testes (H2 ou Testcontainers)

---

## 🛠️ Build e Deploy

### Build para Produção

```bash
# Compilar e gerar JAR
mvn clean package -DskipTests

# Gerar imagem Docker
docker build -t pix-wallet:1.0.0 .

# Executar container
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/pixwallet \
  -e SPRING_DATASOURCE_USERNAME=pix \
  -e SPRING_DATASOURCE_PASSWORD=pixpass \
  pix-wallet:1.0.0
```

### Pipeline CI/CD (Sugestão)

1. **Build**: `mvn clean verify` (testes + cobertura)
2. **Quality Gate**: JaCoCo check (mínimo 70%)
3. **Docker Build**: Criar imagem
4. **Push**: Registry (Docker Hub, ECR, etc)
5. **Deploy**: Kubernetes, ECS, etc

---

## 📈 Métricas de Qualidade

- ✅ **Cobertura de Código**: **60%** (meta: 60%, JaCoCo)
- ✅ **Testes Unitários**: 154 testes (incluindo 25 testes do MetricsService)
- ✅ **Testes de Integração**: 17 cenários
- ✅ **Testes de Validação**: 40 testes (96% cobertura)
- ✅ **Testes de Concorrência**: Validação de race conditions
- ✅ **Clean Architecture**: Separação clara de camadas
- ✅ **SOLID**: Princípios aplicados
- ✅ **DRY**: Reutilização de código (ValidationConstants)
- ✅ **Validação em Camadas**: Presentation → Application → Domain
- ✅ **Observabilidade**: Logs estruturados + Métricas customizadas (Sprint 2)

---

## 📝 Licença

Este projeto está sob a licença MIT.

---

## 👨‍💻 Autor

**Josino Neto**
- GitHub: [@josinon](https://github.com/josinon)
---

**Desenvolvido com ☕ e Java**
