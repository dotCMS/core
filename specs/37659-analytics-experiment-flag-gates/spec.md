# Feature Specification: Feature-Flag Gates for Analytics and Experiment Endpoints

**Feature Branch**: `37659-analytics-experiment-flag-gates`

**Created**: 2026-09-21

**Status**: Draft

**Type**: Task

**Input**: User description: "Add feature-flag gates to EventAnalyticsProxyResource and ExperimentsResource based on FEATURE_FLAG_CONTENT_ANALYTICS and FEATURE_FLAG_EXPERIMENTS — https://github.com/dotCMS/core/issues/37659"

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Both Features Enabled (Priority: P1)

An administrator has both `FEATURE_FLAG_CONTENT_ANALYTICS` and `FEATURE_FLAG_EXPERIMENTS`
enabled. All analytics and experiment capabilities are fully accessible with no restrictions.

**Why this priority**: This is the fully-enabled "happy path" that active customers who have both
flags explicitly set to `true` must not see disrupted. Confirming no regression here protects
every verified customer currently using analytics or experiments.

**Independent Test**: **Unit tests** cover the gate pass-through logic and payload integrity:
verify that neither gate blocks the request when both flags are ON, and that the event payload
reaches the forwarding layer unmodified — no `event_type` filtering applied, no events
discarded.
**Integration tests with a mocked analytics backend** verify the forwarding scenarios
end-to-end — the analytics backend is not available in the test environment, so a mock is
required to confirm that forwarding succeeds and no payload is blocked or modified. Postman
tests cannot be used here: success cases require confirming that the event was actually
forwarded to and accepted by the analytics backend, and that backend is not reachable from
the Postman test environment.

**Acceptance Scenarios**:

1. **Given** both flags are ON, **When** an administrator calls any experiment endpoint
   (`/api/v1/experiments/**`), **Then** the endpoint responds normally with no 403.
2. **Given** both flags are ON, **When** a client POSTs any event type (including `pageview`,
   `content_click`, etc.) with or without experiment context to
   `POST /api/v1/analytics/content/event`, **Then** the payload is forwarded to the analytics
   backend as-is and a success response is returned.
3. **Given** both flags are ON, **When** an administrator calls any analytics read endpoint
   (`GET /api/v1/analytics/**`), **Then** the request is proxied to the analytics backend
   normally.
4. **Given** both flags are ON, **When** an administrator calls the site auth generation
   endpoint, **Then** a site auth token is returned successfully.

---

### User Story 2 — Only Experiments Enabled (Priority: P1)

An administrator has `FEATURE_FLAG_EXPERIMENTS=true` and `FEATURE_FLAG_CONTENT_ANALYTICS=false`.
Experiments are running and need to track pageview events, but the broader content analytics
feature is not licensed or activated.

**Why this priority**: This is a supported mixed state — experiments were historically designed
to operate independently of the full analytics feature. Breaking this case would silently stop
experiment data collection.

**Independent Test**: Split across three test types. **Unit tests** cover the per-event
filtering logic: verify that `pageview` events are forwarded, that non-`pageview` events are
added to `discardedEvents` with `code: "FEATURE_DISABLED"`, that a mixed batch returns `202
PARTIAL_SUCCESS`, and that an all-non-`pageview` batch returns `400 ERROR` with all events
discarded and `success: 0`. **Postman** covers scenario 5 (analytics read endpoints return
`403 FEATURE_DISABLED`) and scenario 2b (all-non-`pageview` batch returns `400 ERROR`) — no
analytics backend contact needed for either. **Integration tests with a mocked analytics
backend** cover scenario 1 (all-`pageview` batch forwarded successfully) and scenario 2
(mixed batch: `pageview` events forwarded, non-`pageview` in `discardedEvents`, `202
PARTIAL_SUCCESS`) — Postman cannot verify forwarding because the analytics backend is not
reachable from the Postman test environment. Scenarios 3 and 4 (experiment endpoints
accessible, siteauth returns a token) verify gate-only behavior and do not require a mocked
backend.

**Acceptance Scenarios**:

1. **Given** only experiments is ON, **When** a client POSTs a `pageview` event
   (with or without experiment context in `context.experiments`) to
   `POST /api/v1/analytics/content/event`, **Then** the event is forwarded to the analytics
   backend and a success response is returned.
2. **Given** only experiments is ON, **When** a client POSTs a batch containing non-`pageview`
   events alongside `pageview` events, **Then** the system returns `202` with the `pageview`
   events forwarded and the non-`pageview` events listed in `discardedEvents` with
   `code: "FEATURE_DISABLED"`.
2b. **Given** only experiments is ON, **When** a client POSTs a batch where ALL events are
   non-`pageview`, **Then** the system returns `400` with `status: "ERROR"`, `success: 0`,
   and all events in `discardedEvents` with `code: "FEATURE_DISABLED"` — nothing is forwarded.
3. **Given** only experiments is ON, **When** an administrator calls any experiment endpoint
   (including start and scheduling operations), **Then** the endpoint responds normally — all
   experiment operations are fully accessible when `FEATURE_FLAG_EXPERIMENTS=true`.
4. **Given** only experiments is ON, **When** an administrator requests a site auth token,
   **Then** a site auth token is returned (required for authenticating event sends).
5. **Given** only experiments is ON, **When** an administrator calls any analytics read/query
   endpoint (`GET /api/v1/analytics/{path}`), **Then** the system returns `403` — **except**
   the siteauth endpoint (`/api/v1/analytics/content/siteauth/generate/{siteId}`), which
   remains accessible as covered by scenario 4 above.

---

### User Story 3 — Both Features Disabled (Priority: P2)

An administrator has both flags set to `false`. Analytics is fully off and experiments cannot
be started or scheduled.

**Why this priority**: Blocking analytics event collection and experiment activation when
neither feature is licensed prevents data from flowing to the analytics backend unnecessarily.

**Independent Test**: **Unit tests** cover each gate in isolation — experiment start/scheduling
operations return `403 FEATURE_DISABLED`; experiment CRUD, results, health, and other lifecycle
operations remain accessible; analytics read endpoints and siteauth return `403 FEATURE_DISABLED`;
the event ingest endpoint returns `400` with `status: "ERROR"`, all events in `discardedEvents`
(`code: "FEATURE_DISABLED"`), and `success: 0` — no backend contact required for any of these.
**Postman** confirms `403 FEATURE_DISABLED` on admin endpoints and `400 ERROR` with all events
in `discardedEvents` on ingest.

**Acceptance Scenarios**:

1. **Given** both flags are OFF, **When** a client POSTs any event batch to
   `POST /api/v1/analytics/content/event`, **Then** the system returns `400` with
   `status: "ERROR"`, all events in `discardedEvents` (`code: "FEATURE_DISABLED"`), and no
   event reaches the analytics backend.
2. **Given** both flags are OFF, **When** an administrator calls an experiment `_start` or
   scheduling operation, **Then** the system returns `403 FEATURE_DISABLED`. All other
   experiment operations (CRUD, results, `_end`, `_cancel`, `isUserIncluded`, `health`) remain
   accessible.
3. **Given** both flags are OFF, **When** an administrator calls any analytics read endpoint
   or the siteauth endpoint, **Then** the system returns `403 FEATURE_DISABLED`.

---

### Edge Cases

- What if `event_type` is missing from the payload when analytics=OFF, experiments=ON? The
  event is forwarded as-is. `event_type` is a required field validated by the analytics
  backend — rejecting it here would produce a misleading `403` instead of the backend's own
  validation error, obscuring the real cause for the caller.

## Requirements *(mandatory)*

### Contract Changes

This feature introduces new `403` response codes on existing admin endpoints and a new partial
ingest response model on the event ingest endpoint. No new endpoints are added, no database
schema changes are made, and no Elasticsearch/OpenSearch index mapping changes are introduced.

**New `403` response paths on admin endpoints (breaking for callers that do not handle this status code):**

- `POST /api/v1/experiments/{id}/_start` and scheduling operations — these operations now
  return `403 FEATURE_DISABLED` when `FEATURE_FLAG_EXPERIMENTS=false`. All other experiment
  endpoints (CRUD, results, `_end`, `_cancel`, `isUserIncluded`, `health`) remain accessible
  regardless of flag state.
- `GET /api/v1/analytics/{path}` (including `/health`) — new `403 FEATURE_DISABLED` when
  `FEATURE_FLAG_CONTENT_ANALYTICS=false`. Previously this path always proxied upstream.
- `GET /api/v1/analytics/content/siteauth/generate/{siteId}` — new `403 FEATURE_DISABLED`
  when both flags are `false`. Previously this endpoint always returned a token.

**New partial ingest response on `POST /api/v1/analytics/content/event`:**

This endpoint never returns `403` for flag-related reasons. Instead it always returns `202`
and reports per-event outcomes. Events that cannot be forwarded due to flag state are listed
in a `discardedEvents` array alongside the CAEM response fields. The response shape mirrors
CAEM's existing partial-success contract and adds a `discarded` count and `discardedEvents`
array:

```json
{
  "status": "SUCCESS | PARTIAL_SUCCESS | ERROR",
  "success": 2,
  "failed": 1,
  "discarded": 2,
  "errors": [
    { "eventIndex": 2, "field": "events[2].local_time",
      "code": "INVALID_DATE_FORMAT", "message": "..." }
  ],
  "discardedEvents": [
    { "eventIndex": 1, "event_type": "content_click", "code": "FEATURE_DISABLED",
      "message": "Content Analytics is disabled. Only pageview events are forwarded." },
    { "eventIndex": 3, "event_type": "content_impression", "code": "FEATURE_DISABLED",
      "message": "Content Analytics is disabled. Only pageview events are forwarded." }
  ]
}
```

- `errors` — CAEM validation errors; indexes are re-mapped to the original batch.
- `discardedEvents` — events filtered by the dotCMS gate before reaching CAEM; always uses original batch indexes.
- `discarded` — count of discarded events (`discardedEvents.length`).
- When no events are forwarded (both flags OFF), `success` and `failed` are `0`; all events appear in `discardedEvents`. The HTTP status is `400` and `status` is `ERROR`, mirroring CAEM's own behavior when `successCount == 0`.
- `status` is promoted to `PARTIAL_SUCCESS` whenever `discarded > 0` but `success > 0`, even if CAEM returned `SUCCESS`. If `success == 0` (all events either gate-discarded or CAEM-rejected), the status is `ERROR` and HTTP is `400`.

### Functional Requirements

**ExperimentsResource — `/api/v1/experiments/**`**

- **FR-001**: When `FEATURE_FLAG_EXPERIMENTS=false`, the system MUST return `403 FEATURE_DISABLED`
  only on experiment start and scheduling operations. All other experiment endpoints — CRUD,
  `_end`, `_cancel`, `isUserIncluded`, `/{id}/results`, and `/health` — MUST remain accessible
  regardless of flag state. Rationale: operators must be able to manage, view, and stop
  experiments even when the flag is off; only activating new runs is restricted.
  - **Auth ordering** (applies to all endpoints gated by this spec): authentication and
    site-permission checks MUST run before the feature-flag gate. An unauthenticated caller
    MUST receive `401`, not `403` — the gate must not leak feature state to callers who have
    not yet established identity.
    **Exception — `POST /api/v1/analytics/content/event`**: this endpoint does not use dotCMS
    user session authentication. It validates a `site_auth` token present in the request
    payload; an invalid or missing `site_auth` returns `400` (malformed request), not `401`.
    This `400` is preserved as-is — the feature-flag gate runs only after `site_auth`
    validation passes, and the existing `400` behavior is not changed by this feature.
- **FR-002**: When `FEATURE_FLAG_EXPERIMENTS=true`, all experiment endpoints MUST continue to
  function as they do today with no behavioral change, including start and scheduling operations.
- **FR-002a**: `GET /api/v1/experiments/health` already produces one of three states via
  existing health check logic — this behavior is not new and requires no changes. The health
  endpoint is accessible regardless of `FEATURE_FLAG_EXPERIMENTS` state (it is not gated):
  - `OK` — required credentials are configured and the analytics backend is reachable.
  - `NOT_CONFIGURED` — no analytics configuration has been set up for the site.
  - `CONFIGURATION_ERROR` — configuration exists but is incomplete (missing required
    credentials) or the analytics backend is unreachable.
  > **Note**: The Experiments portlet UI calls this endpoint to decide whether to render the
  > experiments interface. A response of `OK` enables the UI; `NOT_CONFIGURED` or
  > `CONFIGURATION_ERROR` causes the UI to display an error message to the operator instead.

**EventAnalyticsProxyResource — GET endpoints**

- **FR-003**: `GET /api/v1/analytics/{path}` (all read/query paths except siteauth)
  MUST return `403` when `FEATURE_FLAG_CONTENT_ANALYTICS=false`.
- **FR-003a**: `GET /api/v1/analytics/health` is currently a pass-through proxy — it forwards
  the request upstream to CAEM and returns whatever CAEM responds with. This feature changes
  that behavior when `FEATURE_FLAG_CONTENT_ANALYTICS=true`: dotCMS MUST perform its own
  health evaluation (verify that the required analytics credentials are configured for the
  current site, then probe the analytics backend) and return one of three dotCMS-produced
  states — it MUST NOT pass through the raw upstream response for this path:
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
  health endpoint (FR-002a). When `FEATURE_FLAG_CONTENT_ANALYTICS=false`, this endpoint
  returns `403 FEATURE_DISABLED` per FR-003.
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
- **FR-004**: `GET /api/v1/analytics/content/siteauth/generate/{siteId}` MUST return `403`
  only when BOTH `FEATURE_FLAG_CONTENT_ANALYTICS=false` AND `FEATURE_FLAG_EXPERIMENTS=false`.
- **FR-005**: The siteauth endpoint MUST be accessible whenever EITHER
  `FEATURE_FLAG_CONTENT_ANALYTICS=true` OR `FEATURE_FLAG_EXPERIMENTS=true`, because both
  features require a valid site auth token to authenticate event sends.

**EventAnalyticsProxyResource — POST `/api/v1/analytics/content/event`**

- **Gate ordering**: the feature-flag gate introduced by this feature MUST run before the
  existing `persistenceMode=readonly` check. The gate evaluates flag state and builds the
  `discardedEvents` list first; `persistenceMode` is checked only for the events that remain
  after gate filtering. The ordering is:
  auth → feature-flag gate (this feature) → persistenceMode check → forwarding.

- **FR-006**: When both flags are `false`: all events in the batch are discarded. The endpoint
  MUST return `400` with `status: "ERROR"`, `success: 0`, `failed: 0`, and all events listed
  in `discardedEvents` with `code: "FEATURE_DISABLED"`. No call is made to the analytics
  backend. This mirrors CAEM's own `ERROR` / `400` response when `successCount == 0`.
- **FR-007**: When both flags are `true`: the system MUST forward the entire payload to the
  analytics backend as-is. CAEM's response is returned as-is.
- **FR-008**: When `FEATURE_FLAG_CONTENT_ANALYTICS=false` and `FEATURE_FLAG_EXPERIMENTS=true`:
  the system MUST inspect the `events` array and apply per-event filtering:
  - Events where `event_type = "pageview"` (or `event_type` is absent) MUST be forwarded to
    the analytics backend.
  - Events where `event_type` is present with any other value MUST be added to `discardedEvents`
    with `code: "FEATURE_DISABLED"` and NOT forwarded.
  The forwarded sub-batch is sent to CAEM (re-indexed from 0). dotCMS MUST re-map CAEM's
  `eventIndex` values in the response back to the original batch indexes. The final response
  combines CAEM's `status`, `success`, `failed`, and `errors` (with re-mapped indexes) with
  dotCMS's `discarded` count and `discardedEvents` array.
  If `event_type` is absent on an event, that event is forwarded without being discarded —
  `event_type` is enforced by the analytics backend itself, which will return its own
  validation error through CAEM's `errors` array.

**Feature-disabled error response**

- **FR-009**: Every `403 FEATURE_DISABLED` returned by the admin endpoints (experiments and
  analytics GET/siteauth) MUST include a machine-readable error code `FEATURE_DISABLED` and
  a human-readable message in the standard dotCMS error response envelope. This does not apply
  to the event ingest endpoint, which uses `code: "FEATURE_DISABLED"` inside `discardedEvents`
  per the ingest response model (Contract Changes section).
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

- **FR-010**: Both `FEATURE_FLAG_EXPERIMENTS` and `FEATURE_FLAG_CONTENT_ANALYTICS` currently
  support runtime changes without a server restart — an administrator can toggle them in the
  dotCMS configuration system and all consumers react immediately. This live-toggle capability
  MUST be removed system-wide for both flags. Every consumer of these flags — the gate
  introduced by this feature and all existing consumers — MUST read each flag value exactly
  once at server startup and hold that value for the entire lifetime of the running instance.
  A runtime configuration change MUST NOT alter any consumer's behavior without a server
  restart, regardless of the mechanism used to make that change. This is a deliberate design
  choice: these are paid features, and activation or deactivation MUST require an explicit,
  intentional restart by an operator.
  **Breaking change**: the default value for both `FEATURE_FLAG_EXPERIMENTS` and
  `FEATURE_FLAG_CONTENT_ANALYTICS` MUST change from `true` to `false` **system-wide** —
  for every consumer of these flags across the system, not only the gate introduced by this
  feature. This ensures a new deployment without explicit flag configuration is fully
  disabled: no REST access, no analytics event interception, no experiment serving. Changing
  the default only in the gate would leave other internal consumers active while the API
  surface is blocked — an inconsistent state that does not represent "feature disabled."
  Because the current customer footprint for these features is small, each active customer
  MUST be verified before release to confirm their configuration explicitly sets the relevant
  flag(s) to `true`.
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

- **Feature Flag**: A named boolean configuration property. Currently each flag supports
  runtime changes without a restart; this live-toggle capability MUST be removed system-wide
  as part of this feature (FR-010). After this change, every consumer of these flags reads
  the value once at server startup and holds it for the lifetime of the instance — a restart
  is required for any change to take effect.
  Relevant flags: `FEATURE_FLAG_CONTENT_ANALYTICS`, `FEATURE_FLAG_EXPERIMENTS`.
- **Event Payload**: The JSON body sent to `POST /api/v1/analytics/content/event`. Has the
  shape `{"context": {...}, "events": [...]}`. The `context` object is shared across all
  events in the batch and contains at minimum `site_auth` and optionally `experiments`. The
  `events` array holds one or more event objects, each carrying its own `event_type` field
  (e.g., `"pageview"`, `"content_click"`). Gate logic that inspects `event_type` operates
  per-event within the array; `context.experiments` is forwarded as-is and never modified
  by the gate.
- **Site Auth Token**: A credential generated by the siteauth endpoint and required by the
  client-side JS snippet to authenticate event sends to the analytics backend. Needed by both
  the analytics feature and the experiments feature.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: When `FEATURE_FLAG_EXPERIMENTS=false`, experiment start and scheduling operations
  return `403 FEATURE_DISABLED`; all other experiment operations (CRUD, results, health,
  lifecycle management) remain accessible.
- **SC-002**: When `FEATURE_FLAG_CONTENT_ANALYTICS=false` and `FEATURE_FLAG_EXPERIMENTS=true`,
  only `pageview` events are forwarded to the analytics backend; all other event types are
  discarded and appear in `discardedEvents`.
- **SC-003**: When both flags are `true`, no existing analytics or experiment functionality is
  disrupted — all requests succeed at the same rate as before this change.
- **SC-004**: When both flags are `false`, no data of any kind reaches the analytics backend
  through these endpoints.
- **SC-005**: A flag state change for the gate enforcement takes effect only after a server
  restart — live toggling without a restart is intentionally not supported for these gates.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The analytics event ingest path and the experiment management
  REST layer. Both are part of the modern analytics/experiments feature area (`com.dotcms.*`),
  not the legacy `com.dotmarketing.*` surface. The `EventLogWebInterceptor` (the original
  Jitsu proxy path) is **explicitly out of scope** — it is being removed and must not be
  modified by this work.
- **Backward-compatibility expectations**: When both flags are `true` (the fully-enabled state
  that existing customers have), behavior must be identical to today — no requests that
  currently succeed may be broken. The gate enforcement is additive: it restricts disabled
  states, not the enabled state.
  **Accepted exception — `GET /api/v1/analytics/health`**: the response shape of this
  endpoint changes even when the flag is `true` (see FR-003a). The Analytics portlet frontend
  MUST be updated as part of this feature to consume the new response format.
- **Known related decisions**: Both flags currently support live toggling in other areas of the
  system. This feature must disable that live-toggle behavior for the gate enforcement so that
  activating or deactivating these paid features requires a deliberate restart. The plan phase
  will consult `dotCMS/platform-adrs` for any ADRs governing feature-flag architecture.

## Assumptions

- Both `FEATURE_FLAG_EXPERIMENTS` and `FEATURE_FLAG_CONTENT_ANALYTICS` currently default to
  `true` in the codebase when no explicit value is set in configuration. The gate enforcement
  introduced by this feature must account for this: existing deployments that do not explicitly
  set these flags are currently running with both features enabled, and the gate behavior must
  not silently change that.
- The client-side JS snippet may include `context.experiments` unconditionally regardless of
  server flag state — the server must handle this gracefully (strip, not reject) when
  experiments are off but analytics is on.
- The siteauth token is stateless and does not itself carry feature-flag state — the gate is
  enforced at the endpoint level, not embedded in the token.
- The gates described here are dotCMS-side pre-flight checks and are not a replacement for
  any existing validations in the analytics pipeline.
- No non-UI consumers of `GET /api/v1/analytics/health` are known; the response-shape change
  introduced by FR-003a affects only the Analytics portlet, which is updated as part of this
  feature.
