# Contract: `GET /api/v1/users/filter` sorting

**Branch**: `37458-users-filter-sort` | **Spec**: [../spec.md](../spec.md)

The endpoint's shape does not change. This records the sort behavior the endpoint now
guarantees, and the one description string that changes in `openapi.yaml`.

## Request

`GET /api/v1/users/filter`

| Query param | Type | Default | Behavior after this change |
|---|---|---|---|
| `orderby` | string | none | One of `firstName`, `emailAddress`, `lastLoginDate` sorts by that field. Any other value keeps today's behavior: whitelisted SQL terms (e.g. `mod_date`) are honored, everything else falls back to the default order. Never rejected with 4xx. |
| `direction` | `ASC` \| `DESC` | `ASC` | Applied to the `orderby` field. Ignored when `orderby` is absent. |
| `query`, `page`, `per_page`, `includeanonymous`, `includedefault`, `assetinode`, `permission`, `roleKey`, `includeRoles` | | | unchanged |

Auth: back-end user, unchanged.

## Ordering guarantees

| `orderby` | `ASC` | `DESC` |
|---|---|---|
| absent | `firstname || ' ' || lastname` ascending (unchanged default) | same as absent |
| `firstName` | `firstname` asc, then `lastname` asc | `firstname` desc, then `lastname` desc |
| `emailAddress` | `emailaddress` asc | `emailaddress` desc |
| `lastLoginDate` | oldest login first; users with no recorded login **last** | newest login first; users with no recorded login **last** |

`lastLoginDate` reflects the user's *previous* login, so a user who has logged in exactly once
has no recorded value and sorts with the never-logged-in group.

Collation follows the database (`en_US.utf8` by default), which interleaves upper and lower
case.

## Response

Unchanged: `ResponseEntityView<List<Map<String,Object>>>` with the existing `pagination`
block and `Link` header. Only the order of `entity[]` differs.

## Error behavior

| Input | Before | After |
|---|---|---|
| `orderby=firstName` (any of the three) | 200, order ignored, `ERROR` logged server-side | 200, ordered, no error logged |
| `orderby=mod_date%20desc` | 500 (`order by mod_date desc asc`) | 200, ordered by `mod_date desc` |
| `orderby=<unknown>` | 200, default order, `ERROR` logged | unchanged |
| `direction=<not ASC/DESC>` | 500 (`OrderDirection.valueOf`) | unchanged, out of scope |

## OpenAPI change

Only the `orderby` parameter description in `UserResource.filter` changes, to:

> Field to sort by. Supported: `firstName` (ties broken by last name), `emailAddress`,
> `lastLoginDate` (users without a recorded login sort last). Unsupported values fall back to
> the default order (full name ascending).

`openapi.yaml` is regenerated with `./mvnw compile -pl :dotcms-core --am -DskipTests` and
committed with the Java change. No `@Schema` change.

## Side effect on `GET /api/v1/roles/{roleid}/users`

Same paginator and query. Its `orderby` gains the same three fields with the same guarantees.
Its direction handling was already correct. Nothing else changes.
