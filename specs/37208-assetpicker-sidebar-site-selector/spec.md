# Feature Specification: AssetPicker — new sidebar UI: site selector + site-scoped folder search above the folder tree

**Feature Branch**: `nicobytes/assetpicker-new-sidebar-ui`

**Created**: 2026-08-25

**Status**: Draft

**Type**: New Feature

**Related GitHub Issue**: [#37208](https://github.com/dotCMS/core/issues/37208) — part of epic [#36702](https://github.com/dotCMS/core/issues/36702) (Create AssetPicker component reusing Content Drive)

**Input**: User description: "Crear el spec para el issue 37208 — el gran cambio es que la sección izquierda del AssetPicker ahora tiene un selector de sitio con su propio buscador, y un buscador de carpetas que muestra resultados planos (nombre + ruta completa) reutilizando lo que ya renderiza el folder tree."

## Context

The AssetPicker dialog has two columns. The **right** column (asset search, content-type / workflow / locale chips, results table, pagination) already matches the product design and is **out of scope**. This feature replaces the **left** column.

**Today** the left column is two controls:

1. one search box labelled *"Search sites & folders"*, and
2. a folder tree whose **roots are every site the editor is allowed to browse**.

A single term drives both halves at once: it filters the site list *and*, in parallel, lists matching folders flat under the currently browsed site. Sites and folders are mixed into one control, and "which site am I browsing" is implicit — whichever root the editor happened to expand.

**The new design** splits that single control into three stacked, single-purpose controls: a site selector, a folder search, and a folder tree rooted at one site. The picker becomes **pinned to one site at a time**, chosen explicitly — the same model Content Drive already uses via the global site switcher.

**A second, related surface is in scope.** The Site/Folder field's picker overlay *already* renders exactly the flat, two-line search-result rows this design calls for (folder name in bold, full path underneath), but it builds them inline in its own template. Rather than write that presentation a second time for the AssetPicker, it is extracted once and both surfaces consume it — see User Story 5.

**Three surfaces browse folders; two of them are in scope.** The goal behind that extraction is UI unification across the AssetPicker sidebar, the Site/Folder field, and **Content Drive's sidebar**. Content Drive is deliberately **deferred**, not forgotten: today its sidebar is the folder tree alone — it has no folder search to unify, and its site is chosen by the toolbar's global site switcher rather than by a control in the sidebar. Giving it a folder search is new capability for that portlet, and moving its site control into the sidebar would be a navigation change to a shipped portlet. Both are out of scope here (FR-034). What this feature owes Content Drive is a shared building block it can adopt later **without rework** — so the block is designed for three consumers even though only two adopt it now.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Choose which site I am browsing (Priority: P1)

An editor opening the AssetPicker sees, at the top of the left column, a single control naming the site currently being browsed — a globe icon, the hostname, and a chevron. Opening it reveals a searchable list of **sites only**. Typing narrows that list by hostname; picking a site re-points the whole picker — the folder tree below and the asset results on the right — at the newly chosen site.

**Why this priority**: This is the control the other two depend on. Without an explicit "which site" the folder search has nothing to scope to and the tree has no root. It also removes the term-coupling that makes today's sidebar behave unpredictably, and it is the visible half of the design change.

**Independent Test**: Open the picker, confirm the selector names a site, open it, search for a different hostname, pick it, and confirm both the folder tree and the asset results re-scope to that site. Delivers "browse a different site without leaving the dialog" on its own — the capability today's tree provides only by accident of its roots.

**Acceptance Scenarios**:

1. **Given** the picker is open, **When** the editor looks at the top of the left column, **Then** a site selector is shown displaying the browsed site's hostname with a globe icon and a chevron, positioned above the folder search.
2. **Given** the site selector is open, **When** the editor types into its search box, **Then** only sites whose hostname matches are listed — no folders ever appear in this list.
3. **Given** an installation with more sites than fit in one page, **When** the editor scrolls the site list, **Then** further sites load on demand without the editor requesting them and without the dialog stalling.
4. **Given** the editor is browsing site A, **When** they pick site B in the selector, **Then** the folder tree reloads rooted at site B and the asset list on the right re-scopes to site B's root.
5. **Given** the picker is opening, **When** a location was remembered from a previous pick, **Then** the selector preselects that remembered site.
6. **Given** the picker is opening, **When** no location is remembered — or the remembered site no longer exists or is no longer visible to this editor — **Then** the selector preselects the site currently active in the global site switcher.

---

### User Story 2 - Find a folder by name anywhere in the selected site (Priority: P1)

Directly under the site selector is a second input, *"Search folders…"*. Typing there searches folder **names** across the whole selected site, at any depth, matching anywhere in the name — not just at the start. The tree is replaced by a flat list of matches; each row shows the folder name on the first line and its full path underneath (`demo.dotcms.com / images / thumbnails`). Picking a row scopes the asset list to that folder **and leaves the result list up**, so the editor can try another match without retyping.

**Why this priority**: Finding a deep folder by name is the reason the search exists, and it is the half most broken today — the shared term makes the browsed site drop out of the results, so folder searches can come back looking empty. It is independently valuable the moment a site is pinned.

**Independent Test**: With a site selected, type a fragment that appears mid-name in several folders at different depths, confirm all of them are listed with their full paths, pick one, confirm the asset list re-scopes while the result list stays open with that row marked selected.

**Acceptance Scenarios**:

1. **Given** a site is selected, **When** the editor types a term into the folder search, **Then** the folder tree is replaced by a flat list of matching folders.
2. **Given** the site contains folders `activities`, `images`, `thumbnails`, `application` and `containers`, **When** the editor searches for a fragment that appears anywhere inside those names, **Then** all of them are returned — matching is *contains*, not *starts-with*.
3. **Given** `thumbnails` exists nested under `images`, **When** the editor searches `thumbnails`, **Then** it is returned even though it is not a direct child of the site root — the search is recursive across the whole site.
4. **Given** another site also contains a folder matching the term, **When** the search runs, **Then** that folder is **not** returned — results are scoped to the selected site only.
5. **Given** results are listed, **When** the editor reads a row, **Then** the folder name is on the first line and its full path on the second (`demo.dotcms.com / activities`); a path too long for the column is truncated with an ellipsis rather than wrapping or overflowing the sidebar.
6. **Given** results are listed, **When** the editor picks one, **Then** the asset list scopes to that folder, the result list stays visible, and the picked row is rendered as selected.
7. **Given** a term is active, **When** the editor clears the input — via the clear affordance or by deleting the text — **Then** the folder tree returns, showing the state it was in.
8. **Given** a term is active, **When** the editor changes the site in the selector, **Then** the term is cleared and the tree returns — a term is only meaningful against the site it was typed for.
9. **Given** a term matches nothing in the selected site, **When** the search completes, **Then** an explicit empty-state message is shown, not a blank panel.

---

### User Story 3 - Browse the selected site's folders from a single root (Priority: P2)

With no search term active, the left column shows a folder tree with exactly one root, labelled **All**, standing for the selected site's root. Expanding a node loads its children on demand; a level with more children than one page offers a *Load more*. Picking **All** scopes the asset list to the entire site.

**Why this priority**: Browsing already works today; this story is mostly a *removal* — site nodes stop being roots. It matters for coherence with the other two stories but delivers less new capability on its own, and the paging behaviour underneath it is unchanged.

**Independent Test**: With a site selected and no search term, confirm the tree shows one `All` root, expand two levels, confirm children load lazily and *Load more* appears where a level is truncated, and confirm picking `All` returns whole-site results.

**Acceptance Scenarios**:

1. **Given** a site is selected and no search term is active, **When** the tree renders, **Then** it has exactly one root, labelled `All`, representing that site's root.
2. **Given** the tree is rendered, **When** the editor inspects it, **Then** no site appears as a node — the tree contains only folders belonging to the selected site.
3. **Given** the `All` root is shown, **When** the editor selects it, **Then** the asset list scopes to the whole selected site.
4. **Given** a collapsed folder with children, **When** the editor expands it, **Then** its first page of children loads on demand.
5. **Given** a level with more children than one page, **When** the editor activates *Load more*, **Then** the next page is appended to that level, exactly as before this change.

---

### User Story 4 - Reopen the picker where I left off (Priority: P3)

Having picked an asset from a deep folder, the next time the editor opens the picker — from the same field or a different one — it opens on that site with the tree already expanded down to that folder, and that folder marked as selected.

**Why this priority**: Existing behaviour from epic #36702 that must survive the restructure. It is a regression guard rather than new capability, but it is the one most at risk: the remembered location now has to drive the *site selector*, not just an expanded root.

**Independent Test**: Pick an asset from a nested folder, close the picker, reopen it from a different field, and confirm the site selector names the remembered site and the tree is expanded to and highlighting the remembered folder.

**Acceptance Scenarios**:

1. **Given** a location was remembered, **When** the picker reopens, **Then** the site selector shows the remembered site and the tree is expanded down to the remembered folder with that folder marked selected.
2. **Given** a location was remembered but its site is no longer visible to this editor, **When** the picker reopens, **Then** it falls back to the global site switcher's current site with the tree collapsed at `All`, and no error is shown.

---

### User Story 5 - Folder search results look and behave the same everywhere (Priority: P3)

An editor who has used the Site/Folder field's picker recognises the AssetPicker's folder-search results immediately: the same two-line row, the same folder icon, the same bold name over a truncated path, the same selected and hover treatment. The presentation exists **once** in the product and both surfaces render from it.

**Why this priority**: No new capability on its own — the Site/Folder field must look and behave exactly as it does today after the change. Its value is preventing two near-identical implementations from drifting apart, and it is the reason the AssetPicker's result rows do not need to be designed from scratch. It is also the down payment on unifying the *third* surface, Content Drive, later. It is P3 because Stories 1–3 deliver the feature without it; it is nonetheless **required scope**, not an optional cleanup.

**Independent Test**: Search folders in the Site/Folder field's overlay and in the AssetPicker sidebar with the same term against the same site, and confirm the rows are visually and behaviourally identical; then confirm the Site/Folder field's existing search behaviour — including its own result paging — is unchanged from before.

**Acceptance Scenarios**:

1. **Given** the shared result presentation exists, **When** either the AssetPicker sidebar or the Site/Folder field renders folder-search results, **Then** both render from that single shared building block — the presentation is not duplicated per surface.
2. **Given** the Site/Folder field's overlay, **When** an editor searches folders after this change, **Then** the results look and behave exactly as they did before it, including the field's own result paging.
3. **Given** the shared building block, **When** a consumer needs different paging behaviour, **Then** it supplies that itself — the shared block carries presentation, not paging policy.
4. **Given** Content Drive's sidebar is not adopting the block in this feature, **When** it adopts it later, **Then** no change to the block itself is required to accommodate it — it is parameterised for a third consumer from the start, and is not coupled to the AssetPicker's or the Site/Folder field's own state, layout or paging.

---

### Edge Cases

- **Term typed, then the site changed**: the term is discarded and the tree returns (US2-8). The editor is never left looking at site A's results while the selector says site B.
- **More matches than one page of results**: the list shows the first page and tells the editor to narrow the term. It never pages silently past the cap and never implies the page is the whole result set (FR-020).
- **Term shorter than two characters**: treated as "no search" — the tree stays, no request is issued (FR-012). This is the existing behaviour of both the AssetPicker and the Site/Folder field, and it means the design's single-character screenshot is not reproduced literally.
- **Editor can browse exactly one site**: the selector still renders and still names that site; it simply has one entry. It is not hidden — the design's globe-and-hostname line is also the picker's "where am I" indicator.
- **Editor can browse no site at all**: the left column shows an empty state rather than an empty tree with no root, and the asset list on the right shows its own empty state.
- **The site list or the folder search fails to load**: the failure is surfaced to the editor. A failed load is never rendered as "this site has no folders" — the empty state and the error state are distinguishable.
- **Selected folder is deleted between two openings**: the remembered location silently falls back to the site root; the picker opens rather than erroring.
- **Very long hostname in the selector**: truncated with an ellipsis inside the control; the control never widens the sidebar or pushes the asset column.
- **Rapid typing in the folder search**: the editor sees results for the term they stopped on, not for an earlier keystroke that resolved late.

## Requirements *(mandatory)*

### Functional Requirements

**Site selector**

- **FR-001**: The left column MUST render a dedicated site selector at the top, above the folder search, showing the browsed site's hostname with a globe icon and a chevron affordance.
- **FR-002**: The site selector MUST provide its own search input that filters **sites only**; folders MUST never appear in it.
- **FR-003**: The site list MUST load lazily / in pages, so installations with hundreds of sites open the picker without delay.
- **FR-004**: The site selector MUST reuse the product's existing site-selection control rather than introducing a second, bespoke one, so site listing, filtering, paging and live site events behave identically to every other site selector in the product.
- **FR-005**: Choosing a site MUST load that site's folder tree and re-scope the asset results on the right to that site's root.
- **FR-006**: On open, the preselected site MUST be derived from the globally remembered last-used asset location; when none is remembered, or its site no longer exists or is no longer visible to the editor, the picker MUST fall back to the site currently active in the global site switcher.
- **FR-007**: The site selector MUST exclude the System Host, preserving today's behaviour — System Host is not addressable as a browse location, and its shared assets already surface in every site's listing.

**Folder search**

- **FR-008**: A second input, placeholder *"Search folders…"*, MUST sit directly under the site selector.
- **FR-009**: Folder matching MUST be **contains**, not prefix — a fragment appearing anywhere in a folder name is a match.
- **FR-010**: The search MUST be recursive across the whole selected site, at any nesting depth.
- **FR-011**: Results MUST be scoped to the selected site only; folders from other sites MUST NOT appear.
- **FR-012**: A term shorter than **two characters** MUST be treated as "no search": the folder tree stays visible and no search request is issued. This matches the Site/Folder field's existing threshold and the underlying folder-name search's own minimum.
- **FR-013**: While a term of two or more characters is active, the flat result list MUST **replace** the folder tree in the left column.
- **FR-014**: Each result row MUST show the folder name on the first line and its full path on the second, with a folder icon; a path too long for the column MUST truncate with an ellipsis rather than wrap or overflow.
- **FR-015**: Selecting a result MUST scope the asset list to that folder, keep the result list visible, and render the chosen row as selected.
- **FR-016**: Clearing the input — via the clear affordance or by emptying the text — MUST return the left column to the folder tree.
- **FR-017**: Changing the site while a term is active MUST clear the term and return to the tree.
- **FR-018**: An empty result set MUST render an explicit empty-state message, visually distinct from a load failure.
- **FR-019**: The results shown MUST correspond to the term the editor stopped typing on; superseded in-flight searches MUST NOT overwrite them.
- **FR-020**: The AssetPicker's result list MUST show at most one page of matches. When more matches exist than are shown, it MUST tell the editor so and prompt them to narrow the term; it MUST NOT truncate silently, and MUST NOT offer a "load more" that would page a different query than the one that produced the visible results.

**Folder tree**

- **FR-021**: With no term active, the tree MUST render exactly one root, labelled `All`, representing the selected site's root. It MUST carry the same **folder** affordance as the nodes beneath it, not a site/globe one — the globe now belongs to the site selector above, and repeating it on the root would show the same idea twice and read as if the root were a second site control.
- **FR-022**: Sites MUST NOT appear as tree nodes; the tree MUST contain only folders of the selected site.
- **FR-023**: Selecting `All` MUST scope the asset list to the whole selected site.
- **FR-024**: Lazy expansion and per-level *Load more* paging MUST keep working unchanged.
- **FR-025**: Reopening the picker on a remembered location MUST still expand the tree down to that folder and mark it selected.

**Shared folder-search result presentation**

- **FR-026**: The flat search-result row (folder icon, bold name, truncated full path, hover and selected states) MUST exist as a **single reusable building block** in the shared UI library, rendered from one definition rather than re-implemented per surface.
- **FR-027**: The Site/Folder field MUST be migrated to render its folder-search results from that shared building block, replacing its current inline implementation, with **no visible or behavioural change** to that field.
- **FR-028**: The shared building block MUST carry presentation only. Paging policy stays with each consumer: the Site/Folder field keeps paging its own results as it does today, while the AssetPicker caps at one page per FR-020.
- **FR-029**: The existing presentational folder tree MUST NOT be turned into a mode-switching component to satisfy FR-026 — it is shared with Content Drive and the Site/Folder field and is being changed concurrently by #37174.

**Scope guards / regression**

- **FR-030**: The right column — asset search, content-type / workflow / locale chips, results table, pagination — MUST be unchanged by this feature.
- **FR-031**: The silent mimetype restriction, content-type restrictions, and locale preselection introduced by epic #36702 MUST keep working for both the File and the Image entry points.
- **FR-032**: The new AssetPicker MUST remain absent from the legacy Dojo editor (see #37132) — this feature MUST NOT re-introduce it there.
- **FR-033**: Automated tests MUST cover, at minimum: changing the site, contains-matching search, site-scoped results, selecting a result keeping the list open, clearing the term, the single `All` root, and the Site/Folder field rendering unchanged from the shared building block.
- **FR-034**: Content Drive's sidebar MUST be unchanged by this feature — it gains no folder search, and its site continues to be chosen by the toolbar's global site switcher rather than by a control in the sidebar. The shared building block MUST nonetheless be free of any coupling that would prevent Content Drive adopting it in a follow-up.

### Key Entities

- **Browsed site**: the one site the picker is pinned to at a time. Identified by its identifier and displayed by its hostname. Chosen explicitly by the editor; seeded from the remembered location or the global site switcher. Everything else in the left column, and the scope of the right column, hangs off it.
- **Folder**: a node within the browsed site. Displayed by its name; addressed by its path within the site. Carries whether it has children still to load and whether a level has further pages.
- **Folder search term**: free text scoped to the browsed site. Meaningful only against the site it was typed for; discarded when that site changes.
- **Remembered location**: the site + folder the editor last picked an asset from — one value for the whole system, not one per field. Seeds the site selector and the tree expansion on the next open.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: From an open picker, an editor can reach a folder they know the name of — at any depth in the selected site — in **three interactions or fewer** (focus the search, type, pick a result), without expanding the tree by hand.
- **SC-002**: A folder search returns only folders from the selected site: **0%** of results across the test matrix belong to another site.
- **SC-003**: For fixtures whose matches fit within one page, **100%** of folders whose name contains the typed fragment, at any depth in the selected site, appear in the results — including nested ones invisible from the collapsed tree. Where matches exceed one page, the editor is told so in **100%** of cases rather than shown a silently truncated list.
- **SC-004**: Switching the browsed site updates both the folder view and the asset results to the new site, with **no** stale results from the previous site remaining visible.
- **SC-005**: On an installation with 500+ sites, the picker becomes usable — a site selector with content, a folder view — without the editor perceiving a stall, and scrolling the site list stays smooth.
- **SC-006**: After picking a search result, an editor can jump to a different matching folder **without retyping the term**.
- **SC-007**: Reopening the picker lands on the previously used site and folder in **100%** of cases where that location is still visible to the editor; where it is not, the picker opens on the global site's root and shows **no** error.
- **SC-008**: No path or hostname in the left column ever wraps to a third line or overflows the sidebar, at the picker's narrowest supported width.
- **SC-009**: The behaviour of the right column is provably unchanged — its existing automated tests pass without modification.
- **SC-010**: The Site/Folder field's folder-search results are unchanged after adopting the shared row presentation — its existing automated tests pass without modification — and the two-line result row exists in exactly **one** place in the codebase, down from two.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: Two areas.
  1. The AssetPicker's site-and-folder navigation — introduced very recently by epic #36702 and not yet in a released version, so this is a change to new product surface, not to legacy.
  2. The **Site/Folder field's picker overlay** (FR-027) — shipped, in daily use, and part of the new Edit Content surface. This is a pure de-duplication there: its rendering moves to a shared building block and must come out visually and behaviourally identical.

  Both share folder-browsing helpers with a third surface, Content Drive's sidebar. The legacy Browser Selector the AssetPicker descends from remains in use in the legacy Dojo editor and MUST keep working untouched.
- **Backward-compatibility expectations**:
  - The **cross-site browsing model changes**: today every browsable site is a root and the editor can cross sites by expanding a different root; after this change the picker is pinned to one site, changed only through the new selector. This is an intentional, user-visible behaviour change within an unreleased feature — no customer content, API, or admin workflow depends on the old model.
  - The remembered-location payload is already site-aware and MUST keep being read and written compatibly, including the pre-multi-site bare-path payload, so an editor mid-session does not lose their remembered folder.
  - The Site/Folder field is a **shipped** surface: FR-027 is explicitly a no-visible-change refactor there, and its existing tests are the contract (SC-010).
  - Nothing in the legacy Dojo edit-contentlet path may change: it does not mount this picker (#37132) and must not start to.
- **Known related decisions**:
  - Epic [#36702](https://github.com/dotCMS/core/issues/36702) established the AssetPicker and the single global remembered-location key.
  - [#37132](https://github.com/dotCMS/core/issues/37132) established that picker choice is per host — the new picker is Angular-editor only.
  - **Coordination risk — the highest risk in this feature**: [#37174](https://github.com/dotCMS/core/issues/37174) (folder-tree QA regressions in Content Drive and the **Site/Folder Field**) is in flight against the same shared folder-tree helpers *and* the same Site/Folder field this feature refactors (FR-027). Both would edit that field's picker overlay, with different intents for the same rendering, so sequencing MUST be agreed before implementation rather than resolved as a merge conflict afterwards. Two decisions deliberately narrow this overlap: FR-029 keeps this feature out of the folder-tree component itself, and FR-034 keeps Content Drive untouched — removing the second of #37174's two surfaces from the collision entirely. What remains is the Site/Folder field overlay alone.
  - Content Drive is already pinned to one site via the global switcher, so this change moves the picker **closer** to Content Drive's model. Lifting the new site-selector header into a component shared by both sidebars is explicitly **not** required (FR-034): Content Drive's site control lives in its toolbar, and a second one in its sidebar would be two competing controls on the same screen.
  - **Unification is the goal; Content Drive is the deferred third surface.** The intent behind FR-026 is one folder-search experience across the AssetPicker, the Site/Folder field, and Content Drive. This feature unifies the first two and leaves the block adoptable by the third. Content Drive needs a folder search added before it can adopt anything — new capability for that portlet, better scoped as its own issue, and one that should land after #37174 settles.
  - The plan phase will formally consult `dotCMS/platform-adrs` for binding decisions.

## Assumptions

- **Scope is the AssetPicker's left column, plus the Site/Folder field's adoption of the shared result row.** The AssetPicker's right column is explicitly out of scope (FR-030) and is expected to need no edits. The Site/Folder field is in scope only for FR-027 — swapping its inline result rendering for the shared one, with nothing else about it changing. **Content Drive is out of scope entirely** (FR-034), by decision, not oversight — see Context.
- **"Adoptable without rework" is a design constraint, not a deliverable.** FR-034's second half is verified by review of the block's inputs and coupling, not by a Content Drive integration — there is nothing to integrate until Content Drive has a folder search.
- **The existing site-selection control is fit for purpose.** It already provides lazy paging, a hostname filter, virtual scrolling, a pinned current selection, and reaction to live site events; this feature configures and styles it rather than extending it (FR-004). Should the design's globe-icon-plus-chevron treatment require changes to that shared control, those changes must remain backwards-compatible for its existing consumers.
- **The folder-tree presentation component stays presentational and unchanged** (FR-029). The flat result list is a *list*, not a one-level tree: it has no expansion, no indentation and no toggler, so modelling it as a tree would inherit affordances the design does not show. The shared building block is therefore a result-row/list presentation, not a new mode of the tree.
- **The two-line result row is not new work.** The Site/Folder field already renders it — bold name over `hostname / segment / segment`, with ellipsis truncation. FR-026/FR-027 extract that, they do not invent it, which is why US5 requires *no visible change* there.
- **Paging policy is deliberately not unified.** The Site/Folder field pages its search results today and keeps doing so; the AssetPicker caps at one page (FR-020) because paging its recursive result list would page a different, non-recursive query and return the wrong folders. The shared building block must not force one policy on both (FR-028).
- **The design's single-character search is not reproduced.** The screenshot showing `a` returning results conflicts with the two-character minimum that both surfaces and the underlying search enforce; the minimum wins (FR-012).
- **System Host stays excluded** from the site selector, matching today's tree roots. Its shared assets continue to surface within each site's asset listing.
- **Tree paging carries over unchanged.** Per-level *Load more* in the tree is untouched (FR-024); the flat result list is a different query and is governed separately (FR-020).
- **"Visible to the editor" means browsable.** A site the editor lacks permission to browse never appears in the selector and never survives as a remembered site.
- **The design screenshots are the visual contract** for the three stacked controls and the two-line result rows; where a screenshot and a platform constraint disagree, the constraint wins and the divergence is recorded (see the two-character minimum above).
- **No backend change is required.** The site listing and the recursive folder-name search both already exist and are already used by the current sidebar and by the Site/Folder field. All three open questions resolved in favour of existing platform behaviour, so this feature is entirely front-end.
- **Testing follows the constitution's TDD gates** — tests written, developer-approved, and confirmed failing before implementation (FR-033).
