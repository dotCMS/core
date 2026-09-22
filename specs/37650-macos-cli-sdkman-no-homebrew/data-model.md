# Phase 1 Data Model: macOS CLI native build — removing the Homebrew dependency

**Feature**: `37650-macos-cli-sdkman-no-homebrew` | **Date**: 2026-09-21

## Not applicable — and why

This feature defines **no entities, no persisted state and no schema**. It changes one step of a
GitHub Actions composite action.

Recorded explicitly rather than omitted, so `/speckit-tasks` and the PR reviewer can tell the
difference between "no data model" and "data model not yet written".

| Data-model concern | Applies? | Notes |
|---|---|---|
| Domain entities / fields | No | No Java, no TypeScript, no product code of any kind |
| Database schema | No | No DDL, no migration, no `DotConnect` usage |
| Elasticsearch / OpenSearch mappings | No | No index touched |
| REST request/response shapes | No | No JAX-RS surface; no `@Schema` involved |
| Serialized / cached state | **Partially — see below** | GitHub Actions cache only |
| Content types / velocity / templates | No | None involved |

## The one piece of state: the GitHub Actions cache

The only durable artifact is the runner-side cache of the SDKMAN installation. It is not a data
model, but it carries the state transition this fix is about, so it is documented here.

**Entry**: key `${{ runner.os }}-${{ env.ARCHITECTURE }}-sdkman-install`, path `~/.sdkman`.

**Current (broken) state machine** — the loop the fix breaks:

```
cache MISS ──► Install Bash 4+ (macOS) ──► brew install bash ──► EXIT 1
                                                                    │
                     ┌──────────────────────────────────────────────┘
                     ▼
        Install SDKMan: SKIPPED  ──►  Save Cache: SKIPPED  ──►  cache still absent
                     │
                     └──────────────► next run: cache MISS again (loop)
```

**Target state machine**:

```
cache MISS ──► Install SDKMan (system Bash) ──► Save Cache ──► cache present
cache HIT  ──► install steps skipped ─────────────────────────► cache reused
```

**Invariants**:

- The key format must not change — see [contracts/setup-java-action.md](./contracts/setup-java-action.md).
  Changing it silently invalidates warm caches on platforms that work today, including Linux.
- Cache content remains a standard SDKMAN 5.x install tree, so a cache written before this change
  stays valid after it, and vice versa. No migration or invalidation is needed.
- No cleanup is required for the broken state: the entries never existed, so there is nothing to
  purge. The first green run simply creates them.
