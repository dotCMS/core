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

## Shadow Index Strategy

During Phase 1, OpenSearch receives all writes in parallel to ES but no reads are served from it.
This allows real data to accumulate and consistency to be validated before enabling reads.

**Accepted limitation**: dual-write alone does not guarantee perfect sync between indices.
Adopted strategy: dual-write for ~2 weeks on low-volume customers → validate → activate Phase 2.

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

---

## Operations to Replicate in Shadow Index

| Operation                            | Replicate to OS? |
|--------------------------------------|------------------|
| Content write (publish/save)         | ✅ Yes            |
| Content delete                       | ✅ Yes            |
| Content-type create                  | ✅ Yes            |
| Content-type delete + content cleanup| ✅ Yes            |
| Permission update                    | ✅ Yes            |
| User-triggered reindex               | ❌ No             |
| User-triggered index shutdown        | ❌ No             |
| Site Search index operations         | ✅ Yes            |

---

## Design Rules

- **Never modify ES code** when adding the OS counterpart — zero changes to existing ES classes
- **Errors in OS are fire-and-forget** — a shadow index failure must never affect the business operation or the ES write
- **Routing lives in one place**: `IndexAPIImpl` (and its search/content equivalents) — never dispersed across callers
- **ES is always authoritative** during Phases 1 and 2 — its result is what gets returned to the caller

---

## Code Architecture

### Naming conventions
- ES classes keep their original names
- OS counterpart classes use the `OS` suffix — e.g. `ContentSearchRepositoryOS`
- Shared interfaces/abstractions must not carry `ES` or OS in their name
- Shared interfaces/abstractions must not carry `OS` in their name
- OpenSearch-specific classes must be placed under `com.dotcms.content.index.opensearch`
- General purpose index classes must be placed under `com.dotcms.content.index`

### `@IndexRouter` annotation
Marks the class where the routing decision lives — which index receives a given request.
There must be **one and only one** routing point per functional area. Never duplicate this logic in callers.

### Package structure
OS classes live in the same package as their ES counterparts.

---

## Index Operation Dispatch Model

Two semantically distinct operation types govern how index names determine routing.
Getting this wrong is the root cause of the class of bugs where `os::` prefixed names
reach `ESIndexAPI` and produce malformed index names like `cluster_<id>.os::cluster_<id>.working_...`.

### Phase-dispatched operations ("broadcast")

> **The migration phase decides which providers receive the call. The index name is a payload, not a routing key.**

Used for coordinated schema and data operations that must keep all active providers in sync:
- `putMapping` — mapping schema changes
- `addContentlet`, `removeContentlet` — content writes/deletes
- `createContentIndex` — index creation during bootstrap or reindex

**Rule**: pass a **plain logical name** (`working_20240101`) or a **cluster-prefixed physical name**
(`cluster_<id>.working_20240101`). Never pass an `os::`-tagged name.

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

**Rule**: keep the `os::` tag on the name. Use `IndexTag.resolve(name)` to select the provider,
then strip the tag **inside the call** (not before routing).

```java
// Correct — tag decides provider, strip happens inside
final IndexTag vendor  = IndexTag.resolve(indexName);   // OS or ES
final T         target = (vendor == IndexTag.OS) ? osImpl : esImpl;
target.someOperation(IndexTag.strip(indexName));
```

`PhaseRouter` exposes `readTagged` / `writeTagged` / `writeTaggedChecked` for this pattern.

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
`ESMappingUtilHelper#addCustomMapping(List, IndexTag)`,
`ESMappingUtilHelper#addCustomMapping(Field, List, IndexTag)`.

### Decision rule

| Name format | Correct router method | Routing key |
|---|---|---|
| `working_20240101` (plain logical) | `router.write(…)` | Migration phase |
| `cluster_<id>.working_…` (physical, no tag) | `router.write(…)` | Migration phase |
| `os::cluster_<id>.working_…` (vendor-tagged) | `router.writeTagged(…)` | Vendor tag |

### Why stripping early is wrong

Both `cluster_<id>.working_20240101` (ES) and `os::cluster_<id>.working_20240101` (OS) strip to
the identical physical name `cluster_<id>.working_20240101`. If the tag is stripped before routing,
the discriminator is lost and a phase-dispatch fan-out would reach both providers when only one
was intended.

**Strip the tag inside the provider implementation, never before the routing decision.**

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

## Out of Scope (for now)
- Site Search (`site-search` index) — separate pipeline, separate decision
- Full reindex orchestration for OS
- User-facing query routing during dual-write phase