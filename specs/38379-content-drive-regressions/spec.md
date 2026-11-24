# Issue Resolution Specification: Content Drive regressions and small fixes

**Feature Branch**: `issue-38379-content-drive-regressions`

**Created**: 2026-11-24

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#38379](https://github.com/dotCMS/core/issues/38379)

**Input**: User description: "Content Drive regressions: UVE state, Host/Folder prefill, labels and lock icon, empty-state message, Edit on multi-select. Seven items, all in the issue body."

## Problem Statement *(mandatory)*

Content Drive loses the place an editor was working in when they come back from the page editor, and has five smaller defects found alongside it. They are fixed together because each is small and all of them live in the Content Drive portlet or the shared listing pieces it uses. A seventh report, Host or Folder not prefilled on create (item 2), does not reproduce on trunk and is out of scope; it stays listed so the numbering matches the issue.

1. **Coming back from the page editor loses the browsing state.** An editor browses into a folder, filters the list, opens a page in the page editor (UVE) and returns to Content Drive through the breadcrumb. Content Drive opens at the site root with no filters: the folder, the filters and the folder tree state are gone.
2. **Creating content inside a folder does not prefill Host or Folder (not reproduced, out of scope).** Reported as: an editor browses into a folder, creates content there, and the Host or Folder field is not set to that folder. On trunk built 2026-11-23 it prefills correctly from the New menu in every case tried: a Blog in the new editor with the folder reached from the tree and by double-click, a Call to Action in the legacy editor from a shared URL, and a Page by double-click. The code that passes the folder has not changed since early October. It is left out of this fix; if it is reproduced again, it gets its own issue with the exact steps.
3. **The dotAsset base type is called "DotAsset" in two places.** The upload dialog and Folder Settings call this base type "Asset", but Content Drive's New menu and its content type filter (the list and the chip) say "DotAsset".
4. **The lock icon goes stale after locking from the edit panel.** Locking or unlocking content from the edit side panel does not update the lock icon on that row of the listing until the page is reloaded. Doing the same from the row's right-click menu updates it at once.
5. **Filter row labels are medium weight.** In the content type filter, every row label renders in medium weight; it should be regular. The weight comes from the shared filter row component, which every filter popover uses.
6. **An empty listing still draws the table, wide enough to scroll.** With a content type selected, the listing adds one column per field marked "show in list". With enough of them the table is wider than the screen, and when a search or filter returns nothing the empty header row is still drawn: the listing scrolls sideways and the empty-state message scrolls out of view with it.
7. **Edit Content is offered for one item while several are selected.** With more than one item selected, the editor can still choose Edit Content (or View Content, or Edit Page). It opens a single item, which reads as if every selected item would open, and the selection is gone once the side panel closes.

8. **The content type picker's empty state sits off centre.** Point 3 of #38089, moved here because it is the same kind of fix as item 6. When the picker opened from the New menu has nothing to list (no types of that kind, or a search with no match), its empty state sits to the right of the dialog's centre.

**Severity / Impact**: Medium. Items 1, 4 and 7 affect every editor working in Content Drive, daily: they cost clicks, lose context or give a wrong impression of what an action does. Items 3, 5 and 6 are visual inconsistencies. No data is lost or saved wrongly by any of them.

## Reproduction *(mandatory)*

**Environment**: main as of 2026-11-24 (trunk), any browser, the demo starter site. Item 4 is reproduced in the new content editor, which is the editor Content Drive ships with.

**Steps to Reproduce**:

*Item 1, state lost returning from UVE*

1. Open Content Drive, browse into a folder and apply a filter.
2. Open a page in the page editor (Edit Page).
3. Click the Content Drive crumb in the breadcrumb.

*Item 3, "DotAsset" label*

1. Open Content Drive and open the New menu.
2. Open the content type filter and select the dotAsset base type.

*Item 4, stale lock icon*

1. Open Content Drive and open a contentlet in the edit side panel.
2. Lock it (or unlock it) from the panel.
3. Repeat from the row's right-click menu.

*Item 5, filter row weight*

1. Open Content Drive and open the content type filter.

*Item 6, empty listing*

1. Open Content Drive and filter by a content type that has many fields marked "show in list".
2. Search for a term that returns nothing.

*Item 7, Edit with several items selected*

1. Open Content Drive and select two or more contentlets.
2. Right-click one of the selected rows (and check the toolbar).
3. Choose Edit Content, then close the side panel.

*Item 8, picker empty state*

1. Open Content Drive, open the New menu and pick a base type.
2. Search the picker for a term that matches nothing, and compare the empty state's centre with the dialog's.

**Expected Behavior**:

- Item 1: Content Drive comes back in the folder, with the filters and the tree state the editor left.
- Item 3: Both read "Asset".
- Item 4: The row's lock icon updates as soon as the lock changes, from the panel or the menu.
- Item 5: Row labels are regular weight.
- Item 6: No table is drawn: only the empty-state message, centred in the listing's area, with no horizontal scroll.
- Item 7: Edit Content, View Content and Edit Page are not offered while more than one item is selected.
- Item 8: The empty state is centred in the dialog body.

**Actual Behavior**:

- Item 1: Content Drive opens at the root with no filters.
- Item 3: Both read "DotAsset".
- Item 4: From the panel the icon keeps the old state until a reload; from the menu it updates at once.
- Item 5: Row labels are medium weight.
- Item 6: The empty header row with every column is drawn, the listing scrolls sideways and the message moves out of view.
- Item 7: Edit Content is offered and opens one item; closing the panel clears the selection.
- Item 8: The empty state sits about 14px right of the dialog's centre.

**Reproducibility**: Always, for items 1 and 3 to 8. Item 2 does not reproduce on trunk.

## Scope of Investigation *(mandatory)*

- **Affected area**: the admin UI only. The Content Drive portlet (`core-web/libs/portlets/dot-content-drive`), the GlobalStore breadcrumb trail (`core-web/libs/global-store`), the content editor's lock feature (`core-web/libs/edit-content`), the legacy editor side panel, and shared UI components in `core-web/libs/ui`: the filter row (`dot-filter-list-item`), the folder listing (`dot-folder-list-view`) and the content type filter (`dot-content-type-filter`).
- **Suspected surface**: modern Angular code throughout. No Java change is expected; the server's base type label (`ContentTypeHelper`, key `DotAsset`) is read but must not change.
- **Related known decisions**: the breadcrumb trail rules in [Breadcrumbs](../../docs/frontend/BREADCRUMBS.md). The plan consults `dotCMS/platform-adrs` as usual.

## Root-Cause Hypothesis

1. **Breadcrumb**: the GlobalStore handler that builds the Content Drive crumb (`contentDrive` in `breadcrumb.utils.ts`) leaves the crumb alone when the trail already ends on Content Drive, so the crumb keeps the URL from the editor's first arrival (often the bare `/content-drive` from the menu). Later browsing changes `path`, `filters` and `isTreeExpanded` in the URL but not in the crumb. Browser Back is a separate path and must be checked too.
2. **Prefill**: not reproduced. Content Drive passes the browsed folder to both editors (`folderPath` to the new one, `folder=<inode>` to the legacy one), and both showed it in the field in every case tried.
3. **Label**: the New menu reads Content Drive's own key `content-drive.base-type.dotasset=DotAsset`; the filter shows the server's base type name. The filter is shared with the AssetPicker and the server key with the Content Types portlet, so the rename must be scoped to Content Drive.
4. **Lock icon**: Content Drive reloads the listing only when the panel reports a save. Lock and unlock in the new editor are not saves and report nothing, and closing the new editor's panel does not reload either.
5. **Weight**: `dot-filter-list-item` renders its label with `font-medium`.
6. **Empty listing**: the empty state is rendered as a row inside the table (the table's empty message template), so the header with every extra column is still laid out around it. The grid view already shows the same empty state on its own, outside any table.
7. **Edit on multi-select**: the toolbar's Edit buttons already show only for a single item. The right-click menu builds its entries for the clicked row without looking at the selection, so with several items selected it still offers Edit Content, View Content and Edit Page for that one row, and for a page Edit or View Properties and, for a file, Edit Code, all of which open one item in a side panel.

8. **Picker empty state**: the shared UVE palette list pads its list area on the left only; the cards get their right padding from their own grid, and the empty state got none.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Returning to Content Drive from UVE, by breadcrumb and by browser Back, restores the folder, filters and tree state.
- Content Drive shows "Asset" for the dotAsset base type in the New menu and in the content type filter (list and chip).
- Locking or unlocking from the new editor's side panel updates the row's lock icon without a reload.
- The shared filter row renders labels in regular weight.
- In Content Drive, an empty listing renders no table, only the empty-state message, centred and without horizontal scroll; the table returns with the same columns, sort and page size when results come back. The shared listing does this only when its host asks (`hideEmptyTable`), and reuses the grid view's empty-state frame.
- With more than one item selected, the right-click menu and the toolbar offer no entry that opens one item in a side panel: Edit or View Content, Edit or View Page, Edit or View Properties, Edit Code.

- The picker's empty state gets the same right padding as the cards, in the shared palette list, so it is centred in Content Drive's picker and in the page editor's palette alike.

**Explicitly out of scope / non-goals**:

- The legacy (JSP) editor's lock buttons: Content Drive ships with the new editor, and the legacy panel already refreshes the list when it closes.
- Item 2, Host or Folder prefill on create: it does not reproduce on trunk, so nothing is changed for it here.
- Renaming "DotAsset" outside Content Drive: the Content Types portlet, the Tools portlet, the AssetPicker and the server's `DotAsset` key keep their current label.
- Opening several items at once (stacked side panels) or deciding an order for them.
- Keeping the selection after the side panel closes. Once Edit is not offered for a multi-selection, there is no multi-selection to lose that way; how selection behaves around the side panel in general is not changed here.
- Double-click on a row: it always opens the row that was double-clicked, which is unambiguous, so it is unchanged.
- Other right-click entries for a multi-selection (workflow actions, delete and the like) and the Action Center.
- Remembering Content Drive state across sessions or portlets (tracked separately in #38092).
- Changing which fields show as extra columns, or their widths when there are results.
- The weight of the "All content types selected." banner in the content type filter.
- Medium weight anywhere else, in Content Drive or outside it, other than through the shared filter row.

## Regression Risk *(mandatory)*

- **Blast radius**:
  - The filter row is shared by every filter popover: in Content Drive the language, status, workflow and field filters, and elsewhere the Users, Experiments and Publishing Queue filters. All of them change to regular weight, on purpose.
  - The folder listing is shared by Content Drive, the Action Center preview, the AssetPicker and the relationship field's Add Relationships dialog. Rendering only the empty state is opt-in, so the other three keep their table on an empty result; Add Relationships relies on its header and paging footer to get the editor out of an empty search.
  - The content type filter is shared with the AssetPicker, which must keep "DotAsset".
  - The breadcrumb handler is shared by every portlet's trail; only the Content Drive crumb's behaviour may change.
  - The edit panel's lock and unlock are used outside Content Drive (page editor, other surfaces opening the side panel); any new notification from them must be harmless where nobody listens.
- **Backward compatibility**: no REST, index, database or serialized-state change. Content Drive URLs keep their current parameters, and links shared before the fix keep working.
- **Data considerations**: none. No content is created or changed by any of these fixes.

## Acceptance & Verification *(mandatory)*

- **AC-001**: Returning to Content Drive from UVE through the breadcrumb restores the folder, filters and tree state the editor left.
- **AC-002**: Browser Back from UVE does the same.
- **AC-003 to AC-006**: withdrawn with item 2, which does not reproduce on trunk.
- **AC-007**: Content Drive's New menu shows "Asset" for the dotAsset base type.
- **AC-008**: Content Drive's content type filter shows "Asset" for that base type, in the list and in the chip.
- **AC-009**: The Content Types portlet and the AssetPicker still show "DotAsset".
- **AC-010**: Locking or unlocking from the edit side panel updates that row's lock icon without a reload, in the new editor.
- **AC-011**: withdrawn: Content Drive ships with the new editor, and the legacy panel refreshes the list when it closes.
- **AC-012**: Row labels in the content type filter render in regular weight, and so do the labels of every other filter popover that uses the shared filter row, with no other layout change.
- **AC-013**: When the listing has no results, no table (header or columns) is rendered, only the empty-state message, centred, with no horizontal scroll.
- **AC-014**: When results come back, the table returns with the same columns, sort and page size.
- **AC-015**: The other consumers of the shared listing (Action Center preview, AssetPicker, Add Relationships dialog) keep their table, with its empty state inside it, on an empty result.
- **AC-016**: With more than one item selected, neither the toolbar nor the right-click menu offers an entry that opens one item in a side panel (Edit or View Content, Edit or View Page, Edit or View Properties, Edit Code).
- **AC-017**: With exactly one item selected, or a right-click on a row when nothing else is selected, Edit Content and Edit Page work as before.
- **AC-018**: The content type picker's empty state is centred in the dialog body, with the same inset on both sides.
- **Verification method**:
  - Vitest and Spectator unit tests, written first and confirmed failing, in the affected projects: the GlobalStore breadcrumb handler (AC-001); the Content Drive toolbar and the content type filter (AC-007 to AC-009); the edit-content lock feature, its hosts and side panel, and the Content Drive shell's reaction to a lock change (AC-010); the filter row (AC-012); the folder listing (AC-013 to AC-015); the context menu and the toolbar actions (AC-016, AC-017).
  - Tests assert presence and behaviour, not CSS classes; AC-012, the centring in AC-013 and AC-018 are confirmed manually in the browser.
  - Manual pass in a running instance over the six reproductions above, in both editors where they apply. Browser Back (AC-002) is checked manually if no unit test can drive it.

## Assumptions

- The report's "only the first one opens" (item 7) describes the right-click menu opening the clicked row; no other entry point offers Edit for a multi-selection. The plan confirms this before the tests are written.
- "Asset" uses Content Drive's existing label for that base type (the one the upload dialog shows); no new wording is introduced.
- The extra columns in item 6 are the existing "show in list" columns; how they are chosen is unchanged.
- Browser Back from UVE (AC-002) shares the cause of the breadcrumb case or a close one; if it turns out to be a different defect with a larger fix, it is split into its own issue rather than growing this one.
