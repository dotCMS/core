# Issue Resolution Specification: `GET /v1/users/filter` ignores `orderby` and `direction`

**Feature Branch**: `37458-users-filter-sort`

**Created**: 2026-09-11

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: dotCMS/core#37458 (sub-issue of #37298; surfaced by PR #37457)

**Input**: User description: "GET /api/v1/users/filter?orderby=lastLoginDate&direction=DESC —
orderby and direction must work in this endpoint" for the three fields the Users portlet sends:
`firstName`, `emailAddress`, `lastLoginDate`.

## Problem Statement *(mandatory)*

`GET /api/v1/users/filter` accepts and documents `orderby` and `direction` but ignores both.
Every response is ordered by full name ascending. In the Users portlet the three sortable
headers change nothing, and the portlet's default sort (`lastLoginDate DESC`) has never
applied, so every install shows the list alphabetically.

Side effect: each request naming one of the three fields logs an `ERROR` with a stack trace
from the SQL sort sanitizer.

**Severity / Impact**: Every install with the modern Users portlet, on every load and every
sort click. No data loss, no failed requests. A broken control on a core admin screen, a
misleading default view, and spurious security-flavored log noise.

## Reproduction *(mandatory)*

**Environment**: dotCMS `main` (verified 2026-09-11 on a container whose users-list code
matches `upstream/main` at `6bad8c7ed9`). PostgreSQL, default config, admin user.

**Steps to Reproduce**:

1. Call `GET /api/v1/users/filter?per_page=100` and note the order of `entity[]`.
2. Repeat with `orderby=firstName|emailAddress|lastLoginDate` and `direction=ASC|DESC`.
3. Compare the seven responses.

**Expected Behavior**: Each response is ordered by the requested field and direction.

**Actual Behavior**: All seven are identical, ordered by `firstname || ' ' || lastname asc`.
Each sorted request logs
`ERROR util.SQLUtil - Invalid or pernicious sql parameter passed in : lastLoginDate`.

**Reproducibility**: Always.

## Scope of Investigation *(mandatory)*

- **Affected area**: REST API, users domain. The paginated user list used by the Users portlet.
  `GET /v1/roles/{roleid}/users` shares the same paginator and query.
- **Suspected surface**: Mixed. The endpoint and paginator are modern (`com.dotcms.rest`,
  `com.dotcms.util.pagination`); the query that builds the `ORDER BY` is legacy
  (`com.dotmarketing.business.UserFactoryImpl`). The issue's original mention of
  `UserFactoryLiferayImpl` is wrong; that class is not on this path.
- **Related known decisions**: SQL sort fields are whitelisted via `SQLUtil.sanitizeSortBy`.
  That stays. `RoleResource` already shows the correct way to pass ordering to this paginator.

## Root-Cause Hypothesis

Confirmed on the running container with SQL captured from the factory's debug log. The
params survive to the query builder and are lost in two independent places:

1. **Direction is never forwarded.** `UserResource.filter` puts the order-by key into the
   paginator's extra params but not the direction key, so the factory's default `ASC` always
   applies. Proof: `orderby=mod_date&direction=DESC` executes `order by mod_date asc`.
   `RoleResource` sets both keys and its SQL flips correctly.
2. **The field names are rejected.** `UserFactoryImpl` passes the field through the global
   sanitizer, which snake-cases it (`firstName` → `first_name`) and checks a whitelist built
   for other tables. No user field is on it. The sanitizer logs the error, returns blank, and
   the factory falls back to the full-name sort. Real columns are `firstname`, `lastname`,
   `emailaddress`, `lastlogindate`.

Also found: a term that already carries a direction (`orderby=mod_date%20desc`) produces
`order by mod_date desc asc` and HTTP 500, because the factory appends direction
unconditionally.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Forward `direction` from `UserResource.filter` the way `RoleResource` does.
- In `UserFactoryImpl`, map API field to column before sanitizing:
  `firstName` → `firstname, lastname`; `emailAddress` → `emailaddress`;
  `lastLoginDate` → `lastlogindate NULLS LAST`. Unknown or absent field → today's default order.
- Append direction once, removing the HTTP 500.
- No `SQLUtil` error is logged for the three fields.
- Update the three `UserResource` unit tests; add an integration test asserting real row
  order; add Postman cases per field and direction.

**Explicitly out of scope / non-goals**:

- Other sort columns. The portlet has three headers; grow the map when it grows.
- Adding user columns to the shared `SQLUtil` whitelist (16 call sites in 15 unrelated
  factories).
- The asset-and-permission branch of the paginator.
- Case-insensitive sort. Default collation `en_US.utf8` already interleaves case.
- Direct changes to `GET /v1/roles/{roleid}/users` or `/v1/users/loginAsData`.
- Response shape, pagination metadata, `Link` header, frontend.

## Regression Risk *(mandatory)*

- **Blast radius**:
  - Users portlet default view changes on every install from alphabetical to
    most-recent-login first. Intended, but visible. Release note.
  - `GET /v1/roles/{roleid}/users` gains working sort on the three fields. Its frontend sends
    no sort params, so nothing visible changes.
  - Other callers of `/v1/users/filter` (roles grant picker, four Postman requests) send no
    sort params. Unchanged.
  - Other callers of the users query build default filtering params. Unchanged by design.
- **Backward compatibility**: No schema, index, or contract change. Only row order differs,
  and only when requested. Rollback-safe.
- **Data considerations**: None. Sorting cost is unchanged in kind; `user_` has only its
  primary-key index today and the current full-name sort is already unindexed.

## Acceptance & Verification *(mandatory)*

- **AC-001**: For each of the three fields, `ASC` and `DESC` return correctly ordered,
  mutually reversed results.
- **AC-002**: No sort params → same order as today (full name ascending).
- **AC-003**: `lastLoginDate` in either direction lists never-logged-in users last.
- **AC-004**: `firstName` ties are broken by last name; order is stable across calls.
- **AC-005**: No `SQLUtil` error is logged for the three fields.
- **AC-006**: `orderby=mod_date%20desc` returns 200, not 500.
- **AC-007 (regression)**: `GET /v1/roles/{roleid}/users` with `orderby=firstName` is ordered
  by first name and still limited to role members; without sort params its order is unchanged.
- **AC-008 (regression)**: `UserResource` unit tests assert both order keys reach the
  paginator.
- **Verification method**:
  - Integration: new test in `dotcms-integration` seeding users with staggered names, emails
    and last-login dates (one `NULL`); covers AC-001 to AC-004 and AC-007. Registered at the
    end of the lowest-runtime `MainSuite`.
  - Unit: updated `UserResourceTest` cases for AC-008.
  - Postman: `UserResource.postman_collection.json`, six requests asserting monotonic order,
    plus one for AC-006.
  - Manual: click each Users portlet header, confirm order follows the chevron, confirm no
    `SQLUtil` error in the log (AC-005).
  - TDD gate: tests written, approved, and failing on `main` before implementation.

## Assumptions

- Field names match exactly as the frontend sends them. No aliases.
- Unknown `orderby` is coerced to the default order, not rejected with 4xx, matching the
  sanitizer's existing behavior.
- `direction` defaults to `ASC` when absent, as the endpoint already declares.
- PostgreSQL only. `NULLS LAST` is standard SQL.
