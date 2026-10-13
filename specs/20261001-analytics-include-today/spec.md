# Issue Resolution Specification: Analytics Date Presets and Custom Ranges

**Feature Branch**: `issue-37858-analytics-presets-spec`

**Created**: 2026-10-01

**Status**: Revised draft — requires fresh developer review of the changed scope

**Type**: Issue / Bug Resolution (Tier 1)

**Related GitHub Issue**: [dotCMS/core#37858](https://github.com/dotCMS/core/issues/37858)

**Supersedes**: The inclusive-today preset decision approved in [spec PR #37859](https://github.com/dotCMS/core/pull/37859). The developer subsequently requested completed-day rolling windows, explicit calendar presets, Custom first, and selectable adjacent-month dates. That approval does not cover this revised scope.

## Problem Statement

The Analytics dashboard offers Last 7 Days, Last 30 Days, and Custom, but lacks a direct current-day view and other useful calendar presets. Its custom calendar rejects single-day/short windows, and visible dates from an adjacent month cannot be selected.

Excluding today from Last N Days is intentional: these presets should report complete calendar days. Current-day traffic belongs in Today or a calendar-to-date selection, not in a changed Last 7/30 Days contract.

**Severity / Impact**: Medium. Users cannot conveniently inspect current-day traffic or select short/cross-month reporting windows.

## Reproduction

**Environment**: Current main, Analytics dashboard; user screenshot shows October 2026 with September 27–30 visible but unselectable. Source-inspected and user-reported; no new live browser reproduction is claimed.

1. Open the reporting-period dropdown: Today and the requested calendar presets are missing.
2. Select Custom and attempt a single-day range: the original minimum-seven-day validation rejects it.
3. In October, try clicking one of the visible September dates without navigating months: selection is disabled.

**Expected Behavior**: The dropdown offers the exact ordered list below. Custom opens the date picker. Valid short/single-day ranges and visible past adjacent-month dates are selectable; future dates remain disabled.

**Actual Behavior**: The original dropdown has only three choices and the custom restrictions above.

## Date-Window Contract

All bounds are inclusive calendar dates under the existing frontend date semantics. Weeks start **Sunday**, matching the supplied example; this change introduces no new timezone or locale policy.

| Ordered option | Window |
|---|---|
| Custom | User-selected inclusive from/to dates; choosing it shows the calendar |
| Today | Today only, including current partial data |
| Yesterday | Yesterday only |
| This week | Sunday of the current week through today |
| Last 7 Days | The seven completed calendar days ending yesterday |
| Last week | Sunday–Saturday of the previous week |
| Last 30 Days | The thirty completed calendar days ending yesterday |
| This month | First of the current month through today |
| Last month | First through last day of the previous calendar month |
| Last 90 Days | The ninety completed calendar days ending yesterday |

CAEM relative tokens `last_7_days`, `last_30_days`, and `last_90_days` retain their existing completed-day semantics. Other presets resolve to explicit `from`/`to` date strings. This requires no new CAEM endpoint/token.

## Scope of Investigation

- **Affected area**: Analytics date filters, URL-backed state, API parameter mapping, chart date filling, previous-period comparisons, and localized dropdown labels.
- **Surface**: Existing Angular Analytics portlet and data-access library. No legacy Java, ingestion, storage, REST, or authentication changes.
- **Known decisions**: Preserve inclusive absolute API bounds and the 90-day API cap. Consult relevant ADRs during planning; retain existing reporting-day/timezone behavior.

## Fix Scope & Non-Goals

**In scope**:

- Exactly the ten dropdown entries in the stated order; default remains Last 7 Days.
- Correct boundaries for current/previous week and month, including Sunday, month/year transitions, and leap years.
- Preserve existing completed-day Last 7/30 Days behavior and add Last 90 Days with the same contract.
- Current-day presets retain current-day rows in chart zero-filling/labels.
- Stable URL tokens round-trip all presets without falling back to Last 7 Days.
- Selecting Custom opens the date picker without triggering a query until a complete range is chosen.
- Valid short/single-day custom windows and adjacent-month date selection; invalid/inverted dates rejected, future calendar dates disabled.
- Prior comparisons remain adjacent, non-overlapping, and the same inclusive day count as the selected window. Today compares to yesterday; this is not a same-time-of-day comparison.

**Out of scope**:

- Last 28 Days, quarter/year presets, week-start submenus, or locale-configurable week starts.
- Reporting-timezone redesign, future-date reporting, ingestion/materialized-view latency changes, new backend endpoints, and broad picker/chart redesign.
- Changing the Last N Days API contract to include partial current-day data.

## Regression Risk

- **Blast radius**: All Analytics reports sharing date resolution, request mapping, chart filling, URL state, or custom selection.
- **Compatibility**: Existing Last 7/30 Days names, tokens, and default remain valid. Custom URLs retain inclusive absolute dates. New choices are additive. No data migrations.
- **Data considerations**: Today/This week/This month can contain partial current-day data. Existing latency and browser/server timezone differences remain unchanged. No CAEM inclusive-today deployment dependency.

## Acceptance & Verification

- **AC-001**: Dropdown order is Custom, Today, Yesterday, This week, Last 7 Days, Last week, Last 30 Days, This month, Last month, Last 90 Days. Selecting Custom shows the date picker.
- **AC-002**: With today fixed to Thursday 2026-10-01, Today resolves Oct 1; Yesterday Sep 30; This week Sep 27–Oct 1; Last 7 Days Sep 24–30; Last week Sep 20–26; Last 30 Days Sep 1–30; This month Oct 1; Last month Sep 1–30; Last 90 Days Jul 3–Sep 30.
- **AC-003**: Sunday and month/year/leap-year boundary tests validate the calendar presets. Relative rolling windows span exactly N completed days, with no current-day/N+1 bucket.
- **AC-004**: Today sends identical absolute from/to dates and retains a nonzero current-day chart row. Other calendar presets send their resolved bounds; rolling presets send unchanged relative tokens.
- **AC-005**: URL parameters restore each preset; choosing a preset emits its token immediately, while incomplete Custom emits no completed range.
- **AC-006**: Prior-period comparisons are adjacent and equally sized, including the single day preceding Today/Yesterday.
- **AC-007**: Single-day and one-to-six-day Custom selection remains valid and survives URL restoration; invalid/inverted/incomplete dates remain rejected.
- **AC-008**: With October 2026 displayed, visible September 27–30 dates are selectable and can form a range ending Oct 1 without using month navigation. Future dates, including adjacent-month future dates, remain unselectable.
- **Verification**: Developer-reviewed regression tests must be written, approved, and observed failing before production changes. Run both Analytics Nx/Vitest suites, lint, formatting, and typechecks with baseline comparison where needed. Manually verify all dropdown entries and actual picker behavior against a live stack after deployment; do not claim that verification before it occurs.

## Approval / Assumptions

- The developer explicitly requested the revised presets/order and adjacent-month selection. Formal review of this revised spec and approval of the newly written tests remain required.
- The existing implementation PR may remain stacked and draft pending revised spec approval; no prior approval is silently carried over.
- The matching CAEM PR will withdraw its inclusive-today change while retaining the independently requested required image-build gate.
