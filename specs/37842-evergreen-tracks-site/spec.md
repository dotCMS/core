# Feature Specification: Evergreen Tracks & Taint on dev.dotcms.com Release Pages

**Feature Branch**: `37842-spec-evergreen-tracks-site`

**Created**: 2026-10-01

**Status**: Draft

**Type**: New Feature — **Tier 2** (new data interface between dotCMS/core CI/CD and the docsite; cross-repo: dotCMS/core + dotCMS/new-new-devsite)

**GitHub Issues**: parent [dotCMS/core#37842](https://github.com/dotCMS/core/issues/37842) (Standard/Trailing labels) and [#37316](https://github.com/dotCMS/core/issues/37316) (taint visibility). Implementation sub-issues closed by this spec's FR groups:

| FR group | Closes | Repo |
|---|---|---|
| A–C: site sync (`sync-site`), triggers, resilience | [#37846](https://github.com/dotCMS/core/issues/37846) | dotCMS/core |
| D: GitHub Release taint title | [#37847](https://github.com/dotCMS/core/issues/37847) | dotCMS/core |
| E–H: devsite chips, Current / All Releases / Changelogs pages | [#37843](https://github.com/dotCMS/core/issues/37843) | dotCMS/new-new-devsite |
| I: rollout order and manual prerequisite | [#37842](https://github.com/dotCMS/core/issues/37842) (manual checklist) | corpsites-headless (manual) |

Decision record: spike [#37844](https://github.com/dotCMS/core/issues/37844) (closed) chose a separate `EvergreenState` contentlet over new fields on release entries.

**Input**: PRD "Evergreen Tracks & Taint on Releases Pages" (2026-10-01, Steve Freudenthaler; https://claude.ai/code/artifact/33d9763e-3a2a-48ad-ac3b-13022fcd3ca3) and mockups (Current Releases, All Releases, chip spec: https://claude.ai/artifact/HUCuBxviFgLCEGAbrHZzgS). Summary of the request: dev.dotcms.com's release pages label which release each Evergreen track (Latest, Standard, Trailing) points to on Docker Hub, and flag tainted releases as `Not recommended` — kept up to date automatically by the dotcms/core CI/CD workflows that move the tags, with taint also marked on the GitHub Release title.

## Overview

The site shows **Docker Hub truth**: where `dotcms/dotcms:latest`, `:standard` and `:trailing` point right now — exactly what a customer gets on a pull — not what dotCMS Cloud environments run. Track tags move daily (06:00 ET promote, age-gated 14 d / 28 d) and `latest` moves on every GA cut, while Cloud adopts tags only in its biweekly maintenance window. Hub is the contract a self-hosted customer can verify, and an emergency patch (`-02`) is visible to the public as soon as Hub moves.

**Goal**: a customer can answer "what do I get if I pull `:standard` or `:trailing` today?" in one glance, and never picks a tainted release by accident.

### Decisions fixed by the PRD (binding on the plan)

| Topic | Decision |
|---|---|
| What is shown | Where the Docker Hub tag points now. Hold state (`<track>_hold`) and Cloud adoption timing are **not** shown. |
| How it updates | The CI/CD jobs that move tags also update the site data — no new runtime dependency for the docsite. |
| Where state lives | One `EvergreenState` record on corpsites-headless, never fields on release (Dotcmsbuilds) entries. |
| Sync style | Reconcile (full state each run), not delta. |
| Ordering | **Hub first, site second, always.** A site failure never blocks or rolls back a Hub move. |
| Retry | 3 attempts with backoff (about 10 min total) in-job, then give up non-blocking. |
| Blocking | Non-blocking everywhere, like changelog publish. Failures and drift post to `#dot-releases`. |
| Latest chip | Moves to `EvergreenState`; the devsite's old "first `lts === "3"` row" derivation is deleted — one implementation only. |
| Taint UI | `Not recommended` chip only, no per-release reason text; ships in v1. |
| Pull tag | Immutable `<version>_<sha>` on track rows, with a hint for the floating tag. |

### Data contract — `EvergreenState` (fixed by PRD)

One record on corpsites-headless mirrors Docker Hub; Docker Hub stays the source of truth.

| Field | Type | Example |
|---|---|---|
| `title` | Text | `evergreen-state` |
| `state` | JSON | `{"latest":"26.09.28-02","standard":"26.09.17-02","trailing":"26.09.03-01","tainted":["26.08.31-01","26.08.28-01"]}` |

The devsite joins it to release rows by matching each version string to the release entry's `minor`. The `Latest LTS` / `LTS` chips are unchanged and still come from the release entry's `lts` + `eolDate`.

### First-sync fixture (Docker Hub state at PRD time)

| Release | Docker tag | Chips after v1 |
|---|---|---|
| 26.09.28-02 | `26.09.28-02_3ae61b3` | Latest |
| 26.09.17-02 | `26.09.17-02_f363d73` | Standard |
| 26.09.03-01 | `26.09.03-01_cd466c8` | Trailing |
| 26.08.31-01 | `26.08.31-01_a6d1271` | Not recommended |
| 26.08.28-01 | `26.08.28-01_d5ab3fd` | Not recommended |
| 25.07.10-1v16 | — | Latest LTS (unchanged) |

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Track and taint state reach the site with no manual step (Priority: P1) — #37846

When any CI/CD job moves a track tag or changes a taint marker on Docker Hub, the site's `EvergreenState` record is reconciled to the full Hub state at the end of that job, with no CMS edit by anyone.

**Why this priority**: Every page change depends on this data. Without it, labels would be hand-maintained and go stale — the exact problem #37316 found on 2026-08-31, when two releases were tainted on Hub and nothing a human checks showed it.

**Independent Test**: Run the sync against Hub with the record deliberately wrong (e.g. Standard pointing at an older version, an extra tainted entry); verify the record is rewritten to match Hub exactly, that a second run makes no write, and that no release entry was modified.

**Acceptance Scenarios**:

1. **Given** the daily promote moves `standard` to a new release, **When** the job finishes, **Then** the record's `standard` value equals the version the Hub `standard` tag now resolves to.
2. **Given** an admin `taint` with `mode=apply` on version X, **When** the job finishes, **Then** X is in the record's `tainted` list; **and given** a later `untaint` of X, **Then** X is no longer in it.
3. **Given** the record already matches Hub, **When** the sync runs, **Then** no write or publish happens (one write per real change).
4. **Given** the sync was missed or disabled for one or more runs, **When** the next scheduled run executes, **Then** the record converges to the full current Hub state (reconcile, not delta).
5. **Given** any sync run, **When** it completes, **Then** zero release (Dotcmsbuilds) entries have been created, edited or published by it.
6. **Given** a dry-run, **When** the sync step runs, **Then** it prints the record it would write and the difference from the current record, and writes nothing.

---

### User Story 2 - A customer sees what each track tag pulls today (Priority: P1) — #37843

A self-hosted customer opens Current Releases or All Releases and immediately sees which release `latest`, `standard` and `trailing` point to on Docker Hub, with the immutable tag to pin and the floating tag to pull.

**Why this priority**: This is the customer-facing outcome of #37842. Today the site labels only Latest and Latest LTS, so a customer cannot tell what `:standard` or `:trailing` gives them.

**Independent Test**: With the record set to the first-sync fixture, load both pages and verify the chips and rows match the fixture table; change the record and verify the pages follow it after cache expiry.

**Acceptance Scenarios**:

1. **Given** the fixture state, **When** a customer opens Current Releases, **Then** an Evergreen section shows rows Latest (26.09.28-02), Standard (26.09.17-02), Trailing (26.09.03-01) in that order, each with the immutable Docker tag + copy button, a `or pull dotcms/dotcms:<track>` hint + copy button, released date, age in days, and Clean / Demo Site compose downloads; the LTS section below is unchanged.
2. **Given** the fixture state, **When** a customer opens All Releases, **Then** 26.09.28-02 shows `[Latest]`, 26.09.17-02 shows `[Standard]`, 26.09.03-01 shows `[Trailing]`, with the row highlight Latest gets today, and LTS rows keep their `Latest LTS` / `LTS` chips.
3. **Given** `latest` and `standard` resolve to the same release (quiet fortnight), **When** the pages render, **Then** All Releases shows one row with `[Latest] [Standard]`, and Current Releases shows two rows with the same version, each labeled with its own track.
4. **Given** a customer hovers a track chip, **Then** a one-sentence tooltip explains the track.

---

### User Story 3 - A customer never picks a tainted release by accident (Priority: P2) — #37843, #37846

A release dotCMS has tainted is visibly flagged `Not recommended` wherever the site lists it — All Releases, Current Releases and Changelogs — while staying findable for customers who already run it.

**Why this priority**: Prevents new adoption of known-bad releases. P2 because it reuses the P1 data path and UI.

**Independent Test**: Taint a GA version older than the current `trailing` release via the admin workflow with `mode=apply` (the forward-only planner can never move a track onto it, but the flag is public, so untaint right after); within 1 hour verify all three pages flag it; untaint and verify the flag disappears within 1 hour.

**Acceptance Scenarios**:

1. **Given** version X is tainted on Hub, **When** a customer views All Releases, **Then** X's row is muted (grey text) with a `Not recommended` chip in a warning color, stays in its normal position, keeps its changelog link, Docker tag and compose downloads.
2. **Given** version X is tainted, **When** a customer views X's entry on the Changelogs page, **Then** it carries the `Not recommended` chip.
3. **Given** the `standard` tag still points at tainted X, **When** the pages render, **Then** X shows both `[Standard] [Not recommended]` — the site does not hide or "fix" the state.
4. **Given** a customer hovers `Not recommended`, **Then** the tooltip reads exactly: "dotCMS has flagged this release; use a newer version."

---

### User Story 4 - Tainted releases are flagged on GitHub Releases (Priority: P2) — #37847

Support, customers and engineers who check github.com/dotcms/core/releases see that a release is tainted from its title.

**Why this priority**: Closes the GitHub half of #37316. Independent of the site path.

**Independent Test**: Run admin `taint` on a version older than the current `trailing` release (see US3) in dry-run and verify the planned title change is printed with no mutation; run with `mode=apply` and verify the title; re-run and verify the marker is not doubled; `untaint` and verify the original title is restored.

**Acceptance Scenarios**:

1. **Given** GitHub Release `Release 26.08.31-01`, **When** admin `taint` runs with `mode=apply`, **Then** the title becomes `⚠️ TAINTED Release 26.08.31-01`.
2. **Given** the title already carries the marker, **When** `taint` runs again, **Then** the title is unchanged (never doubled).
3. **Given** a marked title, **When** `untaint` runs with `mode=apply`, **Then** the marker is removed and the original title is restored exactly.
4. **Given** `mode=dry-run`, **When** `taint` or `untaint` runs, **Then** the planned title change (old → new) is printed and nothing is mutated.

---

### User Story 5 - Failures and drift are announced, never block, and self-heal (Priority: P2) — #37846

When the site can't be updated (devsite backend down, token expired, Hub read failed), the release team hears about it in `#dot-releases`, the Hub tag move and the release are unaffected, and the next daily run repairs the site.

**Why this priority**: A silent failure recreates the stale-site problem; a blocking failure would put a docsite outage in the release path.

**Independent Test**: Run the sync with an invalid site token; verify 3 attempts are made, the job that moved the tag still succeeds, a failure notice lands in `#dot-releases`; restore the token and verify the next daily run converges the record.

**Acceptance Scenarios**:

1. **Given** the site backend is unreachable, **When** the sync step runs after a Hub move, **Then** it retries up to 3 attempts with backoff (about 10 min total), then fails non-blocking with a `#dot-releases` notice naming the job and reason, and the Hub move stays in place.
2. **Given** the record still differs from Hub after the daily scheduled run's sync, **Then** a drift notice posts to `#dot-releases` naming which fields differ.
3. **Given** a track tag resolves to a version with no release entry on the site, **When** the sync runs, **Then** it fails loudly to `#dot-releases` and keeps that track's previous value in the record (the old chip stays rather than disappearing), while the other tracks and the `tainted` list are still reconciled.
4. **Given** the Hub read fails or is incomplete, **When** the sync runs, **Then** it writes nothing and fails non-blocking; the next daily run heals it.

### Edge Cases

| Case | Expected behavior |
|---|---|
| Two tracks on one release (quiet fortnight) | One All Releases row with both chips. Two Current Releases rows with the same version, each showing its own track. |
| Track points at a tainted release | Both chips show (e.g. `[Standard] [Not recommended]`) on every page that lists it. Sync does not "fix" it — the site mirrors Hub; ops repoints with a `hold`. |
| Track held (`_hold`) | No visible difference. The chip follows wherever the tag points, held or not. |
| Track tag points at a version with no release row on the site | Sync fails loudly to `#dot-releases`; that track keeps its previous value, so the old chip stays rather than disappearing. The rest of the record (other tracks, `tainted`) is still reconciled, so a missing row never delays a taint. |
| Release pipeline: `latest` moved but the new version's changelog entry failed to publish | Same as above: sync fails loudly, Latest chip stays on the previous release; the next daily run converges once the entry exists. |
| Hub API down or a partial Hub read during sync | Step fails non-blocking and writes nothing (a partial read must never shrink the `tainted` list or blank a track). The next daily run heals it. |
| Site backend down / token rejected | 3 attempts with backoff, then non-blocking failure + `#dot-releases` notice. Hub move unaffected. Worst-case lag: until the next daily run after the backend is reachable (about 24 h). |
| A human edited a release entry | No effect. Sync writes only the `EvergreenState` record, so changelog-publisher's human-edit protection (prior spec FR-011: an entry is trusted only while its last modifier is the service account) is untouched. |
| `tainted` list order differs from Hub listing order | Not a change; comparison is by set, so no write. |
| Taint on an LTS version | Out of scope. Taint is GA-only today (admin rejects non-GA versions). |
| Admin or promote run against a non-production repo (e.g. `dotcms/dotcms-test`) | No site sync and no GitHub Release title change; the site mirrors `dotcms/dotcms` only. |
| `untaint` of a version that is not tainted / title without marker | No-op for the title; sync writes nothing if the record already matches. |
| Taint of a version with no GitHub Release | Registry taint still applies; the missing release is reported (non-blocking) to `#dot-releases`. |
| `EvergreenState` record missing or unreadable on the devsite | Pages still render; release rows and LTS chips show as today, track/taint chips are absent, and the Evergreen section shows a short "track data unavailable" note instead of guessing. No fallback derivation of Latest. |
| Two tag-moving jobs finishing close together | Each sync reconciles full state; serialized with registry mutations, so the last sync reflects the last move. |

## Requirements *(mandatory)*

### Functional Requirements

#### A. Site sync — reconcile the `EvergreenState` record (closes #37846)

- **FR-001**: The system MUST provide a `sync-site` command in the existing `changelog-publisher` tool that reconciles the single `EvergreenState` record on corpsites-headless with Docker Hub state.
- **FR-002**: Each sync MUST read the full registry state for the production repo: the version each of `latest`, `standard` and `trailing` currently resolves to, and every `<version>_tainted` marker. Hold markers MUST NOT appear in the record.
- **FR-003**: The written `state` MUST follow the data contract exactly: keys `latest`, `standard`, `trailing` (one GA version string each, matching the release entry's `minor`) and `tainted` (list of GA version strings).
- **FR-004**: Sync MUST be reconcile, not delta: every run rewrites the record to match the full Hub state read in that run, so any missed run heals on the next one.
- **FR-005**: Sync MUST compare the current record with Hub state and write + publish only when they differ (one write per real change). The `tainted` comparison MUST be order-insensitive.
- **FR-006**: Sync MUST NOT create, modify or publish any release (Dotcmsbuilds) entry; it reads release entries only to verify a track's version has a row.
- **FR-007**: If a track resolves to a GA version that has no release entry the release pages list (live, `released`, `download`), the sync MUST fail loudly (FR-016) and MUST keep that track's previous value in the record; the other tracks and the `tainted` list MUST still be reconciled, so a missing row never blocks a taint or untaint from reaching the site.
- **FR-008**: If the Hub read fails or is incomplete (including a track that resolves to no GA version at all, e.g. its tag digest matches none), the sync MUST NOT write; a partial read must never remove tainted versions or blank a track.
- **FR-009**: Sync MUST support dry-run (the default when not applying) that prints the record it would write and the field-level difference from the current record, with no write.
- **FR-010**: Sync MUST authenticate with the existing service-account token and backend URL already used for changelog publishing (`DOTCMS_DEVSITE_RELEASENOTES_TOKEN`, `DOTCMS_DEVSITE_URL`); no new site credential. The token MUST never be logged.

#### B. Triggers and ordering (closes #37846)

- **FR-011**: Sync MUST run at the end of every job that mutates the registry, after its Hub mutation: (a) `evergreen-tracks-promote` after `apply` — on every run, including days nothing moved (daily drift heal, no separate cron); (b) `evergreen-tracks-admin` after an `apply` of taint / untaint / hold / release-hold; (c) the release pipeline after its latest-promote.
- **FR-012**: **Hub first, site second, always.** Sync MUST run only after the Hub mutation has completed; a site write failure MUST NOT roll back, block or fail the Hub move or the release.
- **FR-013**: In the release pipeline, the sync MUST run after both the latest-promote (`promote-latest`) and the changelog-site publish (`changelog-site-publish`) jobs for that release have finished (succeeded, failed or skipped), so the new version's release entry exists when it can. Today these jobs are independent (`changelog-site-publish` waits on `release-notes`, not on `promote-latest`), so the sync is a new step that waits on both. It is skipped when `promote-latest` did not run (e.g. LTS releases or `skip_latest`).
- **FR-014**: Syncs MUST be serialized with each other, and each sync MUST start only after its triggering mutation finished and read Hub fresh, so the last sync to run always reflects the last move and a sync never leaves the record older than a move that finished before it. Sync MUST NOT extend how long tag-moving jobs hold the registry lock (SC-005).
- **FR-015**: Sync and the GitHub Release title change MUST run only when the mutated repo is the production repo `dotcms/dotcms`; dry-runs of admin MUST preview the site effect of the planned action without writing (see FR-009, FR-022).

#### C. Resilience and alerts (closes #37846)

- **FR-016**: Sync MUST retry a failing attempt up to 3 attempts in-job with backoff (about 10 minutes total) and then give up, non-blocking. Final failures MUST post to `#dot-releases` naming the triggering job, the reason, and a link to the run.
- **FR-017**: After the daily scheduled promote run's sync, the system MUST verify the record matches Hub; if it still differs, it MUST post a drift notice to `#dot-releases` naming the differing fields.
- **FR-018**: Sync MUST post nothing to Slack when it succeeds or has nothing to change (the tag move itself is already announced by the promote workflow).
- **FR-019**: Notification failures MUST NOT fail the job (Slack down never fails a promotion or release).

#### D. GitHub Release taint title (closes #37847)

- **FR-020**: When `evergreen-tracks-admin` runs `taint` with `mode=apply`, after the registry marker is written, the system MUST prepend the marker `⚠️ TAINTED ` to the title of the GitHub Release for that version on dotCMS/core (tag `v<version>`). Idempotent: the marker is never doubled.
- **FR-021**: `untaint` with `mode=apply` MUST remove that marker, restoring the original title exactly; if no marker is present it is a no-op.
- **FR-022**: `mode=dry-run` MUST print the planned title change (old → new) and mutate nothing.
- **FR-023**: Existing Docker-registry taint / untaint behavior MUST be unchanged, and a GitHub Release title failure (including no release for the version) MUST NOT undo the registry marker; it is reported to `#dot-releases`.
- **FR-024**: The marker MUST change only the title; the release body, assets, tag and draft/prerelease/latest flags are untouched.

#### E. Devsite data source (closes #37843)

- **FR-025**: The devsite MUST read track and taint state only from the `EvergreenState` record (one extra single-record query alongside the existing release query) and match versions to release rows by `minor`.
- **FR-026**: The devsite MUST have exactly one source for the Latest chip — `EvergreenState`. The existing derivation (first release with `lts === "3"`) MUST be deleted from both the releases hook and the releases table; no fallback derivation remains.
- **FR-027**: `Latest LTS` and `LTS` chips MUST keep their current derivation from `lts` + `eolDate`.
- **FR-028**: Track and taint state on the site MUST reflect a record change within 1 hour (the devsite's existing 1 h release-data cache bounds this: `RELEASES_CACHE_TTL_SECONDS = 3600` in `services/docs/getReleases/getReleases.ts`). The new `EvergreenState` query MUST NOT be cached longer than that.
- **FR-029**: If the record cannot be read, pages MUST still render (rows, Docker tags, downloads, LTS chips) with track/taint chips absent and a short "track data unavailable" note in the Evergreen section — never a guessed Latest.

#### F. Current Releases page (closes #37843)

- **FR-030**: Current Releases MUST show an **Evergreen tracks** section first with exactly three rows — Latest, Standard, Trailing, in that order — above the **Long-term support** section, which is unchanged (Latest LTS plus other supported LTS lines).
- **FR-031**: Each Evergreen row MUST show: track chip; version linking to its changelog; immutable `<version>_<sha>` Docker tag with a copy button; released date; age in days; Clean and Demo Site compose downloads that use the immutable tag.
- **FR-032**: Under each row's Docker tag, a muted hint MUST read `or pull dotcms/dotcms:<track>` (e.g. `or pull dotcms/dotcms:standard`) with its own copy button.
- **FR-033**: One line under the Evergreen heading MUST define the tracks and link to the Evergreen Tracks page (https://dev.dotcms.com/docs/reference/releases/product-versions/evergreen-tracks). No Cloud timing appears on this page.
- **FR-034**: If two tracks resolve to the same release, both rows MUST still render, each showing its own track. A row whose release is tainted MUST also show `Not recommended`.
- **FR-035**: **Fallback (allowed only if the two-section layout is more than a simple page change in `CurrentReleases.js` / `TableReleases.tsx`)**: add Standard and Trailing rows, with chips, under Latest in today's single table using today's columns. Taking the fallback MUST be stated in the devsite PR; FR-031–FR-034 content still applies to the added rows except the column layout.

#### G. All Releases page (closes #37843)

- **FR-036**: The Type column MUST become a chip list. Possible chips: `Latest`, `Standard`, `Trailing`, `Latest LTS`, `LTS`, `Not recommended`. A row with none shows `–` as today.
- **FR-037**: Chip order inside a cell MUST be: Latest → Standard → Trailing → Latest LTS / LTS → Not recommended.
- **FR-038**: Rows carrying a track chip MUST get the same row highlight Latest gets today.
- **FR-039**: A tainted row MUST be muted (grey text) with `Not recommended` in a warning color; it stays in place, stays linkable, and keeps its Docker tag and compose downloads (images are never deleted; removing downloads is phase 2).
- **FR-040**: Chips MUST carry these tooltips (native title text, per the chip spec mockup): Latest — "Every new GA release."; Standard — "GA after a short stabilization window."; Trailing — "GA after an extended stabilization window."; Latest LTS — "Newest supported LTS."; `LTS` has no tooltip. `Not recommended` MUST carry exactly "dotCMS has flagged this release; use a newer version." No per-release reason text anywhere.
- **FR-041**: Track chips are distinguished by fill weight within one hue (solid → outline as the track gets more conservative), not by hue alone, per the chip spec mockup.
- **FR-042**: The All / Current / LTS filter tabs MUST be unchanged.

#### H. Changelogs page (closes #37843, per #37316)

- **FR-043**: A tainted release's entry on the Changelogs page (https://dev.dotcms.com/docs/reference/releases/product-versions/changelogs) MUST show the `Not recommended` chip with the same tooltip; `untaint` removes it via the same sync. Entry content is otherwise unchanged.

#### I. Rollout and prerequisite (tracked on #37842)

- **FR-044**: **Prerequisite (manual, out of scope for code):** the `EvergreenState` content type (`title` + JSON `state`) and its single record exist on corpsites-headless; the service-account token has read / edit / publish on it; the devsite's GraphQL reader has read access.
- **FR-045**: Rollout MUST follow this order: (1) prerequisite done; (2) core sync shipped and its first run backfills the record (including existing taints 26.08.31-01 and 26.08.28-01); (3) only then the devsite UI ships. The devsite PR MUST NOT merge before step 2 is verified.

### Key Entities

- **EvergreenState record**: Single content record on corpsites-headless (`title` = `evergreen-state`) whose JSON `state` mirrors Hub: the version each track resolves to and the set of tainted versions. Written only by the sync; read by the devsite.
- **Track**: One of `latest`, `standard`, `trailing` — a floating Docker Hub tag on `dotcms/dotcms` that resolves to one GA release (by digest). Moved by the release pipeline (`latest`), the daily promote (`standard`, `trailing`) and admin `hold`.
- **Taint marker**: `<version>_tainted` registry tag; means "no track may advance onto this version". Shown as `Not recommended`.
- **Hold marker**: `<track>_hold` registry tag; internal ops state, deliberately not shown.
- **Release entry (Dotcmsbuilds)**: Existing per-version record behind All Releases, Current Releases and Changelogs (`minor`, `dockerImage`, `releasedDate`, `lts`, `eolDate`, …). Read-only for this feature.
- **GitHub Release**: dotCMS/core release for tag `v<version>`, titled `Release <version>`; its title carries the taint marker.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Within 1 hour of any `latest` / `standard` / `trailing` move on Hub, both All Releases and Current Releases show the new chip and the previous release loses it — for 100% of moves in the first 30 days after launch.
- **SC-002**: Within 1 hour of `taint` with `mode=apply`, the release shows `Not recommended` on All Releases, Changelogs and (when a track points at it) Current Releases; `untaint` removes it within 1 hour.
- **SC-003**: Within the same admin run, `taint` marks the GitHub Release title and `untaint` restores it; re-running either leaves the title unchanged (marker count is always 0 or 1).
- **SC-004**: Zero manual CMS edits are needed for any of the above, after the one-time prerequisite.
- **SC-005**: Zero release pipeline runs or tag moves fail or are delayed by more than the retry budget (about 10 min) because of the site sync.
- **SC-006**: With the sync disabled for one day, the next scheduled run converges every row to Hub (reconcile test passes 100%).
- **SC-007**: The site never trails Hub by more than one daily promote cycle (about 24 h) once the backend is reachable, and every post-daily-run mismatch produces a `#dot-releases` drift notice.
- **SC-008**: On days nothing moved, the record gains zero new versions (no-op runs write nothing).
- **SC-009**: Zero release entries are modified by the sync (their last-modifier and content are unchanged across sync runs).
- **SC-010**: The devsite has a single source for the Latest chip: zero remaining occurrences of the `lts === "3"` Latest derivation in the releases hook and table.
- **SC-011**: A reader can name the release behind each of `:latest`, `:standard`, `:trailing` and copy its immutable tag from the first section of Current Releases without opening another page.
- **SC-012**: On the first sync run, the record matches the first-sync fixture (or the Hub state on that day), including both existing taints.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: CI/CD tooling only in dotCMS/core — the `changelog-publisher` tool (`.github/actions/core-cicd/changelog-publisher`, gains a command), the `evergreen-tracks` admin path (gains a GitHub Release title step) and three workflows (`cicd_evergreen-tracks-promote.yml`, `cicd_evergreen-tracks-admin.yml`, `cicd_6-release.yml`). No product Java code, no `com.dotmarketing.*`, no database schema, no ES mapping, no REST API. The docsite (dotCMS/new-new-devsite) release pages, hook and table change; the Changelogs page gains a chip.
- **Backward-compatibility expectations**: `changelog-publisher publish` and its exit/stdout contract are unchanged. Registry taint / untaint / hold / promote behavior and their Slack notices are unchanged. Release entries and their human-edit protection (prior spec `specs/36605-changelog-site-publish` FR-011) are untouched because the sync never writes them. LTS rows, LTS chips and the All / Current / LTS filters render as today. Rolling back the devsite PR restores today's Latest derivation. Rolling back core stops all sync, so the record goes stale and nothing heals it; roll back the devsite PR first (or with it) so the pages don't show stale chips. Nothing here is rollback-unsafe in core.
- **Known related decisions**: Spike #37844 (separate `EvergreenState` record, 1 h acceptance window to match the devsite cache). `specs/36605-changelog-site-publish` (service-account write path, non-blocking + `#dot-releases` pattern, FR-011 human-edit protection). evergreen-tracks README/RUNBOOK (tag cadence, taint/hold semantics, shared registry-mutation lock, forward-only planner). The plan phase will consult `dotCMS/platform-adrs` for release-pipeline and external-integration ADRs.

## Assumptions

- **Prerequisite is manual and out of scope**: creating the `EvergreenState` content type and record and granting token / GraphQL-reader access on corpsites-headless are done by hand (checklist on #37842); no code in this feature creates them.
- **Production repo only**: sync and the GitHub Release title change act only when the mutated repo is `dotcms/dotcms`; runs against `dotcms/dotcms-test` or other repos skip both. (PRD silent; smallest safe option, since the site mirrors the production tags.)
- **Title marker format**: the prefix is exactly `⚠️ TAINTED ` (marker plus one space) before the existing title, e.g. `⚠️ TAINTED Release 26.08.31-01`; untaint strips exactly that prefix. Only the title changes, not the body. (PRD/#37847 give the marker text but not the separator.)
- **No automatic GitHub title backfill**: the two releases already tainted (26.08.31-01, 26.08.28-01) get their title marker by an operator re-running `taint` with `mode=apply` (idempotent); the site backfill happens automatically on the first sync.
- **GitHub title failures are non-blocking**: a missing GitHub Release or API error is reported to `#dot-releases` and does not fail or undo the registry taint; the operator can re-run. (PRD silent on blocking for this step; mirrors the site rule.)
- **Tainted versions need no release row**: the "no release row → fail loudly" rule (FR-007) applies to track pointers only; a tainted version without a site row is still recorded and simply has nothing to render on.
- **Current Releases tainted track row**: the edge-case rule "both chips show" applies to Current Releases rows as well as All Releases (FR-034).
- **Evergreen row "released date" and "age in days"** come from the release entry's `releasedDate`, age counted in whole days to today in the viewer's local date; the immutable tag comes from the entry's `dockerImage`.
- **Missing record on the devsite**: render without track/taint chips and show a short unavailable note rather than erroring or deriving Latest (PRD forbids a fallback derivation but is silent on the failure display).
- **Slack is silent on success**: the sync posts only on failure or drift; the promote workflow already announces moves.
- **Drift check scope**: the post-daily-run drift check compares the record with Hub; it does not verify the devsite's rendered pages (cache makes that a 1 h-lagged view by design).
- **Serialization**: the sync runs as its own job in a dedicated concurrency group (`evergreen-tracks-site-sync`; dry-runs in `evergreen-tracks-site-sync-dry-run`), serialized with other syncs but outside the `evergreen-tracks-registry` lock, so it never extends how long tag-moving jobs hold that lock (FR-014, SC-005). Each sync starts only after its triggering mutation finished and reads Hub fresh, so the last sync always starts after the last mutation and reflects the last move; the daily reconcile still bounds any stale write to one cycle.
- **Retry budget**: "3 attempts with backoff, about 10 min total" covers the site write path; the Hub read reuses the existing registry client's behavior and, on failure, does not write (FR-008).
- **Wording**: ship "Not recommended"; revisit on customer feedback (PRD open question 3).
- **Taint on LTS** remains out of scope (GA-only).
- **Compose downloads for tainted rows** stay enabled in v1; removing them is a phase-2 option.
- **Timing ACs are 1 hour**, matching the devsite's existing 1 h release-data cache (spike #37844); lowering the cache TTL is not part of this feature.
