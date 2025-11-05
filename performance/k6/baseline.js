import { scenarioSetup, createTransfer, confirmTransfer, webhook_5xx_rate } from './lib/helpers.js';
import { Trend } from 'k6/metrics';
import { check, sleep } from 'k6';

export const timeToConfirm = new Trend('time_to_confirm');

export const options = {
  vus: 30,
  duration: '5m',
  tags: { scenario: 'baseline' },
  thresholds: {
    http_req_failed: ['rate<0.005'],
    'http_req_duration{endpoint:transfer_create}': ['p(95)<300'],
    time_to_confirm: ['p(95)<2000'],
    webhook_5xx_rate: ['rate<0.002']
  }
};

export function setup() { return scenarioSetup(); }

export default function (data) {
  const start = Date.now();
  const t = createTransfer(data.walletA, data.pixKeyB, '5.00');
  if (!t) {
    // Transfer creation failed; abort this iteration early
    return;
  }
  const confirmed = confirmTransfer(t.endToEndId);
  const end = Date.now();
  timeToConfirm.add(end - start);
  check(t, { 'transfer status returned': obj => !!obj.status });
  check({ confirmed }, { 'webhook ok 200': o => o.confirmed });
  // Small think time to emulate user pacing
  sleep(0.5);
}
