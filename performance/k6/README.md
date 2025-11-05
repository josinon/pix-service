# k6 Load Test Suite

## O que é k6?

[k6](https://k6.io/) é uma ferramenta moderna de testes de carga e performance, desenvolvida pela Grafana Labs. Ele permite escrever testes em JavaScript, executá-los com alta performance (escrito em Go) e analisar os resultados através de métricas detalhadas. O k6 é ideal para:

- **Testes de carga**: Simular múltiplos usuários acessando sua aplicação
- **Testes de stress**: Descobrir os limites de capacidade do sistema
- **Testes de soak**: Verificar estabilidade em execuções longas
- **Integração CI/CD**: Automatizar validações de performance

## Como usamos k6 neste projeto

Este projeto utiliza k6 para validar a performance e resiliência do serviço de carteira PIX em diferentes cenários:

### Scripts Disponíveis

- **`stress.js`**: Teste de stress com rampa de carga crescente (ramping arrival rate) para descobrir o ponto de saturação do sistema. Simula criação de transferências PIX + confirmação via webhook.

- **`baseline.js`**: Teste baseline com carga constante (30 VUs durante 5 minutos) para comparação diária de performance e detecção de regressões.

- **`lib/helpers.js`**: Funções reutilizáveis para operações HTTP (criação de carteira, chave PIX, depósito, transferência e confirmação via webhook).

### Variáveis de Ambiente

Configure os testes através de variáveis de ambiente:

- `BASE_URL` (default: `http://localhost:8080`) - URL base da API
- `TRANSFER_AMOUNT` (default: `5.00`) - Valor de cada transferência PIX
- `SCENARIO_NAME` (default: `unspecified`) - Nome do cenário para header `X-Scenario`
- `RUN_ID` (default: auto-gerado) - ID único para correlação via header `X-Run-Id`
- `INITIAL_BALANCE` (default: `200000.00`) - Saldo inicial depositado na carteira origem para evitar erros de saldo insuficiente durante testes longos

### Thresholds (Critérios de Sucesso)

Os testes definem thresholds que determinam se a execução passou ou falhou:

- `http_req_failed < 0.005` - Menos de 0.5% de requisições falhadas
- `http_req_duration{endpoint:transfer_create} p(95) < 300ms` - 95% das criações de transferência em < 300ms
- `time_to_confirm p(95) < 2000ms` - 95% do fluxo completo (criação + confirmação) em < 2s

## Como executar os testes

### Pré-requisitos

1. **Subir a stack da aplicação:**
   ```bash
   docker compose up -d db app
   ```

2. **Aguardar aplicação ficar healthy:**
   ```bash
   docker compose ps
   # Aguarde até app mostrar "healthy"
   ```

### Executando com Docker (Recomendado)

Execute os testes usando a imagem oficial do k6 sem precisar instalar localmente:

**Teste de Stress:**
```bash
docker run --rm -i --network host \
  -e BASE_URL=http://localhost:8080 \
  -e SCENARIO_NAME=stress_test \
  -v $(pwd)/performance/k6:/scripts \
  grafana/k6:latest run /scripts/stress.js
```

**Teste Baseline:**
```bash
docker run --rm -i --network host \
  -e BASE_URL=http://localhost:8080 \
  -e SCENARIO_NAME=baseline_daily \
  -v $(pwd)/performance/k6:/scripts \
  grafana/k6:latest run /scripts/baseline.js
```

**Exportar resultados para JSON:**
```bash
docker run --rm -i --network host \
  -e BASE_URL=http://localhost:8080 \
  -v $(pwd)/performance/k6:/scripts \
  -v $(pwd)/performance/results:/results \
  grafana/k6:latest run --out json=/results/stress-$(date +%Y%m%d-%H%M%S).json /scripts/stress.js
```

### Executando com k6 instalado localmente

Se preferir instalar k6 na máquina:

**macOS:**
```bash
brew install k6
```

**Linux (Fedora/RHEL/CentOS):**
```bash
sudo dnf install https://dl.k6.io/rpm/repo.rpm
sudo dnf install k6
```

**Linux (Debian/Ubuntu):**
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

**Executar testes:**
```bash
# Stress test
BASE_URL=http://localhost:8080 k6 run performance/k6/stress.js

# Baseline test
BASE_URL=http://localhost:8080 k6 run performance/k6/baseline.js
```

Para mais métodos de instalação: https://k6.io/docs/get-started/installation/

## Overview da implementação dos testes

### Fluxo de teste (ambos os scripts)

Cada Virtual User (VU) executa o seguinte fluxo completo:

1. **Criar duas carteiras** (origem e destino) via `POST /wallets`
2. **Criar chaves PIX aleatórias** para ambas as carteiras via `POST /pix/keys/random`
3. **Depositar saldo inicial** na carteira origem via `POST /wallets/{id}/deposit`
4. **Criar transferência PIX** da carteira origem para destino via `POST /pix/transfers`
5. **Simular confirmação** enviando webhook via `POST /pix/webhook` com `eventType=CONFIRMED`

### Diferenças entre os scripts

**stress.js (Teste de Stress):**
- Usa cenário `ramping-arrival-rate` (taxa de chegada crescente)
- Começa com 10 req/s e escala até 100 req/s em múltiplos estágios
- Objetivo: Encontrar o ponto de saturação do sistema
- Duração: ~15 minutos (varia com os stages definidos)

**baseline.js (Teste Baseline):**
- Usa cenário fechado com VUs constantes (30 VUs)
- Carga previsível e constante por 5 minutos
- Objetivo: Comparação diária e detecção de regressões de performance
- Duração: 5 minutos fixos

### Rastreabilidade e correlação

Cada requisição inclui headers customizados para facilitar debugging e correlação com logs/traces:

- `X-Scenario`: Nome do cenário (configurável via `SCENARIO_NAME`)
- `X-Run-Id`: Identificador único da execução do teste (fixo por run)
- `X-Trace-Id`: Identificador único por requisição (muda a cada request)

Esses headers permitem filtrar logs no Loki, por exemplo: `{runId="abc123"}`

### Estratégias de teste

- **Saldo inicial alto** (`INITIAL_BALANCE=200000.00`): Evita falsos positivos de saldo insuficiente (409) durante testes longos, focando thresholds em erros reais (5xx)
- **Confirmação simulada**: O webhook é enviado imediatamente após criação da transferência para medir o fluxo completo de forma determinística
- **Métricas customizadas**: `time_to_confirm` mede o tempo total do fluxo (criação + confirmação)

## Como interpretar os resultados

### Resumo no terminal

Ao final da execução, k6 exibe um resumo com as principais métricas:

```
     ✓ transfer created successfully
     ✓ transfer confirmed
     ✓ status 201

     checks.........................: 100.00% ✓ 15000      ✗ 0
     data_received..................: 4.2 MB  14 kB/s
     data_sent......................: 8.1 MB  27 kB/s
     http_req_blocked...............: avg=1.2ms    min=1µs    med=5µs    max=89ms   p(90)=7µs    p(95)=15µs
     http_req_connecting............: avg=589µs    min=0s     med=0s     max=45ms   p(90)=0s     p(95)=0s
   ✓ http_req_duration..............: avg=145ms    min=12ms   med=98ms   max=2.1s   p(90)=287ms  p(95)=398ms
     http_req_failed................: 0.00%   ✓ 0          ✗ 75000
     http_req_receiving.............: avg=89µs     min=15µs   med=67µs   max=12ms   p(90)=145µs  p(95)=198µs
     http_req_sending...............: avg=45µs     min=8µs    med=32µs   max=8ms    p(90)=78µs   p(95)=102µs
     http_req_tls_handshaking.......: avg=0s       min=0s     med=0s     max=0s     p(90)=0s     p(95)=0s
     http_req_waiting...............: avg=144ms    min=11ms   med=97ms   max=2.1s   p(90)=286ms  p(95)=397ms
     http_reqs......................: 75000   250/s
     iteration_duration.............: avg=5.2s     min=3.8s   med=4.9s   max=15s    p(90)=6.1s   p(95)=7.2s
     iterations.....................: 5000    16.67/s
   ✓ time_to_confirm................: avg=1.2s     min=890ms  med=1.1s   max=3.5s   p(90)=1.6s   p(95)=1.8s
     vus............................: 30      min=0        max=30
     vus_max........................: 30      min=30       max=30
```

### Métricas importantes

**Checks (✓):**
- Todas as asserções devem passar (100%)
- Se houver falhas, indica problemas na lógica ou disponibilidade

**http_req_failed:**
- Deve ser < 0.5% (threshold definido)
- Se ultrapassar, investigue erros HTTP 5xx ou timeouts

**http_req_duration:**
- Analise p(95) e p(99) - latência do usuário
- Compare com thresholds definidos (ex: `p(95) < 300ms` para criação de transferências)

**time_to_confirm (métrica customizada):**
- Tempo total do fluxo completo (criação + confirmação)
- p(95) deve ser < 2000ms (threshold definido)

**http_reqs:**
- Taxa de requisições por segundo (throughput)
- Em stress tests, observe quando a taxa para de crescer (ponto de saturação)

**iteration_duration:**
- Tempo para completar uma iteração inteira do cenário
- Aumento indica degradação de performance

### Analisando thresholds

Os thresholds aparecem com ✓ (passou) ou ✗ (falhou):

```
✓ http_req_failed < 0.005          # PASSOU - Menos de 0.5% de falhas
✗ http_req_duration{endpoint:transfer_create} p(95) < 300ms  # FALHOU - p95 foi 398ms
```

Se um threshold falhar, o k6 retorna exit code 1 (útil para CI/CD).

### Observabilidade complementar

Para análise mais profunda, utilize as ferramentas da stack:

**Grafana (http://localhost:3000):**
- Dashboard de Load Tests para visualizar métricas em tempo real
- Correlacione com métricas da aplicação (JVM, database, etc.)

**Loki (via Grafana):**
- Filtre logs por `X-Run-Id` para isolar execução específica
- Query exemplo: `{container="pixwallet-app"} |= "runId=abc123"`

**Tempo (via Grafana):**
- Rastreie traces distribuídos usando `X-Trace-Id`
- Identifique gargalos em chamadas de serviço

**Prometheus:**
- Métricas de infraestrutura (CPU, memória, threads)
- Correlacione degradação de performance com uso de recursos

### Cenários de extensão

**Teste Soak (estabilidade longa duração):**
```bash
# Copie baseline.js e ajuste duration para 2h
cp performance/k6/baseline.js performance/k6/soak.js
# Edite soak.js: duration: '2h'
```

**Teste Spike (pico repentino):**
```javascript
// Novo script: spike.js
export let options = {
  stages: [
    { duration: '1m', target: 10 },   // Warmup
    { duration: '10s', target: 200 }, // Spike repentino!
    { duration: '3m', target: 200 },  // Sustenta
    { duration: '1m', target: 0 },    // Cooldown
  ],
};
```

**Exportar para JSON e processar:**
```bash
k6 run --out json=results.json stress.js
# Processe results.json com jq, pandas, etc.
```
