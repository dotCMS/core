# UVE Refactor - Team Handoff

**Branch:** `uve-experiment`
**Base:** `main`
**Status:** 🟡 Ready for Testing & Completion
**Changes:** 44 files, +3,378 / -1,623 lines

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

### 3. Inline Editing vs Contentlet Selection Conflict

**Component:** [`dot-uve-contentlet-tools.component.ts`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-contentlet-tools/dot-uve-contentlet-tools.component.ts)

**Problem:**
- `dot-uve-contentlet-tools` previously had `pointer-events: none` to allow iframe to receive click events
- Now it needs to receive click events to select contentlet and open edit form sidebar
- But this blocks the iframe from receiving events, breaking inline editing (which happens in iframe code)

**Current Behavior:**
- 1 click to start inline editing

**Proposed Solution:**
- **Click 1:** Click `dot-uve-contentlet-tools` → Select contentlet → Disable pointer-events again
- **Click 2:** Click iframe content → Start inline editing
- Changes to 2-click interaction pattern

**Challenges:**
- **UX Change:** Users now need 2 clicks instead of 1 for inline editing
- **Visual Hints Required:** Need UI changes to indicate:
  - When contentlet is selected (after first click)
  - That second click is needed for inline editing
  - Clear selection state
- **Discoverability:** How do users learn the new pattern?

**Decision Required:**
- [ ] Implement 2-click pattern with visual selection state
- [ ] Find alternative: keyboard modifier (Cmd/Ctrl+click for selection vs inline)?
- [ ] Find alternative: hover state timeout before enabling pointer-events?
- [ ] Add onboarding tooltip/guide for new interaction pattern
- [ ] Design selection state visual (border, highlight, badge?)

**Impact:** Significant UX change that affects core editing workflow. Needs product/UX team approval.

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
- [ ] **Resolve inline editing vs selection conflict** (see "Design Decisions Needed" section above)
  - Decide on 2-click pattern vs alternative solutions
  - Get product/UX team approval for interaction change
  - Design visual selection state
  - Implement solution in [`dot-uve-contentlet-tools`](core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-contentlet-tools/dot-uve-contentlet-tools.component.ts)
  - Add user onboarding/hints for new pattern
- [ ] **Implement contentlet controls redesign** (see "Design Decisions Needed" section above)
  - Make entire contentlet draggable
  - Move code/delete/edit actions to right panel
  - Implement quick edit mode for simple fields
  - Add keyboard shortcuts for delete action
- [ ] **Fix layout tab issues** (see "Known Issues to Address" section above)
  - Style the row/column reorder UI
  - Preserve column offset values when reordering
  - Test and document offset behavior
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
4. **General** - Ensure all existing features work, no console errors

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

**Last Updated:** 2025-12-28
