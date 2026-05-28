# ES → OpenSearch Migration

## Executive Summary

Migrate dotCMS indexing infrastructure from ElasticSearch to OpenSearch **transparently**.
No downtime, no data loss, no visible behavior change for the end user.

---

## Migration Phases

Controlled via feature flag: `FEATURE_FLAG_OPEN_SEARCH_PHASE`

| Phase | Code Name                      | Writes   | Reads   | ES Status         |
|-------|--------------------------------|----------|---------|-------------------|
| 0     | PHASE_0_MIGRATION_NOT_STARTED  | ES only  | ES only | active            |
| 1     | PHASE_1_DUAL_WRITE_ES_READS    | ES + OS  | ES only | active            |
| 2     | PHASE_2_DUAL_WRITE_OS_READS    | ES + OS  | OS only | active (fallback) |
| 3     | PHASE_3_OPENSEARCH_ONLY        | OS only  | OS only | decommissioned    |

---

## Phase Transitions

Phases are advanced **manually** by changing the value of `FEATURE_FLAG_OPEN_SEARCH_PHASE`
in `dotmarketing-config.properties` (or the equivalent environment variable).
The system reads the flag at startup and on each routing decision — no restart is required
for the change to take effect, but all nodes in the cluster must be updated consistently.

| Transition      | Precondition                                                              |
|-----------------|---------------------------------------------------------------------------|
| Phase 0 → 1     | OS cluster reachable; OS index created via bootstrap or catchup           |
| Phase 1 → 2     | OS data quality validated; mapping schemas must be identical (see Rollback risk) |
| Phase 2 → 3     | OS confirmed stable under production read load; ES decommission approved  |

> There is no automated promotion. Advancing a phase is a deliberate operator action.

---

## Shadow Index Strategy

### Definition

The **shadow index** is the OpenSearch index during the dual-write phases (1 and 2).
It receives every write that goes to ES, acting as a continuously-updated replica.
It is called "shadow" because it follows ES writes rather than owning them.

This is a **transitional state**: once Phase 3 is reached, the shadow index is promoted
to the **primary index** — OS becomes the source of truth and ES is decommissioned.

### Phase lifecycle

| Phase | OS write role | OS read role    | Write failure    | Read failure          |
|-------|---------------|-----------------|------------------|-----------------------|
| 0     | absent        | absent          | —                | —                     |
| 1     | shadow        | absent (ES)     | fire-and-forget  | n/a                   |
| 2     | shadow        | **primary**     | fire-and-forget  | fallback to ES        |
| 3     | **primary**   | **primary**     | propagates       | propagates            |

### Fire-and-forget writes (Phases 1 and 2)

When OS is a shadow, a write failure must **never** affect the ES write or the caller:

- The OS failure is **logged** (warn-level) for observability
- The reindex queue entry is **not** marked as failed
- No `BulkProcessor` rebuild is triggered
- The caller receives a successful result based on ES

Rationale: ES remains the source of truth through Phases 1 and 2. An OS failure is a
consistency concern, not a data-loss event — ES still holds the authoritative state.

### Read fallback (Phase 2 only)

In Phase 2 OS serves reads but ES is still active. If OS throws an exception on a read,
`PhaseRouter` catches it, logs at **ERROR** level, and retries against ES automatically.

- The caller receives a correct result from ES
- The ERROR log makes the OS failure visible for operators
- In Phase 3 there is no fallback — ES is decommissioned and OS failures propagate normally

**Limitation**: the fallback only activates on exceptions. If OS has stale data from a
prior write failure (no exception, just wrong data), the fallback does not trigger — the
caller receives the stale OS data. This is the accepted residual risk of the shadow strategy.

### Implementation

The `shadow` flag on `CompositeBulkProcessor.Entry` carries these semantics.
`BulkProcessorListener.forShadowProvider()` creates a listener that enforces the
fire-and-forget contract: it logs failures but never calls `handleFailure()` on the
reindex queue and never triggers a rebuild.

```
Phase 0 / 3  →  isDualWrite = false  →  shadow = false  →  failures propagate
Phase 1 / 2  →  isDualWrite = true, ops == operationsOS  →  shadow = true  →  fire-and-forget
```

### Accepted limitation

Dual-write alone does not guarantee perfect sync between indices. Adopted strategy:
dual-write for ~2 weeks on low-volume customers → validate → activate Phase 2.

Full reindex on OS is not viable at scale (up to 1000 sites × 100k content pieces per customer).

---

## Index Architecture

### Two indices per cluster
- **Live index**: Published content only
- **Working index**: Superset of live — includes drafts and unpublished versions

### Index naming convention
```
{cluster_prefix}.{live|working}_{timestamp}

# Examples
cluster_08abc3567e.live_20260305193221
cluster_08abc3567e.working_20260305193221
```

Both ES and OS follow the same logical name pattern. The difference is only at the DB storage layer (PK uniqueness):

| Layer                         | ES name                             | OS name                                         |
|-------------------------------|-------------------------------------|-------------------------------------------------|
| Stored in DB                  | `cluster_08abc3.working_20230101`   | `cluster_08abc3.working_20260406.os`            |
| Everywhere else (all callers) | `cluster_08abc3.working_20230101`   | `cluster_08abc3.working_20260406`               |

#### `.os` suffix ownership rule

The `.os` suffix is a **DB uniqueness artifact** — it prevents primary-key collisions between ES
and OS rows in the shared `indicies` table (`index_name` is the PK). It is **exclusively managed
by `VersionedIndicesAPIImpl`**:

- **`saveIndices`** always appends `.os` before writing (idempotent via `IndexTag.OS.tag()`).
- **All load methods** (`loadIndices`, `loadAllIndices`, `loadNonVersionedIndices`) always strip
  the `.os` suffix before returning — the cache also stores stripped names.

**No other class in the codebase adds or removes the `.os` suffix.**
Callers of `VersionedIndicesAPI` always receive clean, client-ready names.
`toPhysicalName` on both ES and OS providers produces `cluster_{id}.name` — no suffix involved.

**Why suffix, not prefix**: The logical name (`cluster_{id}.{type}_{timestamp}`) is fully readable
without any leading marker. Stripping is `name.substring(0, len - 3)` and detection is
`name.endsWith(".os")`. Collision-free guarantee: logical names always end in `_YYYYMMDDHHMMSS`
(numeric) — they can never naturally end in `.os`.

### What lives in the index
| Content                    | Live | Working |
|----------------------------|------|---------|
| Published content          | ✅   | ✅      |
| Draft/unpublished content  | ❌   | ✅      |
| Content types              | ✅   | ✅      |
| Permission representations | ✅   | ✅      |

### Document structure
- Core fields: `identifier`, `inode`, `basetype`, `contenttype`, `languageid`, `variant`
- Status fields: `live`, `working`, `deleted`, `locked`
- Permission fields: `permissions` (whitespace-analyzed text)
- Workflow fields: `wfstep`, `wfscheme`, `wfcurrentstepname`
- Dynamic fields: `*_dotraw` (keyword), `*latlon` (geo), `*_text` (text)
- Content-type specific fields: nested under type name (e.g. `htmlpageasset.url`)

---

## Write Pipeline

### How writes work
- All writes go through a **queue-based system** backed by `dist_reindex_journal` table
- A worker picks up a unit of work and writes/deletes from the index
- **Writes always happen after a DB commit** to guarantee DB ↔ index consistency

### Write triggers
| Trigger                    | Live            | Working         |
|----------------------------|-----------------|-----------------|
| Content published          | ✅ write        | ✅ write        |
| Content saved (draft)      | ❌              | ✅ write        |
| Content deleted            | ✅ delete       | ✅ delete       |
| Content-type created       | ✅ write        | ✅ write        |
| Content-type deleted       | ✅ delete all   | ✅ delete all   |
| Permissions modified       | ✅ update       | ✅ update       |
| Full reindex triggered     | rebuilds index  | rebuilds index  |

### Full reindex behavior
- Creates a new index copy in the background
- Inserts all rows from `contentlet_version_info` into `dist_reindex_journal`
- Keeps the current index live during the process
- **OS is excluded** — a user-triggered full reindex only rebuilds the ES index.
  See [Operations to Replicate in Shadow Index](#operations-to-replicate-in-shadow-index).

---

## Operations to Replicate in Shadow Index

| Operation                            | Replicate to OS? | Notes                                                        |
|--------------------------------------|------------------|--------------------------------------------------------------|
| Content write (publish/save)         | ✅ Yes            |                                                              |
| Content delete                       | ✅ Yes            |                                                              |
| Content-type create                  | ✅ Yes            |                                                              |
| Content-type delete + content cleanup| ✅ Yes            |                                                              |
| Permission update                    | ✅ Yes            |                                                              |
| User-triggered reindex               | ❌ No             | Full reindex at OS scale is not viable — see Accepted Limitation |
| User-triggered index shutdown        | ❌ No             | Lifecycle ops on the shadow index are not user-controllable  |
| Site Search index operations         | ✅ Yes (deferred) | In scope, lower priority than core content index            |

---

## Design Rules

- **Never modify ES code** when adding the OS counterpart — zero changes to existing ES classes
- **OS write failures are fire-and-forget** in Phases 1 and 2 — a shadow write failure must never affect the business operation or the ES write
- **OS read failures fall back to ES** in Phase 2 — `PhaseRouter` catches the exception, logs at ERROR, and retries against ES
- **Routing lives in one place**: `IndexAPIImpl`, `ContentletIndexAPIImpl`, and their search equivalents — never dispersed across callers
- **ES is the authoritative write store** during Phases 1 and 2 — OS is the read source in Phase 2 but ES holds the canonical state for recovery

---

## Code Architecture

### Naming conventions
- ES classes keep their original names
- OS counterpart classes use the `OS` suffix — e.g. `ContentSearchRepositoryOS`
- Shared interfaces/abstractions must not carry `ES` or `OS` in their name
- OpenSearch-specific classes must be placed under `com.dotcms.content.index.opensearch`
- General purpose index classes must be placed under `com.dotcms.content.index`

### `@IndexRouter` annotation
Marks the class where the routing decision lives — which index receives a given request.
There must be **one and only one** routing point per functional area. Never duplicate this logic in callers.

---

## Index Operation Dispatch Model

Two semantically distinct operation types govern how index names determine routing.
Getting this wrong is the root cause of the class of bugs where DB-suffixed names
(e.g. `cluster_<id>.working_….os`) leak out of `VersionedIndicesAPIImpl` and reach
`ESIndexAPI`, producing malformed physical names.

### Phase-dispatched operations ("broadcast")

> **The migration phase decides which providers receive the call. The index name is a payload, not a routing key.**

Used for coordinated schema and data operations that must keep all active providers in sync:
- `putMapping` — mapping schema changes
- `addContentlet`, `removeContentlet` — content writes/deletes
- `createContentIndex` — index creation during bootstrap or reindex

**Rule**: pass a **plain logical name** (`working_20240101`) or a **cluster-prefixed physical name**
(`cluster_<id>.working_20240101`). Never pass a DB-suffixed name (`cluster_<id>.working_….os`).

`PhaseRouter.write()` / `router.writeChecked()` fan-out to all write providers for the current phase.
Each provider's `getNameWithClusterIDPrefix()` handles its own physical name construction.

```java
// Correct — plain name, phase decides who gets it
router.write(impl -> impl.putMapping(physicalName(indexName), mapping));
```

### Tag-dispatched operations ("targeted")

> **The vendor tag on the index name decides which provider handles the call. The phase is irrelevant.**

Used for direct operations against a specific provider's index: diagnostics, catchup creation,
provider-specific reads, or any operation where the caller already knows which backend owns the index.

**Rule**: keep the `.os` suffix on the name. Use `IndexTag.resolve(name)` to select the provider,
then strip the suffix **inside the call** (not before routing).

```java
// Correct — tag decides provider, strip happens inside
final IndexTag vendor  = IndexTag.resolve(indexName);   // OS or ES
final T         target = (vendor == IndexTag.OS) ? osImpl : esImpl;
target.someOperation(IndexTag.strip(indexName));
```

Tag-dispatch is implemented inline in each router class using `IndexTag.resolve()` + direct provider selection. `PhaseRouter` does not have dedicated tagged-routing methods — the routing decision is explicit at the call site.

### IndexTag-parameter overload pattern

A lighter alternative to tag-dispatch when the **caller already holds an `IndexTag` value** (not a
tagged string). Instead of embedding the vendor in the index name, pass it as an explicit parameter.

**When to use**: targeted catchup operations — e.g. creating or refreshing the mapping on only one
provider without affecting the other.

**Rules**:
- `IndexTag` must be the **last parameter** in any public method signature.
- The method resolves the provider from the enum value directly (`tag == IndexTag.OS ? osImpl : esImpl`).
  Do **not** use `IndexTag` to tag or parse the index name strings — they stay plain throughout.
- The no-`IndexTag` overload must keep its original phase-dispatch behavior unchanged.
- `IndexTag` must not propagate below the routing layer (e.g. must not reach `ESIndexAPI` or
  `IndexMappingRestOperationsOS`).

```java
// Correct — IndexTag selects the provider; index names stay plain
public boolean putMapping(List<String> indexes, String mapping, IndexTag tag) throws IOException {
    final IndexMappingRestOperations ops =
            tag == IndexTag.OS ? router.osImpl() : router.esImpl();
    boolean result = true;
    for (final String index : indexes) {
        result &= ops.putMapping(CollectionsUtils.list(index), mapping);
    }
    return result;
}
```

When the operation requires a cascade of private helper methods, thread the `IndexTag` down to the
leaf write calls only. Pure computation methods (JSON building, field resolution) must not receive it.

```java
// Public entry point — IndexTag last, List<String> instead of varargs
public void addCustomMapping(List<String> indexes, IndexTag tag) {
    final String[] arr = indexes.toArray(String[]::new);
    addCustomMappingForRelationships(mappedFields, tag, arr); // tag threaded down
    putContentTypeMapping(contentType, mappingForFields, tag, arr); // leaf write
}

// Leaf write — calls the targeted putMapping overload
private void putContentTypeMapping(ContentType ct, Map<String,JSONObject> fields,
        IndexTag tag, String... indexes) throws JSONException, IOException {
    esMappingAPI.putMapping(CollectionsUtils.list(indexes), json, tag);
}
```

**Existing implementations**: `ContentletIndexAPIImpl#createContentIndex(String, int, IndexTag)`,
`ESMappingAPIImpl#putMapping(List, String, IndexTag)`,
`MappingHelper#addCustomMapping(List, IndexTag)`,
`MappingHelper#addCustomMapping(Field, List, IndexTag)`.

### Decision rule

| Name format | How to route | Routing key |
|---|---|---|
| `working_20240101` (plain logical) | `router.write(…)` | Migration phase |
| `cluster_<id>.working_…` (physical, no tag) | `router.write(…)` | Migration phase |
| `cluster_<id>.working_….os` (vendor-tagged, DB form) | `IndexTag.resolve(name)` → select provider directly | Vendor tag |

### Why the tag is not a routing key

Under the current design the `.os` DB suffix never appears outside of `VersionedIndicesAPIImpl`.
All routing decisions use an **explicit `IndexTag` parameter** passed by the caller — never the
presence or absence of a vendor suffix in an index name string.

The tag-dispatch pattern (reading the `.os` suffix via `IndexTag.resolve()`) remains valid for
hypothetical future use cases, but no production code path currently relies on it.

---

## Known Gotchas

- `match_phrase_prefix` behaves differently between ES and OS — flag for manual review
- `number_of_replicas` must always be set explicitly in OS index settings
- Index timestamp is part of the identity — never hardcode index names
- Working index is always a superset — never write to live without also writing to working

### Index name divergence between providers

**Fresh install** (Phases 1–3 from day zero): ES and OS indices are created in the same bootstrap
call sharing the same timestamp. Their logical names are identical, so fan-out operations that pass
the same name to both providers work correctly.

**Migration catchup** (most production customers): An ES index already exists with its own
timestamp (e.g. `working_20230101`). When the OS shadow index is created later it gets a different
timestamp (e.g. `working_20260406`). **The two providers now hold indices with different logical
names.** Any operation that:

1. Resolves the active index name from the default (ES) provider, and
2. Passes that name unchanged to a fan-out or to the OS provider

will fail on OS with a 404 or exception because the index name from ES does not exist in OS.

**Rule**: every public method on a class annotated with `@IndexRouter` that accepts an index name
must have an `IndexTag`-overloaded version so callers can target the correct provider when names
diverge. The no-tag overload retains its original phase-dispatch behavior and should be used only
when the caller is certain both providers share the same index name (e.g. fresh install,
coordinated reindex).

```java
// Fan-out — safe only when both providers share the same index name
void someOperation(String indexName) {
    router.write(impl -> impl.someOperation(impl.toPhysicalName(indexName)));
}

// Targeted — use when index names may differ between providers
void someOperation(String indexName, IndexTag tag) {
    final ContentletIndexOperations ops =
            tag == IndexTag.OS ? router.osImpl() : router.esImpl();
    ops.someOperation(ops.toPhysicalName(indexName));
}
```

**How to get the correct name per provider**: load the active index from the store that owns that
provider — `legacyIndiciesAPI.loadIndicies()` for ES, `versionedIndicesAPI.loadDefaultVersionedIndices()`
for OS — then pass the resolved name together with the matching `IndexTag` to the targeted overload.

### Rollback risk during dual-write phases

**Context:** In phases 1 and 2 both ES and OS receive mapping writes. If a rollback to N-1 occurs
while the system is in a dual-write phase, N-1 no longer fans out to OS — but OS retains every
mapping that N pushed. The OS index silently drifts ahead of ES.

**Impact:** Benign for ES (which N-1 reads and writes). Invisible in phase 0 rollbacks. Becomes
critical only if the system is later re-upgraded to phase 2 or 3 without first resyncing OS, because
OS would serve stale or inconsistent data.

**Mitigation options (in order of cost):**

1. **Runbook** — document explicitly: *"If rolling back during Phase 1 or 2, trigger a full reindex
   against OS before re-activating the migration."*

2. **Startup drift detection** — in `IndexStartupValidator`, when starting in Phase 1 or 2, compare
   the field mappings of the active ES and OS working indices and log a `WARN` for any divergent
   fields. Does not block startup.

3. **Phase gate validation** — before promoting Phase 1 → 2 (switching reads to OS), assert that ES
   and OS mapping schemas are identical. Reject the promotion if they are not.

Option 2 is the recommended minimum: it makes drift observable from the first restart after a
rollback, with no impact on normal operation.

> **Status**: Option 1 (runbook) is the only mitigation currently in place. Option 2 and 3 are
> not yet implemented — tracked as technical debt before Phase 2 goes to production.

---

### ⚠ Open issue — fan-out error handling with divergent index names

**Status: unresolved — test coverage needed.**

When a public method on an `@IndexRouter`-annotated class accepts an index name and the current
migration phase requires fan-out to both providers, there is no agreed-upon error handling strategy
for the case where the supplied name exists in one provider but not the other.

This is highly likely in production: ES and OS do **not** always hold indices with the same logical
name (see "Index name divergence between providers" above). An ES-resolved name passed to an OS
fan-out will produce a 404 or provider-level exception.

**The gap:** The `IndexTag`-overloaded pattern handles the *routing* decision, but not the *failure*
semantics when the wrong name is inadvertently passed to the wrong provider. Current code
fire-and-forgets OS errors in dual-write phases, which means a 404 on OS may be silently swallowed
even though it signals that the caller passed an incorrect index name rather than a transient
cluster error.

**What needs to happen before this is production-safe:**

- Define whether a "wrong index name" error on OS in dual-write phases should: (a) be swallowed
  like other OS shadow errors, (b) log at ERROR severity with a distinct message, or (c) propagate.
- Write test cases in `OpenSearchUpgradeSuite` that cover: fan-out with a name that exists only in
  ES, fan-out with a name that exists only in OS, and fan-out with a name that exists in neither.
- Verify that `loadProviderIndices` / `ProviderIndices` correctly returns `null` (skip) for a
  provider whose store has no record yet, rather than silently passing a stale or wrong name.

Until test coverage exists for these scenarios, treat any public `@IndexRouter` method that accepts
a raw index name string in dual-write phases as **untested for the name-mismatch case**.

---

## Testing

All tests related to this migration must be added to the **`OpenSearchUpgradeSuite`** test suite.
Never add migration tests to general test suites — keep them isolated and easy to run together.

---

## Deferred (lower priority)
- **Site Search** (`site-search` index) — in scope, but separate pipeline; will be addressed after core content index migration is stable
- **Full reindex orchestration for OS** — not viable at current scale; deferred until a targeted catchup strategy is defined
- **User-facing query routing during dual-write phase** — search queries are not yet phase-aware beyond the read provider selection in `PhaseRouter`