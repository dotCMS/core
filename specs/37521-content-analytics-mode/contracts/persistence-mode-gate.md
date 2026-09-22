# Contract: Persistence Mode gate on the event-ingest proxy

No new endpoint. This documents the **behavior change** to an existing one.

## Endpoint

`POST /api/v1/analytics/content/event` — `EventAnalyticsProxyResource.proxyEventRequest`

## Current contract (unchanged parts)

- Requires `context.site_auth` in the JSON body; invalid/missing → `400` (`SiteAuthValidator`).
- Resolves the current `Host` from the request, injects `site_id` into `context`.
- Forwards the (possibly mutated) body to CAEM's `POST {DOT_ANALYTICS_BASE_URL}/v1/event/ingest`
  via `EventAnalyticsProxyHelper.proxy(...)`, asynchronously.
- Response mirrors CAEM's upstream response.

## New behavior (this feature)

Insert one check after the `Host` is resolved (`site = ContentAnalyticsUtil.getSiteFromRequest(request)`)
and before the forward call:

1. Read `persistenceMode` from `ContentAnalyticsUtil.getAppSecrets(site)` (same accessor already
   used to read `siteAuth`, `autoPageView`, etc.).
2. If `persistenceMode == "readonly"`:
   - Do **not** call `EventAnalyticsProxyHelper.proxy("event/ingest", ...)`.
   - Resume `asyncResponse` with a `200` success response (same shape a successful upstream
     ingest call would return), so callers (the browser script / SDK) see no error and do not
     retry or surface a failure to the site visitor.
   - Do not log this as a warning/error — a Read Only instance suppressing ingest is expected
     behavior, not a fault condition. A `debug`-level log line is acceptable for
     troubleshooting.
3. If `persistenceMode == "readwrite"` (or unset — see below): behavior is unchanged from today.

**Missing/unset `persistenceMode`** (an instance that had Content Analytics configured before
this field existed, mid-upgrade before the App config is re-saved): treat as `"readwrite"` —
satisfies FR-002/FR-002a without requiring a data migration of existing App secrets. The YAML
default (`selected: true` on `"readwrite"`) only applies to a *new* config; existing saved
secrets do not retroactively gain new default values, so the code path must treat "field absent"
the same as `"readwrite"` explicitly, not rely on the YAML default alone.

## Out of scope for this contract

- CAEM's `/v1/event/ingest` endpoint itself is unchanged by this repository's work — no new
  rejection logic is added there (per spec.md Assumptions and research.md R1).
- Read-side endpoints (dashboard queries) are untouched — they don't go through this proxy
  method at all (see `EventAnalyticsProxyResource`'s separate GET catch-all).
