# Data Model: Evergreen Tracks & Taint on dev.dotcms.com Release Pages

**Spec**: [spec.md](spec.md) | **Contracts**: [contracts/](contracts/)

This file holds the field-level shapes that pass between Docker Hub, the `evergreen-tracks` and
`changelog-publisher` tools, corpsites-headless and the devsite. Shapes marked *verified* were
confirmed against trunk code (dotCMS/core `074698f3d21`, new-new-devsite `9b7cac9`). Shapes
marked *verify at first run* depend on the manual prerequisite (FR-044) and are checked in
[quickstart.md](quickstart.md) §2.

## 1. Hub state (`evergreen-tracks state` → file → `changelog-publisher sync-site`)

JSON Schema: [contracts/hub-state.schema.json](contracts/hub-state.schema.json).

| Field | Type | Rule | Source |
|---|---|---|---|
| `latest` | string | GA CalVer `^\d{2}\.\d{2}\.\d{2}-\d{1,2}$` | `_current_version("latest", …)`: the newest GA tag whose digest equals the `latest` tag's digest |
| `standard` | string | same | same, for `standard` |
| `trailing` | string | same | same, for `trailing` |
| `tainted` | string[] | GA CalVer each, unique, sorted oldest → newest | `markers.tainted_versions(names)` filtered to GA versions |

- All four keys are **required**. If any track can't be resolved, the producer exits `1` and
  writes no file (FR-008).
- Hold markers (`<track>_hold`) are never included (FR-002).
- `--as-if-*` (admin dry-run only) changes this object in memory before it is written:

  | `--as-if-action` | Effect |
  |---|---|
  | `taint` | add `--as-if-version` to `tainted` |
  | `untaint` | remove `--as-if-version` from `tainted` |
  | `hold` | set `[--as-if-track]` to `--as-if-version` |
  | `release-hold` | no change (the track moves at the next promote) |

Example (first-sync fixture):

```json
{"latest":"26.09.28-02","standard":"26.09.17-02","trailing":"26.09.03-01","tainted":["26.08.28-01","26.08.31-01"]}
```

## 2. `EvergreenState` record (corpsites-headless)

The content type and record are created by hand (FR-044, out of scope for code).

| Field (variable) | dotCMS field type | Value |
|---|---|---|
| `title` | Text | `evergreen-state` (exact; used in the lookup query) |
| `state` | JSON | the `state` object below |

**`state` object** (written by `sync-site`; read by the devsite)

| Key | Type | Notes |
|---|---|---|
| `latest` / `standard` / `trailing` | string, **optional per key** | A key can be absent only when a track has never had a release row (first sync during an outage). The devsite treats an absent key as "no chip". |
| `tainted` | string[] | always present; comparison is order-insensitive |

Validation and state rules (from FR-004–FR-008):

- **Reconcile**: desired = Hub state, except that a track whose version has no release row keeps
  its **current** record value (or stays absent).
- **Compare-then-publish**: write only when the fields differ. `tainted` is compared as a set.
  Field order for reporting is `latest, standard, trailing, tainted`.
- **Read**: `state` may come back as a JSON **string** or as an already-parsed **object**. Accept
  both. Treat empty, unparseable or non-object values as `{}`, so they are rewritten.
- **Write payload** (System Workflow Publish action, `PUBLISH_ACTION_ID` in `client.py`):

  ```json
  {"contentlet": {"contentType": "EvergreenState", "identifier": "<id>", "title": "evergreen-state",
                  "state": "{\"latest\":\"…\",\"standard\":\"…\",\"trailing\":\"…\",\"tainted\":[…]}"}}
  ```

  `state` is sent as a compact JSON **string** (`json.dumps(..., separators=(",", ":"))`).
  *Verify at first run* that the JSON field accepts it (quickstart §2).

- **Lookup query** (`POST /api/content/_search`, `limit: 2`):
  `+contentType:EvergreenState +EvergreenState.title_dotraw:evergreen-state`. 0 hits means the
  prerequisite is missing (runtime error). More than 1 hit is ambiguous (runtime error).

## 3. Release row (Dotcmsbuilds): read-only

`sync-site` only checks whether a row exists, using the same filter the devsite pages list with
(*verified*, `services/docs/getReleases/getReleases.ts`):

```
+contentType:Dotcmsbuilds +Dotcmsbuilds.minor_dotraw:<version> +Dotcmsbuilds.download:1 +Dotcmsbuilds.released:true +live:true
```

`limit: 1`. A hit means the row exists. No Dotcmsbuilds payload is ever built by `sync-site`
(FR-006, SC-009).

Fields the devsite uses for the new UI (*verified*, GraphQL `DotcmsbuildsCollection`):
`minor` (join key), `dockerImage` (`dotcms/dotcms:<version>_<sha>`), `releasedDate`, `lts`
(`"3"` = current track, `"1"` = LTS), `eolDate`, `parent.eolDate`, `starter`, `starterEmpty`.

## 4. GitHub Release title (dotCMS/core)

| Item | Value |
|---|---|
| Release tag | `v<version>` |
| Normal title | `Release <version>` (*verified* on live releases) |
| Marker | `TAINT_TITLE_MARKER = "⚠️ TAINTED "` (⚠️ + one space + `TAINTED` + one space) |
| `taint_title(t)` | `t` if it already starts with the marker, otherwise marker + `t` (never doubled) |
| `untaint_title(t)` | `t` without the leading marker if present, otherwise `t` unchanged (exact restore) |

Only the release `name` changes (FR-024).

## 5. Devsite view model (`util/evergreen.ts`)

| Export | Shape | Rule |
|---|---|---|
| `type EvergreenState` | `{ latest?: string; standard?: string; trailing?: string; tainted: string[] }` | |
| `parseEvergreenState(raw: unknown)` | `EvergreenState \| null` | accepts an object or a JSON string; `null` when unusable (missing record, bad JSON, `tainted` not an array) |
| `type Chip` | `'Latest' \| 'Standard' \| 'Trailing' \| 'Latest LTS' \| 'LTS' \| 'Not recommended'` | |
| `CHIP_TOOLTIP` | `Record<Chip, string \| undefined>` | Latest "Every new GA release."; Standard "GA after a short stabilization window."; Trailing "GA after an extended stabilization window."; Latest LTS "Newest supported LTS."; LTS `undefined`; Not recommended "dotCMS has flagged this release; use a newer version." |
| `chipsFor(minor, lts, isLatestLts, state)` | `Chip[]` | order Latest → Standard → Trailing → Latest LTS / LTS → Not recommended. Track chips only when `state?.[track] === minor`. `Latest LTS` when `isLatestLts && lts === "1"`; else `LTS` when `lts !== "3"`. `Not recommended` when `state?.tainted` includes `minor`. Empty array → caller renders `–`. |
| `isTainted(minor, state)` | `boolean` | |
| `hasTrackChip(minor, state)` | `boolean` | drives the row highlight (FR-038) |
| `evergreenRows(releases, state)` | `{ track: 'latest'\|'standard'\|'trailing'; release: R }[]` | always in Latest, Standard, Trailing order. One row per track whose version is found in `releases` by `minor`. Two tracks on one release give two rows. Tracks absent from `state` or not found are skipped. |
| `ageInDays(releasedDate, today)` | `number` | whole days between the release date and `today` (local date), minimum 0 |

`lts` arrives from GraphQL as a string (`"1"`, `"3"`), as the current code assumes.
