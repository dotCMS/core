# Feature Specification: Automated Release Changelog Publishing to dev.dotcms.com

**Feature Branch**: `36605-changelog-site-publish`

**Created**: 2026-07-16

**Status**: Draft

**Type**: New Feature

**GitHub Issue**: [dotCMS/core#36605](https://github.com/dotCMS/core/issues/36605) (parent epic: #35693)

**Input**: User description: "Automate publishing dotCMS release changelogs to dev.dotcms.com. When a release fires in dotCMS/core (GitHub release published), generate site-format markdown release notes and upsert-by-version a Dotcmsbuilds contentlet (fields: minor, releaseNotes, dockerImage, releasedDate, showInChangeLog, lts) on the corpsites-headless.dotcms.cloud backend via the dotCMS Workflow API, firing the System Workflow Publish action so it goes live with no human approval. Must be idempotent (re-runs self-heal drift), must include disabledWYSIWYG for the markdown field, must not duplicate build rows (the type doubles as the downloads registry)."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Release notes appear on the site automatically (Priority: P1)

When a dotCMS release is published, its changelog entry appears on the public changelog page (dev.dotcms.com/docs/changelogs) automatically — version, availability date, docker tag, and categorized release notes in the site's established format — without anyone touching the CMS.

**Why this priority**: This is the entire point of the feature. Today the changelog is hand-entered by one person, in batches, 2–4 days after release. Customers treat the site (not GitHub) as the source for release notes, so they see releases late or not at all.

**Independent Test**: Publish (or replay) a release event for a known version and verify a live changelog entry for that version appears on the changelog page with correct version, date, docker tag, and formatted notes — with no manual CMS step in between.

**Acceptance Scenarios**:

1. **Given** a new dotCMS release is published, **When** the automation completes, **Then** a live changelog entry for that version exists on the changelog page with version, availability date, docker tag, and categorized notes.
2. **Given** the automation has published an entry, **When** a customer visits the changelog page, **Then** the new release is listed at the top in the same visual format as existing hand-authored entries.
3. **Given** a release is published, **When** the automation runs, **Then** no human approval step is required at any point for the entry to go live.

---

### User Story 2 - Re-runs are safe and self-healing (Priority: P2)

An operator can re-run the publishing step for any version — after a failure, a notes correction, or a drift between GitHub and the site — and the site converges to the correct state with no duplicates.

**Why this priority**: The changelog data store doubles as the site's downloads registry; a duplicate row corrupts a second customer-facing surface. Idempotency is also the recovery story: with GitHub and the site as two copies of the notes, re-running the pipeline is how drift gets healed.

**Independent Test**: Run the publish step twice for the same version, then run it again with modified notes; verify exactly one entry exists for the version and it reflects the latest notes.

**Acceptance Scenarios**:

1. **Given** an entry for version X already exists, **When** the automation runs again for version X, **Then** the existing entry is updated in place and no second entry is created.
2. **Given** the notes for version X were regenerated with corrections, **When** the automation re-runs, **Then** the live entry reflects the corrected notes.
3. **Given** a partial failure mid-run, **When** the run is retried, **Then** the end state is identical to a single successful run.

---

### User Story 3 - Failures are visible, not silent (Priority: P3)

When publishing to the site fails (backend unreachable, auth expired, rejected payload), the release team can see the failure where they already watch releases, and the release itself is not blocked.

**Why this priority**: A silently failed post recreates today's problem (stale changelog) while removing the human who used to notice. But changelog publishing must never block or fail the software release itself.

**Independent Test**: Run the publish step with an invalid credential and verify the run reports failure visibly while the release process itself is unaffected.

**Acceptance Scenarios**:

1. **Given** the site backend is unreachable, **When** the automation runs, **Then** the failure is visibly reported (failed workflow run/alert) and the GitHub release remains published and intact.
2. **Given** a failed run, **When** the operator re-runs it after the outage, **Then** the entry publishes correctly (per User Story 2).

---

### Edge Cases

- Monorepo tags that are not product releases (e.g., CLI releases) must not create changelog entries.
- A release with no customer-facing changes still gets an entry (existing convention: a short "internal maintenance only" note).
- Hotfix releases (multiple releases same day, `-02`/`-03` suffixes) each get their own entry.
- LTS-track releases must carry the correct track designation so the site's Current/LTS filter buckets them correctly.
- A GitHub release edited after publication: re-running the pipeline for that version updates the site entry (covered by idempotency; automatic re-sync on edit is out of scope).
- The markdown notes must survive storage intact — headings, bullets, code spans, and issue links must render on the site exactly as generated (the storage field is rich-text by default and will mangle markdown unless explicitly stored raw).
- Concurrent runs for the same version (retry racing a re-run) must not produce duplicate rows.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST publish a changelog entry to the site automatically when a dotCMS product release is published, with no human action or approval between release and the entry going live.
- **FR-002**: Each entry MUST include the version identifier, availability date, docker image tag, and release notes categorized per the site's established changelog format (section headings such as Features / Enhancements & Adjustments / Fixes, per-item issue links back to GitHub).
- **FR-003**: The system MUST locate any existing entry for the version and update it in place; it MUST NOT create a second row for a version that already exists (the underlying record set also drives the site's downloads listing).
- **FR-004**: Re-running the pipeline for a version MUST be idempotent: the end state equals a single successful run, and the latest generated notes win.
- **FR-005**: Markdown formatting of the notes MUST be preserved end-to-end so the rendered entry matches the site's existing entries.
- **FR-006**: The system MUST mark each entry with the correct release track (current vs LTS) and changelog visibility so the site's filters place it correctly.
- **FR-007**: Non-product releases from the same repository (e.g., CLI) MUST be excluded.
- **FR-008**: A publishing failure MUST be visibly reported to the release team and MUST NOT block or fail the product release itself.
- **FR-009**: The system MUST authenticate to the site backend using a dedicated service credential stored as a managed secret (no personal tokens).
- **FR-010**: The generated notes MUST follow the site changelog's editorial format (concise, structured, no emoji) rather than the GitHub release notes format.

### Key Entities

- **Changelog entry (build record)**: One per released version. Carries version identifier, release/availability date, docker image tag, formatted release notes, release-track designation, and visibility flags. The same record set powers both the changelog page and the downloads listing — hence the no-duplicates constraint.
- **Release event**: The signal that a product version has shipped from dotCMS/core, carrying the tag/version and pointing at the release notes content.
- **Release notes document**: Markdown content generated from the release's changes, in site format; the single source both GitHub and the site render from.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new release's changelog entry is live on the site within 30 minutes of the release being published (baseline today: 2–4 days).
- **SC-002**: Zero manual CMS steps are required per release for the changelog (baseline today: every entry is hand-created).
- **SC-003**: After any number of runs and re-runs, exactly one changelog/downloads record exists per released version.
- **SC-004**: Rendered entries are visually indistinguishable in structure from existing hand-authored entries (headings, bullets, issue links render correctly) for 100% of published releases.
- **SC-005**: Every failed publish attempt is visible to the release team without anyone having to check the changelog page manually.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The dotCMS/core release pipeline (release-notes generation already feeds GitHub Releases) gains a delivery leg. The public changelog/downloads pages on dev.dotcms.com are consumers: their data model and rendering are untouched — this feature writes into the existing record set the site already reads. The current manual CMS-entry step is replaced, not altered.
- **Backward-compatibility expectations**: Existing hand-authored changelog entries must keep rendering unchanged; automated entries must be structurally indistinguishable from them. The downloads listing (driven by the same records) must keep working — hence the strict no-duplicate/upsert requirement. GitHub Releases behavior is unchanged. If the automation is off or failing, the manual path must still work exactly as today (the automation is additive, not a gate).
- **Known related decisions**: The site changelog's editorial format and Current/LTS track split are long-standing conventions to preserve. Release tagging conventions in the monorepo (product vs CLI vs LTS tags) govern the exclusion rules. The plan phase will formally consult `dotCMS/platform-adrs` for release-pipeline and external-integration ADRs.

## Assumptions

- The site's existing changelog data model and its publish workflow (which has no approval gate) remain as-is; this feature writes into the existing model rather than introducing a new one. Verified 2026-07-16: entries live in the `Dotcmsbuilds` content type on the corpsites-headless backend, publishable in one workflow-API call via the System Workflow, which has no approval step.
- The existing automated release-notes generation (which already feeds GitHub Releases) can produce the site-format markdown; this feature adds the delivery leg, adapting generation to the site's format where needed.
- The site remains the customer-facing source of truth; GitHub release notes remain published as today. One generator feeding both is the drift-prevention strategy, with idempotent re-runs as the repair path.
- Known storage gotcha to honor at implementation time: the notes field is a rich-text (WYSIWYG) field and the payload must explicitly disable WYSIWYG handling for it (`disabledWYSIWYG`) or markdown gets collapsed into a single paragraph.
- A service API token for the site backend will be provisioned and stored as a repository/organization secret; provisioning the token is an operational prerequisite, not part of this feature's code.
- Backfilling the handful of releases published between the last manual update and this feature's go-live is a one-time operational task using the same idempotent pipeline.
- LTS releases also publish GitHub releases from this repository; they are in scope and are distinguished by their tag/track metadata.
