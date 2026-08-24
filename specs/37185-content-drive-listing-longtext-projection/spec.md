# Issue Resolution Specification: Content Drive listing projection carries long-text field values the grid does not render

**Feature Branch**: `37148-drive-listing-longtext-projection`

**Created**: 2026-08-21

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [dotCMS/core#37185](https://github.com/dotCMS/core/issues/37185). Parent epic: [#36814](https://github.com/dotCMS/core/issues/36814). Originally investigated as item 3 of [dotCMS/core#37148](https://github.com/dotCMS/core/issues/37148).

**Input**: Item 3 of issue #37148 ("the listing projection carries long-text field values the grid never renders"), as corrected by the two follow-up comments on that issue.

<!--
  Scope guard: issue #37148 tracks FOUR items. This spec covers ITEM 3 ONLY. Items 1
  (candidate-scan query plan instability), 2 (field-filter chunk multiplier) and 4 (per-row
  user_ lookup) are explicitly out of scope here — see "Fix Scope & Non-Goals".
-->

## Problem Statement *(mandatory)*

Every row that `POST /api/v1/drive/search` returns for a **generic Content** item carries the
full value of that contentlet's long-text fields — WYSIWYG, TextArea and Story Block bodies —
even though the listing is a grid of names, types and dates.

On the benchmarked dataset a single 40-row page measured **159 KB, of which 116 KB (≈73%) was
the `body` field alone**. Issue #37148 attributes ≈65 ms per request to this on folders whose
content has long bodies, against a warm-instance total of 332 ms for the unfiltered case.

The cost is paid on serialization, on the wire, and in browser memory: both frontend consumers
store the returned `list` array verbatim (`dot-content-drive.store.ts:498`,
`with-asset-browse.feature.ts:181`), so the bodies are retained for as long as the page is open.

**Severity / Impact**: Medium. Affects every backend user browsing a folder that contains
generic Content with long-text fields, in both the Content Drive portlet and Site Browser (they
share the code path — see Blast radius). It is a latency and bandwidth tax, not a correctness
defect: no data is wrong, there is just far more of it than any consumer asked for. It is worst
for remote/low-bandwidth editors and it scales with page size.

## Reproduction *(mandatory)*

**Environment**: dotCMS `main` (verified at commit `87745ff315`). PostgreSQL + Elasticsearch.
Community license. Measurements in issue #37148 were taken on a local Docker instance against a
production-scale dataset (~418k contentlets, ~14k folders, ~411k identifiers), warm (30 warm-up
requests discarded — absolute figures move a lot on a cold instance, ratios do not).

**Steps to Reproduce**:

1. Create or locate a folder containing contentlets of a **generic Content** type (base type
   `CONTENT` — not File Asset, not dotAsset, not Page) that has a WYSIWYG or TextArea field, and
   populate that field with a realistic article-length body.
2. As a backend user, `POST /api/v1/drive/search` with `assetPath` pointing at that folder and
   `maxResults: 40`.
3. Inspect the response body: measure total size, then measure the size contributed by the
   long-text field's key (e.g. `body`) across the 40 entries of `entity.list`.
4. Open the Content Drive portlet on the same folder and compare against the columns actually
   rendered by the grid.

**Expected Behavior**: The listing response carries the fields the listing needs — identity,
title, type, language, workflow/version state, mod user, mod date, and whatever columns the grid
is configured to show. Long-text bodies that nothing displays are not transferred.

**Actual Behavior**: Each row is effectively a full contentlet map. The long-text field values
are present in full; ≈73% of the measured payload was long-text content.

**Reproducibility**: Always, for any generic Content row with a non-empty long-text field. It
does **not** reproduce for File Asset, dotAsset or Page rows — those go through narrower
transform option sets (see Root-Cause Hypothesis), which is why the issue describes them as
"already fast".

## Scope of Investigation *(mandatory)*

- **Affected area**: Folder/content listing for the **Content Drive** REST endpoint
  (`POST /api/v1/drive/search`) and, because the code is shared, the **Site Browser** listing
  (`POST /api/v1/browser`) and the legacy DWR Site Browser. The mechanism sits in the
  Contentlet-to-Map transform layer.
- **Suspected surface**: **Both modern and legacy.** The listing API is modern
  (`com.dotcms.browser.BrowserAPIImpl`, `com.dotcms.rest.api.v1.drive.*`), but the transform
  machinery that produces each row is legacy
  (`com.dotmarketing.portlets.contentlet.transform.*`). Per Constitution Principle I, the
  transform package must be improved incrementally, not restructured.
- **Related known decisions**: None identified as binding for this item. `/speckit-plan` will
  formally consult `dotCMS/platform-adrs`. Note that ADR-0018 (read-your-writes / DB vs index
  routing) is cited in issue #37148 for **item 2**, not for this item — this item does not change
  where data is read from, only which keys survive into the response.

## Root-Cause Hypothesis

The row map is seeded with the **entire** contentlet map and then decorated, so field values are
present by default and must be actively removed to be excluded.

1. `DotContentletTransformerImpl#transform` (`DotContentletTransformerImpl.java:101`) starts from
   `final Map<String, Object> map = copyContentlet.getMap();` — the full contentlet map — and then
   applies the resolved strategies to it.
2. Consequently `TransformOptions` are **additive decorations, not a projection**. Omitting an
   option does not omit a field; the only way a field leaves the map is if a strategy calls
   `map.remove(...)`.
3. `BrowserAPIImpl#dotContentMap` (`BrowserAPIImpl.java:2717`) uses
   `new DotTransformerBuilder().defaultOptions()`, whose option set
   (`DotContentletTransformerImpl.java:41-46`) is
   `COMMON_PROPS, CONSTANTS, VERSION_INFO, BINARIES, CATEGORIES_NAME, TAGS, STORY_BLOCK_VIEW, JSON_VIEW`
   — the broadest of the three branches, and the only one carrying `STORY_BLOCK_VIEW` and
   `JSON_VIEW`, which *expand* Story Block content rather than shrinking it.
4. The sibling branches in `BrowserAPIImpl#createContentMap` (`BrowserAPIImpl.java:1881-1893`)
   use `webAssetOptions()` / `dotAssetOptions()`, which are narrower — but per (2) that is **not**
   why they are small. They are small because File Asset, dotAsset and Page content types do not
   carry article-length long-text fields in the first place.

> **Correction to the issue text.** Issue #37148 item 3 says *"Restricting the listing projection
> (as `webAssetOptions()` already does for file assets and pages …) would remove it."* That is
> not how the transformer behaves. `webAssetOptions()` removes nothing; it merely decorates less.
> A fix must **remove keys**, which is a different (and slightly larger) change than swapping an
> option set. The precedent for removal already exists in the codebase — `FILTER_BINARIES`, see
> "Recommended direction".

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- **Decision (resolves OQ-1, 2026-08-24):** truncate long-text field values (option **iii** in
  the original OQ-1) rather than remove them. Bound: **150 characters**. Applied uniformly to
  every long-text field in the listing row, regardless of whether the field is flagged Show In
  List — a `listed` WYSIWYG/TextArea/Story Block field keeps rendering in its grid column, as a
  preview instead of the full body. This was chosen over unconditional removal because the grid
  today has no field-type restriction on Show In List
  (`dot-content-drive-field-filter-menu.component.ts:133`) and removing the key would blank that
  column silently; truncating preserves the column while still bounding payload. It was chosen
  over "preserve full body for listed fields only" because that makes the payload win
  configuration-dependent — any content type with a listed long-text field would keep shipping
  full bodies. The frontend already visually clips these cells with CSS `truncate`
  (`dot-folder-list-view.component.html:215`, Tailwind `overflow:hidden`/`text-overflow:ellipsis`)
  — that is display-only clipping of the full string already in the DOM/response, so it does not
  reduce payload or memory on its own; server-side truncation is what actually removes the bytes.
  No frontend change is required: the component reads whatever string is in `item[column.field]`
  and applies the same CSS truncation to a short string as it does to a long one today.
- Truncate long-text field values in the listing rows produced by
  `BrowserAPIImpl#createContentMap`'s generic-Content branch (`dotContentMap`) to 150 characters.
- Keep every field the Content Drive grid, its toolbar/action menu, and the asset-picker
  actually consume.
- Update the `@Operation`/`@Schema` documentation on `ContentDriveResource#search` so the
  documented response matches what is returned (see the caveat in AC-005).
- Update or remove the dead `item.body` read in the Postman collection (see AC-006).
- Add the regression coverage that does not exist today (see "Verification method").

**Explicitly out of scope / non-goals**:

- **Items 1, 2 and 4 of issue #37148** — the candidate-scan query plan instability, the
  field-filter chunk multiplier, and the per-row `user_` lookup. Each lands as its own PR. The
  issue's own guidance is to sequence them behind item 1 and re-evaluate; this spec does not
  depend on any of them and does not change their behavior.
- Restructuring the transform package, changing how `DotContentletTransformerImpl` seeds its map,
  or converting `TransformOptions` from a decoration model into a real projection model. That is
  a wholesale legacy rewrite (Constitution Principle I) and is not needed to fix this.
- Changing which fields `POST /api/v1/content/_search`, GraphQL, `ContentResource`, the Content
  Editor, or any other transformer consumer returns. Any new option must be **opt-in** so no
  other caller's payload changes.
- Adding pagination, compression, or HTTP-level response tuning to the endpoint.
- Reducing the *other* 27% of the payload.

## Regression Risk *(mandatory)*

- **Blast radius** — `dotContentMap` is **not** Drive-only. It is reached through
  `hydrate` (`BrowserAPIImpl.java:1803`) → `createContentMap` (`:1881`) → `dotContentMap` (`:2717`),
  and `hydrate` has two callers, `getFolderContent` (`:1653`) and `getPaginatedContents` (via
  `hydrateContentletsInParallel`, `:1145`). Reaching them:

  | Consumer | Entry point | Reaches `dotContentMap`? |
  |---|---|---|
  | `ContentDriveHelper:222` | `POST /api/v1/drive/search` | **Yes** — Content Drive portlet browses all base types |
  | `BrowserResource:151` | `POST /api/v1/browser` (Site Browser) | **Yes**, when non-asset content is allowed through |
  | `BrowserAjax` (DWR) | legacy Dojo Site Browser / legacy file dialog | Yes in principle; calls pass `onlyFiles=true` in practice |
  | `DotCMSMacroWebAPI:73` | **Velocity viewtool** (`implements ViewTool`) | Yes in principle; passes `onlyFiles=true` |
  | Asset picker / File field | `POST /api/v1/drive/search` | **No** — pinned to `['DOTASSET','FILEASSET']` (`asset-picker-config.ts:25-28`, `:109`), so it takes the `fileAssetMap`/`dotAssetMap` branches |
  | OSGi plugins | `APILocator.getBrowserAPI()` | Possible — `BrowserAPI` is a public interface; unenumerable |

  So a change at `dotContentMap` changes **Site Browser too**, and is reachable from Velocity and
  from OSGi. Issue #37148's item-3 write-up does not mention this; only its item-1 "Scope note"
  does. **OQ-2** asks whether the two endpoints should be allowed to diverge.

- **The Content Drive grid *does* render arbitrary content-type fields — including long-text ones.**
  This contradicts the issue's premise and is the main functional risk. When exactly one content
  type is selected, the grid appends an extra column for **every** field the type flags "Show In
  List": `fields.filter((field) => field.listed)`
  (`dot-content-drive-field-filter-menu.component.ts:133`), mapped to columns in
  `dot-content-drive-shell.component.ts:414-424` keyed by `field.variable`. **There is no
  field-type restriction on that filter.** A content type with a `listed` WYSIWYG, TextArea or
  Story Block field therefore renders that value in the grid today, and an unconditional removal
  would blank the column. See **OQ-1** — this is the decision that shapes the whole fix.

- **Backward compatibility**:
  - `POST /api/v1/drive/search` is declared internal: `@Hidden` (`ContentDriveResource.java:75`)
    and documented as *"INTERNAL API — NOT FOR EXTERNAL USE … No backward compatibility
    guarantees"* (`:44-54`). Being `@Hidden`, it is **absent from
    `src/main/webapp/WEB-INF/openapi/openapi.yaml`** (verified: no `v1/drive` entry), so no
    OpenAPI regeneration is required for the Drive endpoint.
  - `POST /api/v1/browser` (Site Browser) carries no such internal-API disclaimer. If the change
    reaches it (OQ-2), that is the contract with the weaker escape hatch.
  - `BrowserAPI` is a public `com.dotcms.browser` interface reachable via `APILocator`, and the
    map is handed to Velocity through `DotCMSMacroWebAPI` — so the response shape is also an
    OSGi (M-4) and VTL (H-8) surface, not only a REST (M-3) one.
  - Frontend typing gives **no** safety signal in either direction: `DotCMSContentlet` declares
    `body?: string` (`dot-contentlet.model.ts:50`) as optional and has an open
    `[key: string]: any` index signature (`:57`).
  - **Rollback classification is not as stated in the issue.** Issue #37148's item-3 acceptance
    criteria say *"Labelled for rollback safety, since this changes a REST response shape."* Per
    `docs/core/ROLLBACK_UNSAFE_CATEGORIES.md`, M-3 is rollback-unsafe because **N-1 lacks a
    contract that N introduced**. Here N *removes* a field, so rolling back to N-1 **restores**
    it — this is a forward-compatibility concern (breaks on N if something reads the field), not
    a rollback one, exactly as the H-8 removal note spells out. On the analysis in this spec the
    change looks **rollback-safe**. See **OQ-6**; note also that dotCMS/core auto-applies
    rollback-safety labels to PRs, so this should not be hand-set.

- **Data considerations**: None. No schema change, no ES mapping change, no stored data is
  transformed or migrated. Nothing needs repair on downgrade.

- **Test coverage is effectively absent today**, which cuts both ways — the change will break no
  existing test, and no existing test will catch collateral damage:
  - No test anywhere (Java unit, Java integration, Postman, or Jest) asserts on the full key set
    of a listing row, or asserts that any field is absent.
  - There is no `ContentDriveResourceTest` and no `ContentDriveHelperTest`.
  - `dotCMS/src/test/.../BrowserAPIImplTest.java` covers only ES query-string escaping.
  - `dotcms-integration/.../BrowserAPITest.java` asserts only identity/metadata keys (`total`,
    `list`, `inode`, `identifier`, `name`, `baseType`, `owner`, `extension`, `languageId`, `url`,
    `pageURI`, `path`) — and the assertions that touch `owner`/`extension`/`url` exercise the
    folder and web-asset branches, **not** `dotContentMap`. The generic-Content branch's output
    shape is pinned by nothing.
  - **Correction to the issue.** The second comment on #37148 states the Postman collection
    *"has one test [that] filters results using the content body"* and concludes *"the field is
    part of the asserted contract."* It is **not**. In
    `ContentDriveResource.postman_collection.json`, folder *"Search and Filtering Tests"* →
    request *"Text Search - Alpha Filter"* → test *"Search filters out non-matching items"*, the
    `var body = item.body || '';` read (line 997) feeds a local `allItemsMatch` variable that is
    **computed and then never asserted** — `allItemsMatch` appears exactly once in the whole file
    (line 995, its own declaration), and the only `pm.expect` in that test is
    `pm.expect(list.length).to.be.at.most(10)`. The `item.body` read is dead code. This resolves
    the "intentional or incidental?" question the issue raised in favour of **incidental**, and it
    means the projection change is contained on the test side.

## Acceptance & Verification *(mandatory)*

- **AC-001**: For the reproduction above, long-text field values in the response are truncated to
  150 characters rather than transferred in full. Payload for a 40-row page of long-body generic
  Content drops by at least **half** versus the same request on the pre-change build (in practice
  far more than half, since bodies measured in the hundreds-to-thousands of characters collapse to
  ≤150), measured on the same folder and dataset.
- **AC-002 (blast-radius regression)**: Every field the Content Drive grid, its toolbar and its
  action menu consume is still present and unchanged — at minimum `identifier`, `inode`, `title`,
  `contentType`, `baseType`, `languageId`, `live`, `working`, `archived`, `hasLiveVersion`,
  `modUser`, `modUserName`, `modDate`, `permissions`, `__icon__`, `mimeType`, `extension`,
  `hasTitleImage`, `owner`, `url`/`path` where applicable. The exact list is derived from
  `DotFolderListViewColumnField` (`dot-folder-list-view/models.ts`) plus the action-menu inputs,
  and is pinned by a test (see below) rather than by inspection.
- **AC-003 (Show In List)**: A content type with a `listed` long-text field still renders that
  grid column; its cell value is the 150-character truncation, not the full body and not blank.
  Covered by a test asserting both that the key is present and that its length is bounded.
- **AC-004 (Site Browser)**: `POST /api/v1/browser` behaviour matches whatever **OQ-2** resolves
  to. If the reduction is intended to apply there too, Site Browser listings are verified
  unchanged in every field it renders and no slower; if not, Site Browser rows are verified
  byte-identical to the pre-change build.
- **AC-005 (`@Schema`)**: The endpoint's Swagger annotation is accurate.
  *Caveat on the issue's wording:* item 3's AC *"The `@Schema` annotation on the endpoint matches
  the reduced payload"* is close to a no-op as the code stands. The annotation is
  `@Schema(type = "object", description = "Drive search response containing filtered assets,
  folders, and navigation metadata with content type filtering")`
  (`ContentDriveResource.java:61-62`) — it enumerates no fields, so there is no field list to
  reduce, and per `dotCMS/src/main/java/com/dotcms/rest/CLAUDE.md` `type = "object"` + description
  is the *sanctioned* pattern for a `Map<String, Object>`-shaped response. Concretely this AC is
  satisfied by updating the `description` (and/or `@Operation.description`) to state that listing
  rows carry a reduced projection, and by confirming no `openapi.yaml` diff is produced (the
  endpoint is `@Hidden`). If the team wants a typed schema instead, that is a larger change and
  belongs in **OQ-5**.
- **AC-006 (Postman)**: The dead `item.body` read in *"Search and Filtering Tests" → "Text Search
  - Alpha Filter" → "Search filters out non-matching items"* is removed or replaced with an
  assertion that actually runs, so the collection no longer implies a contract it never enforced.
  The other 59 assertions (on `contentCount`, `folderCount`, `title`, `identifier`,
  `contentType`) still pass unchanged.
- **AC-007 (no collateral consumers)**: `POST /api/v1/content/_search`, GraphQL, the Content
  Editor, and the asset picker / File field are unaffected — verified by the new option being
  opt-in and by the asset picker's base-type pinning, not by assumption.
- **AC-008 (title safety)**: A content type whose **title field is itself** a WYSIWYG or TextArea
  field still returns a correct `title` in the listing. (`COMMON_PROPS` populates `title`
  independently of the field key, so this is expected to hold — but it is unverified and is the
  one place where removing a long-text key could plausibly blank a rendered column even under
  OQ-1's narrowest answer.)

**Verification method**:

- **Integration (new)** — `dotcms-integration`, alongside
  `com.dotcms.browser.BrowserAPITest`: a test that pins the key set of a **generic Content** row
  from `getPaginatedContents` (Drive path) and from `getFolderContent` (Site Browser path). This
  is the coverage that does not exist today. It must assert both directions: required keys
  present (AC-002) *and* long-text keys absent (AC-001). Run targeted:
  `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=BrowserAPITest`
  (plus the new test class, once named).
- **Unit (new)** — `dotCMS/src/test`, in the transform package: the option's removal logic over a
  content type carrying WYSIWYG + TextArea + Story Block + Text fields, asserting exactly which
  keys survive, and asserting that a transformer built **without** the new option is byte-identical
  to today (AC-007).
- **Postman** — `./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false
  -Dpostman.collections=ContentDriveResource` after the AC-006 edit.
- **Jest** — `dot-content-drive.store.spec.ts` and `with-asset-browse.feature.spec.ts` still pass;
  no new frontend code is expected (nothing reads the removed keys — verified: no
  `item.body`/`asset.body`/`content.body` read exists anywhere in `core-web` on a drive or browser
  result).
- **Manual / measurement** — re-run the #37148 measurement for the unfiltered large-folder case
  and record before/after payload size and p50/p95, per the issue's overall criterion that each
  item is re-measured after it lands. Requires the benchmark dataset; not reproducible in CI.

## Recommended direction *(non-binding — the plan decides)*

Issue #37148 sketched two implementations. Evaluated against the code:

**(a) Idiomatic — a new `TransformOptions` value. Recommended.** A close precedent already
exists: `DefaultTransformStrategy#addBinaries` (`DefaultTransformStrategy.java:222-236`) handles
`FILTER_BINARIES` at the same point in the same class —

```java
final List<Field> binaries = contentlet.getContentType().fields(BinaryField.class);
if (options.contains(FILTER_BINARIES)) {
    binaries.forEach(field -> map.remove(field.variable()));
    return;
}
```

The resolved operation for this item is a `map.put` with a truncated value, not a `map.remove` —
`WysiwygField.class`, `TextAreaField.class` and `StoryBlockField.class` fields (all three exist
under `com.dotcms.contenttype.model.field`) get their string value cut to 150 characters in place
of the removal shown above. The mechanism is otherwise identical: same strategy, same trigger
point, same per-field-type iteration. Because `DefaultTransformStrategy` already runs whenever
`COMMON_PROPS` is set — and `defaultOptions` sets it — **no new strategy class is needed**. This
makes (a) roughly **four** files, not the seven the issue estimated:
`TransformOptions.java` (one enum constant), `DefaultTransformStrategy.java` (~15 lines beside the
binaries block), `DotTransformerBuilder.java` (expose it), `BrowserAPIImpl.java` (opt in at
`dotContentMap`). The new option must **not** be added to
`DotContentletTransformerImpl.defaultOptions`, or every transformer consumer changes at once
(AC-007).

**(b) Contained — filter keys inside `BrowserAPIImpl`.** Rejected as the primary approach. It
would duplicate content-type field introspection that `DefaultTransformStrategy` already owns into
a 2700-line class with no other content-type-field responsibility, and it does not reduce the
blast radius at all — both approaches modify the same `dotContentMap`, so both change Site Browser
identically. (b) buys no containment; it only relocates the logic to a worse layer.

There is also a **third option the issue did not consider**, which OQ-1 may force: a
client-declared projection — a `fields` parameter on `DriveRequestForm` so the caller states which
field values it wants. This is the only approach that is correct by construction under "Show In
List" columns, since the frontend already knows exactly which extra columns it is about to render.
It is a larger, additive API change. Recorded as **OQ-3**.

## Open questions *(must be answered before `/speckit-plan` finalizes)*

Each of these is a decision a human owner must make. None is guessed here.

- **OQ-1 — RESOLVED (2026-08-24).** Decision: option **(iii)**, truncate to **150 characters**,
  applied uniformly whether or not the field is `listed`. See "Decision (resolves OQ-1)" under
  Fix Scope & Non-Goals for the reasoning. Options (i) remove-unconditionally and (ii)
  preserve-full-body-if-listed were rejected — (i) blanks a configured column silently, (ii)
  makes the payload win configuration-dependent. Option (iv), client-declared projection, remains
  recorded as OQ-3 for a possible future generalization but is not required to close this item.
- **OQ-2 — Should Site Browser change too?** `dotContentMap` is shared, so the default outcome is
  that `POST /api/v1/browser` gets the same reduction. Is that desired (consistent, and Site
  Browser benefits too), or must Site Browser be held byte-identical — which requires splitting
  the generic-Content branch so Drive and Site Browser use different option sets, and means the
  two endpoints' row shapes permanently diverge? Note Site Browser has no internal-API
  disclaimer, while the Drive endpoint does.
- **OQ-3 — Client-declared projection instead of a server-side denylist?** Should
  `DriveRequestForm` gain an optional `fields` parameter so the caller lists the field values it
  needs (the frontend already knows its `showInListFields`)? This resolves OQ-1 by construction
  and generalizes, but it is an additive API change with its own design and a default-behaviour
  decision (omitted `fields` = today's full map, or = the reduced projection?).
- **OQ-4 — Which field types count as "long text"?** The issue names WYSIWYG, TextArea and Story
  Block. Should the set also include Custom Field, Key/Value, JSON-typed fields, Constant fields
  holding long values, or plain Text fields (which are length-capped but numerous)? Defining this
  as a fixed list of `Field` classes is simple and predictable; deriving it from `dataType`
  (`LONG_TEXT`) is broader and would catch future field types automatically but is harder to
  reason about. Which does the team want?
- **OQ-5 — Typed response schema?** AC-005 as written only updates a prose description, because
  the endpoint's `@Schema` is `type = "object"` with no field list and the endpoint is `@Hidden`
  (so it is not even in `openapi.yaml`). Is that sufficient to satisfy the issue's `@Schema`
  criterion, or does the team want a real typed view class for the listing row — which would be a
  meaningfully larger change and would start documenting an endpoint deliberately marked internal?
- **OQ-6 — Rollback labelling.** The issue's AC says to label this rollback-unsafe as an API
  contract change (M-3). This spec's reading of `docs/core/ROLLBACK_UNSAFE_CATEGORIES.md` is that
  removing a field is a **forward**-compatibility risk, not a rollback risk (N-1 restores the
  field), and that the change is therefore rollback-**safe** — the same reasoning the H-8 removal
  note makes explicit. Does the team accept that reclassification? (Separately: dotCMS/core
  auto-applies rollback-safety labels to PRs within ~5 minutes, so these should not be hand-set.)
- **OQ-7 — Config escape hatch?** Should the reduction sit behind a `Config` property so an
  operator can restore the old payload without a redeploy — useful given the endpoint feeds the
  admin UI and the blast radius includes Site Browser and OSGi — or is a feature flag here
  unjustified complexity for a payload reduction?

## Assumptions

- The measurements quoted (159 KB / 116 KB / ≈73% / ≈65 ms) are taken from issue #37148 as
  reported. They were **not** re-verified here — no dotCMS instance or benchmark dataset was
  available. Per the issue's own second comment, absolute figures are warm-instance values and
  vary with instance warmth; the ratios reproduced across runs.
- All code-level claims in this spec **were** verified by reading `main` at commit `87745ff315`,
  and file:line references point at that state.
- "Generic Content" means base type `CONTENT` — the `else` branch of
  `BrowserAPIImpl#createContentMap` (`:1891-1892`). File Asset, dotAsset and Page rows are
  unaffected by any change scoped to `dotContentMap`.
- The Content Drive grid's fixed columns are the closed set in `DotFolderListViewColumnField`
  (`title`, `live`, `languageId`, `contentType`, `modUser`, `modDate`, `actions`); the action
  menu and toolbar consume additional identity/permission keys, which AC-002 pins by test rather
  than by this list.
- No OSGi plugin in this repository calls `APILocator.getBrowserAPI()`. Customer plugins that do
  cannot be enumerated, so the OSGi exposure is acknowledged as unbounded rather than assessed.
