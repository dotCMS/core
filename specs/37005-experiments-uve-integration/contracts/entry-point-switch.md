# Contract: `FEATURE_FLAG_EXPERIMENTS_PORTLET`

**Spec**: [../spec.md](../spec.md) — FR-011 … FR-015a | **Research**: [../research.md](../research.md) R1

The switch is a configuration property, so its contract is the shape an operator and the frontend
each see. It is the only new server-side surface in this work.

---

## Identity

| | |
|---|---|
| **Name** | `FEATURE_FLAG_EXPERIMENTS_PORTLET` |
| **Type** | boolean |
| **Shipped default** | `false` — explicit, in `dotmarketing-config.properties` |
| **Scope** | Which experiments experience the **UVE Experiments navigation item** leads to. Nothing else. |
| **Owner** | Introduced by #37005; retired by #37008 |
| **Not to be confused with** | `FEATURE_FLAG_EXPERIMENTS` — the backend kill-switch for the whole Experiments feature (default `true`, read by `ConfigExperimentUtil`). Unchanged by this work. |

The name is fixed by the spec's Assumptions. It places the switch in the same family as the other UI
switches (`FEATURE_FLAG_UVE_*`, `FEATURE_FLAG_DOTAI_CONFIG_UI`) and reads unambiguously against
`FEATURE_FLAG_EXPERIMENTS`.

---

## Server side — three additive edits

### 1. Constant

`dotCMS/src/main/java/com/dotcms/featureflag/FeatureFlagName.java`

```java
/**
 * Selects which experiments experience the UVE Experiments navigation item leads to: the new
 * site-wide Experiments portlet when on, the legacy per-page screens when off. Off by default so
 * existing customers keep the flow they have until they opt in.
 *
 * NOT a kill-switch for experiments. {@link #FEATURE_FLAG_EXPERIMENTS} is that, and this value has
 * no bearing on whether experiments are served to site visitors. Retired by #37008.
 * Frontend equivalent: {@code FeaturedFlags.FEATURE_FLAG_EXPERIMENTS_PORTLET}.
 */
String FEATURE_FLAG_EXPERIMENTS_PORTLET = "FEATURE_FLAG_EXPERIMENTS_PORTLET";
```

### 2. Exposure

`dotCMS/src/main/java/com/dotcms/rest/api/v1/system/ConfigurationResource.java`

The constant goes in **both** sets, or it does not work:

- `BOOLEAN_FEATURE_FLAGS` — or it arrives as a string and `=== true` comparisons fail.
- `WHITE_LIST` — or, per the file's own maintenance rule, it is *silently excluded* from the
  response.

Missing from `WHITE_LIST` is the dangerous half: `GET /api/v1/configuration/config` would return
an object without the key, `getKey` would substitute `FEATURE_FLAG_NOT_FOUND`, and the frontend
would read the switch as **on** — the exact inverse of FR-013, with no error anywhere. A test asserts
the key is present in the response.

### 3. Shipped default

`dotCMS/src/main/resources/dotmarketing-config.properties`, beside the other UI switches:

```properties
## Selects the destination of the UVE Experiments navigation item: the new site-wide Experiments
## portlet when true, the legacy per-page screens when false. Off by default so existing customers
## see no change until they opt in. Not related to FEATURE_FLAG_EXPERIMENTS, which governs whether
## experiments are served to site visitors at all and stays at its own default.
FEATURE_FLAG_EXPERIMENTS_PORTLET=false
```

The explicit `false` is **required, not decorative**: every flag reader in the frontend maps an
unset property to *enabled* (see [research.md R1](../research.md)). Declaring the switch without
setting it would ship it on.

**Not touched**: `ConfigExperimentUtil`, `ExperimentWebAPIImpl`, `HTMLPageAssetRenderedAPIImpl`, or
anything else reading `FEATURE_FLAG_EXPERIMENTS` (FR-014, FR-015a).

---

## Wire format

`GET /api/v1/configuration/config?keys=FEATURE_FLAG_EXPERIMENTS_PORTLET`

```json
{ "entity": { "FEATURE_FLAG_EXPERIMENTS_PORTLET": false } }
```

A native JSON boolean, because the key is in `BOOLEAN_FEATURE_FLAGS`. No REST signature changes, no
new endpoint, no `@Schema` change — so no `openapi.yaml` regeneration is required for the resource
itself. (Regenerate and commit if any `@Operation`/`@Parameter` text is touched; it should not be.)

---

## Frontend side

### Enum entry

`core-web/libs/dotcms-models/src/lib/shared-models.ts` — `FeaturedFlags`:

```ts
FEATURE_FLAG_EXPERIMENTS_PORTLET = 'FEATURE_FLAG_EXPERIMENTS_PORTLET'
```

`LOAD_FRONTEND_EXPERIMENTS = 'FEATURE_FLAG_EXPERIMENTS'` (`:28`) stays exactly as it is: still
declared, still zero consumers, and it never gains one under D1. Dropping it is #37008's.

### Read contract

```ts
inject(DotPropertiesService)
    .getFreshFeatureFlag(FeaturedFlags.FEATURE_FLAG_EXPERIMENTS_PORTLET)
    .pipe(catchError(() => of(false)))
```

| Aspect | Behavior | Requirement |
|---|---|---|
| Value `false` (shipped default) | legacy per-page screens | FR-013, FR-016..019 |
| Value `true` | new portlet | FR-021 |
| Property unset **and** key absent from `WHITE_LIST` | reads **on** — which is why both edits above are mandatory | FR-013 |
| Read fails (network, 500, malformed) | `false` — legacy behavior | FR-015 |
| Operator flips it mid-session | next read picks up the new value; navigations already in flight are unaffected | FR-012, SC-002, and the spec's mid-session edge case |

`getFreshFeatureFlag` (uncached) rather than `getFeatureFlag` (process-lifetime cache) so an
operator's flip takes effect on the next gesture rather than the next hard reload — SC-002's
"under one minute … without a deployment or restart". Same reasoning, same reader, as
`dotAiConfigDetailMatchGuard`.

`catchError` at the call site, **not** in `DotPropertiesService`: changing the service's error
behavior would change it for every existing consumer, which is outside this work's scope.

---

## Reversibility

| | |
|---|---|
| Off → on | set the property (Maintenance, env var, or `dotmarketing-config.properties`) |
| On → off | unset or set `false` |
| Redeploy needed | no, in either direction (FR-012) |
| Restart needed | no |
| Rollback-unsafe? | no — no schema, no index, no API contract, no serialized state |

Both destinations ship in the same build (FR-025), so flipping the switch selects an entry point
rather than swapping code.

---

## What the switch must NOT affect (FR-014)

Assertable as negatives, and each one gets a test:

- Experiments served to site visitors — governed by `FEATURE_FLAG_EXPERIMENTS` only.
- Experiment JS injection into rendered pages — same.
- Experiment resolution during page render — same.
- The Experiments portlet's presence in the main navigation (FR-026) — governed by the portlet's own
  registration.
- The variant round-trip's own behavior once the Configure screen is reached (FR-027).
