# ES → OpenSearch Migration Guide

> **Who this is for.** Anyone who has to move a dotCMS install onto OpenSearch 3.x — a Support or
> Cloud engineer handed a customer instance, or a customer running the migration on their own
> installation. It assumes you can reach the instance's dotCMS admin UI, its configuration, its
> database, and both search clusters. It assumes **nothing** about Java or about how the migration
> works internally.
>
> The guide says "the customer" throughout, because that is the party whose search must not break.
> If you are migrating your own installation, that is you.
>
> **How to use it.** Work through the stages in order. Each stage tells you what to do, what to type,
> what you should see, and what to do when you see something else. Each stage ends with a **gate** —
> a short list you must be able to tick before moving on. Do not skip gates; every one of them exists
> because skipping it has cost someone a customer incident.
>
> **You do not need to read Part 3.** It is reference material the stages link into when you want the
> reasoning behind an instruction.

---

## Table of contents

**[Start here](#start-here)** — the ten-minute orientation: why we migrate, the four phases, and when to restart

**Part 1 — The procedure**

| Stage | What you do | Elapsed |
|---|---|---|
| [Stage 0](#stage-0--assess-the-customer) | Assess the customer | half a day |
| [Stage 1](#stage-1--prepare-the-opensearch-server) | Prepare the OpenSearch server | half a day |
| [Stage 2](#stage-2--rehearse-on-a-clone) | Rehearse on a clone | 1–3 days |
| [Stage 3](#stage-3--turn-on-dual-write-phase-0--1) | Turn on dual-write (Phase 0 → 1) | 1 day + soak |
| [Stage 4](#stage-4--move-reads-to-opensearch-phase-1--2) | Move reads to OpenSearch (Phase 1 → 2) | 1 day + soak |
| [Stage 5](#stage-5--cut-over-phase-2--3) | Cut over (Phase 2 → 3) | 1 day |
| [Fast path](#fast-path--small-customers-one-window) | Small customers: one window, straight to Phase 3 | 1 window |

**Part 2 — When something goes wrong**

- [Go back from where you are](#go-back-from-where-you-are)
- [Emergency stop](#emergency-stop)
- [Troubleshooting index](#troubleshooting-index)

**Part 3 — Reference**

- [R1. Concepts and vocabulary](#r1-concepts-and-vocabulary)
- [R2. Why there are four phases](#r2-why-there-are-four-phases)
- [R3. The mirror strategy and where drift comes from](#r3-the-mirror-strategy-and-where-drift-comes-from)
- [R4. How one dotCMS drives two engines](#r4-how-one-dotcms-drives-two-engines)
- [R5. VTL templates and viewTools](#r5-vtl-templates-and-viewtools)
- [R6. Lucene queries and the search endpoints](#r6-lucene-queries-and-the-search-endpoints)
- [R7. OSGi plugins in detail](#r7-osgi-plugins-in-detail)
- [R8. OpenSearch security in detail](#r8-opensearch-security-in-detail)
- [R9. The readiness report, field by field](#r9-the-readiness-report-field-by-field)
- [R10. The phase setting and the restart rule](#r10-the-phase-setting-and-the-restart-rule)
- [R11. Clustered instances](#r11-clustered-instances)
- [R12. Site Search rules](#r12-site-search-rules)
- [R13. Configuration reference](#r13-configuration-reference)
- [R14. Log lines worth recognizing](#r14-log-lines-worth-recognizing)
- [R15. Command reference](#r15-command-reference)

**Companion documents**

| Document | When you need it |
|---|---|
| [`OPENSEARCH_MIGRATION.md`](OPENSEARCH_MIGRATION.md) | The architecture and design behind every rule here. |
| [`OPENSEARCH_CLIENT_CONFIGURATION.md`](OPENSEARCH_CLIENT_CONFIGURATION.md) | Every `OS_*` connection property, its fallback and default. |
| [`OPENSEARCH_MIGRATION_TESTER_GUIDE.md`](OPENSEARCH_MIGRATION_TESTER_GUIDE.md) | Running the local lab stack. |
| [`OPENSEARCH_MIGRATION_TEST_PLAN.md`](OPENSEARCH_MIGRATION_TEST_PLAN.md) | The numbered QA test cases. |
| [`SEARCH_API_MIGRATION.md`](SEARCH_API_MIGRATION.md) | What a plugin developer has to change in their code. |

---

# Start here

**New to this?** Read this chapter end to end — about ten minutes. It explains why the migration
exists, what the four phases are, and the one mechanic people get wrong (when to restart). After that
the guide is a procedure you follow: [Stage 0](#stage-0--assess-the-customer) onward.

---

## 1. Why this migration exists

dotCMS is standardising its search infrastructure on **OpenSearch 3.x**. Every install that is not
already there has to move.

**Two starting points end up in the same guide.** Whatever the customer runs today is the **source
engine**, and it is always configured through the `ES_*` properties — that naming is historical, not
a statement about the product:

| What the customer runs today | Source engine (`ES_*`) | Target engine (`OS_*`) |
|---|---|---|
| Elasticsearch | their Elasticsearch cluster | a new OpenSearch **3.x** cluster |
| OpenSearch **1.x** | their OpenSearch 1.x cluster | a new OpenSearch **3.x** cluster |

An OpenSearch 1.x install migrates by exactly the same procedure as an Elasticsearch one. Everywhere
this guide says "Elasticsearch", read "the engine you are coming from".

**The obvious question first: why can't we just change the URL?**

Because dotCMS's existing client — the one behind `ES_*` — **cannot write content to an OpenSearch
3.x cluster**. That is as true coming from OpenSearch 1.x as from Elasticsearch: 3.x is a breaking
change for the client, not a version bump. Repointing `ES_ENDPOINTS` at the new cluster does not
work — the connection may look fine while content indexing fails. Talking to OpenSearch 3.x needs a
different client, which dotCMS ships alongside the old one.

**So dotCMS temporarily runs two clients at once**, one per engine, and the migration is the
controlled handover between them:

- The two engines start out completely unrelated. The source engine has all the customer's content;
  the **new OpenSearch 3.x cluster is empty** — even if the source engine is itself an OpenSearch,
  the target is a separate cluster with nothing in it.
- You need that new cluster to end up holding the same content, kept current, before anything starts
  depending on it.
- And you need a way back at every point, because a search index is not something a customer can be
  without.

That is the whole shape of the problem. The four phases are how we solve it.

---

## 2. What the search engine actually does for dotCMS

Worth knowing, because it explains why so many things are affected.

dotCMS does not only use the index for "the search box". It keeps a copy of essentially all content
there, and reads it constantly:

| What uses the index | Examples |
|---|---|
| **Content pulls in templates** | `$dotcontent.pull(...)` — most pages on most sites |
| **Search pages** | Anything the visitor types into |
| **The admin content search** | What editors use all day |
| **Site Search** | Built by a separate crawl, its own index |
| **URL maps** | Detail pages resolved by pattern |
| **The REST and GraphQL content APIs** | Headless integrations |
| **Workflow and permission filtering** | Which content a user is allowed to see |

The index holds published content **and** drafts, plus content types, permissions, and workflow
state — split across two indices: `working` (everything) and `live` (published only).

**The practical consequence:** if the index the customer is reading from is incomplete, pages do not
error — they come back **empty or short**. Nothing crashes. That silence is what makes this migration
worth doing carefully.

---

## 3. The four phases, one at a time

One setting, `FEATURE_FLAG_OPEN_SEARCH_PHASE`, holds a number from 0 to 3. It decides two things:
**which engines get written to**, and **which one answers searches**.

The phases move those two independently — writes first, then reads — so that no step ever removes
your way back.

```
             WRITES              READS           Fall back to
  Phase 0    ES                  ES              —            you start here
  Phase 1    ES + OpenSearch     ES              —            OpenSearch is filling up
  Phase 2    ES + OpenSearch     OpenSearch      ES           the real test
  Phase 3    OpenSearch          OpenSearch      nothing      done
```

---

### Phase 0 — where every customer starts

**What it is:** the state every dotCMS is in today. Only the source engine exists — Elasticsearch, or
the customer's existing OpenSearch 1.x. dotCMS never contacts the new OpenSearch 3.x cluster, even if
you have configured it.

**What the customer sees:** normal operation.

**What you do here:** everything in [Stage 0](#stage-0--assess-the-customer) and
[Stage 1](#stage-1--prepare-the-opensearch-server) — assess the customer, and prepare the OpenSearch
server. Both are safe: you can configure OpenSearch completely and dotCMS will still ignore it until
you change the phase.

---

### Phase 1 — dual-write: OpenSearch starts receiving copies

**What changes:** every content change is now written to **both** engines. Publish an article and it
lands in Elasticsearch *and* in OpenSearch.

**What the customer sees:** nothing. Searches are still answered entirely by Elasticsearch. If
OpenSearch fails a write, dotCMS logs it and carries on — the customer's publish still succeeds.

**What this phase proves:** that dotCMS can reach OpenSearch, authenticate, create indices, and write
to them. All the plumbing, with zero customer exposure.

**⚠ The thing everyone gets wrong here:** turning on Phase 1 does **not** copy the content that
already exists. Dual-write only handles changes from this moment forward. A customer with 500,000
contentlets publishing 50 a day would take decades to catch up.

**So Phase 1 is two actions, not one:**

1. Switch to Phase 1 **and restart** — this creates the OpenSearch indices (empty).
2. **Run a full reindex** — this is what actually copies the existing content across.

Then you let it run for a while (a "soak") so real traffic exercises the write path.

---

### Phase 2 — reads move to OpenSearch

**What changes:** searches are now answered by **OpenSearch**. Elasticsearch keeps receiving every
write and stays completely current.

**What the customer sees:** ideally nothing. This is the first time OpenSearch data reaches them.

**The safety net:** if OpenSearch throws an error on a read, dotCMS catches it, logs an `ERROR`, and
**re-runs the search against Elasticsearch**. The customer gets a correct result; you get the error in
the log. That is your early-warning system.

> **But the net only catches errors.** If OpenSearch answers *successfully* with incomplete data —
> because the reindex was never run, or a write silently failed — there is no error to catch. The
> customer just gets fewer results. That is why you verify with the readiness report rather than
> waiting for something to break.

**What this phase proves:** that OpenSearch can serve the customer's real query load correctly.

**⚠ This is where customisations break.** Templates and plugins written against Elasticsearch keep
working perfectly in Phase 1 — because Phase 1 still reads from Elasticsearch. They fail here. A clean
Phase 1 tells you nothing about whether Phase 2 will be clean.

**Your escape hatch:** set the phase back to `1`. It takes effect immediately, needs no restart, and
Elasticsearch is completely current. Use it freely.

---

### Phase 3 — cutover: OpenSearch only

**What changes:** Elasticsearch stops being written and stops being a fallback. OpenSearch is the only
engine.

**What the customer sees:** nothing, if you got here properly.

**What is different now:** an OpenSearch failure reaches the customer instead of being absorbed. And
Elasticsearch starts going stale from this moment — it is no longer receiving anything, so it is a
snapshot, not a live copy.

**Coming back is no longer free.** From Phase 3 you go back to Phase 2 (which resumes dual-write) and
then run a full reindex to refill Elasticsearch with what it missed. Nothing is ever lost — the
database is always the real source of truth — but the recovery costs a window.

---

## 4. Changing the phase: the switch, and when you must restart

This is the mechanic people get wrong, so it gets its own section.

### Where the switch is

The setting is `FEATURE_FLAG_OPEN_SEARCH_PHASE` (as an environment variable,
`DOT_FEATURE_FLAG_OPEN_SEARCH_PHASE`). Values `0`, `1`, `2`, `3`. Anything else, or unset, means
Phase 0.

You can set it in two places, and **they are not equivalent**:

| Where | Reaches | Notes |
|---|---|---|
| Config file / environment variable | **One node.** You must set it on every node yourself. | The usual way. |
| The dotCMS system table (`POST /api/v1/system-table`) | **The whole cluster**, in one call. | Stored in the database. **It wins over the environment variable** — so if you ever set it here, remember that clearing it is part of your rollback. |

Pick one mechanism per customer and stick to it. Mixing them is how you end up with nodes disagreeing
about what phase they are in.

### The restart rule

> **Routing changes instantly. Setup does not.**

Two different things happen when you change the phase:

**Immediately, no restart needed** — dotCMS re-reads the phase on *every single* index operation, so
which engine gets writes and which one answers reads changes on the very next request.

**Only at startup** — three things, and they are the ones that matter:

1. **The OpenSearch indices are created.** This is the big one.
2. The connectivity, version and endpoint checks run.
3. The automatic safety shutdown (fall back to Phase 0) can fire.

So:

| Going | Restart? | Because |
|---|:---:|---|
| **0 → 1** | ✅ **Mandatory** | The OpenSearch indices **do not exist yet**. They are created at boot. |
| **1 → 2** | ✅ **Strongly recommended** | So the startup checks actually run for the phase you are now in. |
| **2 → 3** | ✅ **Strongly recommended** | Same — and Phase 3's check refuses to start on failure, which you want to discover during a controlled restart. |
| **Any downgrade** (2→1, 1→0, 3→2) | ⛔ **Not needed** | Routing reverts on the next request. This is what makes downgrading your emergency lever. |
| After an automatic shutdown | ✅ **Mandatory** | The fallback to Phase 0 lives only in memory. |

### What actually goes wrong if you skip the 0 → 1 restart

It is worth spelling out, because the failure is silent:

1. You set the phase to `1`. Routing switches immediately — dotCMS now sends every write to both
   engines.
2. But nothing created the OpenSearch indices, because that only happens at boot.
3. So every write to OpenSearch targets an index that does not exist.
4. And in Phase 1, OpenSearch write failures are **logged and ignored** by design, so the customer's
   publishes all succeed and nothing surfaces.
5. Meanwhile the startup validation never ran, so you also have no confirmation that OpenSearch is
   even reachable.

The result is a migration that looks switched on and is doing absolutely nothing. Hours later you
notice OpenSearch is empty.

**One line to remember: going up a phase, change the value *and then restart*. Coming down, just
change the value.**

---

## 5. The three rules to keep in your head

> ### Rule 1 — A phase change never copies data. Only a reindex does.
> Setting Phase 1 does **not** put the customer's existing content into OpenSearch. It only starts
> sending *new* writes there. Filling OpenSearch with what already exists is a separate, explicit
> action: a **full reindex**. Forgetting this is the number-one way a migration goes wrong.

> ### Rule 2 — Going up a phase needs a restart. Coming down does not.
> Routing changes the instant you change the value. But the OpenSearch index creation and the
> connectivity validation only happen at **startup**. Turn on Phase 1 without restarting and you get a
> migration that looks enabled and is silently doing nothing.

> ### Rule 3 — The readiness endpoint is the truth. Nothing else is.
> `GET /api/v1/index/migration/readiness` compares both engines from live data and tells you whether
> it is safe to move. The admin UI deliberately **hides** the OpenSearch indices before Phase 3, and
> raw document counts mislead in ways explained in
> [R9](#r9-the-readiness-report-field-by-field). The endpoint does not.

---

## 6. Choose your path

Answer one question: **how long does a full reindex take on this customer?**

| | **Fast path** | **Standard path** |
|---|---|---|
| Reindex duration | Fits comfortably in one maintenance window | Does not |
| Rough size | up to ~50k contentlets | above that |
| What you do | Set Phase 3, restart, let it rebuild | 0 → 1 → 2 → 3, days or weeks apart |
| Search during the change | **Degraded** — empty until the reindex finishes | Never degraded |
| Rollback | Costs a second window (Elasticsearch has to be reindexed too) | Instant, at every step |
| Go to | [Fast path](#fast-path--small-customers-one-window) | [Stage 0](#stage-0--assess-the-customer) |

**If you do not know the reindex duration yet, you are on the standard path** — Stage 0 and Stage 2
measure it, and you can switch to the fast path afterwards if it turns out to be short.

**The fast path is off the table**, whatever the size, if any of these is true:

- The customer has an OSGi plugin bound to Elasticsearch that nobody can recompile.
- Site Search is business-critical and its crawls cannot be run on demand.
- There is no window in which degraded search is acceptable.

---

## 7. What you need before Stage 1

- [ ] Admin access to the customer's dotCMS (CMS Administrator).
- [ ] Access to the customer's database (read is enough for the guide; you will run `SELECT`s).
- [ ] Network access to the current search cluster and to the new OpenSearch 3.x cluster.
- [ ] Admin credentials on the OpenSearch cluster, to create the service account and role.
- [ ] The ability to change the customer's dotCMS configuration and restart their nodes.
- [ ] Somewhere to build a clone (Stage 2).

---

## 8. How this guide is organised

**Part 1 is the procedure** — six stages, in order. Each stage says what you are doing, what to type,
what you should see, and what to do when you see something else. Each ends with a **gate**: a short
list you must be able to tick before moving on. Do not skip gates.

| Stage | You are | Phase after |
|---|---|:---:|
| [0](#stage-0--assess-the-customer) | Assessing the customer | 0 |
| [1](#stage-1--prepare-the-opensearch-server) | Preparing the OpenSearch server | 0 |
| [2](#stage-2--rehearse-on-a-clone) | Rehearsing on a clone | — |
| [3](#stage-3--turn-on-dual-write-phase-0--1) | Turning on dual-write | 1 |
| [4](#stage-4--move-reads-to-opensearch-phase-1--2) | Moving reads to OpenSearch | 2 |
| [5](#stage-5--cut-over-phase-2--3) | Cutting over | 3 |

**Part 2 is what to do when something goes wrong** — how to go back from wherever you are, the
emergency stop, and a table that turns a symptom into the step that fixes it.

**Part 3 is reference (R1–R15).** You do not have to read it. The stages link into it when you want
the reasoning behind an instruction.

---

# Part 1 — The procedure

---

# Stage 0 — Assess the customer

**Goal:** know what you are dealing with before you plan anything.
**Elapsed:** half a day.
**Risk to the customer:** none. Everything here is read-only.

---

### Step 0.1 — Measure the content

**Why:** the size of the corpus determines the reindex duration, which determines the entire plan.

**Do this** against the customer's database:

```sql
-- The two numbers that matter. This is also the exact denominator the readiness
-- endpoint uses, so you can compare against it later.
SELECT COUNT(*) AS working, COUNT(live_inode) AS live FROM contentlet_version_info;
```

**Record:** the working and live counts.

**Then measure the indices** on the current engine:

```bash
curl -sk -u <es-user>:<pass> "https://<es-host>:9200/_cat/indices?v"
```

**Record:** how many dotCMS indices exist, and the `store.size` of the working index.

---

### Step 0.2 — Audit the VTL templates

**Why:** templates that call the legacy `$estool` methods receive Elasticsearch objects. They keep
working in Phase 1 and **break in Phase 2**. This is the most common source of customer-visible
breakage. Background: [R5](#r5-vtl-templates-and-viewtools).

**Do this** over the customer's templates, widgets, and `.vtl` files:

```bash
grep -rn "esSearch\|esRaw" <path-to-customer-vtl>
```

**Every hit is a template that must be reworked before Stage 4.** The safe replacements are
`$estool.search(...)` and `$estool.raw(...)`.

**Also look for** templates that walk aggregations — `.aggregations`, `.buckets`,
`getKeyAsString()`, `getDocCount()`. These are supposed to keep working verbatim, but they are the
thing you will smoke-test hardest in Stage 4.

**Record:** the list of affected templates, and who owns them.

---

### Step 0.3 — Audit the OSGi plugins

**Why:** a plugin compiled against Elasticsearch classes fails at Phase 2, and permanently once
Elasticsearch is dropped. Unlike a template, you cannot fix it yourself. Background:
[R7](#r7-osgi-plugins-in-detail).

**Do this** for each of the customer's bundle jars:

```bash
# Does the bundle import Elasticsearch packages? (the worst case)
unzip -p <bundle>.jar META-INF/MANIFEST.MF | tr ',' '\n' | grep -i elasticsearch

# Does the compiled code reference ES classes or the deprecated search methods?
unzip -o <bundle>.jar -d /tmp/bundle >/dev/null
grep -rl "org/elasticsearch" /tmp/bundle --include=*.class
grep -rl "esSearchRaw\|esSearch"  /tmp/bundle --include=*.class
```

**Grade each plugin:**

| Result | Level | Meaning |
|---|:---:|---|
| No hits at all | **1** | Safe. Nothing to do. |
| Hits on `esSearch` / `esSearchRaw` only | **2** | Breaks at Phase 2. Recompile needed. |
| Hits on `org/elasticsearch` | **3** | Breaks at Phase 2 and will not even load later. Recompile mandatory. |

**Record:** every Level-2 and Level-3 plugin, and **who is going to recompile it**. Hand them
[R7](#r7-osgi-plugins-in-detail) and [`SEARCH_API_MIGRATION.md`](SEARCH_API_MIGRATION.md).

> **A Level-3 plugin with no owner stops the migration.** Do not proceed past Stage 2 hoping it will
> resolve itself.

---

### Step 0.4 — Inventory Site Search

**Why:** Site Search does not follow the content-index rules and is the part most likely to look fine
and be broken. Background: [R12](#r12-site-search-rules).

**Do this** in the customer's dotCMS: open the **Site Search** portlet and record

- how many Site Search indices exist,
- their aliases,
- the crawl schedule for each — and **whether you can trigger a crawl on demand**.

**Record:** if Site Search is business-critical and you cannot run crawls on demand, flag it now.
Every stage from 3 onward requires a crawl.

---

### Step 0.5 — Decide the path

**You now have** the content volume, the template list, the plugin grades, and the Site Search
inventory. Use the table in [Choose your path](#6-choose-your-path).

If you are still unsure, stay on the standard path. Stage 2 measures the reindex on real data and you
can switch then.

---

### ▸ Gate 0 — do not continue until

- [ ] You have the working and live contentlet counts.
- [ ] You have grepped the VTL for `esSearch` / `esRaw` and listed the hits.
- [ ] You have graded every OSGi plugin, and **every Level-2/3 plugin has a named owner**.
- [ ] You know the Site Search index count and whether crawls can be run on demand.
- [ ] You have chosen a path, or explicitly deferred the choice to Stage 2.

---

# Stage 1 — Prepare the OpenSearch server

**Goal:** an OpenSearch 3.x cluster that dotCMS can actually reach, authenticate against, and create
indices on.
**Elapsed:** half a day.
**Risk to the customer:** none. dotCMS is still in Phase 0 and never touches OpenSearch.

> **Do this stage properly and you avoid the two most confusing failures in the whole migration** —
> both of which look like "the migration silently does nothing". Background:
> [R8](#r8-opensearch-security-in-detail).

---

### Step 1.1 — Confirm the version and the separation

**Why:** dotCMS asserts at startup that the cluster is OpenSearch **3.x** and that it is **not the
same server** as the source engine. Either check failing halts the migration.

**Do this:**

```bash
curl -sk -u <os-admin>:<pass> "https://<os-host>:9200/" | jq .version.number
```

**You should see:** a version starting with `3.` (for example `3.8.0`).

**Confirm separation yourself.** dotCMS compares the two endpoint strings, which means
`127.0.0.1:9200` and `localhost:9200` are the same machine but will **not** be detected as
overlapping. Verify by hand that the OpenSearch 3.x endpoint is a genuinely different cluster from
the source engine's.

> **If the customer is already on OpenSearch 1.x**, this is the step where the temptation is to
> upgrade their existing cluster in place and point both `ES_*` and `OS_*` at it. Do not. The whole
> migration depends on two independent clusters, so that the source engine stays intact and you can
> fall back to it at every phase. Stand up a separate OpenSearch 3.x cluster; the 1.x one is
> decommissioned at the end of [Stage 5](#stage-5--cut-over-phase-2--3), like any other source
> engine.

---

### Step 1.2 — Settle transport security

**Why:** a TLS problem surfaces during the migration and looks like a migration problem. Fix it now.

**Do this** — decide these values and write them down; you will set them in Step 1.5.

| Setting | Production value |
|---|---|
| `OS_ENDPOINTS` | `https://…` — TLS activates from the scheme; you do not also need `OS_TLS_ENABLED` |
| `OS_TLS_CERT_REQUIRED` | **`true`** (the default is `false`, which skips certificate and hostname verification) |
| `OS_TLS_CA_CERT` | Path to the PEM CA, when the cluster uses a private CA |
| `OS_TLS_TRUST_SELF_SIGNED` | **Never `true` in production.** Lab only. |

**If you see `PKIX path building failed` later**, it is this step: the CA is not in the JVM truststore
and `OS_TLS_CA_CERT` is not set.

---

### Step 1.3 — Create the service account and role

**Why:** dotCMS does **not** need an OpenSearch admin account. Give it a least-privilege role scoped
to the customer's indices.

**Do this** — create a user (conventionally `dotcms-es-user`) mapped to a role with:

**Cluster permissions:**

```
cluster:monitor/health
cluster:monitor/state
cluster:monitor/nodes/stats
cluster:monitor/main          ← the one everyone forgets. See the warning below.
indices:data/write/bulk
indices:data/read/scroll
indices:data/read/scroll/clear
```

**Index permissions on `cluster_<customer>*`:** `indices_all`, `indices_monitor`

**Index permissions on `*` (read-only, for listing and stats):** `indices:monitor/stats`,
`indices:monitor/settings/get`, `indices:admin/aliases/get`

> The reference implementation of exactly this provisioning is
> `docker/docker-compose-examples/single-node-os-migration/opensearch.py`.

> ### ⚠️ `cluster:monitor/main` — read this before you move on
>
> dotCMS reads the cluster version with `GET /`, which maps to `cluster:monitor/main`. Historic
> dotCMS roles never granted it.
>
> Without it, `GET /` returns **403**, dotCMS cannot tell a 403 from an unreachable cluster, and it
> concludes the cluster is down. It then **halts the migration, resets to Phase 0, and boots normally
> on Elasticsearch alone.** Nothing is broken. Nothing is migrating either. You will be staring at a
> perfectly healthy OpenSearch cluster wondering why it is empty.
>
> Spike #35922 confirmed this is the **only** permission gap between the historic dotCMS role and
> OpenSearch 3.x.

---

### Step 1.4 — Run the three permission checks

**Why:** this is the step that catches the second confusing failure — an index pattern that does not
match the customer's cluster ID. dotCMS's own startup check **cannot see it**, because it only probes
the cluster root, which succeeds.

**First, find the customer's real cluster prefix** by looking at an existing Elasticsearch index name:

```bash
curl -sk -u <es-user>:<pass> "https://<es-host>:9200/_cat/indices?v" | grep working
# e.g. cluster_08abc3567e.working_20230101   →   the prefix is cluster_08abc3567e
```

**Then run all three checks as the dotCMS service account:**

```bash
# 1 — the version probe (cluster:monitor/main)
curl -sk -u <os-user>:<pass> "https://<os-host>:9200/"

# 2 — can it create an index with the customer's real prefix?
curl -sk -u <os-user>:<pass> -X PUT "https://<os-host>:9200/cluster_08abc3567e.permcheck" \
  -H 'Content-Type: application/json' \
  -d '{"settings":{"number_of_shards":1,"number_of_replicas":0}}'

# 3 — clean up
curl -sk -u <os-user>:<pass> -X DELETE "https://<os-host>:9200/cluster_08abc3567e.permcheck"
```

**All three must succeed.**

**If check 2 returns 403:** the role's index pattern does not match the cluster ID. **Widen the
OpenSearch role's pattern.** Do *not* try to change `DOT_DOTCMS_CLUSTER_ID` — it is written to the
`dot_cluster` table on first boot and read from there forever; changing the environment variable on an
instance that already has data does nothing.

> **What this failure looks like if you skip the check:** a "full reindex" that returns HTTP 200 with
> a completed progress bar and creates **no index on either engine**, plus an `ERROR` line naming
> `likelyCause=AUTH_FORBIDDEN`. Issue #36222.

---

### Step 1.5 — Point dotCMS at OpenSearch

**Why:** these are the connection settings. Setting them now is harmless — dotCMS in Phase 0 never
uses them.

**Do this** — set on every node:

```properties
OS_ENDPOINTS=https://<os-host>:9200
OS_AUTH_TYPE=BASIC
OS_AUTH_BASIC_USER=dotcms-es-user
OS_AUTH_BASIC_PASSWORD=<password>
OS_TLS_CERT_REQUIRED=true
OS_TLS_CA_CERT=/path/to/ca.pem
OS_INDEX_REPLICAS=<n>
```

(As environment variables, prefix each with `DOT_`: `DOT_OS_ENDPOINTS`, and so on.)

> **Set the credentials explicitly, even if they look redundant.** Every `OS_*` key silently falls
> back to its matching `ES_*` key when unset. An instance that never configured OpenSearch
> credentials will **reuse the Elasticsearch ones** rather than failing — which works only by
> accident, and breaks the moment the two clusters differ.

> **Set `OS_INDEX_REPLICAS` explicitly.** OpenSearch does not inherit dotCMS's implicit default.

**Do not change the phase yet.**

---

### ▸ Gate 1 — do not continue until

- [ ] `GET /` reports a version starting with `3.`.
- [ ] The OpenSearch cluster is confirmed to be a different server from Elasticsearch.
- [ ] TLS is settled: `OS_TLS_CERT_REQUIRED=true` with a trusted CA, or a documented exception.
- [ ] The service account exists with the permission set from Step 1.3, **including
      `cluster:monitor/main`**.
- [ ] All three checks in Step 1.4 pass, using the customer's **real** cluster prefix.
- [ ] `OS_*` connection settings are set on every node, with credentials set explicitly.
- [ ] The phase is still `0`.

---

# Stage 2 — Rehearse on a clone

**Goal:** run this exact procedure once, on a copy, so that the first time you do it on the
customer's real instance is not the first time you have done it.
**Elapsed:** 1–3 days.
**Risk to the customer:** none. You are not touching their environment.

> **This is the highest-value stage in the guide.** It is not a QA exercise — you are not writing test
> cases and you are not signing off on dotCMS. You are doing a **dry run of the migration** to learn
> two things you cannot learn any other way: **how long the reindex takes on this customer's data**,
> and **whether their own templates and plugins survive Phase 2**.
>
> If the customer is small enough that you are considering the fast path, this stage matters *more*,
> not less — the fast path has a window of degraded search and this is where you find out how long it
> actually lasts.

---

### Step 2.1 — Build the clone

**Why:** the numbers only mean something if the data volume and the customisations are the customer's.

**Do this** — stand up an instance with:

| Component | Requirement |
|---|---|
| Database | A restore of the customer's database. **Not a subset** — the volume is the point. |
| Assets | The asset store, or enough of it for Site Search crawls to be realistic. |
| Source engine | A separate engine holding a copy of their indices, or a fresh one you reindex into. |
| OpenSearch | A separate OpenSearch 3.x, provisioned with the **same role and index pattern** as the real one. |
| Plugins | Their actual bundle jars, deployed. |
| dotCMS | The **same build** you will run in production. |

> **Do not "fix" the cluster ID.** It comes from the restored database, so the clone naturally carries
> the customer's real cluster ID — which is exactly what makes Step 1.4's permission check meaningful
> here.

---

### Step 2.2 — Run Stages 3, 4 and 5 on the clone

**Why:** this is the rehearsal. Follow the stages exactly as written, on the clone.

**Do this:** go to [Stage 3](#stage-3--turn-on-dual-write-phase-0--1), work through to
[Stage 5](#stage-5--cut-over-phase-2--3), then come back here.

**As you go, write down:**

1. **The reindex duration** (Step 3.8). This is the number that drives the whole plan.
2. **Anything in the customer's site that changed** between Phase 1 and Phase 2 (Step 4.4). Every
   difference is something you must have fixed before you do this for real.
3. **Every command that needed adjusting** for this customer — host names, credentials, paths. Your
   real-run runbook should be copy-pasteable.

---

### Step 2.3 — Practise the downgrade

**Why:** you want to have done this once before you need to do it under pressure.

**Do this** on the clone, from Phase 2:

1. Set the phase back to `1` on every node.
2. Confirm — without restarting anything — that the site is serving correctly again.

**You should see:** the change take effect on the next request. No restart, no downtime.

**Record:** how you set the phase and how long it took to reach every node. That is your emergency
procedure.

---

### Step 2.4 — Write the window plan

**Why:** the customer needs real timings, not estimates.

**Do this** — write down, using the numbers you measured:

- The reindex duration, and therefore the length of the window each stage needs.
- Which path you are taking (standard or fast) — revisit
  [Choose your path](#6-choose-your-path) now that you have the real number.
- The rolling-restart sequence for their node count.
- Who is fixing which template and which plugin, and by when.

---

### ▸ Gate 2 — do not continue until

- [ ] The clone reached Phase 3 successfully.
- [ ] You have a **measured** reindex duration on the customer's real data volume.
- [ ] The customer's site rendered the same in Phase 1 and Phase 2 on the clone — or every difference
      is understood and has an owner.
- [ ] Every Level-2/3 plugin has been recompiled and confirmed working on the clone.
- [ ] You have practised the downgrade.
- [ ] You have a written window plan with real timings.

---

# Stage 3 — Turn on dual-write (Phase 0 → 1)

**Goal:** OpenSearch starts receiving every write, and gets filled with the content that already
exists.
**Elapsed:** one working day, then a soak.
**Risk to the customer:** very low. Nothing they see depends on OpenSearch in this phase. An
OpenSearch failure is logged and ignored.

---

### Step 3.1 — Give yourself access to the readiness endpoint

**Why:** it is the only reliable view of migration state, and it needs a role you probably do not
have yet. Set this up before you need it.

**Do this** in the customer's dotCMS:

1. Create a Role and set its **Key** to `os_migration_qa`.
   > It is matched on the role's **Key** field, not its display name. A role called "OS Migration QA"
   > with an empty Key does nothing.
2. Assign that role to the CMS Administrator account you will use.

**Confirm:**

```bash
curl -su admin@dotcms.com:<pass> \
  https://<host>/api/v1/index/migration/readiness | jq .phase
```

**You should see:** a JSON object with `current`, `name`, `readEngine`, `writeEngines`, `dualWrite`.

**If you get 403:** you are missing the role, **or** you are not a CMS Administrator. Both are
required, always.

---

### Step 3.2 — Take the baseline

**Why:** you will want to compare against this later, and it proves the endpoint works before you
depend on it.

**Do this:**

```bash
curl -su admin:<pass> https://<host>/api/v1/index/migration/readiness \
  > baseline-phase0-$(date +%F).json
```

**Record:** save the file with the ticket.

---

### Step 3.3 — Set the phase to 1

**Why:** this is the switch. Background on where it lives and which source wins:
[R10](#r10-the-phase-setting-and-the-restart-rule).

**Pick one mechanism and use it consistently for this customer:**

**Option A — configuration, per node** (survives restart; you must update every node):

```properties
FEATURE_FLAG_OPEN_SEARCH_PHASE=1
```
or as an environment variable, `DOT_FEATURE_FLAG_OPEN_SEARCH_PHASE=1`.

**Option B — the system table, cluster-wide** (one call, all nodes; stored in the database):

```bash
curl -su admin:<pass> -X POST https://<host>/api/v1/system-table \
  -H 'Content-Type: application/json' \
  -d '{"key":"FEATURE_FLAG_OPEN_SEARCH_PHASE","value":"1"}'
```

> **The system table wins over the configuration file and the environment variable.** If you use
> Option B, remember that **clearing it is part of your rollback procedure** — otherwise a later
> change to the environment variable will appear to do nothing. Do not mix the two mechanisms.

---

### Step 3.4 — Restart, one node at a time

**Why:** the OpenSearch indices are created at **startup**, not when you change the flag. This restart
is **mandatory** — Rule 2. Skip it and dual-writes fan out to an index that was never created, and
those misses are silently swallowed.

**Do this, per node:**

1. **Drain the node** at the load balancer.
2. **Restart it.**
3. **Wait for the log lines in Step 3.5** before touching the next node.
4. **Return it to the pool.**

> **Stop the rollout if the first node fails.** The rest will fail identically, and you would rather
> find out with one node down than all of them.

---

### Step 3.5 — Confirm the startup log

**Why:** this is where dotCMS tells you whether it actually connected. Full reference:
[R14](#r14-log-lines-worth-recognizing).

**You should see:**

```
========== OpenSearch Migration — client configuration ==========
  Migration phase   : PHASE_1_DUAL_WRITE_ES_READS
  OS endpoints      : [https://opensearch:9200]
  Authentication    : BASIC (user=dotcms-es-user, password=***)
  ...
=================================================================
INFO   OS version check passed: 3.8.0
INFO   Endpoint separation check passed. ES: [...] — OS: [...]
INFO   OpenSearch startup validation PASSED — connected to OS successfully;
       migration phase PHASE_1_DUAL_WRITE_ES_READS is active.
```

**If you see `Authentication : NONE — connecting ANONYMOUSLY`:** your credentials did not resolve.
dotCMS will still connect if the cluster does not enforce security — on production that line is a
problem, not a note.

**If you see this instead:**

```
ERROR  OpenSearch startup validation FAILED — halting OS migration; dotCMS falls back to
       ES-only (PHASE_0_MIGRATION_NOT_STARTED): <reason>
```

dotCMS is running normally on Elasticsearch and the migration is off. **Stop the rollout** and go to
[Troubleshooting](#troubleshooting-index). `<reason>` will be one of: not reachable, wrong version, or
same endpoints as Elasticsearch.

> **If dotCMS pauses for ~2 minutes at startup**, that is expected when OpenSearch is unreachable —
> it retries 24 times, 5 seconds apart, before giving up. Do not kill the container.

---

### Step 3.6 — Confirm the OpenSearch indices were created

**Why:** this is the proof that Rule 2 was honoured. If this returns nothing, the migration is not
running no matter what the flag says.

**Do this** against the customer's database:

```sql
SELECT * FROM indicies WHERE index_name LIKE '%.os';
```

**You should see:** **two rows** — a working index and a live index, both ending in `.os`.

**If you see none:** the bootstrap did not happen. Check the log for `likelyCause=AUTH_FORBIDDEN` —
that is the Step 1.4 index-pattern problem, and it means the role's pattern does not match the
cluster ID.

> **Do not look for these in the admin UI.** The index portlet and `/api/v1/esindex` **hide** the
> `.os` indices in Phases 0, 1 and 2, from everyone. That is by design. The database and
> `_cat/indices` are never filtered.

---

### Step 3.7 — Confirm one write reaches both engines

**Why:** this is your end-to-end proof that dual-write works, and it takes two minutes.

**Do this:**

1. Publish or edit one piece of content in dotCMS. Note its identifier and language.
2. Get the OpenSearch physical index name from the readiness report:

   ```bash
   curl -su admin:<pass> https://<host>/api/v1/index/migration/readiness \
     | jq -r '.content.WORKING.os.physicalName'
   ```

3. Ask OpenSearch for **the document** — not the count:

   ```bash
   curl -sk -u <os-user>:<pass> \
     "https://<os-host>:9200/<physicalName>/_doc/<identifier>_<languageId>_DEFAULT"
   ```

**You should see:** `"found": true`, with the `modDate` of your edit.

> **Why the document and not the count.** A count that does not move is not proof that nothing was
> written: re-publishing content already in the index is an *update*, so the total stays put. And in
> Phase 1 the OpenSearch copy holds only what has changed since you enabled it — a mirror sitting at
> 15 of 683 documents is the **expected** state until Step 3.8.

---

### Step 3.8 — Run the full reindex

**Why:** **Rule 1.** This is the only operation that puts the customer's existing content into
OpenSearch. Everything up to here only handles new writes.

**Do this:**

```bash
# Start it
curl -su admin:<pass> -X POST https://<host>/api/v1/esindex/reindex

# Watch it
curl -su admin:<pass> https://<host>/api/v1/esindex/reindex
```

**Expect it to take** roughly what you measured in Stage 2.

> **Do not change the phase while it runs.** Flipping the phase mid-reindex is a known hazard — see
> [Go back from where you are](#go-back-from-where-you-are). If you must stop it, stop it explicitly:
> `curl -su admin:<pass> -X DELETE "https://<host>/api/v1/esindex/reindex?switch=true"`.

**When it finishes, check for failures:**

```bash
curl -su admin:<pass> https://<host>/api/v1/esindex/failed
```

---

### Step 3.9 — Crawl every Site Search index

**Why:** a phase change does **not** create Site Search counterparts, and neither does the content
reindex. Only a crawl does. Background: [R12](#r12-site-search-rules).

**Do this, in this order:**

1. **Confirm Step 3.8 finished first.** A crawl builds its corpus by querying the content index. In
   Phase 1 that query still goes to Elasticsearch, so the ordering is not yet critical — but it
   becomes critical from Phase 2 on, and getting into the habit now costs nothing.
2. For **each** Site Search index, run a **full** crawl — not an incremental one — from the Site
   Search portlet ("Run Now").

> **Full, not incremental.** A full crawl builds the OpenSearch copy with the correct field mapping.
> An incremental crawl writes documents in place and can leave a copy with a *dynamic* mapping, which
> silently breaks term aggregations and facets.

**Confirm both engines agree:**

```bash
curl -sk -u <es-user>:<pass> "https://<es-host>:9200/_cat/indices/*sitesearch*?v"
curl -sk -u <os-user>:<pass> "https://<os-host>:9200/_cat/indices/*sitesearch*?v"
```

Names and document counts should match.

---

### Step 3.10 — Confirm with the readiness report

**Why:** this is the objective statement that Stage 3 worked.

**Do this:**

```bash
curl -su admin:<pass> https://<host>/api/v1/index/migration/readiness | jq
```

**You should see:**

| Field | Expected |
|---|---|
| `phase.current` | `1` |
| `content.WORKING.osIndexedPercent` | `100.0`, or very close |
| `content.LIVE.osIndexedPercent` | `100.0`, or very close |
| `content.WORKING.verdict` / `content.LIVE.verdict` | `IN_SYNC` |
| every `siteSearch[].verdict` | `IN_SYNC` |
| `verdict.outOfSyncCount` | `0` |

**If `osIndexedPercent` is low:** the reindex did not finish, or it failed. Re-read Step 3.8.

**If a `siteSearch` row says `MISSING_COUNTERPART`:** that index has not been crawled in a dual-write
phase. Go back to Step 3.9 and crawl it.

**If any `docCount` is `-1`:** the count could not be measured. **This is not zero** — it is treated
as out of sync on purpose. Investigate connectivity to that engine before continuing.

> **On a clustered instance, check the phase on every node**, bypassing the load balancer. Nodes
> disagreeing about the phase is a real failure mode — see [R11](#r11-clustered-instances).

---

### Step 3.11 — Soak

**Why:** real production traffic surfaces intermittent write failures, permission edge cases and
mapping mismatches that a one-day run never reaches.

**How long:**

| Customer | Soak |
|---|---|
| Small | hours to a day |
| Medium | several days |
| Large | ~2 weeks of real traffic |

**During the soak:**

- Re-run the readiness report periodically. `osIndexedPercent` should stay at `100.0`.
- Watch the log for shadow-write `WARN`s. In Phase 1 an OpenSearch write failure is **logged and
  ignored** — the customer's publish succeeds and nothing surfaces. Those `WARN`s are the only live
  signal. (Level configurable via `DOTCMS_SHADOW_WRITE_LOG_LEVEL`.)

---

### ▸ Gate 3 — do not continue until

- [ ] Every node logged `OpenSearch startup validation PASSED`.
- [ ] Two `.os` rows exist in `indicies`.
- [ ] A test publish was confirmed **by document** on OpenSearch.
- [ ] The full reindex completed, with no failed records.
- [ ] Every Site Search index has been fully crawled and both engines agree on its count.
- [ ] The readiness report shows `IN_SYNC` everywhere and `outOfSyncCount: 0`.
- [ ] Every node reports the same phase.
- [ ] The soak has run for the agreed period with no unexplained `WARN`s.
- [ ] **Every Level-2/3 plugin and every `esSearch`/`esRaw` template from Stage 0 is fixed and
      deployed.** They break in Stage 4, not here.

---

# Stage 4 — Move reads to OpenSearch (Phase 1 → 2)

**Goal:** the customer's search results start coming from OpenSearch, with Elasticsearch still
written and still available as a fallback.
**Elapsed:** one working day, then a soak.
**Risk to the customer:** **this is the stage where customisations break.** Templates and plugins that
were fine in Phase 1 fail here. Everything in Stage 0's audit was for this moment.

> **Your safety net:** if anything is wrong, set the phase back to `1`. It takes effect immediately,
> needs no restart, and Elasticsearch is completely current. Do not debug in Phase 2 with the customer
> live on it.

---

### Step 4.1 — Confirm it is safe to advance

**Why:** `safeToAdvance` is a computed answer to exactly the question you are about to act on.

**Do this:**

```bash
curl -su admin:<pass> https://<host>/api/v1/index/migration/readiness | jq .verdict
```

**You should see:** `"safeToAdvance": true` and `"outOfSyncCount": 0`.

**If `safeToAdvance` is `false`:** read `verdict.blockers`. Each entry names an index and the action.
It is almost always "run a full reindex" (go back to Step 3.8) or "re-crawl this Site Search index"
(Step 3.9). Fix them and re-run this step.

> `safeToAdvance` is blocked when the OpenSearch copy is **behind** — promoting would lose that delta.
> The separate `safeToRollback` is blocked when OpenSearch is **ahead**. They are not opposites and
> both can be `false`. Detail: [R9](#r9-the-readiness-report-field-by-field).

---

### Step 4.2 — Set the phase to 2

**Do this** using the same mechanism you chose in Step 3.3, on every node.

---

### Step 4.3 — Restart, one node at a time

**Why:** routing switches live, but only a restart re-runs the startup validation for the phase you
are now actually in. Validate the phase you actually run.

**Do this:** the same drain → restart → confirm log → return to pool sequence as Step 3.4.

**You should see** in each node's log:

```
INFO   OpenSearch startup validation PASSED — connected to OS successfully;
       migration phase PHASE_2_DUAL_WRITE_OS_READS is active.
```

---

### Step 4.4 — Walk the customer's site

**Why:** this is the moment the Stage 0 risks either materialise or do not. Do it now, immediately,
while you still have the phase-1 downgrade one command away.

**Do this** — open, in a browser, with the customer:

- [ ] The home page.
- [ ] Their search page or search results template.
- [ ] Every page you identified in Step 0.2 as using `$estool`.
- [ ] Every page with facets or aggregations.
- [ ] Any page driven by an OSGi plugin from Step 0.3.
- [ ] Site Search results and their facets.

**You are looking for** anything that renders differently from Phase 1 — missing results, empty
facets, a field showing a 32-character hash, an error page, a blank block.

**If something is wrong:** set the phase back to `1` and go to
[Troubleshooting](#troubleshooting-index). Do not try to fix it in place.

> **Common symptoms and what they mean**
>
> | You see | It is |
> |---|---|
> | A field renders as a 32-char hash | Field shadowing on a legacy `$estool.esSearch` template ([R5](#r5-vtl-templates-and-viewtools)) |
> | Facets empty or wrongly named | A legacy `esSearch`/`esRaw` template, or a Site Search index built with a dynamic mapping ([R12](#r12-site-search-rules)) |
> | A block driven by a plugin is blank or errors | A Level-2/3 plugin that was not recompiled ([R7](#r7-osgi-plugins-in-detail)) |
> | Site Search returns far fewer results than expected | The index was crawled against an incomplete content index ([R12](#r12-site-search-rules)) |
> | Results differ but nothing errors | An engine behaviour difference — capture both result sets and raise it |

---

### Step 4.5 — Watch for read fallbacks

**Why:** in Phase 2, a failed OpenSearch read is caught, logged at `ERROR`, and **retried against
Elasticsearch**. The customer gets a correct answer and notices nothing. You are the only one who
sees the problem — and this signal disappears entirely in Phase 3.

**Do this:** watch the logs for read-failure/fallback `ERROR` lines.

**Every one of these is a reason not to advance to Stage 5.** Investigate before cutting over.

---

### Step 4.6 — Soak

**Why:** the point of this soak is production **read** load — real queries, real concurrency, real
aggregations. Phase 1's soak proved the write path; this one proves the read path.

**How long:** at least as long as the Stage 3 soak, and long enough to include the customer's peak
traffic pattern and any scheduled jobs.

---

### ▸ Gate 4 — do not continue until

- [ ] Every node logged `PHASE_2_DUAL_WRITE_OS_READS is active`.
- [ ] Every node reports phase `2`.
- [ ] You walked the customer's site and found no differences from Phase 1.
- [ ] No read-fallback `ERROR`s appeared during the soak.
- [ ] The readiness report still shows `IN_SYNC` and `outOfSyncCount: 0`.
- [ ] The customer has signed off on Phase 2 behaviour.

---

# Stage 5 — Cut over (Phase 2 → 3)

**Goal:** Elasticsearch stops being written and stops being a fallback. OpenSearch is the only engine.
**Elapsed:** one working day.
**Risk to the customer:** **after this, there is no fallback.** An OpenSearch failure surfaces to the
caller instead of being absorbed. Coming back is no longer free.

---

### Step 5.1 — Final readiness check

**Do this:**

```bash
curl -su admin:<pass> https://<host>/api/v1/index/migration/readiness | jq
```

**Everything must be green:**

- [ ] `verdict.safeToAdvance` is `true`
- [ ] `verdict.outOfSyncCount` is `0`
- [ ] `content.WORKING.osIndexedPercent` and `content.LIVE.osIndexedPercent` are `100.0`
- [ ] **every** `siteSearch[]` row is `IN_SYNC` — no `MISSING_COUNTERPART` anywhere
- [ ] no `docCount` is `-1`

> **Site Search matters more here than anywhere else.** In Phase 3 there is no fallback: a Site Search
> index whose OpenSearch copy was never built is simply unsearchable, and the only fix is a re-crawl.

---

### Step 5.2 — Confirm the OpenSearch cluster is production-ready

**Why:** you are about to make it the only engine. Its availability is now the customer's search
availability.

**Confirm:**

- [ ] Replicas are configured (`OS_INDEX_REPLICAS`), not left to chance.
- [ ] Cluster health is green.
- [ ] Monitoring and alerting cover it the way they covered Elasticsearch.
- [ ] Backups/snapshots are in place per the customer's policy.

---

### Step 5.3 — Set the phase to 3

**Do this** using the same mechanism as before, on every node.

---

### Step 5.4 — Restart, one node at a time

**Why:** Phase 3 startup validation is **fail-loud**. There is no Elasticsearch to fall back to, so
dotCMS refuses to start rather than serve a potentially stale index.

**Do this:** the same drain → restart → confirm → return sequence.

**If a node refuses to start** with:

```
OpenSearch startup validation failed in PHASE_3_OPENSEARCH_ONLY. Cannot auto-rollback to ES
(ES may be decommissioned or stale). Restore OS connectivity or manually reset
FEATURE_FLAG_OPEN_SEARCH_PHASE, then restart dotCMS.
```

**stop the rollout.** It is telling you OpenSearch is not reachable. Fix that first; do not restart
the remaining nodes.

---

### Step 5.5 — Confirm the cutover

**Do this:**

1. Walk the customer's site again — the same list as Step 4.4.
2. Publish a piece of content and confirm it becomes searchable.
3. Run one Site Search crawl and confirm the result count is what you expect.
4. Call the readiness report one more time.

> **In Phase 3, do not read `safeToAdvance`.** It is forced `true` because there is no phase beyond 3.
> Read the per-index rows and `osIndexedPercent` instead — those still mean something.
> ([R9](#r9-the-readiness-report-field-by-field))

**You should also see** the `.os` indices become visible in the index portlet and in
`/api/v1/esindex`. That is expected — the visibility rule flips at Phase 3.

---

### Step 5.6 — Cooling-off, then decommission

**Why:** Elasticsearch is stale from the moment you entered Phase 3, but it is still the fastest route
back if you need one.

**Do this:**

1. **Leave Elasticsearch running and untouched** for an agreed cooling-off period.
2. Once the period ends and the customer signs off, decommission it.
3. `ES_ENDPOINTS` is no longer required — the endpoint-separation check is skipped in Phase 3
   precisely because Elasticsearch is gone.

---

### ▸ Done

- [ ] Every node is in phase `3` and started cleanly.
- [ ] The site behaves correctly, publishing works, Site Search works.
- [ ] The readiness report shows `osIndexedPercent: 100.0` on both content rows.
- [ ] Elasticsearch is still up, untouched, for the cooling-off period.
- [ ] The customer has signed off.

---

# Fast path — small customers, one window

**Use this only if** [Choose your path](#6-choose-your-path) sent you here: the reindex fits comfortably
in one maintenance window, degraded search during that window is acceptable, and the Stage 0 audits
came back clean.

**What happens.** When dotCMS boots into Phase 3 with no OpenSearch indices, it:

1. Runs the startup validation — **fail-loud**, so it refuses to start if OpenSearch is unreachable.
2. Creates the OpenSearch working/live pair, deriving the names from the existing Elasticsearch ones.
3. Sees they are empty and **automatically queues a full reindex** in the background.
4. Stops writing to Elasticsearch entirely.

So it bootstraps and backfills itself. You do not trigger the reindex manually.

> ### The cost, stated plainly
>
> **Between step 2 and the end of step 3, OpenSearch is the read engine and it is empty. There is no
> fallback in Phase 3. Search returns nothing until the reindex completes.**
>
> For the duration: content searches return empty or partial; any page built on a content pull renders
> empty or partial; Site Search is stale and **must not be crawled** until the reindex finishes.
>
> This is a real, customer-visible search outage for the length of the reindex. That is the entire
> reason the fast path is limited to customers whose reindex is short.

---

### The procedure

1. **Complete [Stage 0](#stage-0--assess-the-customer) and [Stage 1](#stage-1--prepare-the-opensearch-server) in full.** No shortcuts — Gate 1's
   permission checks matter more here, because a 403 in Phase 3 stops dotCMS from starting.
2. **Complete [Stage 2](#stage-2--rehearse-on-a-clone).** On the clone, take the fast path itself and
   **time the outage window**, not just the reindex.
3. **Fix every template and plugin from Stage 0 first.** In the fast path there is no Phase 1 to hide
   behind — they break the moment you come up.
4. **Set up the readiness role** ([Step 3.1](#step-31--give-yourself-access-to-the-readiness-endpoint))
   and **take the baseline** ([Step 3.2](#step-32--take-the-baseline)).
5. **Announce the window.** Search will be degraded for the measured duration.
6. **Set the phase to `3`** on every node ([Step 3.3](#step-33--set-the-phase-to-1) for the mechanism).
7. **Restart.** In a single-node instance, just restart. In a cluster, roll it — and stop if the first
   node refuses to start.
8. **Confirm the startup log** shows `PHASE_3_OPENSEARCH_ONLY` and `OS version check passed`.
9. **Watch the reindex:**
   ```bash
   curl -su admin:<pass> https://<host>/api/v1/esindex/reindex
   ```
   Search stays degraded until this finishes. Do not crawl Site Search yet.
10. **When the reindex completes**, confirm with the readiness report that
    `content.WORKING.osIndexedPercent` and `content.LIVE.osIndexedPercent` are `100.0`.
11. **Now crawl every Site Search index** — full crawls, not incremental. Not before: a crawl builds
    its corpus by querying the content index, so crawling during the reindex produces a permanently
    truncated index ([R12](#r12-site-search-rules)).
12. **Walk the customer's site** — the checklist in [Step 4.4](#step-44--walk-the-customers-site).
13. **Leave Elasticsearch running** for the cooling-off period.

---

### The fast path's rollback is different — plan it before you start

Elasticsearch stopped receiving writes the moment you entered Phase 3, so it is a point-in-time
snapshot that gets staler by the minute. Anything published after the cutover exists only in
OpenSearch and in the database.

**Nothing is lost** — the database is always authoritative — but the recovery costs a second window:

1. Set the phase back to `0`.
2. Restart.
3. **Immediately run a full reindex**, to rebuild Elasticsearch.

> **Budget for that second window before you start.** If you cannot afford two windows, take the
> standard path instead — Phases 1 and 2 both keep Elasticsearch current, which is exactly what makes
> their rollback instant.

---

# Part 2 — When something goes wrong

---

## Go back from where you are

**Downgrading is instant and needs no restart.** Routing reads the phase on every call, so setting the
value back takes effect on the next request. This is your primary safety lever — use it without
hesitation.

### Find where you actually are, first

```bash
curl -su admin:<pass> https://<host>/api/v1/index/migration/readiness | jq .phase
```

Do this **per node** on a cluster, bypassing the load balancer. Do not trust the configured value: a
failed startup validation may have reset it to 0 in memory ([R14](#r14-log-lines-worth-recognizing)).

### Then pick the move

| You are in | Go to | Cost | When |
|:---:|:---:|---|---|
| **2** | **1** | **Free.** Reads revert to Elasticsearch, which is fully current. Dual-write continues. | Anything wrong after Stage 4: a plugin, a template, a query returning the wrong set, unexplained errors. **This should be your reflex.** |
| **1** | **0** | Cheap. OpenSearch stops receiving writes and begins to drift. | The OpenSearch cluster itself is the problem — unstable, slow, misconfigured. |
| **3** | **2** | Moderate. Dual-write resumes, so Elasticsearch starts receiving changes again; reads stay on OpenSearch, which is complete. **Follow with a full reindex** to backfill what Elasticsearch missed. | Phase 3 is genuinely failing. |
| **3** | **0** | **Expensive.** Leaves the customer reading a stale Elasticsearch index. | Emergency only — and even then, prefer 3 → 2. |

> **Coming back from Phase 3, go to 2 — not 0.** Phase 2 resumes dual-write immediately while reads
> stay on the complete engine. Going straight to 0 points the customer at an Elasticsearch index that
> has received nothing since the cutover, which looks like content loss even though nothing was lost.
> Sequence: **3 → 2 → full reindex → 1**.

### Before you downgrade, glance at `safeToRollback`

```bash
curl -su admin:<pass> https://<host>/api/v1/index/migration/readiness | jq .verdict
```

`safeToRollback: false` means **OpenSearch holds documents Elasticsearch does not**. Downgrading makes
that content unsearchable until a reindex.

That is not a reason never to downgrade — if the site is broken, getting it working matters more. It
tells you the downgrade comes with a follow-up task, and what that task is.

> An **unmeasurable** count (`docCount: -1`) also makes it unsafe, deliberately: it is never compared
> numerically, because treating `-1` as a number would read as a green while OpenSearch may hold more.

### Three situations that need extra care

**Rolling back the dotCMS build during dual-write.** If dotCMS itself is rolled back to an older build
while in Phase 1 or 2, the old build stops writing to OpenSearch — but OpenSearch keeps everything the
newer build already pushed, so it silently **drifts ahead**. Harmless for Elasticsearch, invisible in
Phase 0, critical if you later re-advance to Phase 2 or 3 without resyncing.
→ **Run a full reindex before re-activating the migration.**

**Downgrading during an in-flight reindex.** Rolling the phase back to 0 while a reindex is draining
leaves the OpenSearch reindex pair half-populated. dotCMS handles it — it treats the state as an
OpenSearch reindex abort, keeps the active OpenSearch rows, clears the reindex slots and deletes the
partial `.os` pair so a later boot cannot adopt it, logging a `WARN` with the deleted names. But the
OpenSearch pair that survives is the **old** one and it now drifts.
→ **Prefer letting a reindex finish, or abort it explicitly, over flipping the phase mid-drain. Run a
full reindex before re-advancing.**

**Activating a pre-migration backup index.** dotCMS lets an administrator activate an old inactive
index (Maintenance → Index → *Make Default*) to roll back to a previous reindex. **If that index
predates the migration it has no OpenSearch counterpart, and activating it does not create one.**

| Phase | What you see |
|:---:|---|
| 1 | Nothing. Writes to the OpenSearch counterpart go nowhere. |
| 2 | Still works — the read fallback drops back to Elasticsearch — but logs an `ERROR` per read. **That is your signal.** |
| 3 | No fallback. Empty results or an exception, which reads to the customer as **lost content**. |

You **cannot pre-check** a backup: the readiness report covers only the *active* pair, so a divergent
backup is invisible while inactive.
→ **Sequence: activate → call readiness → if it reports `MISSING_COUNTERPART`, run a full reindex →
only then touch the phase.**

---

## Emergency stop

The customer's search is broken and you need it working now.

**1. Find the phase each node is actually in.**

```bash
curl -su admin:<pass> https://<node-1>/api/v1/index/migration/readiness | jq .phase
```

**2. Downgrade one step**, per the table above. **No restart is needed.** Push the change to every
node at once — this is the one situation where you do *not* want a slow rolling change.

**3. If you set the phase through the system table, clear it there:**

```bash
curl -su admin:<pass> -X DELETE \
  https://<host>/api/v1/system-table/FEATURE_FLAG_OPEN_SEARCH_PHASE
```

Otherwise the system-table value keeps winning over the environment variable and your change does
nothing.

**4. Verify.** Re-read `phase.current` from each node and confirm the customer's search works.

**5. Only then diagnose.** Capture the log lines, the readiness report and the failing query **before**
anything is restarted — a restart can clear the evidence you need.

---

## Troubleshooting index

| Symptom | Cause | Go to |
|---|---|---|
| Phase set to 1 but no `.os` rows in `indicies` | You did not restart. Rule 2. | [Step 3.4](#step-34--restart-one-node-at-a-time) |
| Restarted, still no `.os` indices; log says `AUTH_FORBIDDEN` | OpenSearch role's index pattern does not match the cluster ID | [Step 1.4](#step-14--run-the-three-permission-checks) |
| Startup validation fails, dotCMS boots on ES only | Unreachable, wrong version, same endpoints — **or** missing `cluster:monitor/main` | [Step 1.3](#step-13--create-the-service-account-and-role), [Step 3.5](#step-35--confirm-the-startup-log) |
| Log says `Authentication : NONE — connecting ANONYMOUSLY` | Credentials did not resolve | [Step 1.5](#step-15--point-dotcms-at-opensearch) |
| A "full reindex" returns 200 but creates no index anywhere | OpenSearch 403 misread as "index does not exist"; the first attempt halts the migration | [Step 1.4](#step-14--run-the-three-permission-checks) |
| dotCMS pauses ~2 minutes at startup | Connection retries against an unreachable OpenSearch. Expected. | [Step 3.5](#step-35--confirm-the-startup-log) |
| dotCMS refuses to start | Phase 3 with OpenSearch unreachable — fail-loud by design | [Step 5.4](#step-54--restart-one-node-at-a-time) |
| `PKIX path building failed` | TLS trust chain, not a migration problem | [Step 1.2](#step-12--settle-transport-security) |
| Search results change between page loads | Cluster nodes in different phases | [R11](#r11-clustered-instances) |
| Search empty right after entering Phase 3 | The automatic reindex has not finished | [Fast path](#fast-path--small-customers-one-window) |
| A field renders as a 32-char hash | Field shadowing on a legacy `$estool.esSearch` template | [R5](#r5-vtl-templates-and-viewtools) |
| Facets empty or wrongly named | Legacy `esSearch`/`esRaw`, or a Site Search index with a dynamic mapping | [R5](#r5-vtl-templates-and-viewtools), [R12](#r12-site-search-rules) |
| A plugin throws `ClassNotFoundException` / `ClassCastException` after Phase 2 | Level-2 or Level-3 Elasticsearch coupling | [R7](#r7-osgi-plugins-in-detail) |
| Site Search returns far fewer results than expected | Crawled against an incomplete content index | [R12](#r12-site-search-rules) |
| Site Search alias column blank in the portlet | The index exists only on the engine the current phase does not read from | [R12](#r12-site-search-rules) |
| `safeToAdvance: false` with no obvious cause | Read `verdict.blockers` — it names the index and the action | [Step 4.1](#step-41--confirm-it-is-safe-to-advance) |
| `safeToAdvance: true` in Phase 3 but something is wrong | It is **forced** `true` there. Read the rows, not the boolean. | [R9](#r9-the-readiness-report-field-by-field) |
| `docCount: -1` | The count could not be measured. **Not zero.** Treated as out of sync on purpose. | [R9](#r9-the-readiness-report-field-by-field) |
| Readiness endpoint returns 403 | Missing the migration role, or not a CMS Admin. Both required. | [Step 3.1](#step-31--give-yourself-access-to-the-readiness-endpoint) |
| `.os` indices not visible in the admin UI | Expected in Phases 0/1/2 — hidden from everyone | [Step 3.6](#step-36--confirm-the-opensearch-indices-were-created) |
| You changed the phase and nothing happened | The system table is set and wins over your change | [R10](#r10-the-phase-setting-and-the-restart-rule) |

---

# Part 3 — Reference

> You do not need to read this section to run a migration. The stages link into it when you want the
> reasoning behind an instruction.

---

## R1. Concepts and vocabulary

| Term | What it means in practice |
|---|---|
| **Source engine / ES** | The engine the customer runs today — Elasticsearch, or an older OpenSearch 1.x. Always the `ES_*` properties, whatever it physically is. |
| **Target engine / OS** | OpenSearch **3.x**. The `OS_*` properties. |
| **Index** | Where the engine stores content. dotCMS keeps two: `working` (everything, including drafts) and `live` (published only). |
| **Index name** | Carries a timestamp and, on OpenSearch, a marker. Physical form: `cluster_08abc3.working_20260406.os`. The `cluster_<id>.` prefix identifies the customer. |
| **The `.os` tag** | The suffix distinguishing an OpenSearch index from its Elasticsearch counterpart. It is part of the real name — you see it in the cluster, in `indicies`, and in the readiness report. **Never type it by hand; copy a real name from a listing.** |
| **Migration phase** | 0 to 3. Which engines receive writes, and which one answers searches. |
| **Dual-write** | Phases 1 and 2: every content change is written to *both* engines. |
| **Shadow index / mirror / counterpart** | The OpenSearch copy while Elasticsearch is still authoritative. It follows Elasticsearch's writes rather than owning them. |
| **Full reindex** | Rebuilding an index from the database. The only operation that makes OpenSearch catch up with content that already existed. |
| **`indicies` table** | The database table recording which indexes are active. OpenSearch rows end in `.os`. |

---

## R2. Why there are four phases

> The narrative version, written for someone meeting this for the first time, is
> [Start here §3](#3-the-four-phases-one-at-a-time). This is the compact statement.

A one-step cutover has no way back. Once you stop writing to Elasticsearch it starts going stale
within seconds — and if OpenSearch then turns out to be misconfigured, slow, or missing content, your
rollback target is already out of date.

The four phases exist so that at every step there is **an engine you can still fall back to**, and so
that each risky change is made **one at a time**:

| Phase | Writes | Reads | Elasticsearch | What this step proves |
|:---:|---|---|---|---|
| **0** | ES only | ES | authoritative | Nothing — the starting state. |
| **1** | ES **+** OS | ES | authoritative | dotCMS can *write* to OpenSearch: connectivity, credentials, permissions, mappings. Nothing the customer sees depends on it. |
| **2** | ES **+** OS | **OS** | written; used as fallback | OpenSearch can *serve* the real query load. A failed read is retried against Elasticsearch. |
| **3** | **OS only** | **OS** | decommissioned | The cutover. No fallback remains. |

**Writes move first, then reads, then the old engine is dropped.** Each phase changes exactly one of
those.

**Phase 2 is the load-bearing step.** It is the first time results come from OpenSearch, while
Elasticsearch still holds a complete, current copy. If OpenSearch throws on a read, dotCMS catches it,
logs at `ERROR`, and answers from Elasticsearch. The customer sees a correct result; you see the
error. That early-warning system disappears in Phase 3.

**What the fallback does not cover:** it triggers only on an *exception*. If OpenSearch answers
successfully but with stale or incomplete data — because a write silently failed, or because the
reindex was never run — there is no error to catch and the customer gets the wrong answer with no
trace. That is why the readiness report compares counts rather than waiting for an error.

---

## R3. The mirror strategy and where drift comes from

### What dual-write does

Every content change — publish, save, delete, permission change, content-type change — is written to
both engines immediately after the database transaction commits. Going forward, the two stay in step.

### What it does not do

It does not backfill. The moment you enable Phase 1, OpenSearch has an **empty pair of indices** while
Elasticsearch holds the entire corpus. Dual-write only narrows the gap for content that changes from
that point on. **Only a full reindex closes it** (Rule 1).

### Shadow-write failures are silent on purpose

While Elasticsearch is authoritative (Phases 1 and 2), a failed OpenSearch write is **logged and
ignored**: it does not fail the customer's publish, does not mark the reindex entry failed, and does
not surface to the caller. A problem with the new engine must never break the live site.

> The operational consequence: **an OpenSearch write failure is invisible unless you look for it.**
> The only evidence is a `WARN` (level via `DOTCMS_SHADOW_WRITE_LOG_LEVEL`) and, later, a count
> difference in the readiness report.

In Phase 3 this reverses — OpenSearch is authoritative, so its failures propagate and the customer
*will* see them.

### Where drift comes from

| Source | Phase | How you detect it |
|---|:---:|---|
| The corpus that existed before Phase 1 was enabled | 1, 2 | `osIndexedPercent` far below 100 |
| A shadow write that failed and was swallowed | 1, 2 | `WARN` in the log; later `COUNT_DRIFT` |
| A dotCMS build rollback during dual-write | 1, 2 | OpenSearch ends up *ahead*; `safeToRollback: false` |
| An index activated from a pre-migration backup | 1, 2, 3 | `MISSING_COUNTERPART` on `WORKING`/`LIVE` |
| A Site Search index created before dual-write started | any | `MISSING_COUNTERPART` on a `siteSearch` row |

All of them are reported by the readiness endpoint, and all are fixed the same way: **a full reindex**
for content, **a full crawl** for Site Search.

---

## R4. How one dotCMS drives two engines

### The routing layer

There is exactly **one** place per functional area where "which engine handles this?" is decided. Every
index operation goes through it. It reads the current phase on **every call**, which is why the routing
part of a phase change is instant.

It also knows the two engines hold indices under *different names*, and translates on the way out:
Elasticsearch gets the plain name, OpenSearch gets the `.os` name. You never do that translation
yourself.

### Cutting the cord to the vendor's library

This is what determines which customisations survive.

Historically dotCMS's public search APIs returned **Elasticsearch's own Java classes** —
`org.elasticsearch.action.search.SearchResponse`, `SearchHit`, `SearchHits`, the ES aggregation types.
Anything a customer built on search was therefore compiled against Elasticsearch's SDK.

That is fatal for a migration: those classes do not exist in the OpenSearch client. A plugin holding a
`SearchResponse` cannot be handed an OpenSearch result — there is nothing to hand it.

So the search APIs were rewritten to return **dotCMS's own domain objects** — neutral types that
belong to dotCMS, not to any vendor:

| Old type (Elasticsearch's) | New type (dotCMS's own) |
|---|---|
| `org.elasticsearch.action.search.SearchResponse` | `ContentSearchResponse` |
| `com.dotcms.content.elasticsearch.business.ESSearchResults` | `ContentSearchResults<T>` |
| `org.elasticsearch.search.SearchHits` | `SearchHits` |
| `org.elasticsearch.search.SearchHit` | `SearchHit` |
| `org.elasticsearch.search.TotalHits` | `TotalHits` |

(All the new ones live in `com.dotcms.content.index.domain`.)

Both engines' results are converted into these before they leave the routing layer, so consuming code
works no matter which engine answered.

**The consequence — the main risk of the whole migration:** customer code still written against the
*old* types was written against Elasticsearch specifically, and **breaks when reads move to
OpenSearch**. The old methods still exist, deprecated for removal, so nothing breaks today. They are
the fault line.

---

## R5. VTL templates and viewTools

Templates read the index through **viewTools** — the `$`-prefixed objects available inside `.vtl`. If a
viewTool returns a different shape after reads move to OpenSearch, every page built on it breaks,
usually silently.

### The three viewTools that touch the index

| `$key` | Class | Purpose |
|---|---|---|
| `$dotcontent` | `ContentTool` | The main content-pull tool: `pull`, `pullPerPage`, `query`, `count`, `find`. No aggregation support. |
| `$estool` | `ESContentTool` | Raw index queries — where aggregations live. Four methods; two safe, two not. |
| `$sitesearch` | `SiteSearchWebAPI` | Site Search queries and facets. |

### `$estool` — the method decides whether the template survives

| Method | Returns | Status |
|---|---|---|
| `$estool.search(query)` | `ContentSearchResults` of `ContentMap`; `.aggregations` is a neutral map | ✅ **Safe** |
| `$estool.raw(query)` | `ContentSearchResponse` (neutral) | ✅ **Safe** |
| `$estool.esSearch(query)` | `ESSearchResults`, wrapping raw `Contentlet`s and ES-typed aggregations | ⚠️ **Legacy**, deprecated for removal |
| `$estool.esRaw(query)` | Elasticsearch's own `SearchResponse` | ⚠️ **Legacy**, breaks outright on OpenSearch |

All four take a **raw engine JSON query body**, not a Lucene string. `search` and `raw` lowercase the
whole query, so `contentType` becomes the physical field `contenttype` — convenient, but case-sensitive
exact matching is not available on those two, and aggregation names come back lowercased.

### The three regression classes seen in the field

**1. Aggregations and facets must be equivalent.** (Tickets #37559 / #36026.) An early version
flattened `$results.aggregations` and broke every template walking `.buckets`. The neutral aggregation
type exists so those templates keep working verbatim; it exposes `getBuckets()`, `getHits()`, and per
bucket `getKey()`, `getKeyAsString()`, `getKeyAsNumber()`, `getDocCount()`, `getAggregations()`.
Nested aggregations and `top_hits` sub-aggregations need checking too — `top_hits` comes back under
`.hits`, not `.buckets`.

**2. Field shadowing on raw `Contentlet`s.** (Ticket #37870.) When a template iterates the legacy
`$estool.esSearch(...)` result, each record is a raw `Contentlet`, and Velocity resolves
`$record.<field>` by calling the matching Java getter. If a custom field's variable name collides with
a built-in getter, the getter wins. The known case: a custom field named `contentTypeId` resolves to
`Contentlet.getContentTypeId()` — a 32-character internal hash instead of the field's value.
*Workaround:* `$record.map.contentTypeId`. *Real fix:* move to `$estool.search(...)`, whose records are
`ContentMap` and read fields cleanly.

**3. Templates bound to Elasticsearch SDK types.** Anything calling
`$estool.esSearch(...).getAggregations()` or `$estool.esRaw(...)` receives Elasticsearch classes, which
do not exist on OpenSearch. No workaround — rewrite against `$estool.search(...)` / `$estool.raw(...)`.

---

## R6. Lucene queries and the search endpoints

### Lucene queries are the safe path

dotCMS's own query language (`+contentType:Blog +live:true`) is translated by dotCMS into whatever DSL
the target engine speaks. Customer Lucene queries — content pulls, URL maps, workflow conditions, saved
searches — route through the neutral layer and are **not** engine-bound.

Two caveats:

- **`match_phrase_prefix` behaves differently** between the engines. Any query using it deserves an
  explicit before/after comparison.
- **Relevance scores differ.** Expected, not a bug. What must *not* differ is the **set** of documents
  returned. Compare identifiers, not scores.

### The endpoints

Every search endpoint is phase-aware: the identical request hits Elasticsearch in Phases 0–1 and
OpenSearch in Phases 2–3.

| Endpoint | Query language | Risk |
|---|---|---|
| `POST /api/content/_search` | Lucene | Low — neutral |
| `POST /api/v1/content/_search` | Lucene (identical behaviour) | Low |
| `POST /api/v1/content/search` | Structured form | Low |
| `GET /api/content/indexsearch/{query}/...` | Lucene, identifiers only | Low |
| `POST /api/es/search` | **Raw engine DSL** | Medium |
| `POST /api/es/raw` | **Raw engine DSL** | Medium |
| `POST /api/v1/graphql` | GraphQL | Low |

**The two `/api/es/*` endpoints** accept raw engine DSL, so a customer integration may carry
hand-written Elasticsearch syntax. Their **responses** are deliberately kept in the legacy
Elasticsearch wire format even when OpenSearch served the query, so existing consumers keep parsing
them:

- `/api/es/search` returns `{ "contentlets": [...], "esresponse": { …legacy ES JSON… } }`.
- `/api/es/raw` returns the legacy shape directly: `took`, `hits.total.{value,relation}`, `hits.hits[]`
  with `_id` / `_index` / `_score` / `_source`, and `aggregations`.
- Aggregation keys use the ES typed-key form, e.g. `sterms#content_types`.
- `_score` is `null` for non-scored queries (field-sorted, filter-only, aggregation-only). This was a
  real 500 once (#36478 / #36398) — a 500 from a search response is worth raising.
- Neither shape emits a per-hit `sort` array. An integration relying on it (geo-distance sorting is the
  usual case) will not find it.

---

## R7. OSGi plugins in detail

dotCMS's OSGi container exports the packages on the application classpath, so a plugin can
`Import-Package: org.elasticsearch.search` and it resolves — today. Plugins written any time in the
last decade could and did, because dotCMS's own search API handed them Elasticsearch objects ([R4](#r4-how-one-dotcms-drives-two-engines)).

| Level | What the plugin does | When it breaks |
|:---:|---|---|
| **1** | Calls `contentletAPI.search(...)` / `searchRaw(...)`, or `$dotcontent` | Never. Already neutral. |
| **2** | Calls the deprecated `contentletAPI.esSearch(...)` / `esSearchRaw(...)`, or implements the deprecated `esSearch` / `esSearchRaw` hook methods | **At Phase 2.** Also at compile time once the deprecated methods are removed. |
| **3** | Imports `org.elasticsearch.*` directly and holds them in fields, casts, or signatures | **At Phase 2**, and permanently once Elasticsearch leaves the classpath — the bundle will not resolve. |

### The fix — the mapping is mechanical

| Deprecated | Replacement | New return type |
|---|---|---|
| `contentletAPI.esSearch(q, live, user, roles)` | `contentletAPI.search(q, live, user, roles)` | `ContentSearchResults<Contentlet>` |
| `contentletAPI.esSearchRaw(q, live, user, roles)` | `contentletAPI.searchRaw(q, live, user, roles)` | `ContentSearchResponse` |
| `ContentletAPIPreHook.esSearch(...)` | `ContentletAPIPreHook.search(...)` | `boolean` |
| `ContentletAPIPreHook.esSearchRaw(...)` | `ContentletAPIPreHook.searchRaw(...)` | `boolean` |
| `ContentletAPIPostHook.esSearch(...)` | `ContentletAPIPostHook.search(...)` | `void` |
| `ContentletAPIPostHook.esSearchRaw(...)` | `ContentletAPIPostHook.searchRaw(...)` | `void` |

The hook replacements have default no-op implementations, so a plugin only overrides what it actually
intercepts. Full before/after examples: [`SEARCH_API_MIGRATION.md`](SEARCH_API_MIGRATION.md).

Two extra notes for the developer:

- `ContentSearchResults<T>` is a **typed** `List<T>` — the old `(Contentlet)` casts go away.
- `ContentSearchResponse.toString()` is **not JSON**. Code that called `.toString()` on the old raw
  response to get ES wire-format JSON must switch to the structured accessors: `hits()`,
  `hits().hits()`, `hits().totalHits().value()`, `aggregations()`, `scrollId()`, `tookMillis()`.

### Timing

A Level-2 or Level-3 plugin **does not fail in Phase 1** — Phase 1 still reads from Elasticsearch, so
the plugin gets exactly what it always got. It fails when you enter **Phase 2**.

That is useful: you can enable dual-write, prove the write path, and buy the customer time to
recompile — all without exposing them to the plugin risk. But it also means **a clean Phase 1 tells
you nothing about plugin readiness.**

---

## R8. OpenSearch security in detail

### Non-negotiable requirements

| Requirement | Why | If you get it wrong |
|---|---|---|
| **OpenSearch 3.x** | dotCMS asserts the major version at startup | Validation fails; migration halts to Phase 0 |
| **A separate instance from Elasticsearch** | dotCMS compares the two endpoint sets | Validation fails with an explicit "same endpoint(s)" error |
| **Reachable from every dotCMS node** | Each node connects independently | That node halts its own migration and silently serves Elasticsearch-only |
| **`number_of_replicas` set explicitly** | OpenSearch does not inherit dotCMS's implicit default | Yellow cluster, or unexpected replica behaviour |

The endpoint-separation check is **best-effort on strings**: `127.0.0.1:9200` and `localhost:9200` are
the same server but will not be detected as overlapping. Verify by hand.

### Transport security

| Property | Recommendation |
|---|---|
| `OS_ENDPOINTS` | `https://` in production. TLS activates from the scheme; `OS_TLS_ENABLED` is not also needed. |
| `OS_TLS_CERT_REQUIRED` | `false` by default — verification is **skipped**. Set `true` in production. |
| `OS_TLS_CA_CERT` | PEM CA path for a private CA / internal PKI. |
| `OS_TLS_TRUST_SELF_SIGNED` | Dev and lab only. |

### Authentication

| Property | Notes |
|---|---|
| `OS_AUTH_TYPE` | `BASIC` (default), `JWT`, or `CERT`. |
| `OS_AUTH_BASIC_USER` / `OS_AUTH_BASIC_PASSWORD` | Required for `BASIC`. |
| `OS_AUTH_JWT_TOKEN` | Sent as `Authorization: Bearer …` on every request. |
| `CERT` (mTLS) | Partially implemented — currently falls back to the trust-self-signed strategy. Do not plan a production migration around it. |

> **Every `OS_*` key falls back to its `ES_*` equivalent when unset.** Convenient, and a trap: an
> instance that never configured OpenSearch credentials silently reuses the Elasticsearch ones rather
> than failing.
>
> **Anonymous connection is not an error.** `NONE — connecting ANONYMOUSLY` in the startup banner means
> no credentials resolved. dotCMS still connects if the cluster does not enforce security. On
> production, that line is a problem.

### The service account

The permission set is in [Step 1.3](#step-13--create-the-service-account-and-role). Spike #35922
confirmed `cluster:monitor/main` is the **only** gap between the historic dotCMS role and OpenSearch
3.x — `indices_all` on the customer pattern already expands to the full action set needed, on both OS
1.3 and OS 3.x.

### The index-pattern trap

The role grants `indices_all` on `cluster_<customer>*`, and dotCMS builds physical index names from
`DOT_DOTCMS_CLUSTER_ID`. **If the cluster ID does not match the pattern, dotCMS connects and reads the
version fine, but every index-create is rejected with 403.** The startup gate cannot see it — it probes
`GET /`, which is cluster-scoped and succeeds.

`DOT_DOTCMS_CLUSTER_ID` is **immutable after first boot** — written to `dot_cluster` and read from
there forever. Changing the environment variable on an instance with data does nothing. The fix is
always on the OpenSearch side.

### OpenSearch 3.8 note

OpenSearch 3.8 creates system indices older 3.x releases did not (`.plugins-ml-config`, `top_queries-*`
from query insights). Any script that diffs the *set* of indices between engines must exclude system
indices. The readiness endpoint only looks at dotCMS's own.

---

## R9. The readiness report, field by field

```
GET /api/v1/index/migration/readiness
```

A **read-only** report. It never changes anything, never repairs anything, and never blocks anything.
It answers, from live data, whether the two engines are actually in sync.

### Access

Both conditions, or **403**:

1. The caller is a **CMS Administrator**, and
2. holds the **migration support role**, identified by its **Key** — configured in
   `OS_MIGRATION_INDEX_VISIBILITY_ROLE_KEY`, default **`os_migration_qa`**.

The endpoint is hidden: absent from `openapi.yaml` and from the API playground. You reach it by knowing
the URL. This is deliberate — a regular user should never discover that a migration is running.

> **Related but separate:** the `.os` indices are hidden from the index portlet and `/api/v1/esindex`
> in Phases 0/1/2 **for everyone**, and shown in Phase 3. That is purely phase-based and consults no
> role. The role key above gates the readiness endpoint only.

### Reading it, top-down

1. **`phase`** — `current`, `name`, `readEngine`, `writeEngines`, `dualWrite`. Read it first: it tells
   you what the instance is *actually* doing, which may not be what you configured.
2. **`verdict.safeToAdvance` / `verdict.safeToRollback`** — the go/no-go pair. Different questions, not
   opposites; both can be `false`.

   | | Blocked when | Because |
   |---|---|---|
   | `safeToAdvance` | the OpenSearch copy is **behind** | promoting would lose that delta |
   | `safeToRollback` | the OpenSearch copy is **ahead** | downgrading would hide that delta until a reindex |

3. **`verdict.summary` + `verdict.blockers`** — a sentence for the ticket, then the per-index list of
   what to fix. If `safeToAdvance` is `false`, `blockers` is never empty.
4. **`content` (keyed `WORKING` / `LIVE`) and `siteSearch` (a list)** — the evidence.

### A per-index row

Each row carries `es` and `os`, each `{exists, docCount, physicalName}` — plus `alias` on Site Search
rows. Then:

| Field | How to read it |
|---|---|
| `verdict` | `IN_SYNC` · `MISSING_COUNTERPART` (one engine lacks it) · `COUNT_DRIFT` (both have it, different counts) |
| `driftPercent` | `(OS − ES) / ES × 100`. `0.0` in sync · negative = **behind** (blocks advance) · positive = **ahead** (blocks rollback) · `-100.0` mirror empty or absent · `null` a count could not be measured |
| `databaseDocCount` | Content rows only. What the **database** says the index should hold. |
| `esIndexedPercent` / `osIndexedPercent` | Content rows only. Each engine measured **against the database**. `100.0` = complete; `3.06` = never rebuilt. |
| `docCount: -1` | The count **could not be measured**. Never read it as zero. |
| `physicalName` | The exact name on that server. Copy/paste into `_cat/indices` to verify by hand. |
| `recommendation` | The concrete action. A trailing `NOTE:` flags a damaged alias ([R12](#r12-site-search-rules)). |

### The field that still works in Phase 3

`driftPercent` compares the engines against each other — which stops being an answer once one of them
is the only one left. `osIndexedPercent` compares OpenSearch against the **database**, so it still
means something in Phase 3, where a mirror that was never rebuilt would otherwise read unremarkably.

> **In Phase 3, `safeToAdvance` is forced `true`** (there is no phase 4). Never read the boolean alone
> there — read the per-index rows and `outOfSyncCount`.

### Two things a count cannot tell you

1. **A count that does not move is not proof nothing was written.** The document id is
   `identifier_languageId_variant`, so re-publishing content already in the index is an *update*.
2. **In a dual-write phase the OpenSearch copy only receives what changes from that point on.** A
   mirror at 15 of 683 documents is the expected state until the reindex.

To settle one specific write, ask for the **document** — see
[Step 3.7](#step-37--confirm-one-write-reaches-both-engines).

### Two traps in the report itself

- **It covers only the *active* working/live pair.** An inactive backup index is invisible to it.
- **It reports; it never repairs and never blocks.** Re-running it changes nothing.

---

## R10. The phase setting and the restart rule

### Where the phase lives

`FEATURE_FLAG_OPEN_SEARCH_PHASE` (environment: `DOT_FEATURE_FLAG_OPEN_SEARCH_PHASE`), values `0`–`3`.
An absent or unrecognised value means Phase 0.

| Where | Reach | Survives restart | Notes |
|---|---|:---:|---|
| Environment variable / `dotmarketing-config.properties` | **One node** | ✅ | You must set it on every node yourself. |
| The dotCMS **system table** (`POST /api/v1/system-table`, CMS Admin) | **The whole cluster** | ✅ | Stored in the database and shared. **Wins over the environment variable.** |
| Runtime reset by dotCMS itself | One node, in memory | ❌ | What happens on a failed startup validation. |

> **The system table takes precedence.** If the phase was ever set there, that value wins — and
> dotCMS's own emergency reset to Phase 0 becomes a no-op, because the reset writes to the in-memory
> store while the system-table value keeps winning. **Clearing it is part of your rollback
> procedure.** Pick one mechanism per customer and stick to it.

### The restart rule

> **Routing is live. Setup is not.**

The router reads the phase on every call, so which engine gets writes and reads changes instantly. But
three things only happen at **startup**:

1. The OpenSearch connectivity / version / endpoint-separation validation.
2. The automatic migration halt and fallback to Phase 0 if that validation fails.
3. The creation of the OpenSearch index pair and its `indicies` rows.

| Transition | Restart? | Why |
|---|:---:|---|
| **0 → 1** | ✅ **Mandatory** | The OpenSearch indices do not exist yet; they are created at boot. Flip the flag live and dual-writes fan out to an index that was never created — **silently swallowed** — while validation never runs. |
| **1 → 2** | ✅ **Strongly recommended** | Only a restart re-runs the startup validation for the phase you are now in. |
| **2 → 3** | ✅ **Strongly recommended** | Same, and more so: Phase 3 validation is fail-loud. Discover a connectivity problem at a controlled restart, not on the first customer search. |
| **Any downgrade** | ⛔ **Not required** | Routing reverts immediately. This is what makes downgrade the emergency lever. |
| **After an automatic halt** | ✅ **Mandatory** | The reset to Phase 0 is in memory only. |

### What happens when dotCMS cannot reach OpenSearch

**At startup, Phases 1 and 2 — it degrades safely.** Validation failure logs the reason, resets the
phase to 0 in memory, and dotCMS boots normally on Elasticsearch alone. No crash, no hang, no data
loss. See [R14](#r14-log-lines-worth-recognizing) for the exact lines.

**At startup, Phase 3 — it fails loud and refuses to serve.** There is no Elasticsearch to fall back
to, and silently rolling back to Phase 0 would point the customer at a potentially stale index.

**At runtime:**

| Phase | OpenSearch write fails | OpenSearch read fails |
|:---:|---|---|
| 1 | Logged (`WARN` by default), ignored. Customer unaffected. | n/a — reads come from Elasticsearch |
| 2 | Logged, ignored. Customer unaffected. | Caught, logged at `ERROR`, **retried against Elasticsearch**. |
| 3 | **Propagates to the caller.** | **Propagates to the caller.** |

**Startup delay:** dotCMS retries before giving up — 24 attempts, 5 seconds apart by default
(`OS_CONNECTION_ATTEMPTS`, `OS_CONNECTION_RETRY_SLEEP_SECONDS`). A boot against a dead OpenSearch
pauses roughly two minutes. Expected.

**The blind spot:** the startup gate probes the cluster root only. It cannot see permission failures
scoped to *index names* — which is why [Step 1.4](#step-14--run-the-three-permission-checks) exists.

---

## R11. Clustered instances

### Every node must agree on the phase

The phase is read per node. If node A is in Phase 2 and node B is in Phase 1:

- Both still dual-write, so **no data is lost**.
- But **search results depend on which node served the request** — node A answers from OpenSearch,
  node B from Elasticsearch. If the engines have drifted at all, the customer sees results that change
  between page loads with no pattern.

The mismatch is worse in the Phase-3 direction: a node in Phase 3 stops writing to Elasticsearch
entirely, so a mixed 2/3 cluster leaves Elasticsearch receiving a *subset* of writes — and your
rollback target is quietly corrupt.

> **Never let a cluster sit in mixed phases for longer than the rolling restart takes.**

Verify per node afterwards, reading `phase.current` from each node directly, bypassing the load
balancer.

### Traffic during the change

For upgrades, each node goes down and comes back. Handle it as any rolling restart, with one addition:
**drain the node before restarting it.** A node mid-boot in Phase 1 or 2 may be halfway through
creating OpenSearch indices, and requests it serves during that window can behave inconsistently.

Restart one node, let it come fully up, check its log, return it to the pool, then move on. **If the
first node halts its migration, stop the rollout** — the rest will do the same.

For a **downgrade**, none of this applies: no restart is needed and you want it everywhere as fast as
possible. Push to all nodes at once.

### Reindex during a rolling restart

A full reindex is driven by a database queue, and workers on any node pick entries up. A rolling
restart mid-reindex will not lose entries, but it slows it down and makes completion harder to read.
**Prefer letting a reindex finish before a rolling restart** — or abort it explicitly and restart it
afterwards.

---

## R12. Site Search rules

A Site Search index is not built by publishing. It is built by a **crawl** — a scheduled job, or "Run
Now" from the portlet. Nothing about changing the phase causes a crawl to happen.

**1. A phase change never builds Site Search counterparts retroactively.** An index created in Phase 0
lives only on Elasticsearch. Moving to Phase 1 does *not* create its OpenSearch copy. The copy appears
only when a **full crawl runs while in a dual-write phase (1 or 2)**.

**2. Before advancing a phase, let every crawl run at least once in the current phase.** The rule is
*"transition when every crawl has run at least once in the current phase"* — not merely *"the flag is
set"*.

**3. Run a full, not incremental, crawl per index after moving to Phase 1.** A full crawl builds the
OpenSearch copy with the correct field mapping. An incremental crawl writes documents in place and, if
the copy is missing, lets OpenSearch **auto-create** it with a *dynamic* mapping — which silently
breaks term aggregations and facets. dotCMS now gates this (an incremental checks existence *and*
document-count parity across the write engines and demotes itself to a full rebuild on a mismatch), but
do not rely on the gate when you can just run the full crawl.

**4. Reindex the content first, crawl second — always, in Phases 2 and 3.**

> This has caused the worst observed damage, so it gets its own explanation.
>
> A Site Search crawl **does not read the database.** It builds its bundle from a **search over the
> content index** — which in Phases 2/3 means OpenSearch. If the OpenSearch content index has not been
> rebuilt, the crawl cannot see the content missing from it.
>
> Observed in the field: the content index held 685 live documents on Elasticsearch and 21 on
> OpenSearch (never reindexed). A Phase-3 crawl produced a Site Search index with **14 documents**
> instead of ~443. The crawl answered its query correctly; the corpus it queried was 3% complete.
>
> Three things make this especially nasty:
> - **The crawl reports success.** The counts it logs are of what it bundled, so they look internally
>   consistent.
> - **The bundlers swallow search failures at debug level.** Even a hard error just produces a smaller
>   bundle.
> - **Reindexing afterwards does not repair it.** The Site Search index keeps its 14 documents until it
>   is crawled again. And the readiness report will not flag it — the index exists on OpenSearch, which
>   in Phase 3 is the expected shape. The defect is *inside* the index, not in its shape.

**There is an opt-in warning for exactly this.** Set `SITE_SEARCH_CRAWL_MIN_CONTENT_INDEXED_PERCENT` to
the percentage below which you want to be warned (`95` is the intended value; `0`, the default, means
off). The crawl then logs:

```
Site Search crawl starting against an INCOMPLETE content index: 'working_20260811191012' on
OpenSearch holds 3.06% of the 686 contentlets the database has. […] Run a full reindex first.
```

Advisory only — it never stops the crawl. It costs one sequential scan of `contentlet_version_info`
plus six engine round-trips **per crawl** while enabled (~15–22 ms in practice): negligible against a
crawl, but a recurring cost for a message only someone migrating wants. **Turn it on while you are
migrating, read it, turn it off afterwards.**

### Aliases

A Site Search index is known by its alias, never by its `sitesearch_<timestamp>_<uuid>` name. The
readiness report carries the alias **per engine**, because an index can hold its alias on one side and
not the other — created before dual-write started, counterpart built later. That asymmetry is what you
need to see.

A `recommendation` ending in a `NOTE:` saying the alias looks like an index name is the fingerprint of
a crawl-overwrite bug fixed in issue #36983. The fix stops new occurrences but cannot restore an alias
already lost — the report is the only way to find the indices that still need theirs restored by hand.

---

## R13. Configuration reference

Only the settings a migration touches. Full connection reference:
[`OPENSEARCH_CLIENT_CONFIGURATION.md`](OPENSEARCH_CLIENT_CONFIGURATION.md). Every property has a
`DOT_`-prefixed environment-variable form.

### Migration control

| Property | Default | What it does |
|---|---|---|
| `FEATURE_FLAG_OPEN_SEARCH_PHASE` | `0` | The phase, `0`–`3`. Unrecognised values mean Phase 0. |
| `OS_MIGRATION_INDEX_VISIBILITY_ROLE_KEY` | `os_migration_qa` | Role **key** required — in addition to CMS Admin — to read the readiness endpoint. |
| `DOTCMS_SHADOW_WRITE_LOG_LEVEL` | `WARN` | Log level for fire-and-forget OpenSearch write failures in Phases 1/2. |
| `SITE_SEARCH_CRAWL_MIN_CONTENT_INDEXED_PERCENT` | `0` (off) | Warn when a crawl is about to read a materially incomplete content index. `95` is the intended value. |

### Source engine

| Property | Notes |
|---|---|
| `ES_ENDPOINTS` | Comma-separated URLs. |
| `ES_AUTH_TYPE` | `BASIC` \| `JWT` \| `CERT`. |
| `ES_AUTH_BASIC_USER` / `ES_AUTH_BASIC_PASSWORD` | |

### Target engine

| Property | Default | Notes |
|---|---|---|
| `OS_ENDPOINTS` | derived from `OS_HOSTNAME`/`OS_PROTOCOL`/`OS_PORT` | **Must be a separate instance from `ES_ENDPOINTS`.** Preferred for multi-node. |
| `OS_AUTH_TYPE` | `BASIC` | Falls back to `ES_AUTH_TYPE`. |
| `OS_AUTH_BASIC_USER` / `OS_AUTH_BASIC_PASSWORD` | — | Fall back to the `ES_*` equivalents. **Set explicitly.** |
| `OS_TLS_CERT_REQUIRED` | `false` | Set `true` in production, with a trusted CA. |
| `OS_TLS_CA_CERT` | — | PEM CA path for a private CA. |
| `OS_TLS_TRUST_SELF_SIGNED` | `false` | Dev/lab only. |
| `OS_CONNECTION_ATTEMPTS` | `24` | Startup connection retries. |
| `OS_CONNECTION_RETRY_SLEEP_SECONDS` | `5` | 24 × 5s ≈ 2 min startup pause when OpenSearch is down. |
| `OS_INDEX_REPLICAS` | — | Fixed replica count; overrides auto-expand. **Set it.** |
| `OS_INDEX_AUTO_EXPAND_REPLICAS` | `0-1` | |
| `opensearch.index.number_of_shards` | `1` | dotCMS recommends `1` unless distributing across disks. |

---

## R14. Log lines worth recognizing

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

### Automatic migration halt (Phases 1/2)

```
ERROR  OpenSearch startup validation FAILED — halting OS migration; dotCMS falls back to
       ES-only (PHASE_0_MIGRATION_NOT_STARTED): <reason>
ERROR  OpenSearch migration halted: invalid configuration detected at startup. Verify
       OS_ENDPOINTS, OS version, and FEATURE_FLAG_OPEN_SEARCH_PHASE, then restart dotCMS.
WARN   Migration phase reset to PHASE_0_MIGRATION_NOT_STARTED (was PHASE_1_DUAL_WRITE_ES_READS).
       This change is runtime-only …
```

Two `ERROR`s and one `WARN`, never a `FATAL` on this path. `<reason>` is one of: *cluster not
reachable*, *version mismatch*, or *ES and OS point to the same endpoint(s)*.

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

The index-pattern trap ([R8](#r8-opensearch-security-in-detail)). Widen the OpenSearch role's pattern.

### A Phase-2 read falling back

```
ERROR  <OpenSearch read failure> … retrying against Elasticsearch
```

The customer got a correct result. You did not. Investigate before Stage 5.

---

## R15. Command reference

Index names contain timestamps and differ on every instance. **Never guess a name — list it.**

### What dotCMS thinks exists

```bash
curl -su admin:<pass> https://<host>/api/v1/esindex/indexlist | jq .
curl -su admin:<pass> https://<host>/api/v1/esindex/ | jq .
curl -su admin:<pass> https://<host>/api/v1/esindex/active/type/working
```

> These **hide** `.os` indices before Phase 3, for everyone.

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

> `_cat/indices` and the `indicies` table are **never** filtered. Use them to confirm an index exists
> when the dotCMS view hides it.

### The readiness report

```bash
curl -su admin:<pass> https://<host>/api/v1/index/migration/readiness | jq .

curl -su admin:<pass> https://<host>/api/v1/index/migration/readiness | jq .verdict
curl -su admin:<pass> https://<host>/api/v1/index/migration/readiness | jq .phase

curl -su admin:<pass> https://<host>/api/v1/index/migration/readiness \
  | jq '.content | map_values({esIndexedPercent, osIndexedPercent, verdict})'
```

### Documents and counts

```bash
curl -sk -u <user>:<pass> "https://<host>:9200/<physical-index-name>/_count"

# Did this specific write land?
curl -sk -u <os-user>:<pass> \
  "https://<os-host>:9200/<physical-os-index>/_doc/<identifier>_<languageId>_DEFAULT"
```

### Reindex

```bash
curl -su admin:<pass> -X POST    https://<host>/api/v1/esindex/reindex
curl -su admin:<pass> -X POST   "https://<host>/api/v1/esindex/reindex?contentType=Blog"
curl -su admin:<pass>            https://<host>/api/v1/esindex/reindex          # progress
curl -su admin:<pass> -X DELETE "https://<host>/api/v1/esindex/reindex?switch=true"
curl -su admin:<pass>            https://<host>/api/v1/esindex/failed
```

### Site Search

```bash
curl -sk -u <es-user>:<pass> "https://<es-host>:9200/_cat/indices/*sitesearch*?v"
curl -sk -u <os-user>:<pass> "https://<os-host>:9200/_cat/indices/*sitesearch*?v"
```

### The OpenSearch permission checks

```bash
curl -sk -u <os-user>:<pass> "https://<os-host>:9200/"
curl -sk -u <os-user>:<pass> -X PUT "https://<os-host>:9200/cluster_<id>.permcheck" \
     -H 'Content-Type: application/json' \
     -d '{"settings":{"number_of_shards":1,"number_of_replicas":0}}'
curl -sk -u <os-user>:<pass> -X DELETE "https://<os-host>:9200/cluster_<id>.permcheck"
```

### Setting the phase through the system table

```bash
curl -su admin:<pass> -X POST https://<host>/api/v1/system-table \
  -H 'Content-Type: application/json' \
  -d '{"key":"FEATURE_FLAG_OPEN_SEARCH_PHASE","value":"1"}'

curl -su admin:<pass> https://<host>/api/v1/system-table/FEATURE_FLAG_OPEN_SEARCH_PHASE

# Clear it — part of the rollback procedure
curl -su admin:<pass> -X DELETE \
  https://<host>/api/v1/system-table/FEATURE_FLAG_OPEN_SEARCH_PHASE
```

---

## Where to report what you find

- **Migration bugs:** the QA epic, dotCMS/core#35476. Include the stage, the phase, both physical index
  names, the query, and what you saw on each engine.
- **Plugin and template issues:** back to the customer's developer, with
  [R7](#r7-osgi-plugins-in-detail) and [`SEARCH_API_MIGRATION.md`](SEARCH_API_MIGRATION.md).
