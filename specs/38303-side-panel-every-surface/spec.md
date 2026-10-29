# Feature Specification: Open every content edit and create in the side panel, on every surface

**Feature Branch**: `38303-sidepanel-everything`

**Created**: 2026-10-28

**Status**: Draft

**Type**: New Feature

**Related GitHub Issue**: [#38303](https://github.com/dotCMS/core/issues/38303)

**Input**: User description: "https://github.com/dotCMS/core/issues/38303 — Open every content edit and create in the side panel, on every surface. The side panel becomes the only place content is edited or created from inside the admin; it shows the new editor when the content type opted into it and the legacy editor otherwise; every edit dialog goes away."

## Problem Statement

An author who edits or creates a piece of content inside the dotCMS admin lands in a different container depending on where they started and on which editor the content type uses. Today there are five: a side panel over the current screen, a centered dialog, an iframe dialog, a full-screen route and a new browser tab. The same action, "edit this content", looks and behaves differently in Content Drive, in the page editor (UVE), in the Query Tool, in the Pages portlet, in the old JSP portlets, in the Publishing Queue and when following a link.

That inconsistency costs authors their place. A new tab loses the list they were filtering. A full-screen route loses the page they were editing. A dialog cannot stack, so a related item opened from inside an editor navigates the whole window away and leaves a "back to content" breadcrumb behind in browser storage. Three screens also keep a second code path for when the side-panel feature flag is off, so each one behaves two ways and has to be tested twice.

The groundwork is done. Every side panel in the admin already opens in one outlet at the root of the application, so any screen can mount either editor in a panel. What is missing is one decision about *what* opens: each screen still picks the editor and the container on its own.

This work makes the side panel the only way content is edited or created from inside the admin. One request describes what to open, one opener decides which editor shows, and both editors report the same events to whoever opened them. Every edit dialog is deleted. It is also the first step toward hosting the new editor's fields outside the full editor, starting with the UVE quick edit, which is tracked separately.

## Scope

**What this work delivers**

1. **One request, one opener, one set of events.** A single description of "what to open" (the content, or the content type to create, with its language, folder, title, variant and relationship context) replaces the two request shapes in use today. The caller never says which editor to use. One opener, living beside both editors, picks the new editor when the content type opted into it and the legacy editor otherwise, with the same rules and the same fallback the admin's edit-URL resolver applies today. Both editors report the same events to the opener: `saved` (always carrying the saved content), `languageChanged`, `pageEditorRequested`, `deleted` and `closed`.
2. **Every admin surface opens the panel.** Content Drive, every UVE entry point (including the SDK's edit and create calls from a headless page), the Query Tool, the Pages portlet, the legacy JSP portlets (old content search, Sites), the Publishing Queue dialogs and the two deep-link routes all open the side panel, for both editor types, with no dialog and no new tab.
3. **Legacy screens that are not editors also move into a panel.** Adding content or a widget to a container, adding a form, the SDK's menu reorder and comparing versions open in a generic legacy-screen panel over the UVE, instead of the UVE's own iframe dialog.
4. **Fields never open panels themselves.** The Relationship field and both block editors inside the new editor announce that they want to open content and wait for the result; whoever hosts the form opens the panel. Inside a panel, an existing related item opens in place with a breadcrumb back; a new one, and a block editor's "edit contentlet", stack a new panel. Block editors running inside the legacy editor send their request out of the iframe so the panel that hosts the legacy editor can stack a new panel over itself.
5. **The side-panel flag is no longer read anywhere in the frontend.** The panel always opens, and the only switch left is each content type's editor setting.
6. **Every edit dialog is deleted.** The UVE's iframe dialog and its store, the centered new-editor dialog, the admin's legacy iframe dialog wrapper and the service that drove it, the two unused add-content and reorder-menu wrappers, the block editors' window navigation with its confirm prompt and its browser-storage breadcrumb, and the new-editor panel's dialog-compatibility callbacks.

**Out of scope**

- The full-screen new editor at `#/content/<inode>` stays, for deep links and bookmarks.
- The `edit-page` event (opens the page editor) and the `edit-task` event (the workflow task screen) keep their current behavior.
- Query Tool results that are pages keep opening the page editor.
- "Create new" in a Relationship field stays disabled for content types on the legacy editor.
- Removing `FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL` from the backend is a follow-up issue.
- Hosting the new editor's fields in the UVE quick edit is a separate issue.

**Decisions on the issue's three open questions**

The issue left three questions for this spec. Each is answered here from how the product behaves today, and each is a decision the reviewer can overturn in this PR.

- **URL-mapped content in the UVE.** After a legacy save, the UVE today fetches the saved content and compares its detail URL to the page it is showing; if they differ it loads the new URL, otherwise it reloads the page. The new-editor path only reloads. Since `saved` always carries the saved content in the new contract, the UVE can run that same comparison for both editors. The contract does **not** add a "URL changed" flag: the opener derives it from the saved content (FR-018).
- **Version restore.** The legacy editor reloads itself with the restored version and sends no save. Content Drive's legacy panel today notices the reload and reports a restore so the list refreshes. In the new contract a restored version is reported as `saved`, carrying the restored content, from both editors. The opener does not need to tell a restore from a save: in both cases the content changed and the screen behind must refresh (FR-019).
- **Workflow actions in the legacy editor.** When the author runs a workflow action, the legacy editor saves through its normal path and sends its save notification, or closes when the action removed the content. The UVE's dialog today reloads the page on top of that as a safety net. In the new contract a workflow action is reported through the same events (`saved`, `deleted` or `closed`), and the contract adds no workflow event. Openers that refresh on `closed` as well as on `saved` keep the safety net (FR-020).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Edit and create from the page editor without a dialog (Priority: P1)

An author working on a page in the UVE edits a piece of content on it, drops a new one from the palette, adds one with a container's "+", edits the page's properties, edits URL-mapped content, translates the page, or calls edit or create from a headless page through the SDK. Whatever the entry point and whatever editor the content type uses, the editor opens in a side panel over the page. After saving, the page shows the new values and the panel stays open; closing it leaves the author exactly where they were.

**Why this priority**: The UVE is the surface with the most entry points and the only one that still mounts its own iframe dialog for legacy types and a centered dialog with the flag off. It is where the inconsistency is most visible and where the biggest deletion lands.

**Independent Test**: Open a traditional page and a headless page in the UVE. Trigger each entry point for a content type on the new editor and one on the legacy editor. Each opens the side panel, no dialog appears, and after a save the page shows the saved values.

**Acceptance Scenarios**:

1. **Given** a page in the UVE with a contentlet of a new-editor type, **When** the author opens its full editor, **Then** the new editor opens in a side panel and no dialog opens.
2. **Given** a page in the UVE with a contentlet of a legacy type, **When** the author opens its full editor, **Then** the legacy editor opens in a side panel and the UVE's iframe dialog does not open.
3. **Given** a content type in the palette, **When** the author drops it into a container, **Then** a create panel opens for that type and, after the first save, the new content appears once in that container.
4. **Given** a create panel opened from the palette or from a container "+", **When** the author saves a second time in the same panel, **Then** the content is not added again and no duplicate message is shown.
5. **Given** an edit panel opened from the UVE, **When** the author saves, **Then** the page reflects the saved values on a traditional page and on a headless page, and the panel stays open.
6. **Given** URL-mapped content open from the UVE, **When** the author changes the field that drives its URL and saves, **Then** the UVE loads the content's new URL instead of reloading a URL that no longer exists.
7. **Given** a page with an experiment running, **When** the author edits content or page properties from the UVE, **Then** the editor opens the experiment's variant.
8. **Given** a headless page using the SDK, **When** the page calls `editContentlet()` on a new-editor type, **Then** the new editor opens in the side panel, not the legacy iframe dialog it opens today.
9. **Given** the Edit Source fallback or the translate-page action, **When** it needs the editor, **Then** the editor opens in the side panel.

---

### User Story 2 - Every other admin screen opens the panel (Priority: P1)

An author editing from Content Drive, the Query Tool, the Pages portlet, a legacy JSP portlet, the Publishing Queue or a direct link always gets the same side panel over the screen they are on. The list behind refreshes after a save, and closing the panel returns them to the same screen, dialog or results.

**Why this priority**: These screens are where authors spend their day. Replacing the new-tab and full-screen hand-offs is the user-visible payoff: nobody loses their list, filters or dialog.

**Independent Test**: For each screen, edit and create content of a new-editor type and a legacy type. Each opens the side panel over the screen, and the screen refreshes after a save.

**Acceptance Scenarios**:

1. **Given** Content Drive in list or grid view, **When** the author edits or creates content of either type, **Then** the side panel opens, and the list refreshes after a save, as it does today.
2. **Given** Query Tool results, **When** the author clicks a result of either type, **Then** the side panel opens instead of a new tab, and the results rerun after a save.
3. **Given** Query Tool results, **When** the author clicks a result that is a page, **Then** the page editor opens, as today.
4. **Given** the Pages portlet, **When** the author chooses Edit on a page, **Then** the side panel opens, with the new editor for page types on the new editor, and the list refreshes after a save.
5. **Given** a legacy JSP portlet such as the old content search or Sites, **When** the author edits or creates content, **Then** the side panel opens over the portlet and the portlet's list refreshes after a save or a close.
6. **Given** the Publishing Queue's asset list or select-bundle dialog, **When** the author edits an asset, **Then** the side panel opens above the dialog, and closing the panel returns to the dialog with focus on the row that opened it.
7. **Given** a fresh browser tab, **When** the author opens `/<portlet>/<inode>` or `/c/content/new/<contentType>`, **Then** the portlet renders and the side panel opens over it.

---

### User Story 3 - Both editors behave the same inside the panel (Priority: P1)

Whichever editor the panel shows, the author can rely on the same behavior: saving keeps the panel open and refreshes the screen behind, closing with unsaved changes asks first, deleting closes the panel, switching language keeps the panel open in the new language and the screen behind follows, and if the editor choice cannot be made the legacy editor opens rather than nothing.

**Why this priority**: Without one behavior the unification is cosmetic. Every surface in US1 and US2 depends on these rules holding for both editors.

**Independent Test**: On one surface, run the panel-behavior checklist against a content type on each editor: save, save again, close unsaved through each route, delete, switch language, and force a content type lookup failure.

**Acceptance Scenarios**:

1. **Given** an open panel with unsaved changes, **When** the author presses the X, Escape, clicks the mask or uses browser Back, **Then** a prompt asks before discarding; "keep editing" keeps the changes and "discard" closes.
2. **Given** an open panel with no changes, **When** the author closes it through any of those routes, **Then** it closes with no prompt.
3. **Given** an open panel, **When** the author deletes the content from inside the editor, **Then** the panel closes and the opener is not told a save happened.
4. **Given** an open panel, **When** the author switches language inside the editor, **Then** the panel stays open in the new language and the opener reflects it (Content Drive's URL, the UVE's page language).
5. **Given** a request to edit content whose content type cannot be looked up, **When** the panel opens, **Then** it shows the legacy editor.
6. **Given** a panel opened from any surface, **When** the author saves, **Then** any confirmation toast appears in the screen that opened the panel.

---

### User Story 4 - Content that opens other content stays in the panel stack (Priority: P2)

An author in an editor clicks an existing related item in a Relationship field, creates a new related item, or uses "edit contentlet" in a block editor. The related content opens in the panel stack instead of navigating the window away: in place with a breadcrumb when the author is already in a panel and opening an existing item, stacked on top otherwise. Unsaved work in the editor underneath is still there when the stacked panel closes, and the field shows the saved result.

**Why this priority**: This is where today's behavior is worst (window navigation, a confirm prompt, a breadcrumb in browser storage) and where the contract's "fields do not open panels" rule is proven.

**Independent Test**: From the full-screen editor and from an editor inside a panel, open an existing related item, create a new one, and use "edit contentlet" in the new and the old block editor, inside the new editor and inside the legacy editor.

**Acceptance Scenarios**:

1. **Given** the full-screen new editor with a Relationship field, **When** the author clicks an existing related item, **Then** it opens in a new side panel, and after a save its row in the field shows the saved title.
2. **Given** a new editor inside a side panel with a Relationship field, **When** the author clicks an existing related item, **Then** that panel's content is replaced with the related item and a breadcrumb leads back to the previous content.
3. **Given** a Relationship field in either host, **When** the author chooses "Create new", **Then** a side panel stacks on top, and after a save the new item is listed in the field.
4. **Given** either block editor inside the new editor with a contentlet block, **When** the author chooses "Edit contentlet", **Then** the contentlet opens in a stacked side panel with no confirm prompt, and the unsaved changes in the editor underneath are intact when the panel closes.
5. **Given** either block editor inside the legacy editor, itself in a side panel, **When** the author chooses "Edit contentlet", **Then** the contentlet opens in a second side panel stacked on top.
6. **Given** a stacked panel, **When** the author presses Escape or clicks the mask, **Then** only the front panel closes and the one underneath is unchanged.
7. **Given** a stacked panel opened by "Create new", **When** the author saves, fixes a typo and saves again, **Then** the field lists the new item once, with the latest title.

---

### User Story 5 - Legacy screens open in a panel over the page editor (Priority: P2)

An author in the UVE adds content or a widget to a container, adds a form, reorders a menu from a headless page, or compares versions from the legacy editor. The content selector, the form selector, the menu reorder screen and the compare dialog open in a side panel or the admin's root compare dialog, and the UVE's own iframe dialog is gone.

**Why this priority**: These are the last uses of the UVE's iframe dialog. Deleting that dialog, one of the issue's goals, is only possible once they move.

**Independent Test**: From a traditional page and a headless page, add content, add a widget, add a form, call `reorderMenu()` from the SDK, and compare versions from a legacy editor in a panel.

**Acceptance Scenarios**:

1. **Given** a container on a page, **When** the author chooses to add content or a widget, **Then** the content selector opens in a side panel and the chosen item is placed in the container once.
2. **Given** a container that accepts forms, **When** the author chooses to add a form, **Then** the form selector opens as side panel content.
3. **Given** a headless page using the SDK, **When** it calls `reorderMenu()`, **Then** the menu reorder screen opens in a side panel.
4. **Given** the legacy editor in a side panel, **When** the author compares versions, **Then** the admin's root compare dialog opens, and bringing a version back refreshes the page.
5. **Given** a legacy screen in a panel, **When** the panel is at its default or expanded width, **Then** the screen is usable at both.
6. **Given** a legacy screen in a panel, **When** the author presses Escape inside the iframe, **Then** the panel closes unless a dialog inside the iframe handled the key first.

---

### User Story 6 - Panel stacking and navigation behave consistently (Priority: P3)

Panels stack in open order, the front one is the only one that reacts to Escape and the mask, the main navigation collapses when the first panel opens and is restored when the last one closes, and a panel opened over a modal dialog takes focus until it closes.

**Why this priority**: These rules already hold for the existing panels; this story makes sure the new openers and the new stacking cases (legacy editor over legacy editor, panel over the Publishing Queue dialog) keep them.

**Independent Test**: Open a panel from inside a panel on each surface, and open a panel from the Publishing Queue dialog with a content type on each editor.

**Acceptance Scenarios**:

1. **Given** one panel open, **When** a second opens from inside it, **Then** the second stacks in front, and closing it returns to the first unchanged.
2. **Given** the main navigation expanded, **When** the first panel opens, **Then** the navigation collapses, and it is restored only when the last panel closes.
3. **Given** the Publishing Queue dialog, **When** a panel opens over it, **Then** the panel and its mask render above the dialog, nothing in the dialog can be clicked, typing in the editor is not pulled back into the dialog, Tab moves only between the panel's controls, Escape closes the panel first and only a second Escape closes the dialog.
4. **Given** the legacy editor in a panel over the Publishing Queue dialog, **When** the author types inside the iframe or presses Escape inside it, **Then** the same rules hold.

---

### Edge Cases

- **Save rejected by validation**: the editor shows the errors inside the panel and reports no save. The panel stays open, the screen behind does not refresh.
- **Content type lookup fails**: the panel opens the legacy editor, which handles every type. No error is shown for the lookup itself.
- **Legacy create screen cannot be resolved** (a type the user cannot create): the standard error message is shown and no panel opens.
- **User can see but not edit the content**: the editor shows its own permission message inside the panel. The opener adds no permission check of its own.
- **Legacy save of a page for the first time**: the legacy editor asks to open the page editor instead of reporting a save. The opener closes the panel and opens the page editor, as Content Search does.
- **Delete from the panel**: the opener receives `deleted` and then `closed`, never `saved`. A screen that refreshes on close no longer shows the content.
- **Workflow action that removes the content** (archive, delete, destroy): the legacy editor calls its window hook with `deleted-contenlet` (or `deleted-page` for a page) and then sends `close`; the new editor leaves the content through its host. Both are reported as `deleted` then `closed`, never `saved`.
- **Legacy save notification without the saved content's language**: the `saved` payload is fetched by inode, so every listener gets the same shape regardless of editor.
- **Refresh with a panel open**: unsaved changes are lost, as today. Surfaces that write the open panel to their URL (Content Drive) reopen it; others do not.
- **Panel opened over a modal dialog, then the dialog closes underneath** (the Publishing Queue dialog times out or is closed by code): the panel stays open and closes normally; focus returns to the screen.
- **Second save from a create panel in the UVE**: the content is already in the container, so it is not inserted again and no duplicate message is shown.
- **A stacked panel whose opener panel is closed**: closing the front panel still returns to the panel underneath; the underneath panel's own close guard is untouched.
- **The legacy editor inside a panel receives an `edit-contentlet` or `create-contentlet` request from a block editor**: the request is forwarded to the opener, which stacks a new panel; the legacy editor's own window is never navigated.
- **Panel opened by a deep link, then closed**: the author stays on the portlet the link named, with no panel open.
- **Escape inside a legacy iframe while a dialog inside the iframe is open**: the dialog consumes the key and the panel stays open.

## Requirements *(mandatory)*

### Functional Requirements

**The request**

- **FR-001**: One request type MUST describe every edit and create from inside the admin. For an edit it carries the inode and identifier; for a create, the content type. In both cases it carries the language, the folder, the title to show, the variant and the relationship context when the request comes from a Relationship field. It replaces both request shapes used today, at every call site.
- **FR-002**: The caller MUST NOT say which editor to use. The request names content, never an editor.

**The opener**

- **FR-003**: One opener MUST open the panel for a request. It MUST pick the new editor when the content type opted into the new editor, and the legacy editor otherwise, applying the same rules and the same fallback as the admin's edit-URL resolver.
- **FR-004**: When the content type lookup fails, the opener MUST open the legacy editor.
- **FR-005**: Screens MUST be able to drive the opener from state (a request that becomes set opens the panel, clearing it closes the panel), with the same shape the existing side-panel helper offers. One-off handlers MUST be able to open a request directly.
- **FR-006**: The opener MUST live beside both editors, so no surface has to know about either editor's component.

**The events**

- **FR-007**: Both editors MUST report the same events to the opener: `saved`, `languageChanged`, `pageEditorRequested`, `deleted` and `closed`.
- **FR-008**: The `saved` payload MUST always be the saved contentlet. For a legacy save, the opener MUST fetch the saved contentlet by inode before reporting it, so every listener receives the same shape.
- **FR-009**: `deleted` MUST be reported when the content was removed from inside the editor (a delete, archive or destroy action), followed by `closed`, and `saved` MUST NOT be reported for a delete. The legacy editor already announces a removal through a hook on its own window (`ngEditContentletEvents`, which it calls with `deleted-contenlet` when the content it just saved no longer exists, and with `deleted-page` for a page) before it sends its generic `close`; the panel supplies that hook when the editor loads, so the legacy screens do not change.
- **FR-010**: The legacy editor's workflow wizard, compare dialog and push publish MUST keep going through the admin's root services, as they do today.
- **FR-011**: The new-editor panel's dialog-compatibility callbacks (`onContentSaved`, `onCancel`) MUST be removed; openers consume the events instead.

**Legacy screens**

- **FR-012**: A generic panel MUST load any legacy admin URL (the content selector, the menu reorder screen) and pass the iframe's events to its opener.
- **FR-013**: The legacy editor panel MUST be built on that generic panel and MUST live beside both editors, so Content Drive and every other surface share it.
- **FR-014**: The form selector MUST open as side panel content, replacing the UVE's form-selector dialog.
- **FR-015**: Comparing versions from a legacy editor in a panel MUST open the admin's root compare dialog, not a nested dialog.

**Surfaces**

- **FR-016**: Every surface listed in User Stories 1 and 2 MUST open the side panel through the opener, for content types on either editor, and MUST NOT open a dialog, a new tab or a full-screen route for an edit or a create.
- **FR-017**: After a `saved` event, each surface MUST refresh what it shows: Content Drive its list, the UVE its page, the Query Tool its results, the Pages portlet its list, a legacy JSP portlet its list through the admin's iframe reload, and the Publishing Queue its dialog contents. Every surface MUST also refresh on `closed`, whichever editor the panel showed: a move to another folder or a translation saved after a language switch inside the legacy editor sends no event the opener can use, so a refresh on close is the only way the screen behind finds out. The cost is one quiet refresh per close, which Content Drive already pays today.
- **FR-018**: After a `saved` event for URL-mapped content, the UVE MUST compare the saved content's detail URL with the page it is showing and load the new URL when they differ, for both editors. The contract adds no URL-changed flag; the UVE derives it from the saved contentlet.
- **FR-019**: A restored version MUST be reported as `saved`, carrying the restored contentlet, from both editors. Content Drive's separate restore notification is replaced by it.
- **FR-020**: A workflow action run inside the legacy editor MUST be reported through the same events (`saved`, `deleted` or `closed`). The contract adds no workflow event.
- **FR-021**: Content created from the UVE's palette or a container "+" MUST be inserted into that container once, on the first save. A second save in the same panel MUST NOT insert it again or show a duplicate message.
- **FR-022**: On a page with an experiment running, every editor and legacy screen opened from the UVE MUST edit the experiment's variant.
- **FR-023**: The SDK's `editContentlet()` MUST open the editor the content type chooses, in the side panel, instead of always opening the legacy iframe dialog.
- **FR-024**: The Pages portlet's Edit action (its only edit entry; there is no View action) MUST open the panel through the opener, so page types on the new editor get the new editor, and MUST reload the list after a save.
- **FR-025**: The legacy JSP events `edit-contentlet`, `create-contentlet`, `edit-host` and `create-host` MUST open the panel through the opener. The `edit-page` and `edit-task` events keep their current behavior.
- **FR-026**: The deep links `/<portlet>/<inode>` and `/c/content/new/<contentType>` MUST render the portlet and open the panel over it. Closing the panel leaves the author on the portlet.
- **FR-027**: Query Tool results that are pages MUST keep opening the page editor.

**Fields and nested editors**

- **FR-028**: Fields that open other content (the Relationship field and both block editors inside the new editor) MUST NOT open panels. They MUST emit a request to open content together with a way to receive the saved result, and the host of the form opens the panel.
- **FR-029**: Inside a panel, an existing related item MUST open in place, replacing the panel's content and adding a breadcrumb back to the previous content, as today. In the full-screen editor it MUST open in a new panel.
- **FR-030**: "Create new" in a Relationship field MUST stack a new panel in both hosts, and the field MUST list the new item after a save. The new item MUST be added once: a later save in the same panel updates that row (matched by identifier) instead of adding another, as FR-021 requires for the UVE. "Create new" stays disabled for content types on the legacy editor.
- **FR-031**: "Edit contentlet" in either block editor inside the new editor MUST open a stacked panel with no confirm prompt and MUST leave the unsaved changes in the editor underneath intact.
- **FR-032**: Block editors inside the legacy editor MUST send `edit-contentlet` to the container hosting the legacy editor instead of navigating the window. The legacy editor panel MUST forward `edit-contentlet` and `create-contentlet` to its opener, which stacks a new panel.
- **FR-033**: The block editors' window navigation, their confirm prompt and their writes of the relationship-return breadcrumb to browser storage MUST be removed.

**Panel behavior, both editors**

- **FR-034**: Closing a panel with unsaved changes through the X, Escape, a mask click or browser Back MUST ask before discarding. "Keep editing" keeps the changes; "discard" closes. Browser Back reaches the panel on the surfaces that put the open panel in their URL and route Back to it (Content Drive, the Query Tool), as today; the other surfaces do not write the panel to the URL.
- **FR-035**: Saving MUST keep the panel open and the panel MUST report `saved` on every save, so "save, edit again, save again" refreshes the screen behind each time.
- **FR-036**: Switching language inside the editor MUST keep the panel open in the new language and report `languageChanged`, so the opener can follow it.
- **FR-037**: Confirmation toasts MUST appear in the screen that opened the panel.
- **FR-038**: Panels MUST stack in open order; Escape and a mask click MUST close only the front panel; the main navigation MUST collapse when the first panel opens and be restored when the last closes.
- **FR-039**: A panel opened over a modal dialog MUST render above it with its mask, MUST hold focus (typing and Tab stay in the panel), MUST close on the first Escape with the dialog closing only on a second, and MUST return focus to the dialog's originating row when it closes.
- **FR-040**: Escape pressed inside a legacy iframe MUST close the panel unless a dialog inside the iframe handled it first.
- **FR-041**: Legacy screens hosted in a panel MUST be usable at the panel's default and expanded widths.

**Flag and cleanup**

- **FR-042**: No frontend code MUST read `FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL`. With the flag set to false on the server, every surface still opens the side panel.
- **FR-043**: The UVE's iframe dialog and its store (in the editor and in the shell), the centered new-editor dialog, the admin's legacy iframe dialog wrapper and the service that drove it, and the unused add-content and reorder-menu wrappers MUST be deleted, and nothing MUST reference them.

### Key Entities

- **Edit request**: What to open. Either an edit (inode and identifier) or a create (content type), plus the language, folder, title, variant and, when it comes from a Relationship field, the relationship context. Never names an editor.
- **Editor choice**: Derived from the content type's editor setting: new editor when the type opted in, legacy editor otherwise or when the lookup fails. Owned by the opener, cached per content type for the session as the edit-URL resolver does today.
- **Panel events**: The five things an editor can tell its opener: content was saved (with the contentlet), the language changed (with the language), the page editor was requested (with the page URL and language), the content was deleted, and the panel closed.
- **Open-content request from a field**: What a Relationship field or a block editor emits when it wants other content opened: an edit request plus a reply channel for the saved contentlet. The host decides whether to stack or replace.
- **Legacy screen request**: A legacy admin URL to load in the generic panel, with the events the opener wants forwarded.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Across the surfaces in User Stories 1 and 2, 100% of edit and create actions open the side panel, for both editor types, and 0 open a dialog, a new tab or a full-screen route. Checked with the side-panel flag on and with it off.
- **SC-002**: 0 frontend reads of the side-panel flag remain, and the QA pass with the flag off produces identical results to the pass with it on.
- **SC-003**: After a save from any surface, the screen behind shows the saved values without the author reloading it, in 100% of the QA cases.
- **SC-004**: Content created from the palette or a container "+" appears exactly once in the container after any number of saves in the same panel; 0 duplicate messages.
- **SC-005**: Opening related content from a Relationship field or a block editor never leaves the current screen: 0 window navigations and 0 writes of the relationship-return breadcrumb to browser storage.
- **SC-006**: 100% of unsaved-changes closes through the X, Escape, the mask and browser Back show the prompt, for both editors, on every surface.
- **SC-007**: Every stacking case in the QA plan (panel over panel, legacy editor over legacy editor, panel over the Publishing Queue dialog) passes its focus and Escape checks.
- **SC-008**: The four dialogs and the service named for deletion are gone, with 0 references in the codebase.
- **SC-009**: The team can describe how to edit content from any admin surface in one sentence: it opens in the side panel, with the editor the content type chose.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: Every admin surface that edits or creates content: Content Drive, the page editor (UVE) and its SDK, the Query Tool, the Pages portlet, the Publishing Queue, the two deep-link routes, and the legacy JSP portlets through the admin's custom-event handler. It embeds the **legacy content editor** and other legacy admin screens (content selector, menu reorder) in a new place, using the events they already send. The legacy screens themselves are **not** changed except for the block editors inside the legacy editor, which stop navigating the window and send an event out instead.
- **Backward-compatibility expectations**: No REST, database or index change, so the work is safe to roll back. The SDK's `editContentlet()`, `createContentlet()` and `reorderMenu()` keep their signatures; only where the result opens changes. The legacy editor's workflow wizard, compare and push publish keep working through the admin's root services. Deliberate behavior changes:
  - Every surface that opened a dialog, a new tab or a full-screen route for an edit now opens the side panel (FR-016).
  - The side-panel flag is no longer honored by the frontend; the panel is the only container (FR-042). The flag stays on the backend until the follow-up removes it.
  - The SDK's `editContentlet()` opens the new editor for types that opted in, where it always opened the legacy dialog (FR-023).
  - The Pages portlet opens the new editor for page types that opted in, where it always opened the legacy dialog (FR-024).
  - Related content from a block editor opens in a stacked panel instead of navigating away; the confirm prompt and the browser-storage breadcrumb go away (FR-031, FR-033).
  - Content Drive's separate restore notification becomes a `saved` event (FR-019).
- **Known related decisions**:
  - The root side-panel outlet already exists and every panel in the admin opens there (#37970). This work builds on it and does not change it.
  - Content Drive already opens both editors in the panel and already stopped reading the side-panel flag (#37759). Its legacy panel is the one that moves beside the editors and is generalized.
  - The admin already has one resolver for "which editor opens this content" (the edit-URL resolver). The opener applies its rules rather than adding a second decision point.
  - Each content type chooses its editor. Always opening the new editor was rejected in #37759 because it would silently switch editors for types that opted out; that decision holds here.
  - The three open questions in the issue are decided in Scope: the UVE derives URL changes from the saved contentlet, a restore is a save, and workflow actions reuse the existing events. Each can be overturned in the review of this spec.
  - This is the first step toward hosting the new editor's fields outside the full editor (UVE quick edit), so the request and event contract are designed to be reused by that work.
  - The plan formally consults `dotCMS/platform-adrs`.

## Assumptions

- The legacy editor's events (`save-page`, `close`, `deleted-page`, `workflow-wizard`, `compare-contentlet`, `push-publish`, `edit-contentlet-data-updated`) are stable and sufficient. Embedding it in a panel needs no change to the legacy editor beyond the block editors' outbound event.
- The legacy editor sends no event on a language switch or a restored version: it reloads itself. The panel notices both from the reload, as Content Drive's legacy panel does today.
- A workflow action completed through the wizard results in the legacy editor's normal save, which sends its save notification, or in a close when the action removed the content. This was verified for #37759. Refreshing on `closed` as well as on `saved` is the safety net where a surface needs one.
- The saved contentlet fetched by inode after a legacy save carries the detail URL the UVE needs to detect a URL-mapped change, as the UVE's current fetch does.
- The new editor can report a restored version through the same path it reports a save, since the restore updates the content it holds. Today a restore does not go through that path, so this is work inside the editor, not wiring.
- The new editor can edit and create content in an experiment variant by carrying the variant name to its save, which the admin's save call already forwards to the server. This is verified before the UVE work starts; if the server does not accept it, FR-022 narrows to the legacy editor and this spec is amended.
- The side panel outlet already supports stacking, mask and Escape per panel, main-navigation collapse and browser Back routing through the content's guard. No change to the outlet is needed.
- The side panel is attached to the document body and is modal, so it stacks above a PrimeNG modal dialog. The QA note on the issue covers the focus-trap checks; if the dialog's focus trap wins, resolving it is implementation work inside this issue, not a scope change.
- The Publishing Queue dialogs stay modal dialogs. Only the editor they open moves to the panel.
- The legacy JSP portlets refresh through the admin's existing iframe reload call, as the custom-event handler does today after the legacy dialog closes.
- Deep links keep rendering the portlet they name; the panel opens over it, so a user without that portlet in their menu is redirected as today.
- The unsaved-changes prompt reuses the existing wording for both editors. No new user-facing text is needed beyond panel titles.
- The backend flag `FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL` stays defined and served until its follow-up removal; the frontend simply ignores it.
