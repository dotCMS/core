# Contract: `PATCH /api/v1/experiments/{experimentId}`

The only contract this feature changes. Everything not listed is unchanged.

**Entry point**: `com.dotcms.rest.api.v1.experiments.ExperimentsResource#update`
**Body**: `ExperimentForm` · **Response**: `ResponseEntitySingleExperimentView`

The eligibility rule is enforced in `ExperimentsAPIImpl.save()`, not in the resource, so it holds
for every caller rather than only for requests arriving through this endpoint. The resource just
carries the submitted `pageId` across.

## What changes

`pageId` moves from *accepted-and-silently-discarded* to *applied or refused*.

| Request | Before | After |
|---------|--------|-------|
| `pageId` absent | ignored | ignored (unchanged) |
| `pageId` == stored value | silently discarded | **200** — no-op, never an error, whatever the status or variant count |
| `pageId` != stored, DRAFT + control is the only variant | silently discarded | **200** — page applied, control variant's `url` regenerated from the new page |
| `pageId` != stored, not DRAFT | silently discarded | **400** — message names the status rule |
| `pageId` != stored, DRAFT with ≥1 non-control variant | silently discarded | **400** — message names the variants rule |

No other field's behavior changes. No new field. No field removed. Response shape identical.

## Eligibility

```
DRAFT  AND  trafficProportion.variants == exactly one, whose id is "DEFAULT"
```

Evaluated only when the submitted `pageId` differs from the stored one — an equal value never
reaches the check (that ordering is what makes the no-op unconditional).

## Errors

`IllegalArgumentException` → 400 via
`com.dotcms.rest.exception.mapper.badrequest.IllegalArgumentExceptionMapper`.

The message must name which condition failed — a bare "invalid request" does not satisfy FR-003.

Not re-validated here: **schedule conflicts on the target page** (FR-007). A page may host several
experiments as long as their schedules do not overlap, and `start()` owns that rule. Duplicating it
would be stricter than the platform is elsewhere.

Pre-existing and unchanged: a nonexistent or unreadable target page is refused by `save()`'s
existing `getHtmlPageAsset` + `validatePermissionToEdit`, which run against the **patched**
experiment. Note that path raises `DotStateException`, which is not a 400 — pre-existing behavior,
out of scope, but call it out in the PR so it is not read as a regression.

## Compatibility

Additive in shape, behavioral in effect. A client that echoes the experiment back unchanged is
unaffected, because an equal `pageId` is a no-op — this is the whole reason that rule exists. Only a
caller sending a genuinely different page at an ineligible experiment sees a new 400, and that
caller is currently being told its change succeeded when it did not.

`openapi.yaml` is auto-generated. If `@Operation`/`@Parameter`/`@Schema` annotations change,
regenerate with `./mvnw compile -pl :dotcms-core -DskipTests` and commit the yaml with the Java.
