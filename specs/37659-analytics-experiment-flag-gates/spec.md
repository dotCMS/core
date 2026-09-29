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

`FEATURE_FLAG_EXPERIMENTS=false` and the Analytics App is configured for the site. No
experiment has ever been activated for this instance (the free slot is available). The
operator can run exactly one experiment with a maximum duration of 10 days; event collection
works normally because the App is configured. Scheduling, canceling a scheduled experiment,
and archiving are blocked. Once the experiment completes, the free slot is consumed and no
further experiments can be started without upgrading.

**Why this priority**: This is the primary evaluation path for new customers — they must be
able to run a real experiment with real data collection under the free tier. A broken or
confusing limited mode would prevent adoption.

**Acceptance Scenarios**:

1. **Given** experiments=OFF and Analytics App is configured and free slot is available,
   **When** the Experiments portlet loads, **Then** `GET /api/v1/experiments/health` returns
   `tier: "limited"` and `freeExperimentUsed: false`. The creation form shows: *"You are in
   limited experiment mode. The maximum experiment duration is 10 days. Once started, the
   experiment cannot be stopped and must finish normally."* The **scheduling section**, the
   **Stop Experiment** button, and the **archive** action are disabled with an upgrade tooltip.
2. **Given** experiments=OFF and App configured and free slot available, **When** the user
   clicks **Start** on the experiment creation form, **Then** a confirmation dialog is shown
   with the message: *"You are about to use your one free experiment. Because you are in
   limited mode, this experiment cannot be stopped once it starts — it must run until it
   completes naturally. To unlock the ability to stop experiments, schedule them, and run
   multiple at once, upgrade your plan. Contact dotCMS Customer Success to get started."*
   If the user clicks **Cancel**, the dialog is dismissed and the experiment is not started.
   If the user clicks **Continue**, the `_start` call is made with a duration ≤ 10 days,
   the experiment starts successfully, and event collection is active —
   `POST /api/v1/analytics/content/event` forwards events to CAEM because the App is
   configured.
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

### User Story 3 — Disabled Mode (Priority: P2)

The Analytics App is not configured for the site. The operator has no analytics backend
connected — event collection is unavailable, no experiment can be started, and experiment
activation is blocked regardless of the `FEATURE_FLAG_EXPERIMENTS` state. Basic experiment
management (CRUD, health) remains accessible so operators can still view and clean up
existing experiments; however, experiment results are not available because the analytics
backend is not connected.

**Why this priority**: When the Analytics App is absent, no data collection is possible.
Allowing experiments to start without a connected backend would produce experiments with
zero event data, giving operators meaningless results. Blocking activation closes this path
and surfaces a clear signal to contact dotCMS for setup.

**Acceptance Scenarios**:

1. **Given** the Analytics App is not configured, **When** `GET /api/v1/experiments/health`
   is called, **Then** it returns `tier: "limited"`, `freeExperimentUsed: true/false`, and
   `warning: "ANALYTICS_DISABLED"` when `FEATURE_FLAG_EXPERIMENTS=true`; the Experiments
   portlet displays a warning banner.
2. **Given** the Analytics App is not configured, **When** `GET /api/v1/analytics/health` is
   called, **Then** it returns `NOT_CONFIGURED` — the analytics health endpoint always returns
   a structured response and never returns `503`, even when the App is absent.
3. **Given** the Analytics App is not configured, **When** a backend user attempts any
   experiment activation operation (`_start`, `_schedule`, `_cancel`, or `archive`), **Then** the system blocks the operation and returns `503 Service
   Unavailable`. Basic management operations (CRUD, health, `_end`, and `isUserIncluded`)
   remain accessible; experiment results are not available.
4. **Given** the Analytics App is not configured, **When** the experiment results endpoint
   is called, **Then** the system returns `503 Service Unavailable` — results depend on the
   analytics backend and cannot be retrieved without it.
5. **Given** the Analytics App is not configured, **When** a client POSTs any event batch to
   `POST /api/v1/analytics/content/event`, **Then** the system returns `503 Service Unavailable`
   and no event reaches the analytics backend.
6. **Given** the Analytics App is not configured, **When** any analytics read/query endpoint
   (`GET /api/v1/analytics/{path}`) is called, **Then** the system returns `503 Service Unavailable`.
7. **Given** the Analytics App is not configured, **When** the Experiments portlet loads,
   **Then** `_start`, `_schedule`, `_cancel`, and `archive` are all disabled with the
   upgrade tooltip.

---

## Requirements *(mandatory)*

### Contract Changes

This feature introduces new `403` and `503` response codes on existing endpoints, and
extends the health endpoint response shape. No new endpoints are added, no database schema
changes are made, and no Elasticsearch/OpenSearch index mapping changes are introduced.

**1. Experiment activation endpoints**

Affected: `POST /api/v1/experiments/{id}/_start` (immediate and future-dated),
`POST /api/v1/experiments/scheduled/{id}/_cancel`, and `archive`.

*When the Analytics App is not configured (any flag state):*
- All activation endpoints → `503 Service Unavailable`. Activation is blocked because there
  is no backend to track events.
- CRUD, health, `_end`, and `isUserIncluded` remain accessible; results are not available.

*When `FEATURE_FLAG_EXPERIMENTS=false` and the free slot is already used:*
- `_start` (immediate), future-dated `_start`, `_cancel`, `archive`, and `_end` → `403 FEATURE_DISABLED`.
- CRUD, results, health, and `isUserIncluded` remain accessible.

*When `FEATURE_FLAG_EXPERIMENTS=false` and the free slot is available (limited mode):*
- `_start` with an immediate start date → allowed with a 10-day duration cap.
- Future-dated `_start`, `_cancel`, `archive`, and `_end` → `403 FEATURE_DISABLED`.
- CRUD, results, health, and `isUserIncluded` remain accessible.

**2. Experiment health endpoint**

`GET /api/v1/experiments/health` — response shape extended with `tier`,
`freeExperimentUsed`, and `warning` fields (see FR-002a). The endpoint itself remains
always accessible regardless of flag state or App configuration.

**3. Experiment results endpoint**

`GET /api/v1/experiments/{id}/results` — returns `503 Service Unavailable` when the
Analytics App is not configured. Results depend on the analytics backend and cannot be
retrieved without it. Previously this endpoint always proxied upstream.

**4. Analytics health endpoint**

`GET /api/v1/analytics/health` — always returns a structured response regardless of App
configuration. Returns `NOT_CONFIGURED` when the App is absent. Never returns `503`.

**5. Analytics read/query endpoints**

`GET /api/v1/analytics/{path}` (excluding `/health`) — returns `503 Service Unavailable`
when the Analytics App is not configured for the site. Previously this path always proxied
upstream. `503` is used rather than `403` because this is a configuration gap, not a
permission failure.

**6. Siteauth token endpoint**

`GET /api/v1/analytics/content/siteauth/generate/{siteId}` — **no change**. This endpoint
is never gated by this feature. It must remain always accessible to allow operators to
configure the Analytics App regardless of flag state.

**7. Event ingest endpoint**

`POST /api/v1/analytics/content/event`:
- Returns `503 Service Unavailable` when the Analytics App is not configured — regardless
  of the experiments flag state.
- Forwards as-is when the App is configured — regardless of the experiments flag state.
  This allows the limited experiment mode to collect event data.

### Functional Requirements

**ExperimentsResource — `/api/v1/experiments/**`**

- **FR-001**: When `FEATURE_FLAG_EXPERIMENTS=false`, the system operates in **limited experiment
  mode**. The following rules apply:
  > **Implementation note**: `_schedule` in this spec refers to `POST /{id}/_start` with a
  > future `startDate` (which puts the experiment in SCHEDULED state). `_abort` refers to
  > `POST /v1/experiments/scheduled/{experimentId}/_cancel`. Neither `_schedule` nor `_abort`
  > exist as separate endpoints in `ExperimentsResource`.
  - **`_start`** — allowed only when the global count of experiments in `{RUNNING, SCHEDULED,
    ENDED}` across all sites is zero. If the count is ≥ 1, the system MUST return
    `403 FEATURE_DISABLED`. When `_start` is allowed, the system MUST enforce a maximum
    10-day duration: if the requested end date exceeds 10 days from the start date, the system
    MUST return `400 Bad Request` with a message indicating the duration limit. If no end date
    is provided, the system MUST set it to 10 days from the start date.
  - **`_schedule`** — MUST always return `403 FEATURE_DISABLED`.
  - **`_cancel`** — MUST always return `403 FEATURE_DISABLED`. Rationale: aborting a running
    or scheduled experiment resets it to DRAFT, which would allow the client to restart and
    effectively bypass the one-experiment limit.
  - **`archive`** — MUST always return `403 FEATURE_DISABLED`. Rationale: the free slot
    check counts experiments in `{RUNNING, SCHEDULED, ENDED}` state — it does not count
    ARCHIVED ones. Allowing archive would let a user run their free experiment, archive it
    once finished, and recover the free slot to run another, bypassing the one-experiment
    limit entirely.
  - **`_end`** — MUST return `403 FEATURE_DISABLED` in limited mode. Rationale: stopping a
    RUNNING experiment moves it back to DRAFT state, which is not counted by the free slot
    check (`{RUNNING, SCHEDULED, ENDED}`). A user could exploit this to recover the free
    slot mid-experiment and start a new one, bypassing the one-experiment limit. This is
    also consistent with the UI disabling the **Stop Experiment** button and the confirmation
    dialog warning the user the experiment cannot be stopped once started.
  - **All other operations** (CRUD, `isUserIncluded`, `/{id}/results`, `/health`) MUST
    remain accessible regardless of flag state and slot availability. Rationale: operators
    must be able to manage and view experiments even when the flag is off.
  - **Test**: Unit — verify `_schedule`, `_cancel`, `archive`, and `_end` return `403` (body
    contract verified in FR-009 unit test); verify `_start` returns `403` when global count ≥ 1; verify `_start` succeeds when
    count = 0 and duration ≤ 10 days; verify `_start` returns `400` when duration > 10
    days; verify `_start` sets end date to 10 days when omitted; verify CRUD/results/health
    remain accessible. Postman — confirm `403 FEATURE_DISABLED` and `400` responses on a
    running server.
  - **Auth ordering** (applies to all endpoints gated by this spec): authentication and
    site-permission checks MUST run before the feature-flag gate. An unauthenticated caller
    MUST receive `401`, not `403` — the gate must not leak feature state to callers who have
    not yet established identity.
    **Exception — `POST /api/v1/analytics/content/event`**: no dotCMS session auth. The
    configuration gate runs first — `503` immediately if the App is not configured, no
    payload inspection. `site_auth` validation runs only after the gate passes; invalid or
    missing token → `400`. Existing `400` behavior unchanged.
- **FR-002**: When `FEATURE_FLAG_EXPERIMENTS=true` **and** the Analytics App is configured for
  the site, all experiment endpoints MUST continue to function as they do today with no
  behavioral change, including start and scheduling operations. If the Analytics App is not
  configured, activation operations (`_start`, `_schedule`, `_abort`, `archive`) return `503`
  per FR-001.
  - **Test**: Unit — verify no gate fires and no `403`/`503` is returned when both conditions
    are met. No Postman or integration tests required — this is a regression guard only.
- **FR-002a**: `GET /api/v1/experiments/health` is always accessible regardless of
  `FEATURE_FLAG_EXPERIMENTS` state. It MUST return two pieces of information:
  1. The existing health state (unchanged): `OK`, `NOT_CONFIGURED`, or `CONFIGURATION_ERROR`.
  2. **New** — the experiment tier fields:
     - `tier`: `"limited"` when `FEATURE_FLAG_EXPERIMENTS=false` OR the Analytics App is not
       configured for the site; `"full"` only when both `FEATURE_FLAG_EXPERIMENTS=true` AND
       the Analytics App is configured.
     - `freeExperimentUsed`: `true` if any experiment globally exists in `{RUNNING, SCHEDULED,
       ENDED}` across all sites; `false` otherwise. Always present when `tier="limited"`,
       regardless of the trigger (flag or App config); omitted or null only when `tier="full"`.
  Additionally, when the Analytics App is not configured, the response MUST include a
  `warning` field regardless of flag state or slot availability:
  - `warning`: `"ANALYTICS_DISABLED"` — present whenever the Analytics App is not
    configured for the site; omitted otherwise.
  Example responses:

  *experiments=false, App configured, slot used:*
  ```json
  { "health": "OK", "tier": "limited", "freeExperimentUsed": true }
  ```
  *experiments=true, App not configured (misconfigured — warning present):*
  ```json
  { "health": "OK", "tier": "limited", "freeExperimentUsed": false,
    "warning": "ANALYTICS_DISABLED" }
  ```
  *experiments=false, App not configured, slot available:*
  ```json
  { "health": "OK", "tier": "limited", "freeExperimentUsed": false,
    "warning": "ANALYTICS_DISABLED" }
  ```
  *experiments=false, App not configured, slot used:*
  ```json
  { "health": "OK", "tier": "limited", "freeExperimentUsed": true,
    "warning": "ANALYTICS_DISABLED" }
  ```
  > The warning is always included when the App is not configured, regardless of flag state
  > or slot availability — it describes a system condition the operator must act on.
  > **Note**: The Experiments portlet UI calls this endpoint on load to drive button state:
  > - `tier="full"` → all buttons enabled normally.
  > - `tier="limited"` → **scheduling section**, **Stop Experiment**, and **archive** are
  >   always disabled with tooltip *"Upgrade your plan to unlock this feature. Contact
  >   dotCMS."* regardless of `freeExperimentUsed`.
  >   - `freeExperimentUsed=false` → **Start** button enabled. Creation form informational
  >     message, date picker restriction, and Start confirmation dialog are defined in
  >     US2 scenarios 1 and 2.
  >   - `freeExperimentUsed=true` → **Start** also disabled with the same upgrade tooltip.
  > - `warning="ANALYTICS_DISABLED"` → MUST display a dismissable warning banner: *"Analytics
  >   data collection is currently disabled. Experiments require Analytics to be enabled for
  >   full functionality. Please contact dotCMS to activate it."* Shown alongside the normal
  >   interface; does not block usage.
  > **`FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS` scope**: when `true` the health check evaluates
  > `dotContentAnalytics-config` (the Analytics App); when `false` (current default) it
  > evaluates the legacy `dotExperiments-config` path. The new tier fields (`tier`,
  > `freeExperimentUsed`, `warning`) apply only to the `true` branch. The `false` branch is
  > unchanged by this feature.
  - **Test — backend**: Unit — verify `tier`, `freeExperimentUsed`, and `warning` values in
    each state (flag=true+App configured, flag=false+App configured, flag=true+App missing,
    flag=false+App missing). Postman — verify response shape and field presence/absence on
    a running server.
  - **Test — frontend (Jest/Spectator)**:
    - Given `tier="limited"`: verify **archive** is disabled with upgrade tooltip regardless
      of `freeExperimentUsed`.
    - Given `tier="limited"` and `freeExperimentUsed=false`: verify **Start** is enabled and
      the scheduling section is disabled with upgrade tooltip.
    - Given `tier="limited"` and `freeExperimentUsed=true`: verify **Start** is disabled with
      upgrade tooltip.
    - Given `tier="limited"`: verify **Stop Experiment** is disabled with upgrade tooltip
      regardless of `freeExperimentUsed`.
    - Given `tier="limited"` and `freeExperimentUsed=false`: verify the creation form shows
      the limited-mode informational message; verify the date picker disables end dates beyond
      10 days from the selected start date; verify clicking **Start** opens the confirmation
      dialog; verify **Cancel** closes it without starting; verify **Continue** starts the
      experiment.
    - Given `warning="ANALYTICS_DISABLED"`: verify the dismissable warning banner is rendered
      with the expected message text.

**EventAnalyticsProxyResource — GET endpoints**

- **FR-003**: `GET /api/v1/analytics/{path}` (all read/query paths except `/health` and siteauth)
  MUST return `503 Service Unavailable` when the Analytics App is not configured for the
  site — this signals a configuration problem, not a permission failure. When the App is
  configured, requests are proxied to the analytics backend normally.
  - **Test**: Unit — verify `503` when App not configured; verify proxy proceeds when
    configured. Postman — confirm `503` on a running server when App is absent.
- **FR-003a**: `GET /api/v1/analytics/health` is currently a pass-through proxy — it forwards
  the request upstream to CAEM and returns whatever CAEM responds with. This feature changes
  that behavior unconditionally: dotCMS MUST always perform its own health evaluation (verify
  that the required Analytics App credentials are configured for the current site, then probe
  the analytics backend) and return one of three dotCMS-produced states — it MUST NOT pass
  through the raw upstream response for this path:
  - `OK` — required credentials are configured and the analytics backend is reachable.
  - `NOT_CONFIGURED` — no analytics configuration has been set up for the site.
  - `CONFIGURATION_ERROR` — credentials exist but are incomplete, or the backend is
    unreachable.

  The response body uses a single `health` field carrying the state string.
  Examples:

  *Backend reachable and configured:*
  ```json
  { "health": "OK" }
  ```
  *App not configured:*
  ```json
  { "health": "NOT_CONFIGURED" }
  ```
  *Credentials incomplete or backend unreachable:*
  ```json
  { "health": "CONFIGURATION_ERROR" }
  ```

  Auth: backend user required (`401` if unauthenticated); site READ permission required
  (`403 SITE_ACCESS_DENIED` if missing). Both checks run before the health evaluation.
  This endpoint is always accessible and never gated.

  > **Why this scope**: the Analytics portlet UI relies on these three states to decide
  > whether to render — a raw proxy response cannot serve that role. Follows the same
  > pattern as the experiments health endpoint (FR-002a).
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
  - **Test — backend**: Integration (mocked CAEM) — verify `OK` when credentials present and
    backend reachable; `NOT_CONFIGURED` when App absent; `CONFIGURATION_ERROR` when credentials
    incomplete or backend unreachable. Postman — verify response shape on a running server.
    Unit tests not applicable: the health check requires a real credential check and backend
    probe that cannot be meaningfully exercised without a mocked backend.
  - **Test — frontend (Jest/Spectator)**:
    - Given `NOT_CONFIGURED` response: verify the "not configured" error message is rendered
      directing the operator to contact Customer Success.
    - Given `CONFIGURATION_ERROR` response: verify the "configuration error" message is
      rendered directing the operator to check their Analytics configuration or contact
      dotCMS Support.
    - Given `OK` response: verify the analytics dashboard renders normally without any error
      message.
    - Health check service: verify it maps the three-state backend response (`OK`,
      `NOT_CONFIGURED`, `CONFIGURATION_ERROR`) correctly instead of using the legacy
      `entity.available` field.
- **FR-004**: `GET /api/v1/analytics/content/siteauth/generate/{siteId}` MUST always be
  accessible — it is never gated by this feature and MUST never return `403` or `503` from
  the feature-flag gate. Rationale: this token is required to configure the Analytics App
  itself; blocking it would make App setup impossible (chicken-and-egg).
  - **Test**: Unit — verify the endpoint is accessible regardless of flag state and App
    configuration. Postman — confirm the endpoint returns a token in all states.

**EventAnalyticsProxyResource — POST `/api/v1/analytics/content/event`**

- **Gate ordering**: this endpoint has no dotCMS session auth. The configuration gate MUST
  run first, before any payload inspection. The ordering is:
  configuration gate (App credentials check) → `site_auth` validation → `persistenceMode` check → forwarding.

- **FR-006**: The event ingest gate checks only whether the Analytics App has credentials
  configured for the site (`NOT_CONFIGURED` check) — it does not perform a full health probe:
  - If the Analytics App credentials are **absent** (`NOT_CONFIGURED`): MUST return
    `503 Service Unavailable` regardless of `FEATURE_FLAG_EXPERIMENTS` state. No call is
    made to the analytics backend.
  - If the Analytics App credentials are **present** (`OK` or `CONFIGURATION_ERROR`): MUST
    forward the entire event payload to the analytics backend as-is, for all event types,
    regardless of `FEATURE_FLAG_EXPERIMENTS` state. CAEM's response is returned as-is — if
    credentials are incomplete or the backend is unreachable, CAEM returns its own error and
    that is passed back to the caller. No filtering, no mutation.
  - **Test**: Unit — verify `503` when App not configured (flag=true and flag=false). Integration
    (mocked CAEM) — verify payload forwarded as-is and CAEM response returned when App
    configured; Postman cannot verify forwarding since the analytics backend is not reachable
    from the Postman test environment.

**Feature-disabled error response**

- **FR-009**: Every `403 FEATURE_DISABLED` returned by the experiment activation endpoints
  (`_start`, `_cancel`, `archive`, `_end`) MUST include a machine-readable error code
  `FEATURE_DISABLED` and a human-readable message in the standard dotCMS error response
  envelope. The event ingest endpoint never returns `403` — it returns `503` when the App is
  not configured and forwards when it is.
  - Example `403` response body:
    ```json
    { "errors": [{ "errorCode": "FEATURE_DISABLED", "message": "The Experiments feature is currently disabled. Please contact dotCMS to enable it." }] }
    ```
  - The `FEATURE_DISABLED` error code distinguishes feature-flag `403`s from other `403`s
    on the same endpoints (e.g. `SITE_ACCESS_DENIED` for permission failures).
  - **Test**: Unit — for each gated endpoint (`_start`, `_cancel`, `archive`, `_end`), verify
    that a flag-disabled `403` response body contains `errorCode: "FEATURE_DISABLED"` and a
    non-empty human-readable message field.
  - **Test**: Postman — for each flag-disabled `403` case, verify the response body contains
    `errorCode: "FEATURE_DISABLED"` and the expected human-readable message.
  > **Note — SDK follow-up (out of scope for this issue)**: the client-side analytics SDK
  > calls `POST /api/v1/analytics/content/event` directly from customer site browsers. When
  > the Analytics App is not configured the gate returns `503 Service Unavailable` — the SDK
  > should catch that response and degrade gracefully (e.g. log a warning rather than throwing
  > an unhandled error) so the `503` is visible in the network tab but does not surface as a
  > console error on the customer site. This SDK update is intentionally deferred because the
  > SDK is under active development; it should be tracked as a follow-up task.

**Flag state refresh**

- **FR-010**: `FEATURE_FLAG_EXPERIMENTS` live-toggle MUST be removed system-wide. Every
  consumer MUST read the flag once at startup and hold it for the instance lifetime — a
  restart is required for changes to take effect. Rationale: experiments is a paid feature;
  a restart enforces an intentional, operator-driven activation step.
  Analytics is no longer flag-controlled — it is driven by whether the Analytics App is
  configured for the site, which takes effect immediately.
  **Breaking change**: default MUST change from `true` → `false` system-wide. Each active
  customer MUST be verified before release to confirm their config explicitly sets `true`.
  - **Rollback safety**: deployment only *adds* explicit `=true` for verified active
    customers. A rollback restores the pre-deploy state for all categories: active customers
    keep their `=true`, others revert to the default, explicit `=false` entries are
    untouched. No config cleanup needed.
  - **Test suite audit (required before the default flip ships)**: audit all existing unit
    tests in `:dotcms-core` and integration tests in `:dotcms-integration` that exercise
    experiment functionality. Any test that relies on the flag defaulting to `true` without
    explicitly setting it MUST be updated to initialize `FEATURE_FLAG_EXPERIMENTS=true` in
    its setup — otherwise CI breaks the moment the default changes.
  - **Scope constraint**: only the live-toggle for `FEATURE_FLAG_EXPERIMENTS` MUST be removed.
    The live-toggle behavior for `FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS` and
    `ENABLE_EXPERIMENTS_AUTO_JS_INJECTION` MUST remain intact — those flags are not in scope
    for this feature.
  - **Test**: Integration — toggle the flag without restarting; verify gate behavior does
    not change.
  - **Test**: Unit — verify that when `FEATURE_FLAG_EXPERIMENTS` is absent from configuration
    the resolved value is `false` (i.e. the new default applies correctly).

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
  and analytics read endpoints return `503 Service Unavailable`.

- **Site Auth Validation**: The configuration gate runs first on
  `POST /api/v1/analytics/content/event` — if the Analytics App is not configured the
  endpoint returns `503` immediately with no payload inspection. `site_auth` validation runs
  only after the gate passes; an invalid or missing `site_auth` returns `400 Bad Request`.
  **Implementation note**: `EventAnalyticsProxyResource.proxyEventRequest()` currently
  validates `site_auth` at lines 200–211 before any App-config check — this order must be
  inverted as part of this feature.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: When `FEATURE_FLAG_EXPERIMENTS=false` and the Analytics App is configured
  (limited experiment mode): `_schedule`, `_cancel`, and `archive` return `403 FEATURE_DISABLED`;
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
  activation operations return `503`. Basic management operations (CRUD, health) remain
  accessible; experiment results are not available.
- **SC-004**: A change to `FEATURE_FLAG_EXPERIMENTS` takes effect only after a server restart
  — live toggling without a restart is intentionally not supported. Changes to the Analytics
  App configuration take effect immediately without a restart.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The analytics event ingest path and the experiment management
  REST layer are part of the modern analytics/experiments feature area (`com.dotcms.*`).
  However, FR-010 requires removing the live-toggle system-wide across **all** consumers of
  `FEATURE_FLAG_EXPERIMENTS` — the plan phase must locate every reader of this flag and
  convert it to read-once-at-startup, including any consumers in the legacy
  `com.dotmarketing.*` surface (e.g. page-rendering paths that gate experiment script
  injection). The `EventLogWebInterceptor` (the original Jitsu proxy path) is **explicitly
  out of scope** — it is being removed and must not be modified by this work.
- **`FEATURE_FLAG_CONTENT_ANALYTICS` disposition**: This flag is removed from the new gate
  logic introduced by this feature — analytics availability is now determined by Analytics App
  configuration, not by this flag. However, `FEATURE_FLAG_CONTENT_ANALYTICS` MUST remain in
  `FeatureFlagName.java` and `AnalyticsTrackWebInterceptor` MUST NOT be modified by this work
  — the interceptor continues to use this flag to gate page-view event tracking as it does
  today. Migrating or removing `AnalyticsTrackWebInterceptor` is deferred to a future cleanup
  task. The new code introduced by this feature MUST NOT reference or depend on this flag.
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

- `FEATURE_FLAG_EXPERIMENTS` currently defaults to `true` in the codebase. This default
  MUST be changed to `false` as part of this release — see pre-release step below.
- The siteauth token is stateless and does not carry flag or App configuration state — the
  gate is enforced at the endpoint level, not embedded in the token.
- The gates described here are dotCMS-side pre-flight checks and are not a replacement for
  any existing validations in the analytics pipeline.
- No non-UI consumers of `GET /api/v1/analytics/health` are known; the response-shape change
  introduced by FR-003a affects only the Analytics portlet, which is updated as part of this
  feature.

## Pre-Release Steps *(required before shipping)*

> **Breaking change**: the default value of `FEATURE_FLAG_EXPERIMENTS` is flipping from
> `true` to `false`. Any customer relying on the default will have experiments silently
> disabled after this release.

1. **Audit active customers** — identify every customer currently using experiments (i.e.,
   any instance where experiments have been started or are running).
2. **Verify explicit flag** — for each active customer, confirm that `FEATURE_FLAG_EXPERIMENTS=true`
   is explicitly set in their configuration and is **not** relying on the default value.
3. **Set explicit value where missing** — for any active customer without an explicit entry,
   add `FEATURE_FLAG_EXPERIMENTS=true` to their configuration before the release is deployed.
4. **Change the codebase default** — flip the default from `true` to `false` as part of the
   implementation (FR-010).

Steps 1–3 MUST be completed and verified before step 4 ships to any environment that hosts
active customers.
