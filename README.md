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

O projeto implementa **full observability stack** com os **3 pilares**:

### 1. Métricas (Prometheus + Grafana)

**Prometheus** coleta métricas da aplicação via `/actuator/prometheus`:
- Taxa de requisições (throughput)
- Latência (p50, p95, p99)
- Uso de memória/CPU
- Métricas de JVM
- Métricas de banco de dados

**Acessar Prometheus**: http://localhost:9090

**Grafana** visualiza as métricas em dashboards:
- Dashboard de aplicação
- Dashboard de banco de dados PostgreSQL

**Acessar Grafana**: http://localhost:3000 (admin/admin)

### 2. Logs

Logs estruturados via **SLF4J + Logback**:
- Níveis: INFO, WARN, ERROR
- Contexto de transação
- Correlação de requests

**Ver logs da aplicação**:
```bash
docker-compose logs -f app
```

### 3. Tracing (Tempo + OpenTelemetry)

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

- ✅ **Cobertura de Código**: **72%** (meta: 70%, JaCoCo)
- ✅ **Testes Unitários**: 129 testes
- ✅ **Testes de Integração**: 17 cenários
- ✅ **Testes de Validação**: 40 testes (96% cobertura)
- ✅ **Testes de Concorrência**: Validação de race conditions
- ✅ **Clean Architecture**: Separação clara de camadas
- ✅ **SOLID**: Princípios aplicados
- ✅ **DRY**: Reutilização de código (ValidationConstants)
- ✅ **Validação em Camadas**: Presentation → Application → Domain

---

## 📝 Licença

Este projeto está sob a licença MIT.

---

## 👨‍💻 Autor

**Josino Neto**
- GitHub: [@josinon](https://github.com/josinon)
---

**Desenvolvido com ☕ e Java**
