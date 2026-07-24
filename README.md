# QuotaGuard

Multi-tenant API rate limiting with token buckets — tenants register,
get an API key, and every request is metered against a configurable quota.

## Architecture

Client ──X-API-Key──► RateLimitInterceptor ──allow──► DemoController
                            │
                            └──deny──► 429 + Retry-After
                            │
                            ▼
                     RateLimiterService ── ConcurrentHashMap<apiKey, TokenBucket>
                            │
                     TenantService ──► TenantRepository ──► PostgreSQL

## API

| Method | Path                    | Auth      | Description             |
|--------|-------------------------|-----------|-------------------------|
| POST   | /api/tenants            | none      | Register, get API key   |
| PUT    | /api/tenants/{id}/limits| none      | Update quota config     |
| GET    | /api/data/quote         | X-API-Key | Rate-limited resource   |

## Run it

1. docker run --name quotaguard-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=quotaguard -e TZ=Asia/Kolkata -p 5432:5432 -d postgres
2. .\mvnw spring-boot:run
3. POST /api/tenants → copy apiKey → GET /api/data/quote with X-API-Key header

## Design decisions

- Token bucket over fixed window: allows short bursts, no spike at the window edge
- Lazy refill: no background threads; tokens are recomputed from elapsed time on each request
- Per-bucket synchronized: each tenant's bucket locks independently; verified with a 20-thread test
- In-memory buckets: simple, but single-instance only — Redis/Bucket4j would be the multi-instance path
- ddl-auto=update: convenient for development, not for production

## Tests

.\mvnw test — includes a concurrent stress test proving exactly N admissions under load