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

**Why this priority**: This is the fully-enabled "happy path" that existing customers upgrading to
this gate enforcement must not see disrupted. Confirming no regression here protects the majority
of production deployments.

**Independent Test**: Enable both flags; verify all existing experiment endpoints respond
normally and all analytics event types are forwarded to the analytics backend as-is.

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

**Independent Test**: Enable only experiments; verify experiment endpoints work, pageview events
are forwarded, non-pageview events are rejected, and analytics read endpoints are blocked.

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

**Independent Test**: Enable only analytics; verify all event types are accepted, experiment
context is stripped when present, and all experiment management endpoints are blocked.

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

**Independent Test**: Disable both flags; verify all analytics and experiment endpoints return 403.

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

- What happens if the flag state changes while a request is in flight? The flag state is read
  at request entry; the in-flight request completes under the flag state it started with.
- What happens if `context.experiments` is present but empty (null, empty object, empty array)?
  Treated the same as absent — no experiment context to strip or validate.
- What if `event_type` is missing from the payload when analytics=OFF, experiments=ON? The
  event is forwarded as-is. `event_type` is a required field validated by the analytics
  backend — rejecting it here would produce a misleading `403` instead of the backend's own
  validation error, obscuring the real cause for the caller.

### Test Strategy

The following test cases must be covered across unit, integration, and API test layers:

- Both flags OFF → all experiment endpoints (`/api/v1/experiments/**`), all analytics read
  endpoints (`/api/v1/analytics/{path}`), the siteauth endpoint, and the event ingest endpoint
  (`/api/v1/analytics/content/event`) return `403` with the feature-disabled message.
- Analytics=ON, Experiments=OFF → experiment endpoints return `403`; analytics events without
  experiment context are accepted; events with experiment context have it stripped and are
  still accepted.
- Analytics=OFF, Experiments=ON → experiment endpoints are accessible; `pageview` events are
  accepted; non-`pageview` events are rejected with `403`; analytics read endpoints return
  `403`; siteauth endpoint is accessible.
- Both flags ON → all endpoints are accessible; all event types are accepted and forwarded
  as-is.
- Feature-disabled `403` responses include a human-readable message naming the disabled
  feature and directing the operator to contact dotCMS.
- Siteauth endpoint is accessible when either flag is ON; returns `403` only when both are OFF.

**Postman tests** cover all cases that return `403` — these never reach the analytics backend
so no CAEM infrastructure is required. This includes: both flags OFF, non-`pageview` events
when only experiments is ON, experiment endpoints when experiments is OFF, and analytics read
endpoints when analytics is OFF.

**Integration tests with a mocked analytics backend** cover all success cases where the
request is forwarded — event accepted and forwarded as-is, experiment context stripped before
forwarding, `pageview` events forwarded when only experiments is ON. A live analytics backend
is not available in the test environment, so the CAEM server must be mocked to verify these
flows end-to-end.

## Requirements *(mandatory)*

### Functional Requirements

**ExperimentsResource — `/api/v1/experiments/**`**

- **FR-001**: The system MUST return `403` on all experiment endpoints when
  `FEATURE_FLAG_EXPERIMENTS=false`, regardless of the analytics flag state. This includes
  CRUD operations, lifecycle actions (`_start`, `_end`, `_cancel`), `isUserIncluded`,
  `/{id}/results`, and `/health`.
- **FR-002**: When `FEATURE_FLAG_EXPERIMENTS=true`, all experiment endpoints MUST continue to
  function as they do today with no behavioral change.
- **FR-002a**: `GET /api/v1/experiments/health` MUST verify that the experiments and analytics
  configuration is properly set up for the current site and return one of three states:
  - `OK` — required credentials are configured and the analytics backend is reachable.
  - `NOT_CONFIGURED` — no analytics configuration has been set up for the site.
  - `CONFIGURATION_ERROR` — configuration exists but is incomplete (missing required
    credentials) or the analytics backend is unreachable.
  When `FEATURE_FLAG_EXPERIMENTS=false`, this endpoint returns `403` per FR-001.

**EventAnalyticsProxyResource — GET endpoints**

- **FR-003**: `GET /api/v1/analytics/{path}` (all read/query paths except siteauth and health)
  MUST return `403` when `FEATURE_FLAG_CONTENT_ANALYTICS=false`.
- **FR-003a**: `GET /api/v1/analytics/health` MUST return `403` when
  `FEATURE_FLAG_CONTENT_ANALYTICS=false`. This endpoint is explicitly called out because it
  is used to verify the analytics backend connectivity and has no value when analytics is
  disabled.
- **FR-004**: `GET /api/v1/analytics/content/siteauth/generate/{siteId}` MUST return `403`
  only when BOTH `FEATURE_FLAG_CONTENT_ANALYTICS=false` AND `FEATURE_FLAG_EXPERIMENTS=false`.
- **FR-005**: The siteauth endpoint MUST be accessible whenever EITHER
  `FEATURE_FLAG_CONTENT_ANALYTICS=true` OR `FEATURE_FLAG_EXPERIMENTS=true`, because both
  features require a valid site auth token to authenticate event sends.

**EventAnalyticsProxyResource — POST `/api/v1/analytics/content/event`**

- **FR-006**: The event ingest endpoint MUST return `403` when both flags are `false`.
- **FR-007**: When `FEATURE_FLAG_CONTENT_ANALYTICS=true` and `FEATURE_FLAG_EXPERIMENTS=false`:
  the system MUST strip `context.experiments` from the event payload before forwarding, and
  MUST still forward the event (not drop it). Stripping is silent — no error returned.
- **FR-008**: When both flags are `true`: the system MUST forward the event payload to the
  analytics backend as-is, regardless of event type or the presence of experiment context.
- **FR-009**: When `FEATURE_FLAG_CONTENT_ANALYTICS=false` and `FEATURE_FLAG_EXPERIMENTS=true`:
  the system MUST accept and forward events where `event_type = "pageview"`, and MUST reject
  with `403` any event where `event_type` is present with a value other than `"pageview"`.
  If `event_type` is absent, the event MUST be forwarded without rejection — `event_type` is
  a required field enforced by the analytics backend itself, which will return its own
  validation error. Rejecting an absent `event_type` at this layer would produce a misleading
  `403` (feature disabled) instead of the backend's precise validation response, obscuring the
  real cause for the caller.

**Feature-disabled error response**

- **FR-010**: Every `403` returned because a feature flag is `false` MUST include a
  human-readable message in the response body that states which feature is disabled and
  directs the operator to contact dotCMS to have it enabled.
  - Example: *"The Experiments feature is currently disabled. Please contact dotCMS to enable it."*
  - The message MUST follow the standard dotCMS error response envelope so clients can
    parse it consistently alongside other API errors.

**Flag state refresh**

- **FR-011**: A server restart is required for flag changes to take effect on the gate
  enforcement introduced by this feature. Live toggling without a restart is intentionally
  not supported. Both flags currently support live toggling in other parts of the system;
  that live-toggle capability MUST be disabled as part of this feature to ensure controlled
  activation. Rationale: these are paid features — a restart gives operators an explicit,
  intentional activation step rather than an immediate live toggle.

### Key Entities

- **Feature Flag**: A named boolean configuration property. Each flag is cached at startup and
  refreshed when an administrator changes its value via the dotCMS configuration system.
  Relevant flags: `FEATURE_FLAG_CONTENT_ANALYTICS`, `FEATURE_FLAG_EXPERIMENTS`.
- **Event Payload**: The JSON body sent to `POST /api/v1/analytics/content/event`. Contains a
  `context` object (with at minimum `site_auth`, optionally `experiments`) and an `event_type`
  field indicating the kind of event (e.g., `pageview`, `content_click`).
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
