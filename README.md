# QuotaGuard — Distributed API Rate Limiter

![CI](https://github.com/SajithYahya0/quotaguard/actions/workflows/ci.yml/badge.svg)

Multi-tenant REST API rate limiter using token buckets. Tenants register for API keys with configurable quotas; enforcement via a Spring `HandlerInterceptor` returning `429` with accurate `Retry-After` and `X-RateLimit-Remaining` headers.

## The distributed problem (and the fix)

v1 kept token buckets in a `ConcurrentHashMap` guarded by `synchronized`.
That is a JVM-scoped lock. Run two instances behind a load balancer and a
capacity-10 tenant gets 20 requests through — each instance hands out its
own quota.

Reproduce it yourself:

    docker compose up --build
    ./burst.sh <api-key>

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
./burst.sh <api-key>
```

## Tech stack

Java 21, Spring Boot 4.1, Spring Data JPA, Spring Data Redis, PostgreSQL, Redis, Lua, Docker Compose, GitHub Actions