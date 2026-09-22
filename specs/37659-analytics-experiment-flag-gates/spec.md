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

- What happens if the flag state changes while a request is in flight? The flag state is read
  at request entry; the in-flight request completes under the flag state it started with.
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
- **FR-002a**: `GET /api/v1/experiments/health` MUST verify that the experiments and analytics
  configuration is properly set up for the current site and return one of three states:
  - `OK` — required credentials are configured and the analytics backend is reachable.
  - `NOT_CONFIGURED` — no analytics configuration has been set up for the site.
  - `CONFIGURATION_ERROR` — configuration exists but is incomplete (missing required
    credentials) or the analytics backend is unreachable.
  When `FEATURE_FLAG_EXPERIMENTS=false`, this endpoint returns `403` per FR-001.
  > **Note**: The Experiments portlet UI calls this endpoint to decide whether to render the
  > experiments interface. A response of `OK` enables the UI; `NOT_CONFIGURED` or
  > `CONFIGURATION_ERROR` causes the UI to display an error message to the operator instead.

**EventAnalyticsProxyResource — GET endpoints**

- **FR-003**: `GET /api/v1/analytics/{path}` (all read/query paths except siteauth and health)
  MUST return `403` when `FEATURE_FLAG_CONTENT_ANALYTICS=false`.
- **FR-003a**: `GET /api/v1/analytics/health` is an existing endpoint, not a new one introduced
  by this feature. It is called out explicitly here because of its UI and diagnostic role (see
  Note below) — the gate that FR-003 applies to all analytics read paths covers this path too.
  The endpoint MUST return `403` when `FEATURE_FLAG_CONTENT_ANALYTICS=false`. When
  `FEATURE_FLAG_CONTENT_ANALYTICS=true`, the response MUST surface one of three states
  (mirroring FR-002a):
  - `OK` — required credentials are configured and the analytics backend is reachable.
  - `NOT_CONFIGURED` — no analytics configuration has been set up for the site.
  - `CONFIGURATION_ERROR` — configuration exists but is incomplete (missing required
    credentials such as the analytics URL) or the analytics backend is unreachable.
  This endpoint is explicitly called out because it is the operator's primary tool for
  diagnosing setup problems; returning a proxy error or timeout when the flag is ON but
  misconfigured would obscure the real cause.
  > **Note**: The Analytics portlet UI calls this endpoint to decide whether to render the
  > analytics interface. A response of `OK` enables the UI; `NOT_CONFIGURED` or
  > `CONFIGURATION_ERROR` causes the UI to display an error message to the operator instead.
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
  - **Test**: Postman — for each flag-disabled `403` case, verify the response body matches
    the standard dotCMS error envelope shape and contains the expected human-readable message.

**Flag state refresh**

- **FR-011**: A server restart is required for flag changes to take effect on the gate
  enforcement introduced by this feature. Live toggling without a restart is intentionally
  not supported. Live flag changes MUST NOT alter gate behavior on a running instance.
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
