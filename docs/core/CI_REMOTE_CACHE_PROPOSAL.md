# Content-addressed build caching across CI

Status: **layer 2 (Maven) implemented against an OVH S3 bucket; layers 1 and 3 open**
Context: follows the measurement work in #36942 / #36945

## What shipped

The Maven layer, because it needed no new dependency: the Apache Maven Build
Cache Extension was **already installed** (`.mvn/extensions.xml`, v1.2.0) and
already enabled — but local-only, so on a fresh runner it cached nothing. The
missing piece was shared storage.

| Piece | Where |
|---|---|
| `<remote>` block, off by default | `.mvn/maven-build-cache-config.xml` |
| Signing proxy + `BUILD_CACHE_ARGS` | `.github/actions/core-cicd/build-cache-remote/action.yml` |
| Appends the flags to the build | `.github/actions/core-cicd/maven-job/action.yml` |
| Wiring + provenance record | `.github/workflows/cicd_comp_build-phase.yml` |
| Credentials per trust level | `cicd_1-pr.yml` (read-only), `cicd_2-merge-queue.yml`, `cicd_3-trunk.yml` |

Scope is the **Initial Artifact Build** only — the serial prefix that gates every
test job. Test jobs are untouched; they consume the `maven-repo` artifact, and
caching a test phase would let a green run mean "we did not run".

**Transport.** The extension speaks HTTP `PUT`/`GET`/`HEAD`; S3 needs SigV4.
Rather than add a Maven S3 wagon — the maintained ones aren't (`seahen` 1.3.3 is
2021, `gkatzioura` 2.3 is 2019, both AWS SDK v1) — an
[`aws-sigv4-proxy`](https://github.com/awslabs/aws-sigv4-proxy) container signs
on the way out and Maven talks to `127.0.0.1`. Zero new Maven dependencies.

Verified against MinIO before wiring: `PUT`/`GET`/`HEAD` all `200`; a missing key
answers `404` (a `403` would be read as a hard error, not a miss); 10 MB bodies
round-trip byte-identical; a build with an **empty** local cache restores from the
remote; and with the remote unreachable the build still exits `0`.

**Two gates checked empirically, not assumed:**

1. *Does a cache hit dirty the working tree?* `cicd_comp_build-phase.yml` fails a
   PR when `git status --porcelain` is non-empty, and `openapi.yaml` is a tracked
   file generated at compile. Built `:dotcms-core --am` twice: 12 modules restored
   from cache including `dotcms-core`, `openapi.yaml` md5 identical across both
   runs, `git status` unchanged. **Safe.**
2. *Do PR keys match trunk keys?* Mostly — but **not** for `dotcms-core-web`.
   `core-web/pom.xml` declares profile `is_pr`, auto-activated by the
   `-Dgithub.event.name=pull_request` that the build phase passes. It flips
   `skip.validate` and `nx.affected.options`, so PR and trunk genuinely run
   different builds and correctly hash differently. The other ~20 Java modules
   share keys. Expect hits on the Java side, none on core-web.

**Sizing, honestly:** the ceiling is the ~7.0m of Maven time inside a 14.2m build
job, against a 74–103m PR wall clock. The twin-tail shard rebalance (#36943)
remains the larger lever.

## Turning it off

A cache you cannot bypass is a liability, so there are four levels, none of which
need a workflow edit:

| Scope | How |
|---|---|
| One pull request | add the label `CI : No Build Cache` |
| Everything, immediately | set repo/org variable `BUILD_CACHE_DISABLED=true` |
| One local build | `./mvnw … -Dmaven.build.cache.enabled=false` |
| Force a rebuild but still publish | `-Dmaven.build.cache.skipCache=true` (skips reads, not writes) |

`-Dmaven.build.cache.remote.enabled=false` keeps the local cache and ignores the
shared one, which is the useful setting when you suspect the remote specifically.

## Releases are not affected

Release, LTS, nightly, manual-deploy and CLI-release workflows do **not** pass the
build-cache secrets, and no workflow in this repo uses `secrets: inherit` — so the
action sees empty credentials and exports an empty `BUILD_CACHE_ARGS`. Those
pipelines build from scratch exactly as they do today.

That is deliberate rather than incidental: a release is the one build where
"we did not actually compile this" is least acceptable, and the time saved is
worth least. Only the PR, merge-queue and trunk workflows opt in.

## Infrastructure

Provisioned on OVH S3-compatible object storage, as org-level GitHub secrets:

| Secret | Used by |
|---|---|
| `OVH_S3_BUILD_CACHE_BUCKET_ACCESS_KEY` / `..._SECRET_KEY` | merge queue, trunk — read-write |
| `OVH_S3_BUILD_CACHE_BUCKET_ACCESS_KEY_RO` / `..._SECRET_KEY_RO` | PRs — GetObject only |
| `OVH_S3_BUILD_CACHE_BUCKET_ENDPOINT` / `..._NAME` | all |

A lifecycle expiry rule is set on the bucket. It has to be: `maxBuildsCached`
only trims the *local* cache, and every run also drops a `build-cache-report.xml`
under a fresh UUID.

One interaction to be aware of: `remote.save.final=true` means an existing entry
is never rewritten, so a hot entry's age is never refreshed and it expires on
schedule even while it is being used. That self-heals — the next trusted build
repopulates it — but there is a window after each expiry where PRs miss. The
alternative, dropping `save.final`, would let any writer overwrite an existing
entry, which is worse.

Nothing verifies from the repo side that the `_RO` key is genuinely read-only, so
the build-cache action asserts it: on any job that should not write, it attempts
one `PUT` and expects `403`. A `200` raises a warning, because a writable
"read-only" key looks exactly like a correct setup until someone abuses it.

## The principle

Nothing should be built twice from identical inputs — across runs, across
branches, across PRs.

Today every run rebuilds everything: the whole nx workspace, all 24 Maven
modules, and the Docker image, from scratch, on a fresh runner. The
`Initial Artifact Build` that does most of this is the **serial prefix** gating
all ~25 test jobs.

The fix is content-addressed caching: hash a unit of work's real inputs, and if
that hash has been seen, fetch the output instead of recomputing it. Three
layers, three tools.

| Layer | Tool | Today |
|---|---|---|
| Frontend (`nx run-many -t build`) | nx remote cache | local only, discarded with the runner |
| Java modules (CLI, core, all 24) | Apache Maven Build Cache Extension | not used |
| Docker image | buildx registry cache | **no layer cache at all** |

The three are independent and can land in parallel.

## Why `actions/cache` cannot be the answer

GitHub Actions caches are **branch-scoped**. A run reads caches from its own
branch and the default/base branch — never a sibling branch. Two PRs building
identical code share nothing. That is a deliberate security boundary.

So `actions/cache` only supports *save-on-trunk, restore-everywhere*: a PR
benefits only from what `main` has already built.

Second problem: `.nx/cache` is **2.0 GB** on a working machine. The Actions
cache is 10 GB per repo with LRU eviction, already shared with the Maven
repository, pnpm store and Node binary caches. A large, frequently-rewritten
entry would evict the others and could make builds *slower*.

A real remote cache is keyed purely on the content hash — no branch dimension,
no 10 GB ceiling.

## Runner geography (measured, not assumed)

12 samples via PR #36948, cross-checked two ways (Azure IMDS `.location` and the
`Azure Region:` line GitHub prints in every job log header — they agreed 100%):

```
centralus       x3   Iowa
westus2         x3   Washington
eastus          x2   Virginia
northcentralus  x2   Illinois
westus          x1   California
eastus2         x1   Virginia
```

Runners are spread across **six US Azure regions**, coast to coast. No region
dominates. Consequences:

- A **single-region bucket is the wrong shape** — fast for roughly one job in
  six, slow for the rest (westus↔eastus is ~60–70ms RTT).
- This favours a managed multi-region cache, or a bucket behind a CDN. If
  self-hosting in one region anyway, pick a central compromise (`us-east-2`),
  not `us-east-1`.
- Small per-task entries (nx outputs, module jars) tolerate this well; **large
  artifacts do not** — see the Docker section.

Incidental finding: all runners are 4 vCPU / 15 GB, but the VM SKU varies by
generation (`Standard_D4ads_v5` ×10, `D4ds_v6` ×1, `D4ds_v7` ×1). That is a
concrete cause of run-to-run wall-time noise, and a reason to judge any change
over several runs.

## Layer 1 — frontend (nx): still open, and the obvious route is closed

Nx already hashes task inputs; it only lacks somewhere to keep results. But
**`@nx/s3-cache` is not usable**, for two independent reasons:

1. **Deprecated 2026-05-21 with an unpatchable CVE** — CVE-2025-36852
   ("CREEP"), which also covers `@nx/gcs-cache`, `@nx/azure-cache` and
   `@nx/shared-fs-cache`. One credential grants read *and* write across the whole
   cache, nothing records which branch produced an entry, and the CI workflow
   file isn't in the cache key — so a PR that changes only the workflow hashes to
   the same key trunk will later hit.
2. **Forking is not permitted.** The npm tarball ships compiled (`index.js`, no
   source) under a proprietary Narwhal Technologies EULA whose §2(b) forbids
   derivative works and decompilation. There is no OSS package to fork.

Remaining options:

- **Nx Cloud** — free tier, multi-region edge (which the runner spread makes
  genuinely valuable), nothing to operate. Build outputs leave our
  infrastructure, so it needs a security review.
- **Our own cache server** — Nx documents a stable OpenAPI contract
  (`PUT`/`GET /v1/cache/{hash}`, **409 on overwrite**) behind
  `NX_SELF_HOSTED_REMOTE_CACHE_SERVER` and
  `NX_SELF_HOSTED_REMOTE_CACHE_ACCESS_TOKEN`. A small sidecar implementing those
  three endpoints against the same S3 bucket is legal, dependency-free, and lets
  us enforce the write-once semantics the deprecated package got wrong. Same
  sidecar shape as the SigV4 proxy already used for Maven.

## Layer 2 — Java modules (Maven Build Cache Extension)

The [Apache Maven Build Cache Extension](https://maven.apache.org/extensions/maven-build-cache-extension/)
fingerprints each module's inputs by content digest, caches its outputs, and
skips the module on a hit. Remote cache works over plain HTTP `PUT`/`GET`/`HEAD`,
or anything Maven Wagon speaks (S3, SSH).

**This is the answer to "we don't need to build the CLI every build."** Rather
than hand-maintaining which modules a PR needs, the hash decides — and it covers
`dotcms-cli`, `dotcms-api-data-model`, `dotcms-core` and the other 21 modules at
once.

It also **subsumes two other pieces of work**:

- the Maven reactor trim still open on #36945, and
- the path-filter artifact reuse that #36081 attempted for the CLI.

Earlier I argued that reusing a prior WAR needs a *provably complete* predicate
for "did anything relevant change". Content hashing **is** that predicate,
computed rather than curated, which removes the class of bug where a hand-written
filter drifts and a PR silently tests a stale binary.

### The real work: hidden inputs

Cache correctness depends on the hash covering every real input. Plugins that
read undeclared state produce hits that should have been misses. dotCMS has
several to handle explicitly:

- git metadata read during the build (`git rev-parse` for `ShortRevision` /
  `scmBranch`)
- `openapi.yaml` generated by `swagger-maven-plugin` at compile
- starter zip assembly
- the `process-annotations` immutables pass

Also enable `project.build.outputTimestamp` — timestamps embedded at package
time make outputs non-reproducible and silently destroy hit rate.

## Layer 3 — Docker image

Currently there is **no layer cache**: every build runs `docker build` on a fresh
runner with an empty local cache. buildx registry caching
(`--cache-to`/`--cache-from type=registry`) fixes this and transfers only changed
layers.

Do **not** try to cache the image through the Maven or nx cache. The `docker save`
tar is ~1 GB, and with runners spread across six regions, pulling that from a
distant cache can cost more than rebuilding. Layer-level caching against a
registry is the right granularity.

## What must NOT be cached (at least not first)

**Test execution.** The Maven extension caches `package` and later phases by
default and *can* cache `test`. Memoised test results mean a green run may mean
"we did not run", not "it passed". We have already measured real flakiness in
this suite, plus a hang that burned a 122m job timeout — masking that is worse
than the time it saves.

Cache compile/package. Run tests. Revisit only once hashing is demonstrably
correct.

## Security

A cache that untrusted code can **write** is a remote code execution vector: a
poisoned entry is replayed as a build output on trunk.

The CREEP write-up is worth reading as the canonical description of this failure
mode. Its lesson: **the control is the credential, not the client.** A job holding
a write-capable key does not need to go through the cache plugin at all — one
`aws s3 cp` poisons any key it can compute. So client-side switches
(`maven.build.cache.remote.save.enabled`, nx's `ciMode`) are defence in depth
only; they are not the boundary.

- **PR builds get a GetObject-only key pair.** This is the actual boundary.
- **Only trusted refs write** — merge queue and trunk populate, PRs consume.
  `-Dmaven.build.cache.remote.save.enabled` is set from the event, and
  `.remote.save.final=true` stops any build overwriting an existing entry.
- **Fork PRs receive no secrets at all** and fall back to a normal uncached
  build.
- **Every entry carries provenance.** Writing runs upload a `provenance.json`
  beside each hash (ref, sha, event, workflow ref, run id, actor),
  first-writer-wins, so an entry keeps the identity of the build that created it.
  This is the missing "which branch produced this artifact" that CREEP names.
- Hashes must include toolchain versions, so a Node or JDK bump cannot collide
  with an older entry.

This costs some hit rate — a PR cannot reuse another PR's *novel* work — but
keeps the case that matters: unchanged subsystems.

## Order

1. ~~nx remote cache~~ — reordered: the S3 adapter turned out to be deprecated,
   CVE'd and unforkable, so this became the *harder* layer, not the easier one.
2. **Maven build cache** — **done**, read-only on PRs. Measure hit rate before
   trusting it; the hidden inputs above are still the risk to watch.
3. **buildx registry cache** for Docker. Independent of 1 and 2, needs no bucket
   (we already push to a registry), and is currently a complete gap.
4. **nx**, via Nx Cloud or our own sidecar implementing Nx's cache-server
   contract.
5. **Only then** consider caching test phases.

Land #36946 first and re-measure — it stops building SDK projects the WAR never
contains, so it shrinks both the serial prefix and the 33m frontend job, and
today's baselines will not hold afterwards.
