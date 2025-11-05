import { Trend } from 'k6/metrics';
import { scenarioSetup, createTransfer, confirmTransfer, webhook_5xx_rate } from './lib/helpers.js';
import { check } from 'k6';

export const timeToConfirm = new Trend('time_to_confirm');

export const options = {
  scenarios: {
    stress: {
      executor: 'ramping-arrival-rate',
      startRate: 25,        // warm-up initial RPS (will ramp quickly)
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 500,
      stages: [
        { duration: '30s', target: 25 }, // warm-up (ignorar métricas desta fase)
        { duration: '1m', target: 50 },
        { duration: '1m', target: 100 },
        { duration: '1m', target: 150 },
        { duration: '1m', target: 200 },
        { duration: '1m', target: 250 },
        { duration: '1m', target: 300 }
      ],
      tags: { scenario: 'stress' }
    }
  },
  thresholds: {
    http_req_failed: ['rate<0.005'],
    'http_req_duration{endpoint:transfer_create}': ['p(95)<300'],
    time_to_confirm: ['p(95)<2000'],
    webhook_5xx_rate: ['rate<0.002'] // foco em falhas de servidor reais no webhook
  },
  discardResponseBodies: false,
};

export function setup() {
  return scenarioSetup();
}

export default function (data) {
  const start = Date.now();
  const t = createTransfer(data.walletA, data.pixKeyB, '5.00');
  if (!t) {
    // Skip webhook if transfer creation failed; could add retry logic here
    return;
  }
  const confirmed = confirmTransfer(t.endToEndId);
  const end = Date.now();
  timeToConfirm.add(end - start); // ms
  check(t, { 'transfer status returned': obj => !!obj.status });
  check({ confirmed }, { 'webhook ok 200': o => o.confirmed });
}

export function teardown(data) {
  // Placeholder: could trigger summary or cleanup if needed.
}
