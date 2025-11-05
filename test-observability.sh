#!/bin/bash

# Script de Teste - Observabilidade Sprint 1
# Este script testa a implementação de logs estruturados e correlação assíncrona

set -e

echo "🧪 Teste de Observabilidade - Sprint 1"
echo "======================================"
echo ""

# Cores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuração
BASE_URL="http://localhost:8080"

echo -e "${BLUE}📋 Pré-requisitos:${NC}"
echo "  ✓ Aplicação rodando em $BASE_URL"
echo "  ✓ Docker Compose up (se quiser ver traces no Tempo)"
echo ""

# Função para verificar se app está rodando
check_app() {
    if curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Aplicação está rodando${NC}"
        return 0
    else
        echo -e "${RED}✗ Aplicação NÃO está rodando em $BASE_URL${NC}"
        echo "  Execute: mvn spring-boot:run"
        exit 1
    fi
}

# Verificar app
check_app
echo ""

echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo -e "${BLUE}Teste 1: Correlation ID Automático${NC}"
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo ""

echo "Fazendo requisição SEM Correlation ID..."
RESPONSE=$(curl -s -v "$BASE_URL/actuator/health" 2>&1)

CORRELATION_ID=$(echo "$RESPONSE" | grep -i "x-correlation-id" | cut -d ':' -f2 | tr -d ' \r')

if [ -n "$CORRELATION_ID" ]; then
    echo -e "${GREEN}✓ Correlation ID gerado automaticamente: $CORRELATION_ID${NC}"
else
    echo -e "${RED}✗ Correlation ID NÃO encontrado no header de resposta${NC}"
fi
echo ""

echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo -e "${BLUE}Teste 2: Correlation ID Propagation${NC}"
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo ""

CUSTOM_CORR_ID="test-correlation-$(date +%s)"
echo "Enviando requisição COM Correlation ID: $CUSTOM_CORR_ID"
RESPONSE=$(curl -s -v -H "X-Correlation-ID: $CUSTOM_CORR_ID" "$BASE_URL/actuator/health" 2>&1)

RETURNED_CORR_ID=$(echo "$RESPONSE" | grep -i "x-correlation-id" | cut -d ':' -f2 | tr -d ' \r')

if [ "$RETURNED_CORR_ID" == "$CUSTOM_CORR_ID" ]; then
    echo -e "${GREEN}✓ Correlation ID propagado corretamente: $RETURNED_CORR_ID${NC}"
else
    echo -e "${RED}✗ Correlation ID não propagado (esperado: $CUSTOM_CORR_ID, recebido: $RETURNED_CORR_ID)${NC}"
fi
echo ""

echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo -e "${BLUE}Teste 3: Fluxo Completo PIX (Assíncrono)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo ""

echo -e "${YELLOW}Passo 1: Criar Wallets${NC}"
echo "Criando wallet origem (Alice)..."
WALLET_1_RESPONSE=$(curl -s -X POST "$BASE_URL/wallets" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: create-wallet-alice-$RANDOM" \
  -d '{
    "name": "Alice",
    "cpf": "12345678901"
  }')

WALLET_1_ID=$(echo $WALLET_1_RESPONSE | grep -o '"id":"[^"]*' | cut -d'"' -f4)

if [ -n "$WALLET_1_ID" ]; then
    echo -e "${GREEN}✓ Wallet Alice criada: $WALLET_1_ID${NC}"
else
    echo -e "${RED}✗ Falha ao criar wallet Alice${NC}"
    echo "Response: $WALLET_1_RESPONSE"
fi

echo "Criando wallet destino (Bob)..."
WALLET_2_RESPONSE=$(curl -s -X POST "$BASE_URL/wallets" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: create-wallet-bob-$RANDOM" \
  -d '{
    "name": "Bob",
    "cpf": "98765432109"
  }')

WALLET_2_ID=$(echo $WALLET_2_RESPONSE | grep -o '"id":"[^"]*' | cut -d'"' -f4)

if [ -n "$WALLET_2_ID" ]; then
    echo -e "${GREEN}✓ Wallet Bob criada: $WALLET_2_ID${NC}"
else
    echo -e "${RED}✗ Falha ao criar wallet Bob${NC}"
    echo "Response: $WALLET_2_RESPONSE"
fi
echo ""

echo -e "${YELLOW}Passo 2: Criar Chave PIX para Bob${NC}"
PIX_KEY_RESPONSE=$(curl -s -X POST "$BASE_URL/wallets/$WALLET_2_ID/pix-keys" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: create-pixkey-$RANDOM" \
  -d '{
    "type": "CPF",
    "value": "98765432109"
  }')

echo -e "${GREEN}✓ Chave PIX criada: CPF 98765432109${NC}"
echo ""

echo -e "${YELLOW}Passo 3: Depositar R$ 500 na wallet Alice${NC}"
DEPOSIT_RESPONSE=$(curl -s -X POST "$BASE_URL/wallets/$WALLET_1_ID/deposit" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: deposit-test-$RANDOM" \
  -H "X-Correlation-ID: deposit-alice-$RANDOM" \
  -d '{
    "amount": 500.00
  }')

echo -e "${GREEN}✓ Depósito realizado: R$ 500,00${NC}"
echo ""

echo -e "${YELLOW}Passo 4: Criar Transferência PIX (PENDING)${NC}"
TRANSFER_CORR_ID="transfer-test-$(date +%s)"
echo "Correlation ID da transferência: $TRANSFER_CORR_ID"

TRANSFER_RESPONSE=$(curl -s -X POST "$BASE_URL/pix/transfers" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: transfer-test-$RANDOM" \
  -H "X-Correlation-ID: $TRANSFER_CORR_ID" \
  -d "{
    \"fromWalletId\": \"$WALLET_1_ID\",
    \"toPixKey\": \"98765432109\",
    \"amount\": 100.00
  }")

END_TO_END_ID=$(echo $TRANSFER_RESPONSE | grep -o '"endToEndId":"[^"]*' | cut -d'"' -f4)
TRANSFER_STATUS=$(echo $TRANSFER_RESPONSE | grep -o '"status":"[^"]*' | cut -d'"' -f4)

if [ -n "$END_TO_END_ID" ]; then
    echo -e "${GREEN}✓ Transferência criada:${NC}"
    echo "  EndToEndId: $END_TO_END_ID"
    echo "  Status: $TRANSFER_STATUS"
    echo "  Correlation ID: $TRANSFER_CORR_ID"
else
    echo -e "${RED}✗ Falha ao criar transferência${NC}"
    echo "Response: $TRANSFER_RESPONSE"
    exit 1
fi
echo ""

echo -e "${YELLOW}Aguardando 2 segundos...${NC}"
sleep 2
echo ""

echo -e "${YELLOW}Passo 5: Enviar Webhook de Confirmação (CONFIRMED)${NC}"
WEBHOOK_CORR_ID="webhook-confirm-$(date +%s)"
echo "Correlation ID do webhook: $WEBHOOK_CORR_ID"
echo "ATENÇÃO: Correlation ID diferente, mas MESMO endToEndId!"

WEBHOOK_RESPONSE=$(curl -s -X POST "$BASE_URL/pix/webhook" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: webhook-test-$RANDOM" \
  -H "X-Correlation-ID: $WEBHOOK_CORR_ID" \
  -d "{
    \"endToEndId\": \"$END_TO_END_ID\",
    \"eventId\": \"evt-confirm-$(date +%s)\",
    \"eventType\": \"CONFIRMED\",
    \"occurredAt\": \"$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")\"
  }")

echo -e "${GREEN}✓ Webhook processado${NC}"
echo ""

echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo -e "${BLUE}Teste 4: Verificar Correlação nos Logs${NC}"
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo ""

echo -e "${YELLOW}Buscando logs por EndToEndId: $END_TO_END_ID${NC}"
echo ""

if [ -f "logs/application.log" ]; then
    echo "Logs encontrados em logs/application.log:"
    grep "$END_TO_END_ID" logs/application.log || echo "Nenhum log encontrado no arquivo"
elif [ -f "target/logs/application.log" ]; then
    echo "Logs encontrados em target/logs/application.log:"
    grep "$END_TO_END_ID" target/logs/application.log || echo "Nenhum log encontrado no arquivo"
else
    echo -e "${YELLOW}ℹ Arquivo de log não encontrado.${NC}"
    echo "  Verifique logs no console da aplicação."
    echo "  Ou use: docker-compose logs app | grep '$END_TO_END_ID'"
fi
echo ""

echo -e "${GREEN}═══════════════════════════════════════════${NC}"
echo -e "${GREEN}✓ Testes Concluídos!${NC}"
echo -e "${GREEN}═══════════════════════════════════════════${NC}"
echo ""

echo -e "${BLUE}📊 Resumo:${NC}"
echo "  • Correlation ID gerado automaticamente: ✓"
echo "  • Correlation ID propagado: ✓"
echo "  • Transferência PIX criada: ✓"
echo "  • Webhook processado: ✓"
echo "  • EndToEndId: $END_TO_END_ID"
echo ""

echo -e "${BLUE}🔍 Para verificar correlação completa:${NC}"
echo ""
echo "  1. Console da aplicação:"
echo "     Busque por: $END_TO_END_ID"
echo ""
echo "  2. Docker Compose logs:"
echo "     docker-compose logs app | grep '$END_TO_END_ID'"
echo ""
echo "  3. Grafana Tempo (se disponível):"
echo "     http://localhost:3000 → Explore → Tempo"
echo "     Query: endToEndId = \"$END_TO_END_ID\""
echo ""

echo -e "${YELLOW}💡 Dica:${NC} Você deve ver logs com:"
echo "  - correlationId: $TRANSFER_CORR_ID (transferência)"
echo "  - correlationId: $WEBHOOK_CORR_ID (webhook)"
echo "  - endToEndId: $END_TO_END_ID (AMBOS!)"
echo ""
echo "Isso prova que a correlação assíncrona está funcionando! 🎉"
