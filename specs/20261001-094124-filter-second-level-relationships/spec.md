# Issue Resolution Specification: Enhancing Relationship PP Filtering: Adding Second-Level Related Content Option

**Feature Branch**: `20261001-094124-filter-second-level-relationships`

**Created**: 2026-10-01

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#31400](https://github.com/dotCMS/core/issues/31400)

**Input**: User description: "https://github.com/dotCMS/core/issues/31400 -- check on the mentioned and related issues also to get the whole picture"

<!--
  This is the dotCMS ISSUE-RESOLUTION spec (used by /speckit-specify-fix). Unlike the
  feature spec, it is framed around a defect: what is wrong, how to reproduce it, and how
  we will know it is fixed. It still flows into /speckit-plan, where the Legacy Impact and
  ADR Alignment gates apply. Keep this technology-light — root-cause and fix details are
  refined in the plan.
-->

## Problem Statement *(mandatory)*

Push Publishing (PP) bundles a Contentlet together with its dependencies, including related
content reached through Relationship fields. Today that traversal only ever reaches **one level
deep**: if Contentlet A relates to B, and B relates to C, pushing A includes B but never C — even
when the "Everything and Dependencies" filter (the filter whose own description promises to
resolve "any dependency ... may need") is selected. Customers who rely on multi-level content
graphs (e.g. a Page → Banner → Asset chain) receive incomplete bundles: content renders broken or
missing on the receiving environment because an indirectly-related piece of content never arrived.

This is not a new complaint — it is the third time this exact symptom has been reported
(see Reproduction and the chain of related issues below), and a previous full fix for it was
shipped and then reverted for over-correcting. This spec intentionally reframes the fix as an
**opt-in filter option** rather than a change to existing filter behavior, to avoid repeating that
regression.

**Severity / Impact**: Priority 2 (Important). Affects any back-end user pushing content that sits
two or more Relationship-field hops away from the pushed asset, using any PP filter. Per the
issue's linked support tickets (Freshdesk #29642, Helpdesk #29668, Freshdesk #39487), this has
recurred across multiple customer environments.

## Reproduction *(mandatory)*

**Environment**: Latest `main` (also reproducible on prior releases — see "Related known
decisions" below for version history).

**Steps to Reproduce** (from the issue, content types renamed to match the issue's A/B/C example):

1. Create Content Type `C` with a single Text field `title`.
2. Create Content Type `B` with a Text field `title` and a relationship field `rel` pointing to `C`.
3. Create Content Type `A` with a Text field `title` and a relationship field `rel` pointing to `B`.
4. Create a Contentlet for `C`, named `c1`.
5. Create a Contentlet for `B`, named `b1`, related to `c1`.
6. Create a Contentlet for `A`, named `a1` (not related to `c1` directly — only transitively via `b1`).
7. Create a bundle, add `a1`, and generate it using the "Everything and Dependencies" filter (the
   issue notes this reproduces with any filter that has `relationships` and `dependencies` enabled).
8. Inspect the bundle's manifest.

**Expected Behavior**: The acceptance criteria for this issue is that `c1` is excluded from the
bundle when using "Everything and Dependencies" (that filter's behavior must stay exactly as it is
today), **and** that a new, separate PP filter exists which a user can deliberately choose to have
`c1` (and deeper transitive relations) included.

**Actual Behavior**: `c1` IS currently included in the manifest even under "Everything and
Dependencies" — i.e., the codebase is presently in the "too many items" state the sibling issue
#31036/PR #31412 round-trip was trying to avoid (see below). There is no filter today — new or
existing — that lets a user explicitly opt in to second-level traversal.

**Reproducibility**: Always, given any relationship chain of depth ≥ 2.

## Scope of Investigation *(mandatory)*

- **Affected area**: Push Publishing (PP) bundle dependency resolution — specifically, how
  Relationship-field dependencies are walked when a bundle is generated, and how PP Filters
  (`.yml` descriptors selectable in the PP UI) gate that behavior.
- **Suspected surface**: Modern (`com.dotcms.*`). The relevant traversal lives in
  `com.dotcms.publisher.util.dependencies.PushPublishigDependencyProcesor` and the filter model in
  `com.dotcms.publishing.PublisherFilter` / `PublisherFilterImpl` / `FilterDescriptor`, with filter
  definitions as YAML under `dotCMS/src/main/webapp/WEB-INF/publishing-filters/`. No legacy
  (`com.dotmarketing.*`) changes are anticipated; `com.dotmarketing.business.RelationshipAPI` is
  only read, not modified.
- **Related known decisions — full issue chain**:
  - **#29031** (closed) — "Spike: Determine solution for Push Publishing dependency failures due
    to depth." The original report: pushing a parent only carried one level of related content: a
    grandchild relation ("toBeRelated") never arrived.
  - **#31036** (closed, fixed by PR #31054, merged 2025-01-06, QA-passed) — Reproduced the same
    one-level-only limitation with an explicit A→B→C example. Its acceptance criteria explicitly
    states the team **decided to send all related content regardless of its level**, and
    deliberately **did not add a depth-config property**, relying instead on the dependency
    processor's existing "each asset is processed only once" de-duplication to make unbounded
    recursion safe against circular relationships (verified with a dedicated cyclic-reference
    integration test).
  - **PR #31412** (merged 2025-02-19) — Reverted PR #31054 wholesale: "Some customers reported
    that after change #31036, their Push actions included too many items." The revert PR body
    explicitly states a new card was opened to find "a better solution to the original issue" and
    names that card as **this issue, #31400**.
  - **#31400** (this issue, open) — Asks for the #31036/#31054 capability (unbounded relationship
    depth) to come back, but **gated behind a new, separate, opt-in PP filter**, while the
    "Everything and Dependencies" filter keeps today's (post-revert) shallow, single-level
    behavior unchanged. No PR currently targets #31400.
  - Net effect: the *mechanism* needed (recurse into each related Contentlet's own dependencies
    instead of just listing it) was already built, tested for cycle-safety, and shipped once. What
    caused the revert was applying it unconditionally to the default filter, not the mechanism
    itself being wrong.

## Root-Cause Hypothesis

When PP walks a Contentlet's relationships
(`PushPublishigDependencyProcesor.processContentDependency`), it adds each directly related
Contentlet to the bundle via a method that only places it in the manifest — it does **not** also
enqueue that related Contentlet for its own dependency processing. As a result, a related
Contentlet's *own* relationships (the second level, e.g. `b1`'s relation to `c1`) are never
walked. The codebase already contains a second method that does both — add to the manifest *and*
enqueue for recursive processing — and PR #31054 proved it is cycle-safe; today it is simply never
used for this call site. No `[NEEDS CLARIFICATION]` on the mechanism itself — it is directly
confirmed by the merged-then-reverted PR #31054/#31412 pair.

One hypothesis needed developer confirmation before planning could lock in an approach, and has
now been resolved:

**Resolved (2026-10-01)**: The new opt-in filter's relationship traversal is **capped at exactly
one extra level** — pushing A includes B (first level, already works today) and C (second level,
today's bug), but NOT any further level (e.g. D, if C also relates to D). This matches the issue's
literal "Second-Level Related Content" title and its single A→B→C reproduction example. This is a
deliberate departure from PR #31054's prior (unbounded-depth) implementation: that mechanism
recursed through every level with no stopping point, which is one plausible reason customers saw
"too many items" after it shipped as the default. Capping at a fixed two levels means the plan
must add new depth-tracking to the traversal (counting hops from the originally pushed asset),
not merely reuse #31054's existing "recurse forever" method unconditionally.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Add one new, named PP filter (non-default) that, when selected, includes related content
  exactly one level beyond what already works today — i.e., second-level related content such as
  `c1` in the reproduction steps.
- Add the depth-tracking needed to stop at that second level (no existing code path does this
  today — #31054's prior mechanism recursed unconditionally, which is explicitly not what this fix
  does).
- Leave every existing PP filter — most importantly "Everything and Dependencies"
  (`Intelligent.yml`) — behaved exactly as it does today (first-level relationships only).
- Cover both halves with an automated regression test: the new filter includes second-level
  content but stops there; the existing default filter(s) continue to exclude it entirely.

**Explicitly out of scope / non-goals**:

- Changing the traversal behavior of any *existing* filter (`Intelligent.yml`, `ShallowPush.yml`,
  `ContentOnly.yml`, `ForcePush.yml`, `WebContentOnly.yml`) — their manifests must be byte-for-byte
  unchanged by this fix.
- A configurable or unbounded depth (e.g. "traverse N levels" or "traverse everything") — the
  resolved decision is a fixed two-level cap, not a general depth-configuration feature. A future
  issue could extend this to N levels, but that is explicitly not part of this fix.
- Any change to how Relationships themselves are stored, queried, or resolved
  (`RelationshipAPI`) — this is purely about which already-discoverable related Contentlets get
  added to a bundle.
- Front-end/UI work beyond the new filter appearing automatically in the existing PP filter
  dropdown (which already lists filters discovered from the YAML descriptors directory) — no new
  UI components are anticipated.

## Regression Risk *(mandatory)*

- **Blast radius**: Confined to Push Publishing bundle generation
  (`PushPublishigDependencyProcesor`, `PublisherFilter`/`PublisherFilterImpl`,
  `FilterDescriptor`, and the PP filter YAML directory). No other subsystem reads these classes.
  Existing filters are unaffected as long as the new behavior is strictly additive and
  defaults to "off" for any filter that doesn't explicitly opt in.
- **Backward compatibility**: `PublisherFilter` is a public interface; if the plan adds a new
  method to it, it must be a `default` method (or otherwise additive) so any out-of-repo
  implementers (e.g. OSGi plugins) do not fail to compile/load. The new YAML filter key must
  default to the pre-existing (shallow) behavior when absent, so every filter descriptor written
  before this fix continues to behave identically.
- **Data considerations**: None — no stored content, DB schema, or ES mapping changes. This only
  affects what gets bundled into a `.tar.gz` push at generation time, which is already
  transient, generated artifact, not persisted state.

## Acceptance & Verification *(mandatory)*

- **AC-001**: When the new opt-in filter is selected, pushing `a1` (per the Reproduction steps)
  produces a bundle manifest that includes `c1` (second-level related content).
- **AC-002**: When "Everything and Dependencies" (or any other existing filter) is selected,
  pushing `a1` produces a bundle manifest that **excludes** `c1` — i.e., today's shallow behavior
  is provably unchanged. This is the regression check for the #31036 → #31412 round-trip.
- **AC-003**: Extending the chain to a third level (A→B→C→D) and pushing `a1` with the new opt-in
  filter produces a bundle manifest that includes `c1` but **excludes** `D` — proving the cap
  stops at exactly two levels and does not silently fall back to #31054's unbounded recursion.
- **AC-004**: Pushing content with a circular relationship chain (A→B→C→A) under the new opt-in
  filter completes without an OutOfMemory error or infinite loop (regression guard matching
  PR #31054's original cyclic-reference coverage, now re-verified under the depth-capped logic).
- **Verification method**: New/updated integration tests in
  `dotcms-integration/src/test/java/com/dotcms/publisher/util/DependencyManagerTest.java` (and/or
  `com.dotcms.publishing.PublisherAPIImplTest`), run via
  `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=DependencyManagerTest -Dmaven.build.cache.enabled=false`
  (confirm `Tests run: N` in `target/failsafe-reports/*.txt` rather than trusting exit code, per
  this repo's known build-cache short-circuiting of failsafe).

## Assumptions

- The new filter's YAML file name, display title, and sort position in the PP filter dropdown are
  implementation details to be finalized during planning/implementation, not product decisions
  that block this spec — any reasonable, descriptive naming is acceptable absent other guidance.
- The existing "each asset is processed only once" de-duplication in
  `PushPublishigDependencyProcesor` (already proven by PR #31054's cyclic-reference test) is
  assumed to still be present and sufficient to make recursive traversal cycle-safe; the plan
  should confirm this rather than re-derive it from scratch.
- No ADR is known to govern PP filter depth semantics; the plan phase will confirm this via
  `/speckit-adr-context` against `dotCMS/platform-adrs`.
