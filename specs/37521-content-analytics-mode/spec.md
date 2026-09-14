# Feature Specification: Content Analytics Mode (Persist / Read Only)

**Feature Branch**: `37521-content-analytics-mode`

**Created**: 2026-09-14

**Status**: Draft

**Type**: New Feature

**Input**: User description: "Specification work for ticket: https://github.com/dotCMS/core/issues/37521"

## Clarifications

### Session 2026-09-14

- Q: Should changing Analytics Mode be recorded in an audit/activity log? → A: No special audit trail — matches existing Content Analytics app config save behavior (no App config field in dotCMS currently gets a dedicated audit-log entry on save).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Stop an instance from persisting analytics events (Priority: P1)

A customer runs a non-production dotCMS instance (e.g. UAT) that currently sends analytics
events into the same shared analytics dataset as their Production instance. They want to stop
that instance from writing data — without creating dedicated users or roles in Production, and
without anyone else's data being affected.

**Why this priority**: This is the entire reason the feature exists — it replaces a much more
complex cross-environment access model (epic #37349) with a single per-instance switch. Without
this story there is no feature.

**Independent Test**: On an instance already configured for Content Analytics, set Analytics
Mode to "Read Only", perform a tracked action (e.g. view a page), and confirm no new event
reaches the Content Analytics infrastructure while existing dashboard data for that
tenant/project is still visible.

**Acceptance Scenarios**:

1. **Given** an instance configured for Content Analytics with Analytics Mode set to "Read &
   Write", **When** an admin changes Analytics Mode to "Read Only" and saves, **Then** the
   instance stops sending analytics events from that point forward.
2. **Given** an instance with Analytics Mode set to "Read Only", **When** a site visitor
   triggers a trackable action, **Then** no analytics event for that action is sent to the
   Content Analytics infrastructure.
3. **Given** an instance with Analytics Mode set to "Read Only", **When** an admin changes
   Analytics Mode back to "Read & Write" and saves, **Then** the instance resumes sending
   analytics events without requiring a restart.

---

### User Story 2 - Existing customers keep working unchanged after upgrade (Priority: P2)

A customer already has Content Analytics configured and events flowing today. After upgrading
to the version that introduces Analytics Mode, nothing should change for them unless they
deliberately act.

**Why this priority**: A silent behavior change on upgrade (events stopping without anyone
choosing that) would be a regression and a support incident. This must hold before the feature
can ship.

**Independent Test**: Take an instance with Content Analytics already configured and events
flowing, upgrade it, and confirm events continue flowing with no configuration change required.

**Acceptance Scenarios**:

1. **Given** an instance that had Content Analytics configured before this feature existed,
   **When** the instance is upgraded, **Then** its Analytics Mode is "Read & Write" and it
   continues sending events exactly as before.

---

### User Story 3 - Dashboards keep working regardless of mode (Priority: P3)

An admin on a "Read Only" instance still wants to view that tenant/project's analytics
dashboards and reports.

**Why this priority**: Read Only must mean "no ingest," not "no access" — otherwise the feature
removes value (viewing analytics) instead of just removing risk (unwanted writes).

**Independent Test**: On an instance set to "Read Only", open the Content Analytics dashboard
and confirm existing data for the tenant/project renders normally.

**Acceptance Scenarios**:

1. **Given** an instance with Analytics Mode set to "Read Only", **When** an admin opens the
   Content Analytics dashboard, **Then** existing analytics data for that tenant/project
   displays exactly as it would on a "Read & Write" instance.

---

### Edge Cases

- Switching Analytics Mode from "Read & Write" to "Read Only" does not delete, hide, or alter
  any analytics events already persisted — it only stops new events going forward.
- An instance that has never had the Content Analytics app configured shows no Analytics Mode
  input and is unaffected by this feature.
- Analytics data for a tenant + project is never split or labeled by which environment produced
  it — a "Read Only" instance and a "Read & Write" instance for the same tenant/project
  contribute to (or read) the exact same dataset, with no environment distinction anywhere.
- An instance that the Platform Team has not enabled for Content Analytics access at all has no
  Analytics Mode to set — that enablement gate is a precondition of this feature, not part of it.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide an "Analytics Mode" input in the Content Analytics app
  configuration with exactly two selectable values: "Read & Write" and "Read Only".
- **FR-002**: System MUST default Analytics Mode to "Read & Write" for every instance that had
  Content Analytics already configured before this feature existed, so no customer's event flow
  changes as a side effect of upgrading.
- **FR-003**: When Analytics Mode is "Read & Write", system MUST continue sending analytics
  events to the Content Analytics infrastructure exactly as it does today.
- **FR-004**: When Analytics Mode is "Read Only", system MUST NOT send any analytics events to
  the Content Analytics infrastructure.
- **FR-005**: System MUST apply an Analytics Mode change without requiring the dotCMS instance
  to be restarted.
- **FR-006**: System MUST allow users to view existing analytics dashboards and reports
  regardless of the instance's current Analytics Mode.
- **FR-007**: System MUST NOT classify, distinguish, or filter analytics data by originating
  environment anywhere in the pipeline — all events for a given tenant and project are combined
  with no environment dimension, superseding the environment-selector approach previously
  proposed in epic #37349.
- **FR-008**: System MUST accept analytics events without requiring an environment identifier —
  omitting it MUST NOT cause the event to be rejected, reversing the required-environment
  validation introduced under #37407.
- **FR-009**: System MUST NOT require a dedicated audit/activity log entry for Analytics Mode
  changes — it is saved like any other Content Analytics app configuration field, with no new
  audit trail introduced by this feature.

### Key Entities

- **Instance Analytics Configuration**: A per-dotCMS-instance setting living in the Content
  Analytics app configuration. Holds the Analytics Mode value ("Read & Write" or "Read Only").
  Only meaningful on an instance the Platform Team has already enabled for Content Analytics
  access.
- **Analytics Event**: A tracked user/content interaction submitted to the Content Analytics
  infrastructure. Identified by tenant and project; carries no environment identity.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An admin can change an instance's analytics-persistence behavior end-to-end
  (open configuration, change mode, save) in under one minute, with no deployment or restart.
- **SC-002**: 100% of instances that were sending analytics data before this change continue
  doing so immediately after upgrading, with zero customer action required.
- **SC-003**: An instance set to "Read Only" produces zero new analytics events in the shared
  analytics dataset while retaining full, unchanged access to its existing dashboards and
  reports.
- **SC-004**: Customers can control per-instance analytics persistence without creating or
  managing any additional users or roles for cross-instance access — eliminating the operational
  burden the original epic (#37349) set out to avoid.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The Content Analytics app configuration (a dotCMS Cloud
  feature) and the analytics event submission path from a dotCMS instance to the Content
  Analytics infrastructure. This is modern, actively-developed functionality — not legacy
  `com.dotmarketing.*` surface — though the underlying Apps/Integrations configuration framework
  it builds on predates it.
- **Backward-compatibility expectations**: Every instance with Content Analytics already
  configured must keep working exactly as before immediately after upgrade (default "Read &
  Write"). The only contract change is relaxing the recently-introduced required `environment`
  parameter on event submission back to optional — no other existing behavior changes.
- **Known related decisions**: Reverses the required, non-blank `environment` parameter decision
  from epic #37349's 2026-09-09 amendment (formalized in the `dot-ca-event-manager`
  constitution v1.2.0, Principle II, and specced under #37407). The `environment` column already
  added to the ClickHouse schema is left in place, unused, with no migration performed. The plan
  phase will formally consult `dotCMS/platform-adrs` (e.g. ADR-0022) for anything governing the
  Content Analytics app configuration or event-submission contract.

## Assumptions

- The Platform Team's existing mechanism for enabling which instances may access the Content
  Analytics infrastructure at all is unchanged by this feature; Analytics Mode only governs
  persist-vs-read-only behavior on top of that existing gate.
- "Content Analytics app" refers to the existing per-instance App/Integration configuration
  screen for Content Analytics — this feature adds a field to it, not a new settings page.
- Read Only is enforced by the dotCMS instance itself simply not submitting events; the Content
  Analytics infrastructure requires no corresponding server-side rejection logic for this
  feature.
- Users who can already edit the Content Analytics app configuration today are the same users
  authorized to change Analytics Mode — no new permission model is introduced.
