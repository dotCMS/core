# Feature Specification: Content Drive — Title / All Fields search scope, with literal-text search terms

**Feature Branch**: `issue-37479-content-drive-search-scope`

**Created**: 2026-09-11

**Last revised**: 2026-09-15 — two requirements amended during implementation (FR-029 narrowed
2026-09-14, FR-003 narrowed 2026-09-15 — see each requirement inline, and `tasks.md` for the full
record). **Approved on PR #37518 at `1ce8cdd1bf` predates both amendments — re-approval is
required before PR 2 opens.**

**Status**: Draft — pending re-approval of the FR-029 and FR-003 amendments

**Type**: New Feature

**Resolves**: [#37479](https://github.com/dotCMS/core/issues/37479) (search scope selector) **and**
[#37532](https://github.com/dotCMS/core/issues/37532) (Lucene query-syntax characters in search
terms — High severity, customer ticket
[39185](https://helpdesk.dotcms.com/a/tickets/39185)). The two were specified separately and merged
on 2026-09-14 once #37532's defect was found to live in the very clause this feature rewrites — see
[Why #37532 lands here](#why-37532-lands-here).

**Related history**: [#36688](https://github.com/dotCMS/core/issues/36688) (replaced the broad `catchall:*kw*` wildcard with the current strategy), [#36814](https://github.com/dotCMS/core/issues/36814) (search performance at scale). Sibling in flight: [#37426](https://github.com/dotCMS/core/issues/37426) / [PR #37487](https://github.com/dotCMS/core/pull/37487) (Content Drive **browse** scopes) — the two features land fields on the same request object and are deliberately named apart; see [Review Decision 5](#review-decisions-settled-2026-09-13).

**Input (#37479)**: User description: "Content Drive: add a Title / All Content scope selector to the search box. Default scope = All Content (no regression); Title mode matches strictly the contentlet title plus folder names, not fileName/metadata.name; scope persists only in the URL filters; sorting unchanged."

> The issue's words are recorded verbatim above. The wide option is named **All Fields** in this spec — see [Review Decision 7](#review-decisions-settled-2026-09-13) for why "All Content" was rejected.

---

## Premise Corrections

Six things do not survive verification against `main`. Three of them narrow the work; the fourth
adds a rule the issue's file list has no place for; the fifth settles which query path the search
scope actually governs; the sixth is a live customer defect sitting in the clause this feature
rewrites.

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
The search scope governs only the Elasticsearch clause used for **contentlets**. An acceptance
criterion that reads "Title mode returns only rows whose title or folder name matches" is already
half-true today and stays true in All Fields mode.

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
picked **All Fields** — the default — on a drive where nothing is filtered at all.

**Consequence for this feature**: FR-021. The scope counts as filter state only while it differs
from the default, mirroring how the search term already deletes its own key when it goes empty
(`dot-content-drive.store.ts:228-234`).

### 5. Content Drive's contentlet text search is always index-routed — the SQL text path is unreachable

The browsing service can match a text filter two different ways. `BrowserAPIImpl` either hands the
term to Elasticsearch or, when `useElasticsearchFiltering` is false, builds a SQL predicate that
runs `contentlet_as_json::text ILIKE '%token%'` over the whole serialized contentlet plus the asset
name (`BrowserAPIImpl.java:2053-2060`, `appendFilterQuery` at `:2222`). The builder's own default is
`false` (`BrowserQuery.java:292`), so the SQL path is the fallback in general.

Content Drive never takes it. `ContentDriveHelper` sets `useElasticsearchFiltering(true)`
unconditionally whenever the request carries text (`ContentDriveHelper.java:180-184`), and
`isUseElasticSearchForFiltering` then returns true because a text filter is present
(`BrowserAPIImpl.java:1582-1589`).

**Consequence for this feature**: the search scope has exactly one query path to govern, and
FR-010's "no all-fields aggregate, no leading-wildcard gate" is a statement about the Elasticsearch
clause alone. There is no second, SQL-shaped text match that a Title scope could silently fail to
narrow. This is also what settles the ADR-0018 question — see
[ADR-0018 alignment](#adr-0018-alignment).

### 6. The search box does not treat the term as literal text today

The edge case "a term is never allowed to alter the structure of the query" was written into this
spec as a property to preserve. It is not a property the search box has. `GlobalSearchAttributeStrategy`
escapes **only its final clause**:

```java
// :37-38 — the MANDATORY gate, built from the RAW value
luceneQuery.append("+(catchall:").append(value).append("*^10 OR ")
        .append(fieldName).append("_dotraw:*").append(value).append("*^2)");
// :46-48 — escaping happens here, and applies to this clause alone
value = value.replaceAll(SPECIAL_CHARS_TO_ESCAPE, "\\\\$1");
luceneQuery.append("title:").append(value).append("*");
```

Three distinct defects follow, and nothing sanitizes the value upstream — `ContentDriveHelper:183`
passes `filters().text()` straight into `withFilter`, which reaches the strategy raw:

1. **The gate is built from unescaped input.** A term such as `ABC (XETRA: DB)` produces
   `+(catchall:ABC (XETRA: DB)*^10 OR ...)`, which is not valid `query_string` syntax.
2. **The escape set is incomplete.** `SPECIAL_CHARS_TO_ESCAPE` (`:20`) is a private regex missing
   `/`, while `LuceneQueryUtils.LUCENE_SPECIAL_CHARS` — vendor-neutral, documented, already used by
   `TextFieldStrategy` — covers the full reserved set including `/`.
3. **Consecutive separators emit empty clauses.** `:40-45` splits on `[,|\s+]` with no empty-token
   filter, so `"a  b"` yields a `title:^5` clause with no term.

A query that fails to parse is then **swallowed**: `BrowserAPIImpl:893-895` logs and returns an
empty set, and the multi-query path does the same at every level. The user sees "no results" for
content they know exists — the exact symptom of customer ticket 39185.

**Consequence for this feature**: FR-027 to FR-030, and a carve-out in FR-009/SC-002, because
fixing this **changes All Fields results** for affected terms. The parallel path is already correct
and is the model to copy: Content Drive's *field* filters go through `TextFieldStrategy:45-46,53-54`,
which escapes via `LuceneQueryUtils.escape` and filters empty tokens.

---

## Why #37532 lands here

[#37532](https://github.com/dotCMS/core/issues/37532) was raised against the **Content Search
portlet**, where `ContentletAjax.searchContentletsByUser()` builds field-filter clauses without
escaping and swallows the resulting parse failure at `ContentletAjax.java:1058`. It is merged into
this spec rather than kept separate for two reasons.

**The issue asks for it.** It is explicitly filed as an enhancement rather than a defect fix, and
says so in its own words: the legacy field-level query generation should be reworked rather than
patched, and *"Content Drive is intended to replace the Content Search portlet, so the improved
field-level search behaviour should land there."* This spec is that landing.

**The defect is already here.** Premise Correction 6 shows all three failure modes — unescaped
input, incomplete escape set, empty clauses — plus the silent swallow, living in Content Drive's
own search box. Fixing #37479 without them would mean rewriting the exact clause that carries the
bug and leaving the bug in it.

**What this does NOT do, and it matters**: the Content Search portlet keeps its current behavior.
A user filtering on a content type field in that portlet still gets a silent empty result set for a
value containing `:` or `(`. Closing #37532 on this work is only honest if the team accepts that
trade — it is the direction the issue itself sets, but the customer on ticket 39185 is using the
portlet today, not Content Drive. If that is not acceptable, the portlet needs its own fix and
#37532 should stay open against it. Flagged rather than assumed.

Scope split, explicitly:

| #37532 acceptance criterion | Where it lands |
|---|---|
| Values treated as literal text, not query syntax | **Here** — FR-027 (search box). Already satisfied for Content Drive field filters. |
| Full reserved set `\ + - ! ( ) : ^ [ ] " { } ~ * ? \| & /` verified | **Here** — SC-010 |
| Consecutive whitespace no longer emits `**` clauses | **Here** — FR-028 |
| Failed query surfaces an error instead of a silent zero result | **Here** — FR-029 |
| `/` added to the escape set of the global catchall path | **Here** — FR-027, by adopting `LuceneQueryUtils.escape` |
| Regression test covering the reported headline value | **Here** — SC-009 |
| Equivalent field-filter behaviour confirmed in Content Drive | **Here** — FR-030, as verification; the code already does it |
| Rework of `ContentletAjax` field-filter construction | **Not here** — legacy portlet, slated for replacement |
| Error state in the Content Search portlet's own UI | **Not here** — same reason |

---

## Decisions (settled 2026-09-11)

The issue leaves four decisions open and marks them as needing a call before implementation. The
issue owner settled them on 2026-09-11. **Approval of this spec (PR 1) is the record of that
sign-off**; if any of them is reversed, the spec must be re-approved before `/speckit-plan` runs.

| # | Decision | Settled as | Why |
|---|---|---|---|
| 1 | Default scope | **All Fields** | The no-regression choice. Every user who does nothing keeps exactly the results they get today, and the fast path is opt-in. Defaulting to Title would silently change what an existing saved workflow returns. |
| 2 | Field coverage in Title mode | **Contentlet `title` only** — not `fileName`, not `metadata.name` | Keeps the promise the label makes, and keeps the query to a single field so the cost argument holds. File assets are still reachable by name in practice (see [Assumptions](#assumptions)). |
| 3 | Stickiness | **URL only, per search** | The scope behaves like every other Content Drive filter: it survives reload, Back and Forward, and a shared link, and resets to the default on a clean entry. No new per-user preference storage. |
| 4 | Sorting in Title mode | **Unchanged** | Void as asked — see [Premise Correction 1](#1-results-are-not-sorted-by-score-when-a-term-is-present). Both scopes send the grid's current sort. |

## Review Decisions (settled 2026-09-13)

Review round 1 on [PR #37518](https://github.com/dotCMS/core/pull/37518) raised four further calls.
They are settled here and are part of what PR 1's approval signs off.

| # | Decision | Settled as | Why |
|---|---|---|---|
| 5 | What the two scopes are called in prose | **"search scope"**, never bare "scope" | Content Drive is simultaneously gaining a **browse** scope (#37426) that says *where* you are browsing. Bare "scope" would name either, and both end up as fields on the same request object, so the ambiguity would outlive the specs. The sibling spec spells "browse scope" throughout for the matching half. |
| 6 | The wire name and its home | **`filters.searchScope`**, inside the existing filters object | `AbstractQueryFilters` is `{ text, filterFolders }` today, and `filterFolders`' Javadoc says "when text is provided". Both members exist to qualify the text search, which is exactly what the search scope does — it says which fields `text` reads and means nothing without `text`. Beside `text`, they travel together and a scope with no text is visibly nonsense rather than a validation rule someone must remember. The browse scope stays top level for the opposite reasons: it qualifies `assetPath`, and "Clear all" resets `filters`, which must never be able to navigate you elsewhere. |
| 7 | The label of the wide option | **All Fields** (values `TITLE` / `ALL_FIELDS`) | "All Content" describes a *set of content*, which is what the browse scope's **All** genuinely means. This scope does not widen which content is searched — it widens which **fields** of each document the term is read against. Naming it "All Content" would have put two different meanings of the same word on the same request. |
| 8 | ADR-0018's `DB ∪ Index` title routing | **Out of scope, explicitly** — Title scope inherits today's index-only routing | See [ADR-0018 alignment](#adr-0018-alignment). The union is gated on title-persistence work the ADR itself defers, and no text search consults the DB title column today. Title scope neither creates nor widens that gap, and it picks the union up for free when the gated work lands. |

### Merge of #37532 (settled 2026-09-14)

| # | Decision | Settled as | Why |
|---|---|---|---|
| 9 | Whether #37532 (literal-text search terms) joins this spec | **Merged in**, for the search box only | The defect lives in the clause FR-010 rewrites, and #37532 itself directs the improved behaviour to land in Content Drive. Keeping them apart would mean rewriting the buggy clause and leaving the bug in it. See [Why #37532 lands here](#why-37532-lands-here). |
| 10 | FR-009's "no regression" promise vs. fixing the defect | **FR-009 yields** — a carve-out, written down | Escaping the gate *changes* All Fields results for terms containing reserved characters. That is the point, but it contradicts FR-009/SC-002 as originally approved, so the exception is stated rather than smuggled in. |

## ADR-0018 alignment

[ADR-0018](https://github.com/dotCMS/platform-adrs/blob/main/decisions/0018-database-first-content-drive-search-with-index-deferred-text-filtering.md)
routes the **Title** criterion to `DB ∪ Index` — the `contentlet.title` column unioned with index
records — so that a just-saved or just-renamed item stays findable while the index catches up. This
feature's Title scope is defined as an Elasticsearch clause and consults no DB column. That is a
deliberate call, not an omission, for three reasons:

1. **The ADR defers its own union.** It states that `contentlet.title` "is **not reliably
   populated**", that the display title is derived and `Contentlet.title()` is `@Nullable`, and that
   "populating `contentlet.title` reliably is a known gap to be addressed in a separate issue". The
   ADR fixes the routing contract that will *consume* the column once it lands; it does not claim
   the column is usable today.
2. **No text search consults it today, in either scope.** Per
   [Premise Correction 5](#5-content-drives-contentlet-text-search-is-always-index-routed--the-sql-text-path-is-unreachable),
   Content Drive's contentlet text matching is index-only. The pre-existing exposure to index lag is
   identical in All Fields scope and would be identical in Title scope. This feature does not
   introduce it, and narrowing the clause does not deepen it — an item missing from the index is
   missing from both scopes equally.
3. **The union arrives for free.** When the title-persistence issue lands and the text path becomes
   `DB-title ∪ index`, Title scope is the scope that benefits most directly, because the DB column
   it unions in *is* the title. Nothing in this spec has to be undone for that to happen.

**What this means for the plan**: the implementation must not make Title scope *harder* to union
later — FR-026. Read-your-writes for the text path stays tracked where the ADR put it, outside this
feature.

---

## Problem Statement

The Content Drive search box has exactly one behavior: every term is matched against every indexed
field of every document. An author who knows the **name** of what they are looking for has no way to
say so. The term hits body copy, Story Block content and metadata alike, so a common word returns a
large slice of the drive and the one row the author wanted is buried among documents that merely
mention it.

The same breadth is also the expensive part of the request. The mandatory gate of the all-fields
query is `+(catchall:<value>*^10 OR title_dotraw:*<value>*^2)`
(`GlobalSearchAttributeStrategy.java:38-40`): `catchall` aggregates every field of the document, so
a common term is cheap to look up and enormous in what it returns, and `title_dotraw:*<value>*` is a
leading wildcard, which forces a scan over every distinct raw title rather than a prefix seek.

In the drive, the matched set is not the end of the work. Matches are fed through database hydration
and per-chunk permission filtering (`BROWSER_CONTENT_CHUNK_SIZE`, default 900) before a page can be
returned, so a broad match multiplies database round trips and permission checks — not just index
time. Narrowing the candidate set at the source makes everything downstream cheaper with it.

So the search scope selector earns its place twice: it is the result quality authors are asking for,
and it gives them a fast path that avoids the most expensive clause in the query.

## UI Surface

The ASCII diagram in the issue is the only mock — no image or design file accompanies it. What it
establishes, and all this spec fixes, is which components are on screen:

- The **search input** — the Content Drive search box as it exists today.
- A **search scope control beside it**, offering **Title** and **All Fields**. Its label is the
  active scope; opening it marks the active option with a check.
- A short **explanation of what each option matches**, available from the control. The distinction
  between "the item's name" and "anything written anywhere in the item" is not self-evident from two
  words, and the control is new (FR-022).
- The **placeholder** of the input, which follows the active scope.

Nothing else about the control's appearance is fixed here. How the two sit together, and whether the
explanation is a tooltip, helper text or per-option description, are implementation decisions.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Find a known item by its name (Priority: P1)

An author knows the name of the item they want — a page called "Pricing", an image called "hero" —
and types it into the Content Drive search box. Today they get back everything whose body or blocks
happen to contain the word. They open the search scope control next to the box, choose **Title**,
and the list narrows to rows whose name actually matches. The placeholder changes to say *Search by
title*, so the box states what it will do before they type again.

**Why this priority**: This is the whole feature. It delivers the result quality the issue was
raised for and the cheap query path on its own, with nothing else built. Stories 2 and 3 protect it;
neither creates value without it.

**Independent Test**: Fully testable by selecting **Title** with a term present and confirming that
a document which contains the term only in its body or Story Block — and not in its name —
disappears from the list, while the row whose name matches stays. Delivers the narrowing on its own.

**Acceptance Scenarios**:

1. **Given** the Content Drive is open with no search term, **When** the author looks at the search
   box, **Then** a search scope control is visible next to the input, reading **All Fields**, and
   the placeholder describes an all-fields search.
2. **Given** a term is present in **All Fields** scope, **When** the author opens the search scope
   control and selects **Title**, **Then** the search re-runs immediately with the same term,
   results are restricted to name matches, pagination returns to page 1, and the control reads
   **Title** with a check mark beside that option.
3. **Given** a document whose title does **not** contain the term but whose body or Story Block
   does, **When** the search scope is **Title**, **Then** that document is absent from the results.
4. **Given** that same document, **When** the search scope is **All Fields**, **Then** it is
   present — the all-fields results are identical to what the drive returns today for that term.
5. **Given** search scope **Title** and a term that matches a folder's name, **When** the search
   runs, **Then** the folder is listed, exactly as it is in **All Fields** scope.
6. **Given** the search scope control is open, **When** the author selects the scope that is already
   active, **Then** nothing is re-fetched and the current page is preserved.
7. **Given** the author has not used this control before, **When** they open it, **Then** an
   explanation is available that distinguishes matching the item's name from matching anything
   written anywhere in the item.

---

### User Story 2 - The search scope travels with the view (Priority: P2)

An author narrows to **Title**, finds the row, opens it, and comes back with the browser Back
button — the drive returns to the Title-scoped results, not to an all-fields list. They copy the
address and send it to a colleague, who opens the same narrowed view.

**Why this priority**: Without it the search scope silently resets on every reload and navigation,
and an author who has narrowed their search loses that narrowing without being told. It is a
correctness guarantee over Story 1 rather than a capability of its own, so it ranks below it.

**Independent Test**: Fully testable by selecting **Title**, reloading the page, and confirming the
control still reads **Title** and the results are still narrowed — then navigating away and back.

**Acceptance Scenarios**:

1. **Given** search scope **Title** with a term, **When** the page is reloaded, **Then** the control
   reads **Title**, the term is preserved, and the results are the Title-scoped results.
2. **Given** the author switched from **All Fields** to **Title**, **When** they press browser Back,
   **Then** the view returns to the **All Fields** results for that term.
3. **Given** a Content Drive address carrying search scope **Title**, **When** a different user
   opens it, **Then** they see the same narrowed view, subject to their own permissions.
4. **Given** the author enters Content Drive with no search scope in the address, **When** the drive
   loads, **Then** the search scope is **All Fields**.
5. **Given** an address carrying an unrecognized search scope value, **When** the drive loads,
   **Then** the search scope falls back to **All Fields** and the drive loads normally, with no
   error surfaced.
6. **Given** the author clears all filters, **When** the drive reloads its results, **Then** the
   search scope returns to **All Fields** along with the other filter defaults.

---

### User Story 3 - Everything that is not Content Drive is untouched (Priority: P3)

A developer using the **Asset Picker**, and an integration calling the Content Drive search
endpoint, see no change at all. The Asset Picker's search box keeps the single all-fields behavior
it has today, with no search scope control on screen. A request that does not mention the search
scope behaves exactly as it does now.

**Why this priority**: It is a constraint on Stories 1 and 2 rather than a journey of its own, and
it is verified by absence. It still has to be stated and tested, because the search box is shared
and the endpoint is public.

**Independent Test**: Fully testable by opening the Asset Picker and confirming no search scope
control appears and search behaves as before, and by replaying a stored Content Drive search request
with no `searchScope` field and comparing the results to the current ones.

**Acceptance Scenarios**:

1. **Given** the Asset Picker is open, **When** the author looks at its search box, **Then** no
   search scope control is present and searching behaves exactly as it does today.
2. **Given** a Content Drive search request that omits the search scope entirely, **When** it is
   processed, **Then** the results are identical to today's all-fields results.
3. **Given** a Content Drive search request that names the all-fields scope explicitly, **When** it
   is processed, **Then** the results are identical to the request that omits it.
4. **Given** a Content Drive search request naming a search scope value the system does not
   recognize, **When** it is processed, **Then** it is rejected with a client error that names the
   offending value, rather than silently widening or narrowing the results.
5. **Given** any of the other entry points that reach the same content listing — the assets REST
   API, the legacy admin file browser, the Velocity macro viewtool — **When** they list content
   after this change, **Then** they return exactly what they returned before it.

---

### User Story 4 - A name that contains punctuation is found (Priority: P1)

An author searches for `ABC Bank (XETRA: DBKGn.DB / NYSE: DB)` — the headline of a piece of content
they are looking at in another tab. Today the drive reports "no results found". The colons,
parentheses and slash are read as query syntax rather than as part of the name, the query fails to
parse, the failure is logged and discarded, and the author is told their content does not exist.
After this change the term is matched as the literal text it is, and the item is returned — in both
search scopes.

**Why this priority**: It is listed fourth but ranks P1. This is the live customer defect behind
[#37532](https://github.com/dotCMS/core/issues/37532) (High severity, ticket 39185), and it is a
silent wrong answer rather than a missing capability — the worst failure mode a search box has,
because the author has no way to tell a broken query from an empty drive. Story 1 builds on the same
clause, so the two are implemented together.

**Independent Test**: Fully testable by seeding content whose title carries each character in the
reserved set, searching for it verbatim, and confirming it is returned. Delivers the fix on its own,
with or without the search scope control on screen.

**Acceptance Scenarios**:

1. **Given** content whose title is `ABC Bank (XETRA: DBKGn.DB / NYSE: DB)`, **When** the author
   pastes that title into the search box in **All Fields** scope, **Then** the item is returned.
2. **Given** that same content, **When** the author searches for it in **Title** scope, **Then** the
   item is returned — the fix applies to the new clause as well as the existing one.
3. **Given** a term containing any character of the reserved set
   `\ + - ! ( ) : ^ [ ] " { } ~ * ? | & /`, **When** the search runs, **Then** the character is
   matched as literal text and never alters the structure of the query.
4. **Given** a term containing consecutive spaces or separators, **When** the search runs, **Then**
   no empty clause is emitted and the results are the same as for the single-separator form.
5. **Given** a search request that nonetheless fails to execute, **When** the drive renders the
   response, **Then** the author is shown an error state, **not** an empty result list presented as
   a successful search.

---

### Edge Cases

- **Search scope changed with an empty term.** No search is narrowed and nothing is re-fetched
  beyond the drive's normal unfiltered listing; the control still records the new scope so the next
  term uses it, and the placeholder updates.
- **Search scope changed while a search is in flight.** The later request is the one whose results
  are shown; an earlier in-flight response never overwrites it.
- **Term matches only folder or link names, in Title scope.** Those rows are listed — folder and
  link matching is scope-independent ([Premise Correction 2](#2-folders-and-links-are-already-matched-on-name-only-in-both-scopes)).
- **Multi-word term in Title scope.** The term narrows rather than widens: a row must be a name
  match for the phrase as entered, not merely for one of its words.
- **Term containing characters the query syntax treats specially.** Matched as literal text in both
  scopes; a term is never allowed to alter the structure of the query. This does **not** hold today
  ([Premise Correction 6](#6-the-search-box-does-not-treat-the-term-as-literal-text-today)) and is
  made true by FR-027.
- **Term that is only separators**, or that reduces to no usable token after splitting. It yields no
  text clause at all rather than an empty one, and the drive lists as it would with no term.
- **Search scope combined with the other Content Drive filters** — content type, language, status,
  workflow, shared assets, per-field filters. The search scope narrows the text match only; every
  other filter keeps applying as it does today, and combining them narrows further rather than
  conflicting.
- **Search scope combined with a browse scope** (#37426, in flight). The two are independent: a
  browse scope says which slice of content is being listed, a search scope says which fields the
  term is read against within it. Neither may change the other's answer.
- **Search scope selected while a folder is selected in the tree.** A new search already resets the
  folder scope to the site root; changing the search scope of an existing search behaves
  consistently with that.
- **Search scope set explicitly back to All Fields.** The drive returns to the state it would have
  had if the control had never been touched: nothing recorded in the address, and no "clear all
  filters" offered on an otherwise unfiltered drive.
- **A file whose title was edited to something other than its file name**, searched by file name in
  Title scope: it does not match. See [Assumptions](#assumptions).
- **An item saved or renamed moments before the search, not yet indexed.** It is missing from the
  results in **both** scopes, exactly as it is today — Title scope does not make this worse. See
  [ADR-0018 alignment](#adr-0018-alignment).

## Requirements *(mandatory)*

### Functional Requirements

**The control**

- **FR-001**: The Content Drive search box MUST present a search scope control adjacent to the
  search input, within the same visual container, offering exactly two options: **Title** and
  **All Fields**.
- **FR-002**: The control MUST display the active search scope as its label, and MUST mark the
  active option with a check when opened.
- **FR-003**: ~~The search input's placeholder MUST describe the active search scope, so the box
  states what it will do before the author types.~~ **Amended 2026-09-15.** The placeholder MUST
  NOT vary by search scope. It is always the shared search box's own default ("Search"), in both
  Title and All Fields. Direct instruction from the issue owner during UI review — not a defect or
  a constraint discovered while building; FR-003 as originally written was fully implementable and
  had been implemented and tested. Consequence: User Story 1's acceptance scenario 2 and the
  narrative around SC-007 describing a scope-aware placeholder are superseded by this wording.
- **FR-004**: Selecting a search scope MUST re-run the current search immediately, without requiring
  the author to retype or re-submit the term.
- **FR-005**: Selecting a search scope MUST reset pagination to the first page.
- **FR-006**: Re-selecting the already-active search scope MUST NOT trigger a new search.
- **FR-007**: The search scope control MUST be reachable and operable by keyboard and MUST expose
  its current selection to assistive technology.

**Behavior**

- **FR-008**: In **Title** search scope, a contentlet MUST be returned only when its title matches
  the term. A contentlet whose term occurrence is confined to body copy, Story Block content or
  metadata MUST NOT be returned.
- **FR-009**: In **All Fields** search scope, results MUST be identical to what Content Drive search
  returns today for the same term and filters — no regression of any kind. **One carve-out, and only
  one**: terms containing Lucene reserved characters or consecutive separators, whose results change
  by design under FR-027 and FR-028 because today's behaviour for them is a malformed query and a
  silent empty result set. Every term outside that carve-out MUST be unchanged, and the carve-out
  MUST NOT be used to justify any other difference.
- **FR-010**: In **Title** search scope, the query MUST NOT use an all-fields aggregate clause, and
  MUST NOT use a leading-wildcard term as its mandatory gate. This is the requirement that makes the
  search scope a genuine fast path rather than a display filter.
- **FR-011**: Folder and link name matching MUST behave identically in both search scopes.
- **FR-012**: The active sort MUST be unaffected by the search scope; both scopes MUST apply the
  sort the author has chosen, with the existing default.
- **FR-013**: The search scope MUST compose with every other Content Drive filter without altering
  their behavior.

**State and contract**

- **FR-014**: A non-default search scope MUST be encoded in the address alongside the other Content
  Drive filters, and MUST be restored from it on reload and on browser Back/Forward.
- **FR-015**: An absent or unrecognized search scope in the address MUST resolve to **All Fields**,
  without surfacing an error.
- **FR-016**: The search scope MUST NOT be persisted as a per-user preference; a clean entry into
  Content Drive MUST start at **All Fields**.
- **FR-017**: The Content Drive search request MUST carry the search scope as an optional field that
  defaults to all-fields behavior, so a request that omits it is processed exactly as it is today.
  **The Asset Picker is the caller this protects**: it builds its own Content Drive search request
  (`with-asset-browse.feature.ts` → `DotContentDriveService.search()`, the same `POST /drive/search`
  Content Drive uses) and will never name a search scope. Any future change to the default has to
  confront the Asset Picker by name, not merely re-run Content Drive's tests.
- **FR-018**: A request naming an unrecognized search scope value MUST be rejected with a client
  error identifying the value, rather than silently defaulting.
- **FR-019**: The shared search box MUST expose the search scope control as opt-in. Surfaces that do
  not opt in — the Asset Picker today — MUST render and behave exactly as they do now.
- **FR-020**: Clearing all filters MUST return the search scope to **All Fields**.
- **FR-021**: The search scope MUST count as filter state only while it is not the default.
  Selecting **All Fields** MUST leave the drive in the state it would have been in had the control
  never been touched — in particular, it MUST NOT cause a "clear all filters" affordance to be
  offered on a drive that is otherwise unfiltered.

**Naming, blast radius and forward compatibility**

- **FR-022**: The control MUST make available a short explanation of what each option matches,
  distinguishing the item's name from anything written anywhere in the item. The two labels alone
  MUST NOT be relied on to convey the distinction.
- **FR-023**: The concept MUST be called **search scope** wherever it is named — in the UI copy, in
  the address, in the request, and in the code. The request field MUST be `searchScope` and MUST sit
  **inside the existing `filters` object**, beside `text`, whose meaning it qualifies and without
  which it means nothing. Its values MUST be `TITLE` and `ALL_FIELDS`. Bare "scope" MUST NOT be used
  for it, because Content Drive is concurrently gaining a **browse** scope (#37426) that is a
  different thing in the same request.
- **FR-024**: The change MUST stay within the text-search branch of the shared content-listing
  service. Callers that reach that listing by another door — the assets REST API
  (`WebAssetHelper`), the legacy admin file browser (`BrowserAjax`), the Velocity macro viewtool
  (`DotCMSMacroWebAPI`) — MUST produce exactly the results they produce today. Widening the change
  into shared query building would widen the blast radius from two callers to six.
- **FR-025**: A request carrying a search scope with no text MUST be treated as the contract error
  it is, rather than silently ignored — the field qualifies `text` and has no meaning without it.
- **FR-026**: **Title** search scope MUST NOT foreclose ADR-0018's `DB-title ∪ index` routing. When
  the deferred title-persistence work lands, adding the DB title column to the Title path MUST be an
  additive change to this feature, not a rewrite of it. This feature does not implement that union
  and does not widen the index-lag gap that exists today
  ([ADR-0018 alignment](#adr-0018-alignment)).

**Literal-text search terms (#37532)**

- **FR-027**: A search term MUST be matched as literal text. **Every** clause built from the term
  MUST escape the full Lucene `query_string` reserved set — `\ + - ! ( ) : ^ [ ] " { } ~ * ? | & /`
  — not only the last one, and the mandatory gate in particular. The escaping MUST use the shared,
  vendor-neutral utility rather than a strategy-private character set, so the reserved set has one
  definition and `/` is covered. Wildcards the system adds itself MUST be applied **after** escaping
  so they are not themselves escaped. This applies to **both** search scopes: the Title clause
  introduced by FR-010 is subject to it exactly as the existing all-fields clause is.
- **FR-028**: Consecutive separators in a term MUST NOT produce empty clauses. A term that reduces
  to no usable token MUST produce no text clause at all, rather than a clause with an empty value.
- **FR-029**: ~~A search request whose query fails to execute MUST surface an error state to the
  author. It MUST NOT be reported as a successful search that found nothing. The failure MUST
  remain logged, but logging MUST NOT be the only response to it.~~ **Amended 2026-09-14.** The
  front end MUST surface, as an error state rather than an empty result, every search failure it
  can itself observe (network and server errors reaching the browser). The **browsing service**
  (`BrowserAPIImpl`) MUST NOT be required to distinguish "the query failed" from "nothing matched"
  for its own internal execution failures — that distinction MUST remain logged only, exactly as
  before this feature.

  Narrowed after building the stronger version and reverting it, for two reasons: (1) FR-027's
  escaping removes every user-reachable way to break the query, so what remained was infrastructure
  failure only, forceable through the public API solely via a term wide enough to hit the
  Elasticsearch boolean-clause ceiling; (2) raising it broke a security guarantee —
  `ContentDriveFieldFilterTest#testMalformedDateBoundIsSafe` requires a Lucene-injection attempt to
  be escaped, match nothing, and produce no 500, and surfacing query failures turned that into an
  error response, telling an attacker their probe had landed. Consequence: #37532's UI criterion is
  met for observable front-end failures, not for every internal query failure.
- **FR-030**: Content Drive's per-field filters MUST match terms literally on the same reserved set
  as FR-027, and this MUST be confirmed by test rather than by inspection. The implementation
  already satisfies this; the requirement exists so a regression would be caught and so #37532's
  corresponding criterion is demonstrably met.
- **FR-031**: The all-fields search behaviour MUST remain shared with the Search portlet and the
  Relationships dialog. FR-027's escaping corrects a defect in that shared path and therefore
  benefits them too; it MUST NOT be implemented as a Content-Drive-only branch that leaves the
  shared strategy broken for its other consumers.

### Key Entities

- **Search scope**: Which fields of a document a Content Drive search term is matched against. Two
  values — *Title* and *All Fields* — with *All Fields* as the default. Lives alongside the search
  term as part of the drive's filter state, is carried in the address, and is sent with the search
  request as `filters.searchScope`. Named in full throughout: Content Drive is separately gaining a
  **browse scope** (#37426) that says *where* you are browsing. The two are independent — a browse
  scope says where you are, a search scope says how a search reads what is there.
- **Content Drive search request**: The existing description of what the drive should list —
  location, term, content types, languages, status, workflow, per-field criteria, sort, paging. Its
  `filters` object today holds `text` and `filterFolders`, both of which qualify the text search;
  the search scope joins them as a third, equally text-dependent member.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On a drive containing a document whose body mentions the search term and a document
  whose name is the search term, an author in **Title** scope sees only the second — verified as a
  binary pass on a seeded dataset.
- **SC-002**: For any term and filter combination **that contains no Lucene reserved character and
  no consecutive separator**, All Fields results are byte-identical to the results the same drive
  returns before this change — zero regressions across the search cases covered by the endpoint's
  test suite. Terms inside the FR-009 carve-out are measured by SC-009 and SC-010 instead, and the
  set of tests that change is enumerated in the PR rather than left implicit.
- **SC-003**: On a large dataset, a **Title** search returns its first page faster than the same
  term in **All Fields** scope, and the before/after comparison is recorded on the issue. The
  target is a measurable reduction, not a fixed threshold; the comparison itself is the deliverable
  the issue asks for.
- **SC-004**: A Content Drive address carrying a search scope reproduces the same narrowed view for
  a second user 100% of the time, and reload and Back/Forward preserve the search scope in 100% of
  attempts.
- **SC-005**: 100% of search requests that omit `filters.searchScope` produce today's results —
  confirmed by an explicit endpoint test for the omitted field, not only by the explicit all-fields
  case.
- **SC-006**: The Asset Picker's search box shows no search scope control and its search behavior is
  unchanged, confirmed by its existing tests passing without modification.
- **SC-007**: An author who knows the name of the item they want reaches it from the search box
  without scrolling past unrelated body matches, on a drive where the all-fields search for the
  same term returns more than one page.
- **SC-008**: Every caller of the shared content listing is accounted for by name rather than by a
  blanket claim, and the three that are not Content Drive or the Asset Picker return identical
  results before and after the change.
- **SC-009**: Searching the exact headline reported on customer ticket 39185 —
  `ABC Bank (XETRA: DBKGn.DB / NYSE: DB) and PSL Launch independent European CLO Total Return Indices`
  — returns the item, in both search scopes, as a regression test that fails before the change and
  passes after it.
- **SC-010**: Every character in the reserved set `\ + - ! ( ) : ^ [ ] " { } ~ * ? | & /` is
  covered by a test that seeds a title containing it and finds that title by searching for it
  verbatim — the full set, not a sample, in both search scopes.
- **SC-011**: A search whose query fails to execute produces a visible error state in 100% of
  attempts, and zero of those attempts render as a successful empty result list.
- **SC-012**: Content Drive's per-field filters pass the same reserved-set coverage as SC-010,
  confirming #37532's field-filter criterion by test rather than by inspection.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: Content Drive's keyword search, and the browsing service that
  backs it. The browsing service is long-standing, pre-Content-Drive product surface shared with
  other file-browsing entry points; Content Drive's search endpoint and its front end are recent.
  The shared search box is also used by the Asset Picker, which is explicitly not in scope. Two
  callers reach the Content Drive search endpoint (Content Drive and the Asset Picker); three more
  reach the same underlying listing by other doors (FR-024).
- **Backward-compatibility expectations**: Strict. All-fields search must be unchanged for every
  existing caller and every existing address; the search scope is additive and optional at every
  layer, and its absence must be indistinguishable from today. No existing behavior is deprecated.
  The all-fields query strategy is shared with the Search portlet and the Relationships dialog and
  must keep serving them unmodified — the Title scope is a sibling path, not a branch inside the
  existing one.
- **Legacy surface deliberately left as-is**: the **Content Search portlet** keeps the behaviour
  #37532 reports. Its `ContentletAjax.searchContentletsByUser()` field-filter branch
  (`ContentletAjax.java:874-906`) still builds unescaped per-token clauses and still swallows the
  parse failure at `:1058`. #37532 frames the legacy construction as something to replace rather
  than patch, and names Content Drive as where the improved behaviour should land — so this spec
  improves the replacement, not the thing being replaced. **Consequence to accept consciously**: a
  user filtering in the Content Search portlet still sees a silent empty result set until Content
  Drive replaces it. See [Why #37532 lands here](#why-37532-lands-here).
- **Shared-path defect, shared-path fix**: FR-027 corrects `GlobalSearchAttributeStrategy`, which
  also serves the Search portlet and the Relationships dialog. Unlike the Title scope — a sibling
  path deliberately kept out of the shared strategy — the escaping defect is *in* the shared
  strategy and is fixed there, so every consumer stops mis-parsing reserved characters. This is the
  one place where this feature intentionally changes behaviour beyond Content Drive, and FR-031
  states it so it is not mistaken for scope creep.
- **Known related decisions**: [#36688](https://github.com/dotCMS/core/issues/36688) deliberately
  replaced a broad leading-wildcard all-fields query with the current strategy, for the same cost
  reasons argued here — the Title scope must not reintroduce what that issue removed.
  [#36814](https://github.com/dotCMS/core/issues/36814) tracks search performance at scale and is
  where SC-003's measurement belongs. **ADR-0018** (Database-First Content Drive Search) is the one
  ADR this feature touches; its `DB ∪ Index` title routing is addressed explicitly in
  [ADR-0018 alignment](#adr-0018-alignment) and deferred with reasons, not by omission.
  [#37426](https://github.com/dotCMS/core/issues/37426) lands a **browse** scope on the same request
  object; the naming split in FR-023 is agreed between the two specs.
- **Contract debt deliberately not taken on**: the Content Drive request also lets a caller say the
  same thing twice (`archived: true` alongside `status: ["ARCHIVED"]`, reconciled in
  `ContentDriveHelper`), and carries `live` as a boolean that selects a *version* rather than
  filtering anything; six fields that genuinely are filter-bar chips sit at the top level rather
  than in `filters`. That cleanup is worth doing while the endpoint still has two callers, but it is
  a contract change of its own and neither this feature nor #37426 should carry it.

## Assumptions

- **File assets remain findable by name in Title scope, through their title.** Decision 2 excludes
  `fileName` and `metadata.name`, but a file asset carries a title field that dotCMS keeps in step
  with the file name — it is seeded from it and rewritten on rename
  (`FileAssetAPIImpl.java:498`). So searching a file by its name works in Title scope for the
  ordinary case. The gap is narrow and deliberate: a file whose title has been edited to something
  other than its file name will not match on the file name. If that gap proves to matter in
  practice, widening Title scope to cover file names is a follow-up with its own spec, not a
  silent change here.
- **Read-your-writes for text search stays where ADR-0018 put it.** The `contentlet.title` column
  is not reliably populated today and the ADR defers the work that would make it so. This feature
  assumes it will remain unpopulated for the life of this implementation, and that Title scope's
  index-only matching is therefore no worse than the all-fields search it sits beside.
- **The issue's ASCII diagram is the whole design input.** No mock image or design file exists for
  this control, and none is being waited on. The spec fixes which components are present and how
  they behave; their appearance is settled during implementation.
- **The search scope is a front-end-visible concept only for Content Drive.** No other portlet gains
  it in this work, and the query strategy shared with the Search portlet and the Relationships
  dialog is left as-is.
- **"Title" is the label authors understand**, including for folders and files, and does not need to
  read differently per row type. **"All Fields"** is understood with the help of FR-022's
  explanation rather than on its own.
- **The existing address-encoding scheme for Content Drive filters can carry the search scope**
  without a new mechanism, and an unknown value degrades to the default the same way an unknown
  status does today.
- **The measurement in SC-003 needs a dataset large enough for the difference to exceed noise.** The
  issue does not name one; producing it is part of the work, and the comparison is recorded on the
  issue rather than in the repository.
- **No database, index-mapping or content-model change is required.** The search scope selects
  between two ways of querying what is already indexed, and the escaping fix is a pure string
  transform over the term before it reaches the index.
- **Escaping the reserved set does not degrade the matching #36688 tuned.** The characters are made
  literal, not removed, so the mandatory catchall prefix gate and the boost structure keep the shape
  that issue settled. Terms free of reserved characters produce a byte-identical query, which is
  what makes SC-002's narrowed promise measurable.
- **A visible error state is reachable from where the failure happens.** FR-029 assumes the drive
  can distinguish "query failed" from "nothing matched" and carry that to the UI. Today the
  distinction is destroyed at `BrowserAPIImpl:893-895`, which returns an empty set for both; how the
  signal is carried out is an implementation decision, but the two cases must stop being the same
  value.
- **Closing #37532 on this work is the team's call, not this spec's.** The spec delivers every one
  of its acceptance criteria that applies to Content Drive, and states plainly which two do not.
  Whether that is enough to close the issue against the customer ticket is a decision for the issue
  owner.
