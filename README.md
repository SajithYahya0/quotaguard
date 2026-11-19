# QuotaGuard — Distributed API Rate Limiter

![CI](https://github.com/SajithYahya0/quotaguard/actions/workflows/ci.yml/badge.svg)

Multi-tenant REST API rate limiter using token buckets. Tenants register for API keys with configurable quotas; enforcement via a Spring `HandlerInterceptor` returning `429` with accurate `Retry-After` and `X-RateLimit-Remaining` headers.

## The distributed problem (and the fix)

v1 kept token buckets in a `ConcurrentHashMap` guarded by `synchronized`.
That is a JVM-scoped lock. Run two instances behind a load balancer and a
capacity-10 tenant gets 20 requests through — each instance hands out its
own quota.

Reproduce it yourself (create a tenant first, see Quick start):

    QUOTAGUARD_LIMITER=memory docker compose up --build   # v1 limiter
    ./burst.sh <api-key>                                  # ~20 admitted

    docker compose up --build                             # redis limiter (default)
    ./burst.sh <api-key>                                  # 10 admitted

Compose runs two app instances (ports 8080 and 8081) against one Postgres and
one Redis; `burst.sh` alternates requests between them like a round-robin
load balancer.

| quotaguard.limiter | admitted (capacity 10) |
|--------------------|------------------------|
| memory             | ~20  ← the bug         |
| redis              | 10   ← correct          |

**Fix:** bucket state moved to Redis, and read-refill-decide-write became a
single Lua script. Redis executes commands single-threaded, so the script is
atomic across every instance. The script uses Redis' own `TIME` so clock skew
between app servers can't corrupt the refill maths.

Both implementations ship behind a `RateLimiter` interface and are selected by
the `quotaguard.limiter` property — the in-memory one is kept deliberately so
the failure above stays demonstrable.

## Quick start

```bash
# prerequisites: Docker
docker compose up --build

# create a tenant
curl -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{"name":"acme","capacity":10,"refillPerSec":0.1}'

# burst test (use the apiKey from the response above)
./burst.sh <api-key>            # Windows PowerShell: .\burst.ps1 -Key <api-key>
```

## Production hardening

- **Hashed API keys:** Postgres stores `SHA-256(key)`; the raw key is returned once, at creation. Buckets are keyed by tenant id, so no secret sits in Redis.
- **Tenant cache:** Caffeine caches the key-to-tenant lookup for 30 s (misses too), so the hot path is one Redis round trip instead of Postgres plus Redis. A limit update evicts locally; other instances catch up within 30 s.
- **Redis failure mode:** Redis calls time out at 200 ms. `quotaguard.fail-open=true` (default) keeps serving with `X-RateLimit-Status: degraded`; `false` returns `503` with `Retry-After: 1`.

## Load test

```bash
docker compose up --build -d
k6 run loadtest/quota.js            # options: -e RATE=500 -e BASE_URL=http://localhost:8081
```

## Run the tests

```bash
docker compose up -d postgres redis   # the tests use real Postgres and Redis
./mvnw verify
```

## Tech stack

Java 21, Spring Boot 4.1, Spring Data JPA, Spring Data Redis, PostgreSQL, Redis, Lua, Caffeine, Docker Compose, GitHub Actions, k6