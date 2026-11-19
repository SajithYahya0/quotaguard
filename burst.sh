#!/usr/bin/env bash
# Fires requests alternately at both instances (8080, 8081) and counts the outcomes.
# Capacity-10 tenant: redis limiter -> 10 OK; v1 in-memory limiter -> ~20 OK (each JVM has its own bucket).
#
# Usage: ./burst.sh <api-key> [requests]      (Windows PowerShell: .\burst.ps1 -Key <api-key>)
set -euo pipefail

key="${1:?usage: ./burst.sh <api-key> [requests]}"
total="${2:-40}"
ok=0
limited=0
other=0

for ((i = 1; i <= total; i++)); do
  port=$((i % 2 == 0 ? 8080 : 8081))
  code=$(curl -s -o /dev/null -w '%{http_code}' -H "X-API-Key: ${key}" \
    "http://localhost:${port}/api/data/quote" || true)
  case "$code" in
    200) ok=$((ok + 1)) ;;
    429) limited=$((limited + 1)) ;;
    *) other=$((other + 1)) ;;
  esac
done

echo "${total} requests across 8080/8081  ->  200 OK: ${ok}   429: ${limited}   other: ${other}"
