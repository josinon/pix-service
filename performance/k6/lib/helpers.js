import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

// Simple random id (not RFC UUID but sufficient for idempotency/event ids here)
export function randomId() {
  return 'id-' + Math.random().toString(16).substring(2) + Date.now().toString(16);
}

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const TRANSFER_AMOUNT = __ENV.TRANSFER_AMOUNT || '5.00'; // string to keep JSON simple
// Large initial balance to avoid insufficient funds during sustained load.
// Can be overridden via INITIAL_BALANCE env variable.
export const INITIAL_BALANCE = __ENV.INITIAL_BALANCE || '200000.00';
export const SCENARIO_NAME = __ENV.SCENARIO_NAME || __ENV.K6_SCENARIO || 'unspecified';
export const RUN_ID = __ENV.RUN_ID || `run-${Date.now().toString(16)}`;
export const FAIL_SAMPLE_PCT = parseFloat(__ENV.FAIL_SAMPLE_PCT || '5'); // percent of failed webhooks to log (best-effort)
// Safety skew (ms) to avoid future timestamp validation errors due to client/server clock drift
export const WEBHOOK_TIMESTAMP_SKEW_MS = parseInt(__ENV.WEBHOOK_TIMESTAMP_SKEW_MS || '750');
const FAIL_SAMPLE_CAP = 500; // hard cap to avoid log flood
let failSampleEmitted = 0;

function buildHeaders({ idempotencyKey, traceId = randomId(), extra = {} }) {
  const base = {
    'Content-Type': 'application/json',
    'X-Scenario': SCENARIO_NAME,
    'X-Run-Id': RUN_ID,
    'X-Trace-Id': traceId,
  };
  if (idempotencyKey) base['Idempotency-Key'] = idempotencyKey;
  return Object.assign(base, extra);
}

export function createWallet() {
  const res = http.post(`${BASE_URL}/wallets`, null, { headers: buildHeaders({}), tags: { endpoint: 'wallet_create' } });
  check(res, { 'create wallet status 201': r => r.status === 201 });
  const body = tryParse(res);
  return body.id;
}

export function createRandomPixKey(walletId) {
  const payload = JSON.stringify({ type: 'RANDOM', value: '' });
  const res = http.post(`${BASE_URL}/wallets/${walletId}/pix-keys`, payload, { headers: buildHeaders({}), tags: { endpoint: 'pix_key_create' } });
  check(res, { 'pix key created 201': r => r.status === 201 });
  const body = tryParse(res);
  return body.value; // use value as PIX key destination
}

export function depositWallet(walletId, amount = TRANSFER_AMOUNT) {
  const idempotencyKey = randomId();
  const payload = JSON.stringify({ amount });
  const res = http.post(`${BASE_URL}/wallets/${walletId}/deposit`, payload, { headers: buildHeaders({ idempotencyKey }), tags: { endpoint: 'wallet_deposit' } });
  check(res, { 'deposit ok 200': r => r.status === 200 });
  return idempotencyKey;
}

export function createTransfer(fromWalletId, toPixKey, amount = TRANSFER_AMOUNT) {
  const idempotencyKey = randomId();
  const payload = JSON.stringify({ fromWalletId, toPixKey, amount });
  const res = http.post(`${BASE_URL}/pix/transfers`, payload, { headers: buildHeaders({ idempotencyKey }), tags: { endpoint: 'transfer_create', service: 'pix' } });
  // Defensive check: if request failed or body missing, return null to avoid TypeError upstream
  const ok = res && res.status === 201;
  check(res, { 'transfer created 201': r => r.status === 201 });
  if (!ok) {
    return null; // caller must handle retry or abort
  }
  const body = tryParse(res) || {};
  if (!body.endToEndId) {
    // Unexpected missing field; treat as failure
    return null;
  }
  return { endToEndId: body.endToEndId, status: body.status, idempotencyKey };
}

export function confirmTransfer(endToEndId) {
  // Simple immediate webhook confirmation (original behavior)
  const occurredAt = new Date(Date.now() - Math.max(0, WEBHOOK_TIMESTAMP_SKEW_MS)).toISOString();
  const payload = JSON.stringify({ endToEndId, eventId: randomId(), eventType: 'CONFIRMED', occurredAt });
  const res = http.post(`${BASE_URL}/pix/webhook`, payload, { headers: buildHeaders({}), tags: { endpoint: 'transfer_confirm', service: 'pix' } });
  const status = res.status;
  classifyWebhookResult(status, res, endToEndId);
  check(res, { 'webhook ok 200': r => r.status === 200 });
  return status === 200;
}

export function tryParse(res) {
  try { return JSON.parse(res.body); } catch (e) { return {}; }
}

export function scenarioSetup() {
  // Create two wallets and a PIX key for destination wallet
  const walletA = createWallet();
  const walletB = createWallet();
  const pixKeyB = createRandomPixKey(walletB);
  // Seed a large balance to sustain many transfers without business rejections
  depositWallet(walletA, INITIAL_BALANCE);
  return { walletA, walletB, pixKeyB };
}

export function wait(ms) { sleep(ms / 1000); }

// --- Webhook classification metrics ---
// Rates (fraction metrics) for failure categories
export const webhook_4xx_rate = new Rate('webhook_4xx_rate');
export const webhook_5xx_rate = new Rate('webhook_5xx_rate');
// Counters for detailed breakdown
export const webhook_success_count = new Counter('webhook_success_count');
export const webhook_404_count = new Counter('webhook_404_count');
export const webhook_other_4xx_count = new Counter('webhook_other_4xx_count');
export const webhook_5xx_count = new Counter('webhook_5xx_count');
export const webhook_error_code_count = new Counter('webhook_error_code_count');

function classifyWebhookResult(status, res, endToEndId) {
  const body = tryParse(res) || {};
  const errorCode = body.code || 'none';
  if (status === 200) {
    webhook_success_count.add(1);
    return;
  }
  if (status >= 500) {
    webhook_5xx_rate.add(1);
    webhook_5xx_count.add(1);
    webhook_error_code_count.add(1, { error_code: errorCode, status: String(status) });
    sampleFailed(status, endToEndId, errorCode, body.message);
    return;
  }
  if (status >= 400 && status < 500) {
    webhook_4xx_rate.add(1);
    if (status === 404) {
      webhook_404_count.add(1);
    } else {
      webhook_other_4xx_count.add(1);
    }
    webhook_error_code_count.add(1, { error_code: errorCode, status: String(status) });
    sampleFailed(status, endToEndId, errorCode, body.message);
  }
}

function sampleFailed(status, endToEndId, errorCode, message) {
  if (failSampleEmitted >= FAIL_SAMPLE_CAP) return;
  if (Math.random() * 100 <= FAIL_SAMPLE_PCT) {
    failSampleEmitted++;
    console.warn(`[webhook-fail] status=${status} code=${errorCode} e2e=${endToEndId} msg=${message || ''}`);
  }
}
