#!/usr/bin/env bash
# #13 smoke test — needs a valid gateway JWT in $JWT.
set -euo pipefail
BASE="${BASE:-http://localhost:8081}"
echo "== health =="; curl -sf "$BASE/actuator/health" | head -c 300; echo
echo "== unauthenticated POST must be 401/403 =="
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/chatclient" \
  -H 'Content-Type: application/json' -d '{"message":"hi"}')
echo "status=$code"; [ "$code" = "401" ] || [ "$code" = "403" ]
echo "== authenticated chat =="
curl -sf -X POST "$BASE/chatclient" -H "Authorization: Bearer $JWT" \
  -H 'Content-Type: application/json' \
  -d '{"message":"List advisors","sessionId":"smoke-1"}' | head -c 500; echo
echo SMOKE-OK
