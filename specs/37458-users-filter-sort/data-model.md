# Data Model: `GET /v1/users/filter` sort fix

**Branch**: `37458-users-filter-sort` | **Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

No new entities, tables, or persisted fields. This documents the existing data the fix reads
and the in-memory mapping it introduces.

## Existing table: `user_` (read only)

| Column | Type | Used by this fix as |
|---|---|---|
| `userid` | varchar, PK | row identity; only indexed column |
| `firstname` | varchar | sort key for `firstName`; part of the default order |
| `lastname` | varchar | secondary key for `firstName`; part of the default order |
| `emailaddress` | varchar | sort key for `emailAddress` |
| `lastlogindate` | timestamptz, nullable | sort key for `lastLoginDate`; `NULL` for users with fewer than two logins |
| `companyid`, `delete_in_progress` | | existing filter predicates, unchanged |

`lastlogindate` semantics: `UserManagerImpl.updateLastLogin` copies `logindate` into
`lastlogindate` and then sets `logindate = now`. It holds the *previous* login, so it is `NULL`
after a first login.

## Existing value object: `UserAPI.FilteringParams` (unchanged)

| Field | Type | Default | Set by |
|---|---|---|---|
| `orderBy` | `String` | `null` | `UserResource.filter` (today), `RoleResource.loadUsersByRoleId` |
| `orderDirection` | `String` | `SQLUtil._ASC` (`" asc"`) | `RoleResource.loadUsersByRoleId` today; **`UserResource.filter` after F1** |
| `includeAnonymousUser` | `boolean` | `false` | both resources |
| `includeDefaultUser` | `boolean` | `false` | both resources |

Map keys read by `Builder.build(Map)`: `orderby`, `orderdirection`, `includeanonymous`,
`includedefault`.

## New in-memory mapping: sortable user fields (F2, private to `UserFactoryImpl`)

| API field (exact match) | Column expression template | Notes |
|---|---|---|
| `firstName` | `firstname {dir}, lastname {dir}` | tiebreak on last name |
| `emailAddress` | `emailaddress {dir}` | |
| `lastLoginDate` | `lastlogindate {dir} NULLS LAST` | `NULL` last in both directions |

Resolution rules:

1. `orderBy` is `null`/blank → default `firstname || ' ' || lastname asc`.
2. `orderBy` matches a key above → expression with `{dir}` substituted; sanitizer not called.
3. Otherwise → existing path: `SQLUtil.sanitizeSortBy(orderBy)`; blank result → default;
   non-blank result → append `{dir}` only if the term does not already end in ` asc`/` desc`.

`{dir}` normalization: trim, lower-case; `desc` → `desc`; anything else → `asc`.

## Sort direction values across layers

| Layer | Representation | Example |
|---|---|---|
| HTTP query | `direction=ASC` / `DESC` (`@DefaultValue("ASC")`) | `direction=DESC` |
| `UserResource` | `OrderDirection` enum | `OrderDirection.DESC` |
| extra-params map / `FilteringParams` | `SQLUtil._ASC` / `SQLUtil._DESC` (leading space) | `" desc"` |
| SQL | `asc` / `desc` | `order by emailaddress desc` |
