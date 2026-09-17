# Phase 1 Data Model: Creator Name on the Experiments API

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

No persistent data model changes. No table, column, index, migration or upgrade task. This document
describes the **in-memory and wire** shape only.

---

## Entity: Experiment (`com.dotcms.experiments.model.AbstractExperiment`)

### Existing members that this change must not disturb

| Member | Kind | JSON | Invariant to preserve |
|---|---|---|---|
| `createdBy()` | abstract attribute, `String` | `createdBy` | Key, value (the creator's **user id**) and meaning unchanged — FR-005. Persisted in `experiment.created_by`. |
| `getOwner()` | `@Value.Derived`, `@JsonIgnore` | *(absent)* | Still returns `createdBy()`. Permission behaviour unchanged — FR-006. Stays out of the payload. |
| `lastModifiedBy()` | abstract attribute, `String` | `lastModifiedBy` | Untouched, no name companion — FR-008. |

### New member

| Property | Value |
|---|---|
| **Name** | `createdByUserName` |
| **Java** | `default String createdByUserName()` on `AbstractExperiment` |
| **Immutables kind** | **None** — deliberately not `@Value.Default/Derived/Lazy`, so it is not an attribute: no field, no builder method, no `equals`/`hashCode`/`toString` participation, no `build()` cost |
| **Jackson** | `@JsonProperty(value = "createdByUserName", access = JsonProperty.Access.READ_ONLY)` |
| **Swagger** | `@Schema(description = "...", example = "Admin User")` on the same method |
| **Type** | `String`, **always non-null and non-empty** (FR-003) — a real name, `System`, or `unknown` |
| **Persisted** | No — resolved per serialization (FR-016, FR-017) |
| **Settable** | No — read-only by design; `READ_ONLY` is what keeps an inbound payload parseable (FR-023) |
| **Derivation** | `ExperimentCreatorNameResolver.resolve(createdBy())` |

### Evaluation timing (the property that makes the design work)

| Path | Builds an Experiment? | Resolves the name? |
|---|---|---|
| `ExperimentTransformer` (every DB row: `find`, `list`) | Yes | **No** |
| `addTargetingConditions` → `withTargetingConditions` rebuild on `find` | Yes | **No** |
| `cacheRunningExperiments()` → running-experiments list cache (page render) | Yes | **No** — FR-015 |
| Push-publish dependency walk (`DependencyManager`) | Yes | **No** — FR-015 |
| Jackson serialization of a REST response | No | **Yes** — once per experiment per response |

A `@Value.Derived` member would answer "Yes" in every row of that table; a `@Value.Lazy` member
would answer "Yes" once per instance and then memoize it — including in the shared, long-lived
running-experiments cache entries. See [research.md](./research.md) R1/R2.

---

## Derivation rule: `ExperimentCreatorNameResolver`

Input: the raw `createdBy` user id. Output: a non-empty display string.

```
resolve(createdById):
  1. createdById not set          -> return "unknown"          (never null/empty; FR-003)
  2. createdById is "system"      -> return "System"           (no lookup at all; FR-010a)
  3. user = userAPI.loadUserById(createdById)      # UserCache-backed (research R3)
  4. fullName = user.getFullName()                 # first + middle + last
  5. fullName is set              -> return fullName
  6. otherwise                    -> return "unknown"          # blank-name user; FR-010
  on NoSuchUserException          -> return "unknown"          # deleted / orphaned; FR-009
  on DotDataException / any other -> return "unknown"          # never fail the request; FR-011
```

The two labels are `BrowserAPIImpl.ownerName`'s, which answers the same question for the Content
Drive folder view (FR-010b). That implementation reaches the user through `UserLocalManagerUtil` and
so bypasses `UserCache`; this one deliberately goes through `APILocator.getUserAPI()` instead.

**Per-entry isolation** (FR-012): the rule is applied independently per experiment, so one bad id in
a list degrades exactly one entry.

**Logging** (FR-013): `Logger.debug` for the expected not-found case — it is a data condition, not
an error, and debug is off by default, so a listing full of orphaned creators cannot flood the log.
`Logger.warn` for an unexpected infrastructure failure, which is rare by nature and worth surfacing.

**Testability seam**: the resolver accepts a `UserAPI` so every branch above is unit-testable with a
mock; production code goes through `APILocator.getUserAPI()`.

---

## Wire shape

Before:

```json
{
  "id": "0e8b8b1e-...",
  "name": "Homepage CTA test",
  "createdBy": "dotcms.org.1",
  "lastModifiedBy": "dotcms.org.1"
}
```

After — one added key, nothing else moved:

```json
{
  "id": "0e8b8b1e-...",
  "name": "Homepage CTA test",
  "createdBy": "dotcms.org.1",
  "createdByUserName": "Admin User",
  "lastModifiedBy": "dotcms.org.1"
}
```

Unresolvable creator (deleted user, or a user whose name parts are all blank):

```json
{
  "createdBy": "deleted-user-id-4711",
  "createdByUserName": "unknown"
}
```

Created by the system user:

```json
{
  "createdBy": "system",
  "createdByUserName": "System"
}
```

Property order in real responses is alphabetical — `DotObjectMapperProvider.createDefaultMapper()`
enables `SORT_PROPERTIES_ALPHABETICALLY` when `dotcms.rest.sort.json.properties` is true (the
default), which places `createdByUserName` immediately after `createdBy`.
