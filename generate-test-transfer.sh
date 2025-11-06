#!/bin/bash

# Script para gerar uma transferência PIX completa e obter o endToEndId
set -e

API_BASE="http://localhost:8080"

echo ""
echo "🚀 Iniciando geração de transferência PIX de teste..."
echo ""

# PASSO 1: Criar Wallet de Origem
echo "📝 PASSO 1: Criando Wallet de Origem..."
FROM_WALLET_RESPONSE=$(curl -s -X POST "${API_BASE}/wallets" \
  -H 'Content-Type: application/json' \
  -d '{"initialAmount": 0.00}')

FROM_WALLET_ID=$(echo $FROM_WALLET_RESPONSE | grep -o '"id":"[^"]*' | cut -d'"' -f4)
echo "   ✅ Wallet Origem criada: ${FROM_WALLET_ID}"

# Fazer depósito na wallet de origem
curl -s -X POST "${API_BASE}/wallets/${FROM_WALLET_ID}/deposit" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: deposit-$(date +%s%N)" \
  -d '{"amount": 1000.00}' > /dev/null
echo "   💰 Depósito realizado: R$ 1000.00"
echo ""

# PASSO 2: Criar Wallet de Destino
echo "📝 PASSO 2: Criando Wallet de Destino..."
TO_WALLET_RESPONSE=$(curl -s -X POST "${API_BASE}/wallets" \
  -H 'Content-Type: application/json' \
  -d '{"initialAmount": 0.00}')

TO_WALLET_ID=$(echo $TO_WALLET_RESPONSE | grep -o '"id":"[^"]*' | cut -d'"' -f4)
echo "   ✅ Wallet Destino criada: ${TO_WALLET_ID}"
echo ""

# PASSO 3: Criar Chave PIX na Wallet de Destino
echo "📝 PASSO 3: Criando Chave PIX Random na Wallet Destino..."
PIX_KEY_RESPONSE=$(curl -s -X POST "${API_BASE}/wallets/${TO_WALLET_ID}/pix-keys" \
  -H 'Content-Type: application/json' \
  -d '{"type": "RANDOM", "value": ""}')

PIX_KEY=$(echo $PIX_KEY_RESPONSE | grep -o '"value":"[^"]*' | cut -d'"' -f4)
echo "   ✅ Chave PIX criada: ${PIX_KEY}"
echo ""

# PASSO 4: Criar Transferência PIX
echo "📝 PASSO 4: Criando Transferência PIX..."
TRANSFER_RESPONSE=$(curl -s -X POST "${API_BASE}/pix/transfers" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: transfer-$(date +%s%N)" \
  -d "{\"fromWalletId\": \"${FROM_WALLET_ID}\", \"toPixKey\": \"${PIX_KEY}\", \"amount\": 100.00}")

END_TO_END_ID=$(echo "$TRANSFER_RESPONSE" | grep -o '"endToEndId":"[^"]*' | cut -d'"' -f4)
echo "   ✅ Transferência criada!"
echo "   🎯 EndToEndId: ${END_TO_END_ID}"
echo ""

# PASSO 5: Confirmar via Webhook
echo "📝 PASSO 5: Confirmando transferência via Webhook..."
WEBHOOK_RESPONSE=$(curl -s -X POST "${API_BASE}/pix/webhook" \
  -H 'Content-Type: application/json' \
  -d "{\"endToEndId\": \"${END_TO_END_ID}\", \"eventId\": \"evt-$(date +%s%N)\", \"eventType\": \"CONFIRMED\", \"occurredAt\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}")

echo "   ✅ Webhook processado!"
echo ""

# Resumo
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║            🎉 TRANSFERÊNCIA PIX CRIADA COM SUCESSO!            ║"
echo "╠════════════════════════════════════════════════════════════════╣"
echo "║                                                                ║"
echo "║  📊 Use este valor no Dashboard Grafana:                      ║"
echo "║                                                                ║"
printf "║  🎯 EndToEndId: %-44s║\n" "${END_TO_END_ID}"
echo "║                                                                ║"
echo "╠════════════════════════════════════════════════════════════════╣"
echo "║  🔍 Acesse: http://localhost:3000/d/pix-correlation           ║"
echo "║  Cole o EndToEndId no campo no topo do dashboard!             ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

echo "⏳ Aguardando 5 segundos para métricas serem processadas..."
sleep 5

echo ""
echo "✅ Pronto! Verifique se a métrica End-to-End aparece no dashboard!"
echo ""
