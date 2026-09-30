# Contract: Experiment Activation Gate

**Feature**: `issue-37659-analytics-experiment-flag-gates`

All endpoints in this document are under `/api/v1/experiments/**`.

---

## Gate Decision Matrix

The gate is evaluated **after** authentication and site-permission checks (auth/permission
failures return `401`/`403 SITE_ACCESS_DENIED` before the gate fires).

| Endpoint | App not configured | flag=false + slot used | flag=false + slot free | flag=true |
|---|---|---|---|---|
| `POST /{id}/_start` (immediate) | `503` | `403 FEATURE_DISABLED` | Allowed (10-day cap; `400` if exceeded) | Allowed |
| `POST /{id}/_start` (future-dated) | `503` | `403 FEATURE_DISABLED` | `403 FEATURE_DISABLED` | Allowed |
| `POST /scheduled/{id}/_cancel` | `503` | `403 FEATURE_DISABLED` | `403 FEATURE_DISABLED` | Allowed |
| `PUT /{id}/_archive` | `503` | `403 FEATURE_DISABLED` | `403 FEATURE_DISABLED` | Allowed |
| `POST /{id}/_end` | Allowed* | `403 FEATURE_DISABLED` | `403 FEATURE_DISABLED` | Allowed |
| `GET /health` | Allowed | Allowed | Allowed | Allowed |
| CRUD (`GET`, `POST`, `PATCH`, `DELETE`) | Allowed | Allowed | Allowed | Allowed |
| `GET /{id}/results` | `503` | Allowed | Allowed | Allowed |
| `GET /{id}/isUserIncluded` | Allowed | Allowed | Allowed | Allowed |

*`_end` is gated solely by `FEATURE_FLAG_EXPERIMENTS` — App configuration has no effect on it
because `_end` does not interact with CAEM.

---

## `403 FEATURE_DISABLED` Response Body

All `403` responses from the experiment gate MUST use this shape:

```json
{
  "errors": [
    {
      "errorCode": "FEATURE_DISABLED",
      "message": "The Experiments feature is currently disabled. Please contact dotCMS to enable it."
    }
  ]
}
```

The `errorCode` value `FEATURE_DISABLED` distinguishes feature-flag `403`s from permission
`403`s (which use `SITE_ACCESS_DENIED`).

---

## `503 Service Unavailable` Response Body

All `503` responses from the App-not-configured gate MUST use this shape:

```json
{
  "errors": [
    {
      "errorCode": "ANALYTICS_NOT_CONFIGURED",
      "message": "Analytics is not configured for this site. Please configure the Analytics App to use this feature."
    }
  ]
}
```

---

## `GET /api/v1/experiments/health` Extended Response

Always accessible. Auth required (`401` if unauthenticated).

**New fields** added to the existing `health` response:

| Field | Type | When present |
|-------|------|-------------|
| `health` | `"OK"` \| `"NOT_CONFIGURED"` \| `"CONFIGURATION_ERROR"` | Always |
| `tier` | `"full"` \| `"limited"` | Always |
| `freeExperimentUsed` | `boolean` | When `tier="limited"` |
| `warning` | `"ANALYTICS_DISABLED"` | When App not configured |

**Example responses** (see `data-model.md` for full state table).
