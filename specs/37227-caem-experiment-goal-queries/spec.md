# Feature Specification: CAEM-Backed Experiment Goal Result Queries

**Feature Branch**: `37227-caem-experiment-goal-queries`

**Created**: 2026-08-28

**Status**: Draft

**Type**: New Feature

**Input**: GitHub issue #37227 — Add CAEM-backed experiment result query classes with legacy CubeJS fallback switch

## User Scenarios & Testing *(mandatory)*

<!--
  These user stories describe the behavior of the dotCMS experiment evaluation system —
  the "user" is the dotCMS platform calling the experiment results API internally.
  The end consumer of this work is any dotCMS operator who views experiment results
  in the UI or queries them via the API.
-->

### User Story 1 — Bounce Rate Goal Evaluated via CAEM (Priority: P1)

A dotCMS operator runs an A/B experiment with a bounce rate goal. When the CAEM analytics backend is enabled, the experiment evaluation system retrieves goal results from the CAEM sessions endpoint rather than from CubeJS. The operator sees the same goal rate summary — unique sessions per variant, success count, and success rate — as they would with the CubeJS path, but the data now comes from the CAEM/ClickHouse infrastructure.

**Why this priority**: Bounce rate is the most critical goal type to migrate because it exercises the core sessions endpoint integration. If this goal type works correctly, the HTTP client and variant-level result mapping are proven for all other metrics.

**Independent Test**: Can be verified by running an experiment with a bounce rate goal and a known session dataset, switching the flag to CAEM, querying experiment results, and confirming that the returned per-variant session counts and bounce rates match the values derivable directly from the CAEM sessions API for the same experiment and date range.

**Acceptance Scenarios**:

1. **Given** the CAEM switch is enabled and an experiment with a bounce rate goal is active, **When** the experiment evaluation system computes results, **Then** it retrieves total sessions and bounce sessions per variant from the CAEM sessions endpoint and returns a goal rate consistent with `bounceSessions / totalSessions × 100` per variant.
2. **Given** the CAEM switch is disabled, **When** the experiment evaluation system computes results for the same experiment, **Then** it uses the existing CubeJS implementation with no behavioral change — the CAEM HTTP client is never called.
3. **Given** the CAEM switch is enabled but no sessions have been recorded for the experiment run, **When** results are requested, **Then** the system returns a zero success rate without an error — the same empty-result behavior as the CubeJS path.
4. **Given** the CAEM switch is enabled and the CAEM endpoint returns an authentication error, **When** results are requested, **Then** the system surfaces the failure without silently returning incorrect goal rates.

---

### User Story 2 — Exit Rate Goal Evaluated via CAEM (Priority: P1)

A dotCMS operator runs an experiment with an exit rate goal configured against a specific reference page. When the CAEM backend is enabled, the evaluation system calls the CAEM sessions endpoint with the reference page filter and retrieves exit sessions per variant. The operator sees the same per-variant exit rate breakdown as before, now sourced from CAEM.

**Why this priority**: Exit rate is the second metric directly exposed in the CAEM sessions endpoint. It shares the HTTP client with bounce rate and validates the `referencePage` filter path, which is required for correct exit rate computation.

**Independent Test**: Can be verified by configuring an experiment with an exit rate goal for a known reference page, enabling the switch, and confirming that the per-variant exit session counts returned by the evaluation system match a direct query to the CAEM sessions endpoint with the same filters.

**Acceptance Scenarios**:

1. **Given** the CAEM switch is enabled and an experiment has an exit rate goal with `referencePage=/checkout`, **When** results are computed, **Then** the system queries the CAEM sessions endpoint with `referencePage=/checkout` and the experiment's `experimentId` and `runningId`, returning `exitSessions / totalSessions × 100` per variant.
2. **Given** sessions exist but none exited on the configured reference page, **When** results are requested with CAEM enabled, **Then** the system returns a zero exit rate per variant without error.
3. **Given** the CAEM switch is disabled, **When** exit rate results are requested, **Then** the existing CubeJS exit rate query is used unchanged.

---

### User Story 3 — Reach-Target Goal Evaluated via CAEM (Priority: P2)

A dotCMS operator runs an experiment where the success condition is that a session visited a target page after the reference page. When the CAEM backend is enabled, the evaluation system calls the CAEM sessions behavior endpoint with `behavior=reachTarget`, and returns the per-variant success rate.

**Why this priority**: Reach-target is the most common experiment goal type. It exercises the sessions behavior endpoint integration — a separate HTTP call path from the sessions endpoint used for bounce/exit metrics.

**Independent Test**: Can be verified by seeding sessions with known page-visit sequences (some in the correct order, some reversed), enabling the switch, and confirming that the returned `successSessions` per variant match only sessions where the target was visited strictly after the reference.

**Acceptance Scenarios**:

1. **Given** the CAEM switch is enabled and an experiment has a reach-target goal (`referencePage=/landing`, `targetUrl=/thank-you`), **When** results are computed, **Then** the system queries the CAEM sessions behavior endpoint with `behavior=reachTarget&referencePage=/landing&targetUrl=/thank-you` and the experiment filters, returning `successSessions / totalSessions × 100` per variant.
2. **Given** sessions exist where the target was visited before the reference page, **When** results are computed with CAEM enabled, **Then** those sessions are excluded from `successSessions` — ordering is enforced by the CAEM backend.
3. **Given** the CAEM switch is disabled, **When** reach-target results are requested, **Then** the existing CubeJS reach-target query is used unchanged.

---

### User Story 4 — URL-Parameter Goal Evaluated via CAEM (Priority: P2)

A dotCMS operator runs an experiment where success is defined by the presence of a specific URL query parameter (e.g., `?converted=true`) in any page visited during the session. When the CAEM backend is enabled, the evaluation system calls the CAEM sessions behavior endpoint with `behavior=urlParam` and returns per-variant success rates.

**Why this priority**: URL-param goals cover the case where a fixed target URL cannot be specified. It reuses the same sessions behavior HTTP path as reach-target and validates the `paramName`/`paramValue` parameter handling.

**Independent Test**: Can be verified with a known session dataset where some sessions contain the target URL parameter and others do not, confirming that only sessions with at least one matching event are counted in `successSessions`.

**Acceptance Scenarios**:

1. **Given** the CAEM switch is enabled and an experiment has a URL-param goal (`paramName=converted`, `paramValue=true`), **When** results are computed, **Then** the system queries the CAEM sessions behavior endpoint with `behavior=urlParam&paramName=converted&paramValue=true` and the experiment filters, returning per-variant success rates.
2. **Given** a session contains the URL parameter in some events but not all, **When** results are requested with CAEM enabled, **Then** that session is counted in `successSessions` — the CAEM "any event matches" semantics are respected.
3. **Given** the CAEM switch is disabled, **When** URL-parameter results are requested, **Then** the existing CubeJS URL-parameter query is used unchanged.

---

### User Story 5 — Gradual Rollout with Safe Fallback (Priority: P1)

A dotCMS operator or platform engineer wants to enable the CAEM analytics backend for experiment results in a controlled way. They toggle a configuration flag to switch between the legacy CubeJS path and the new CAEM path. Both paths coexist — disabling the flag at any time immediately restores the CubeJS behavior with no data loss or service interruption.

**Why this priority**: The switch is the safety mechanism that makes all other user stories safe to ship. Without it, any CAEM infrastructure issue would directly impact experiment result availability. With it, the team can validate CAEM parity before fully committing to it.

**Independent Test**: Can be verified by toggling the configuration flag and confirming that (a) when disabled, CAEM HTTP calls are never made and experiment results match the current CubeJS output, and (b) when enabled, CAEM is called and the CubeJS path is bypassed — across all four goal types.

**Acceptance Scenarios**:

1. **Given** the flag is set to disabled (the default state), **When** the platform evaluates experiment results for any goal type, **Then** the existing CubeJS implementations are invoked and no CAEM HTTP calls are made.
2. **Given** the flag is set to enabled, **When** the platform evaluates experiment results, **Then** the new CAEM-backed implementations are invoked for all four goal types.
3. **Given** the flag is toggled from enabled back to disabled while the system is running, **When** the next result evaluation occurs, **Then** the CubeJS path is used without requiring a restart.
4. **Given** both implementations are present in the codebase, **When** a developer inspects the code, **Then** the CubeJS classes are unmodified and the CAEM classes are clearly distinct with no shared mutable state.

---

### Edge Cases

- What happens when the CAEM endpoint is unreachable during result evaluation? The system surfaces the failure without silently returning stale or zero results from an alternate source — the operator must be aware the data could not be retrieved.
- What happens when the experiment's `runningId` is not recognized by CAEM? The evaluation system returns an empty/zero result set, consistent with the CAEM API contract.
- What happens if the CAEM response returns an unexpected shape? The parsing layer fails fast with a clear error rather than silently producing incorrect goal rates.
- What happens if an experiment has a goal type not yet covered by CAEM (e.g., `CLICK_ON_ELEMENT`)? The switch applies only to the four supported goal types; unsupported types continue to use CubeJS regardless of the flag.
- What happens when both `experimentId` and `runningId` are required by CAEM but the experiment model has only one of them? This is an invariant violation — the experiment cannot be evaluated and an error is surfaced.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide new result query implementations for `BOUNCE_RATE`, `EXIT_RATE`, `REACH_PAGE`, and `URL_PARAMETER` goal types that retrieve experiment results from the CAEM analytics API instead of CubeJS.
- **FR-002**: The new CAEM-backed implementations MUST call the CAEM sessions endpoint (`GET /v1/analytics/sessions`) for bounce rate and exit rate goals, and the CAEM sessions behavior endpoint (`GET /v1/analytics/sessions/behavior`) for reach-target and URL-parameter goals, as specified in GitHub issues #37223 and #37224 respectively.
- **FR-003**: For bounce rate, the CAEM query MUST request `metrics=totalSessions,bounceSessions,bounceRate` with `experimentId`, `runningId`, and `dimensions=variant` to produce per-variant goal rates.
- **FR-004**: For exit rate, the CAEM query MUST include the experiment's configured reference page as `referencePage`, alongside `experimentId`, `runningId`, and `dimensions=variant`.
- **FR-005**: For reach-target, the CAEM behavior query MUST use `behavior=reachTarget` with the goal's `referencePage` and `targetUrl`, plus `experimentId`, `runningId`, and `dimensions=variant`.
- **FR-006**: For URL-parameter, the CAEM behavior query MUST use `behavior=urlParam` with the goal's `paramName` and `paramValue`, plus `experimentId`, `runningId`, and `dimensions=variant`.
- **FR-007**: The system MUST provide an HTTP client component capable of constructing, authenticating, and sending requests to the CAEM analytics API and parsing the response into the experiment result model.
- **FR-008**: The system MUST include a configuration switch that selects between the legacy CubeJS result query path and the new CAEM result query path at runtime. The switch MUST default to the CubeJS path (CAEM disabled by default).
- **FR-009**: The switch MUST apply to all four supported goal types simultaneously — partial switching (CAEM for some goals, CubeJS for others) is out of scope.
- **FR-010**: The existing CubeJS-based result query classes (`BounceRateResultQuery`, `ExitRateResultQuery`, `ReachTargetAfterExperimentPageResultQuery`) MUST remain unmodified and fully functional regardless of the switch state.
- **FR-011**: When the CAEM switch is disabled, the system MUST NOT make any HTTP calls to the CAEM analytics API during experiment result evaluation.
- **FR-012**: The CAEM HTTP client MUST use the same HMAC Bearer token authentication already used by the existing `EventAnalyticsProxyHelper` — no new credential management is introduced.
- **FR-013**: The system MUST map CAEM per-variant response rows to the existing `VariantResults` and `GoalResults` model used by the rest of the experiment evaluation pipeline, preserving all downstream consumers without modification.
- **FR-014**: When the CAEM endpoint returns zero rows (no matching sessions), the system MUST produce a zero success rate per variant — not an error and not `null`.
- **FR-015**: Unit tests MUST cover the switch dispatch logic for all four goal types in both the enabled and disabled states.
- **FR-016**: Unit tests MUST cover each new CAEM-backed result query class, including parameter construction, response parsing, and the empty-result case.
- **FR-017**: Unit tests MUST cover each CAEM query's error case (non-2xx response, malformed body) to confirm failures are surfaced rather than silently swallowed.

### Key Entities

- **MetricExperimentResultsQuery**: The existing interface that all goal-type result query builders implement. The new CAEM-backed classes implement this same interface.
- **ExperimentResultsQueryFactory**: The existing factory that maps `MetricType` to a `MetricExperimentResultsQuery` instance. The switch logic lives here — it selects the CubeJS or CAEM implementation based on the configuration flag.
- **CAEM HTTP Client**: A new component responsible for sending authenticated HTTP requests to the CAEM analytics API (`/v1/analytics/sessions` and `/v1/analytics/sessions/behavior`) and parsing the standard response envelope into the internal result model.
- **BounceRateCAEMResultQuery** *(new)*: CAEM-backed implementation of `MetricExperimentResultsQuery` for `BOUNCE_RATE` goals. Queries the CAEM sessions endpoint with bounce metrics and variant dimension.
- **ExitRateCAEMResultQuery** *(new)*: CAEM-backed implementation for `EXIT_RATE` goals. Queries the CAEM sessions endpoint with exit metrics, `referencePage` filter, and variant dimension.
- **ReachTargetCAEMResultQuery** *(new)*: CAEM-backed implementation for `REACH_PAGE` and `URL_PARAMETER` goals. Queries the CAEM sessions behavior endpoint with the appropriate behavior type and goal parameters.
- **GoalResults / VariantResults**: Existing model classes that hold per-variant session counts and success rates. The CAEM-backed implementations populate these from CAEM response data — no model changes required.
- **Configuration Switch**: A dotCMS `Config` property that toggles the active result query implementation. Readable at runtime without requiring a restart.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: When the CAEM switch is enabled, per-variant goal rates for all four goal types match values independently derivable from direct CAEM API queries for the same experiment, run, and date range — no off-by-one or ordering errors.
- **SC-002**: When the CAEM switch is disabled, experiment result evaluation behaves identically to the pre-feature state — all existing integration tests pass without modification.
- **SC-003**: The configuration switch can be toggled between enabled and disabled without restarting the application, and the selected implementation takes effect on the next result evaluation.
- **SC-004**: All unit tests for the new CAEM-backed query classes and the switch dispatch logic pass, covering at minimum: correct query construction per goal type, response parsing, empty-result handling, and error surfacing.
- **SC-005**: The existing CubeJS-based result query classes (`BounceRateResultQuery`, `ExitRateResultQuery`, `ReachTargetAfterExperimentPageResultQuery`) are unmodified and all their existing tests continue to pass.
- **SC-006**: The CAEM HTTP client makes no outbound calls when the switch is disabled — verifiable by unit test inspection of the switch dispatch path.
- **SC-007**: A goal type not yet covered by this feature (e.g., `CLICK_ON_ELEMENT`) continues to use the CubeJS path regardless of the switch state — no regression for unsupported goal types.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: This feature extends `com.dotcms.experiments.business.result` — specifically `ExperimentResultsQueryFactory` and the four goal-type result query classes. This package is modern (`com.dotcms.*`), not legacy (`com.dotmarketing.*`). The existing CubeJS query classes are not removed or modified.
- **Backward-compatibility expectations**: The CubeJS path must remain fully operational with no behavioral change when the switch is disabled. All existing experiment result API responses, admin UI displays, and downstream consumers must be unaffected. No database schema changes or API contract changes are introduced by this feature.
- **Known related decisions**: The `ExperimentsAPIImpl` composes `ExperimentResultsQueryFactory` — changes to the factory's dispatch logic must preserve the `ExperimentsAPIImpl` call sites unchanged. The CAEM HTTP client must reuse the existing bearer token retrieval mechanism (`ContentAnalyticsUtil` / `EventAnalyticsProxyHelper`) so no new credential infrastructure is needed. The two upstream CAEM endpoints this feature depends on (issue #37223 and #37224) must be available before this switch can be meaningfully enabled.

## Assumptions

- The CAEM analytics API endpoints required by this feature (`GET /v1/analytics/sessions` with bounce/exit/variant extensions, and `GET /v1/analytics/sessions/behavior`) will be available in the CAEM service before this switch is enabled in production. This feature only adds the dotCMS-side callers; endpoint availability depends on issues #37223 and #37224.
- Experiment context (`experimentId`, `runningId`, and `variant`) is already captured in the CAEM event pipeline. No new data collection or ingestion changes are needed in dotCMS for this feature.
- The CAEM API uses the same HMAC Bearer token already configured for the content analytics proxy. No new secrets management is required.
- The per-variant response from CAEM (`rows` array with one entry per variant) maps directly to the existing `VariantResults` model. The variant label in CAEM matches the variant identifier used in the dotCMS experiment model.
- The switch defaults to disabled (CubeJS) and must be explicitly enabled. Operators enable it only after confirming CAEM parity with CubeJS for their experiment data.
- The four supported goal types (`BOUNCE_RATE`, `EXIT_RATE`, `REACH_PAGE`, `URL_PARAMETER`) cover all active experiment goal types tracked in dotCMS. `CLICK_ON_ELEMENT` goals are not yet supported by CAEM and remain on CubeJS regardless of the switch.
- Both the CubeJS implementation and the CAEM implementation produce compatible output for the same `VariantResults` / `GoalResults` model — the switch is transparent to callers of `ExperimentResultsQueryFactory`.
- The date range used for CAEM queries is the experiment's running window (`from`/`to` derived from the experiment run start and end dates).