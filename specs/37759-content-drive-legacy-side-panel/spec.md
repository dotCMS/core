# Feature Specification: Content Drive — open legacy-editor content in the side panel

**Feature Branch**: `37759-content-drive-open-legacy-editor-content-in-the-side-panel`

**Created**: 2026-09-29

**Status**: Draft

**Type**: New Feature

**Related GitHub Issue**: [#37759](https://github.com/dotCMS/core/issues/37759), Part 2

**Delivered in**: [#37832](https://github.com/dotCMS/core/pull/37832), the one PR for all of #37759.
It already carries Part 1 (route redirects), brought over from
[#37829](https://github.com/dotCMS/core/pull/37829), which was closed so the issue ships in a single
PR. This spec covers Part 2; its implementation is added to the same PR once the spec is approved.

**Input**: User description: "#37759 — Content Drive: open legacy-editor content in the side panel instead of routing to Content Search. Reuse the legacy editor's existing in-frame event contract, refresh the Content Drive list quietly on save and on every close, keep new-editor content and the side-panel-flag-off behavior unchanged."

## Problem Statement

Content Drive is replacing Content Search and Site Browser. An administrator can remove both from
a user's menu and leave only Content Drive, and that user must still be able to do everything the
two portlets did.

Content Drive already edits and creates content in a **side panel** over the list, so the author
keeps their folder, filters and page. That only covers content types that opted into the new
content editor. For every other content type, a legacy-editor type, Content Drive leaves the drive
and opens the editor inside the **Content Search** portlet.

The Content Search route is only reachable when Content Search is in the user's menu. A user
without it cannot edit or create any legacy-editor content from Content Drive: the app sends them
to their first portlet, with no explanation.

The legacy editor itself is not tied to the menu. The server lets any user with the right
permissions open it, and the page editor (UVE) already shows it embedded. Only the Content Search
route in the admin UI depends on the menu.

## Scope

**What this work delivers**

1. **Legacy-editor content opens in a Content Drive side panel.** Editing and creating content of
   a type that uses the legacy editor opens that editor in a panel over the list, the same way
   new-editor content already opens. Content Drive never sends it to the Content Search route
   again, whatever the side-panel flag says.
2. **What the author needs from the legacy editor works from the panel**: save, workflow actions
   (including the workflow wizard and push publish), delete, and comparing versions from the
   History tab, including bringing an older version back. The panel listens only to the legacy
   editor events these need. The editor's links to the page editor keep working as they are.
3. **The list stays current**: it refreshes quietly on every save and every close, keeping
   folder, filters and page. Unsaved changes ask for confirmation before the panel closes.
4. **The URL always says which panel is open**, for both editors: `editContent` with
   `editContentLang` while editing, `createContent=<type>` while creating (switching to the edit
   params once the new content is saved). `editContentLang` follows a language switch inside the
   legacy editor, and the params are removed when the panel closes. Params that can't hold
   together open nothing. Every create starts in Content Drive's default language. The same
   URLs, when opened, reopen the panel in the editor the content type chose.
5. **New-editor side-panel paths stay in Content Drive**: "switch to the old editor" reopens the
   content in the legacy panel, and a load error closes the panel instead of leaving for Content
   Search.
6. **The full-page hand-off is removed**: the `CD_` params that brought the author back from the
   full-page legacy editor, the code that reads them, and Part 1's loop check, which only existed
   because of that hand-off.
7. **The URL also follows the open folder dialog**: New Folder, Folder Settings and Edit
   Permissions write a param while they are open, so a refresh or a shared link reopens them, and
   remove it when they close.

**How it fits with Part 1**

Issue #37759 has two parts, delivered together in one PR. Part 1 is already implemented on the
branch, and this work, Part 2, builds on it.

- **Part 1 — route redirects** (already on the branch, first written in
  [#37829](https://github.com/dotCMS/core/pull/37829)). When a user has Content Drive but not
  Content Search or Site Browser, the old routes redirect to Content Drive: `/c/content/<inode>`
  becomes `/content-drive?editContent=<identifier>&editContentLang=<id>`, `/c/content/new/<type>`
  becomes `/content-drive?createContent=<type>`, and the listing and Site Browser URLs become their
  Content Drive equivalents. It covers every link that starts **outside** Content Drive. To avoid a
  loop, it does not redirect an edit link sent from Content Drive itself, and it restores the `CD_`
  params of Content Drive's hand-off. Part 2 removes both (item 6 above).
- **Part 2 — this spec.** Content Drive opens legacy-editor content itself, and **reads** the two
  links Part 1 builds.

**In scope**

The second column is what a user without Content Search gets with Part 1 alone, which is the
branch as it stands before this work. The code pointers in parentheses are for the reviewer; the
plan decides the design.

| Path | With Part 1 alone | Covered by |
|---|---|---|
| Edit legacy-editor content from Content Drive (double-click, context menu, toolbar) | Goes to `c/content/<inode>`. Part 1's loop check leaves it alone, so the user lands on their first portlet (`DotContentDriveNavigationService#editContentlet`) | US1 |
| Create content of a legacy-editor type from Content Drive | Goes to `c/content/new/<type>`, which Part 1 redirects back to the same Content Drive folder and filters, with nothing open (`DotContentDriveNavigationService.createContent`) | US2 |
| A `?editContent=` link naming legacy-editor content, including the ones Part 1 builds from other screens (Query Tool, Publishing Queue, the Block Editor, form-entry emails) | Opens in the **new** editor, whatever the type chose (`openEditByIdentifier` never looks up the type) | US5 |
| A `?createContent=<type>` link, which Part 1 builds from `c/content/new/<type>` (for example the starter onboarding cards) | Nothing reads it, so Content Drive opens with nothing selected | US2 |
| The URL while a create panel is open (either editor) | `editContent=new`, without the type, so a refresh or a shared link can't reopen it (`dot-content-drive-shell.component.ts`, `NEW_CONTENT_MARKER`) | US2, US7 |
| The URL while a folder dialog is open: New Folder, Folder Settings (both through the store's dialog state) and Edit Permissions (a JSP dialog the row context menu opens directly, `dot-folder-list-context-menu.component.ts`) | No param, so a refresh closes the dialog and it can't be shared | US8 |
| "Switch to the old editor" inside the new-editor side panel | Turns the new editor off for the type, then goes to `c/content/<inode>` and the user lands on their first portlet (`content.feature.ts`, `disableNewContentEditor`) | US6 |
| The new-editor side panel failing to load its content | Leaves the drive for `c/content` (`content.feature.ts`, `initializeExistingContent` error branch) | US6 |
| Compare versions from the legacy editor's History tab, inside the panel, and Bring Back | Needs the admin app's compare dialog, which listens for the `compare-contentlet` event (`edit_contentlet_js_inc.jsp`, `emmitCompareEvent`), and its Bring Back | US6 |
| Switching language inside the legacy editor | The editor reloads itself in the new language and sends no event, so the URL can't follow it | US7 |
| The `CD_` params from [#33726](https://github.com/dotCMS/core/pull/33726), added to every legacy hand-off so closing the full-page legacy editor returns to Content Drive (`mapQueryParamsToCDParams`, `mapParamsFromEditContentlet`, the `onClose` of `DotContentletWrapperComponent` and `DotCreateContentletComponent`), restored by Part 1's redirect, whose loop check exists only because of that hand-off | Still produced and read | FR-026, FR-027 |

**Out of scope**

- Everything else Part 1 delivers: the redirect rules, and the callers that now pass a folder to
  the Site Browser route (templates and containers stored as files).
- Removing the side-panel flag itself. Query Tool, the relationship field of the new editor and
  the page editor (UVE) read it too, so its removal is its own piece of work. This work only stops
  Content Drive's legacy-editor path from depending on it.
- Editing related content from a Block Editor field inside either panel. The bubble menu
  navigates the whole admin window away, for both editors.

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
round trip away from the drive for every other user.

**Independent Test**: Remove Content Search from a test user's menu. Open a legacy-editor
contentlet from Content Drive through each of the three entry points, change a field, save, close,
and check that the list shows the change in the same folder, filters and page. Repeat once with
the side-panel flag off.

**Acceptance Scenarios**:

1. **Given** a legacy-editor contentlet in the current folder, **When** the author double-clicks
   it, **Then** the legacy editor opens in a side panel over Content Drive and the browser does
   not leave Content Drive.
2. **Given** the same contentlet, **When** the author picks **Edit** from the row context menu
   or the toolbar, **Then** the same side panel opens.
3. **Given** a user whose menu does not include Content Search, **When** they open a
   legacy-editor contentlet from Content Drive, **Then** the editor opens in the panel and they
   are not redirected to another portlet.
4. **Given** the side-panel flag is off, **When** the author opens a legacy-editor contentlet,
   **Then** it still opens in the side panel. New-editor content keeps opening full screen with
   the flag off, as today.
5. **Given** the panel is open, **When** the author saves, **Then** the Content Drive list
   refreshes and keeps the current folder, filters and page, and the panel stays open.
6. **Given** the panel is open, **When** the author closes it by any means (the editor's own
   close, X, ESC, or a click outside the panel), **Then** the list refreshes without showing the
   loading placeholder and keeps the current folder, filters and page.

---

### User Story 2 - Create legacy-editor content in the right folder (Priority: P1)

An author picks a legacy-editor content type from Content Drive's create action. The legacy
create form opens in the side panel with the folder Content Drive is showing already chosen as its
location. After saving, the new content appears in the list for that folder.

The same happens when the create request arrives as a link. Part 1 turns every
`c/content/new/<type>` link into a Content Drive `createContent` link, and the new content goes in
the folder that link opens Content Drive on.

**Why this priority**: This is the same blocking case as Story 1, for creation. An author
without Content Search in their menu cannot add legacy-editor content from Content Drive at all.

**Independent Test**: As a user without Content Search in their menu, browse to a sub-folder,
create content of a legacy-editor type, save, close, and check that the new item is listed in
that sub-folder. Then open a starter onboarding "create" card and check that the create form opens
in the Content Drive panel with the site root as its location.

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
   Part 1 redirects to `/content-drive?createContent=<type>`, **Then** the create form for that
   type opens in the side panel, in the editor the type chose. The link names no folder, so
   Content Drive opens on the site root and that is the new content's location.
5. **Given** a `createContent` link that also names a folder (`path`), **When** it is opened,
   **Then** Content Drive opens on that folder and the create form uses it as the location.
6. **Given** any create, from Content Drive's create action or a `createContent` link, in either
   editor, **When** the form opens, **Then** it starts in Content Drive's default language.

---

### User Story 3 - Workflow, delete and page actions from inside the panel (Priority: P2)

From the legacy editor in the panel, the author runs workflow actions (publish, unpublish,
archive, and so on), including ones that need extra input (comments, assignment, push publish),
deletes or destroys the content, or follows one of its links to the page editor. Each action
works as it does in Content Search, and the list reflects the result.

**Why this priority**: Editing without workflow is only half of the job, but Stories 1 and 2
already unblock the author for plain saves. These actions complete the parity with the Content
Search experience.

**Independent Test**: From the panel, run a plain workflow action, one that opens the workflow
wizard, one that opens the push publish dialog, and a delete. After each one, check that the list
shows the new state.

**Acceptance Scenarios**:

1. **Given** the panel is open, **When** the author runs a workflow action, **Then** the list
   refreshes and shows the content's new status, keeping folder, filters and page.
2. **Given** a workflow action that needs extra input, **When** the author runs it, **Then** the
   workflow wizard opens, and on completion the action is applied to the content in the panel and
   the list refreshes, as it does after a plain save.
3. **Given** a push publish action, **When** the author runs it, **Then** the push publish dialog
   opens and works as it does in Content Search.
4. **Given** the author deletes or destroys the content from the panel, **When** the action
   completes, **Then** the panel closes and the item is gone from the list.
5. **Given** the editor's links to the page editor (**Preview page**, and the pages listed in the
   **References** tab), **When** the author follows one, **Then** the page editor opens, as it
   does from the full-page editor today.

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
panel open, or a link from any other screen: Part 1 redirects every `c/content/<inode>` link
(Query Tool, Publishing Queue, the Block Editor, form-entry emails) to this URL.

**Why this priority**: With Part 1 in place, this URL is how every screen outside Content Drive
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
   opened, **Then** the legacy editor still opens in the side panel. A URL naming new-editor
   content opens the full-page new editor, as today.

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
legacy-editor item, compare two versions from its History tab, and bring the older one back.

**Acceptance Scenarios**:

1. **Given** new-editor content open in the side panel, **When** the author uses "switch to the
   old editor", **Then** the same content, in the same language, reopens in the legacy panel and
   the browser stays in Content Drive.
2. **Given** the new-editor side panel, **When** its content fails to load, **Then** the standard
   error message is shown, the panel closes, and the list keeps its folder, filters and page.
3. **Given** the legacy panel open, **When** the author compares two versions from the History
   tab, **Then** the compare dialog opens over Content Drive.
4. **Given** that compare dialog, **When** the author uses **Bring Back** on a version, **Then**
   that version is restored in the panel, the compare dialog closes, and the list refreshes.

---

### User Story 7 - The URL follows the open panel (Priority: P2)

Whichever editor is open, the Content Drive URL says what the panel shows, so a refresh, a copied
link or browser Back behaves the same for edit and create, and for both editors. Closing the
panel removes those params.

**Why this priority**: Today only an open edit panel can be refreshed or shared. A create panel
writes a bare `new` marker without the type, so a refresh closes it. With Part 1 building
`createContent` links from other screens, Content Drive should write the same link for its own
create panel.

**Independent Test**: For each editor, open an edit panel and a create panel and check the URL.
Refresh each one. Save a new piece of content, refresh again, then close the panel and check that
the params are gone.

**Acceptance Scenarios**:

1. **Given** the author opens content to edit, in either editor, **When** the panel opens,
   **Then** the URL carries `editContent=<identifier>` and `editContentLang=<language id>`.
2. **Given** the author starts creating content of a type, in either editor, **When** the panel
   opens, **Then** the URL carries `createContent=<content type variable>`, next to the `path` of
   the folder Content Drive is showing.
3. **Given** a create panel with nothing saved yet, **When** the author refreshes the page,
   **Then** an empty create form for the same type opens in the same folder.
4. **Given** a create panel, **When** the author saves the new content for the first time,
   **Then** the URL switches to that content's `editContent` and `editContentLang` without adding
   a browser history entry, and a refresh reopens the saved content rather than a second empty
   create form.
5. **Given** any panel open, **When** it closes for any reason, **Then** `editContent`,
   `editContentLang` and `createContent` are removed from the URL.
6. **Given** the legacy panel open, **When** the author switches language inside the editor,
   **Then** `editContentLang` changes to that language without adding a browser history entry,
   and a refresh reopens the content in that language.
7. **Given** a URL with params that can't hold together (`editContent` with `createContent`, a
   panel param with a folder-dialog param, or two folder-dialog params), **When** it is opened,
   **Then** nothing opens and all of those params are removed.

---

### User Story 8 - The URL follows the open folder dialog (Priority: P3)

The folder dialogs behave like the panels: while one is open, the Content Drive URL says which
one and for which folder, so a refresh or a copied link reopens it, and closing it removes the
param. This covers **New Folder** (from the create action), and **Folder Settings** and **Edit
Permissions** (from a folder's context menu).

**Why this priority**: It is not needed to replace Content Search or Site Browser. It was asked
for on review so that folders follow the same URL rules as content, and it reuses the URL
handling this work builds for the panels.

**Independent Test**: Open each folder dialog and check the URL. Refresh with each one open and
check that the same dialog reopens for the same folder. Close each one by every means, including
browser Back, and check that the param is gone.

**Acceptance Scenarios**:

1. **Given** the author opens **New Folder** while browsing a folder, **When** the dialog opens,
   **Then** the URL carries `createFolder=true` next to the `path` of that folder, and a refresh
   reopens the dialog to create a folder in the same place.
2. **Given** the author opens **Folder Settings** for a folder, **When** the dialog opens,
   **Then** the URL carries `editFolder=<folder identifier>`, and a refresh reopens the settings of
   that folder.
3. **Given** the author opens **Edit Permissions** for a folder, **When** the dialog opens,
   **Then** the URL carries `folderPermissions=<folder identifier>`, and a refresh reopens the
   permissions of that folder.
4. **Given** any folder dialog open, **When** it closes for any reason (save, cancel, X, ESC,
   browser Back), **Then** its param is removed from the URL.
5. **Given** a URL naming a folder the author cannot find or cannot edit, **When** it is opened,
   **Then** the standard error message is shown, no dialog opens, and the param is removed.

---

### Edge Cases

- **Delete from the panel**: the panel closed, so the list refreshes, as on every close (FR-014).
  Content Drive does not try to tell a delete from any other close.
- **Close with no changes**: this still triggers one quiet refresh. That costs one extra list
  request and is accepted, so that changes the drive cannot see (moves, language switches) are
  never missed.
- **Save rejected by validation** (a required field left empty, a unique field clashing): the
  legacy editor shows the errors inside the panel and reports no save. The panel stays open, the
  changes stay unsaved, the list is not refreshed, and a create panel's URL keeps `createContent`.
- **Refresh with unsaved changes**: the unsaved changes are lost, as they are today for an open
  edit panel. The panel reopens from the URL.
- **Content type lookup fails** (network error, type deleted): the standard error message is
  shown and nothing opens, as today.
- **User can see but not edit the content**: the legacy editor shows its own permission message
  inside the panel. Content Drive adds no permission check of its own.
- **Legacy create form cannot be resolved** (for example, a type the user cannot create): the
  standard error message is shown and the panel does not open.
- **`createContent` names a type that doesn't exist or the user can't create**: the standard
  error message is shown, nothing opens, and the author stays on Content Drive.
- **Side-panel flag off**: legacy-editor content still opens in the panel, while new-editor content
  opens full screen, as today. The two editors behave differently in that configuration. This is
  accepted: the flag is on by default, and a full-page legacy editor could only bring the author
  back to Content Drive through the `CD_` params this work removes.
- **Side-panel flag not yet resolved** on a cold deep-link load: legacy-editor content does not
  depend on the flag and opens in the panel. New-editor content falls back to the full-page
  editor, as it does today.
- **Old links with `CD_` params, or with `editContent=new`**: Part 1's redirect drops the `CD_`
  params like any other param Content Drive doesn't know, and Content Drive keeps ignoring
  `editContent=new`, as it does today.
- **Folder dialog for a folder that is gone or out of reach** (deleted, moved to another site, no
  permission, a bad identifier): the standard error message is shown, no dialog opens, and the
  param is removed.
- **A URL with params that can't hold together** (`editContent` with `createContent`, a panel
  param with a folder-dialog param, or two folder-dialog params): Content Drive can't tell which
  one the author meant, so nothing opens and all of those params are removed.
- **Related content from a Block Editor field inside a panel**: the Block Editor's bubble menu
  navigates the whole admin window to the related content's editor, for both panels, and the
  open panel is lost. Part 1's redirect lands the author back in Content Drive with that content
  open, but not in the panel they started from. This is out of scope here.
- **Page content (HTML pages)**: opening a page from Content Drive still goes to the page editor,
  not to either side panel. Creating one follows the same create flow as any other type, as
  Content Drive's own create action already does, including the starter card's
  `createContent=htmlpageasset` link.

## Requirements *(mandatory)*

### Functional Requirements

**Routing decision**

- **FR-001**: Editing a contentlet whose type has not opted into the new editor MUST open the
  legacy editor in a Content Drive side panel, from every edit entry point (double-click, row
  context menu, toolbar), and MUST NOT navigate to the Content Search route.
- **FR-002**: Creating content of a type that has not opted into the new editor MUST open the
  legacy create form in a Content Drive side panel, and MUST NOT navigate to the Content Search
  route.
- **FR-003**: A create form opened from Content Drive, from its create action or from a
  `createContent` link and in either editor, MUST pre-select, as the new content's location, the
  folder Content Drive is showing: the folder the author is browsing, or, for a `createContent`
  link, the folder the link names (FR-024). It MUST start in Content Drive's default language.
- **FR-004**: FR-001 and FR-002 MUST work for a user whose menu does not include Content Search.
- **FR-005**: Content types that opted into the new editor MUST keep their current behavior,
  including the side-panel flag deciding between the panel and the full-page editor, except for
  the create's default language (FR-003), FR-020, FR-023, FR-025, FR-028 and FR-029.
- **FR-006**: The legacy-editor panel MUST NOT depend on the side-panel flag: FR-001, FR-002,
  FR-021 and FR-024 apply whether the flag is on, off or not yet resolved. The flag keeps deciding
  only how new-editor content opens.
- **FR-007**: Which editor opens MUST keep following each content type's own editor setting. The
  panel MUST NOT switch a legacy-editor type to the new editor.

**In-panel behavior**

- **FR-008**: The panel MUST listen only to the legacy editor events these requirements need, and
  ignore the rest: close (FR-014, FR-016), save (FR-013, FR-018, FR-025), data changed (FR-017,
  FR-018), workflow wizard (FR-009), push publish (FR-010) and compare versions. Comparing versions
  from the History tab MUST open the compare dialog over Content Drive, and its **Bring Back** MUST
  restore the chosen version in the panel, close the compare dialog and refresh the list.
- **FR-009**: Workflow actions that need extra input MUST open the workflow wizard from the panel,
  and on completion MUST hand the wizard's input back to the legacy editor in the panel, which
  applies the action.
- **FR-010**: Push publish actions MUST open the push publish dialog from the panel.
- **FR-011**: The legacy editor's links to the page editor (**Preview page** and the
  **References** tab) MUST keep opening the page editor. They are plain links, so the panel does
  not handle them.
- **FR-012**: The panel MUST show a title that identifies the content being edited, or the type
  being created.

**List refresh**

- **FR-013**: Saving in the panel, including a workflow action (with or without the wizard), MUST
  refresh the Content Drive list while the panel stays open, and keep the current folder, filters
  and page.
- **FR-014**: Closing the panel for any reason (the editor's own close, X, ESC,
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

- **FR-020**: While a panel is open, for either editor, the Content Drive URL MUST name what it
  shows: `editContent=<identifier>` with `editContentLang=<language id>` for an edit, and
  `createContent=<content type variable>` for a create, replacing today's `editContent=new`
  marker. When the author switches language inside the legacy panel, `editContentLang` MUST
  follow it, without adding a browser history entry. Closing the panel for any reason MUST remove
  all three params. Refresh, Back and shared links MUST behave the same for both editors.
- **FR-021**: Opening a Content Drive URL that names legacy-editor content (`editContent` with
  `editContentLang`) MUST open the legacy editor in the panel. This applies however the URL was
  reached, including Part 1's redirect from `c/content/<inode>`. Content Drive MUST look up the
  content type's editor setting before opening; it MUST NOT default to the new editor.
- **FR-022**: Opening a Content Drive URL that names new-editor content MUST behave as today.
- **FR-023**: The Content Drive links that open a panel (`editContent` with `editContentLang`,
  and `createContent`) are supported entry points. Part 1 redirects to them, other screens may
  link to them, and they MUST keep working for both editor kinds. A URL that combines params that
  can't hold together (`editContent` with `createContent`, a panel param with a folder-dialog
  param, or two folder-dialog params) MUST open nothing and have all of those params removed.
- **FR-024**: Opening a Content Drive URL with `createContent=<content type variable>` MUST open
  the create form for that type, following FR-002 (legacy-editor type) or today's new-editor
  create (other types). Content Drive opens on the folder the URL names (`path`), or on the site
  root when it names none, and that folder is the new content's location, in Content Drive's
  default language (FR-003). The param stays
  in the URL while the create panel is open (FR-020), so a refresh opens an empty create form for
  the same type in the same folder.
- **FR-025**: When content created in a panel is saved for the first time, for either editor,
  the URL MUST switch from `createContent` to that content's `editContent` and `editContentLang`,
  without adding a browser history entry, so a refresh reopens the saved content instead of a
  second create form.

**Removing the full-page hand-off**

- **FR-026**: Content Drive MUST NOT produce the `CD_` params. The code that produces them and
  the code that reads them MUST be removed: the producer in Content Drive's navigation, the
  Content Drive branch in the `onClose` of the full-page legacy edit and create screens (which then
  keep their behavior for users who reach them from Content Search), Part 1's `CD_` restoration in
  the route guard, and the shared helpers when nothing else uses them.
- **FR-027**: Part 1's loop check (an edit link sent from Content Drive itself is not redirected
  back into Content Drive) MUST be removed. Once Content Drive stops linking to the Content Search
  route there is no loop to prevent, so edit and create links are redirected the same way,
  wherever they come from.

**Staying inside Content Drive**

- **FR-028**: "Switch to the old editor" inside the new-editor side panel MUST reopen the same
  content, in the same language, in the legacy panel, without leaving Content Drive. It still
  turns the new editor off for that content type, as it does today. The full-page new editor keeps
  its current behavior.
- **FR-029**: A load failure in the new-editor side panel MUST show the standard error message
  and close the panel, keeping the current folder, filters and page. It MUST NOT navigate to the
  Content Search route. The full-page new editor keeps its current behavior.

**Folder dialogs in the URL**

- **FR-030**: While a folder dialog is open, the Content Drive URL MUST name it: `createFolder=true`
  for New Folder (its parent is the folder in `path`, or the site root), `editFolder=<folder
  identifier>` for Folder Settings, and `folderPermissions=<folder identifier>` for Edit
  Permissions. Closing the dialog for any reason (save, cancel, X, ESC, browser Back) MUST remove
  the param.
- **FR-031**: Opening a Content Drive URL with one of these params MUST open the matching dialog,
  for the folder the param names, resolving that folder on load when the dialog needs more than
  its identifier. The param is read on load, like the panel params.
- **FR-032**: Browser Back while a folder dialog is open MUST close it the same way its own close
  does, and opening one MUST add a single history entry, like opening a panel.
- **FR-033**: A folder dialog opened from the URL MUST follow the same permission rules as the
  context menu that opens it: Folder Settings needs edit permission on the folder, and Edit
  Permissions needs permission to edit its permissions. When the folder can't be resolved or the
  author lacks the permission, the standard error message is shown, no dialog opens and the param
  is removed.

**Tests**

- **FR-034**: Automated tests MUST cover the routing decision (new vs legacy editor, flag on vs
  off, edit vs create vs `editContent` link vs `createContent` link), the handling of each legacy
  editor event, the list refresh after save and after every kind of close, the URL params for each
  panel state (FR-020, FR-024, FR-025), the language switch in the legacy panel, Bring Back, URLs
  with params that can't hold together (FR-023), FR-026 through FR-029, and the folder-dialog
  params (FR-030 through FR-033).

### Key Entities

- **Edit panel request**: what Content Drive has asked to open: the content (or content type
  for create), its language, and its title. This already exists for the new editor.
- **Editor kind**: which editor the open request uses, new or legacy, based on the content
  type's editor setting. Everything that follows the open panel (URL, Back, refresh on close)
  works the same for both kinds.
- **Panel URL params**: `editContent` and `editContentLang` for an edit, `createContent` for a
  create. They describe the open panel and are the same links Part 1 builds from other screens.
- **Folder-dialog URL params**: `createFolder`, `editFolder` and `folderPermissions`. They describe
  the open folder dialog, and at most one of them, or of the panel params, is in the URL at a time.
  A URL that combines params that can't hold together opens nothing and has all of them removed
  (FR-023).
- **Legacy editor event**: a notification the embedded legacy editor sends to the admin UI. The
  panel listens to close, save, data changed, workflow wizard, push publish and compare versions,
  and ignores every other one (FR-008).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user without Content Search in their menu can edit and create content of every
  legacy-editor content type from Content Drive, with the side-panel flag on or off, and, with the
  flag on, can switch a type back to the old editor from the new editor's side panel. Zero of these attempts redirect them
  to another portlet.
- **SC-002**: After saving or closing the legacy-editor panel, the author is on the same folder,
  filters and page they started from in 100% of cases, with no manual navigation.
- **SC-003**: Changes made in the panel (edits, new content, status changes, deletions) show in
  the list by the time the panel has closed, without a manual refresh.
- **SC-004**: Closing the panel never shows the list's loading placeholder.
- **SC-005**: Zero unsaved edits are discarded without the author confirming it, except on a page
  refresh, which loses them as it does today.
- **SC-006**: New-editor content sees no change in behavior apart from the create panel's URL
  (FR-020, FR-025) and the two paths in FR-028 and FR-029. Its existing automated tests pass,
  apart from the ones that assert the `editContent=new` marker. The navigation tests that expect
  legacy-editor content to route to Content Search are replaced, since that routing is removed.
- **SC-007**: Opening legacy-editor content from any screen outside Content Drive (Query Tool,
  Publishing Queue, Block Editor, form-entry emails) opens the legacy editor in 100% of cases.
  Zero of them open the new editor.
- **SC-008**: Refreshing the page with any panel open reopens the same panel: the same content in
  the same language for an edit, and a create form for the same type in the same folder, or the
  saved content once a create has been saved.
- **SC-009**: Refreshing the page with a folder dialog open reopens the same dialog for the same
  folder, and closing it leaves no folder-dialog param in the URL.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The Content Drive portlet's edit and create routing, its
  side-panel host and its URL state. It embeds the **legacy content editor** (the JSP-based
  edit-contentlet screen that Content Search and the page editor already embed), which is part of
  the older product surface. The legacy editor itself is **not** changed. It is only hosted in a
  new place, using the events it already sends. The full-page legacy edit and create screens lose
  the `CD_` branch of their close handler, and keep their behavior for Content Search.
- **Backward-compatibility expectations**: New-editor content, page content, and the Content
  Search portlet itself keep working as today. There are no REST, database or index changes, so
  the feature is safe to roll back. Deliberate changes:
  - With the side-panel flag off, legacy-editor content opens in the panel instead of the
    full-page legacy editor, and a Content Drive link to it opens the legacy panel instead of the
    full-page new editor, which did not match that content type's editor setting (FR-006, FR-021).
  - The create panel's URL changes from `editContent=new` to `createContent=<type>`, for both
    editors (FR-020).
  - The folder dialogs write a URL param while they are open (FR-030).
  - Content Drive no longer sends anyone to the full-page legacy editor, so the `CD_` round trip
    added in #33726 is removed (FR-026).
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
  - Part 1 on its own opens legacy-editor content that arrives from other screens in the new
    editor (User Story 5). The fix is deliberately kept here rather than in Part 1 (decided
    2026-09-30). Both parts ship in the same PR (decided 2026-09-30, closing #37829), so no
    release carries Part 1 without this fix.
  - The `CD_` params and Part 1's loop check are removed in this work, and the legacy panel no
    longer depends on the side-panel flag (decided 2026-09-30, on review). This replaces an earlier
    decision to keep the `CD_` round trip while the flag exists. The cost is that the flag no
    longer works as a kill switch for the legacy panel, and that with the flag off the two editors
    open differently. Removing the flag altogether was left out because Query Tool, the
    relationship field and UVE read it too.
  - The create panel's URL uses `createContent=<type>`, the param Part 1 already builds, rather
    than `editContent=new` plus a type param, so an incoming link and an open panel share one
    format (decided 2026-09-30, on review).
  - The panel listens only to the legacy editor events its requirements need, rather than the
    full contract the full-page wrapper handles, since the product is moving away from the legacy
    editor (decided 2026-10-01, on review). A URL with params that can't hold together opens
    nothing, rather than letting one kind of param win (decided 2026-10-01, on review).
  - The folder dialogs follow the same URL rules as the panels (decided 2026-09-30, on review, as
    a nice to have). It is the lowest-priority story (US8) because it does not block replacing
    Content Search or Site Browser.

## Assumptions

- The legacy editor events listed in FR-008 are stable and enough for these requirements.
  Embedding it needs no change to the legacy editor.
- The legacy editor sends no event when the author switches language: it reloads itself in the
  new language. Bring Back also reloads it, with the restored version, and sends no save event.
  The panel has to notice both from the reload, and refresh the list itself after a Bring Back.
- The legacy editor's links to the page editor are plain links. The `edit-page` event the editor
  defines is never sent by any of its screens, so the panel needs nothing for them.
- Content Drive already loads its default language, so starting a create in it needs no extra
  request.
- The server already lets a user without Content Search in their menu load the legacy editor.
  Only the admin UI route is gated. Content-level permissions are still enforced by the editor
  itself.
- A workflow action completed through the wizard reports a save the same way a plain save does.
  This was checked in the legacy editor: the wizard hands its input back to the editor, which
  runs its normal save, and that save sends the save notification (or the close or page-deleted
  notification when the action removed the content). It only holds if the panel hands the
  wizard's input back to the editor (FR-009); the refresh on every close (FR-014) is the safety
  net if it doesn't.
- Push publish reaches the panel only through workflow actions: the legacy editor's action panel
  lists workflow actions, and one with push publish inputs opens the workflow wizard or the push
  publish dialog (FR-009, FR-010). The legacy editor has no **Add to bundle** action of its own. In
  Content Search it lived in the list, and Content Drive already offers it, together with push
  publish, from its own action center.
- Both editors report the saved content when a save succeeds (the new-editor panel with the saved
  contentlet, the legacy editor with the save notification's data), which is what FR-025 needs to
  switch the URL to the edit params.
- A folder can be resolved by its identifier when a folder-dialog URL is opened: the folder REST
  API already looks folders up by id. The Edit Permissions dialog only needs the identifier.
- The refresh after a save (FR-013) also uses the quiet refresh, like the refresh on close. The
  new-editor panel's own refresh after a save is out of scope and stays as it is.
- The unsaved-changes prompt reuses the new-editor side panel's existing wording. No new
  user-facing text is needed beyond what the panel title requires.
- The quiet refresh of the list is the only feedback Content Drive gives after a save or a close.
  It shows no toast or other notice of its own. Messages the legacy editor or the workflow and
  push publish dialogs already show keep appearing as they do today.
- Part 1 builds the link formats this spec reads and writes: `editContent` with `editContentLang`
  for edits, and `createContent` with an optional `path` for creates. Both parts are on the same
  branch, so a rename changes both together.
