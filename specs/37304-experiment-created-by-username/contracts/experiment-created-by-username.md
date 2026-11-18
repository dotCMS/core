# Contract: `createdByUserName` on the Experiment payload

**Feature**: [../spec.md](../spec.md) | **Plan**: [../plan.md](../plan.md)

The contract is the `Experiment` schema itself, so it is inherited by every response that embeds an
Experiment. `openapi.yaml` is generated from the model annotations — this file states what the
regenerated yaml must contain, it is not a second source of truth.

---

## Schema fragment (expected in `dotCMS/src/main/webapp/WEB-INF/openapi/openapi.yaml`)

Under `components.schemas.Experiment.properties`, alphabetically between `createdBy` and
`creationDate`:

```yaml
    Experiment:
      type: object
      properties:
        createdBy:
          type: string                       # unchanged
        createdByUserName:
          type: string
          description: >-
            Display name of the user who created the experiment. Reports "System" for the
            system user and "unknown" when the user cannot be resolved or has no name set,
            so the value is never empty.
          example: Admin User
        creationDate:
          type: string
          format: date-time
```

Constraints the generated yaml must satisfy:

- `createdBy` keeps `type: string` and gains no description change (FR-005).
- No property is removed, renamed or retyped (FR-007).
- `owner`, `identifier`, `permissionId`, `manifestInfo` stay **absent** — they are `@JsonIgnore`d
  derived members and must remain so (FR-006).
- `lastModifiedBy` is unchanged, with no name companion (FR-008).

## Response wrappers (unchanged — listed to make the "no edit" explicit)

| Wrapper | Shape | Change |
|---|---|---|
| `ResponseEntitySingleExperimentView` | `ResponseEntityView<Experiment>` | **None** — inherits the field |
| `ResponseEntityExperimentView` | `ResponseEntityView<List<Experiment>>` | **None** — inherits the field |

## Endpoints that must carry the field

All 14 inherit it without being edited. Enumerated in [../spec.md](../spec.md#endpoints-in-scope):
create, `PATCH`, `_archive`, `GET /{id}`, `GET` list, `DELETE /goals/primary`, `_start`, `_end`,
`scheduled/{id}/_cancel`, `POST /variants`, `DELETE /variants/{name}`, `PUT /variants/{name}`,
`PUT /variants/{name}/_promote`, `DELETE /targetingConditions/{id}`.

Explicitly **not** carrying it, because they return no Experiment:
`DELETE /v1/experiments/{experimentId}` (returns the string `"Experiment deleted"`),
`POST /isUserIncluded`, `GET /{id}/results`, `GET /health`.

## Behavioural contract

| Condition | `createdBy` | `createdByUserName` | HTTP |
|---|---|---|---|
| Creator resolves, has a name | user id | full name (first + middle + last) | 200 |
| Creator resolves, all name parts blank | user id | `unknown` | 200 |
| Creator does not resolve (deleted/orphaned) | user id | `unknown` | 200 |
| User lookup fails (infrastructure) | user id | `unknown` | 200 |
| Creator is the system user | `system` | `System`, short-circuited before any lookup | 200 |
| One bad creator among many in a list | per entry | only the affected entry falls back | 200 |

The two fallback labels are the ones `BrowserAPIImpl.ownerName` already publishes for the Content
Drive folder view, so the same orphaned owner reads the same in both listings.

The field is never `null`, never absent and never `""`.

## Where the field is produced

`ExperimentView` (`com.dotcms.rest.api.v1.experiments`) wraps the Experiment for a response and adds
the field; the Experiment is `@JsonUnwrapped`, so the payload stays a flat object and the wire shape
is unchanged except for the addition. `ResponseEntitySingleExperimentView` and
`ResponseEntityExperimentView` do the wrapping, so none of the 14 endpoints changed.

The `Experiment` schema in `openapi.yaml` is therefore untouched, and a new `ExperimentView` schema
carries the flattened fields plus `createdByUserName`. Both response wrappers now `$ref` the view.

Because the model is a plain data object again, the field does **not** reach push-publish bundles or
starter exports, and those paths pay no user lookup — no mix-in or other mitigation is needed.

## Deserialization contract (FR-023)

`createdByUserName` is **read-only**: serialized on the way out, ignored on the way in. A payload
containing it must still deserialize into an `Experiment` without error.

With the field on the view rather than the model, this is now free: the Experiment payload has no
serialize-only property, so it round-trips exactly as it did before this change. `ExperimentView` is
a response type and is never deserialized. The guard is `bareExperiment_stillRoundTripsAndKeepsItsContract`.

## Verification

```bash
# Regenerate and inspect the contract
JAVA_HOME="$HOME/.sdkman/candidates/java/25.0.2-ms" \
  ./mvnw compile -pl :dotcms-core --am -DskipTests -Ddocker.skip

grep -A 8 '^    Experiment:' dotCMS/src/main/webapp/WEB-INF/openapi/openapi.yaml
git diff --stat dotCMS/src/main/webapp/WEB-INF/openapi/openapi.yaml   # must show the added property
```

The committed yaml must match what the build produces, or the CI contract check fails (FR-019).
