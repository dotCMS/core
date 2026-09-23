# Data Model: Content Analytics Persistence Mode (Persist / Read Only)

## Instance Analytics Configuration

Not a new persistence entity — an addition to the existing **Content Analytics App** config,
stored the same way every other field in `dotContentAnalytics-config.yml` is stored (as an App
`Secret`, per-Host, via `AppsAPI`).

| Field | Type | Values | Default | Notes |
|---|---|---|---|---|
| `persistenceMode` | `SELECT` (`com.dotcms.security.apps.Type.SELECT`) | `"readwrite"` (label "Read & Write"), `"readonly"` (label "Read Only") | `"readwrite"` (`selected: true`) | Follows the same YAML shape as `signatureValidationType` in `dotsaml-config.yml`. Read via `ContentAnalyticsUtil.getAppSecrets(host)` the same way `siteAuth`, `autoPageView`, etc. are read today. |

**Scope**: per-Host, same as every other Content Analytics app field (the App framework has no
separate "instance-wide" storage tier — System Host is simply the Host every other Host's
secrets fall back to when not overridden, which is how the "instance-wide" framing in spec.md's
Key Entities is achieved in practice: the field is configured at System Host and not overridden
per-site).

**Validation rule**: Exactly one option carries `selected: true` in the YAML default — enforced
by `AppDescriptorHelper`'s existing `SELECT` validation (already in place, not new code).

## Analytics Event

Unchanged by this feature — the JSON body posted to
`POST /api/v1/analytics/content/event` and forwarded verbatim (plus an injected `site_id`) to
CAEM's `POST /v1/event/ingest`. This plan does not add, remove, or rename any field in that
body. (The `environment` field — its ingest requiredness — is governed entirely on the CAEM
side; see `research.md` R2/R5.)
