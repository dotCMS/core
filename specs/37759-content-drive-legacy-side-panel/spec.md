# Feature Specification: Content Drive — open legacy-editor content in the side panel

**Feature Branch**: `37759-content-drive-open-legacy-editor-content-in-the-side-panel`

**Created**: 2026-09-29

**Status**: Draft

**Type**: New Feature

**Related GitHub Issue**: [#37759](https://github.com/dotCMS/core/issues/37759) (Part 1)

**Stacked on**: [#37829](https://github.com/dotCMS/core/pull/37829) (Part 2 of the same issue: route redirects). The stack is `main` → #37829 → this work.

**Input**: User description: "#37759 — Content Drive: open legacy-editor content in the side panel instead of routing to Content Search. Reuse the legacy editor's existing in-frame event contract, refresh the Content Drive list quietly on save and on every close, keep new-editor content and the side-panel-flag-off behavior unchanged."

## Problem Statement

Content Drive already edits and creates content in a **side panel** over the list, so the author
keeps their folder, filters and page. That only covers content types that opted into the new
content editor. For every other content type, a legacy-editor type, Content Drive leaves the drive
and opens the editor inside the **Content Search** portlet.

The Content Search route is only reachable when Content Search is in the user's menu. An
administrator who removes Content Search from a user's menu, which is a normal way to steer
authors toward Content Drive, leaves that user unable to edit or create any legacy-editor content
from Content Drive. The app sends them to their first portlet instead, and they get no
explanation.

The legacy editor itself is not tied to the menu. The server lets any user with the right
permissions open it, and the page editor (UVE) already shows it embedded. Only the Content Search
route in the admin UI depends on the menu.

## Scope

Issue #37759 has two parts, delivered as two stacked PRs:

- **Part 2 — route redirects** ([#37829](https://github.com/dotCMS/core/pull/37829), the PR this
  work sits on). When a user has Content Drive but not Content Search or Site Browser,
  `MenuGuardService` redirects the old routes to Content Drive: `/c/content/<inode>` becomes
  `/content-drive?editContent=<identifier>&editContentLang=<id>`, `/c/content/new/<type>` becomes
  `/content-drive?createContent=<type>`, and the listing and Site Browser URLs become their
  Content Drive equivalents. It covers every link that starts **outside** Content Drive.
- **Part 1 — this spec.** Content Drive opens legacy-editor content itself, in a side panel,
  instead of handing it to the Content Search route. It also has to **read** the two links Part 2
  builds, and open each one in the editor the content type chose.

**In scope**

| Path | What happens with Part 2 alone |
|---|---|
| Edit legacy-editor content from Content Drive (double-click, context menu, toolbar) | goes to `c/content/<inode>`. Part 2 does not redirect it (its loop check), so the user lands on their first portlet (`DotContentDriveNavigationService#editContentlet`) |
| Create content of a legacy-editor type from Content Drive | goes to `c/content/new/<type>`, which Part 2 redirects back to Content Drive with nothing open (`DotContentDriveNavigationService.createContent`) |
| A `?editContent=` link naming legacy-editor content, including the ones Part 2 builds from other screens (Query Tool, Publishing Queue, the Block Editor, emails) | opens in the **new** editor, whatever the type chose (`openEditByIdentifier` never looks up the type) |
| A `?createContent=<type>` link, which Part 2 builds from `c/content/new/<type>` (for example the starter onboarding cards) | nothing reads it, so Content Drive opens with nothing selected |
| "Switch to the old editor" inside the new-editor side panel | `c/content/<inode>`, after turning the new editor off for the type (`content.feature.ts`, `disableNewContentEditor`) |
| The new-editor side panel failing to load its content | `c/content` (`content.feature.ts`, `initializeExistingContent` error branch) |
| Compare versions from the legacy editor's History tab, inside the panel | the admin app's compare dialog, via the `compare-contentlet` event (`edit_contentlet_js_inc.jsp`, `emmitCompareEvent`) |

**Out of scope**

- Everything Part 2 delivers: the redirect rules, and the callers that now pass a folder to the
  Site Browser route (templates and containers stored as files).
- Editing related content from a Block Editor field inside either panel. The bubble menu
  navigates the whole admin window away, for both editors.
- Removing the `CD_` query params that bring the author back to Content Drive from the full-page
  legacy editor (added in [#33726](https://github.com/dotCMS/core/pull/33726), and restored by
  Part 2's redirect). They are only produced when the side-panel flag is off, and they stay until
  the flag itself is removed.
- Side-panel flag off and Content Search not in the menu. Content Drive still hands legacy
  content to the full-page editor, and Part 2's loop check sends the author to their first
  portlet, as today.

**Not planned**

- Server-rendered JSP and Java links that use `p_p_id=content` or `p_p_id=site-browser` portlet
  URLs (dashboard, link checker, publishing JSPs, the page editor's "reorder menu"). They never
  go through the Angular routes.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Edit legacy-editor content without leaving Content Drive (Priority: P1)

An author browsing a folder in Content Drive opens a contentlet whose type uses the legacy editor,
by double-click, the row context menu **Edit**, or the toolbar. The legacy editor opens in a side
panel over the list. The author edits, saves or runs workflow actions, then closes the panel and
lands back on the same folder, filters and page, with the list showing the result.

**Why this priority**: This is the blocking case. Without it, a user without Content Search in
their menu cannot edit legacy-editor content from Content Drive at all. It also removes the
round-trip away from the drive for every other user.

**Independent Test**: With the side-panel flag on, remove Content Search from a test user's
menu. Open a legacy-editor contentlet from Content Drive through each of the three entry points,
change a field, save, close, and check that the list shows the change in the same folder, filters
and page.

**Acceptance Scenarios**:

1. **Given** a legacy-editor contentlet in the current folder, **When** the author double-clicks
   it, **Then** the legacy editor opens in a side panel over Content Drive and the browser does
   not leave Content Drive.
2. **Given** the same contentlet, **When** the author picks **Edit** from the row context menu
   or the toolbar, **Then** the same side panel opens.
3. **Given** a user whose menu does not include Content Search, **When** they open a
   legacy-editor contentlet from Content Drive, **Then** the editor opens in the panel and they
   are not redirected to another portlet.
4. **Given** the panel is open, **When** the author saves, **Then** the Content Drive list
   refreshes and keeps the current folder, filters and page, and the panel stays open.
5. **Given** the panel is open, **When** the author closes it by any means (the editor's own
   close, X, ESC, or a click outside the panel), **Then** the list refreshes without showing the
   loading placeholder and keeps the current folder, filters and page.

---

### User Story 2 - Create legacy-editor content in the current folder (Priority: P1)

An author picks a legacy-editor content type from Content Drive's create action. The legacy
create form opens in the side panel with the current folder already chosen as its location. After
saving, the new content appears in the list for that folder.

**Why this priority**: This is the same blocking case as Story 1, for creation. An author
without Content Search in their menu cannot add legacy-editor content from Content Drive at all.

**Independent Test**: As a user without Content Search in their menu, browse to a sub-folder,
create content of a legacy-editor type, save, close, and check that the new item is listed in
that sub-folder.

**Acceptance Scenarios**:

1. **Given** the author is browsing a folder, **When** they create content of a legacy-editor
   type, **Then** the legacy create form opens in the side panel and the browser does not leave
   Content Drive.
2. **Given** that create form, **When** it loads, **Then** its site/folder location already
   shows the folder the author was browsing.
3. **Given** the author saves the new content, **When** the panel closes, **Then** the new item
   appears in the list for that folder.
4. **Given** a user without Content Search in their menu, **When** they open a link to
   `c/content/new/<type>` from another screen (for example a starter onboarding card), which
   Part 2 redirects to `/content-drive?createContent=<type>`, **Then** the create form for that
   type opens in the side panel, in the editor the type chose.

---

### User Story 3 - Workflow, delete and page actions from inside the panel (Priority: P2)

From the legacy editor in the panel, the author runs workflow actions (publish, unpublish,
archive, and so on), including ones that need extra input (comments, assignment, push publish),
deletes or destroys the content, or jumps to the page editor. Each action works as it does in
Content Search, and the list reflects the result.

**Why this priority**: Editing without workflow is only half of the job, but Stories 1 and 2
already unblock the author for plain saves. These actions complete the parity with the Content
Search experience.

**Independent Test**: From the panel, run a plain workflow action, one that opens the workflow
wizard, one that opens the push publish dialog, and a delete. After each one, check that the list
shows the new state once the panel is closed.

**Acceptance Scenarios**:

1. **Given** the panel is open, **When** the author runs a workflow action, **Then** the list
   refreshes and shows the content's new status, keeping folder, filters and page.
2. **Given** a workflow action that needs extra input, **When** the author runs it, **Then** the
   workflow wizard opens, and on completion the action is applied to the content in the panel.
3. **Given** a push publish action, **When** the author runs it, **Then** the push publish dialog
   opens and works as it does in Content Search.
4. **Given** the author deletes or destroys the content from the panel, **When** the action
   completes, **Then** the panel closes and the item is gone from the list.
5. **Given** the editor offers a link to the page editor, **When** the author follows it,
   **Then** the page editor opens for that page.

---

### User Story 4 - Unsaved changes are never lost silently (Priority: P2)

An author who has changed fields in the legacy editor and tries to close the panel without saving
is asked to confirm, with the same prompt the new-editor side panel uses.

**Why this priority**: Moving the editor into a panel makes accidental closes easier (ESC, a click
outside, browser Back). The panel must not throw away work that the full-page editor would have
kept.

**Independent Test**: Change a field, then try X, ESC, a click outside the panel and browser
Back. Each one asks for confirmation. Cancelling keeps the edits, and confirming closes the panel
and refreshes the list.

**Acceptance Scenarios**:

1. **Given** unsaved changes in the panel, **When** the author presses X, ESC, clicks outside the
   panel, or uses browser Back, **Then** a confirmation matching the new-editor side panel's
   unsaved-changes prompt appears.
2. **Given** that prompt, **When** the author chooses to keep editing, **Then** the panel stays
   open with their edits intact.
3. **Given** that prompt, **When** the author confirms, **Then** the panel closes and the list
   refreshes quietly.
4. **Given** the author has just saved and made no further changes, **When** they close the
   panel, **Then** no confirmation is shown.

---

### User Story 5 - Links open legacy-editor content in the right editor (Priority: P1)

A Content Drive URL that names a piece of content to edit opens that content in the panel, in
the editor its content type chose. The URL can be a shared link, a bookmark, a refresh with the
panel open, or, since Part 2, a link from any other screen: Part 2 redirects every
`c/content/<inode>` link (Query Tool, Publishing Queue, the Block Editor, form-entry emails) to
this URL.

**Why this priority**: With Part 2 in place, this URL is how every screen outside Content Drive
opens content for a user without Content Search. Today it always opens the new editor, so a
legacy-editor type opens in an editor it opted out of. For example, a Query Tool result for a
legacy-editor `Banner` opens in the new editor.

**Independent Test**: As a user without Content Search in their menu, run `+contentType:Banner`
(a legacy-editor type) in the Query Tool and open a result. Check that the legacy editor opens in
the Content Drive panel for that content. Then do the same from a copied Content Drive URL.

**Acceptance Scenarios**:

1. **Given** a Content Drive URL naming a legacy-editor contentlet, **When** it is opened, **Then**
   the legacy editor opens in the side panel for that content.
2. **Given** a user without Content Search in their menu, **When** they open a legacy-editor
   result from the Query Tool (or any other screen that links to `c/content/<inode>`), **Then**
   the legacy editor opens in the Content Drive side panel, never the new editor.
3. **Given** a Content Drive URL naming a new-editor contentlet, **When** it is opened, **Then**
   the new editor opens in the side panel, as today.
4. **Given** the side-panel flag is off, **When** a URL naming a legacy-editor contentlet is
   opened, **Then** Content Drive hands it to the full-page legacy editor (not the full-page new
   editor). A user without Content Search then lands on their first portlet, through Part 2's
   loop check.

---

### User Story 6 - Switching editors and load errors stay inside Content Drive (Priority: P2)

Some actions inside the new-editor side panel still send the author to the Content Search route:
switching the content type back to the old editor, and the editor failing to load. The legacy
editor's History tab also asks the admin UI to compare versions. From Content Drive, each of
these should keep the author in the drive.

**Why this priority**: Without it, a user without Content Search in their menu who clicks
"switch to the old editor" in the panel is sent to their first portlet. That is the same failure
this spec fixes for Stories 1 and 2.

**Independent Test**: As a user without Content Search in their menu, open new-editor content in
the panel and switch to the old editor. Then force a load error in the panel. Then open a
legacy-editor item and compare two versions from its History tab.

**Acceptance Scenarios**:

1. **Given** new-editor content open in the side panel, **When** the author uses "switch to the
   old editor", **Then** the same content, in the same language, reopens in the legacy panel and
   the browser stays in Content Drive.
2. **Given** the new-editor side panel, **When** its content fails to load, **Then** the standard
   error message is shown, the panel closes, and the list keeps its folder, filters and page.
3. **Given** the legacy panel open, **When** the author compares two versions from the History
   tab, **Then** the compare dialog opens over Content Drive.

---

### Edge Cases

- **Delete of content that is not a page**: the legacy editor reports this the same way as a
  plain Cancel. So the list refreshes on every close, whatever the reason, rather than trying to
  detect what happened.
- **Close with no changes**: this still triggers one quiet refresh. That costs one extra list
  request and is accepted, so that changes the drive cannot see (moves, language switches,
  workflow wizard actions) are never missed.
- **Content type lookup fails** (network error, type deleted): the standard error message is
  shown and nothing opens, as today.
- **User can see but not edit the content**: the legacy editor shows its own permission message
  inside the panel. Content Drive adds no permission check of its own.
- **Legacy create form URL cannot be resolved** (for example, a type the user cannot create): the
  standard error message is shown and the panel does not open.
- **Author switches language inside the legacy editor**: the list refreshes on close. A shared
  link taken afterwards may still name the language the panel was opened with.
- **Side-panel flag off and Content Search not in the menu**: this spec does not change it.
  Content Drive hands legacy content to the full-page editor, and Part 2's loop check sends the
  author to their first portlet instead of back into Content Drive. Part 2's loop check covers
  edit links only, so FR-028 extends it to create links; without that, a flag-off legacy create
  would bounce between Content Drive and the create route.
- **Related content from a Block Editor field inside a panel**: the Block Editor's bubble menu
  navigates the whole admin window to the related content's editor, for both panels, and the
  open panel is lost. Part 2's redirect lands the author back in Content Drive with that content
  open, but not in the panel they started from. This is out of scope here.
- **`createContent` names a type that doesn't exist or the user can't create**: the standard
  error message is shown, nothing opens, and the author stays on Content Drive.
- **Side-panel flag not yet resolved** on a cold deep-link load: the drive falls back to the
  full-page editor for that content type, as it already does for new-editor content.
- **Page content (HTML pages)**: this is unchanged. Opening a page from Content Drive still goes
  to the page editor, not to either side panel.

## Requirements *(mandatory)*

### Functional Requirements

**Routing decision**

- **FR-001**: With the side-panel flag on, editing a contentlet whose type has not opted into the
  new editor MUST open the legacy editor in a Content Drive side panel, from every edit entry
  point (double-click, row context menu, toolbar), and MUST NOT navigate to the Content Search
  route.
- **FR-002**: With the side-panel flag on, creating content of a type that has not opted into
  the new editor MUST open the legacy create form in a Content Drive side panel, and MUST NOT
  navigate to the Content Search route.
- **FR-003**: The legacy create form opened from Content Drive MUST pre-select the folder the
  author is browsing as the new content's location.
- **FR-004**: FR-001 and FR-002 MUST work for a user whose menu does not include Content Search.
- **FR-005**: Content types that opted into the new editor MUST keep their current side-panel
  behavior, unchanged, except for FR-024 and FR-025.
- **FR-006**: With the side-panel flag off, editing and creating legacy-editor content MUST keep
  routing to the full-page legacy editor, as today, with the `CD_` query params that bring the
  author back to Content Drive on close. Those params, and the code that reads them, stay while
  the flag exists.
- **FR-007**: Which editor opens MUST keep following each content type's own editor setting. The
  panel MUST NOT switch a legacy-editor type to the new editor.

**In-panel behavior**

- **FR-008**: The panel MUST support everything the full-page legacy editor supports through its
  existing event contract with the admin UI: close, save, page deletion, change tracking, page
  editor navigation, load notification and version comparison.
- **FR-009**: Workflow actions that need extra input MUST open the workflow wizard from the panel
  and apply the action to the content in the panel on completion.
- **FR-010**: Push publish actions MUST open the push publish dialog from the panel.
- **FR-011**: A request from inside the legacy editor to open the page editor MUST navigate to
  the page editor.
- **FR-012**: The panel MUST show a title that identifies the content being edited, or the type
  being created.

**List refresh**

- **FR-013**: Saving in the panel (including a workflow action that saves) MUST refresh the
  Content Drive list while the panel stays open, and keep the current folder, filters and page.
- **FR-014**: Closing the panel for any reason (the editor's own close, a page deletion, X, ESC,
  a click outside the panel, confirmed browser Back) MUST refresh the Content Drive list, and
  keep the current folder, filters and page.
- **FR-015**: The refresh on close MUST NOT show the list's loading placeholder.
- **FR-016**: Deleting or destroying content from the panel MUST close the panel and remove the
  item from the list.

**Unsaved changes**

- **FR-017**: Closing the panel with unsaved changes (X, ESC, a click outside the panel, browser
  Back) MUST ask for confirmation with the same prompt and wording the new-editor side panel uses.
- **FR-018**: Changes count as unsaved from the first change the legacy editor reports until the
  next successful save.
- **FR-019**: Browser Back while the panel is open MUST go through the same unsaved-changes
  confirmation as the panel's own close controls.

**Links and URL state**

- **FR-020**: The Content Drive URL MUST reflect the open legacy-editor panel the same way it
  does for the new-editor panel, so refresh, Back and shared links behave the same for both.
- **FR-021**: Opening a Content Drive URL that names legacy-editor content MUST open the legacy
  editor in the panel when the flag is on, and the full-page legacy editor when the flag is off.
  This applies however the URL was reached, including Part 2's redirect from `c/content/<inode>`.
  Content Drive MUST look up the content type's editor setting before opening; it MUST NOT
  default to the new editor.
- **FR-022**: Opening a Content Drive URL that names new-editor content MUST behave as today.
- **FR-027**: Opening a Content Drive URL with `createContent=<content type variable>` MUST open
  the create form for that type, following FR-002 (legacy-editor type) or today's new-editor
  create (other types). The new content's location is the folder the URL names (`path`), or the
  site root. Content Drive reads the param once, on load, and removes it from the URL once the
  create form opens, so a refresh does not open a second create form.
- **FR-028**: Part 2's loop check (a legacy link sent from Content Drive itself is not redirected
  back into Content Drive) MUST cover create links as well as edit links, so that a flag-off
  legacy create from Content Drive, for a user without Content Search, lands on their first
  portlet instead of looping.

**Staying inside Content Drive**

- **FR-024**: With the side-panel flag on, "switch to the old editor" inside the new-editor side
  panel MUST reopen the same content, in the same language, in the legacy panel, without leaving
  Content Drive. It still turns the new editor off for that content type, as it does today. The
  full-page new editor keeps its current behavior.
- **FR-025**: With the side-panel flag on, a load failure in the new-editor side panel MUST show
  the standard error message and close the panel, keeping the current folder, filters and page.
  It MUST NOT navigate to the Content Search route. The full-page new editor keeps its current
  behavior.
- **FR-026**: The Content Drive links that open a panel (`editContent` with `editContentLang`,
  and `createContent`) are supported entry points. Part 2 redirects to them, other screens may
  link to them, and they MUST keep working for both editor kinds.

**Tests**

- **FR-023**: Automated tests MUST cover the routing decision (new vs legacy editor, flag on vs
  off, edit vs create vs `editContent` link vs `createContent` link), the handling of each legacy
  editor event, the list refresh after save and after every kind of close, FR-024, FR-025 and
  FR-028.

### Key Entities

- **Edit panel request**: what Content Drive has asked to open: the content (or content type
  for create), its language, and its title. This already exists for the new editor.
- **Editor kind**: which editor the open request uses, new or legacy, based on the content
  type's editor setting. Everything that follows the open panel (URL, Back, refresh on close)
  works the same for both kinds.
- **Legacy editor event**: a notification the embedded legacy editor sends to the admin UI
  (close, save, page deleted, data changed, open page editor, loaded, workflow wizard, push
  publish).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user without Content Search in their menu can edit and create content of every
  legacy-editor content type from Content Drive, and can switch a type back to the old editor
  from the side panel. Zero of these attempts redirect them to another portlet.
- **SC-002**: After saving or closing the legacy-editor panel, the author is on the same folder,
  filters and page they started from in 100% of cases, with no manual navigation.
- **SC-003**: Changes made in the panel (edits, new content, status changes, deletions) show in
  the list by the time the panel has closed, without a manual refresh.
- **SC-004**: Closing the panel never shows the list's loading placeholder.
- **SC-005**: Zero unsaved edits are discarded without the author confirming it.
- **SC-006**: Content types that use the new editor, and installations with the side-panel flag
  off, see no change in behavior. Their existing automated tests pass unchanged, apart from the
  tests that are moved to the flag-off group because they assumed flag-on routing to Content
  Search.
- **SC-007**: Opening legacy-editor content from any screen outside Content Drive (Query Tool,
  Publishing Queue, Block Editor, form-entry emails) opens the legacy editor in 100% of cases.
  Zero of them open the new editor.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The Content Drive portlet's edit and create routing, and its
  side-panel host. It embeds the **legacy content editor** (the JSP-based edit-contentlet screen
  that Content Search and the page editor already embed), which is part of the older product
  surface. The legacy editor itself is **not** changed. It is only hosted in a new place, using
  the events it already sends.
- **Backward-compatibility expectations**: New-editor content, installations with the side-panel
  flag off, page content, and the Content Search portlet itself all keep working unchanged. There
  are no REST, database or index changes, so the feature is safe to roll back. One deliberate
  correction: with the flag off, a Content Drive link to legacy-editor content now opens the
  full-page **legacy** editor. Today it opens the full-page new editor, which does not match that
  content type's editor setting.
- **Known related decisions**:
  - The side panel for the new editor, and its flag (on by default), already exist. This feature
    extends that pattern rather than adding a second way to edit.
  - Each content type chooses its own editor. The alternative of always opening the new editor in
    the panel was rejected, because it would silently switch editors for types that opted out
    (FR-007).
  - The embedding, event handling and workflow/push publish support that Content Search uses live
    in the admin application, where a portlet library cannot reach them. Where that capability
    lives is a plan-phase decision. There is a precedent: the page editor (UVE) is itself a
    portlet library and already embeds the legacy editor in its own dialog. It listens to the
    same events, including the workflow wizard, without depending on the admin application.
  - The plan formally consults `dotCMS/platform-adrs`.
  - This work is stacked on Part 2 (#37829). Part 2 merges first, and on its own it opens
    legacy-editor content that arrives from other screens in the new editor (User Story 5). The
    fix is deliberately kept here rather than in Part 2 (decided 2026-09-30), so both PRs should
    ship in the same release.
  - The `CD_` round trip from the full-page legacy editor back to Content Drive is kept while the
    side-panel flag exists (decided 2026-09-30). Once the flag is removed, the `CD_` params, the
    two legacy close handlers that read them, and Part 2's `CD_` restoration and loop check can be
    removed together.

## Assumptions

- The legacy editor's existing event contract is stable and sufficient. Embedding it needs no
  change to the legacy editor.
- The server already lets a user without Content Search in their menu load the legacy editor.
  Only the admin UI route is gated. Content-level permissions are still enforced by the editor
  itself.
- It is not confirmed that a save notification always follows a workflow wizard action. The
  refresh on every close (FR-014) covers that case whatever the answer.
- The refresh after a save (FR-013) also uses the quiet refresh, like the refresh on close. The
  new-editor panel's own refresh after a save is out of scope and stays as it is.
- The unsaved-changes prompt reuses the new-editor side panel's existing wording. No new
  user-facing text is needed beyond what the panel title requires.
- The quiet refresh of the list is the only feedback Content Drive gives after a save or a close.
  It shows no toast or other notice of its own. Messages the legacy editor or the workflow and
  push publish dialogs already show keep appearing as they do today.
- The side-panel flag stays on by default, so users without Content Search in their menu get the
  fix without any configuration.
- Part 2 (#37829) keeps the link formats this spec reads: `editContent` with `editContentLang`
  for edits, and `createContent` with an optional `path` for creates. If review renames
  `createContent`, FR-027 follows the new name.
