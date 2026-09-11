# Feature Specification: Content Drive — Title / All Content scope selector for the search box

**Feature Branch**: `issue-37479-content-drive-search-scope`

**Created**: 2026-09-11

**Status**: Draft

**Type**: New Feature

**Related GitHub Issue**: [#37479](https://github.com/dotCMS/core/issues/37479) — related history: [#36688](https://github.com/dotCMS/core/issues/36688) (replaced the broad `catchall:*kw*` wildcard with the current strategy), [#36814](https://github.com/dotCMS/core/issues/36814) (search performance at scale)

**Input**: User description: "Content Drive: add a Title / All Content scope selector to the search box. Default scope = All Content (no regression); Title mode matches strictly the contentlet title plus folder names, not fileName/metadata.name; scope persists only in the URL filters; sorting unchanged."

---

## Premise Corrections

Four things the issue takes for granted do not survive verification against `main`. Three of them
narrow the work; the fourth adds a rule the issue's file list has no place for.

### 1. Results are **not** sorted by score when a term is present

Open decision 4 asks to *"confirm that score-descending sorting holds in Title mode."* There is no
score sorting to hold. The drive sends whatever sort the grid holds, and its default is
modification date:

- `core-web/.../dot-content-drive/portlet/src/lib/shared/constants.ts:51-54` — `DEFAULT_SORT = { field: 'modDate', order: DESC }`
- `.../store/dot-content-drive.store.ts:157` — `sortBy: sort()?.field + ':' + sort()?.order`, unconditionally
- `AbstractDriveRequestForm.java:81` — the server default is `SORT_BY = "modDate"` too

The only trace of score sorting is a **stale comment** at `dot-content-drive.store.ts:481`
("Since we are using scored search for the title we need to sort by score desc") sitting above code
that does nothing of the kind. Nothing in `BrowserAPIImpl` or `ContentDriveHelper` substitutes a
score sort when a filter is set.

**Consequence for this feature**: open decision 4 is void. Sorting is out of scope entirely — both
scopes keep sending the grid's sort, and neither introduces a scope-dependent sort rule. The stale
comment should be corrected while the file is open (progressive enhancement), not acted upon.

### 2. Folders and links are already matched on name only, in both scopes

The issue frames "match against the contentlet title **(and folder/file names)** only" as new Title
behavior. Folders and links never reach Elasticsearch at all — they are loaded from the database and
narrowed in Java by a case-insensitive substring on their own name:

- `BrowserAPIImpl.java:3026` — `folders.removeIf(f -> !f.getName().toLowerCase().contains(browserQuery.filter.toLowerCase()))`, gated on `filterFolderNames`
- `BrowserAPIImpl.java:2908-2913` — the equivalent for links, on `Link::getTitle`, deliberately **not** gated on that flag

**Consequence for this feature**: folder and link matching is scope-independent and must not change.
The scope selector governs only the Elasticsearch clause used for **contentlets**. An acceptance
criterion that reads "Title mode returns only rows whose title or folder name matches" is already
half-true today and stays true in All Content mode.

### 3. `buildPureESQuery` is unreachable for Content Drive under shipped configuration

The issue flags the older `title:'*x*'^5 OR catchall:*x*^3 OR fileName:*x*^2` shape in
`buildPureESQuery` (~line 610) as *"worth confirming whether the drive reaches it"*. It does not:
`doPureESQuery` is selected only when `BROWSE_API_HEURISTIC_TYPE` is set to `PURE_ES`, and the
shipped default is `HYBRID_SINGLE_CHUNKED_QUERY_ES` (`BrowserAPIImpl.java:701-709`), which routes to
`buildBaseESQuery`.

**Consequence for this feature**: `buildPureESQuery` is **out of scope**. Changing it would mean
changing a code path no shipped configuration exercises, and doing so unverified is worse than
leaving it consistent with its own heuristic.

### 4. Storing the scope as a filter would offer "Clear all" on an unfiltered drive

The issue's file list routes the scope through the drive's filter state — which is what carries it
into the address — but stops there. That state also feeds the filter chip bar, and
`hasNonDefaultFilters` (`utils/functions.ts:334-355`) treats **every** key other than `sharedAssets`
and `languageId` as a non-default filter. That signal is exactly what shows the bar's "Clear all"
(`dot-filter-bar.component.html:7`).

So a scope written into the filters on every selection would light up "Clear all" the moment someone
picked **All Content** — the default — on a drive where nothing is filtered at all.

**Consequence for this feature**: FR-021. The scope counts as filter state only while it differs
from the default, mirroring how the search term already deletes its own key when it goes empty
(`dot-content-drive.store.ts:228-234`).

---

---

## Decisions (settled 2026-09-11)

The issue leaves four decisions open and marks them as needing a call before implementation. The
issue owner settled them on 2026-09-11. **Approval of this spec (PR 1) is the record of that
sign-off**; if any of them is reversed, the spec must be re-approved before `/speckit-plan` runs.

| # | Decision | Settled as | Why |
|---|---|---|---|
| 1 | Default scope | **All Content** | The no-regression choice. Every user who does nothing keeps exactly the results they get today, and the fast path is opt-in. Defaulting to Title would silently change what an existing saved workflow returns. |
| 2 | Field coverage in Title mode | **Contentlet `title` only** — not `fileName`, not `metadata.name` | Keeps the promise the label makes, and keeps the query to a single field so the cost argument holds. File assets are still reachable by name in practice (see [Assumptions](#assumptions)). |
| 3 | Stickiness | **URL only, per search** | The scope behaves like every other Content Drive filter: it survives reload, Back and Forward, and a shared link, and resets to the default on a clean entry. No new per-user preference storage. |
| 4 | Sorting in Title mode | **Unchanged** | Void as asked — see [Premise Correction 1](#1-results-are-not-sorted-by-score-when-a-term-is-present). Both scopes send the grid's current sort. |

---

## Problem Statement

The Content Drive search box has exactly one behavior: every term runs a global, all-content search.
An author who knows the **name** of what they are looking for has no way to say so. The term is
matched against every indexed field of every document — body copy, Story Block content, metadata —
so a common word returns a large slice of the drive and the one row the author wanted is buried
among documents that merely mention it.

The same breadth is also the expensive part of the request. The mandatory gate of the all-content
query is `+(catchall:<value>*^10 OR title_dotraw:*<value>*^2)`
(`GlobalSearchAttributeStrategy.java:38-40`): `catchall` aggregates every field of the document, so
a common term is cheap to look up and enormous in what it returns, and `title_dotraw:*<value>*` is a
leading wildcard, which forces a scan over every distinct raw title rather than a prefix seek.

In the drive, the matched set is not the end of the work. Matches are fed through database hydration
and per-chunk permission filtering (`BROWSER_CONTENT_CHUNK_SIZE`, default 900) before a page can be
returned, so a broad match multiplies database round trips and permission checks — not just index
time. Narrowing the candidate set at the source makes everything downstream cheaper with it.

So the scope selector earns its place twice: it is the result quality authors are asking for, and it
gives them a fast path that avoids the most expensive clause in the query.

## UI Surface

The ASCII diagram in the issue is the only mock — no image or design file accompanies it. What it
establishes, and all this spec fixes, is which components are on screen:

- The **search input** — the Content Drive search box as it exists today.
- A **scope dropdown beside it**, offering **Title** and **All Content**. Its label is the active
  scope; opening it marks the active option with a check.
- The **placeholder** of the input, which follows the active scope.

Nothing else about the control's appearance is fixed here. How the two sit together is an
implementation decision.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Find a known item by its name (Priority: P1)

An author knows the name of the item they want — a page called "Pricing", an image called "hero" —
and types it into the Content Drive search box. Today they get back everything whose body or blocks
happen to contain the word. They open the scope control next to the box, choose **Title**, and the
list narrows to rows whose name actually matches. The placeholder changes to say *Search by title*,
so the box states what it will do before they type again.

**Why this priority**: This is the whole feature. It delivers the result quality the issue was
raised for and the cheap query path on its own, with nothing else built. Stories 2 and 3 protect it;
neither creates value without it.

**Independent Test**: Fully testable by selecting **Title** with a term present and confirming that
a document which contains the term only in its body or Story Block — and not in its name —
disappears from the list, while the row whose name matches stays. Delivers the narrowing on its own.

**Acceptance Scenarios**:

1. **Given** the Content Drive is open with no search term, **When** the author looks at the search
   box, **Then** a scope control is visible next to the input, reading **All Content**, and the
   placeholder describes an all-content search.
2. **Given** a term is present in **All Content** scope, **When** the author opens the scope control
   and selects **Title**, **Then** the search re-runs immediately with the same term, results are
   restricted to name matches, pagination returns to page 1, and the control reads **Title** with a
   check mark beside that option.
3. **Given** a document whose title does **not** contain the term but whose body or Story Block
   does, **When** the scope is **Title**, **Then** that document is absent from the results.
4. **Given** that same document, **When** the scope is **All Content**, **Then** it is present —
   the all-content results are identical to what the drive returns today for that term.
5. **Given** scope **Title** and a term that matches a folder's name, **When** the search runs,
   **Then** the folder is listed, exactly as it is in **All Content** scope.
6. **Given** the scope control is open, **When** the author selects the scope that is already
   active, **Then** nothing is re-fetched and the current page is preserved.

---

### User Story 2 - The scope travels with the view (Priority: P2)

An author narrows to **Title**, finds the row, opens it, and comes back with the browser Back
button — the drive returns to the Title-scoped results, not to an all-content list. They copy the
address and send it to a colleague, who opens the same narrowed view.

**Why this priority**: Without it the scope silently resets on every reload and navigation, and an
author who has narrowed their search loses that narrowing without being told. It is a correctness
guarantee over Story 1 rather than a capability of its own, so it ranks below it.

**Independent Test**: Fully testable by selecting **Title**, reloading the page, and confirming the
control still reads **Title** and the results are still narrowed — then navigating away and back.

**Acceptance Scenarios**:

1. **Given** scope **Title** with a term, **When** the page is reloaded, **Then** the control reads
   **Title**, the term is preserved, and the results are the Title-scoped results.
2. **Given** the author switched from **All Content** to **Title**, **When** they press browser
   Back, **Then** the view returns to the **All Content** results for that term.
3. **Given** a Content Drive address carrying scope **Title**, **When** a different user opens it,
   **Then** they see the same narrowed view, subject to their own permissions.
4. **Given** the author enters Content Drive with no scope in the address, **When** the drive loads,
   **Then** the scope is **All Content**.
5. **Given** an address carrying an unrecognized scope value, **When** the drive loads, **Then** the
   scope falls back to **All Content** and the drive loads normally, with no error surfaced.
6. **Given** the author clears all filters, **When** the drive reloads its results, **Then** the
   scope returns to **All Content** along with the other filter defaults.

---

### User Story 3 - Everything that is not Content Drive is untouched (Priority: P3)

A developer using the Asset Picker, and an integration calling the Content Drive search endpoint,
see no change at all. The Asset Picker's search box keeps the single all-content behavior it has
today, with no scope control on screen. A request that does not mention the scope behaves exactly as
it does now.

**Why this priority**: It is a constraint on Stories 1 and 2 rather than a journey of its own, and
it is verified by absence. It still has to be stated and tested, because the search box is shared
and the endpoint is public.

**Independent Test**: Fully testable by opening the Asset Picker and confirming no scope control
appears and search behaves as before, and by replaying a stored Content Drive search request with no
scope field and comparing the results to the current ones.

**Acceptance Scenarios**:

1. **Given** the Asset Picker is open, **When** the author looks at its search box, **Then** no
   scope control is present and searching behaves exactly as it does today.
2. **Given** a Content Drive search request that omits the scope entirely, **When** it is processed,
   **Then** the results are identical to today's all-content results.
3. **Given** a Content Drive search request that names the all-content scope explicitly, **When** it
   is processed, **Then** the results are identical to the request that omits it.
4. **Given** a Content Drive search request naming a scope value the system does not recognize,
   **When** it is processed, **Then** it is rejected with a client error that names the offending
   value, rather than silently widening or narrowing the results.

---

### Edge Cases

- **Scope changed with an empty term.** No search is narrowed and nothing is re-fetched beyond the
  drive's normal unfiltered listing; the control still records the new scope so the next term uses
  it, and the placeholder updates.
- **Scope changed while a search is in flight.** The later request is the one whose results are
  shown; an earlier in-flight response never overwrites it.
- **Term matches only folder or link names, in Title scope.** Those rows are listed — folder and
  link matching is scope-independent ([Premise Correction 2](#2-folders-and-links-are-already-matched-on-name-only-in-both-scopes)).
- **Multi-word term in Title scope.** The term narrows rather than widens: a row must be a name
  match for the phrase as entered, not merely for one of its words.
- **Term containing characters the query syntax treats specially.** Handled the same way in both
  scopes; a term is never allowed to alter the structure of the query.
- **Scope combined with the other Content Drive filters** — content type, language, status,
  workflow, shared assets, per-field filters. The scope narrows the text match only; every other
  filter keeps applying as it does today, and combining them narrows further rather than
  conflicting.
- **Scope selected while a folder is selected in the tree.** A new search already resets the folder
  scope to the site root; changing the scope of an existing search behaves consistently with that.
- **Scope set explicitly back to All Content.** The drive returns to the state it would have had if
  the control had never been touched: nothing recorded in the address, and no "clear all filters"
  offered on an otherwise unfiltered drive.
- **A file whose title was edited to something other than its file name**, searched by file name in
  Title scope: it does not match. See [Assumptions](#assumptions).

## Requirements *(mandatory)*

### Functional Requirements

**The control**

- **FR-001**: The Content Drive search box MUST present a scope control adjacent to the search
  input, within the same visual container, offering exactly two options: **Title** and
  **All Content**.
- **FR-002**: The control MUST display the active scope as its label, and MUST mark the active
  option with a check when opened.
- **FR-003**: The search input's placeholder MUST describe the active scope, so the box states what
  it will do before the author types.
- **FR-004**: Selecting a scope MUST re-run the current search immediately, without requiring the
  author to retype or re-submit the term.
- **FR-005**: Selecting a scope MUST reset pagination to the first page.
- **FR-006**: Re-selecting the already-active scope MUST NOT trigger a new search.
- **FR-007**: The scope control MUST be reachable and operable by keyboard and MUST expose its
  current selection to assistive technology.

**Behavior**

- **FR-008**: In **Title** scope, a contentlet MUST be returned only when its title matches the
  term. A contentlet whose term occurrence is confined to body copy, Story Block content or
  metadata MUST NOT be returned.
- **FR-009**: In **All Content** scope, results MUST be identical to what Content Drive search
  returns today for the same term and filters — no regression of any kind.
- **FR-010**: In **Title** scope, the query MUST NOT use an all-fields aggregate clause, and MUST
  NOT use a leading-wildcard term as its mandatory gate. This is the requirement that makes the
  scope a genuine fast path rather than a display filter.
- **FR-011**: Folder and link name matching MUST behave identically in both scopes.
- **FR-012**: The active sort MUST be unaffected by the scope; both scopes MUST apply the sort the
  author has chosen, with the existing default.
- **FR-013**: The scope MUST compose with every other Content Drive filter without altering their
  behavior.

**State and contract**

- **FR-014**: A non-default scope MUST be encoded in the address alongside the other Content Drive
  filters, and MUST be restored from it on reload and on browser Back/Forward.
- **FR-015**: An absent or unrecognized scope in the address MUST resolve to **All Content**,
  without surfacing an error.
- **FR-016**: The scope MUST NOT be persisted as a per-user preference; a clean entry into Content
  Drive MUST start at **All Content**.
- **FR-017**: The Content Drive search request MUST carry the scope as an optional field that
  defaults to all-content behavior, so a request that omits it is processed exactly as it is today.
- **FR-018**: A request naming an unrecognized scope value MUST be rejected with a client error
  identifying the value, rather than silently defaulting.
- **FR-019**: The shared search box MUST expose the scope control as opt-in. Surfaces that do not
  opt in — the Asset Picker today — MUST render and behave exactly as they do now.
- **FR-020**: Clearing all filters MUST return the scope to **All Content**.
- **FR-021**: The scope MUST count as filter state only while it is not the default. Selecting
  **All Content** MUST leave the drive in the state it would have been in had the control never been
  touched — in particular, it MUST NOT cause a "clear all filters" affordance to be offered on a
  drive that is otherwise unfiltered.

### Key Entities

- **Search scope**: Which part of a document a Content Drive search term is matched against. Two
  values — *Title* and *All Content* — with *All Content* as the default. Lives alongside the search
  term as part of the drive's filter state, is carried in the address, and is sent with the search
  request.
- **Content Drive search request**: The existing description of what the drive should list —
  location, term, content types, languages, status, workflow, per-field criteria, sort, paging.
  Gains the scope as one more optional element of the text-matching part.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On a drive containing a document whose body mentions the search term and a document
  whose name is the search term, an author in **Title** scope sees only the second — verified as a
  binary pass on a seeded dataset.
- **SC-002**: For any term and filter combination, **All Content** results are byte-identical to
  the results the same drive returns before this change — zero regressions across the search cases
  covered by the endpoint's test suite.
- **SC-003**: On a large dataset, a **Title** search returns its first page faster than the same
  term in **All Content** scope, and the before/after comparison is recorded on the issue. The
  target is a measurable reduction, not a fixed threshold; the comparison itself is the deliverable
  the issue asks for.
- **SC-004**: A Content Drive address carrying a scope reproduces the same narrowed view for a
  second user 100% of the time, and reload and Back/Forward preserve the scope in 100% of attempts.
- **SC-005**: 100% of search requests that omit the scope produce today's results — confirmed by an
  explicit endpoint test for the omitted field, not only by the explicit all-content case.
- **SC-006**: The Asset Picker's search box shows no scope control and its search behavior is
  unchanged, confirmed by its existing tests passing without modification.
- **SC-007**: An author who knows the name of the item they want reaches it from the search box
  without scrolling past unrelated body matches, on a drive where the all-content search for the
  same term returns more than one page.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: Content Drive's keyword search, and the browsing service that
  backs it. The browsing service is long-standing, pre-Content-Drive product surface shared with
  other file-browsing entry points; Content Drive's search endpoint and its front end are recent.
  The shared search box is also used by the Asset Picker, which is explicitly not in scope.
- **Backward-compatibility expectations**: Strict. All-content search must be unchanged for every
  existing caller and every existing address; the scope is additive and optional at every layer, and
  its absence must be indistinguishable from today. No existing behavior is deprecated. The
  all-content query strategy is shared with the Search portlet and the Relationships dialog and must
  keep serving them unmodified — the Title scope is a sibling path, not a branch inside the existing
  one.
- **Known related decisions**: [#36688](https://github.com/dotCMS/core/issues/36688) deliberately
  replaced a broad leading-wildcard all-fields query with the current strategy, for the same cost
  reasons argued here — the Title scope must not reintroduce what that issue removed.
  [#36814](https://github.com/dotCMS/core/issues/36814) tracks search performance at scale and is
  where SC-003's measurement belongs. `/speckit-plan` will formally consult `dotCMS/platform-adrs`.

## Assumptions

- **File assets remain findable by name in Title scope, through their title.** Decision 2 excludes
  `fileName` and `metadata.name`, but a file asset carries a title field that dotCMS keeps in step
  with the file name — it is seeded from it and rewritten on rename
  (`FileAssetAPIImpl.java:498`). So searching a file by its name works in Title scope for the
  ordinary case. The gap is narrow and deliberate: a file whose title has been edited to something
  other than its file name will not match on the file name. If that gap proves to matter in
  practice, widening Title scope to cover file names is a follow-up with its own spec, not a
  silent change here.
- **The issue's ASCII diagram is the whole design input.** No mock image or design file exists for
  this control, and none is being waited on. The spec fixes which components are present and how
  they behave; their appearance is settled during implementation.
- **The scope is a front-end-visible concept only for Content Drive.** No other portlet gains it in
  this work, and the query strategy shared with the Search portlet and the Relationships dialog is
  left as-is.
- **"Title" is the label authors understand**, including for folders and files, and does not need to
  read differently per row type.
- **The existing address-encoding scheme for Content Drive filters can carry the scope** without a
  new mechanism, and an unknown value degrades to the default the same way an unknown status does
  today.
- **The measurement in SC-003 needs a dataset large enough for the difference to exceed noise.** The
  issue does not name one; producing it is part of the work, and the comparison is recorded on the
  issue rather than in the repository.
- **No database, index-mapping or content-model change is required.** The scope selects between two
  ways of querying what is already indexed.
