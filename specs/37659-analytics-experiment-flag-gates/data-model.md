# Data Model: Feature Gates for Analytics and Experiment Endpoints

**Feature**: `issue-37659-analytics-experiment-flag-gates`
**Date**: 2026-09-30

No new database tables or Elasticsearch/OpenSearch index mapping changes are introduced by
this feature. The data model additions are purely in the Java response layer and the Angular
type definitions.

---

## New: `ExperimentsHealthView` (Java — response shape)

**Package**: `com.dotcms.rest.api.v1.experiments`
**File**: `ExperimentsHealthView.java`

Replaces the current `Map<String, Health>` return type of `ExperimentsResource.healthcheck()`.
Carries the existing health state plus the new tier fields.

| Field | Type | Present when | Description |
|-------|------|-------------|-------------|
| `health` | `ExperimentsAPI.Health` enum (`OK`, `NOT_CONFIGURED`, `CONFIGURATION_ERROR`) | Always | Existing health state — unchanged semantics |
| `tier` | `String` (`"limited"` or `"full"`) | Always | `"full"` only when `FEATURE_FLAG_EXPERIMENTS=true` AND App configured; `"limited"` otherwise |
| `freeExperimentUsed` | `Boolean` | When `tier="limited"`; `null` when `tier="full"` | `true` if any experiment globally is in `{RUNNING, SCHEDULED, ENDED}`; `false` otherwise |
| `warning` | `String` | Present when App not configured; absent otherwise | Fixed value `"ANALYTICS_DISABLED"` when App is absent |

**Validation rules**:
- `tier` is `"full"` iff `FEATURE_FLAG_EXPERIMENTS=true` AND `getAppSecrets(host).isNotEmpty()`.
- `freeExperimentUsed` is `null` when `tier="full"` (slot concept doesn't apply).
- `warning` is `"ANALYTICS_DISABLED"` whenever `getAppSecrets(host).isEmpty()`, regardless of flag state.

**State table** (four combinations):

| flag | App | tier | freeExperimentUsed | warning |
|---|---|---|---|---|
| true | configured | `"full"` | null | absent |
| false | configured | `"limited"` | true/false | absent |
| true | not configured | `"limited"` | true/false | `"ANALYTICS_DISABLED"` |
| false | not configured | `"limited"` | true/false | `"ANALYTICS_DISABLED"` |

---

## Updated: `HealthEntity` (TypeScript — analytics portlet)

**File**: `core-web/libs/portlets/dot-analytics/data-access/src/lib/types/analytics-api.types.ts`

Adds the new `health` field alongside the existing `available` field. The `available` field
is preserved for safe rollback and remains in the type definition; the mapping function will
prefer `health` when present.

```typescript
export interface HealthEntity {
    available?: string | boolean;    // legacy field — kept for rollback safety
    health?: 'OK' | 'NOT_CONFIGURED' | 'CONFIGURATION_ERROR';  // new field from FR-003a
}
```

---

## Updated: `ExperimentHealthResponse` (TypeScript — experiments data-access)

**File**: `core-web/libs/data-access/src/lib/dot-experiments/dot-experiments.service.ts`

The experiments service's health check response type gains the three new fields from
`ExperimentsHealthView`. The existing health check response interface (if one exists) is
extended; otherwise a new one is introduced:

```typescript
export interface ExperimentsHealthResponse {
    health: 'OK' | 'NOT_CONFIGURED' | 'CONFIGURATION_ERROR';
    tier: 'full' | 'limited';
    freeExperimentUsed?: boolean | null;
    warning?: 'ANALYTICS_DISABLED';
}
```

---

## Gate State — runtime entities (not persisted)

The following are runtime-evaluated states, not stored in the database.

### Analytics App Configuration State

Determined per-request by `ContentAnalyticsUtil.getAppSecrets(host)`:
- **Configured** — `getAppSecrets(host)` returns a non-empty map.
- **Not configured** — `getAppSecrets(host)` returns an empty map.

### Free Slot State

Determined per-`_start` call (limited mode only):
- **Slot used** — `experimentsAPI.list(ExperimentFilter.builder().statuses(Set.of(RUNNING, SCHEDULED, ENDED)).build(), systemUser).size() >= 1`.
- **Slot available** — the above list is empty.

### Feature Flag State

Read once at startup from `Config.getBooleanProperty("FEATURE_FLAG_EXPERIMENTS", false)`.
Held in `ConfigExperimentUtil.INSTANCE.featureFlagExperiments` (`AtomicBoolean`). Never
refreshed after startup — a restart is required for changes to take effect (FR-010).

---

## Response Shape Changes

### `GET /api/v1/experiments/health` — extended

**Before** (current):
```json
{ "entity": { "health": "OK" } }
```

**After** (with tier fields):
```json
{ "entity": { "health": "OK", "tier": "full", "freeExperimentUsed": null } }
```
```json
{ "entity": { "health": "OK", "tier": "limited", "freeExperimentUsed": false } }
```
```json
{ "entity": { "health": "NOT_CONFIGURED", "tier": "limited", "freeExperimentUsed": false, "warning": "ANALYTICS_DISABLED" } }
```

### `GET /api/v1/analytics/health` — new dotCMS-evaluated response

**Before** (raw CAEM proxy):
Forwarded whatever CAEM returned — shape varied.

**After** (dotCMS-evaluated, always consistent):
```json
{ "entity": { "health": "OK" } }
{ "entity": { "health": "NOT_CONFIGURED" } }
{ "entity": { "health": "CONFIGURATION_ERROR" } }
```
