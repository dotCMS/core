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

Both index sets follow the same logical name pattern. The new index set (the one being introduced
by this migration) carries an explicit distinction tag — present at every layer, including the
service / API surface. The legacy index set has no tag. **Today the tag value is `.os`**, chosen
because the migration target happens to be OpenSearch; the value is centralized in `IndexTag` and
can change in the future without touching the rest of the architecture.

**Two distinct name layers** — keep them separate:

- **Service / API layer (logical name)**: the form callers pass around in business code. It carries
  the tag if it belongs to the new index set, no tag if it belongs to the legacy set. **No cluster
  prefix.** Examples: `working_20230101` (legacy) · `working_20260406.os` (new).
- **Persistence layer (physical name)**: the form actually sent to a cluster or written to the
  `indicies` DB table. **Adds the `cluster_<id>.` prefix** on top of the logical name. Examples:
  `cluster_08abc3.working_20230101` (legacy) · `cluster_08abc3.working_20260406.os` (new).

The cluster prefix is an infrastructure detail added at the persistence boundary by
`getNameWithClusterIDPrefix` (called from inside `toPhysicalName`). Service-layer code can — and
should — work with logical names; the prefix appears only when a name is about to hit a cluster
or a DB write.

| Layer                                | Legacy name                         | Tagged name (today `.os`)                       |
|--------------------------------------|-------------------------------------|-------------------------------------------------|
| Service / API (logical name)         | `working_20230101`                  | `working_20260406.os`                           |
| Row in the `indicies` DB table       | `cluster_08abc3.working_20230101`   | `cluster_08abc3.working_20260406.os`            |
| Physical index in the cluster        | `cluster_08abc3.working_20230101`   | `cluster_08abc3.working_20260406.os`            |

The DB row and the cluster index are the **same string** (both physical names). `VersionedIndicesAPI`
load methods return the canonical physical form (with tag, with cluster prefix) — they do not
strip — so a name resolved from the store can be passed directly to any `OSIndexAPIImpl`,
`MappingOperationsOS`, or search call without further transformation.

#### Distinction tag as the canonical marker

The tag is the **explicit-distinction marker** on every physical name in the new index set. The
choice to make the distinction explicit (rather than relying on cluster separation alone) buys two
things:

1. **DB PK uniqueness** in the shared `indicies` table (`index_name` is the PK) — without an
   explicit marker, the legacy row and the new row for the same logical name would collide.
2. **Cluster name distinction** in single-cluster test profiles where `DOT_ES_ENDPOINTS ==
   OS_ENDPOINTS` (e.g. the `opensearch-upgrade` Maven profile). Without the tag, a Phase 1
   fan-out that writes the same logical name to both providers would hit `resource_already_exists`
   on the second write. With the tag, the new index coexists with the legacy index in the shared
   cluster.

**Tag value**: stored as a single constant in `IndexTag` (`IndexTag.OS.tag(name)` /
`IndexTag.OS.isTagged(name)` / `IndexTag.OS.strip(name)`). Changing the literal — for example
swapping `.os` for something else later — is a one-line change in the enum; no caller hardcodes
the suffix.

**Who applies the tag**:

- **`ContentletIndexOperationsOS.toPhysicalName(name)`** is the canonical entry point. It calls
  `getNameWithClusterIDPrefix` then `IndexTag.OS.tag()`. Idempotent via `IndexTag.OS.isTagged`.
- **`MappingOperationsOS.physicalName(index)`** applies the same transformation internally so
  that mapping operations resolve to the tagged name even when callers pass a logical name.
- **`VersionedIndicesAPIImpl.saveIndices`** applies `IndexTag.OS.tag()` idempotently before
  INSERT as a belt-and-suspenders guard — by the time `saveIndices` is reached, names are already
  tagged in the normal production flow.

**Who does NOT strip the tag**:

- `VersionedIndicesAPI` load methods (`loadIndices`, `loadDefaultVersionedIndices`, `loadAllIndices`,
  `loadNonVersionedIndices`) return the canonical tagged form. The cache holds the same.
- `OSIndexAPIImpl.getIndicesStats`, `listIndices`, `getClusterHealth` strip the `cluster_X.`
  prefix but **preserve the tag** in the returned keys. Callers comparing keys to logical names
  must apply `IndexTag.OS.tag()` to the comparand or strip the tag from the keys explicitly via
  `IndexTag.OS.strip()`.

**Idempotency contract**: every public method on the new-index-set layer accepts any of:

- Logical name without tag (`working_20240101`) — at the service layer, means "no specific provider,
  let the dispatch decide".
- Logical name with tag (`working_20240101.os`) — at the service layer, means "this belongs to the
  new index set".
- Physical name with cluster prefix, no tag (`cluster_X.working_20240101`) — already half-resolved.
- Canonical physical with both prefix and tag (`cluster_X.working_20240101.os`) — fully resolved.

All four flow to the cluster as the same canonical physical string after passing through
`toPhysicalName` / `physicalName`. Idempotency is guaranteed by the `isTagged` and `hasClusterPrefix`
guards inside those methods.

**Why suffix, not prefix**: the logical name (`cluster_{id}.{type}_{timestamp}`) is fully readable
without any leading marker. Stripping is `name.substring(0, len - tag.length())` and detection is
`name.endsWith(tag)`. Collision-free guarantee for the current `.os` value: logical names always
end in `_YYYYMMDDHHMMSS` (numeric) — they can never naturally end in `.os`. A future tag value
must preserve this property (no overlap with the logical-name grammar).

#### The tag is part of the name identity — never strip it on return

A tagged name like `working_20240101.os` **is** the canonical identity of that index — not a
decorated form of `working_20240101`. They are two different indices on two different providers,
and the base name (everything before the tag) matches the ES counterpart by construction, so the
tag is the *only* thing that distinguishes them. Any method that **returns or accepts an index
name** MUST preserve the tag end-to-end:

- **Read getters** — `ContentletIndexAPIImpl.getCurrentIndex()`, `getNewIndex()`, and
  `getActiveIndexName()` return the tagged name verbatim in Phases 2/3 (they strip only the
  `cluster_X.` prefix). A caller that receives `working_20240101.os` knows unambiguously it is
  the OS index; one that receives `working_20240101` is talking to ES. Stripping the tag here
  would erase the only discriminator and reintroduce the ES/OS collision in any `Set<String>` or
  `Map<String, ?>` keyed by index name.
- The same applies to provider methods (`getIndicesStats`, `listIndices`, `getClusterHealth`):
  returned keys keep the tag (as documented above).

**The one legitimate strip — deriving the embedded timestamp.** Some methods extract the
`_YYYYMMDDHHMMSS` value out of a name (`elapsedSinceIndexCreated`,
`VersionedIndicesAPIImpl.extractTimestamp`, the `indexSuffixOS` computation in `initOSCatchup`).
The timestamp parser cannot consume a trailing `.os`, so these MUST strip the tag **locally,
before parsing**: `IndexTag.strip(name).substring(name.lastIndexOf('_') + 1)`. This is *not* a
contradiction of the rule above — the strip is applied to a throwaway local used to parse a
number; the name that is returned or stored keeps its tag. Rule of thumb: **strip to parse a
value out of a name, never to hand a name back.**

#### Tag manipulation is the sole responsibility of `IndexTag`

All read/write of the vendor marker on an index name MUST go through the `IndexTag` enum.
`IndexTag` is the only place that knows the literal value (`.os`), whether it is a prefix or a
suffix, and the idempotency rules around it. Any code outside `IndexTag` that handles the marker
directly — even a thin wrapper or a one-liner — is a bug.

| Operation             | ✅ Correct                                                | ❌ Incorrect                                                       |
|-----------------------|----------------------------------------------------------|--------------------------------------------------------------------|
| Apply the marker      | `IndexTag.OS.tag(name)`                                  | `name + ".os"`                                                     |
| Strip the marker      | `IndexTag.OS.untag(name)` / `IndexTag.strip(name)`       | `name.substring(0, name.length() - 3)` / `name.replace(".os","")` |
| Detect the marker     | `IndexTag.OS.isTagged(name)`                             | `name.endsWith(".os")`                                             |
| Identify the vendor   | `IndexTag.resolve(name)` / `IndexTag.vendorOf(name)`     | `name.contains(".os") ? OS : ES`                                   |

This rule applies at every layer — providers, routers, mapping helpers, factories, integration
tests, and debug utilities. A helper method in another class that re-implements `tag()` / `strip()`
/ `isTagged()` (even faithfully) is still a violation: the moment the literal or the
prefix/suffix position changes in `IndexTag`, that helper becomes silently wrong. There are no
"performance shortcuts" or "convenience wrappers" worth the divergence risk.

**Why this is strict, not advisory**: the tag is a load-bearing migration artifact. The codebase
will eventually want to either change its value (e.g. swap `.os` for something neutral) or remove
it entirely (see "Future: tag retirement after ES decommission" below). Both are one-line edits
inside `IndexTag` — but only if no other class has taken on the responsibility. Every direct
string operation on the marker outside `IndexTag` is a hidden coupling that turns a one-line
change into a codebase-wide search-and-replace.

#### Future: tag retirement after ES decommission

The tag exists to solve concrete problems that only apply while both index sets coexist (PK
uniqueness in the shared `indicies` table, cluster name distinction in single-cluster test
profiles, routing between two providers). Once ES is decommissioned (Phase 3 complete, no
rollback window) these problems disappear and the tag becomes purely cosmetic.

Retiring the tag is **not free** — there are three viable strategies, none zero-cost:

1. **Leave it in place.** Treat `.os` as a historical marker on all existing index names. Zero
   ongoing cost; mildly confusing for readers without context.
2. **One-time rename migration.** For each OS index, reindex into a non-tagged name (OpenSearch
   has no direct rename — the standard path is reindex + alias swap + delete). Update the
   corresponding `indicies` rows. Comparable in scope and cost to a full OS reindex.
3. **Configure the tag to `""` in `IndexTag.OS`.** New writes stop adding the tag; existing
   indices keep theirs. The codebase must tolerate both forms indefinitely or until strategy 2
   is executed — usually the worst of both worlds.

The architecture is set up so that strategy 1 requires no action and strategy 3 is a one-line
change in `IndexTag`. Strategy 2 is a planned future migration tracked separately when the ES
decommission plan is finalized.

**Implication for present-day work**: do not hardcode the literal `.os` anywhere except in
`IndexTag.OS`. Comparisons must go through `IndexTag.OS.isTagged()` and `IndexTag.OS.strip()`,
so a future change of the literal — or its removal — does not require touching call sites.

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

Two semantically distinct operation types govern how index names determine routing. The dispatch
type determines whether the migration phase or a per-call vendor selector decides which provider
handles the operation — getting this wrong leads to writes landing in the wrong provider or
failing entirely.

### Phase-dispatched operations ("broadcast")

> **The migration phase decides which providers receive the call. The index name is a payload, not a routing key.**

Used for coordinated schema and data operations that must keep all active providers in sync:
- `putMapping` — mapping schema changes
- `addContentlet`, `removeContentlet` — content writes/deletes
- `createContentIndex` — index creation during bootstrap or reindex

**Rule**: pass an **untagged name** — logical (`working_20240101`) or cluster-prefixed
(`cluster_X.working_20240101`). Do **not** pass a tagged name (`working_20240101.os` or
`cluster_X.working_20240101.os`) into a fan-out — the presence of the tag is the signal that the
caller already knows which provider owns the name; route via tag-dispatch instead. See "Why
tagged names don't fan out" below.

`PhaseRouter.write()` / `router.writeChecked()` fan out to all write providers for the current
phase. The router class (`ContentletIndexAPIImpl`, `IndexAPIImpl`, …) calls
`provider.toPhysicalName(name)` on each provider before dispatch, so:

- Legacy provider receives `cluster_X.working_20240101` (no tag — its `toPhysicalName` only adds
  the cluster prefix).
- Tagged provider receives `cluster_X.working_20240101.os` (cluster prefix + tag, applied by
  `ContentletIndexOperationsOS.toPhysicalName`).

The name passed BY the caller is a payload — same string for both providers — and each provider's
own `toPhysicalName` localises it to the form that provider's cluster actually holds.

```java
// Correct — name is a payload; each provider's toPhysicalName builds its own form
router.write(impl -> impl.putMapping(impl.toPhysicalName(indexName), mapping));
```

#### Why tagged names don't fan out

A tagged service-layer name (`working_20240101.os` or its cluster-prefixed form) carries semantic
intent: the caller is referring to **a specific index in the new index set**. That index has no
automatic equivalent in the legacy index set because:

- In **fresh install**, both sets share the same logical name minus the tag — so the equivalence
  exists, but stripping the tag to fan out is only a coincidence of that scenario.
- In **migration catchup** (the majority of production customers), the legacy and new sets hold
  indices with **different timestamps** entirely — `working_20230101` (legacy) and
  `working_20260406.os` (new). There is no string transformation that maps one to the other;
  the legacy name has to be resolved from the legacy store directly.

So treating "strip the tag to fan out" as a general rule is incorrect — it breaks the catchup case.
The mapping between tagged and untagged names is a **store lookup**, not a string operation.

Mechanically, what happens today if you pass a tagged name to a fan-out call:

- **Tagged provider**: `toPhysicalName` is idempotent on the tag → name stays unchanged → operation
  hits the correct index. ✅
- **Legacy provider**: its `toPhysicalName` only adds the cluster prefix; it does **not** strip the
  tag (and shouldn't — see above). The tagged string passes through unchanged → operation hits
  `cluster_X.working_20240101.os` in the legacy cluster → that index does not exist → 404 /
  `index_not_found_exception`. ❌

A tagged name implies the caller already knows which provider owns it. That's the **tag-dispatched**
case, not fan-out — route it explicitly via `IndexTag` and skip the fan-out entirely (see next
section). Tagged names resolved from `versionedIndicesAPI.loadDefaultVersionedIndices()` must go
through tag-dispatched calls, not phase-dispatched fan-out.

### Tag-dispatched operations ("targeted")

> **The tag on the index name decides which provider handles the call. The phase is irrelevant.**

Used for direct operations against a specific provider's index: diagnostics, catchup creation,
provider-specific reads, or any operation where the caller already knows which index set owns the
name. A tagged name resolved from `versionedIndicesAPI.loadDefaultVersionedIndices()` is the
prototypical case.

**Rule**: use `IndexTag.resolve(name)` to select the provider, then pass the name through to the
provider as-is (or via `provider.toPhysicalName(name)` for cluster-prefix idempotency). Do **not**
strip the tag — the new index in the cluster carries the tag in its actual name; stripping would
point the request at a non-existent index.

```java
// Correct — tag decides provider; name flows through unchanged (the cluster index has .os in its name)
final IndexTag       owner  = IndexTag.resolve(indexName);   // legacy or new
final ContentletIndexOperations target =
        (owner == IndexTag.OS) ? osImpl : esImpl;
target.someOperation(target.toPhysicalName(indexName));      // toPhysicalName is idempotent on tag
```

Tag-dispatch is implemented inline in each router class using `IndexTag.resolve()` + direct
provider selection. `PhaseRouter` does not have dedicated tagged-routing methods — the routing
decision is explicit at the call site.

### IndexTag-parameter overload pattern

A lighter alternative to tag-dispatch when the **caller already holds an `IndexTag` value** (not a
tagged string in the index name itself). Instead of relying on the name to carry the routing
signal, pass it as an explicit enum parameter.

**When to use**: targeted catchup operations — e.g. creating or refreshing the mapping on only one
provider without affecting the other — where the caller has an untagged or logical name and
already knows which index set it should land on.

**Rules**:
- `IndexTag` must be the **last parameter** in any public method signature.
- The method resolves the provider from the enum value directly (`tag == IndexTag.OS ? osImpl : esImpl`).
- Inside the overload, do **not** manually call `IndexTag.OS.tag()` or `.strip()` on the index name
  strings. Let the selected provider's `toPhysicalName` (or `physicalName` for mapping ops) do the
  canonicalization — that is where tag application lives, and it is idempotent regardless of which
  form the caller passed in.
- The no-`IndexTag` overload must keep its original phase-dispatch behavior unchanged.
- The `IndexTag` enum value itself must not propagate below the routing layer (it must not reach
  `ESIndexAPI` or `IndexMappingRestOperationsOS`). The tag in the **name string**, on the other
  hand, is allowed to reach those layers — it is part of the canonical OS physical name.

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
| `working_20240101` (logical, no tag) | `router.write(…)` | Migration phase |
| `cluster_<id>.working_20240101` (physical, no tag) | `router.write(…)` | Migration phase |
| `working_20240101.os` (logical, tagged) | `IndexTag.resolve(name)` → select provider directly | Tag in the name |
| `cluster_<id>.working_20240101.os` (physical, tagged) | `IndexTag.resolve(name)` → select provider directly | Tag in the name |

The cluster prefix (`cluster_<id>.`) is orthogonal to the routing decision — it is just an
infrastructure detail of the persistence form. The tag is what determines the dispatch.

If the caller holds the routing intent as an `IndexTag` enum value (not in the name string), use
the **IndexTag-parameter overload** described above instead of any of the four rows in the table.

### Why both routing keys coexist

The two routing keys — migration phase and tag in the name — are not in tension; they cover
different caller situations:

- **Phase as routing key** is for callers that operate on a logical name with no knowledge of which
  index set should own the operation. Bulk content writes, schema updates, the user-triggered
  `createContentIndex` flow — these all express "synchronize the active providers for this name".
  The migration phase is the right signal because it encodes what "active providers" means today.
- **Tag in the name as routing key** is for callers that already hold a name resolved from a
  specific store (most commonly `versionedIndicesAPI.loadDefaultVersionedIndices()`). The tag in
  the name string IS the proof that the caller already knows which index set owns it — there is
  nothing for the migration phase to decide.

Treating the tag as a routing key would be a category error only if the tag were merely a DB
artifact invisible to service code. In this codebase the tag is **part of the canonical name at
every layer** (see "Distinction tag as the canonical marker"), so a tagged name in a caller's
hand carries genuine semantic intent — and routing by tag is the natural expression of that intent.

---

## Known Gotchas

- `match_phrase_prefix` behaves differently between ES and OS — flag for manual review
- `number_of_replicas` must always be set explicitly in OS index settings
- Index timestamp is part of the identity — never hardcode index names
- Working index is always a superset — never write to live without also writing to working

### Index name divergence between providers

**Fresh install** (Phases 1–3 from day zero): ES and OS indices are created in the same bootstrap
call. The base timestamps match; the OS name carries the `.os` tag on top — e.g.
`working_20260406` (ES) and `working_20260406.os` (OS). Divergence is limited to the tag.

**Migration catchup** (most production customers): An ES index already exists with its own
timestamp (e.g. `working_20230101`). When the OS shadow index is created later it gets a fresh
timestamp and the tag (e.g. `working_20260406.os`). **The two providers now hold indices whose
names differ in both the timestamp AND the tag.** No string transformation can convert one into
the other — the timestamps were generated independently.

This is why a name resolved from one provider cannot be passed unchanged to the other:

1. A name resolved from `legacyIndiciesAPI.loadIndicies()` (ES) — e.g. `working_20230101` — does
   not exist in OS, regardless of any tag manipulation.
2. A name resolved from `versionedIndicesAPI.loadDefaultVersionedIndices()` (OS) — e.g.
   `working_20260406.os` — does not exist in ES, even if the tag is stripped.

**Rule**: every public method on a class annotated with `@IndexRouter` that accepts an index name
must have an `IndexTag`-aware path so callers can target the correct provider when names diverge.
The plain (no-tag) overload retains its **phase-dispatched** behavior and is only safe when the
caller is passing a logical untagged name that is meant to apply to whichever providers the
current phase considers active (fresh install, coordinated reindex, or a freshly minted name the
caller has not yet resolved through a single-provider store).

```java
// Phase-dispatched — pass an untagged logical name; each provider applies its own toPhysicalName.
void someOperation(String indexName) {
    router.write(impl -> impl.someOperation(impl.toPhysicalName(indexName)));
}

// Tag-dispatched — caller already holds a tagged or provider-resolved name; route by tag.
void someOperation(String indexName, IndexTag tag) {
    final ContentletIndexOperations ops =
            tag == IndexTag.OS ? router.osImpl() : router.esImpl();
    ops.someOperation(ops.toPhysicalName(indexName));
}
```

**How to get the correct name per provider:**

- **ES**: `legacyIndiciesAPI.loadIndicies()` returns untagged names like `working_20230101`. Pair
  with `IndexTag.ES` (or call the ES provider directly).
- **OS**: `versionedIndicesAPI.loadDefaultVersionedIndices()` returns canonical tagged names like
  `working_20260406.os`. The tag is already part of the string, so `IndexTag.resolve(name)`
  recovers it without a separate parameter — see "Tag-dispatched routing" above for the call
  shape.

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

**Status: routing/noise semantics resolved for `ContentletIndexAPIImpl.delete()` (#36423);
primary-failure propagation for `delete()` tracked separately in #36430; still open for other
fan-out methods.**

When a public method on an `@IndexRouter`-annotated class accepts an index name and the current
migration phase requires fan-out to both providers, the supplied name may exist in one provider
but not the other. This is highly likely in production: ES and OS do **not** always hold indices
with the same logical name (see "Index name divergence between providers" above). An ES-resolved
name passed to an OS fan-out will produce a 404 or provider-level exception.

**Decided semantics for `delete()`** (QA #36219, TC-041):

- An explicitly **tagged name is tag-dispatched** to its owning provider and never fanned out
  (per "Why tagged names don't fan out" above).
- For a bare-name fan-out, the **shadow leg skips** names its engine does not hold (exists-check)
  and logs the skip through the shadow-write policy (`DOTCMS_SHADOW_WRITE_LOG_LEVEL`, default
  WARN) — an expected divergent-name miss is not an ERROR.
- Genuine shadow failures stay fire-and-forget (policy-level log); the primary leg still logs
  at ERROR. Surfacing primary failures to the *caller* (the `PhaseRouter.writeBoolean`
  contract: re-throw after all providers were called) is #36430.
- Covered in `OpenSearchUpgradeSuite` by `ContentletIndexAPIImplMigrationIntegrationTest`
  (name only in ES, only in OS via tag-dispatch, and paired; name-in-neither propagation
  coverage lands with #36430).

**Still open for other fan-out methods** (e.g. mapping and lifecycle operations):

- The same "expected miss vs. genuine failure" distinction has not been applied outside
  `delete()`; a 404 on OS may still be silently swallowed where it signals a caller bug rather
  than a transient cluster error.
- Verify that `loadProviderIndices` / `ProviderIndices` correctly returns `null` (skip) for a
  provider whose store has no record yet, rather than silently passing a stale or wrong name.

Until test coverage exists for those scenarios, treat other public `@IndexRouter` methods that
accept a raw index name string in dual-write phases as **untested for the name-mismatch case**.

---

## Testing

All tests related to this migration must be added to the **`OpenSearchUpgradeSuite`** test suite.
Never add migration tests to general test suites — keep them isolated and easy to run together.

---

## Deferred (lower priority)
- **Site Search** (`site-search` index) — in scope, but separate pipeline; will be addressed after core content index migration is stable
- **Full reindex orchestration for OS** — not viable at current scale; deferred until a targeted catchup strategy is defined
- **User-facing query routing during dual-write phase** — search queries are not yet phase-aware beyond the read provider selection in `PhaseRouter`