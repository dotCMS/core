# Issue Resolution Specification: Include Today's Analytics Events

**Feature Branch**: `issue-37858-analytics-today-spec`

**Created**: 2026-10-01

**Status**: Draft — awaiting developer approval before implementation planning

**Type**: Issue / Bug Resolution (Tier 1)

**Related GitHub Issue**: [dotCMS/core#37858](https://github.com/dotCMS/core/issues/37858)

**Input**: Users cannot view events logged today in dotCMS Analytics. Make the dashboard match CAEM's inclusive-today relative ranges and permit a today-only custom view.

## Problem Statement

The Analytics dashboard's predefined date windows end yesterday. Users therefore cannot see today's events in Last 7 days or Last 30 days. The corresponding CAEM change includes today while preserving exactly N calendar days; without a matching core change, frontend chart filling and labels can discard today's returned rows.

The custom calendar permits today as an endpoint but rejects ranges shorter than seven days. This prevents selecting today alone even though the query API accepts a single-day absolute range.

**Severity / Impact**: Medium. Analytics users cannot inspect current-day traffic through predefined windows or isolate today's traffic through custom selection.

## Reproduction

**Environment**: Current `main`; Analytics dashboard with events for the selected site today. Source-inspected and user-reported; a new browser reproduction has not yet been recorded.

**Steps to Reproduce**:

1. Log analytics events for a site today.
2. Open its Analytics dashboard and select Last 7 days or Last 30 days.
3. Inspect the final chart label and current-day values.
4. Select Custom and choose today as both the start and end date.

**Expected Behavior**: Predefined windows include today and display exactly the requested number of calendar days. A single-day custom window is accepted and requests that day's analytics.

**Actual Behavior**: Predefined chart windows end yesterday; custom same-day selection is rejected.

**Reproducibility**: Deterministic date-window behavior; observing a nonzero current-day value requires stored events and any normal ingestion/materialized-view refresh to have completed.

## Scope of Investigation

- **Affected area**: Analytics dashboard date filters, URL-backed state, chart date filling, and previous-period comparisons.
- **Suspected surface**: Modern Angular Analytics portlet and its data-access library under `core-web/libs/portlets/dot-analytics/`. No Java or legacy `com.dotmarketing.*` changes are expected.
- **Related known decisions**: Existing date-only API parameters and inclusive absolute bounds remain unchanged. Relevant ADRs must be consulted during planning after spec approval.

## Root-Cause Hypothesis

Predefined frontend windows intentionally cover completed days ending yesterday. That no longer matches the inclusive-today CAEM contract proposed in [dotCMS/dot-ca-event-manager#139](https://github.com/dotCMS/dot-ca-event-manager/pull/139). A separate minimum-seven-day rule is shared by custom calendar selection and URL validation, which rejects single-day windows.

## Fix Scope & Non-Goals

**In scope**:

- Include today in Last 7 days and Last 30 days without creating an N+1-day window.
- Preserve returned current-day values when generating chart labels and zero-filling missing dates.
- Keep previous comparison periods equally sized, adjacent, and non-overlapping.
- Allow valid one-day and other short custom ranges through calendar selection and restored URL state.
- Preserve invalid/inverted-date rejection and the existing calendar restriction against future dates.
- Update affected comments, regression tests, and any directly stale documentation.

**Explicitly out of scope / non-goals**:

- Adding new Today/Yesterday dropdown presets or changing existing relative range tokens.
- Changes to ingestion, refresh latency, storage, authentication, REST endpoints, or the CAEM implementation.
- Defining a new reporting timezone policy or solving existing browser/server timezone differences.
- Broad chart, calendar, or Analytics portlet redesigns.

## Regression Risk

- **Blast radius**: All Analytics reports that share date-window, zero-fill, comparison, or custom-range validation utilities.
- **Backward compatibility**: Existing predefined names, custom `from`/`to` URL state, and inclusive absolute API parameters remain valid. The predefined window moves forward one day; custom validation becomes less restrictive. No database migrations or stored-event changes.
- **Data considerations**: Today's counts can be incomplete and can change as traffic arrives. Deploy the frontend correction with CAEM's inclusive-today contract; existing data does not require repair.

## Acceptance & Verification

- **AC-001**: With today fixed to 2024-01-15, Last 7 days covers January 9–15 inclusive, and Last 30 days covers December 17–January 15 inclusive. Neither includes an eighth/thirty-first day.
- **AC-002**: Given nonzero API data for today and at least one missing day, the chart retains today's value, labels today as its final day, fills only missing days with zero, and has exactly N daily buckets.
- **AC-003**: Previous periods immediately precede the current window and have the same calendar-day count. For Last 7 days ending January 15, the comparison is January 2–8. A one-day custom range compares against the preceding single day.
- **AC-004**: Selecting today twice in Custom emits identical start/end dates. The resulting dashboard request uses equal `from` and `to`; restoring those custom URL parameters preserves the selected day rather than resetting to Last 7 days.
- **AC-005**: Valid one-day through six-day custom windows are accepted, existing longer windows remain valid, invalid/inverted dates remain rejected, and the calendar continues to disallow future-date selection.
- **AC-006**: Existing predefined API tokens remain `last_7_days` and `last_30_days`; no ingestion or backend contracts are changed in core.
- **Verification method**: Developer-reviewed regression tests for date boundaries, sparse/current-day chart data, comparisons, custom validation, calendar emissions, URL restoration, and request parameters. Tests must be approved and observed failing for the intended reason before implementation. Run targeted Nx/Vitest suites for the Analytics data-access and portlet projects, then relevant lint/type checks. After both repositories' changes are deployed, manually verify newly logged current-day events appear in the dashboard and today-only custom selection survives a reload. Do not claim browser/live-stack verification unless it has actually been performed.

## Assumptions

- The matching CAEM inclusive-today change is deployed in coordination with this frontend change.
- Single-day reporting uses the existing Custom interaction, not a new dropdown preset.
- Existing reporting-day/timezone semantics are retained; this fix changes inclusion and window length, not timezone ownership.
- The user authorizes issue/PR creation. Spec approval, test approval, and the failing-test gate remain pending; no implementation has been written.
