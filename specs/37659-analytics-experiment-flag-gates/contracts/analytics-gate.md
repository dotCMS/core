# Contract: Analytics Endpoint Gate

**Feature**: `issue-37659-analytics-experiment-flag-gates`

All endpoints in this document are under `/api/v1/analytics/**`.

---

## Gate Decision Matrix

| Endpoint | App not configured | App configured |
|---|---|---|
| `GET /health` | `{ "health": "NOT_CONFIGURED" }` (200) | `{ "health": "OK" }` or `{ "health": "CONFIGURATION_ERROR" }` (200) |
| `GET /{path:.*}` (all other GET paths) | `503 Service Unavailable` | Proxied to CAEM |
| `GET /content/siteauth/generate/{siteId}` | Always accessible (never gated) | Always accessible (never gated) |
| `POST /content/event` | `503 Service Unavailable` | Forwarded to CAEM as-is |

`FEATURE_FLAG_EXPERIMENTS` has no effect on any of these analytics endpoints.

---

## `GET /api/v1/analytics/health` — dotCMS-Evaluated Response

**Always returns 200** regardless of App configuration. Never returns `503`. Auth required
(`401` if unauthenticated); site READ permission required (`403 SITE_ACCESS_DENIED` if missing).

| App state | Backend reachable | Response body |
|---|---|---|
| Not configured | — | `{ "entity": { "health": "NOT_CONFIGURED" } }` |
| Configured (credentials present) | Yes | `{ "entity": { "health": "OK" } }` |
| Configured but credentials incomplete, or backend unreachable | No / error | `{ "entity": { "health": "CONFIGURATION_ERROR" } }` |

This endpoint no longer proxies the raw CAEM response. The response is always dotCMS-produced
in the standard `ResponseEntityView` envelope.

---

## `POST /api/v1/analytics/content/event` — Gate Ordering

Gate order (no session auth on this endpoint):

1. Parse JSON body
2. Resolve site from request
3. **App config check** — if `getAppSecrets(site).isEmpty()` → `503 Service Unavailable`
4. `site_auth` validation — if invalid or missing → `400 Bad Request`
5. Persistence-mode check — if `"readonly"` → `200` (suppressed, no forward)
6. Forward to CAEM

**`503` response body**:
```json
{
  "errors": [
    {
      "errorCode": "ANALYTICS_NOT_CONFIGURED",
      "message": "Analytics is not configured for this site."
    }
  ]
}
```

---

## `GET /api/v1/analytics/content/siteauth/generate/{siteId}` — No Change

This endpoint is never gated. It must remain accessible in all flag states and App
configuration states to allow operators to set up the Analytics App credentials.
No `503` is ever returned from the feature-flag gate for this path.
