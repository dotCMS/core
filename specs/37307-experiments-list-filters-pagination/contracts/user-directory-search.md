# Contract: shared user-directory search

**Consumer of**: `GET /api/v1/users/filter` (existing, unchanged)
**New home**: `core-web/libs/data-access/src/lib/dot-users/`, exported from `libs/data-access/src/index.ts`

No endpoint is added or modified by this feature. This contract records what the client capability
must expose, and the two constraints of the underlying endpoint that the implementation must honour.

## Request

| Parameter | Value | Notes |
|---|---|---|
| `query` | free text, or a user id | Matches id, first name, last name, email and full name, **as a substring**, case-insensitively |
| `page` | 1-based | The endpoint declares a default of `0`, but `PaginationUtil.getPage` coerces `page <= 0` to the first page, so it is 1-based in practice |
| `per_page` | page size | Endpoint default is 40 |

## Response

The raw envelope must be returned to the caller, not a mapped array:

```
{ entity: DotUserRow[], pagination?: { currentPage, perPage, totalEntries } }
```

`pagination.totalEntries` is required by the option list to decide whether more pages remain
(`page * perPage < totalEntries`). A capability that maps to a bare array makes paging impossible —
which is exactly why the existing roles-portlet copy asks for `per_page=500` and stops.

Only two fields of a row are consumed here: the user id and a display name.

## Permissions — load-bearing

`GET /v1/users/filter` requires only `requiredBackendUser(true)`, so **any back-end user** can call
it. That is the same population that can reach the Experiments portlet.

`GET /v1/users/{userId}` requires `isAdmin()` **or** access to both the Roles and Users portlets,
and throws `ForbiddenException` otherwise (`UserResource.java:583-627`). It **must not** be used to
resolve a selected creator's name (FR-009b): doing so would produce a filter that labels correctly
when a person is picked and breaks on reload, for non-administrators only — a permission-dependent
failure invisible to anyone testing as an administrator.

## Two behaviours the caller must implement

1. **Exact-id selection.** Because `query` matches ids as a substring, querying one id can also
   return ids that merely contain it — a shorter id is a prefix of a longer one. The caller must
   select the result whose id is exactly equal and must not take the first result (FR-009c).
2. **No bulk lookup exists.** `UserPaginator` exposes `query`, `includeanonymous`,
   `includedefault`, `assetinode`, `permission`, `roles`, `roleKey`, `removeCurrentUser`,
   `requestPassword` and `includeRoles` — there is no ids parameter. Resolving N selected people is
   N calls (FR-009d, and the cost recorded in spec assumption 11).

## Errors

Failures must reach `DotHttpErrorManagerService`, the screen's shared error reporting, and must
leave the table's current rows untouched (FR-012). The option list must render its own failure
state rather than an empty directory, so "nothing matched" and "failed to load" stay
distinguishable.

## Scope

Consolidating the two existing portlet-local copies of this call onto this capability is out of
scope (FR-015a) and has a follow-up of its own. Neither is touched by this feature.
