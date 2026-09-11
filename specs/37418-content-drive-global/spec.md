# Issue Resolution Specification: Content Drive: adding a content-type filter to the global search hides matches the search alone found

**Feature Branch**: `37418-content-drive-global`

**Created**: 2026-09-11

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37211](https://github.com/dotCMS/core/issues/37211)

**Input**: User description: "Content Drive: adding a content-type filter to the global search hides matches the search alone found"

<!--
  This is the dotCMS ISSUE-RESOLUTION spec (used by /speckit-specify-fix). Unlike the
  feature spec, it is framed around a defect: what is wrong, how to reproduce it, and how
  we will know it is fixed. It still flows into /speckit-plan, where the Legacy Impact and
  ADR Alignment gates apply. Keep this technology-light — root-cause and fix details are
  refined in the plan.
-->

## Problem Statement *(mandatory)*

In Content Drive, the global (free-text) search filter and the content-type filter don't
compose correctly. Typing a search term with no content-type filter should return every
content item matching that term; adding a content-type filter should only narrow that same
set. Instead, Content Drive's unfiltered global search silently omits matching content that
the *same* search term finds as soon as a content-type filter is added — and removing the
content-type filter again makes that content disappear. The legacy Search All portlet does
not have this problem: it returns the correct matches for the same term with no content-type
filter needed.

This is not a "search can't find this term" bug — the term does match the content. It's a
"content that matches never gets asked" bug: on a large site, the affected content can be
silently excluded from every unfiltered global search result set, with no error, warning, or
indication to the user that results are incomplete.

**Severity / Impact**: Any Content Drive user searching without a content-type filter, on a
site with enough content that the affected item isn't scanned early. Confirmed on File Asset
content reached by filename; the underlying mechanism is not File-Asset-specific and can
plausibly bury matches of any content type. Silent data-completeness bug — no error is
raised, so users have no way to know their global search results are incomplete unless they
happen to retry with a content-type filter.

## Reproduction *(mandatory)*

**Environment**: Content Drive portlet, large dataset (~718,174 contentlets, per issue).

**Steps to Reproduce**:

1. In Content Drive, using only the global search filter, type "capy".
2. Capybara image assets (uploaded, matching by filename) are missing from the results.
3. Without clearing the search term, add a content-type filter for File Asset.
4. The same capybara images now appear.
5. Remove the content-type filter again (keep the search term) — the capybara images
   disappear from the results again.
6. In the legacy Search All portlet, searching "capy" with no content-type filter correctly
   returns the capybara images.

**Expected Behavior**: The global search filter alone returns every content item that also
matches when a content-type filter is added for the same term. Adding a content-type filter
only narrows that same match set.

**Actual Behavior**: The unfiltered global search misses matches that appear as soon as a
content-type filter is added, and lose them again as soon as it's removed.

**Reproducibility**: Always, for content whose matching rows are not reached within the
search's DB-scan budget before that budget is exhausted (see Root-Cause Hypothesis). Not
tied to File Assets specifically — reproducible for any content type/term combination where
the matching rows fall outside the scanned window when no content-type filter narrows the
candidate set.

## Scope of Investigation *(mandatory)*

- **Affected area**: Content Drive global search (free-text filter), specifically the
  DB+ES hybrid search heuristic in `BrowserAPIImpl`.
- **Suspected surface**: Modern (`com.dotcms.*`) — `com.dotcms.browser.BrowserAPIImpl`
  (`dotCMS/src/main/java/com/dotcms/browser/BrowserAPIImpl.java`), invoked via
  `com.dotcms.rest.api.v1.drive.ContentDriveHelper#driveSearch`.
- **Related known decisions**: None identified yet from `dotCMS/platform-adrs` — the plan
  phase's `speckit-adr-context` hook will confirm. The same hybrid DB+ES chunked-scan
  heuristic in `BrowserAPIImpl` was already the site of a related, but distinct, class of
  bugs fixed in [PR #37395](https://github.com/dotCMS/core/pull/37395) (single-pass field
  filter resolution) — worth reviewing for a compatible fix pattern.

## Root-Cause Hypothesis

**Confirmed via code-level investigation** (not yet reproduced live in a running
environment — see Verification below):

Content Drive's global-search-alone request is served by
`BrowserAPIImpl.doHybridSingleChunkedQueryES` (the default `HEURISTIC_TYPE`, per
`BrowserAPIImpl.java:503`, `HYBRID_SINGLE_CHUNKED_QUERY_ES`), regardless of whether a
content-type filter is present. This heuristic:

1. Scans the `identifier`/content SQL candidate set in fixed-size DB-order pages
   (`BROWSER_CONTENT_CHUNK_SIZE`, default **900** — `BrowserAPIImpl.java:805-809`).
2. ES-filters each page's candidates against the free-text term.
3. Stops as soon as either `maxResults` is satisfied, or the total rows scanned reaches
   `BROWSER_DB_MAX_SCAN_ROWS` (default **50,000** — `BrowserAPIImpl.java:802-803`, enforced
   at `BrowserAPIImpl.java:254`).

When **no content-type filter** is given, the SQL candidate set is effectively the whole
site's content in DB order — matching rows for a given term can be far enough down that
order that the scan hits its stopping condition before those rows are ever loaded and
ES-filtered. They are not failing the ES match; **they are never sent to ES at all.**

When a content-type filter **is** given, the query adds `struc.inode in (...)`
(`BrowserAPIImpl.java:2331`), sharply narrowing and reordering the SQL candidate set so the
matching rows appear early in the scan and are reached before the cutoff.

Two adjacent theories were checked and **refuted**, ruling out alternative root causes:

- *"The ES query only searches `fileName` when a content type is specified"* — refuted:
  `buildBaseESQuery` (`BrowserAPIImpl.java:1331-1369`) builds the identical
  `GlobalSearchAttributeStrategy` clause against `catchall`/`title` regardless of
  content-type filter state; the separate `fileName`/`metadata.name` clause only fires when
  `browserQuery.fileName` is explicitly set, which Content Drive's text search never does
  (`ContentDriveHelper.java:183` only calls `.withFilter(...)`).
- *"`catchall` doesn't index filenames, so ES itself can't match them"* — refuted:
  `ESMappingAPIImpl` copies the file's name into the field map (`ESMappingAPIImpl.java:1013-1014`)
  and the generic per-field loop folds every String field into `catchall`
  (`ESMappingAPIImpl.java:604-606,630`). The term does match via `catchall` — if the document
  ever reaches ES.

The legacy Search All portlet does not exhibit this bug because it does not go through this
DB-prescan/chunk mechanism — it queries ES directly. (Architecturally confirmed; the exact
backing class/line was not pinned down in this pass — candidates are
`ESSearchAPIImpl`/`ESContentResourcePortlet` under `dotCMS/src/main/java/com/dotcms/**` —
and should be confirmed during planning.)

[NEEDS CLARIFICATION: Confirm the exact Search All REST resource/service class and line
reference, to formally document the working comparison path in the plan.]

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Ensure Content Drive's unfiltered global search returns the same match set that the same
  term would return with any content-type filter applied, for the same term, on the same
  data — i.e. the DB-order chunked scan must not silently drop candidates that would
  otherwise match ES, regardless of scan/page cutoffs.
- Preserve the hybrid DB+ES heuristic's purpose (bounding scan cost on very large sites)
  while making its cutoff behavior consistent rather than order-dependent — either by
  removing the silent-drop behavior, exhausting the full scan before giving up, or another
  approach identified during planning.

**Explicitly out of scope / non-goals**:

- Rewriting Search All or unifying its search path with Content Drive's.
- General performance tuning of the hybrid heuristic beyond what's needed to fix the
  correctness gap (that was the subject of #37395 and siblings, already merged).
- Adding `fileName`/`metadata.name` as an explicit query field in
  `GlobalSearchAttributeStrategy` — not needed, since `catchall` already carries filename
  tokens (see Root-Cause Hypothesis); doing so would not address the actual defect.
- Any UI/UX change to Content Drive's search or filter controls.

## Regression Risk *(mandatory)*

- **Blast radius**: Any Content Drive global search request without a content-type filter,
  on large sites — this is the primary, default search path in Content Drive. A behavior
  change here affects result completeness and possibly response latency/resource cost for
  every such search.
- **Backward compatibility**: No API contract or serialized-state changes expected; this is
  an internal search-heuristic correctness fix. If the fix removes or changes the scan
  cutoff, it must not reintroduce the runaway-scan-cost problem the chunking was originally
  designed to avoid (see PR #37395 and siblings for that prior performance context).
- **Data considerations**: None — no data migration or repair; existing content and indices
  are unaffected, only how they are searched.

## Acceptance & Verification *(mandatory)*

- **AC-001**: In Content Drive, searching a term with no content-type filter returns every
  content item that the same term also returns when any content-type filter is applied,
  narrowed only by that content-type's own scope.
- **AC-002**: Adding a content-type filter to an existing global-search-only result set only
  removes items outside that content type; it introduces no new items.
- **AC-003**: For the same search term, Content Drive's unfiltered global search and the
  legacy Search All portlet return equivalent match sets (allowing for any documented,
  intentional scope differences between the two, e.g. base type support).
- **AC-004**: Verified against a large dataset (~718,174 contentlets, matching the scale in
  the issue) — the fix must hold at that scale, not just on small test datasets where the
  scan/page cutoffs are never hit.
- **Verification method**: Extend `dotcms-integration`'s
  `com.dotcms.browser.BrowserAPITest` (or add a new test class registered in the relevant
  `MainSuite`/`Junit5Suite`, per `docs/testing/INTEGRATION_TESTS.md`) with a case that seeds
  enough content to exceed `BROWSER_CONTENT_CHUNK_SIZE`/force the scan-limit path, places a
  matching item late in DB order, and asserts it's returned by an unfiltered global search.
  Manual verification against a large/large-like dataset per AC-004, given the original
  report requires ~718,174 contentlets.

## Assumptions

- The root-cause investigation in this spec was performed by static code analysis (reading
  `BrowserAPIImpl`, `ESMappingAPIImpl`, `ContentDriveHelper`, `GlobalSearchAttributeStrategy`)
  rather than a live reproduction against a running dotCMS instance with the reported
  dataset. The mechanism is well-supported by the code paths cited above, but the plan phase
  should confirm with an actual reproduction (e.g. via `just test-integration-ide`) before
  finalizing the fix approach.
- "Content-type filter" in the issue refers to Content Drive's content-type/base-type
  narrowing filter (`browserQuery.contentTypeIds`), not a mimeType filter — both narrow the
  same SQL candidate set per the code read, but this should be confirmed if the fix touches
  the query builder directly.
