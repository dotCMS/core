# UVE Refactor - Team Handoff

**Branch:** `uve-experiment`
**Base:** `main`
**Status:** 🟡 Ready for Testing & Completion
**Changes:** 44 files, +3,378 / -1,623 lines

**Recent Updates:**
- ✅ Resolved inline editing vs contentlet selection conflict with dual overlay system
- ✅ Implemented hover/selected state management in contentlet tools
- ✅ Relocated toolbar buttons (palette toggle, copy URL, right sidebar toggle)
- ✅ Added right sidebar with toggle and empty state
- ✅ Fixed responsive preview broken by zoom implementation

---

## What Was Done

### Architecture Refactor
Extracted business logic from the monolithic `EditEmaEditor` component into specialized services, reducing component complexity from ~2000 to ~1000 lines.

**Main Component:**
- [`edit-ema-editor.component.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/edit-ema-editor.component.ts) - Refactored to use new service architecture

### New Services (No Tests Yet - May become 3 after consolidation)

| Service | Purpose | Location |
|---------|---------|----------|
| **DotUveActionsHandlerService** | Centralized action handling (edit, delete, reorder) | [`dot-uve-actions-handler.service.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/services/dot-uve-actions-handler/dot-uve-actions-handler.service.ts) |
| **DotUveBridgeService** | PostMessage communication with iframe | [`dot-uve-bridge.service.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/services/dot-uve-bridge/dot-uve-bridge.service.ts) |
| **DotUveDragDropService** | Drag-and-drop logic | [`dot-uve-drag-drop.service.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/services/dot-uve-drag-drop/dot-uve-drag-drop.service.ts) |
| **DotUveZoomService** | Zoom controls (25%-150%) - **Should be moved to UVEStore** | [`dot-uve-zoom.service.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/services/dot-uve-zoom/dot-uve-zoom.service.ts) |

### New Components (No Tests Yet)

| Component | Purpose | Location |
|-----------|---------|----------|
| **DotUveIframeComponent** | Iframe with zoom support | [`dot-uve-iframe/`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-iframe/) |
| **DotUveZoomControlsComponent** | Zoom UI controls | [`dot-uve-zoom-controls/`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-zoom-controls/) |
| **DotRowReorderComponent** | Row/column drag-and-drop reordering | [`dot-row-reorder/`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-palette/components/dot-row-reorder/) |

### State Management
- [`withSave.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/store/features/editor/save/withSave.ts) - Enhanced to re-fetch page content after save
- [`dot-uve.store.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/store/dot-uve.store.ts) - Improved integration with new services

### UI/UX Improvements

#### Contentlet Tools - Dual State Management
**Component:** [`dot-uve-contentlet-tools.component.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-contentlet-tools/dot-uve-contentlet-tools.component.ts)

**Implemented:**
- **Hover state**: Blue overlay with opacity, clickable, allows page interaction
- **Selected state**: Border outline, transparent background, tools visible, `pointer-events: none` on bounds to allow iframe interaction
- **Dual overlays**: Both hover and selected can be visible simultaneously
- **Click-to-select**: Clicking hover overlay selects contentlet and shows action buttons
- **Computed signals**: `isHoveredDifferentFromSelected`, `showHoverOverlay`, `showSelectedOverlay`, `selectedContentContext`, `isSelectedContainerEmpty`, `selectedHasVtlFiles`
- **Signal methods**: `handleClick()` sets selection, `signalMethod()` resets selection when contentlet changes

**User Benefit:** Hovering shows subtle preview without blocking interaction; clicking selects and shows tools while still allowing page interaction.

#### Editor Component - Toolbar Button Relocations
**Component:** [`edit-ema-editor.component.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/edit-ema-editor.component.ts)

**Implemented:**
- **Right sidebar toggle**: Added `$rightSidebarOpen` signal with width-based animation
- **Button relocations**:
  - Palette toggle moved from `dot-uve-toolbar` to `browser-toolbar` (start)
  - Copy URL button moved from `dot-uve-toolbar` to `browser-url-bar-container` (left of URL bar)
  - Right sidebar toggle added to `browser-toolbar` (end) with flipped icon
- **Empty state**: "Select a contentlet" message when sidebar is empty
- **Image drag prevention**: `draggable="false"` on toggle button images
- **Grid layout**: Updated to `grid-template-columns: min-content 1fr min-content` for three-column layout

**User Benefit:** More intuitive placement of controls near content area, consistent animations, clear empty states.

#### Toolbar Component - Cleanup
**Component:** [`dot-uve-toolbar.component.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-toolbar/dot-uve-toolbar.component.ts)

**Removed:**
- Palette toggle button and functionality
- Copy URL button and overlay panel
- Related imports (`ClipboardModule`, `OverlayPanelModule`)
- `$pageURLS` computed signal and `triggerCopyToast()` method

**User Benefit:** Cleaner toolbar component, functionality moved to more appropriate locations.

---

## Design Decisions Needed

### 1. Contentlet Controls at Zoom

### Problem
The contentlet-level controls in [`dot-uve-contentlet-tools`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-contentlet-tools/dot-uve-contentlet-tools.component.ts) become too small to use when zoomed out.

### Proposed Solution
**Remove the overlay buttons and replace with:**

1. **Move/Drag** - Make entire contentlet draggable (no button needed)
2. **Code Button** - Move to right panel alongside edit content
3. **Delete** - Keyboard action (Delete/Backspace) and/or button in right panel
4. **Edit** - Button in right panel with two modes:
   - **Quick Edit** (default) - Edit simple fields inline in right panel:
     - Text, Textarea, Select, Multi-select, Radio, Checkbox
   - **Full Editor** - Button to open complete editor for other field types

**Impact:** Requires refactoring [`dot-uve-contentlet-tools.component.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-contentlet-tools/dot-uve-contentlet-tools.component.ts) and enhancing right panel UI.

### 2. Layout Tree Completeness

**Current State:** Layout tab shows: Rows > Columns

**Missing Levels:** Containers > Contentlets (actual content pieces)

**Proposed Enhancement (Optional):**
- **Containers:** Add to tree with double-click to edit:
  - Add/remove allowed content types
  - Set max contentlets per container
  - Quick container configuration without leaving editor

- **Contentlets:** Show actual content pieces in tree for better visibility

**Considerations:**
- **Permissions:** Not all users have permission to create/edit containers - need to understand and respect permission layer
- **Scope Creep:** Could be deferred to post-MVP
- **Complexity:** Additional tree levels add UI complexity

**Decision Required:**
- [ ] Ship as-is with Rows/Columns only (simpler, faster to market)
- [ ] Add Containers level with editing capability
- [ ] Add full tree: Rows > Columns > Containers > Contentlets
- [ ] Defer to future iteration based on user feedback

**Impact:** [`dot-row-reorder.component.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-palette/components/dot-row-reorder/dot-row-reorder.component.ts) would need significant expansion to support deeper tree levels and permission checks.

### 3. Inline Editing vs Contentlet Selection Conflict ✅ RESOLVED

**Component:** [`dot-uve-contentlet-tools.component.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-contentlet-tools/dot-uve-contentlet-tools.component.ts)

**Solution Implemented:**
- **Dual overlay system**: Separate hover (blue, clickable) and selected (border, tools visible) overlays
- **Selected overlay**: Uses `pointer-events: none` on bounds, allowing iframe interaction while tools remain visible
- **Click-to-select**: Clicking hover overlay selects contentlet and shows action buttons
- **Visual feedback**: Clear distinction between hover (blue highlight) and selected (border outline) states
- **Non-blocking interaction**: Selected state allows page interaction while maintaining tool visibility

**Result:** Users can hover to preview, click to select and access tools, and continue interacting with the page without overlay interference. Both states can be visible simultaneously for better context.

---

## Known Issues to Address

#### 1. Viewport Units with Zoom
**Issue:** The zoom feature sets the iframe width to the full extent of the loaded page, which breaks viewport height CSS units (`vh`, `vw`, `vmin`, `vmax`) used by customer developers.

**Solution:** Customers need to add `max-height` to elements using viewport units.

**Action Needed:**
- [ ] Document this requirement in user-facing documentation
- [ ] Consider adding a warning/notice in the editor UI when zoom is active
- [ ] Update developer best practices guide

#### 2. Layout Tab Styling & Column Offset Handling
**Component:** [`dot-row-reorder.component.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-palette/components/dot-row-reorder/dot-row-reorder.component.ts)

**Issues:**
- **Needs styling** - Layout tab UI requires polish and refinement
- **Column offset wiped on reorder** - When moving columns, their offset values are lost. Current logic only calculates based on column width, not preserving offset configuration

**Action Needed:**
- [ ] Design and implement proper styling for layout tab
- [ ] Fix column offset preservation logic when reordering
- [ ] Test edge cases (columns with various offset values)
- [ ] Document expected behavior for offset handling during reorder

#### 2b. Palette UI Visual Grouping
**Components:** [`dot-uve-palette.component.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-palette/dot-uve-palette.component.ts) and related palette components

**Issue:** Card and list items in the palette are too visually separated, creating poor logical grouping.

**Action Needed:**
- [ ] Review spacing/margins between palette items
- [ ] Improve visual grouping to show relationships
- [ ] Consider: Group cards, list sections more clearly
- [ ] Ensure consistent spacing throughout palette tabs
- [ ] Test with actual content to verify readability

#### 3. Headless Pages Not Supported
**Issue:** The new content editing form and row/column reordering features don't work with headless pages.

**Action Needed:**
- [ ] Decide: Should headless pages support these features?
- [ ] If yes: Implement support for headless page architecture
- [ ] If no: Disable/hide these features when editing headless pages
- [ ] Add clear messaging to users when features are unavailable for headless pages

#### 4. Row/Column Names Using Wrong Property
**Issue:** Currently using `styleClass` property to store row/column names, but this property is intended for CSS classes, not labels.

**Solution:** Backend schema changes needed to add proper metadata fields.

**Action Needed:**
- [ ] Backend: Add `name` field to row/column data model
- [ ] Backend: Add `description` field to row/column data model
- [ ] Backend: Update API endpoints to support new fields
- [ ] Frontend: Update [`dot-row-reorder.component.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-palette/components/dot-row-reorder/dot-row-reorder.component.ts) to use new fields
- [ ] Frontend: Keep `styleClass` for CSS classes only
- [ ] Migration: Consider data migration for existing styleClass "names"

#### 5. "Rules" Feature Not Loading
**Issue:** "Rules" feature is not loading in the editor. Unknown if this is a regression from refactor or an existing issue with Angular dev server (port 4200).

**Action Needed:**
- [ ] Investigate: Does "rules" work in production build?
- [ ] Investigate: Did refactor break rules functionality?
- [ ] Investigate: Is this a dev server proxy/routing issue?
- [ ] Test rules in full dotCMS environment (port 8080)
- [ ] Fix or document known limitation if dev-server only issue

#### 6. Missing "Edit All Pages vs This Page" Dialog
**Issue:** The new palette form to edit content doesn't ask users whether to edit content globally or just for this page.

**Current Flow (Exists):** Dialog with two options:
- **"All Pages"** - Edit the original/global content (changes appear on all pages using this content)
- **"This Page"** - Copy the content, add copy to this page, then edit (changes only affect current page)

**New Flow (Missing):** Goes directly to edit without asking, potentially editing global content unintentionally.

**Impact:** Users could accidentally modify content globally when they only intended to change it on one page.

**Action Needed:**
- [ ] Add "All Pages vs This Page" dialog before opening edit form
- [ ] Implement copy logic when "This Page" is selected
- [ ] Handle workflow: Check if content is shared across pages → Show dialog
- [ ] Handle workflow: If content only on current page → Skip dialog, go straight to edit
- [ ] Reuse existing dialog UI/logic from current edit flow

#### 7. Responsive Preview Broken by Zoom Implementation ✅ RESOLVED
**Issue:** Responsive preview (device viewport switching) stopped working after adding zoom-in-out functionality to the iframe.

**Component:** [`dot-uve-iframe.component.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-iframe/dot-uve-iframe.component.ts) and [`dot-uve-zoom.service.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/services/dot-uve-zoom/dot-uve-zoom.service.ts)

**Solution Implemented:**
- Fixed zoom transform/scaling conflicts with responsive preview viewport resizing
- Zoom and responsive preview now work together correctly
- Iframe sizing calculations updated to account for both features

**Result:** Responsive preview (device viewport switching) now works correctly with zoom functionality enabled.

---

## What's Missing

### 1. Backend Changes Required
- [ ] **Row/Column name fields** (see "Known Issues to Address" section above)
  - Add `name` and `description` fields to row/column data model
  - Update API endpoints
  - Plan data migration if needed

### 2. Testing

**⚠️ Critical:** This is a major refactor - comprehensive smoke and regression testing required before release.

#### Unit Tests
- [ ] Unit tests for 3 new services (after zoom consolidation) or 4 if keeping zoom service
- [ ] Unit tests for 3 new components
- [ ] Unit tests for zoom logic in UVEStore (after consolidation)
- [ ] Update existing tests for refactored `EditEmaEditor`

#### Smoke Testing (New Features)
- [ ] Zoom controls work at all levels (25%, 50%, 75%, 100%, 125%, 150%)
- [ ] Row reordering via drag-and-drop
- [ ] Column reordering within rows
- [ ] Row/column style class editing
- [ ] Content editing form (quick edit mode)
- [ ] All new services communicate correctly

#### Regression Testing (Existing Features - Don't Break These!)
- [ ] Page loading and rendering
- [ ] Content drag-and-drop from palette
- [ ] Inline editing (text, WYSIWYG)
- [ ] Add/remove contentlets
- [ ] Container operations
- [ ] Delete contentlets
- [ ] Undo/redo (if implemented)
- [ ] Page saving
- [ ] Publish/unpublish workflows
- [x] **Responsive preview/device switching** ✅ **FIXED - See Known Issues #7**
- [ ] Page preview at different viewports
- [ ] Multi-language support
- [ ] Permissions enforcement
- [ ] SEO tools integration
- [ ] Block editor integration
- [ ] Form editing
- [ ] Template changes
- [ ] Device/persona switching

### 3. Documentation
- [ ] JSDoc comments for service methods
- [ ] Component input/output documentation
- [ ] Architecture diagram showing service relationships

### 4. Accessibility
- [ ] Keyboard navigation for drag-and-drop
- [ ] ARIA labels for interactive elements
- [ ] Screen reader testing

### 5. UX Improvements
- [ ] **Decide on layout tree completeness** (see "Design Decisions Needed" section above)
  - Ship as-is or add Container/Contentlet levels?
  - Research permission layer requirements if proceeding
  - Estimate effort for container editing dialog
- [ ] **Implement "All Pages vs This Page" dialog** (see "Known Issues to Address" section above)
  - Add dialog before opening edit form
  - Implement content copy logic for "This Page" option
  - Check if content is shared across pages
  - Reuse existing dialog from current flow
- [x] **Resolve inline editing vs selection conflict** ✅ **COMPLETED**
  - Implemented dual overlay system (hover + selected states)
  - Selected overlay uses `pointer-events: none` to allow iframe interaction
  - Clear visual distinction between hover and selected states
  - Both states can be visible simultaneously
- [ ] **Implement contentlet controls redesign** (see "Design Decisions Needed" section above)
  - Make entire contentlet draggable
  - Move code/delete/edit actions to right panel
  - Implement quick edit mode for simple fields
  - Add keyboard shortcuts for delete action
- [ ] **Fix layout tab issues** (see "Known Issues to Address" section above)
  - Style the row/column reorder UI
  - Preserve column offset values when reordering
  - Test and document offset behavior
- [ ] **Improve palette visual grouping** (see "Known Issues to Address" section above)
  - Fix spacing/margins between items
  - Improve visual relationships and grouping
  - Ensure consistency across all palette tabs
- [x] **Fix responsive preview with zoom** ✅ **COMPLETED**
  - Fixed zoom/responsive preview conflict
  - Zoom and responsive preview now work together
  - Iframe sizing calculations updated to account for both features
- [ ] **Handle headless pages** (see "Known Issues to Address" section above)
  - Decide on headless page support strategy
  - Implement or disable features accordingly
  - Add user messaging for unsupported scenarios

### 6. Polish
- [ ] Error handling in services
- [ ] Loading states during operations
- [ ] Browser compatibility testing (Chrome, Firefox, Safari, Edge)

### 7. Internationalization
- [ ] **No translation in new UI** - All new components/services need i18n support
- [ ] Add message keys to `webapp/WEB-INF/messages/Language.properties`
- [ ] Test with different locales

### 8. Code Quality & Refactoring

**⚠️ Important:** This code "works" but needs proper cleanup before release. Expect to find quick fixes and shortcuts that need refactoring.

- [ ] **Consolidate zoom service into UVEStore**
  - Move [`dot-uve-zoom.service.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/services/dot-uve-zoom/dot-uve-zoom.service.ts) logic into [`dot-uve.store.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/store/dot-uve.store.ts)
  - Remove unnecessary service abstraction
  - Update components to use store directly
  - Clean up service references

- [ ] **Move `updateRows` method to proper location**
  - Currently in [`withSave.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/store/features/editor/save/withSave.ts) but doesn't belong there
  - **Should move to:** [`withLayout.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/store/features/layout/withLayout.ts) - already exists!
  - Proper separation: layout operations belong in layout feature, not save feature
  - Update all references to use the layout feature

- [ ] **Comprehensive code review and cleanup**
  - Review all new services for proper error handling
  - Check for memory leaks (subscriptions, event listeners)
  - Look for "TODO" or "FIXME" comments
  - Identify hardcoded values that should be constants
  - Review conditional logic for edge cases
  - Ensure proper use of RxJS operators (no nested subscribes, proper cleanup)
  - Check for race conditions in async operations

- [ ] **Specific files to scrutinize:**
  - [`edit-ema-editor.component.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/edit-ema-editor.component.ts) - Large refactor, likely has shortcuts
  - [`dot-uve-actions-handler.service.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/services/dot-uve-actions-handler/dot-uve-actions-handler.service.ts) - Complex logic, review flow
  - [`dot-row-reorder.component.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-palette/components/dot-row-reorder/dot-row-reorder.component.ts) - New component, review patterns
  - [`withSave.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/store/features/editor/save/withSave.ts) - Added `updateRows` method - **May not belong here, needs review**

- [ ] Fix any ESLint warnings
- [ ] Remove `any` types where possible
- [ ] Clean up commented code
- [ ] Remove console.log/debug statements

---

## How to Review

### Compare Changes
```bash
# View all changes
git diff origin/main...uve-experiment

# View specific file
git diff origin/main...uve-experiment -- core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/edit-ema-editor.component.ts

# View stats
git diff origin/main...uve-experiment --stat

# See commits
git log origin/main..uve-experiment --oneline
```

### Test Manually
1. **Row/Column Reordering** - Drag rows, expand rows, reorder columns, edit style classes
2. **Zoom** - Zoom in/out, test scroll position, verify drag-and-drop at different zooms
3. **Contentlet Editing** - Open form, validate, save, cancel
4. **Contentlet Hover/Selection** - Hover to see blue overlay, click to select (border + tools), verify page interaction still works when selected
5. **Toolbar Buttons** - Test palette toggle, right sidebar toggle, copy URL button in new locations
6. **Right Sidebar** - Toggle open/closed, verify empty state message, verify contentlet selection updates sidebar
7. **General** - Ensure all existing features work, no console errors

---

## Key Files to Understand

| Type | Key Files |
|------|-----------|
| **Services** | [`services/`](core-web/libs/portlets/edit-ema/portlet/src/lib/services/) |
| **Main Component** | [`edit-ema-editor.component.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/edit-ema-editor.component.ts) |
| **New Components** | [`components/dot-uve-iframe/`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-iframe/), [`components/dot-uve-zoom-controls/`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-zoom-controls/), [`components/dot-row-reorder/`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-palette/components/dot-row-reorder/) |
| **Store** | [`store/dot-uve.store.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/store/dot-uve.store.ts), [`store/features/editor/save/withSave.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/store/features/editor/save/withSave.ts) |

---

## Architecture

```
EditEmaEditor (Main Component)
    ├── UVEStore (State + Zoom logic to be consolidated)
    └── Services
        ├── DotUveActionsHandlerService (Actions)
        ├── DotUveBridgeService (PostMessage)
        ├── DotUveDragDropService (Drag & Drop)
        └── DotUveZoomService (Zoom - TO BE REMOVED, move to store)
```

---

## Recent Commits

```
de10f910e1 clean up state management
def181c3c1 Update styles in EditEmaEditor and TemplateBuilder
0ebaca4867 Refactor DotEmaShellComponent and update routing
22fdb6705c Enhance EditEmaEditor: add cancel functionality
a2787aa462 Enhance DotRowReorderComponent: add column drag/sort animations
```

---

## Definition of Done

- [ ] All tests written and passing
- [ ] Accessibility audit complete
- [ ] Code review approved
- [ ] QA sign-off
- [ ] Documentation complete

---

## Production Readiness Plan

### 🔴 CRITICAL (Block Production Release)

These issues **must** be fixed before production:

#### 1. Missing "Edit All Pages vs This Page" Dialog ⚠️ **HIGH RISK**
**Location:** [`dot-uve-actions-handler.service.ts:235-236`](core-web/libs/portlets/edit-ema/portlet/src/lib/services/dot-uve-actions-handler/dot-uve-actions-handler.service.ts)

**Current Code:**
```typescript
[DotCMSUVEAction.EDIT_CONTENTLET]: (contentlet: DotCMSContentlet) => {
    dialog.editContentlet({ ...contentlet, clientAction: action });
}
```

**Problem:** Users can accidentally edit global content when they only intended to change it on one page.

**Solution:** Reuse existing logic from [`edit-ema-editor.component.ts:1028-1056`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/edit-ema-editor.component.ts) `handleEditContentlet` method:
- Check `contentlet.onNumberOfPages`
- If > 1, show `DotCopyContentModalService` dialog
- If user selects "This Page", copy content before editing
- If content only on current page, skip dialog and go straight to edit

**Impact:** Users could accidentally modify content globally affecting all pages.

---

#### 2. "Rules" Feature Not Loading
**Issue:** Unknown if regression from refactor or dev server issue.

**Action Items:**
- [ ] Test in production build (not just dev server on port 4200)
- [ ] Test in full dotCMS environment (port 8080)
- [ ] Check if refactor broke rules functionality
- [ ] Verify proxy/routing configuration
- [ ] Fix or document known limitation

**Impact:** Feature may be completely broken in production.

---

#### 3. Column Offset Preservation Bug
**Location:** [`dot-row-reorder.component.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-palette/components/dot-row-reorder/dot-row-reorder.component.ts)

**Issue:** When reordering columns, offset values are lost. Logic only calculates based on width, not preserving offset configuration.

**Action Items:**
- [ ] Review column reorder logic
- [ ] Preserve offset values during reorder
- [ ] Test edge cases (columns with various offset values)
- [ ] Document expected behavior

**Impact:** User configuration lost during reordering operations.

---

#### 4. Comprehensive Regression Testing
**Issue:** Major refactor requires full testing before release.

**Critical Test Areas:**
- [ ] Page loading and rendering
- [ ] Content drag-and-drop from palette
- [ ] Inline editing (text, WYSIWYG)
- [ ] Add/remove contentlets
- [ ] Container operations
- [ ] Delete contentlets
- [ ] Page saving
- [ ] Publish/unpublish workflows
- [ ] Multi-language support
- [ ] Permissions enforcement
- [ ] SEO tools integration
- [ ] Block editor integration
- [ ] Form editing
- [ ] Template changes
- [ ] Device/persona switching

**Impact:** Risk of breaking existing functionality.

---

### 🟠 HIGH PRIORITY (Should Fix Before Release)

#### 5. Code Architecture Cleanup

**5a. Consolidate Zoom Service into UVEStore**
- **Location:** [`dot-uve-zoom.service.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/services/dot-uve-zoom/dot-uve-zoom.service.ts)
- **Action:** Move logic into [`dot-uve.store.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/store/dot-uve.store.ts), remove service abstraction
- **Why:** Reduces unnecessary service layer, aligns with store pattern

**5b. Move `updateRows` Method**
- **Current:** [`withSave.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/store/features/editor/save/withSave.ts)
- **Should be:** [`withLayout.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/store/features/layout/withLayout.ts)
- **Why:** Proper separation of concerns - layout operations don't belong in save feature

---

#### 6. Unit Testing (Zero Coverage Currently)

**Services (3 after zoom consolidation):**
- [ ] `DotUveActionsHandlerService` - Complex action routing logic
- [ ] `DotUveBridgeService` - PostMessage communication
- [ ] `DotUveDragDropService` - Drag-and-drop logic

**Components:**
- [ ] `DotUveIframeComponent` - Iframe with zoom support
- [ ] `DotUveZoomControlsComponent` - Zoom UI controls
- [ ] `DotRowReorderComponent` - Row/column reordering

**Store:**
- [ ] Zoom logic in UVEStore (after consolidation)
- [ ] Update existing tests for refactored `EditEmaEditor`

**Impact:** No test coverage means high risk of regressions.

---

#### 7. Internationalization (i18n)
**Issue:** All new UI components have hardcoded English text.

**Action Items:**
- [ ] Add message keys to `webapp/WEB-INF/messages/Language.properties`
- [ ] Replace all hardcoded strings in new components
- [ ] Test with different locales
- [ ] Verify all user-facing text is translatable

**Affected Components:**
- `DotUveZoomControlsComponent`
- `DotRowReorderComponent`
- `DotUveIframeComponent`
- Right sidebar empty state
- All new service error messages

**Impact:** Product not usable in non-English locales.

---

### 🟡 MEDIUM PRIORITY (Polish & UX)

#### 8. Code Cleanup
**Found Issues:**
- **Console statements:** 23 `console.*` calls found (most are `console.error` for error handling - acceptable, but some `console.warn` should be removed)
- **TODO comments:** 3 TODOs found:
  - `dot-uve.store.ts:39` - "TODO: remove when the unit tests are fixed"
  - `inline-edit.service.ts:237` - Format handling TODO
  - `dot-ema-dialog.component.ts:408` - Emit function TODO
- **Linting:** No current linting errors ✅

**Action Items:**
- [ ] Remove `console.log`/`console.debug` statements (keep `console.error` for error handling)
- [ ] Remove `any` types where possible
- [ ] Clean up commented code
- [ ] Address or remove TODO comments

---

#### 9. Error Handling & Loading States
- [ ] Review all new services for proper error handling
- [ ] Add loading states during operations
- [ ] Ensure proper error messages to users
- [ ] Check for memory leaks (subscriptions, event listeners)

---

### 🟢 LOW PRIORITY (Nice to Have)

#### 10. Documentation
- [ ] JSDoc comments for service methods
- [ ] Component input/output documentation
- [ ] Architecture diagram showing service relationships

#### 11. Accessibility
- [ ] Keyboard navigation for drag-and-drop
- [ ] ARIA labels for interactive elements
- [ ] Screen reader testing

#### 12. Browser Compatibility
- [ ] Test in Chrome, Firefox, Safari, Edge
- [ ] Verify zoom functionality across browsers
- [ ] Test drag-and-drop across browsers

---

## Implementation Priority

### Phase 1: Critical Fixes (Week 1)
1. Fix "All Pages vs This Page" dialog
2. Investigate and fix "Rules" feature
3. Fix column offset preservation
4. Begin regression testing

### Phase 2: Architecture & Testing (Week 2)
1. Consolidate zoom service into store
2. Move `updateRows` to `withLayout.ts`
3. Write unit tests for services
4. Write unit tests for components

### Phase 3: Polish & i18n (Week 3)
1. Add i18n support
2. UI/UX improvements
3. Code cleanup
4. Error handling improvements

### Phase 4: Final QA (Week 4)
1. Complete regression testing
2. Accessibility audit
3. Browser compatibility testing
4. Documentation

---

## Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Missing dialog causes data loss | 🔴 High | Medium | Fix in Phase 1 |
| Rules feature broken in production | 🔴 High | Unknown | Test immediately |
| Column offset bug loses user config | 🔴 High | High | Fix in Phase 1 |
| Regression in existing features | 🔴 High | Medium | Comprehensive testing |
| No test coverage | 🟠 Medium | High | Add tests in Phase 2 |
| i18n missing | 🟠 Medium | High | Add in Phase 3 |
| Code quality issues | 🟡 Low | High | Cleanup in Phase 3 |

---

**Last Updated:** 2025-01-28
