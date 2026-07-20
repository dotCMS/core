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
| `releaseNotes` | WYSIWYG (stores markdown) | The GitHub release body (single shared artifact), fetched via `gh release view`; the publisher injects per-version heading anchors and strips GitHub's compare footer mechanically | MUST be sent with `disabledWYSIWYG: ["releaseNotes"]` or markdown collapses to one `<p>` (FR-005). Stored form: `### Fixes {#Fixes-<version>}` etc., per-item `[[#NNNNN](github issues url)]`, short prose intro, no emoji. |
| `dockerImage` | text | deployment output docker tag (e.g. `dotcms/dotcms:26.07.10-01_<sha>`) | From the release's produced image tag. |
| `releasedDate` | date | run date (today) | For an older-line patch this is the actual ship date, NOT the original line's date (FR-013). |
| `showInChangeLog` | radio (bool) | constant `true` (send JSON boolean `true`) | Makes it appear on the changelog page (FR-006). |
| `lts` | radio | constant `3` (send JSON integer `3`) | **Verified 2026-07-16** against `26.06.30-01`: the stored contentlet value for a current-track entry is the integer `3` — i.e. the frontend `+Dotcmsbuilds.lts:3` filter matches the *stored* value, they do not differ. Hardcode `3` for current-track. |
| `released` | radio (bool) | constant `true` (send JSON boolean `true`) | **Verified 2026-07-16**: stored `true` on the current-track sample. |
| `download` | radio | constant `1` (send JSON integer `1`) | **Verified 2026-07-16**: stored `1` on the current-track sample. |

**Read-only metadata used for control flow:**

| Field | Use |
|-------|-----|
| `modUserName` | Human-edit protection (FR-011): if ≠ the automation service account, skip + notify, do not overwrite. **Verified 2026-07-16**: this field is the last modifier's display name (`givenName surname`) — the `26.06.30-01` sample reads `"Jamie Mauro"` (release manager, hand-authored). Querying `/api/v1/users/current` with a token returns `givenName`/`surname`, confirming the write's `modUserName` = the authenticating user's display name. **No automation-written entry exists yet** (this feature introduces the first), so the service-account display name is not yet observable in the data — it is whatever the `DOTCMS_DEVSITE_TOKEN` user is named at provisioning (D8/FR-009). The tool takes the expected service-account identity as config (CLI `--service-account`, or env `DOTCMS_DEVSITE_SERVICE_ACCOUNT`) and compares it against the entry's last modifier; a mismatch = a human touched it → skip. **Comparison prefers the immutable `modUser` id over the mutable `modUserName` display name** (a rename must not silently disable FR-011 protection): the tool compares against `modUser` when the entry carries it (verified present — `user-5a8a91c4-…` on the sample) and falls back to `modUserName` only if a `modUser` id proves unavailable at provisioning time. So the configured value is normally the service account's **user id**. |
| `identifier` / `inode` | Returned by `_search` (verified present: `identifier` and `inode` are both top-level contentlet fields); the update-in-place path fires the workflow action against the existing `identifier` so no duplicate row is created. |

**Verified current-track sample (`26.06.30-01`, read-only `_search` 2026-07-16):**
`lts=3`, `released=true`, `download=1`, `showInChangeLog=true`, `modUserName="Jamie Mauro"`,
`dockerImage="dotcms/dotcms:26.06.30-01_84b0486"`, `releasedDate="2026-06-30 00:00:00.0"`
(stored as `yyyy-MM-dd HH:mm:ss.S`; the write sends the plain `yyyy-MM-dd` ship date, which
the date field accepts). These replace the earlier "confirm"/"open" placeholders — the
publisher hardcodes `lts=3`, `released=true`, `download=1`, `showInChangeLog=true`.

## API contract (client calls to the site's authoring backend)

Base URL comes from the `DOTCMS_DEVSITE_URL` repo variable — intentionally not committed
to this public repo, and a variable so backend migrations (like the 2026-07 authoring-host
cutover, which orphaned the previous hostname) are a settings change. Auth: `Authorization: Bearer
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
    "lts": 3,
    "released": true,
    "download": 1,
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
