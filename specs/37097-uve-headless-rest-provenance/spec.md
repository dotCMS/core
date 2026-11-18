# Issue Resolution Specification: UVE pushes REST-sourced pageAsset to headless clients

**Feature Branch**: `37097-uve-headless-rest-provenance`

**Created**: 2026-09-10

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37097](https://github.com/dotCMS/core/issues/37097)

**Input**: User description: "UVE pushes REST-sourced pageAsset to headless clients — #36410's gate checks requestMetadata existence, not asset provenance. Full analysis, repro and proposed fix in dotCMS/core#37097."

<!--
  This is the dotCMS ISSUE-RESOLUTION spec (used by /speckit-specify-fix). Unlike the
  feature spec, it is framed around a defect: what is wrong, how to reproduce it, and how
  we will know it is fixed. It still flows into /speckit-plan, where the Legacy Impact and
  ADR Alignment gates apply. Keep this technology-light — root-cause and fix details are
  refined in the plan.
-->

## Problem Statement *(mandatory)*

UVE (Universal Visual Editor) can push its own REST-fetched copy of a page into a headless
client's iframe, overwriting the client's correct GraphQL-sourced render with a structurally
different payload. Relationship fields arrive flattened to bare identifier strings instead of
resolved objects, and Site-or-Folder / Category fields are missing entirely or carry different
data than the client's own render. Components that read a field on a related item then throw,
and the page dies in `EDIT_MODE` with no editor interaction from the author.

This is a follow-up to #36410, which shipped in `26.07.17-01` and passed QA against its own
acceptance criteria. The customer is still broken in Production on `26.07.21-01` because
#36410's fix gates on the wrong condition: whether the client's GraphQL query has been
*registered*, not whether the payload about to be sent actually *came from* it.

**Severity / Impact**: High — major functionality broken. Affects all headless sites on
Evergreen using the SDK's GraphQL client (the default from `@dotcms/client` v1 onward) whose
components read nested fields on related content. The reporting customer has 26 of 30 GraphQL
fragments requesting nested relationship objects, so every contentlet on the affected pages is
impacted; roughly 20 further components degrade silently (duplicate-key React warnings) rather
than crashing outright, so the crash count understates the reach.

## Reproduction *(mandatory)*

**Environment**: Evergreen `26.07.21-01` (Production and UAT, dotCMS Cloud, `standard` track).
Regression confirmed present from `26.07.17-01` onward — i.e. present in the release that
shipped #36410. A site with a UVE app config pointing at a headless front end (any truthy
`clientHost`, so `pageType = HEADLESS`).

**Steps to Reproduce**:

1. Configure a site with a UVE app config pointing at a headless front end (any truthy
   `clientHost` → `pageType = HEADLESS`).
2. Build a page whose components read nested fields on related content — e.g. `image.inode`,
   or a Site-or-Folder field's `folderPath` — with no optional chaining.
3. Have the front end fetch the page through the SDK's GraphQL client (`client.page.get()`).
4. Open the page in UVE `EDIT_MODE`.
5. Observe the page render correctly from the app's own GraphQL fetch, then crash with no
   editor interaction.

**Expected Behavior**: Every `UVE_SET_PAGE_DATA` payload the client receives carries
relationship fields as resolved objects, matching the client's registered GraphQL query — i.e.
the client never receives a REST-shaped page payload while operating as a headless client.

**Actual Behavior**: REST-shaped payloads are interleaved with the client's own GraphQL
renders. A captured production load of `/news-and-insights` showed the sequence: GraphQL
render, GraphQL render, REST push (unresolved relationships), REST push (unresolved
relationships), GraphQL render, GraphQL render — i.e. the bad pushes arrive *after* the app has
already rendered correctly twice, ruling out a "first load of the session" explanation.
Resulting client errors include:

```
TypeError: Cannot read properties of undefined (reading 'startsWith')
  at buildPath          <- receives image.inode
  at buildImagePath

TypeError: Cannot read properties of undefined (reading 'folderPath')
  at buildQuery
```

Field-by-field comparison from the same load (client GraphQL payload vs. pushed REST payload):

| Content type | Field | Client (GraphQL) | Pushed (REST) |
|---|---|---|---|
| FeaturedLinks | `featuredLinksLink1`/`2`/`3`, `featuredLinksSpotlightLink` | resolved object | bare identifier |
| EmailCapture | `emailCaptureTeaserCta` | resolved object | bare identifier |
| NewsCollection | `newsCollectionTopicLinks` | resolved objects (3) | identifiers (3) |
| NewsCollection | `newsCollectionRootPath` (Site-or-Folder) | resolved object | **absent** (raw folder ID instead) |
| NewsCollection | `newsCollectionInitialData` | resolved object | **absent** |
| Campaign Hero | `singleStylingBackgroundImage` | resolved object | unresolved |

**Reproducibility**: Reliable given the environment above; the capture shows it recurring
multiple times within a single `EDIT_MODE` load. One specific trigger identified: the client's
GraphQL request registers (`requestMetadata` set) and then aborts or fails mid-flight (observed
as `net::ERR_ABORTED` during a mode switch) — in that state the stale REST asset stays in the
store and the current gate cannot tell it apart from a resolved GraphQL asset.

## Scope of Investigation *(mandatory)*

- **Affected area**: UVE (Universal Visual Editor) headless page-preview messaging — the
  `postMessage` bridge that pushes page data into a headless client's iframe during
  `EDIT_MODE`.
- **Suspected surface**: Modern `com.dotcms.*`-equivalent — this is entirely frontend
  (Angular/NgRx Signal Store) code in `core-web`, not backend Java. No `com.dotmarketing.*`
  legacy surface is implicated. (The REST page-JSON shape limitation described below is
  existing, unchanged backend behavior — see Non-Goals — not part of the fix surface.)
- **Related known decisions**: None known locally; the plan phase's mandatory ADR consultation
  (`/speckit-adr-context` against `dotCMS/platform-adrs`) will confirm whether any ADR governs
  the UVE messaging protocol or the headless/GraphQL preview architecture.

## Root-Cause Hypothesis

`edit-ema-editor.component.ts` gates the iframe push on `pageType === HEADLESS && !hasClientQuery`
(skip only when `requestMetadata` is empty). This asks whether the client's GraphQL query has
been *registered*, not whether the payload about to be sent actually *came from* it — the two
are not equivalent:

1. `CLIENT_READY` arrives → the actions-handler service sets `requestMetadata`, fires an async
   GraphQL fetch, and sets `isClientReady = true`, all within one synchronous block
   (`dot-uve-actions-handler.service.ts:293-328`).
2. The store's `pageAsset()` selector (`withPage.ts:232`) depends on `requestMetadata()` and
   returns a new object literal on every recomputation. The reload effect's custom `equal`
   compares the page-asset reference (`withEditor.ts:266`), so step 1 alone causes it to
   re-emit.
3. The effect re-runs: `isClientReady` is `true` and `hasClientQuery` is `true` — both gates
   pass. But the stored page-asset response is still the REST asset, because the loading-state
   transition deliberately preserves it while the GraphQL fetch is still in flight.
4. The iframe push then sends the REST asset, merely tagged with `requestMetadata`.

Once `requestMetadata` is set, it is never re-checked for provenance, so the gate stays open
for any later re-emit — including the case where the GraphQL fetch subsequently fails
(`withPageApi.ts`'s error handling sets `uveStatus` to `ERROR` and returns without updating the
stored page asset, leaving the stale REST asset in place with no signal that it never resolved).

`depth` (the REST page-JSON relationship-expansion parameter) is not a viable fix path:
`depth=0` returns bare identifier strings, not one level of related objects as prior work
assumed; and REST page JSON has no representation for Site-or-Folder or Category fields at any
depth, since those are expanded only via dedicated GraphQL data fetchers with no REST
equivalent. No value of `depth` can make a REST payload match a GraphQL query.

Provenance is tracked as a field on the stored page-asset state itself — a `source: 'rest' |
'graphql'` tag set wherever the asset is written — rather than derived from a separate flag.
The tag travels with the data it describes, so any code holding the asset can answer "where did
this come from" without consulting other state. The plan phase works out exactly where that tag
is set and read.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Track provenance (REST vs. GraphQL) on the stored page-asset response itself, rather than
  inferring it from whether `requestMetadata` is set.
- Change the headless push gate so it keys on "is there a GraphQL-sourced asset for the current
  page" — if not, send `UVE_RELOAD_PAGE` instead of any page data, in every state where the
  stored asset is REST-sourced (never registered, registered-but-unresolved, and
  registered-then-aborted/failed).
- Audit the other four known `UVE_SET_PAGE_DATA` senders (`uve-optimistic-save.service.ts`,
  two sites in `withPageApi.ts`, `withSave.ts`) and confirm none can deliver a REST-sourced
  asset to a headless client; fix any that can.
- Ensure post-edit flows (drag/drop, add, remove, save) on a headless page with a registered
  query continue to reflect the change via GraphQL-sourced data after the fix.
- Regression coverage confirming traditional (non-headless) pages are unaffected.

**Explicitly out of scope / non-goals**:

- Relationship fields resolving to zero related items (`heroImage`, `heroVideo`,
  `heroImageMobileRendition`, `featuredLinksViewAll` returning `[]` in the GraphQL/client
  payload itself) — a separate, already-suspected single-cardinality/language-fallback bug,
  unrelated to REST-vs-GraphQL provenance. Needs its own issue once root-caused.
- REST/GraphQL shape parity for `HostFolderField` and Category fields. This fix ensures no
  REST-shaped payload reaches a headless client at all — it does **not** add these fields to
  the REST page-JSON shape. That parity gap remains open and out of scope.
- A Category field (`newsCollectionCategoryFilter`) returning *different* resolved data (not
  merely unresolved data) between the client payload and a would-be pushed payload — a distinct
  defect, not fixed by provenance gating.
- Any change to the REST page-JSON endpoint (`/api/v1/page/json`) itself, or to its `depth`
  parameter semantics.

## Regression Risk *(mandatory)*

- **Blast radius**: The reload/push effect in `edit-ema-editor.component.ts` and the signal
  store composables it depends on (`withPage`, `withEditor`, `withPageApi`, `withSave`) are
  shared by both traditional and headless UVE page rendering. Any change to the gate must leave
  the traditional-page path (which exits before the headless branch today) untouched.
- **Backward compatibility**: This is frontend-only, in-memory store state — no persisted data,
  API contract, or DB/ES mapping is touched. No rollback-unsafe change is anticipated, but the
  plan phase should confirm nothing in the postMessage protocol (`UVE_SET_PAGE_DATA`,
  `UVE_RELOAD_PAGE`) changes shape in a way older SDK client versions wouldn't tolerate.
- **Data considerations**: None — no stored/persisted data requires migration or repair.

## Acceptance & Verification *(mandatory)*

- **AC-001**: UVE never sends `UVE_SET_PAGE_DATA` to a `HEADLESS` client carrying a `pageAsset`
  sourced from `/api/v1/page/json`. Only assets sourced from the client's registered GraphQL
  request are pushed.
- **AC-002**: Provenance is tracked on the stored page-asset response itself (e.g. the asset
  records its own source) rather than inferred from whether `requestMetadata` is set.
- **AC-003**: The reported repro no longer reproduces for **both** observed failure patterns:
  (a) a relationship field flattened from a resolved object to a bare identifier string (e.g.
  `newsCollectionTopicLinks`), and (b) a field present in the client's GraphQL payload and
  **absent entirely** from the REST shape (e.g. `newsCollectionRootPath`).
- **AC-004 (edge case)**: The reload fallback (`UVE_RELOAD_PAGE`) is keyed on the *absence of a
  GraphQL-sourced asset for the current page*, not on whether `requestMetadata` is set. This
  holds across all four rows of the state matrix below.
- **AC-005**: After an author edit (drag/drop, add, remove, save) on a headless page with a
  registered query, the preview reflects the change using GraphQL-sourced data.
- **AC-006 (regression)**: Traditional (non-headless) pages are unchanged — they continue to
  receive server-rendered REST content and exit the reload effect before the headless branch,
  exactly as today.
- **AC-007**: The four other `UVE_SET_PAGE_DATA` senders (`uve-optimistic-save.service.ts:54`,
  `withPageApi.ts:566`, `withPageApi.ts:599`, `withSave.ts:130`) are audited and confirmed
  unable to deliver a REST-sourced asset to a headless client.

**State matrix (must hold for AC-001 and AC-004):**

| `requestMetadata` | Stored asset | Expected behavior |
|---|---|---|
| Never registered (`CLIENT_READY` never fired) | REST | No page-data push; send `UVE_RELOAD_PAGE` |
| Registered, GraphQL fetch not yet resolved | REST | No page-data push; send `UVE_RELOAD_PAGE` |
| Registered, then the client's GraphQL request aborted or failed | REST (stale) | No page-data push; send `UVE_RELOAD_PAGE` |
| Registered, GraphQL fetch resolved | GraphQL | Push the GraphQL-sourced asset |

Rows 2 and 3 are the states the shipped #36410 gate cannot distinguish (`requestMetadata` is
non-null in both), so the push proceeds incorrectly today. Row 3 is the state captured by the
reporter.

- **Verification method**:
  - A Jest/Spectator spec asserting no `UVE_SET_PAGE_DATA` carrying a REST-sourced asset is
    emitted for a `HEADLESS` page, including the case where `requestMetadata` is set but the
    stored asset came from REST (covers AC-001, AC-003, AC-004, all four state-matrix rows).
  - A Jest/Spectator spec asserting `UVE_RELOAD_PAGE` is emitted for a `HEADLESS` page with no
    registered GraphQL request.
  - A Jest/Spectator spec covering the post-edit flow (AC-005) confirming the preview reflects
    GraphQL-sourced data after a drag/drop, add, remove, or save on a headless page.
  - Existing traditional-page specs continue passing unmodified (AC-006).
  - Manual verification against the original customer repro (or an equivalent local headless
    front end using `@dotcms/client`'s GraphQL mode) confirming zero REST-shaped
    `UVE_SET_PAGE_DATA` pushes across a full `EDIT_MODE` load, matching the "Steps to
    Reproduce" above.

## Assumptions

- The four additional `UVE_SET_PAGE_DATA` senders named in AC-007 are assumed, per the issue
  report, to be the complete set of senders outside the primary reload effect; the plan phase
  should re-confirm this is exhaustive against current `main` before implementation.
- "GraphQL-sourced" is assumed to mean the asset currently held in the store was produced by
  resolving the client's registered GraphQL request for the *current* page — a stale
  GraphQL-sourced asset from a previous page navigation is treated as equivalent to "no
  GraphQL-sourced asset for the current page" and should trigger the reload fallback, not a
  push. The plan phase should confirm this interpretation.
- No new persisted or backend state is introduced; provenance tracking is assumed to be
  achievable entirely within existing frontend store state.
