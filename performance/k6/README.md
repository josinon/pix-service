# k6 Load Test Suite (Simplified)

## Scripts
- `stress.js` Ramping arrival rate to discover saturation (transfer creation + simulated confirmation).
- `baseline.js` Closed workload (30 VUs, 5m) for daily comparison.
- `lib/helpers.js` Reusable HTTP helpers (wallet creation, PIX key, deposit, transfer, webhook confirmation).

## Environment Variables
- `BASE_URL` (default `http://localhost:8080`)
- `TRANSFER_AMOUNT` (default `5.00`)
 - `SCENARIO_NAME` (default `unspecified`) para header `X-Scenario`
 - `RUN_ID` (default auto gerado) para correlação `X-Run-Id`
 - `INITIAL_BALANCE` (default `200000.00`) saldo inicial depositado na carteira origem para evitar erros de saldo insuficiente durante testes longos

## Thresholds
Defined inline in each script:
- `http_req_failed < 0.005`
- `http_req_duration{endpoint:transfer_create} p(95) < 300ms`
- `time_to_confirm p(95) < 2000ms`

## Workflow
1. Start stack: `docker compose up -d db app`
2. Run stress test: `BASE_URL=http://localhost:8080 k6 run performance/k6/stress.js`
3. Run baseline: `BASE_URL=http://localhost:8080 k6 run performance/k6/baseline.js`
4. Observe metrics in Grafana (custom dashboard recommended) and review k6 summary.

## Notes
- Confirmation is simulated by directly POSTing `/pix/webhook` with `eventType=CONFIRMED` immediately after creation.
- For true async measurement, replace `confirmTransfer` logic with polling or wait until external webhook arrives.
- Increase `stages` in `stress.js` if no saturation observed.
 - Cada requisição inclui headers: `X-Scenario`, `X-Run-Id`, `X-Trace-Id` (único por request) para facilitar correlação em logs/traces.
 - O saldo inicial grande (`INITIAL_BALANCE`) reduz falsos positivos de erro (409 Insufficient Funds) e deixa o threshold de falha focado em problemas reais (5xx).

## Extending
- Add soak: copy `baseline.js` with longer `duration` (e.g., `2h`).
- Add spike: new script with sudden jump in `vus`.
- Export results JSON: `k6 run --out json=results.json stress.js`.
 - Filtrar logs Loki por `X-Run-Id` para isolar execução: `{ runId="<valor>" }` caso você inclua esses campos no layout de logging.
