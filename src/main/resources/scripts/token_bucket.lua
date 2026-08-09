-- KEYS[1] = quota:{apiKey}
-- ARGV[1] = capacity   ARGV[2] = refillPerSec ARGV[3] = ttlSeconds
-- returns {allowed(0|1), remainingWholeTokens, retryAfterSeconds}

local capacity = tonumber(ARGV[1])
local refillPerSec = tonumber(ARGV[2])
local ttl = tonumber(ARGV[3])

-- Redis' own clock, not the app server's: two instances with skewed
-- system clocks must not disagree about how much time has passed.
local t   = redis.call('TIME')
local now = tonumber(t[1]) + tonumber(t[2]) / 1000000

local data   = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
local tokens = tonumber(data[1])
local ts     = tonumber(data[2])

if tokens == nil then          -- first request from this tenant
    tokens = capacity
    ts     = now
end

local elapsed = math.max(0, now - ts)
tokens = math.min(capacity, tokens + elapsed * refillPerSec)

local allowed    = 0
local retryAfter = 0

if tokens >= 1 then
    tokens  = tokens - 1
    allowed = 1
elseif refillPerSec > 0 then
    retryAfter = math.ceil((1 - tokens) / refillPerSec)
else
    retryAfter = -1            -- refill disabled: never
end

redis.call('HSET', KEYS[1], 'tokens', tokens, 'ts', now)
redis.call('EXPIRE', KEYS[1], ttl)

return { allowed, math.floor(tokens), retryAfter }