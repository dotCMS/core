#!/usr/bin/env bash
# Checks for dotCMS SQL injection in /api/auditPublishing/getAll
# Fixed in: dotcms/dotcms:v26.04.28-03_6017bcd and dotcms/dotcms:v26.05.06-01+
#
# The patch added backend-user authentication to the endpoint.
# An unauthenticated 401 means patched; anything else means the
# auth gate is absent and the injectable query is reachable.
#
# Usage: ./test-si75-vuln.sh <target-url>
#
# Examples:
#   ./test-si75-vuln.sh demo.dotcms.com
#   ./test-si75-vuln.sh https://your-instance.dotcms.com

TARGET="${1:?Usage: $0 <target-url>}"

# Ensure scheme is present — default to https://
[[ "$TARGET" != http://* && "$TARGET" != https://* ]] && TARGET="https://$TARGET"

URL="${TARGET%/}/api/auditPublishing/getAll"

BODY_FILE=$(mktemp)
trap 'rm -f "$BODY_FILE"' EXIT

CURL_OUT=$(curl -k -s -L \
  -o "$BODY_FILE" \
  -w '%{http_code} %{url_effective}' \
  -X POST "$URL" \
  -H 'Content-Type: application/json' \
  -d '["probe"]')

HTTP_CODE="${CURL_OUT%% *}"
FINAL_URL="${CURL_OUT##* }"
BODY=$(cat "$BODY_FILE")

hr() { printf '%.0s─' {1..60}; echo; }

case "$HTTP_CODE" in
  401|403)
    hr
    echo "  RESULT: NOT VULNERABLE"
    hr
    echo "  HTTP $HTTP_CODE — the auth gate is present."
    echo "  This instance requires a back-end user login to reach"
    echo "  /api/auditPublishing/getAll, blocking the SQL injection."
    echo ""
    echo "  Note: a WAF in front of dotCMS can also produce this result."
    hr
    ;;
  200|404)
    if [[ "$FINAL_URL" != *"/api/auditPublishing/getAll"* ]]; then
      hr
      echo "  RESULT: INCONCLUSIVE"
      hr
      echo "  HTTP $HTTP_CODE — request was redirected away from the endpoint."
      echo "  Redirect destination: $FINAL_URL"
      echo ""
      echo "  The instance may be forwarding unauthenticated requests to a"
      echo "  login page rather than returning 401. Inspect the redirect chain:"
      echo "    curl -k -L -v $URL"
      hr
    elif echo "$BODY" | grep -qi "<html"; then
      hr
      echo "  RESULT: INCONCLUSIVE"
      hr
      echo "  HTTP $HTTP_CODE — response body is HTML, not a dotCMS API response."
      echo "  Verify the target URL points to a dotCMS instance: $URL"
      hr
    else
      hr
      echo "  RESULT: VULNERABLE"
      hr
      echo "  HTTP $HTTP_CODE — the endpoint accepted an unauthenticated request."
      echo "  The auth gate is absent and the injectable query is reachable."
      echo ""
      echo "  Update to release dotcms/dotcms:v26.05.06-01 or later:"
      hr
    fi
    ;;
  000)
    hr
    echo "  RESULT: INCONCLUSIVE"
    hr
    echo "  Could not connect to: $URL"
    hr
    ;;
  *)
    hr
    echo "  RESULT: INCONCLUSIVE"
    hr
    echo "  HTTP $HTTP_CODE — unexpected response."
    echo "  A WAF or proxy may be in front of dotCMS."
    hr
    ;;
esac
