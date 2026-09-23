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
reaches the forwarding layer unmodified — no stripping, no `event_type` filtering applied.
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
   `content_click`, or custom types) with or without experiment context to
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

**Independent Test**: Split across three test types. **Unit tests** cover the `event_type`
filtering logic: verify that `pageview` passes the gate, that any other present `event_type`
value returns `403`, and that an absent `event_type` is forwarded without rejection.
**Postman** covers the `403` cases (scenarios 2 and 5: non-`pageview` events rejected,
analytics read endpoints blocked) — no analytics backend contact needed. **Integration tests
with a mocked analytics backend** cover scenario 1 (`pageview` events forwarded) — Postman
cannot verify forwarding because the analytics backend is not reachable from the Postman test
environment; only a mocked backend can confirm the event is actually delivered. Scenarios 3
and 4 (experiment endpoints accessible, siteauth returns a token) verify gate-only behavior
and do not require a mocked backend.

**Acceptance Scenarios**:

1. **Given** only experiments is ON, **When** a client POSTs a `pageview` event
   (with or without experiment context in `context.experiments`) to
   `POST /api/v1/analytics/content/event`, **Then** the event is forwarded to the analytics
   backend and a success response is returned.
2. **Given** only experiments is ON, **When** a client POSTs any non-`pageview` event
   (e.g., `content_click`) to `POST /api/v1/analytics/content/event`, **Then** the system
   returns `403` and the event is not forwarded.
3. **Given** only experiments is ON, **When** an administrator calls any experiment endpoint,
   **Then** the endpoint responds normally.
4. **Given** only experiments is ON, **When** an administrator requests a site auth token,
   **Then** a site auth token is returned (required for authenticating event sends).
5. **Given** only experiments is ON, **When** an administrator calls any analytics read/query
   endpoint (`GET /api/v1/analytics/{path}`), **Then** the system returns `403` — **except**
   the siteauth endpoint (`/api/v1/analytics/content/siteauth/generate/{siteId}`), which
   remains accessible as covered by scenario 4 above.

---

### User Story 3 — Only Analytics Enabled (Priority: P2)

An administrator has `FEATURE_FLAG_CONTENT_ANALYTICS=true` and `FEATURE_FLAG_EXPERIMENTS=false`.
Content analytics is collecting events, but the A/B experimentation feature is disabled.

**Why this priority**: Analytics-only deployments must still collect clean event data. The risk
here is that client-side code might still inject experiment context into event payloads — that
must be stripped rather than dropped entirely, so analytics data is not lost.

**Independent Test**: Split across three test types. **Unit tests** cover the
`context.experiments` stripping logic: verify that the field is removed from the payload
before forwarding when present, and that the payload is otherwise unchanged. **Postman**
covers scenario 3 (experiment endpoints return `403`) — no backend contact needed.
**Integration tests with a mocked analytics backend** cover scenarios 1, 2, and 4 — Postman
cannot verify these because the analytics backend is not reachable from the Postman test
environment; a mocked backend is the only way to confirm that events are forwarded and that
`context.experiments` is absent from the payload the mock receives.

**Acceptance Scenarios**:

1. **Given** only analytics is ON, **When** a client POSTs any event without experiment context
   to `POST /api/v1/analytics/content/event`, **Then** the event is forwarded as-is and a
   success response is returned.
2. **Given** only analytics is ON, **When** a client POSTs an event that includes
   `context.experiments` in the payload, **Then** the `context.experiments` field is removed
   from the payload before forwarding, the event is still sent, and a success response is
   returned.
3. **Given** only analytics is ON, **When** an administrator calls any experiment endpoint
   (`/api/v1/experiments/**`), **Then** the system returns `403`.
4. **Given** only analytics is ON, **When** an administrator calls analytics read endpoints or
   the siteauth endpoint, **Then** the requests succeed normally.

---

### User Story 4 — Both Features Disabled (Priority: P2)

An administrator has both flags set to `false`. Neither analytics nor experiments are active.

**Why this priority**: Blocking both endpoints when neither feature is licensed prevents data
from flowing to the analytics backend unnecessarily and closes the surface area for
unauthenticated or unexpected calls.

**Independent Test**: **Unit tests** cover each gate conditional in isolation: verify that
experiments endpoints, analytics read endpoints, the siteauth endpoint, and the event ingest
endpoint each return `403` for the both-flags-OFF combination — no backend contact required.
**Postman** complements the unit tests with end-to-end `403` verification, confirming the
feature-disabled message appears in the expected error envelope on a running server.

**Acceptance Scenarios**:

1. **Given** both flags are OFF, **When** a client POSTs any event to
   `POST /api/v1/analytics/content/event`, **Then** the system returns `403` and no event
   reaches the analytics backend.
2. **Given** both flags are OFF, **When** an administrator calls any experiment endpoint,
   **Then** the system returns `403`.
3. **Given** both flags are OFF, **When** an administrator calls any analytics read endpoint
   or the siteauth endpoint, **Then** the system returns `403`.

---

### Edge Cases

- What happens if `context.experiments` is present but empty (null, empty object, empty array)?
  Treated the same as absent — no experiment context to strip or validate.
- What if `event_type` is missing from the payload when analytics=OFF, experiments=ON? The
  event is forwarded as-is. `event_type` is a required field validated by the analytics
  backend — rejecting it here would produce a misleading `403` instead of the backend's own
  validation error, obscuring the real cause for the caller.

## Requirements *(mandatory)*

### Contract Changes

This feature introduces new `403` response codes on existing endpoints and one silent payload
mutation. No new endpoints are added, no database schema changes are made, and no
Elasticsearch/OpenSearch index mapping changes are introduced.

**New `403` response paths (breaking for callers that do not handle this status code):**

- `ALL /api/v1/experiments/**` — all operations now return `403` when
  `FEATURE_FLAG_EXPERIMENTS=false`. Previously these endpoints never returned `403` due to a
  disabled feature flag.
- `POST /api/v1/analytics/content/event` — new `403` when both flags are `false`, and a
  second new `403` when `FEATURE_FLAG_CONTENT_ANALYTICS=false` and
  `FEATURE_FLAG_EXPERIMENTS=true` for any event where `event_type` is present and not
  `"pageview"`.
- `GET /api/v1/analytics/{path}` (including `/health`) — new `403` when
  `FEATURE_FLAG_CONTENT_ANALYTICS=false`. Previously this path always proxied upstream.
- `GET /api/v1/analytics/content/siteauth/generate/{siteId}` — new `403` when both flags
  are `false`. Previously this endpoint always returned a token.

**Payload mutation (observable by the analytics backend, not by the caller):**

- `POST /api/v1/analytics/content/event` — when `FEATURE_FLAG_CONTENT_ANALYTICS=true` and
  `FEATURE_FLAG_EXPERIMENTS=false`, the `context.experiments` field is silently removed from
  the forwarded payload. The caller receives a success response; only the analytics backend
  sees the stripped payload.

### Functional Requirements

**ExperimentsResource — `/api/v1/experiments/**`**

- **FR-001**: The system MUST return `403` on all experiment endpoints when
  `FEATURE_FLAG_EXPERIMENTS=false`, regardless of the analytics flag state. This includes
  CRUD operations, lifecycle actions (`_start`, `_end`, `_cancel`), `isUserIncluded`,
  `/{id}/results`, and `/health`.
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
  function as they do today with no behavioral change.
- **FR-002a**: `GET /api/v1/experiments/health` already produces one of three states via
  existing health check logic — this behavior is not new and requires no changes:
  - `OK` — required credentials are configured and the analytics backend is reachable.
  - `NOT_CONFIGURED` — no analytics configuration has been set up for the site.
  - `CONFIGURATION_ERROR` — configuration exists but is incomplete (missing required
    credentials) or the analytics backend is unreachable.
  **What is new**: when `FEATURE_FLAG_EXPERIMENTS=false`, this endpoint MUST return `403`
  per FR-001 — the gate on top of the existing health check is the only addition this
  feature makes to this endpoint.
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
  existing `persistenceMode=readonly` check. If the flags gate rejects the request with
  `403`, the `persistenceMode` check MUST NOT be evaluated. The ordering is:
  auth → feature-flag gate (this feature) → persistenceMode check → forwarding.

- **FR-006**: The event ingest endpoint MUST return `403` when both flags are `false`.
- **FR-007**: When `FEATURE_FLAG_CONTENT_ANALYTICS=true` and `FEATURE_FLAG_EXPERIMENTS=false`:
  the system MUST strip `context.experiments` from the top-level `context` object before
  forwarding the request, and MUST still forward the request (not drop it). Stripping is
  top-level only — `context.experiments` is shared across all events in the batch and appears
  once in the payload, not per-event. Stripping is silent — no error returned.
- **FR-008**: When both flags are `true`: the system MUST forward the event payload to the
  analytics backend as-is, regardless of event type or the presence of experiment context.
- **FR-009**: When `FEATURE_FLAG_CONTENT_ANALYTICS=false` and `FEATURE_FLAG_EXPERIMENTS=true`:
  the system MUST inspect the `events` array in the payload and apply the following
  batch-level rule: if **any** event in the batch has an `event_type` that is present with a
  value other than `"pageview"`, the **entire request** MUST be rejected with `403` — no
  events from the batch are forwarded. If all events in the batch have `event_type =
  "pageview"` (or have no `event_type` field), the request is forwarded as-is.
  If `event_type` is absent on an event, that event does not trigger rejection — `event_type`
  is a required field enforced by the analytics backend itself, which will return its own
  validation error. Rejecting an absent `event_type` at this layer would produce a misleading
  `403` (feature disabled) instead of the backend's precise validation response.
  **Batch semantics rationale**: the gate treats each request as an atomic unit — it either
  passes or fails in full. Partial forwarding (dropping offending events and forwarding the
  rest) would silently discard data and make debugging client-side issues significantly harder.

**Feature-disabled error response**

- **FR-010**: Every `403` returned because a feature flag is `false` MUST include a
  machine-readable error code `FEATURE_DISABLED` and a human-readable message in the response
  body that states which feature is disabled and directs the operator to contact dotCMS.
  - Example response body:
    ```json
    { "errors": [{ "errorCode": "FEATURE_DISABLED", "message": "The Experiments feature is currently disabled. Please contact dotCMS to enable it." }] }
    ```
  - The `FEATURE_DISABLED` error code distinguishes feature-flag `403`s from other `403`s
    on the same endpoints (e.g. `SITE_ACCESS_DENIED` for permission failures), allowing
    clients and UIs to show the correct message for each case.
  - The response MUST follow the standard dotCMS error response envelope.
  - **Test**: Postman — for each flag-disabled `403` case, verify the response body contains
    `errorCode: "FEATURE_DISABLED"` and the expected human-readable message.

**Flag state refresh**

- **FR-011**: Both `FEATURE_FLAG_EXPERIMENTS` and `FEATURE_FLAG_CONTENT_ANALYTICS` currently
  support runtime changes without a server restart — an administrator can toggle them in the
  dotCMS configuration system and other parts of the system react immediately. The gate
  introduced by this feature MUST NOT use that capability. The gate MUST read each flag value
  exactly once — at server startup — and hold that value for the entire lifetime of the running
  instance. A runtime configuration change MUST NOT alter the gate decision without a server
  restart, regardless of the mechanism used to make that change. This is a deliberate design
  choice: these are paid features, and activation or deactivation MUST require an explicit,
  intentional restart by an operator.
  Other existing consumers of these flags (for example, experiment feature-enablement logic
  elsewhere in the system) continue to react to configuration changes without a restart — this
  divergent state is intentional and accepted. The gate MUST be isolated from any live-refresh
  mechanism that other consumers use; wiring it into the same refresh path is an implementation
  error.
  **Breaking change**: the default value for both `FEATURE_FLAG_EXPERIMENTS` and
  `FEATURE_FLAG_CONTENT_ANALYTICS` MUST change from `true` to `false`. Any deployment
  that does not explicitly set these flags will have analytics and experiments blocked
  after this change ships. Because the current customer footprint for these features is
  small, each active customer MUST be verified before release to confirm their
  configuration explicitly sets the relevant flag(s) to `true`.
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

- **Feature Flag**: A named boolean configuration property. In the current system, each flag
  is cached at startup and refreshed live when an administrator changes its value via the
  dotCMS configuration system — other consumers of these flags continue to observe live
  updates. **Exception — the gate introduced by this feature**: the gate enforcement MUST read
  the flag value at startup only and MUST NOT react to live configuration changes. A server
  restart is required for gate behavior to change (FR-011). The live-toggle capability is
  preserved for all other existing consumers of these flags.
  Relevant flags: `FEATURE_FLAG_CONTENT_ANALYTICS`, `FEATURE_FLAG_EXPERIMENTS`.
- **Event Payload**: The JSON body sent to `POST /api/v1/analytics/content/event`. Has the
  shape `{"context": {...}, "events": [...]}`. The `context` object is shared across all
  events in the batch and contains at minimum `site_auth` and optionally `experiments`. The
  `events` array holds one or more event objects, each carrying its own `event_type` field
  (e.g., `"pageview"`, `"content_click"`). Gate logic that inspects `event_type` operates
  per-event within the array; gate logic that inspects `context.experiments` operates on the
  single top-level `context` object.
- **Experiment Context**: The `context.experiments` field in an event payload. Carries
  experiment enrollment metadata used to attribute pageview events to experiment variants.
- **Site Auth Token**: A credential generated by the siteauth endpoint and required by the
  client-side JS snippet to authenticate event sends to the analytics backend. Needed by both
  the analytics feature and the experiments feature.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: When `FEATURE_FLAG_EXPERIMENTS=false`, zero requests to any experiment endpoint
  succeed — all return `403` with a human-readable message identifying the disabled feature
  and directing the operator to enable it or contact dotCMS support.
- **SC-002**: When `FEATURE_FLAG_CONTENT_ANALYTICS=false` and `FEATURE_FLAG_EXPERIMENTS=true`,
  only `pageview` events are forwarded to the analytics backend; all other event types are
  rejected before reaching the backend.
- **SC-003**: When `FEATURE_FLAG_CONTENT_ANALYTICS=true` and `FEATURE_FLAG_EXPERIMENTS=false`,
  all analytics events are forwarded successfully regardless of type, and no experiment context
  reaches the analytics backend.
- **SC-004**: When both flags are `true`, no existing analytics or experiment functionality is
  disrupted — all requests succeed at the same rate as before this change.
- **SC-005**: When both flags are `false`, no data of any kind reaches the analytics backend
  through these endpoints.
- **SC-006**: A flag state change for the gate enforcement takes effect only after a server
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
