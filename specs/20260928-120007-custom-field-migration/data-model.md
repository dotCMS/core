# Phase 1 Data Model: Instance-Wide Custom Field Migration Script

This feature has no database; "data model" here means the shapes that flow between `pull`,
the agent's migration step, and `push` — the manifest file is the actual contract, so its
schema is the center of this document. See `contracts/manifest-schema.md` for the literal
JSON shape and `contracts/cli-interface.md` for the CLI-level contract.

## Content Type *(read from the instance, not owned by this feature)*

What `pull` reads from `/api/v1/contenttype*` to find custom fields.

| Field | Type | Notes |
|---|---|---|
| `id` | string | Content type id, used to fetch the full type (fields aren't in the list response) |
| `variable` | string | Content type's variable name — the `Type` half of a `Type.field` key |
| `fields` | list of Custom Field | Only fields with `clazz == com.dotcms.contenttype.model.field.ImmutableCustomField` are in scope |

## Custom Field *(read from the instance, not owned by this feature)*

| Field | Type | Notes |
|---|---|---|
| `id` | string | Field id, used for the in-field `push` update endpoint |
| `variable` | string | The `field` half of a `Type.field` key |
| `values` | string (VTL) | Either a `#dotParse`/`#parse`/`mergeTemplate` reference, or literal VTL |

## Migration Item (in-memory during `pull`, persisted as a Manifest Entry)

The unit of work discovery produces and publish consumes. Exactly one of two kinds:

| Field | Type | Applies to | Notes |
|---|---|---|---|
| `kind` | `"asset"` \| `"field"` | both | Downloadable file vs. in-field VTL |
| `key` | string | both | The dA id (asset) or `Type.field` (field) — what `--only` matches against |
| `file` | relative path | both | Where it lives under `original/` and `migrated/` |
| `usedBy` | list of `Type.field` | both | Every content-type field that references this item (FR-003) |
| `label` | string | both | Human-readable identity for reporting |
| `status` | `pending` \| `published` \| `skipped` \| `failed` | both | State transitions below |
| `statusReason` | string, present when not `pending`/`published` | both | Why a skip/failure happened — required by FR-012 |
| `identifier`, `inode`, `languageId`, `contentType`, `binaryField`, `fileName` | asset fields | asset only | `inode` is the optimistic-concurrency token checked before publish (FR-009) |
| `typeId`, `fieldId` | field fields | field only | Used to re-fetch and compare the field's current `values` before publish (FR-009) |

### State transitions

```
pending --(push succeeds)--> published
pending --(server changed since pull, OR not a valid inline migration)--> skipped
pending --(publish request fails)--> failed
```

`published`, `skipped`, and `failed` are all terminal for a given manifest — re-running `push`
on an already-`published` entry is a no-op unless the customer re-runs `pull` first (which
would then report it as already migrated per FR-013, and it would not reappear as `pending`).

## Manifest File

One `manifest.json` per `pull` run, living at `<workdir>/manifest.json`. See
`contracts/manifest-schema.md` for the literal JSON Schema.

| Field | Type | Notes |
|---|---|---|
| `baseUrl` | string | Which instance this manifest belongs to — guards against pointing `push` at the wrong instance's workdir |
| `pulledAt` | ISO-8601 timestamp | When discovery ran |
| `entries` | list of Manifest Entry | See Migration Item above |

**Persistence rule** (see research.md): written once after `pull` completes; during `push`,
written back to disk after *every* entry's outcome is determined, not only once at the end of
the batch, so an interrupted run leaves an accurate partial record (Edge Cases, FR-012).

## Scan Report (pull's non-manifest output)

Everything in the five-way classification (FR-002) that is *not* a Migration Item — reported to
the customer, nothing downloaded or written to a manifest for these:

| Bucket | Meaning |
|---|---|
| `alreadyMigrated` | dA files / fields already containing the modern-editor marker |
| `coreFiles` | Fields loading a file shipped with dotCMS itself — migrated in core, not here |
| `inlineClean` | In-field VTL with no legacy patterns — nothing to do |
| `otherPaths` | `#dotParse` by a non-`/dA/` path — out of scope by design |
| `unresolved` | A referenced dA id that couldn't be resolved or downloaded — reported as a failure, not silently dropped |

## CLI Result (final JSON summary — the agent-facing contract)

What `pull` and `push` each print to stdout as their last line (FR-015). See
`contracts/cli-interface.md` for the literal shape and exit-code mapping.

| Field | Type | Notes |
|---|---|---|
| `command` | `"pull"` \| `"push"` | |
| `counts` | map of status → integer | E.g. `{"pending": 7, "alreadyMigrated": 2, ...}` for `pull`; `{"published": 5, "skipped": 1, "failed": 0}` for `push` |
| `entries` | list, for both `pull` and `push` | Enough detail for an agent to decide what to tell the customer next, without re-parsing human-readable text; for `push` this is populated even though `manifest.json` has also already been updated, since it's cheaper for an agent to read the final JSON than re-open the manifest |
| `exitCode` | integer, mirrors the process's actual exit code | Redundant with the process exit code on purpose — makes the JSON self-describing even if captured out of band |
