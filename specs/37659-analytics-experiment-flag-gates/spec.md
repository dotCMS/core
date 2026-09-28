# Feature Specification: Feature Gates for Analytics and Experiment Endpoints

**Feature Branch**: `37659-analytics-experiment-flag-gates`

**Created**: 2026-09-21

**Status**: Draft

**Type**: Task

**Input**: User description: "Add feature-flag gates to EventAnalyticsProxyResource and ExperimentsResource based on FEATURE_FLAG_CONTENT_ANALYTICS and FEATURE_FLAG_EXPERIMENTS — https://github.com/dotCMS/core/issues/37659"

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Both Features Fully Enabled (Priority: P1)

`FEATURE_FLAG_EXPERIMENTS=true` and the Analytics App is configured for the site. All
experiment and analytics capabilities are fully accessible with no restrictions.

**Why this priority**: This is the fully-enabled "happy path" that active customers must not
see disrupted. Confirming no regression here protects every verified customer currently using
analytics or experiments.

**Acceptance Scenarios**:

1. **Given** both flags are ON, **Then** all experiment and analytics endpoints behave exactly
   as they do today — no `403`, no blocked operations, no payload changes. This story exists
   solely to confirm no regression for customers with both features fully enabled.

---

### User Story 2 — Limited Experiment Mode (Priority: P1)

`FEATURE_FLAG_EXPERIMENTS=false` and the Analytics App is configured for the site. The
operator has one free experiment run available (never been used), wants to evaluate the
experiments feature before committing to a full license. Event collection is active because
the App is configured; only experiment activation is restricted.

**Why this priority**: This is the primary evaluation path for new customers — they must be
able to run a real experiment with real data collection under the free tier. A broken or
confusing limited mode would prevent adoption.

**Acceptance Scenarios**:

1. **Given** experiments=OFF and Analytics App is configured and free slot is available,
   **When** the Experiments portlet loads, **Then** `GET /api/v1/experiments/health` returns
   `tier: "limited"` and `freeExperimentUsed: false`. The creation form shows: *"You are in
   limited experiment mode. The maximum experiment duration is 10 days."* The `_schedule`,
   `_abort`, and `archive` buttons are disabled with an upgrade tooltip.
2. **Given** experiments=OFF and App configured and free slot available, **When** a backend
   user calls `_start` with a duration ≤ 10 days, **Then** the experiment starts successfully
   and event collection is active — `POST /api/v1/analytics/content/event` forwards events
   to CAEM because the App is configured.
3. **Given** experiments=OFF and App configured and free slot available, **When** a backend
   user calls `_start` with a duration > 10 days, **Then** the system returns `400 Bad
   Request` with a message indicating the 10-day duration limit.
4. **Given** experiments=OFF and App configured and free slot available, **When** the date
   picker is used in the creation form, **Then** end dates beyond 10 days from the start
   date are disabled — the UI enforces the limit before submission.
5. **Given** experiments=OFF and App configured and free slot is now used (experiment in
   RUNNING, SCHEDULED, or ENDED state), **When** `GET /api/v1/experiments/health` is called,
   **Then** it returns `tier: "limited"` and `freeExperimentUsed: true`. The `_start` button
   is also disabled with an upgrade tooltip.

---

### User Story 3 — Limited / Disabled Mode (Priority: P2)

The system enters limited mode when either `FEATURE_FLAG_EXPERIMENTS=false` **or** the
Analytics App is not configured for the site. Both conditions produce the same user-visible
behavior: experiment activation is blocked, analytics reads are blocked, and experiment
management remains accessible. The trigger determines the response code and the health
endpoint warning (see FRs), but the operator experience is identical.

**Why this priority**: Ensuring a consistent, clear limited mode regardless of the trigger
(flag or App config) prevents confusion and closes the path to accidentally running
experiments without analytics data collection.

**Acceptance Scenarios**:

1. **Given** the system is in limited mode, **When** `GET /api/v1/experiments/health` is
   called, **Then** it returns `tier: "limited"` with `freeExperimentUsed: true/false`. If
   triggered by a missing Analytics App, it also includes `warning: "ANALYTICS_DISABLED"` and
   the Experiments portlet displays a warning banner.
2. **Given** the system is in limited mode, **When** `GET /api/v1/analytics/health` is
   called, **Then** it returns `NOT_CONFIGURED` — the analytics health endpoint always returns
   a structured response and never returns `503`, even when the App is absent.
3. **Given** the system is in limited mode, **When** a backend user calls `_start`, `_schedule`,
   `_abort`, or `archive`, **Then** the system blocks the operation. CRUD, results, health,
   `_end`, `_cancel`, and `isUserIncluded` remain accessible. When `FEATURE_FLAG_EXPERIMENTS=false`
   and the free slot is available, `_start` is allowed with a 10-day duration cap (see FR-001).
4. **Given** the system is in limited mode, **When** a client POSTs any event batch to
   `POST /api/v1/analytics/content/event`, **Then** the system blocks the request and no
   event reaches the analytics backend.
5. **Given** the system is in limited mode, **When** any analytics read/query endpoint
   (`GET /api/v1/analytics/{path}`) is called, **Then** the system blocks the request.
6. **Given** the system is in limited mode, **When** the Experiments portlet loads, **Then**
   it disables `_start` (with upgrade tooltip) when `freeExperimentUsed=true`; disables
   `_schedule`, `_abort`, and `archive` always (with upgrade tooltip).

---

## Requirements *(mandatory)*

### Contract Changes

This feature introduces new `403` and `503` response codes on existing endpoints. No new
endpoints are added, no database schema changes are made, and no Elasticsearch/OpenSearch
index mapping changes are introduced.

**New response codes on previously-unrestricted endpoints:**

- `POST /api/v1/experiments/{id}/_start`, `_schedule`, `_abort`, `archive`:
  - Returns `403 FEATURE_DISABLED` when `FEATURE_FLAG_EXPERIMENTS=false` and the free slot
    is already used (`_start`), or always for `_schedule`, `_abort`, `archive`.
  - Returns `503 Service Unavailable` when `FEATURE_FLAG_EXPERIMENTS=true` but the Analytics
    App is not configured — activation is blocked because there is no backend to track events.
  - When `FEATURE_FLAG_EXPERIMENTS=false` and the free slot is available, `_start` is allowed
    with a 10-day duration cap. CRUD, results, health, `_end`, `_cancel`, and `isUserIncluded`
    remain accessible; `_schedule`, `_abort`, and `archive` remain blocked.
- `GET /api/v1/experiments/health` — response shape extended with `tier`, `freeExperimentUsed`,
  and `warning` fields (see FR-002a); the endpoint itself remains always accessible.
- `GET /api/v1/analytics/{path}` (including `/health`) — new `503 Service Unavailable` when
  the Analytics App is not configured for the site. Previously this path always proxied
  upstream. `503` is used rather than `403` because this is a configuration gap, not a
  permission failure.
- `GET /api/v1/analytics/content/siteauth/generate/{siteId}` — new `403 FEATURE_DISABLED`
  when `FEATURE_FLAG_EXPERIMENTS=false` and the Analytics App is not configured. Previously
  this endpoint always returned a token.
- `POST /api/v1/analytics/content/event`:
  - `503 Service Unavailable` when the Analytics App is not configured — regardless of the
    experiments flag state.
  - Forward as-is when the App is configured — regardless of the experiments flag state.
    This allows the limited experiment mode to collect event data.

### Functional Requirements

**ExperimentsResource — `/api/v1/experiments/**`**

- **FR-001**: When `FEATURE_FLAG_EXPERIMENTS=false`, the system operates in **limited experiment
  mode**. The following rules apply:
  - **`_start`** — allowed only when the global count of experiments in `{RUNNING, SCHEDULED,
    ENDED}` across all sites is zero. If the count is ≥ 1, the system MUST return
    `403 FEATURE_DISABLED`. When `_start` is allowed, the system MUST enforce a maximum
    10-day duration: if the requested end date exceeds 10 days from the start date, the system
    MUST return `400 Bad Request` with a message indicating the duration limit. If no end date
    is provided, the system MUST set it to 10 days from the start date.
  - **`_schedule`** — MUST always return `403 FEATURE_DISABLED`.
  - **`_abort`** — MUST always return `403 FEATURE_DISABLED`. Rationale: aborting a running
    or scheduled experiment resets it to DRAFT, which would allow the client to restart and
    effectively bypass the one-experiment limit.
  - **`archive`** — MUST always return `403 FEATURE_DISABLED`. Rationale: archiving a DRAFT
    experiment without ever running it would consume the free slot; archiving an ENDED
    experiment could be used to clear the slot and restart.
  - **All other operations** (CRUD, `_end`, `_cancel`, `isUserIncluded`, `/{id}/results`,
    `/health`) MUST remain accessible regardless of flag state and slot availability.
    Rationale: operators must be able to manage, view, and stop experiments even when the
    flag is off.
  - **Auth ordering** (applies to all endpoints gated by this spec): authentication and
    site-permission checks MUST run before the feature-flag gate. An unauthenticated caller
    MUST receive `401`, not `403` — the gate must not leak feature state to callers who have
    not yet established identity.
    **Exception — `POST /api/v1/analytics/content/event`**: this endpoint does not use dotCMS
    user session authentication. It validates a `site_auth` token present in the request
    payload; an invalid or missing `site_auth` returns `400` (malformed request), not `401`.
    This `400` is preserved as-is — the feature-flag gate runs only after `site_auth`
    validation passes, and the existing `400` behavior is not changed by this feature.
- **FR-002**: When `FEATURE_FLAG_EXPERIMENTS=true` **and** the Analytics App is configured for
  the site, all experiment endpoints MUST continue to function as they do today with no
  behavioral change, including start and scheduling operations. If the Analytics App is not
  configured, activation operations (`_start`, `_schedule`, `_abort`, `archive`) return `503`
  per FR-001.
- **FR-002a**: `GET /api/v1/experiments/health` is always accessible regardless of
  `FEATURE_FLAG_EXPERIMENTS` state. It MUST return two pieces of information:
  1. The existing health state (unchanged): `OK`, `NOT_CONFIGURED`, or `CONFIGURATION_ERROR`.
  2. **New** — the experiment tier fields:
     - `tier`: `"limited"` when `FEATURE_FLAG_EXPERIMENTS=false` OR the Analytics App is not
       configured for the site; `"full"` only when both `FEATURE_FLAG_EXPERIMENTS=true` AND
       the Analytics App is configured.
     - `freeExperimentUsed`: `true` if any experiment globally exists in `{RUNNING, SCHEDULED,
       ENDED}` across all sites; `false` otherwise. Only meaningful when
       `FEATURE_FLAG_EXPERIMENTS=false`; omitted or null when `tier="full"` or when limited
       due to App not configured (since `_start` is always blocked by `503` in that case).
  Additionally, when `FEATURE_FLAG_EXPERIMENTS=true` but the Analytics App is not configured,
  the response MUST include a `warning` field:
  - `warning`: `"ANALYTICS_DISABLED"` — present only when experiments=ON and the Analytics
    App is not configured; omitted otherwise.
  Example responses:
  ```json
  { "health": "OK", "tier": "limited", "freeExperimentUsed": true }
  ```
  ```json
  { "health": "OK", "tier": "limited", "freeExperimentUsed": false,
    "warning": "ANALYTICS_DISABLED" }
  ```
  > **Note**: The Experiments portlet UI calls this endpoint on load to decide whether to
  > render the experiments interface and to drive button state in limited mode:
  > - `tier="full"` → all experiment operations enabled normally.
  > - `tier="limited"`, `freeExperimentUsed=false` → `_start` enabled; `_schedule`, `_abort`,
  >   and `archive` disabled with tooltip *"Upgrade your plan to unlock this feature. Contact dotCMS."*
  >   The experiment creation form MUST display an informational message such as *"You are in
  >   limited experiment mode. The maximum experiment duration is 10 days."* The duration date
  >   picker MUST restrict the end date to a maximum of 10 days from the selected start date —
  >   dates beyond that range MUST be disabled in the picker.
  > - `tier="limited"`, `freeExperimentUsed=true` → `_start`, `_schedule`, `_abort`, and
  >   `archive` all disabled with tooltip *"Upgrade your plan to unlock this feature. Contact dotCMS."*
  > - `warning="ANALYTICS_DISABLED"` → the portlet MUST display a dismissable warning banner:
  >   *"Analytics data collection is currently disabled. Experiments require Analytics to be
  >   enabled for full functionality. Please contact dotCMS to activate it."* This banner is
  >   shown alongside the normal experiments interface and does not block usage.

**EventAnalyticsProxyResource — GET endpoints**

- **FR-003**: `GET /api/v1/analytics/{path}` (all read/query paths except siteauth)
  MUST return `503 Service Unavailable` when the Analytics App is not configured for the
  site — this signals a configuration problem, not a permission failure. When the App is
  configured, requests are proxied to the analytics backend normally.
- **FR-003a**: `GET /api/v1/analytics/health` is currently a pass-through proxy — it forwards
  the request upstream to CAEM and returns whatever CAEM responds with. This feature changes
  that behavior unconditionally: dotCMS MUST always perform its own health evaluation (verify
  that the required Analytics App credentials are configured for the current site, then probe
  the analytics backend) and return one of three dotCMS-produced states — it MUST NOT pass
  through the raw upstream response for this path:
  - `OK` — required credentials are configured and the analytics backend is reachable.
  - `NOT_CONFIGURED` — no analytics configuration has been set up for the site.
  - `CONFIGURATION_ERROR` — configuration exists but is incomplete (missing required
    credentials) or the analytics backend is unreachable.
  The auth model of the current catch-all MUST be preserved: a backend user is required
  (unauthenticated callers receive `401`) and the caller must have site READ permission
  (callers without permission receive `403 SITE_ACCESS_DENIED`). These checks run before
  the feature-flag gate and before the health evaluation.
  This is intentional scope beyond "add a gate": the Analytics portlet UI depends on these
  structured states to decide whether to render; a raw proxy response cannot serve that role
  reliably. The health check pattern mirrors the one already implemented for the experiments
  health endpoint (FR-002a). The `NOT_CONFIGURED` state now carries the full meaning of
  "analytics not available for this site" — whether because the App was never set up or
  because credentials are absent. This endpoint is always accessible; it is never gated.
  > **Note**: The Analytics portlet UI calls this endpoint to decide whether to render the
  > analytics interface. A response of `OK` enables the UI; `NOT_CONFIGURED` or
  > `CONFIGURATION_ERROR` causes the UI to display an error message to the operator instead.
  **UI changes required as part of this feature:**
  - The Analytics portlet health check client MUST be updated to consume the new three-state
    response (OK / NOT_CONFIGURED / CONFIGURATION_ERROR) instead of the current
    `entity.available` boolean mapping.
  - When the backend returns `NOT_CONFIGURED`, the Analytics portlet MUST display a message
    communicating that Analytics is an advanced feature and directing the operator to contact
    their Customer Success representative for more information.
    Example: *"Analytics is an advanced feature that enables you to collect and analyze user
    behavior data. Please contact your Customer Success representative for more information."*
  - When the backend returns `CONFIGURATION_ERROR`, the Analytics portlet MUST display a
    message directing the operator to check their Analytics configuration or contact dotCMS
    Support.
    Example: *"Please check your Analytics configuration, or contact dotCMS Support for
    assistance."*
  - The Analytics portlet error component already contains the message keys for these states;
    they become reachable once the health check client passes the backend states through.
- **FR-004**: `GET /api/v1/analytics/content/siteauth/generate/{siteId}` MUST return
  `403 FEATURE_DISABLED` only when `FEATURE_FLAG_EXPERIMENTS=false` AND the Analytics App is
  not configured for the site. If either condition is met (flag=true or App configured), the
  endpoint MUST be accessible.
- **FR-005**: The siteauth endpoint MUST be accessible whenever `FEATURE_FLAG_EXPERIMENTS=true`
  or the Analytics App is configured, because event sends to CAEM require a valid site auth
  token.

**EventAnalyticsProxyResource — POST `/api/v1/analytics/content/event`**

- **Gate ordering**: the feature-flag gate introduced by this feature MUST run before the
  existing `persistenceMode=readonly` check. The ordering is:
  auth → feature-flag gate (this feature) → persistenceMode check → forwarding.

- **FR-006**: The event ingest gate is driven solely by Analytics App configuration:
  - If the Analytics App is **not configured**: MUST return `503 Service Unavailable`
    regardless of `FEATURE_FLAG_EXPERIMENTS` state. No call is made to the analytics backend.
  - If the Analytics App is **configured**: MUST forward the entire event payload to the
    analytics backend as-is, for all event types, regardless of `FEATURE_FLAG_EXPERIMENTS`
    state. CAEM's response is returned as-is. No filtering, no mutation. This allows the
    limited experiment mode to actually collect event data when the App is set up.

**Feature-disabled error response**

- **FR-009**: Every `403 FEATURE_DISABLED` returned by any gated endpoint MUST include a
  machine-readable error code `FEATURE_DISABLED` and a human-readable message in the standard
  dotCMS error response envelope. This applies to all gated endpoints including the event
  ingest endpoint.
  - Example `403` response body:
    ```json
    { "errors": [{ "errorCode": "FEATURE_DISABLED", "message": "The Experiments feature is currently disabled. Please contact dotCMS to enable it." }] }
    ```
  - The `FEATURE_DISABLED` error code distinguishes feature-flag `403`s from other `403`s
    on the same endpoints (e.g. `SITE_ACCESS_DENIED` for permission failures).
  - **Test**: Postman — for each flag-disabled `403` case, verify the response body contains
    `errorCode: "FEATURE_DISABLED"` and the expected human-readable message.
  > **Note — SDK follow-up (out of scope for this issue)**: the client-side analytics SDK
  > calls `POST /api/v1/analytics/content/event` directly from customer site browsers. When
  > the feature is disabled and the gate returns `403 FEATURE_DISABLED`, the SDK should catch
  > that response and degrade gracefully (e.g. log a warning rather than throwing an unhandled
  > error) so the `403` is visible in the network tab but does not surface as a console error
  > on the customer site. The `FEATURE_DISABLED` error code provides the hook the SDK needs
  > to identify this specific case. This SDK update is intentionally deferred because the SDK
  > is under active development; it should be tracked as a follow-up task.
  - **Test**: Postman — for each flag-disabled `403` case, verify the response body contains
    `errorCode: "FEATURE_DISABLED"` and the expected human-readable message.

**Flag state refresh**

- **FR-010**: `FEATURE_FLAG_EXPERIMENTS` currently supports runtime changes without a server
  restart — an administrator can toggle it and all consumers react immediately. This live-toggle
  capability MUST be removed system-wide. Every consumer of this flag MUST read its value
  exactly once at server startup and hold that value for the entire lifetime of the running
  instance. A runtime configuration change MUST NOT alter any consumer's behavior without a
  server restart. The intended activation mechanism is: set the flag value in configuration
  (via the dotCMS system config UI or a config file), then perform a server restart — at which
  point the new value is read once at startup and takes effect system-wide. This is a
  deliberate design choice: experiments is a paid feature, and activation or deactivation
  MUST require an explicit, intentional restart by an operator.
  Analytics is no longer controlled by a flag — it is controlled by whether the Analytics App
  is configured for the site, which takes effect immediately when the App is set or removed.
  **Breaking change**: the default value for `FEATURE_FLAG_EXPERIMENTS` MUST change from
  `true` to `false` **system-wide** — for every consumer of this flag across the system.
  Because the current customer footprint for experiments is small, each active customer MUST
  be verified before release to confirm their configuration explicitly sets the flag to `true`.
  Rationale: these are paid features — a restart gives operators an explicit, intentional
  activation step rather than an immediate live toggle. `FEATURE_FLAG_CONTENT_ANALYTICS`
  may be re-defaulted to `true` once the feature is considered broadly available.
  - **Rollback safety**: this change is rollback-safe. The deployment process only *adds*
    explicit `=true` entries for verified active customers — it does not remove existing
    explicit `=false` entries. A rollback therefore restores the pre-deploy state for every
    customer category: active customers retain their explicit `=true` (features stay enabled),
    non-active customers revert to the `true` default (harmless — they were not using the
    features), and any customer with an explicit `=false` remains blocked. No config cleanup
    is required before rolling back.
  - **Test**: Integration — toggle a flag via the dotCMS configuration system without
    restarting the server and confirm the gate behavior does not change; the flag state read
    at startup must remain in effect. The `true`→`false` default-value change is a
    deployment-time verification described in the Assumptions section, not an automated test.

### Key Entities

- **Feature Flag**: `FEATURE_FLAG_EXPERIMENTS` — a named boolean configuration property that
  controls the experiment tier (full access vs limited mode). Currently supports runtime
  changes without a restart; this live-toggle capability MUST be removed system-wide as part
  of this feature (FR-010). After this change, the value is read once at server startup and
  held for the lifetime of the instance — a restart is required for changes to take effect.
  `FEATURE_FLAG_CONTENT_ANALYTICS` is removed; analytics availability is now determined by
  whether the Analytics App is configured for the site.
- **Analytics App**: The site-level App configuration that provides credentials (site auth,
  bearer token, URL) for connecting to the CAEM analytics backend. When the App is configured
  for a site, analytics is enabled for that site. When it is absent or incomplete, analytics
  is not available — the health endpoint returns `NOT_CONFIGURED` or `CONFIGURATION_ERROR`
  and analytics read endpoints return `403`.

- **Site Auth Validation**: The `site_auth` field in the `POST /api/v1/analytics/content/event`
  request payload is validated before the feature-flag gate runs. An invalid or missing
  `site_auth` returns `400 Bad Request` — this is payload validation, not session
  authentication, and is unaffected by flag or App configuration state.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: When `FEATURE_FLAG_EXPERIMENTS=false` and the Analytics App is configured
  (limited experiment mode): `_schedule`, `_abort`, and `archive` return `403 FEATURE_DISABLED`;
  `_start` succeeds once (free slot, max 10-day duration enforced with `400` if exceeded) and
  returns `403 FEATURE_DISABLED` thereafter; all other experiment operations remain accessible;
  event ingest forwards to CAEM as-is. The experiments health endpoint returns `tier: "limited"`
  and `freeExperimentUsed` reflecting slot state; the Experiments portlet disables gated
  buttons and shows an upgrade tooltip.
- **SC-002**: When the Analytics App is configured, all event types are forwarded to the
  analytics backend as-is — event ingest depends solely on App configuration, not on the
  experiments flag.
- **SC-003**: When the Analytics App is not configured, no data reaches the analytics backend
  — event ingest and analytics read requests return `503 Service Unavailable`, and experiment
  activation operations return `503`. Management operations (CRUD, results, health) remain
  accessible.
- **SC-004**: A change to `FEATURE_FLAG_EXPERIMENTS` takes effect only after a server restart
  — live toggling without a restart is intentionally not supported. Changes to the Analytics
  App configuration take effect immediately without a restart.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The analytics event ingest path and the experiment management
  REST layer. Both are part of the modern analytics/experiments feature area (`com.dotcms.*`),
  not the legacy `com.dotmarketing.*` surface. The `EventLogWebInterceptor` (the original
  Jitsu proxy path) is **explicitly out of scope** — it is being removed and must not be
  modified by this work.
- **Backward-compatibility expectations**: When `FEATURE_FLAG_EXPERIMENTS=true` and the
  Analytics App is configured (the fully-enabled state that existing customers have), behavior
  must be identical to today — no requests that currently succeed may be broken. The gate
  enforcement is additive: it restricts disabled or misconfigured states, not the enabled state.
  **Accepted exception — `GET /api/v1/analytics/health`**: the response shape of this endpoint
  changes unconditionally (see FR-003a) — it no longer proxies raw CAEM responses. The
  Analytics portlet frontend MUST be updated as part of this feature to consume the new format.
- **Known related decisions**: `FEATURE_FLAG_EXPERIMENTS` currently supports live toggling.
  This feature must disable that live-toggle behavior so that activating or deactivating
  experiments requires a deliberate restart. The Analytics App configuration (which replaces
  `FEATURE_FLAG_CONTENT_ANALYTICS`) takes effect immediately — no restart needed. The plan
  phase will consult `dotCMS/platform-adrs` for any ADRs governing feature-flag architecture.

## Assumptions

- `FEATURE_FLAG_EXPERIMENTS` currently defaults to `true` in the codebase when no explicit
  value is set in configuration. The gate enforcement introduced by this feature must account
  for this: existing deployments that do not explicitly set this flag are currently running
  with experiments enabled, and the gate behavior must not silently change that.
- The siteauth token is stateless and does not carry flag or App configuration state — the
  gate is enforced at the endpoint level, not embedded in the token.
- The gates described here are dotCMS-side pre-flight checks and are not a replacement for
  any existing validations in the analytics pipeline.
- No non-UI consumers of `GET /api/v1/analytics/health` are known; the response-shape change
  introduced by FR-003a affects only the Analytics portlet, which is updated as part of this
  feature.
