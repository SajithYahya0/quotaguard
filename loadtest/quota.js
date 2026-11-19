// QuotaGuard load test: latency through the full limiter path
// (API-key lookup + Redis Lua call) at a fixed request rate.
//
//   k6 run loadtest/quota.js                                   # 300 req/s for 60 s against :8080
//   k6 run -e RATE=600 -e BASE_URL=http://localhost:8081 loadtest/quota.js
//
// If the summary shows dropped_iterations > 0, your machine couldn't keep up: lower RATE.
import http from "k6/http";
import { check } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const RATE = Number(__ENV.RATE || 300);

export const options = {
  scenarios: {
    steady: {
      executor: "constant-arrival-rate",
      rate: RATE,
      timeUnit: "1s",
      duration: "60s",
      preAllocatedVUs: 50,
      maxVUs: 300,
    },
  },
  summaryTrendStats: ["avg", "p(50)", "p(95)", "p(99)", "max"],
  thresholds: { http_req_failed: ["rate<0.01"] },
};

// One tenant with a quota far above the test volume, so we measure the limiter's cost, not 429s.
export function setup() {
  const res = http.post(
    `${BASE_URL}/api/tenants`,
    JSON.stringify({ name: "k6", capacity: 10000000, refillPerSec: 1000000 }),
    { headers: { "Content-Type": "application/json" } },
  );
  if (res.status !== 201) {
    throw new Error(`tenant create failed: ${res.status} ${res.body}`);
  }
  return { apiKey: res.json("apiKey") };
}

export default function (data) {
  const res = http.get(`${BASE_URL}/api/data/quote`, { headers: { "X-API-Key": data.apiKey } });
  check(res, { "status is 200": (r) => r.status === 200 });
}
