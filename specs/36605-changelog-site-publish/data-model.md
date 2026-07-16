# Phase 1 Data Model & API Contract: Changelog Site Publishing

The feature has no local persistence. The "data model" is the shape of the remote
`Dotcmsbuilds` contentlet the tool upserts, plus the corpsites Workflow-API contract it
calls as a client. All values verified against the live backend 2026-07-16.

## Entity: `Dotcmsbuilds` contentlet (one per released version)

The same record set powers both the changelog page and the downloads registry — hence the
strict one-row-per-version upsert (FR-003, SC-003).

| Field | Type | Source in the pipeline | Notes |
|-------|------|------------------------|-------|
| `minor` | text (version string) | `release-prepare.outputs.release_version` (e.g. `26.07.10-01`, no `v` prefix) | The upsert key. Searched via `minor_dotraw` exact match. |
| `releaseNotes` | WYSIWYG (stores markdown) | `/tmp/site-release-notes.md` (site-format generation, D2) | MUST be sent with `disabledWYSIWYG: ["releaseNotes"]` or markdown collapses to one `<p>` (FR-005). Site editorial format: `### Fixes {#Fixes-<version>}` etc., per-item `[#NNNNN](github url)`, short prose intro, no emoji. |
| `dockerImage` | text | deployment output docker tag (e.g. `dotcms/dotcms:26.07.10-01_<sha>`) | From the release's produced image tag. |
| `releasedDate` | date | run date (today) | For an older-line patch this is the actual ship date, NOT the original line's date (FR-013). |
| `showInChangeLog` | radio (bool) | constant `true` | Makes it appear on the changelog page (FR-006). |
| `lts` | radio | current-track value | **Open**: confirm the exact current-track value from an existing current-track entry before hardcoding (task brief flagged `lts:3` frontend-query semantics — verify the stored contentlet value, which may differ from the GraphQL filter value). |
| `released`, `download` | (existing fields) | as used by current entries | Set to match how current-track hand-authored entries set them; confirm from a sample entry during implementation. |

**Read-only metadata used for control flow:**

| Field | Use |
|-------|-----|
| `modUserName` | Human-edit protection (FR-011): if ≠ the automation service account, skip + notify, do not overwrite. |
| `identifier` / `inode` | Returned by `_search`; the update-in-place path fires the workflow action against the existing identifier so no duplicate row is created. |

## API contract (client calls to corpsites-headless.dotcms.cloud)

Base URL `https://corpsites-headless.dotcms.cloud`. Auth: `Authorization: Bearer
$DOTCMS_DEVSITE_TOKEN` (FR-009; never logged).

### 1. Locate existing entry — `POST /api/content/_search`

```json
{ "query": "+contentType:Dotcmsbuilds +Dotcmsbuilds.minor_dotraw:26.07.10-01", "limit": 2 }
```

- Results at `entity.jsonObjectView.contentlets` (limit 2 so >1 is detectable → error, D4).
- 0 hits → create path; 1 hit → read `modUserName` + `identifier` for the upsert decision.

### 2. Publish (create or update) — `PUT /api/v1/workflow/actions/{actionId}/fire`

- `actionId` = `b9d89c80-3d88-4311-8365-187323c96436` (System Workflow Publish; no approval
  step, so firing publishes live — FR-001).
- Body carries the field set above plus, critically:

```json
{
  "contentlet": {
    "contentType": "Dotcmsbuilds",
    "minor": "26.07.10-01",
    "releaseNotes": "### Features ... markdown ...",
    "dockerImage": "dotcms/dotcms:26.07.10-01_<sha>",
    "releasedDate": "2026-07-16",
    "showInChangeLog": true,
    "lts": "<current-track value — confirm>",
    "identifier": "<present only on the update path>",
    "disabledWYSIWYG": ["releaseNotes"]
  }
}
```

- Create vs update is the *presence of `identifier`*; both go through the same fire call →
  idempotent (FR-004). The update path targets the identifier found in step 1 so exactly one
  row ever exists per version (SC-003).

## State transitions (the tool's decision table)

| Search result | `modUserName` | `--force` | Action |
|---------------|---------------|-----------|--------|
| 0 hits | — | — | **Create** + fire Publish |
| 1 hit | = service account | — | **Update in place** + fire Publish (idempotent) |
| 1 hit | ≠ service account | no | **Skip** → Slack skip notice (FR-011) |
| 1 hit | ≠ service account | yes | **Update in place** (explicit operator override) |
| >1 hit | — | — | **Error** → non-zero exit → Slack failure (must never happen) |

Any network/auth/payload error at any step → non-zero exit → Slack failure notice, release
unaffected (FR-008). No automatic backfill: the tool only ever acts on the single version
it was invoked with (FR-012).
