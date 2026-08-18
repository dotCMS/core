# ES → OpenSearch Migration — Operator Runbook

> **Who this is for.** Support and Cloud engineers who have to take a real customer instance from
> Elasticsearch to OpenSearch, end to end, without having read a line of the Java. It assumes you can
> reach the customer's dotCMS admin UI, its configuration, its database, and both search clusters.
> It does **not** assume you know how the routing works internally.
>
> **What it is not.** This is not the architecture document and not the QA test plan. When you need
> the *why* behind a rule, follow the pointer to the companion doc — but you can run a migration
> without opening any of them.

**Companion documents**

| Document | When you need it |
|---|---|
| [`OPENSEARCH_MIGRATION.md`](OPENSEARCH_MIGRATION.md) | The architecture and design. Every rule in this runbook traces back to it. |
| [`OPENSEARCH_CLIENT_CONFIGURATION.md`](OPENSEARCH_CLIENT_CONFIGURATION.md) | Every `OS_*` connection property, its ES fallback and its default. |
| [`OPENSEARCH_MIGRATION_TESTER_GUIDE.md`](OPENSEARCH_MIGRATION_TESTER_GUIDE.md) | Running the local lab stack and exercising the migration by hand. |
| [`OPENSEARCH_MIGRATION_TEST_PLAN.md`](OPENSEARCH_MIGRATION_TEST_PLAN.md) | The numbered QA test cases. |
| [`SEARCH_API_MIGRATION.md`](SEARCH_API_MIGRATION.md) | The Java/VTL API changes a plugin developer must make. |

---

## Table of contents

**Part I — Understand what you are doing**
1. [What this migration actually is](#1-what-this-migration-actually-is)
2. [The vocabulary you need](#2-the-vocabulary-you-need)
3. [Why there are four phases](#3-why-there-are-four-phases)
4. [The mirror strategy: how the active index gets copied](#4-the-mirror-strategy-how-the-active-index-gets-copied)
5. [How one dotCMS talks to two engines at once](#5-how-one-dotcms-talks-to-two-engines-at-once)

**Part II — Work out what will break**

6. [Affected areas — the impact checklist](#6-affected-areas--the-impact-checklist)
7. [VTL templates and viewTools](#7-vtl-templates-and-viewtools)
8. [Lucene queries and the search REST endpoints](#8-lucene-queries-and-the-search-rest-endpoints)
9. [OSGi plugins and the removal of vendor types](#9-osgi-plugins-and-the-removal-of-vendor-types)
10. [What to do when you find a bug in a plugin](#10-what-to-do-when-you-find-a-bug-in-a-plugin)

**Part III — Prepare**

11. [OpenSearch server requirements and security criteria](#11-opensearch-server-requirements-and-security-criteria)
12. [What happens when dotCMS cannot reach OpenSearch](#12-what-happens-when-dotcms-cannot-reach-opensearch)
13. [Choosing a customer: selection and sizing criteria](#13-choosing-a-customer-selection-and-sizing-criteria)
14. [Rehearse on a clone before you touch the real environment](#14-rehearse-on-a-clone-before-you-touch-the-real-environment)
15. [The fast path: straight to Phase 3](#15-the-fast-path-straight-to-phase-3)

**Part IV — Execute**

16. [The readiness endpoint — your source of truth](#16-the-readiness-endpoint--your-source-of-truth)
17. [Setting the phase, and when a restart is mandatory](#17-setting-the-phase-and-when-a-restart-is-mandatory)
18. [Cluster considerations: node consistency and traffic](#18-cluster-considerations-node-consistency-and-traffic)
19. [The step-by-step migration](#19-the-step-by-step-migration)
20. [Site Search has its own rules](#20-site-search-has-its-own-rules)

**Part V — Recover**

21. [Downgrading to the previous phase](#21-downgrading-to-the-previous-phase)
22. [Emergency stop](#22-emergency-stop)
23. [Troubleshooting quick reference](#23-troubleshooting-quick-reference)

**Appendices**

- [A. Verification commands](#appendix-a--verification-commands)
- [B. Configuration reference](#appendix-b--configuration-reference)
- [C. Log lines worth recognizing](#appendix-c--log-lines-worth-recognizing)

---

# Part I — Understand what you are doing

## 1. What this migration actually is

dotCMS stores a searchable copy of every piece of content in a search engine. Historically that
engine is **Elasticsearch**. We are moving it to **OpenSearch 3.x**.

The design goal is that the customer never notices. No downtime, no data loss, no change in how
search behaves. To make that possible, dotCMS was built to run against **both engines at the same
time**: it can write every change to both, read from either, and fall back to the old one when the
new one misbehaves. A single setting decides how far along that spectrum an instance is.

That setting is the **migration phase**, and moving it is the entire job. Everything else in this
runbook is either *what to check before you move it* or *what to do when the move goes wrong*.

**Two things to internalise before anything else:**

- **A phase change never copies data retroactively.** Moving from Phase 0 to Phase 1 does not
  populate OpenSearch with the content that already exists. It only starts sending *new* writes
  there. Filling OpenSearch with the existing corpus is a separate, explicit action: a **full
  reindex**. Forgetting this is the single most common way a migration goes wrong.
- **The phase is not a progress bar; it is a routing switch.** Setting it to 3 does not mean the
  migration is done. It means "route everything to OpenSearch from now on". If OpenSearch is not
  ready, you have just pointed the customer at an empty index.

## 2. The vocabulary you need

| Term | What it means in practice |
|---|---|
| **Source engine / ES** | The engine the customer runs today. Elasticsearch, or an older OpenSearch 1.x. In configuration it is always the `ES_*` family of properties, whatever it physically is. |
| **Target engine / OS** | OpenSearch **3.x**, the engine we are migrating to. The `OS_*` family of properties. |
| **Index** | Where the engine stores content so it can be searched. dotCMS keeps two: `working` (everything, including drafts) and `live` (published only). |
| **Index name** | Carries a timestamp and, on the OpenSearch side, a marker. Physical example: `cluster_08abc3.working_20260406.os`. The `cluster_<id>.` prefix identifies the customer; the `_20260406…` part is when the index was created. |
| **The `.os` tag** | The suffix that distinguishes an OpenSearch index from its Elasticsearch counterpart. It is part of the real index name — you will see it in the cluster, in the `indicies` database table, and in the readiness report. **Never type it by hand; always copy a real name from a listing.** |
| **Migration phase** | A number, 0 to 3. Which engines receive writes, and which one answers searches. See §3. |
| **Dual-write** | Phases 1 and 2: every content change is written to *both* engines, so they stay in sync going forward. |
| **Shadow index / mirror** | The OpenSearch copy while Elasticsearch is still authoritative. It follows the Elasticsearch writes rather than owning them. |
| **Full reindex** | Rebuilding an index from the database, from scratch. The only operation that makes OpenSearch catch up with content that already existed. Expensive; plan a window. |
| **Counterpart** | The index on the other engine that mirrors this one. "Missing counterpart" means one engine has it and the other does not. |
| **Readiness endpoint** | `GET /api/v1/index/migration/readiness` — the report that tells you whether it is safe to move. See §16. |
| **`indicies` table** | The database table recording which indexes are active. OpenSearch rows end in `.os`. |

> **Note on the word "twin".** Older notes call the OpenSearch copy the "twin". This document says
> **counterpart**, **copy**, or **mirror**; they all mean the same thing.

## 3. Why there are four phases

You could imagine flipping a customer from Elasticsearch to OpenSearch in one step. We deliberately
do not, because a one-step cutover has no way back. Once you stop writing to Elasticsearch, it
starts going stale within seconds — and if OpenSearch then turns out to be misconfigured, slow, or
missing content, your rollback target is already out of date.

The four phases exist so that at every step there is **an engine you can still fall back to**, and so
that each risky change is made **one at a time**:

| Phase | Writes go to | Searches answered by | Elasticsearch is | What this step proves |
|:---:|---|---|---|---|
| **0** | ES only | ES | authoritative | Nothing yet — this is the starting state. |
| **1** | ES **+** OS | ES | authoritative | That dotCMS can *write* to OpenSearch: connectivity, credentials, permissions, mappings. Nothing the customer sees depends on OpenSearch. |
| **2** | ES **+** OS | **OS** | still written, used as fallback | That OpenSearch can *serve* the customer's real query load correctly. If a read fails, dotCMS silently retries against Elasticsearch. |
| **3** | **OS only** | **OS** | decommissioned | The cutover. No fallback remains. |

Read down the middle column: **writes move first, then reads, then the old engine is dropped.** Each
phase changes exactly one of those.

**Why the fallback matters.** Phase 2 is the load-bearing step. It is the first time a customer's
search results come from OpenSearch — but Elasticsearch is still receiving every write and still
holds a complete, current copy. If OpenSearch throws an error on a read, dotCMS catches it, logs it
at `ERROR`, and answers from Elasticsearch instead. The customer sees a correct result; you see the
error in the log. That is your early-warning system, and it disappears the moment you enter Phase 3.

**What the fallback does *not* cover.** It only triggers on an *exception*. If OpenSearch answers
successfully but with stale or incomplete data — because a write silently failed earlier, or because
you never ran the reindex — there is no error to catch and the customer gets the wrong answer with no
trace. This is why §16's readiness report exists: it compares document counts rather than waiting for
an error.

## 4. The mirror strategy: how the active index gets copied

In phases 1 and 2, the OpenSearch index is a **shadow** of the Elasticsearch one. Understanding
exactly what that does and does not guarantee is the difference between a smooth migration and a
customer incident.

### What dual-write does

Every content change — publish, save, delete, permission change, content-type change — is written to
both engines, immediately after the database transaction commits. Going forward, the two engines stay
in step.

### What dual-write does *not* do

It does not backfill. The moment you enable Phase 1, OpenSearch has an **empty pair of indices** and
Elasticsearch has the customer's entire corpus. Dual-write only narrows that gap for content that
changes from that point on. A customer with 500,000 contentlets who publishes 50 a day would take
decades to converge.

**The only way to close the gap is a full reindex**, which reads every row from the database and
fans it out to both engines. From Phase 1 onward the reindex is migration-aware: it builds new index
slots on both engines, dual-writes every document, and switches both over at the end.

### Shadow-write failures are silent on purpose

While Elasticsearch is still authoritative (phases 1 and 2), a failed OpenSearch write is **logged
and ignored**. It does not fail the customer's publish, does not mark the reindex entry as failed,
and does not raise anything to the caller.

This is deliberate — a problem with the new engine must never break the live site. But it has a
direct operational consequence for you:

> **An OpenSearch write failure is invisible unless you look for it.** The customer's publish
> succeeds. The only evidence is a `WARN` in the log (level configurable via
> `DOTCMS_SHADOW_WRITE_LOG_LEVEL`) and, later, a document-count difference in the readiness report.

In Phase 3 this reverses: OpenSearch is authoritative, so its failures propagate to the caller and
the customer *will* see them.

### Where drift comes from

| Source of drift | Phase | How you detect it |
|---|:---:|---|
| The corpus that existed before Phase 1 was enabled | 1, 2 | Readiness report: `osIndexedPercent` far below 100 |
| A shadow write that failed and was swallowed | 1, 2 | `WARN` in the log; later a `COUNT_DRIFT` verdict |
| A rollback to an older dotCMS build during dual-write | 1, 2 | OpenSearch ends up *ahead* of Elasticsearch; `safeToRollback: false` |
| An index activated from a pre-migration backup | 1, 2, 3 | `MISSING_COUNTERPART` on `WORKING`/`LIVE` |
| A Site Search index created before dual-write started | any | `MISSING_COUNTERPART` on a `siteSearch` row |

Every one of these is reported by the readiness endpoint (§16), and every one is fixed the same way:
**a full reindex** for content, **a full crawl** for Site Search.

## 5. How one dotCMS talks to two engines at once

You do not need the internals to run a migration, but you do need this much, because it explains
which customisations survive the migration and which do not.

### The routing layer

There is exactly **one** place in dotCMS where the decision "which engine handles this?" is made, per
functional area. Every index operation — write, read, create, delete, reindex — goes through it. The
router reads the current phase on **every single call**, which is why the routing part of a phase
change takes effect immediately, without a restart.

The router also knows that the two engines hold indices under *different names*, and translates on
the way out: the Elasticsearch leg gets the plain name, the OpenSearch leg gets the `.os` name. You
never have to do that translation yourself, and you should never try.

### Cutting the cord to the vendor's library

This is the part that matters for customer customisations.

Historically, dotCMS's public search APIs returned **Elasticsearch's own Java classes** —
`org.elasticsearch.action.search.SearchResponse`, `SearchHit`, `SearchHits`, the ES aggregation
types. Anything a customer built on top of search (a plugin, a viewTool, a template) was therefore
compiled against Elasticsearch's SDK.

That is fatal for a migration: those classes do not exist in the OpenSearch client. A plugin holding
a `SearchResponse` cannot be handed an OpenSearch result — there is nothing to hand it.

So the search APIs were rewritten to return **dotCMS's own domain objects** instead — neutral types
that belong to dotCMS, not to any vendor:

| Old type (Elasticsearch's) | New type (dotCMS's own) |
|---|---|
| `org.elasticsearch.action.search.SearchResponse` | `ContentSearchResponse` |
| `com.dotcms.content.elasticsearch.business.ESSearchResults` | `ContentSearchResults<T>` |
| `org.elasticsearch.search.SearchHits` | `SearchHits` |
| `org.elasticsearch.search.SearchHit` | `SearchHit` |
| `org.elasticsearch.search.TotalHits` | `TotalHits` |

(All the new ones live in `com.dotcms.content.index.domain`.)

Both engines' results are converted into these types before they leave the routing layer. Code that
consumes them keeps working no matter which engine answered — that is the whole point.

**The consequence, and it is the main risk of the whole migration:** any customer code still written
against the *old* types was written against Elasticsearch specifically, and **will break when reads
move to OpenSearch**. The old methods still exist and are marked deprecated for removal, so nothing
breaks today — but they are the fault line. §9 covers how to find and fix them.

---

# Part II — Work out what will break

## 6. Affected areas — the impact checklist

Run through this list for every customer **before** you plan their migration. Anything you tick needs
a look; anything you cannot rule out needs a rehearsal (§14).

| # | Area | What to look for | Risk |
|:---:|---|---|:---:|
| 1 | **VTL templates / widgets** | Any use of `$estool` (`ESContentTool`), especially `esSearch(...)` or `esRaw(...)` | **High** |
| 2 | **VTL aggregations / facets** | Templates walking `.aggregations`, `.buckets`, `getKeyAsString()`, `getDocCount()` | **High** |
| 3 | **OSGi plugins** | Any bundle importing `org.elasticsearch.*`, or calling `esSearch` / `esSearchRaw` on `ContentletAPI` | **High** |
| 4 | **Site Search** | Any site-search index, its crawl schedule, and its aliases | **High** |
| 5 | **Raw engine REST calls** | Integrations calling `/api/es/search` or `/api/es/raw` with hand-written DSL | Medium |
| 6 | **Lucene queries** | Custom queries in content pulls, URL maps, workflows, saved searches | Low–Medium |
| 7 | **Content pulls** | `$dotcontent.pull(...)`, `$dotcontent.query(...)` and friends | Low |
| 8 | **GraphQL** | Content queries resolved through the index | Low |
| 9 | **Push publishing** | Bundles that carry index-dependent state | Low |
| 10 | **Index automation** | Scripts calling `/api/v1/esindex/...` on a schedule | Medium |

Items 1–4 are where real customer incidents have come from. Items 6–9 route through the same neutral
layer and have historically been fine, but should still be smoke-tested on the clone.

## 7. VTL templates and viewTools

VTL is where migration regressions surface most often, because customer templates read the index
through **viewTools** — the `$`-prefixed objects available inside `.vtl` files. If a viewTool returns
a different shape after reads move to OpenSearch, every page built on it breaks, usually silently.

### The three viewTools that touch the index

| `$key` | Class | Purpose |
|---|---|---|
| `$dotcontent` | `ContentTool` | The main content-pull tool: `pull`, `pullPerPage`, `query`, `count`, `find`. No aggregation support. |
| `$estool` | `ESContentTool` | Raw index queries — this is where aggregations live. Four methods; two safe, two not. |
| `$sitesearch` | `SiteSearchWebAPI` | Site Search queries and facets. |

### `$estool` — which method a template uses decides whether it survives

| Method | Returns | Status |
|---|---|---|
| `$estool.search(query)` | `ContentSearchResults` of `ContentMap`; `.aggregations` is a neutral map | ✅ **Safe** — vendor-neutral |
| `$estool.raw(query)` | `ContentSearchResponse` (neutral) | ✅ **Safe** |
| `$estool.esSearch(query)` | `ESSearchResults`, wrapping raw `Contentlet`s and ES-typed aggregations | ⚠️ **Legacy** — deprecated for removal |
| `$estool.esRaw(query)` | Elasticsearch's own `SearchResponse` | ⚠️ **Legacy** — deprecated for removal; breaks outright on OpenSearch |

All four take a **raw engine JSON query body**, not a Lucene string. Note that `search` and `raw`
lowercase the whole query before running it, so `contentType` becomes the physical field
`contenttype` — convenient, but it means case-sensitive exact matching is not available on those two,
and aggregation names come back lowercased.

**How to audit a customer's templates.** Grep their VTL for `esSearch` and `esRaw`. Every hit is a
line that needs rework before Phase 2.

### The three regression classes seen in the field

**1. Aggregations and facets must be byte-for-byte equivalent.** (Customer tickets #37559 / #36026.)
An early version of the migration flattened `$results.aggregations` and silently broke every template
that walked `.buckets`. The neutral aggregation type exists specifically so those templates keep
working verbatim. The neutral objects expose `getBuckets()`, `getHits()`, and per bucket `getKey()`,
`getKeyAsString()`, `getKeyAsNumber()`, `getDocCount()`, `getAggregations()`.

*What to test:* run the same aggregation template in Phase 1 and Phase 2 and diff the rendered
output. Bucket names and doc counts must be identical. Test nested aggregations and `top_hits`
sub-aggregations too — `top_hits` comes back under `.hits`, not `.buckets`.

**2. Field shadowing on raw `Contentlet`s.** (Open customer ticket #37870.) When a template iterates
the legacy `$estool.esSearch(...)` result, each record is a raw `Contentlet`, and Velocity resolves
`$record.<field>` by calling the matching Java getter. If a custom field's variable name collides
with a built-in getter, the getter wins. The known case is a custom field named `contentTypeId`,
which resolves to `Contentlet.getContentTypeId()` — a 32-character internal hash instead of the
field's value.

*Symptom to recognise:* "a field turned into a hash after the migration."
*Workaround:* `$record.map.contentTypeId` instead of `$record.contentTypeId`.
*Real fix:* move the template to `$estool.search(...)`, whose records are `ContentMap` and read
fields cleanly.

**3. Templates bound to Elasticsearch SDK types.** Anything calling
`$estool.esSearch(...).getAggregations()` or `$estool.esRaw(...)` receives Elasticsearch classes.
Those do not exist on OpenSearch, so the template fails once reads move. There is no workaround —
these must be rewritten against `$estool.search(...)` / `$estool.raw(...)`.

### The test that catches all three

> **Render the same template, unchanged, in Phase 1 and in Phase 2. Diff the output.**

Same input, same output. Any difference is a finding. This one comparison is worth more than reading
the template code, because it exercises the actual data the customer has.

## 8. Lucene queries and the search REST endpoints

### Lucene queries are the safe path

dotCMS's own query language (`+contentType:Blog +live:true`) is translated by dotCMS into whatever
DSL the target engine speaks. Customer Lucene queries — in content pulls, URL maps, workflow
conditions, saved searches — therefore route through the neutral layer and are **not** engine-bound.

That said, the two engines are not bit-identical query processors. Two things to watch:

- **`match_phrase_prefix` behaves differently** between the engines. Any query using it deserves an
  explicit before/after comparison.
- **Relevance scores differ.** This is expected and not a bug. What must *not* differ is the **set**
  of documents returned. Compare identifiers, not scores.

### The REST endpoints, and what changes

Every search endpoint is phase-aware: the identical request hits Elasticsearch in phases 0–1 and
OpenSearch in phases 2–3. So the test is always the same **cross-phase diff**.

| Endpoint | Query language | Migration risk |
|---|---|---|
| `POST /api/content/_search` | Lucene | Low — neutral |
| `POST /api/v1/content/_search` | Lucene (same behaviour) | Low |
| `POST /api/v1/content/search` | Structured form | Low |
| `GET /api/content/indexsearch/{query}/...` | Lucene, identifiers only | Low |
| `POST /api/es/search` | **Raw engine DSL** | Medium |
| `POST /api/es/raw` | **Raw engine DSL** | Medium |
| `POST /api/v1/graphql` | GraphQL | Low |

**The two `/api/es/*` endpoints deserve a note.** They accept raw engine DSL, which means a customer
integration may have hand-written Elasticsearch-specific syntax. Their **responses**, however, are
deliberately kept in the legacy Elasticsearch wire format even when OpenSearch served the query — so
existing consumers keep parsing them. Specifically:

- `/api/es/search` returns `{ "contentlets": [...], "esresponse": { …legacy ES JSON… } }`.
- `/api/es/raw` returns the legacy shape directly: `took`, `hits.total.{value,relation}`,
  `hits.hits[]` with `_id` / `_index` / `_score` / `_source`, and `aggregations`.
- Aggregation keys use the ES typed-key form, e.g. `sterms#content_types`.
- `_score` comes back as `null` for non-scored queries (field-sorted, filter-only, aggregation-only).
  This was a genuine 500-error bug once (#36478 / #36398) — if you see a 500 from a search response,
  file it.
- Neither shape emits a per-hit `sort` array. An integration relying on per-hit `sort` (geo-distance
  sorting is the usual case) will not find it.

## 9. OSGi plugins and the removal of vendor types

This is the area most likely to produce a nasty surprise, because plugin code is compiled ahead of
time and its breakage is a hard failure, not a degraded result.

### Why plugins are exposed at all

dotCMS's OSGi container exports the packages that are on the application's classpath, so a plugin can
`Import-Package: org.elasticsearch.search` and it resolves — today. Plugins written any time in the
last decade could and did do exactly that, because dotCMS's own search API handed them Elasticsearch
objects (see §5).

Three levels of exposure, in increasing severity:

| Level | What the plugin does | When it breaks |
|:---:|---|---|
| **1** | Calls `contentletAPI.search(...)` / `searchRaw(...)`, or `$dotcontent` | Never. Already neutral. |
| **2** | Calls the deprecated `contentletAPI.esSearch(...)` / `esSearchRaw(...)`, or implements the deprecated `esSearch` / `esSearchRaw` hook methods | **At Phase 2**, when the result it expects is no longer an Elasticsearch object. Also at compile time once the deprecated methods are removed. |
| **3** | Imports `org.elasticsearch.*` types directly and holds them in its own fields, casts, or signatures | **At Phase 2**, and permanently once Elasticsearch is dropped from the classpath — the bundle will not even resolve. |

### How to audit a customer's plugins

Ask for the bundle jars, then:

```bash
# Level 3 — does the bundle import Elasticsearch packages?
unzip -p <bundle>.jar META-INF/MANIFEST.MF | tr ',' '\n' | grep -i elasticsearch

# Level 2/3 — does the compiled code reference ES classes or the deprecated methods?
unzip -o <bundle>.jar -d /tmp/bundle >/dev/null
grep -rl "org/elasticsearch" /tmp/bundle --include=*.class
grep -rl "esSearchRaw\|esSearch" /tmp/bundle --include=*.class
```

If you have the source, the same grep over `src/` is faster and more readable.

### The fix

The plugin developer replaces the deprecated calls and recompiles. The mapping is mechanical:

| Deprecated | Replacement | New return type |
|---|---|---|
| `contentletAPI.esSearch(q, live, user, roles)` | `contentletAPI.search(q, live, user, roles)` | `ContentSearchResults<Contentlet>` |
| `contentletAPI.esSearchRaw(q, live, user, roles)` | `contentletAPI.searchRaw(q, live, user, roles)` | `ContentSearchResponse` |
| `ContentletAPIPreHook.esSearch(...)` | `ContentletAPIPreHook.search(...)` | `boolean` |
| `ContentletAPIPreHook.esSearchRaw(...)` | `ContentletAPIPreHook.searchRaw(...)` | `boolean` |
| `ContentletAPIPostHook.esSearch(...)` | `ContentletAPIPostHook.search(...)` | `void` |
| `ContentletAPIPostHook.esSearchRaw(...)` | `ContentletAPIPostHook.searchRaw(...)` | `void` |

The hook replacements have default no-op implementations, so a plugin only overrides what it actually
intercepts. The full before/after examples are in [`SEARCH_API_MIGRATION.md`](SEARCH_API_MIGRATION.md).

Two extra notes for the developer:

- `ContentSearchResults<T>` is a **typed** `List<T>` — the old `(Contentlet)` casts can go away.
- `ContentSearchResponse.toString()` is **not JSON**. A plugin or template that called
  `.toString()` on the old raw response to get ES wire-format JSON must switch to the structured
  accessors (`hits()`, `hits().hits()`, `hits().totalHits().value()`, `aggregations()`,
  `scrollId()`, `tookMillis()`).

### Timing this against the phases

A Level-2 or Level-3 plugin **does not fail in Phase 1**. Phase 1 still reads from Elasticsearch, so
the plugin gets exactly what it always got. It fails when you enter **Phase 2**.

That is genuinely useful: it means you can enable dual-write, prove the write path, and buy the
customer time to recompile their plugins — all without exposing them to the plugin risk. But it also
means **a clean Phase 1 tells you nothing about plugin readiness.** The plugin audit is a Phase 2
precondition, not a Phase 1 one.

## 10. What to do when you find a bug in a plugin

You will usually find it on the clone (§14) or in the first hours of Phase 2. The response is the
same either way, in this order:

**1. Stop the exposure — go back to Phase 1.** Setting the phase to 1 routes reads back to
Elasticsearch immediately, without a restart, and dual-write keeps OpenSearch current in the
meantime. This is the cheapest, safest lever you have and it should be your reflex. Check
`safeToRollback` in the readiness report first (§16) — it is only unsafe if OpenSearch is *ahead* of
Elasticsearch, which in a dual-write phase it should not be.

**2. Classify the bug before escalating.** The three categories need different owners:

| Symptom | Likely cause | Owner |
|---|---|---|
| Bundle fails to resolve / `ClassNotFoundException` on an `org.elasticsearch.*` class | Level-3 plugin (§9) | Customer / plugin developer |
| `ClassCastException`, or a method that used to return data now returns nothing | Level-2 plugin using deprecated APIs | Customer / plugin developer |
| A field renders as a 32-char hash | Field shadowing (§7, ticket #37870) | Template fix — use `$record.map.<field>` |
| Aggregation buckets empty or differently named | Aggregation regression, **or** a Site Search index built with a dynamic mapping (§20) | Check §20 first; if not that, file it against dotCMS |
| Result *set* differs between phases for the same query | Engine behaviour difference | File it against dotCMS with both result sets |

**3. If it is dotCMS's bug, capture the evidence before it evaporates.** File it with: the phase, the
exact physical index names on both engines, the query, and the output from *each* phase. A report
without the cross-phase diff is very hard to act on.

**4. If it is the customer's plugin, give them the mapping table from §9 and a deadline.** The
deprecated methods are marked for removal. Staying on Phase 1 indefinitely is a holding pattern, not
a resolution — the customer has to recompile eventually, and the migration window is when they have
the most incentive to do it.

**5. Never work around a plugin bug by editing index names or configuration by hand.** Anything that
makes the two engines disagree about which index is active is far more expensive to undo than a phase
downgrade.

---

# Part III — Prepare

## 11. OpenSearch server requirements and security criteria

### Non-negotiable requirements

| Requirement | Why | What happens if you get it wrong |
|---|---|---|
| **OpenSearch 3.x** | dotCMS asserts the major version at startup | Startup validation fails; migration halts back to Phase 0 |
| **A separate instance from Elasticsearch** | dotCMS compares the two endpoint sets and refuses to run them against the same host:port | Startup validation fails with an explicit "same endpoint(s)" error |
| **Reachable from every dotCMS node** | Each node connects independently | The node that cannot connect halts its own migration and silently serves Elasticsearch-only |
| **`number_of_replicas` set explicitly** | OpenSearch does not inherit dotCMS's implicit default | Yellow cluster, or unexpected replica behaviour |

The endpoint-separation check is **best-effort on strings**: `127.0.0.1:9200` and `localhost:9200`
are the same server but will not be detected as overlapping. Do not rely on it as a safety net —
verify the two endpoints are genuinely different clusters yourself.

### Transport security

| Property | Recommendation |
|---|---|
| `OS_ENDPOINTS` | Use `https://` in production. TLS activates automatically from the scheme; you do not also need `OS_TLS_ENABLED`. |
| `OS_TLS_CERT_REQUIRED` | `false` by default — certificate and hostname verification are **skipped**. Set to `true` in production, with the CA in the JVM truststore or in `OS_TLS_CA_CERT`. |
| `OS_TLS_CA_CERT` | Path to the PEM CA when the cluster uses a private CA / internal PKI. |
| `OS_TLS_TRUST_SELF_SIGNED` | Dev and lab only. Never in production. |

> A misconfigured trust chain surfaces as `PKIX path building failed`. That is a TLS problem, not a
> migration problem — fix it before you touch the phase.

### Authentication

| Property | Notes |
|---|---|
| `OS_AUTH_TYPE` | `BASIC` (default), `JWT`, or `CERT`. |
| `OS_AUTH_BASIC_USER` / `OS_AUTH_BASIC_PASSWORD` | Required for `BASIC`. |
| `OS_AUTH_JWT_TOKEN` | Sent as `Authorization: Bearer …` on every request. |
| `CERT` (mTLS) | Partially implemented — currently falls back to the trust-self-signed strategy. Do not plan a production migration around it. |

> **Every `OS_*` key falls back to its `ES_*` equivalent when unset.** This is convenient and also a
> trap: an instance that never configured OpenSearch credentials will silently reuse the
> Elasticsearch ones rather than failing. If the two clusters have different credentials, set the
> `OS_*` keys explicitly.
>
> **Anonymous connection is not an error.** If no credentials resolve at all, the startup banner
> reports `NONE — connecting ANONYMOUSLY`. dotCMS will still connect if the cluster does not enforce
> security. On a production cluster that line means your credentials did not resolve.

### The service account: least privilege, and the one permission everyone forgets

dotCMS does **not** need an OpenSearch admin account. The standard provisioning creates a dedicated
user (`dotcms-es-user`) mapped to a per-customer role, scoped to that customer's indices. The
reference implementation is `docker/docker-compose-examples/single-node-os-migration/opensearch.py`.

**Cluster-level permissions:**

```
cluster:monitor/health
cluster:monitor/state
cluster:monitor/nodes/stats
cluster:monitor/main          ← see the warning below
indices:data/write/bulk
indices:data/read/scroll
indices:data/read/scroll/clear
```

**Index permissions on `cluster_<customer>*`:** `indices_all`, `indices_monitor`.

**Index permissions on `*` (read-only, for listing and stats):** `indices:monitor/stats`,
`indices:monitor/settings/get`, `indices:admin/aliases/get`.

> ### ⚠️ `cluster:monitor/main` is the single most common OpenSearch-side blocker
>
> dotCMS's startup validator calls `GET /` to read the cluster's version. That maps to the action
> `cluster:monitor/main`. Historic dotCMS roles never granted it.
>
> Without it, the request returns **403**, dotCMS cannot tell a 403 from an unreachable cluster,
> and it concludes the cluster is down. It then **halts the migration, resets to Phase 0, and boots
> normally on Elasticsearch alone**. Nothing is broken; nothing is migrating either. You will be
> looking at a perfectly healthy OpenSearch cluster wondering why nothing is being written to it.
>
> **Check this first, always.** Spike #35922 confirmed this is the *only* permission gap between the
> historic dotCMS role and OS 3.x — everything else (`indices_all` on the customer pattern) already
> expands to the full action set needed, on both OS 1.3 and OS 3.x.

### The index-pattern trap

The role grants `indices_all` on `cluster_<customer>*`. dotCMS builds its physical index names from
`DOT_DOTCMS_CLUSTER_ID`. **If the cluster ID does not match the role's index pattern, dotCMS can
connect and read the cluster version fine, but every index-create is rejected with a 403.**

The startup gate cannot see this: it probes `GET /`, which is cluster-scoped and succeeds. The 403
only appears later, on the first index creation. Symptoms (issue #36222):

- A "full reindex" that returns HTTP 200 with a completed progress bar, but creates **no new index on
  either engine**, and no `Full reindex started` line in the log.
- An `ERROR` line naming `likelyCause=AUTH_FORBIDDEN` and "falling back to ES-only".

**`DOT_DOTCMS_CLUSTER_ID` is immutable after the first boot** — it is written to the `dot_cluster`
table and read from there forever after. Changing the environment variable on an instance that
already has data does nothing. So the fix is always on the OpenSearch side: widen the role's index
pattern to match the actual cluster ID.

**Verification, before you change any phase:**

```bash
# 1. What cluster prefix does dotCMS actually use? (look at an existing ES index name)
curl -sk -u <es-user>:<pass> https://<es-host>:9200/_cat/indices?v | grep working

# 2. Can the dotCMS service account create an index with that prefix on OpenSearch?
curl -sk -u <os-user>:<pass> -X PUT "https://<os-host>:9200/cluster_<id>.permcheck" \
  -H 'Content-Type: application/json' -d '{"settings":{"number_of_shards":1,"number_of_replicas":0}}'
curl -sk -u <os-user>:<pass> -X DELETE "https://<os-host>:9200/cluster_<id>.permcheck"

# 3. Can it read the root (the version probe)?
curl -sk -u <os-user>:<pass> https://<os-host>:9200/
```

All three must succeed before you enable Phase 1. Step 3 is the `cluster:monitor/main` check.

### One more OpenSearch 3.8 note

OpenSearch 3.8 creates system indices that older 3.x releases did not (`.plugins-ml-config`,
`top_queries-*` from query insights). If you write any script that diffs the *set* of indices between
the two engines, exclude system indices or it will report false differences. The readiness endpoint
already only looks at dotCMS's own indices.

## 12. What happens when dotCMS cannot reach OpenSearch

The behaviour is deliberately different by phase, and knowing which one you are in tells you how
urgent the situation is.

### At startup

dotCMS validates the OpenSearch configuration during boot, whenever the phase is 1 or higher. It
checks three things: **the cluster is reachable**, **it reports version 3.x**, and **its endpoints do
not overlap Elasticsearch's**.

**In phases 1 and 2 — it degrades safely.** Validation failure logs the reason, resets the phase to 0
**in memory**, and dotCMS boots normally on Elasticsearch alone. No crash, no hang, no data loss. You
will see three lines:

```
ERROR  OpenSearch startup validation FAILED — halting OS migration; dotCMS falls back to
       ES-only (PHASE_0_MIGRATION_NOT_STARTED): <reason>
ERROR  OpenSearch migration halted: invalid configuration detected at startup. Verify
       OS_ENDPOINTS, OS version, and FEATURE_FLAG_OPEN_SEARCH_PHASE, then restart dotCMS.
WARN   Migration phase reset to PHASE_0_MIGRATION_NOT_STARTED (was PHASE_1_DUAL_WRITE_ES_READS).
       This change is runtime-only — persist it in dotmarketing-config.properties to survive a restart.
```

`<reason>` is one of: *cluster not reachable*, *version mismatch* (not OpenSearch 3.x), or *ES and OS
point to the same endpoint(s)*.

> **The reset is in memory only.** Fix the configuration and restart to retry. If you leave the
> configured phase at 1, the next restart tries again — which is usually what you want.

**In Phase 3 — it fails loud and refuses to serve.** There is no Elasticsearch to fall back to, and
silently rolling back to Phase 0 would point the customer at a potentially stale Elasticsearch index.
So dotCMS aborts startup with:

```
OpenSearch startup validation failed in PHASE_3_OPENSEARCH_ONLY. Cannot auto-rollback to ES
(ES may be decommissioned or stale). Restore OS connectivity or manually reset
FEATURE_FLAG_OPEN_SEARCH_PHASE, then restart dotCMS.
```

This is correct behaviour, and it is also why Phase 3 requires that you be confident in the
OpenSearch cluster's availability, not just its correctness.

### At runtime

| Phase | OpenSearch write fails | OpenSearch read fails |
|:---:|---|---|
| 1 | Logged (`WARN` by default), ignored. Customer unaffected. | n/a — reads come from Elasticsearch |
| 2 | Logged, ignored. Customer unaffected. | Caught, logged at `ERROR`, **retried against Elasticsearch**. Customer gets a correct result. |
| 3 | **Propagates to the caller.** | **Propagates to the caller.** |

### On startup delay

If OpenSearch is unreachable, dotCMS retries the connection before giving up — by default 24 attempts
with 5 seconds between them (`OS_CONNECTION_ATTEMPTS`, `OS_CONNECTION_RETRY_SLEEP_SECONDS`). So a
boot against a dead OpenSearch can pause for roughly two minutes before falling back. **That pause is
expected**; do not kill the container thinking it hung.

### The blind spot to remember

The startup gate probes the cluster root only. It cannot see permission failures scoped to *index
names* (§11). A cluster that passes validation can still reject every index-create with a 403. That
is why §11's three-step curl check exists.

## 13. Choosing a customer: selection and sizing criteria

Not every customer should be migrated the same way, and the deciding factor is almost always **how
long a full reindex takes**, because that is the one operation the whole plan is built around.

### The measurements to take first

Take these on the customer's real instance, before planning anything:

| Measurement | How to get it |
|---|---|
| Number of contentlets | `SELECT COUNT(*) FROM contentlet_version_info;` |
| Live contentlets | `SELECT COUNT(live_inode) FROM contentlet_version_info;` |
| Number of indices | `_cat/indices` on the source engine, plus the `indicies` table |
| Size on disk of the working index | `_cat/indices?v` — the `store.size` column |
| Number of Site Search indices, and their crawl schedules | The Site Search portlet |
| Number of sites | The Sites portlet |
| Number of OSGi bundles, and their ES exposure level | §9 |
| Reindex duration | The customer's own history, if they have run one; otherwise measure it on the clone (§14) |

### The classification

| | **Small** | **Medium** | **Large** |
|---|---|---|---|
| Contentlets | up to ~50k | ~50k–500k | 500k+ |
| Full reindex | under ~1 hour | 1–several hours | many hours; needs a real window |
| Site Search indices | 0–2 | a few | many, or business-critical |
| Sites | a handful | dozens | hundreds |
| OSGi exposure | none, or Level 1 | Level 2, recompilable | Level 3, or unmaintained plugins |
| **Recommended path** | Fast path — straight to Phase 3 in one window (§15) | Standard — 0 → 1 → 2 → 3, days apart | Standard, with a **long** dual-write soak (2+ weeks) and a rehearsed reindex |

These boundaries are **guidance, not a rule**. A "small" customer with an unmaintained Level-3 plugin
is not a fast-path candidate; a "large" customer with no customisations at all may move faster than
the table suggests. The reindex duration and the plugin audit are the two inputs that actually
decide.

### The disqualifiers

Do **not** start a migration — of any size — while any of these is true:

- The customer has a Level-3 OSGi plugin (§9) with no one able to recompile it.
- Site Search is business-critical and its crawl schedule cannot be run on demand.
- The OpenSearch cluster's service account has not passed §11's three-step permission check.
- There is no window in which a full reindex can complete.
- You have not rehearsed on a clone (§14).

### Why a long dual-write soak, for the big ones

The standing recommendation for large customers is **dual-write for roughly two weeks, then
validate, then move to Phase 2.** The reason is not that dual-write converges the data — it does not
(§4) — but that two weeks of real production traffic is what surfaces the intermittent write
failures, permission edge cases, and mapping mismatches that a one-day test never reaches. The
reindex is what fills the index; the soak is what proves the pipeline.

## 14. Rehearse on a clone before you touch the real environment

**This is the single highest-value thing in this runbook.** Migrate a copy of the customer's
instance before you migrate the customer's instance.

A clone gives you what no checklist can: the customer's *actual* content types, templates, plugins,
Site Search configuration, and data volumes, exercised against the real migration code — with no
consequence if it goes wrong.

### What a useful clone needs

| Component | Requirement |
|---|---|
| Database | A restore of the customer's database. Not a subset — the content volume is what makes the reindex timing meaningful. |
| Assets | The asset store, or enough of it that Site Search crawls and file-asset queries are realistic. |
| Search engine | A **separate** source engine holding a copy of their indices, or a fresh one you reindex into from the restored DB. |
| OpenSearch | A separate OpenSearch 3.x instance, provisioned with the **same role and index pattern** the customer's real one will use. |
| Plugins | Their actual bundle jars, deployed. |
| dotCMS version | The **same build** you will run in production. |

> **The cluster ID must match.** `DOT_DOTCMS_CLUSTER_ID` comes from the restored database, so a clone
> of the customer's DB naturally carries the customer's cluster ID — which is exactly what makes the
> OpenSearch index-pattern permission check meaningful on the clone. Do not "fix" it to something
> else.

### What the rehearsal must produce

Before you sign off on the real migration, the clone should have given you:

1. **A measured reindex duration** at the customer's real data volume. This number drives the window.
2. **A pass/fail on every item in §6's checklist**, exercised with the customer's own templates and
   plugins — not synthetic ones.
3. **A cross-phase diff** of their most important queries and pages, Phase 1 versus Phase 2.
4. **A clean readiness report** (§16) at each phase boundary.
5. **A practised downgrade.** Go 2 → 1 on the clone and confirm the site is still correct. You want
   to have done this once before you need to do it under pressure.
6. **A written window plan** with the actual timings you measured, not estimates.

### Rehearsing the fast path

If the customer is a fast-path candidate (§15), the rehearsal is even more important, because the
fast path has a period where search is degraded. The clone is where you find out how long that period
actually is.

## 15. The fast path: straight to Phase 3

For a small customer, walking 0 → 1 → 2 → 3 over several days is a lot of ceremony for a reindex that
finishes in twenty minutes. There is a legitimate shortcut: **set the phase to 3 and restart.**

### What dotCMS does when it boots into Phase 3 with no OpenSearch indices

1. Startup validation runs, and in Phase 3 it is **fail-loud** — if OpenSearch is unreachable or the
   wrong version, dotCMS refuses to start. (Which is what you want: better a refusal than an empty
   index served to the customer.)
2. dotCMS sees no OpenSearch indices and creates the working/live pair, deriving their names from the
   existing Elasticsearch ones.
3. It sees those indices are empty and **automatically queues a full reindex** in the background.
4. Elasticsearch is not written to at all from this moment on.

So the fast path bootstraps and backfills itself. You do not have to trigger the reindex manually.

### The cost, stated plainly

> **Between step 2 and the end of step 3, OpenSearch is the read engine and it is empty. There is no
> fallback in Phase 3. Search returns nothing until the reindex completes.**

Concretely, for the duration of the reindex:

- Content searches return empty or partial results.
- Any page built on a content pull renders empty or partial.
- Site Search is stale (its indices were not rebuilt), and **must not be crawled** until the content
  reindex finishes — a crawl reads *from* the content index, so crawling now produces a permanently
  truncated Site Search index (§20).

This is a real, customer-visible search outage for the length of the reindex. That is the entire
reason the fast path is limited to customers whose reindex is short.

### When the fast path is appropriate

All of these must hold:

- ✅ Measured reindex duration on the clone is **comfortably inside the maintenance window** — an hour
  is a reasonable ceiling, but the number that matters is the one you measured.
- ✅ There is a genuine maintenance window in which degraded search is acceptable.
- ✅ The plugin audit (§9) came back clean, or the plugins were recompiled and verified on the clone.
- ✅ The OpenSearch permission check (§11) passes.
- ✅ You rehearsed the whole thing on the clone, including the timing.
- ✅ **You have a rollback plan** — see below.

### The fast path's rollback is different, and worse

Going straight to Phase 3 means Elasticsearch stops receiving writes immediately. From that moment,
Elasticsearch is a **point-in-time snapshot**, and it gets staler by the minute.

Rolling back from Phase 3 to Phase 0 is therefore not a clean undo: any content published after the
cutover exists only in OpenSearch and in the database. It is **not** lost — the database is always
authoritative — but Elasticsearch will not know about it until you run a full reindex against
Elasticsearch too.

**So the fast-path rollback plan is: set the phase back to 0, restart, and immediately run a full
reindex.** Budget for that as a second window of the same length. If you cannot afford two windows,
take the standard path instead — Phase 1 and 2 both keep Elasticsearch current, which is exactly what
makes their rollback instant.

---

# Part IV — Execute

## 16. The readiness endpoint — your source of truth

Everything in Part IV hangs off one endpoint:

```
GET /api/v1/index/migration/readiness
```

It is a **read-only report**. It never changes anything, never repairs anything, and never blocks
anything. What it does is answer, from live data, the only question that matters at a phase boundary:
**are the two engines actually in sync, and is it safe to move?**

### Who can call it, and why it is locked down

Access requires **both**:

1. The caller is a **CMS Administrator**, and
2. the caller holds the **migration support role**.

A plain admin without the role gets **403**. So does a role holder who is not an admin. Both
conditions, always.

The role is identified by its **role key**, configured in `OS_MIGRATION_INDEX_VISIBILITY_ROLE_KEY`,
default **`os_migration_qa`**. Note that dotCMS matches on the role's *Key* field, not its display
name — creating a role called "OS Migration QA" without setting the Key does nothing.

The endpoint is also marked hidden: it does not appear in `openapi.yaml` or in the API playground.
You reach it by knowing the URL.

> **Why so restrictive?** Because a regular user should never be able to discover that a migration is
> running on their instance. The role gate is the mechanism; the hidden flag stops casual discovery.

**To grant access:**

1. In dotCMS, create a Role and set its **Key** to `os_migration_qa` (or whatever
   `OS_MIGRATION_INDEX_VISIBILITY_ROLE_KEY` is set to).
2. Assign that role to the CMS Administrator account you will use.

```bash
curl -u admin@dotcms.com:<password> \
  https://<host>/api/v1/index/migration/readiness | jq
```

There is no `ResponseEntityView` envelope — the JSON **is** the report.

> **Related but separate:** the `.os` indices are hidden from the index portlet and from
> `/api/v1/esindex` in phases 0, 1 and 2, for **everyone**, and shown in Phase 3. That is purely
> phase-based and consults no role. The role key above governs the readiness endpoint only.

### Reading the report, top-down

**1. `phase`** — `current` and `name`, plus `readEngine`, `writeEngines`, and `dualWrite`. Everything
below is relative to this. Read it first, always, because it tells you what the instance is *actually*
doing, which may not be what you configured (see §12: a failed startup validation resets it).

**2. `verdict.safeToAdvance` and `verdict.safeToRollback`** — the go/no-go pair. They answer different
questions and are **not opposites**; both can be `false` at once.

| | Blocked when | Because |
|---|---|---|
| `safeToAdvance` | the OpenSearch copy is **behind** | promoting toward OpenSearch-only would lose that delta |
| `safeToRollback` | the OpenSearch copy is **ahead** | downgrading would hide that delta until a reindex |

**3. `verdict.summary` and `verdict.blockers`** — a sentence you can paste into a ticket, then the
per-index list of what to fix. If `safeToAdvance` is `false`, `blockers` is never empty.

**4. `content` and `siteSearch`** — the evidence. `content` is keyed by slot (`WORKING`, `LIVE` — a
fixed pair). `siteSearch` is a list (an open set).

### Reading a per-index row

Each row carries `es` and `os`, each with `{exists, docCount, physicalName}` — and, on Site Search
rows only, `alias`. Then:

| Field | How to read it |
|---|---|
| `verdict` | `IN_SYNC` · `MISSING_COUNTERPART` (one engine lacks the index) · `COUNT_DRIFT` (both have it, different counts) |
| `driftPercent` | `(OS − ES) / ES × 100`. `0.0` in sync · **negative = OpenSearch behind** (blocks advance) · **positive = OpenSearch ahead** (blocks rollback) · `-100.0` mirror empty or absent · `null` a count could not be measured |
| `databaseDocCount` | Content rows only. What the **database** says the index should hold. |
| `esIndexedPercent` / `osIndexedPercent` | Content rows only. Each engine measured **against the database**. `100.0` = complete. `3.06` = never rebuilt. |
| `docCount: -1` | The count **could not be measured**. Never read it as zero — the verdict treats it as out of sync deliberately. |
| `physicalName` | The exact name on that server. Copy/paste it into `_cat/indices` to verify by hand. |
| `recommendation` | The concrete action: re-crawl or reindex. A trailing `NOTE:` flags a damaged alias (§20). |

### The field that keeps working in Phase 3

`driftPercent` compares the two engines against each other — which stops being an answer the moment
one of them is the only one left. `osIndexedPercent` compares OpenSearch against the **database**, so
it still means something in Phase 3, where a mirror that was never rebuilt would otherwise read
unremarkably.

> **In Phase 3, `safeToAdvance` is forced `true`** (there is no phase 4). Never read the boolean
> alone in Phase 3 — read the per-index rows and `outOfSyncCount`. This is precisely the phase where
> a problem is immediate and customer-visible.

### Two things a document count cannot tell you

1. **A count that does not move is not proof that nothing was written.** The document id is
   `identifier_languageId_variant`, so re-publishing content already in the index is an *update* and
   the total stays put.
2. **In a dual-write phase, the OpenSearch copy only ever receives what changes from that point
   on.** A mirror sitting at 15 of 683 documents is the *expected* state until you run the reindex.

To settle whether one specific write landed, ask for the **document**, not the count:

```bash
curl -sk -u <os-user>:<pass> \
  "https://<os-host>:9200/<physicalName-from-the-report>/_doc/<identifier>_<languageId>_DEFAULT"
# "found": true with the modDate of your edit ⇒ the dual-write landed
```

### The two traps in the report itself

- **It only covers the *active* working/live pair.** An inactive backup index is invisible to it.
  If you activate a backup, call the endpoint again *afterwards* — see §21.
- **It reports; it never repairs and never blocks.** Re-running it changes nothing. The fix is always
  you running a reindex or a crawl.

## 17. Setting the phase, and when a restart is mandatory

### Where the phase lives

The phase is the setting `FEATURE_FLAG_OPEN_SEARCH_PHASE` (as an environment variable:
`DOT_FEATURE_FLAG_OPEN_SEARCH_PHASE`). Values `0` to `3`. An absent or unrecognised value means
Phase 0.

There are three places it can be set, and they have different reach:

| Where | Reach | Survives restart | Notes |
|---|---|---|---|
| Environment variable / `dotmarketing-config.properties` | **One node** | ✅ | You must set it on every node yourself. |
| The dotCMS **system table** (`POST /api/v1/system-table`, CMS Admin required) | **The whole cluster** | ✅ | Stored in the database and shared. **This source wins over the environment variable.** |
| Runtime reset by dotCMS itself (`haltMigration`) | One node, in memory | ❌ | What happens on a failed startup validation (§12). |

> **The system table takes precedence.** If `FEATURE_FLAG_OPEN_SEARCH_PHASE` was ever set through the
> system table, that value wins over the environment variable and over the properties file — and it
> also means dotCMS's own emergency reset to Phase 0 becomes a no-op, because the reset writes to the
> in-memory store while the system-table value keeps winning. **If you use the system table, remember
> that clearing it is part of your rollback procedure.**
>
> Pick one mechanism per customer and stick to it. Mixing them is how you end up with nodes
> disagreeing about what phase they are in.

### The restart rule

Here is the part that catches people out.

> **Routing is live. Setup is not.**

The router reads the phase on every single call, so **which engine gets writes and reads changes
instantly** when you change the value. But three things only happen at **startup**, inside dotCMS's
initialisation:

1. The OpenSearch **connectivity / version / endpoint-separation validation**.
2. The automatic **migration halt** and fallback to Phase 0 if that validation fails.
3. The **creation of the OpenSearch index pair and its `indicies` rows** (bootstrap and catchup).

Which gives:

| Transition | Restart? | Why |
|---|:---:|---|
| **0 → 1** | ✅ **Mandatory** | The OpenSearch indices do not exist yet. They are created at boot. Flip the flag live and dual-writes fan out to an index that was never created — and those misses are **silently swallowed** (§4), while validation never runs. You get a migration that looks enabled and is doing nothing. |
| **1 → 2** | ✅ **Strongly recommended** | Routing switches live, but only a restart re-runs the startup validation for the phase you are now actually in. Validate the phase you run. |
| **2 → 3** | ✅ **Strongly recommended** | Same, and more so: Phase 3 validation is fail-loud. You want to discover a connectivity problem at a controlled restart, not when the first customer search returns nothing. |
| **Any downgrade** | ⛔ **Not required** | Routing reverts immediately. Downgrade is the one direction that is genuinely instant — which is what makes it your emergency lever (§21). |
| **After an automatic halt** | ✅ **Mandatory** | The reset to Phase 0 is in memory only. Fix the configuration and restart to retry the migration. |

### The one-line summary to remember

> **Going up the phases: change the value, then restart, then verify. Coming down: change the value
> and you are done.**

## 18. Cluster considerations: node consistency and traffic

Everything above assumed a single node. On a clustered instance, two more things matter.

### Every node must agree on the phase

The phase is read per node, from that node's configuration. If node A is in Phase 2 and node B is in
Phase 1:

- Both nodes still dual-write, so **no data is lost** — that part is safe.
- But **search results depend on which node served the request**. Node A answers from OpenSearch,
  node B from Elasticsearch. If the two engines have drifted at all, the customer sees results that
  change between page loads, with no pattern. This is extremely confusing to diagnose from the
  outside.

The mismatch is worse in the phase-3 direction: a node in Phase 3 stops writing to Elasticsearch
entirely, so a mixed 2/3 cluster leaves Elasticsearch receiving a *subset* of writes — and your
rollback target is now quietly corrupt.

> **Rule: never let a cluster sit in mixed phases for longer than the rolling restart takes.**

**Two ways to keep them consistent:**

- **System table** (recommended when it fits): one call, every node picks it up. The change is stored
  in the database and shared cluster-wide.
- **Environment variables**: you must update every node. Do it as one deployment change, not
  node-by-node over hours.

Either way, **verify per node** afterwards — read `phase.current` from the readiness endpoint against
each node directly, bypassing the load balancer.

### Traffic direction during the change

For **upgrades** (0 → 1, 1 → 2, 2 → 3), the restart requirement means each node goes down and comes
back. Handle this the way you would any rolling restart, with one migration-specific addition:

1. **Drain the node** at the load balancer before restarting it. This is not optional courtesy — a
   node mid-boot in Phase 1 or 2 may be halfway through creating OpenSearch indices, and requests it
   serves during that window can behave inconsistently.
2. **Restart one node. Let it come fully up. Check its log** for the validation banner and the
   `OS version check passed` line (Appendix C) before restarting the next one. If the first node
   halts its migration, **stop the rollout** — the remaining nodes will do the same, and you would
   rather find out with one node down than all of them.
3. **Return the node to the pool. Then move to the next.**
4. **After the last node**, call the readiness endpoint against each node individually and confirm
   they all report the same `phase.current`.

For a **downgrade**, none of this applies — no restart is needed, routing reverts on the next call,
and you want it to take effect everywhere as fast as possible. Push the change to all nodes at once.

### A note on the reindex during a rolling restart

A full reindex is driven by a queue in the database, and workers on any node pick entries up. A
rolling restart during an in-flight reindex will not lose entries, but it will slow it down and it
makes the completion signal harder to read. **Prefer letting a reindex finish before a rolling
restart** — or abort it explicitly and restart it afterwards. Do not flip a phase mid-reindex; that
is a documented hazard (§21).

## 19. The step-by-step migration

This is the standard path. §15 covers the fast path.

### Before you start — the gate

- [ ] The plugin audit (§9) is complete and every Level-2/Level-3 finding has an owner.
- [ ] The VTL audit (§7) is complete — you have grepped for `esSearch` and `esRaw`.
- [ ] The OpenSearch permission check (§11) passes all three curls.
- [ ] You have measured the reindex duration on a clone (§14).
- [ ] You have practised a downgrade on the clone.
- [ ] The migration support role exists and your account holds it (§16).
- [ ] You have a baseline: the readiness report at Phase 0, saved.
- [ ] You know the customer's Site Search crawl schedule (§20).

### Phase 0 → 1: start dual-write

**What changes:** OpenSearch starts receiving every write. Nothing the customer sees depends on it.

1. Set `FEATURE_FLAG_OPEN_SEARCH_PHASE=1` on every node.
2. **Restart** (rolling, per §18). This is mandatory — the OpenSearch indices are created at boot.
3. Watch the log for the configuration banner, `OS version check passed: 3.x.x`, and
   `Endpoint separation check passed` (Appendix C). If instead you see the halt lines, stop and go to
   §12.
4. Confirm the OpenSearch indices now exist:
   ```sql
   SELECT * FROM indicies WHERE index_name LIKE '%.os';
   ```
   Two rows expected (working and live). If there are none, the bootstrap did not happen — check the
   log for an `AUTH_FORBIDDEN` line (§11).
5. **Publish one piece of content and verify it landed on both engines**, by document, not by count
   (§16). This is your proof that the write path works end to end.
6. **Run a full reindex.** This is what actually fills OpenSearch with the existing corpus.
   ```bash
   curl -u admin:<pass> -X POST https://<host>/api/v1/esindex/reindex
   curl -u admin:<pass>    https://<host>/api/v1/esindex/reindex   # progress
   ```
   Let it finish. Do not change the phase while it runs.
7. Call the readiness endpoint. You want `content.WORKING.osIndexedPercent` and
   `content.LIVE.osIndexedPercent` at or very near `100.0`, and `verdict` `IN_SYNC` on both.
8. **Soak.** For a small customer, hours. For a large one, the standing recommendation is around two
   weeks of real traffic (§13). During the soak, re-check the readiness report periodically and watch
   for shadow-write `WARN`s.

### Phase 1 → 2: move reads to OpenSearch

**What changes:** the customer's search results now come from OpenSearch. Elasticsearch is still
written and still available as a fallback. **This is where plugin and template bugs appear.**

1. **Call the readiness endpoint. `safeToAdvance` must be `true`.** If it is not, read `blockers` and
   fix them — that is almost always "run a full reindex" or "re-crawl this Site Search index".
2. Confirm every Site Search index has been crawled at least once during Phase 1 (§20).
3. Set the phase to `2` on every node.
4. **Restart** (rolling, per §18) so the startup validation runs for the phase you are now in.
5. **Immediately verify the customer's key pages and queries.** Front page, search page, anything
   built on `$estool` or a plugin. This is the moment the §7 and §9 risks materialise.
6. **Watch the log for read-fallback `ERROR`s.** In Phase 2 a failed OpenSearch read is caught and
   retried against Elasticsearch, and logged at `ERROR`. Those lines are your early warning that
   something is wrong with OpenSearch — the customer will not notice, but you should.
7. Soak again. The point of this soak is production *read* load: real queries, real concurrency, real
   aggregations.

> **If anything looks wrong, go back to Phase 1.** It is instant, needs no restart, and the customer
> is served correctly from Elasticsearch again. Do not debug in Phase 2 with the customer live on it.

### Phase 2 → 3: cut over

**What changes:** Elasticsearch stops being written and stops being a fallback. This is the
irreversible-ish step (§21).

1. **Call the readiness endpoint one more time.** `safeToAdvance` must be `true`, `outOfSyncCount`
   must be `0`, and both content rows must show `osIndexedPercent` at `100.0`.
2. Confirm **every** Site Search index has an OpenSearch counterpart that is in sync (§20). Phase 3
   is where a missing counterpart becomes unrecoverable-without-a-crawl.
3. Confirm the OpenSearch cluster's **availability** posture is production-grade — replicas,
   monitoring, alerting. There is no fallback after this.
4. Set the phase to `3` on every node.
5. **Restart** (rolling, per §18). Phase 3 validation is fail-loud: if a node refuses to start, it is
   telling you OpenSearch is not reachable. Stop the rollout and fix that first.
6. Verify search, publishing, and Site Search.
7. Call the readiness endpoint again. **Remember that `safeToAdvance` is forced `true` in Phase 3** —
   read the per-index rows and `osIndexedPercent`, not the boolean (§16).
8. Keep Elasticsearch running, untouched, for an agreed cooling-off period. It is stale from the
   moment you entered Phase 3, but it is still the fastest route back if you need one (§21).

### After Phase 3

- The `.os` indices become visible in the index portlet and in `/api/v1/esindex` for everyone.
  This is expected.
- Elasticsearch can be decommissioned once the cooling-off period ends and the customer has signed
  off.
- `ES_ENDPOINTS` is no longer required — the endpoint-separation check is skipped in Phase 3
  precisely because Elasticsearch is gone.

## 20. Site Search has its own rules

Site Search is the part of the migration most likely to look fine and be broken. It deserves its own
section because it does not follow the content-index model.

### Why it is different

A Site Search index is not built by publishing. It is built by a **crawl** — a scheduled job, or "Run
Now" from the portlet. Nothing about changing the phase causes a crawl to happen.

### The four rules

**1. A phase change never builds Site Search counterparts retroactively.** An index created in Phase
0 lives only on Elasticsearch. Moving to Phase 1 does *not* create its OpenSearch copy. The copy
appears only when a **full crawl runs while in a dual-write phase (1 or 2)**.

**2. Before advancing a phase, let every Site Search crawl run at least once in the current phase.**
The rule of thumb is *"transition when every crawl has run at least once in the current phase"* — not
merely *"the flag is set"*.

**3. After moving to Phase 1, run one full (not incremental) crawl per index before trusting
incrementals.** A full crawl builds the OpenSearch copy with the correct field mapping. An incremental
crawl writes documents in place and, if the OpenSearch copy is missing, lets OpenSearch
**auto-create** it with a *dynamic* mapping — which silently breaks term aggregations and facets.
(dotCMS now gates this: an incremental crawl checks existence *and* document-count parity across the
write engines and demotes itself to a full rebuild if they disagree. But do not rely on the gate when
you can just run the full crawl.)

**4. Reindex the content first, crawl second. Always, in phases 2 and 3.**

> This is the rule that has caused the worst observed damage, so it gets its own explanation.
>
> A Site Search crawl **does not read the database**. It builds its bundle from a **search over the
> content index** — which in phases 2 and 3 means OpenSearch. If the OpenSearch content index has not
> been rebuilt by a full reindex, the crawl simply cannot see the content that is missing from it.
>
> Observed in the field: the content index held 685 live documents on Elasticsearch and 21 on
> OpenSearch (never reindexed). A Phase-3 crawl produced a Site Search index with **14 documents**
> instead of ~443. The crawl answered its query correctly; the corpus it queried was 3% complete.
>
> Three things make this especially nasty:
> - **The crawl reports success.** The counts it logs are of what it bundled, so they look
>   internally consistent.
> - **The bundlers swallow search failures at debug level.** Even a hard search error just produces
>   a smaller bundle.
> - **Reindexing afterwards does not repair it.** The Site Search index keeps its 14 documents until
>   it is crawled again. And the readiness report will not flag it — the index exists on OpenSearch,
>   which in Phase 3 is the expected shape. The defect is *inside* the index, not in its shape.

**There is an opt-in warning for exactly this.** Set
`SITE_SEARCH_CRAWL_MIN_CONTENT_INDEXED_PERCENT` to the percentage below which you want to be warned
(`95` is the intended value; `0`, the default, means off). The crawl then logs a `WARN` naming the
index, the engine, the percentage, and the fact that reindexing afterwards will not repair the
result:

```
Site Search crawl starting against an INCOMPLETE content index: 'working_20260811191012' on
OpenSearch holds 3.06% of the 686 contentlets the database has. […] Run a full reindex first.
```

It is advisory only — it never stops the crawl. It costs one sequential scan of
`contentlet_version_info` plus six engine round-trips **per crawl** while enabled (~15–22 ms in
practice), which is negligible against a crawl but is a recurring cost for a message only someone
diagnosing a migration wants. **Turn it on while you are migrating, read it, turn it off afterwards.**

### Aliases

A Site Search index is known by its alias, never by its `sitesearch_<timestamp>_<uuid>` name. The
readiness report carries the alias **per engine**, because an index can hold its alias on one side and
not the other — created before dual-write started, counterpart built later. That asymmetry is exactly
what you need to see.

If a `recommendation` ends with a `NOTE:` saying the alias looks like an index name, that is the
fingerprint of a crawl-overwrite bug fixed in issue #36983. The fix stops new occurrences but cannot
restore an alias already lost — the report is the only way to find the indices that still need theirs
restored manually.

### Verifying Site Search by hand

```bash
curl -sk -u <user>:<pass> "https://<es-host>:9200/_cat/indices/*sitesearch*?v"
curl -sk -u <user>:<pass> "https://<os-host>:9200/_cat/indices/*sitesearch*?v"
```

Name and document count should agree between the two.

---

# Part V — Recover

## 21. Downgrading to the previous phase

### The good news

**Downgrading is instant and needs no restart.** Routing reads the phase on every call, so setting
the value back takes effect on the next request. This is your primary safety lever and you should not
hesitate to use it.

### The order of preference

| From | To | Cost | Use when |
|:---:|:---:|---|---|
| 2 | 1 | **Free.** Reads go back to Elasticsearch, which is fully current. Dual-write continues. | Anything wrong after moving reads: a plugin, a template, a query returning the wrong set, unexplained errors. **This should be your reflex.** |
| 1 | 0 | Cheap. OpenSearch stops receiving writes and begins to drift. | The OpenSearch cluster itself is a problem — unstable, slow, misconfigured. |
| 3 | 2 | **Expensive.** See below. | Only when Phase 3 is genuinely failing. |
| 3 | 0 | **Expensive.** Requires a full Elasticsearch reindex. | Emergency only. |

### Before you downgrade: check `safeToRollback`

Call the readiness endpoint. `safeToRollback: false` means **OpenSearch holds documents Elasticsearch
does not**. Downgrading would make that content unsearchable until a reindex.

This is not a reason to never downgrade — if the site is broken in Phase 2, getting it working again
matters more. But it tells you that the downgrade comes with a follow-up task, and what that task is.

An **unmeasurable** count on either engine (`docCount: -1`) also makes it unsafe, deliberately: it is
never compared numerically, because treating `-1` as a number would read as a green while OpenSearch
may actually hold more.

### Coming back from Phase 3

Phase 3 is where downgrading stops being free, because Elasticsearch has been receiving **nothing**
since you entered it. Everything published since the cutover exists in the database and in
OpenSearch, but not in Elasticsearch.

Nothing is lost — the database is always authoritative — but the recovery has a cost:

1. Set the phase to **2** (not 0). Phase 2 resumes dual-write, so Elasticsearch starts receiving new
   changes again immediately, and reads still come from OpenSearch, which is complete.
2. **Run a full reindex.** This is what backfills Elasticsearch with everything it missed.
3. Only once the readiness report shows both engines in sync should you consider going to Phase 1 or
   0.

Going 3 → 0 directly leaves the customer reading from a stale Elasticsearch index — which looks like
content loss to them, even though nothing was lost. **Prefer 3 → 2 → reindex → 1.**

### Rolling back the dotCMS build during dual-write

If dotCMS itself is rolled back to an older build while in Phase 1 or 2, the old build stops writing
to OpenSearch — but OpenSearch keeps everything the newer build already pushed, so it silently
**drifts ahead**.

This is harmless for Elasticsearch (which the old build reads and writes) and invisible in Phase 0.
It becomes critical only if the system is later re-upgraded to Phase 2 or 3 without resyncing.

> **Runbook: if you roll the build back during Phase 1 or 2, run a full reindex against OpenSearch
> before re-activating the migration.**

### Downgrading during an in-flight reindex

Rolling the phase back to 0 while a full reindex is draining is a distinct hazard. The phase is
re-read per batch, so the remaining entries index to Elasticsearch only and the OpenSearch reindex
pair freezes half-populated.

dotCMS handles this: the Phase-0 switchover treats it as an OpenSearch reindex abort — the active
OpenSearch rows survive, the reindex slots are cleared, and the partial `.os` pair is deleted from the
cluster so a later boot can never adopt it. It is logged at `WARN` with the deleted index names.

But the OpenSearch pair that survives is the **old** one, and it stops receiving writes in Phase 0, so
it drifts.

> **Rule: prefer letting an in-flight reindex finish, or abort it explicitly, over flipping the phase
> mid-drain. Before re-activating Phase 2 afterwards, run a full reindex.**

### The pre-migration backup index trap

dotCMS lets an administrator activate an old inactive index (Maintenance → Index → *Make Default*) to
roll back to a previous reindex. **If that index predates the migration, it has no OpenSearch
counterpart — and activating it does not create one.**

dotCMS repoints both stores by name transformation, without checking that the OpenSearch index
exists. That is deliberate and correct for normal catchup (where the copy is being built and will
exist in a moment), but it means an old backup silently points OpenSearch at an index that never
existed.

| Phase | What you see |
|:---:|---|
| 1 | Nothing. Writes to the OpenSearch counterpart go nowhere. |
| 2 | Still works — the read fallback drops back to Elasticsearch — but logs an `ERROR` per read. **That is your signal.** |
| 3 | No fallback. Empty results or an exception, which reads to the customer as **lost content**. |

**Three things to know:**

1. **You cannot pre-check a backup.** The readiness report covers only the *active* pair, so a
   divergent backup is invisible while it sits inactive.
2. **The sequence is: activate → call readiness → reindex if it reports `MISSING_COUNTERPART` → only
   then change phase.**
3. **In Phase 3 the verdict does not protect you.** `safeToAdvance` is forced `true` there. Read
   `osIndexedPercent` and the per-index rows.

> **Operational rule: after activating any pre-migration index, run a full reindex before touching
> the phase.**

## 22. Emergency stop

The customer's search is broken and you need it working now.

**1. Identify the phase the instance is actually in.**

```bash
curl -u admin:<pass> https://<host>/api/v1/index/migration/readiness | jq .phase
```

Do this per node if clustered. Do not trust the configured value — a failed startup validation may
have reset it (§12).

**2. Downgrade one step.**

| Currently in | Set it to | Effect |
|:---:|:---:|---|
| 2 | **1** | Reads revert to Elasticsearch instantly. Almost always the right move. |
| 1 | **0** | OpenSearch stops receiving traffic entirely. |
| 3 | **2** | Dual-write resumes; reads stay on OpenSearch. Follow with a full reindex to backfill Elasticsearch. |

No restart is needed for any of these. Push the change to every node at once — this is the one
situation where you do *not* want a slow rolling change.

**3. If you set the phase through the system table, clear it there.** Otherwise the system-table value
keeps winning over whatever you put in the environment, and your change does nothing.

**4. Verify.** Re-read `phase.current` from each node and confirm the customer's search works.

**5. Only then diagnose.** Capture the log lines, the readiness report, and the failing query before
anything is restarted — a restart can clear the evidence you need.

## 23. Troubleshooting quick reference

| Symptom | Most likely cause | Where to look |
|---|---|---|
| Phase set to 1 but no `.os` rows in `indicies` | You did not restart after 0 → 1 | §17 |
| Phase set to 1, restarted, still no `.os` indices | 403 on index-create: the role's index pattern does not match the cluster ID | §11 |
| Startup validation fails, dotCMS boots on ES only | Unreachable / wrong version / same endpoints — **or** missing `cluster:monitor/main` | §11, §12 |
| A "full reindex" returns 200 but creates no index anywhere | OpenSearch 403 misread as "index does not exist"; the first click halts the migration. A second click works but leaves the engines diverged. | §11 |
| dotCMS pauses ~2 minutes at startup | Connection retries against an unreachable OpenSearch | §12 |
| dotCMS refuses to start | Phase 3 with OpenSearch unreachable — fail-loud by design | §12 |
| Search results change between page loads | Cluster nodes in mixed phases | §18 |
| Search empty right after entering Phase 3 | The automatic reindex has not finished | §15 |
| A VTL field renders as a 32-char hash | Field shadowing on `$estool.esSearch` (#37870) | §7 |
| Aggregation buckets empty or wrongly named | Legacy `esSearch`/`esRaw`, or a Site Search index built with a dynamic mapping | §7, §20 |
| A plugin throws `ClassNotFoundException` / `ClassCastException` after Phase 2 | Level-2 or Level-3 ES coupling | §9 |
| Site Search returns far fewer results than expected | Crawled against an incomplete content index | §20 |
| Site Search alias column blank in the portlet | The index exists only on the engine the current phase does not read from | §20 |
| `safeToAdvance: false` with no obvious cause | Read `verdict.blockers` — it names the index and the action | §16 |
| `safeToAdvance: true` in Phase 3 but something is wrong | It is **forced** `true` in Phase 3 — read the rows, not the boolean | §16 |
| `docCount: -1` in the report | The count could not be measured. Not zero. Treated as out of sync deliberately. | §16 |
| Readiness endpoint returns 403 | You are missing the migration support role, or you are not a CMS Admin. Both are required. | §16 |
| `PKIX path building failed` | TLS trust chain, not a migration problem | §11 |

---

# Appendix A — Verification commands

Index names contain timestamps and differ on every instance. **Never guess a name — list it.**

### What dotCMS thinks exists

```bash
# The dotCMS view (hides .os indices before Phase 3, for everyone)
curl -su admin:<pass> https://<host>/api/v1/esindex/indexlist | jq .

# Full stats, and which index is active
curl -su admin:<pass> https://<host>/api/v1/esindex/ | jq .
curl -su admin:<pass> https://<host>/api/v1/esindex/active/type/working
```

### What each engine actually holds

```bash
curl -sk -u <es-user>:<pass> "https://<es-host>:9200/_cat/indices?v"   # no .os
curl -sk -u <os-user>:<pass> "https://<os-host>:9200/_cat/indices?v"   # ends in .os
```

### What the database records

```sql
SELECT * FROM indicies WHERE index_name LIKE '%.os';       -- OpenSearch rows
SELECT * FROM indicies WHERE index_name NOT LIKE '%.os';   -- Elasticsearch rows

SELECT CASE WHEN index_name LIKE '%.os' THEN 'OPENSEARCH' ELSE 'ELASTICSEARCH' END AS engine,
       COUNT(*) FROM indicies GROUP BY 1;

-- What the indices should hold
SELECT COUNT(*) AS working, COUNT(live_inode) AS live FROM contentlet_version_info;
```

> The `_cat/indices` listing and the `indicies` table are **never** filtered. Use them to confirm an
> index really exists when the dotCMS view hides it.

### The readiness report

```bash
curl -su admin:<pass> https://<host>/api/v1/index/migration/readiness | jq .

# Just the go/no-go
curl -su admin:<pass> https://<host>/api/v1/index/migration/readiness | jq .verdict

# Just the completeness numbers
curl -su admin:<pass> https://<host>/api/v1/index/migration/readiness \
  | jq '.content | map_values({esIndexedPercent, osIndexedPercent, verdict})'
```

### Document counts and single documents

```bash
curl -sk -u <user>:<pass> "https://<host>:9200/<physical-index-name>/_count"

# Did this specific write land?
curl -sk -u <os-user>:<pass> \
  "https://<os-host>:9200/<physical-os-index>/_doc/<identifier>_<languageId>_DEFAULT"
```

### Reindex

```bash
curl -su admin:<pass> -X POST   https://<host>/api/v1/esindex/reindex          # start (all types)
curl -su admin:<pass> -X POST  "https://<host>/api/v1/esindex/reindex?contentType=Blog"
curl -su admin:<pass>          https://<host>/api/v1/esindex/reindex          # progress
curl -su admin:<pass> -X DELETE "https://<host>/api/v1/esindex/reindex?switch=true"  # stop + switch
curl -su admin:<pass>          https://<host>/api/v1/esindex/failed           # failed records
```

### Permission check on the OpenSearch service account

```bash
curl -sk -u <os-user>:<pass> "https://<os-host>:9200/"                     # cluster:monitor/main
curl -sk -u <os-user>:<pass> -X PUT "https://<os-host>:9200/cluster_<id>.permcheck" \
     -H 'Content-Type: application/json' \
     -d '{"settings":{"number_of_shards":1,"number_of_replicas":0}}'       # index pattern
curl -sk -u <os-user>:<pass> -X DELETE "https://<os-host>:9200/cluster_<id>.permcheck"
```

### The phase, per node

```bash
# Bypass the load balancer — ask each node directly
curl -su admin:<pass> https://<node-1>/api/v1/index/migration/readiness | jq .phase
```

### Setting the phase through the system table (cluster-wide)

```bash
curl -su admin:<pass> -X POST https://<host>/api/v1/system-table \
  -H 'Content-Type: application/json' \
  -d '{"key":"FEATURE_FLAG_OPEN_SEARCH_PHASE","value":"1"}'

# Read it back
curl -su admin:<pass> https://<host>/api/v1/system-table/FEATURE_FLAG_OPEN_SEARCH_PHASE

# Clear it (important during a rollback — otherwise it keeps winning)
curl -su admin:<pass> -X DELETE https://<host>/api/v1/system-table/FEATURE_FLAG_OPEN_SEARCH_PHASE
```

---

# Appendix B — Configuration reference

Only the settings you actually touch during a migration. The full connection reference is in
[`OPENSEARCH_CLIENT_CONFIGURATION.md`](OPENSEARCH_CLIENT_CONFIGURATION.md).

Every property below has a `DOT_` prefixed environment-variable form
(`FEATURE_FLAG_OPEN_SEARCH_PHASE` → `DOT_FEATURE_FLAG_OPEN_SEARCH_PHASE`).

### Migration control

| Property | Default | What it does |
|---|---|---|
| `FEATURE_FLAG_OPEN_SEARCH_PHASE` | `0` | The phase, `0`–`3`. The one setting that changes behaviour. Unrecognised values mean Phase 0. |
| `OS_MIGRATION_INDEX_VISIBILITY_ROLE_KEY` | `os_migration_qa` | Role **key** required — in addition to CMS Admin — to read the readiness endpoint. |
| `DOTCMS_SHADOW_WRITE_LOG_LEVEL` | `WARN` | Log level for fire-and-forget OpenSearch write failures in phases 1/2. |
| `SITE_SEARCH_CRAWL_MIN_CONTENT_INDEXED_PERCENT` | `0` (off) | Warn when a crawl is about to read a materially incomplete content index. `95` is the intended value. Turn it on while migrating; turn it off afterwards. |

### Source engine (Elasticsearch / OS 1.x)

| Property | Notes |
|---|---|
| `ES_ENDPOINTS` | Comma-separated URLs. |
| `ES_AUTH_TYPE` | `BASIC` \| `JWT` \| `CERT`. |
| `ES_AUTH_BASIC_USER` / `ES_AUTH_BASIC_PASSWORD` | |

### Target engine (OpenSearch 3.x)

| Property | Default | Notes |
|---|---|---|
| `OS_ENDPOINTS` | derived from `OS_HOSTNAME`/`OS_PROTOCOL`/`OS_PORT` | **Must be a separate instance from `ES_ENDPOINTS`.** Preferred way to configure multi-node. |
| `OS_AUTH_TYPE` | `BASIC` | Falls back to `ES_AUTH_TYPE`. |
| `OS_AUTH_BASIC_USER` / `OS_AUTH_BASIC_PASSWORD` | — | Fall back to the `ES_*` equivalents. **Set explicitly if the two clusters differ.** |
| `OS_TLS_CERT_REQUIRED` | `false` | Set `true` in production, with a trusted CA. |
| `OS_TLS_CA_CERT` | — | PEM CA path for a private CA. |
| `OS_TLS_TRUST_SELF_SIGNED` | `false` | Dev/lab only. |
| `OS_CONNECTION_ATTEMPTS` | `24` | Startup connection retries. |
| `OS_CONNECTION_RETRY_SLEEP_SECONDS` | `5` | Sleep between them. 24 × 5s ≈ 2 min startup pause when OpenSearch is down. |
| `OS_INDEX_REPLICAS` | — | Fixed replica count; overrides auto-expand. Set it explicitly. |
| `OS_INDEX_AUTO_EXPAND_REPLICAS` | `0-1` | |
| `opensearch.index.number_of_shards` | `1` | dotCMS recommends `1` unless distributing across disks. |

> **The fallback chain:** every `OS_*` key tries the matching `ES_*` key before its built-in default.
> When you report a configuration finding, always say **which engine's** setting you changed — the
> resolution paths differ.

---

# Appendix C — Log lines worth recognizing

### Healthy startup with the migration on

```
========== OpenSearch Migration — client configuration ==========
  Migration phase   : PHASE_1_DUAL_WRITE_ES_READS
  OS endpoints      : [https://opensearch3:9200]
  Authentication    : BASIC (user=dotcms-es-user, password=***)
  TLS enabled       : true
  TLS cert required : false
  TLS trust selfsign: true
  TLS CA cert       : (not set)
  (connectivity + OS version are verified by the checks that follow)
=================================================================
INFO   OS version check passed: 3.8.0
INFO   Endpoint separation check passed. ES: [...] — OS: [...]
INFO   OpenSearch startup validation PASSED — connected to OS successfully;
       migration phase PHASE_1_DUAL_WRITE_ES_READS is active.
```

> `Authentication : NONE — connecting ANONYMOUSLY` means no credentials resolved. dotCMS will still
> connect if the cluster does not enforce security. On production, that line is a problem.

### Automatic migration halt (phases 1/2)

```
ERROR  OpenSearch startup validation FAILED — halting OS migration; dotCMS falls back to
       ES-only (PHASE_0_MIGRATION_NOT_STARTED): <reason>
ERROR  OpenSearch migration halted: invalid configuration detected at startup. …
WARN   Migration phase reset to PHASE_0_MIGRATION_NOT_STARTED (was PHASE_1_DUAL_WRITE_ES_READS).
       This change is runtime-only …
```

Two `ERROR`s and one `WARN`, never a `FATAL` on this path.

### Phase 3 refusing to start

```
OpenSearch startup validation failed in PHASE_3_OPENSEARCH_ONLY. Cannot auto-rollback to ES
(ES may be decommissioned or stale). Restore OS connectivity or manually reset
FEATURE_FLAG_OPEN_SEARCH_PHASE, then restart dotCMS.
```

### A permission problem, not a connectivity one

```
ERROR  … likelyCause=AUTH_FORBIDDEN … falling back to ES-only
```

This is the §11 index-pattern trap. Widen the OpenSearch role's index pattern to match the cluster
ID.

### A Phase-2 read falling back

```
ERROR  <OpenSearch read failure> … retrying against Elasticsearch
```

The customer got a correct result. You did not. Investigate before advancing to Phase 3.

### A crawl about to build a truncated index

```
WARN   Site Search crawl starting against an INCOMPLETE content index: '…' on OpenSearch holds
       3.06% of the 686 contentlets the database has. […] Run a full reindex first.
```

Only appears when `SITE_SEARCH_CRAWL_MIN_CONTENT_INDEXED_PERCENT` is set (§20).

---

## Where to report what you find

- **Migration bugs and QA findings:** the QA epic, dotCMS/core#35476. Include the phase, both
  physical index names, the query, and the output from each phase.
- **Plugin and template issues:** back to the customer's developer, with the mapping table in §9 and
  the guidance in [`SEARCH_API_MIGRATION.md`](SEARCH_API_MIGRATION.md).
