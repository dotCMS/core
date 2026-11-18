# Issue Resolution Specification: Bulk-content-removal safeguard counts archived contentlets, blocking legitimate container operations

**Feature Branch**: `issue-37377-archived-contentlets-net-loss-guard`

**Created**: 2026-09-17

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37377](https://github.com/dotCMS/core/issues/37377)

**Input**: User description: "We have to fix the bulk-removal safeguard's contentlet count to only consider non-archived content. Not sure if a spec is really needed here because this is a simple fix in theory, but let's check if that assumption is right. Important items for the spec: we should have some test in place for the fix, also we should make sure that we don't break existing functionality (regression)."

## Problem Statement *(mandatory)*

dotCMS has a "net loss" safeguard that rejects a page-content save when the save would remove
more contentlets from a page than a configured threshold allows. Its purpose is to catch stale
clients and buggy callers that would silently wipe a page's content.

The safeguard decides how much content a save would remove by counting the contentlets the page
currently holds. That count includes contentlets in **every** state — including **archived**
ones (`contentlet_version_info.deleted = true`).

An archived contentlet can still leave a stale row in `multi_tree` (for example, a row created
before archiving was changed to automatically remove content from its container). That stale row
is not live content and is not rendered on the page, but the safeguard counts it. The count is
therefore inflated, the computed "net loss" is larger than the real loss, and the safeguard
rejects a save that a user is legitimately allowed to make. The API returns HTTP 409 Conflict
and tells the user to refresh — which never helps, because the stale row is still there on the
next attempt.

**Severity / Impact**: Medium. Low frequency — it requires (a) an affected page to already hold
a stale archived-contentlet reference in `multi_tree`, **and** (b) the safeguard to be switched
on, since it is disabled by default (`MULTITREE_NET_LOSS_THRESHOLD = -1`). When both hold, the
affected page edit is fully blocked for every editor, with no in-product workaround other than a
DBA manually deleting the stale `multi_tree` row. Reported via Freshdesk ticket 39004.

## Reproduction *(mandatory)*

**Environment**: Any dotCMS build that contains both the net-loss safeguard
(`MultiTreeAPIImpl.overridesMultitreesByPersonalization`, added in #36098) and the
archive-removes-from-container behavior. **The safeguard must be enabled** — the deployment must
set `MULTITREE_NET_LOSS_THRESHOLD` to `0` or greater; with the shipped default of `-1` the
safeguard never rejects a save and the bug is latent. Postgres backend; page editor (UVE) or the
equivalent `PageResource` save endpoint.

**Steps to Reproduce**:

1. Set `MULTITREE_NET_LOSS_THRESHOLD=1` (or any value `>= 0`).
2. Take a page with a container holding N live contentlets, plus at least two `multi_tree` rows
   whose child identifier is **archived** (`contentlet_version_info.deleted = true`) for the
   language/variant being edited — i.e. stale references that predate the archive-removes-from-
   container behavior.
3. In the page editor, make a single ordinary edit to that page (add, remove, or reorder one
   contentlet) and save. The client submits the page's live content only; the archived
   identifiers are not in the payload.
4. **Expected Behavior**: The safeguard counts only non-archived content, computes a net loss of
   at most 1, stays under the threshold, and the save succeeds.
5. **Actual Behavior**: The safeguard counts the archived rows as existing content. The computed
   net loss is inflated by the number of stale archived rows, exceeds the threshold, and the save
   is rejected with `StalePageSaveException` → HTTP 409 Conflict ("Save rejected: net content
   loss exceeds the configured threshold. Please refresh and try again."). Refreshing does not
   help; the block is permanent for that page.

**Reproducibility**: Always, given the required data state (stale archived `multi_tree` row) and
a threshold small enough that the extra archived rows push the net loss over it.

## Scope of Investigation *(mandatory)*

- **Affected area**: Content management — page/container content persistence (`multi_tree`),
  reached from the page editor (UVE) and the `/api/v1/page` save endpoints.
- **Suspected surface**: **Legacy** — `com.dotmarketing.factories.MultiTreeAPIImpl`
  (the safeguard block in `overridesMultitreesByPersonalization` and the private
  `getOriginalContentlets(...)` overloads plus their `SELECT_CHILD_BY_PARENT*` SQL constants).
  The caller-side 409 mapping lives in modern code (`com.dotcms.rest.api.v1.page.PageResource`)
  and is expected to be untouched. Confirmed during planning.
- **Related known decisions**: The safeguard itself was introduced deliberately (PR #36098) to
  stop empty/stale payloads wiping page content; this fix must not weaken that intent. The plan
  formally consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

The safeguard's "existing content" count comes from `getOriginalContentlets(...)`, whose SQL
(`SELECT_CHILD_BY_PARENT`, `..._VARIANT`, `..._VARIANT_LANGUAGE`, `..._TWO_LANGUAGES`) selects
`multi_tree.child` and, where a language is involved, joins only the `contentlet` table to filter
by `language_id`. Nothing in that path consults `contentlet_version_info.deleted`, so an archived
identifier with a surviving `multi_tree` row is counted as existing page content. The hypothesis
is that adding a non-archived predicate (an `EXISTS` / join against `contentlet_version_info`
with `deleted = false` on the matching identifier, language and variant) to the count used by the
safeguard is sufficient to fix the reported behavior.

**This is not purely a one-line change**, and that is the main reason this spec exists:

1. `getOriginalContentlets(...)` is **not** exclusive to the safeguard. Its four private
   overloads have three call sites, only one of which is the guard:

   | Call site | Result feeds | Effect of filtering the shared helper |
   |---|---|---|
   | `overridesMultitreesByPersonalization` (guard block) | the safeguard's `existing` count | the intended fix |
   | `overridesMultitreesByPersonalization` (post-guard) | `originalContentletIds` → `refreshContentletReferenceCount(...)` | archived IDs no longer invalidated → stale cached reference count |
   | `saveMultiTrees` | `originalContents` → same refresh | same, on a path that has **no** safeguard and is unrelated to this issue |

   `refreshContentletReferenceCount(...)` only calls
   `multiTreeCache.removeContentletReferenceCount(id)` — it invalidates a cache entry and
   deletes nothing, so the failure mode is a stale "referenced by N pages" count for archived
   content, not content loss. The plan must decide whether to filter inside the shared helper
   (and then also handle both refresh call sites deliberately) or introduce a count used only by
   the safeguard, and justify the choice. Note `saveMultiTrees` uses the 2-arg overload backed by
   `SELECT_CHILD_BY_PARENT`, the same constant the other overloads are built from — changing that
   constant in place changes `saveMultiTrees` too.
2. The safeguard is documented in-code as deliberately mirroring the downstream DELETE branching
   so that "the guard counts the same rows that will be removed." The DELETE statements do
   **not** exclude archived content, so after this fix the guard and the DELETE intentionally
   diverge: the stale archived row is still deleted (it is not in the incoming payload) but is no
   longer counted as a loss. That divergence is correct for this issue but must be stated and the
   stale in-code comment updated, or the next reader will "fix" it back.
3. The predicate must be applied consistently across **all four** count paths (no-language,
   variant-only, single-language, and the `DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE` two-language
   pair), and must key on identifier **plus** language **plus** variant, since
   `contentlet_version_info` is keyed `(identifier, lang, variant_id)` — a contentlet archived in
   one language/variant is not archived in another.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- The contentlet count that feeds the net-loss safeguard in
  `MultiTreeAPIImpl.overridesMultitreesByPersonalization` counts only non-archived
  (`contentlet_version_info.deleted = false`) content, for the identifier/language/variant
  actually in play, across all four language-scoping branches.
- The empty-payload warning log emitted from the same block reports the same non-archived count,
  so the log does not contradict the decision.
- Updating the in-code comments that assert the guard mirrors the DELETE row-for-row.
- Automated test coverage for both the fix and the safeguard's original behavior (see
  Acceptance & Verification).

**Explicitly out of scope / non-goals**:

- Changing which rows the save's DELETE statements remove, or any other `multi_tree` read/write
  path beyond the safeguard's count.
- Changing `refreshContentletReferenceCount` semantics or the contentlet reference-count cache
  behavior for archived content.
- Cleaning up pre-existing stale `multi_tree` rows (no data migration / upgrade task).
- Changing the safeguard's default (`MULTITREE_NET_LOSS_THRESHOLD = -1`), its threshold
  semantics, the `StalePageSaveException` type, or the HTTP 409 mapping in `PageResource`.
- Revisiting the archive-removes-from-container behavior or
  `DELETE_ORPHANED_CONTENTS_FROM_CONTAINER`.
- Any frontend/UVE change.

## Regression Risk *(mandatory)*

- **Blast radius**: `overridesMultitreesByPersonalization` is the single write path for page
  content from the page editor and the page REST API, so a mistake here affects **all** page
  saves, not just the reported edge case.

  **What is structurally safe**: the removal of `multi_tree` rows is performed by independent
  DELETE statements (`deleteEntriesByRequestedLanguage`,
  `deleteEntriesByRequestedAndDefaultLanguage`, `DELETE_ALL_MULTI_TREE_SQL*`) that never call
  `getOriginalContentlets(...)`. Changing the count therefore **cannot** change which content is
  saved to or removed from a page. Content loss is not a plausible outcome of this fix.

  **What is genuinely at risk**, in order of likelihood:
  1. **Reference-count cache staleness.** Applying the non-archived filter inside the shared
     `getOriginalContentlets(...)` helper also narrows `originalContentletIds` /
     `originalContents`, so `refreshContentletReferenceCount(...)` stops invalidating the cached
     reference count of archived contentlets whose rows were just removed. Symptom: a stale
     "referenced by N pages" figure on archived content.
  2. **Collateral change to `saveMultiTrees`.** That method is a second public entry point
     (reached via `MultiTreeFactory.saveMultiTrees`), carries no safeguard, and shares the 2-arg
     overload backed by `SELECT_CHILD_BY_PARENT` — the constant the other three overloads are
     composed from. Editing that constant in place silently changes a path outside this issue's
     scope.
  3. **An over-broad predicate** (ignoring variant, or requiring a *live* rather than merely
     non-archived version) would under-count real content and **disable** the safeguard this fix
     is meant to refine.
  4. **Query cost**: the added predicate runs on every guarded save, so it must stay
     index-friendly on `contentlet_version_info (identifier, lang, variant_id)`.
- **Backward compatibility**: No API contract, DB schema, or serialized-state change. Read-side
  SQL only. Not rollback-unsafe: reverting the change restores the previous (over-counting)
  behavior with no data left behind. No `openapi.yaml` regeneration expected.
- **Data considerations**: Existing stale `multi_tree` rows are neither repaired nor migrated by
  this fix; they simply stop inflating the safeguard's count, and are removed by the ordinary
  save DELETE as they are today. No upgrade task.

## Acceptance & Verification *(mandatory)*

- **AC-001**: With `MULTITREE_NET_LOSS_THRESHOLD` set to a value `>= 0`, a page whose container
  holds stale `multi_tree` rows referencing archived contentlets accepts an ordinary save whose
  real net loss is within the threshold — the archived rows contribute nothing to the count and
  no `StalePageSaveException` is thrown.
- **AC-002** (regression — the safeguard still works): With the same threshold, a save whose net
  loss of **non-archived** content exceeds the threshold is still rejected with
  `StalePageSaveException`; an empty payload against a page holding non-archived content is still
  rejected; and with the default `-1` the safeguard still never rejects a save.
- **AC-003** (regression — scoping is respected): A contentlet archived in language A but not in
  language B is still counted when the page is saved in language B, and under
  `DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE=true` the two-language count applies the same non-archived
  rule to both the requested and the default language. Variant-scoped saves count only the
  variant's own archived state.
- **AC-004** (regression — no collateral change): Page saves that involve no archived content
  behave exactly as before. Specifically: the set of `multi_tree` rows written and removed by a
  save is byte-for-byte unchanged from current behavior in every language/variant branch; the
  contentlet reference counts invalidated by `refreshContentletReferenceCount(...)` after a save
  are unchanged for non-archived content; and `saveMultiTrees` (the second, unguarded entry point
  sharing the same helper) behaves exactly as before, whether or not archived content is
  involved.
- **Verification method**:
  - New integration tests in
    `dotcms-integration/src/test/java/com/dotmarketing/factories/MultiTreeAPITest.java`, beside
    the existing `test_overridesMultitrees_*NetLossThreshold*` tests (which already cover
    threshold `0` + empty payload, threshold `-1` disabled, excessive drop, small drop) — at
    minimum: archived contentlet not counted (AC-001), archived-in-another-language still counted
    (AC-003).
  - Run: `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dmaven.build.cache.enabled=false -Dit.test=MultiTreeAPITest`,
    confirming `Tests run: N` in `target/failsafe-reports/*.txt` rather than trusting the exit
    code.
  - Confirm `MultiTreeAPITest` is registered in the relevant `MainSuite*`/`Junit5Suite*` so the
    new tests actually run in CI.
  - Per Constitution Principle V, these tests are written, dev-approved, and confirmed **failing
    (Red)** before the fix is implemented.

## Assumptions

- The issue's "bulk-removal safeguard" is the `MULTITREE_NET_LOSS_THRESHOLD` net-loss guard in
  `MultiTreeAPIImpl.overridesMultitreesByPersonalization`; no second safeguard with a separate
  contentlet count is in scope.
- "Archived" means `contentlet_version_info.deleted = true` for the matching identifier, language
  and variant — not "has no live version". Content that is merely unpublished (working-only, not
  archived) must still be counted.
- The reporting environment has `MULTITREE_NET_LOSS_THRESHOLD` explicitly configured `>= 0`;
  otherwise the reported block could not occur. Confirming the ticket's actual configured value
  is useful but does not change the fix.
- Postgres only (the codebase is documented as PostgreSQL-exclusive), so the SQL may use
  Postgres-specific constructs consistent with the surrounding statements.
