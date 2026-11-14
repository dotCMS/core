# Issue Resolution Specification: UVE drag-and-drop reorder is unreliable

**Feature Branch**: `issue-38575-uve-drag-drop-reorder`

**Created**: 2026-11-12

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#38575](https://github.com/dotCMS/core/issues/38575)

**Input**: User description: "When users drag a block in the Universal Visual Editor and the block covers the page container edge to edge, barely moving it (~10px) sends it to the end of the layout. Keep a grayed-out version of the source until it is dropped in its new position. Auto-scroll near the top/bottom edge works but is clunky, yanky and glitchy."

## Problem Statement *(mandatory)*

Reordering content blocks by drag and drop in the Universal Visual Editor (UVE) canvas does not behave the way an editor expects, in three related ways.

1. **A short drag moves the block to the end of its container.** If the editor picks a block up and releases it while the pointer is still over that same block, the block is moved to the last position in its container and the change is saved. This is very easy to trigger on pages whose blocks span the container edge to edge, because the pointer has nowhere to go sideways and stays over the dragged block. Releasing on the empty space between two blocks has the same result.
2. **Nothing marks the block being dragged.** The editor has no cue showing where the block came from, and no way to tell that releasing there should cancel the move.
3. **Auto-scroll while dragging is rough.** When the pointer nears the top or bottom edge of the page preview the page scrolls, but in uneven steps at a fixed speed, and the drop targets and insertion line disappear while it scrolls. On a page with ten blocks where two or three fit in the preview, moving a block from the top to the bottom is slow and hard to aim.

**Severity / Impact**: Medium. Affects every editor who reorders content in the UVE, on both traditional and headless pages. Defect 1 silently saves an order the editor did not ask for; defects 2 and 3 make intentional reordering slow and error-prone on long pages.

## Reproduction *(mandatory)*

**Environment**: main, 1.0.0-SNAPSHOT (November 10, 2026 build), Chrome on macOS. Any page built from a standard (non-advanced) template; most visible with full-width blocks.

**Steps to Reproduce** (short drag):

1. Open in the UVE, in Draft mode, a page with at least three full-width content blocks in one container.
2. Grab the first block by its drag handle.
3. Move the pointer about 10px and release.

**Expected Behavior**: The order of the blocks does not change and nothing is saved.

**Actual Behavior**: The block is moved to the last position in the container and the page is saved.

**Steps to Reproduce** (gap drop):

1. On the same page, drag the first block down and release on the empty space between the second and third blocks.

**Expected Behavior**: The block lands between the second and third blocks.

**Actual Behavior**: The block is moved to the last position in the container.

**Steps to Reproduce** (auto-scroll):

1. Open a page with about ten blocks where two or three fit in the preview.
2. Drag the first block to the bottom edge of the preview and hold it there.

**Expected Behavior**: The page scrolls smoothly and continuously, faster the closer the pointer is to the edge, and the drop targets and insertion line remain visible and aligned with the blocks.

**Actual Behavior**: The page scrolls in uneven steps at a fixed speed. Drop targets and the insertion line vanish and only return once scrolling stops.

**Reproducibility**: The short-drag and auto-scroll behaviors follow directly from how the editor resolves a drop and drives scrolling today, and are expected every time. They were diagnosed by reading the code and have not yet been recorded in a browser session; the plan phase starts by recording them.

## Scope of Investigation *(mandatory)*

- **Affected area**: Universal Visual Editor, editing canvas: moving an existing content block within or between containers, the drop-target overlay drawn over the page preview, and the auto-scroll of the previewed page during a drag. Also the message exchange between the editor and the page (the `@dotcms/uve` SDK), which is how the editor asks the page to scroll.
- **Suspected surface**: Frontend only (`core-web`): the edit-ema portlet and the `@dotcms/uve` SDK. No Java, no `com.dotmarketing.*` legacy code, no REST or database change.
- **Related known decisions**: The editor does not reach into the page; everything it knows about block positions arrives from the page through messages, and the same path must serve traditional and headless (cross-origin) pages. `@dotcms/uve` is a published package, so customer sites may run an older version than the editor expects. The plan consults `dotCMS/platform-adrs` for anything binding on the editor/SDK contract.

## Root-Cause Hypothesis

1. **Short drag and gap drop.** A move is resolved as "take the block out of its container, then insert it next to the block it was dropped on". When the block it was dropped on is the dragged block itself, that reference no longer exists after the removal, and the editor falls back to adding the block at the end. A drop on the space between blocks is treated as a drop on the container as a whole, which carries no before/after position, so it takes the same fallback.
2. **No source cue.** The drop-target overlay treats the dragged block's own area like any other target.
3. **Auto-scroll.** Scrolling advances one fixed step per pointer event received while the pointer is in a fixed-size edge zone, and each step is animated by the page. Pointer events arrive at an uneven rate, so the animated steps interrupt each other. The editor also discards its knowledge of block positions on every step, which is why the drop targets disappear until scrolling ends.
4. **Suspected, to confirm in planning:** at the very top or bottom of the page the page declines to scroll, but the editor has already discarded the block positions, which would leave the edge zone unable to accept a drop there.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Releasing a dragged block over its own original position changes nothing and saves nothing.
- Releasing on the position that would leave the block where it already is (immediately after the block above it, or immediately before the block below it) also changes nothing and saves nothing.
- Releasing on the space between blocks places the block next to the nearest block. A block is added at the end only when the container has no other blocks to position against.
- While a block is being dragged, its original position is shown as a grayed-out placeholder until the drag ends. The placeholder is not a drop target and shows no insertion line. It is drawn by the editor over the preview, not by changing the page, so it looks and behaves the same on traditional and headless pages.
- Auto-scroll during a drag is continuous, its speed increases the closer the pointer is to the top or bottom edge of the preview, and it stops when the pointer moves back toward the middle of the preview, when the block is dropped, and when the drag is cancelled. It keeps going when the pointer moves past the top or bottom edge, moves out sideways while the page is already scrolling, or leaves the application window during a drag that started inside the editor. It never starts while the pointer is beside the preview, and a file dragged in from the desktop stops scrolling when it leaves the window.
- Drop targets and the insertion line stay visible and aligned with the page while it auto-scrolls.
- Blocks at the very top and very bottom of the page can receive a drop.
- Pages running an older `@dotcms/uve` keep working: dragging, dropping and auto-scroll still function, with at least today's behavior.

**Explicitly out of scope / non-goals**:

- Reordering rows and columns in the palette's Layers tab.
- The Layout tab (template builder), including its row drag handle.
- Dragging new content from the palette, other than it benefiting from the same gap-drop and auto-scroll behavior. There is no source placeholder for it because it has no original position.
- Horizontal auto-scroll, touch devices, and keyboard-driven reordering.
- Any change to how a page save is stored or to server APIs.

## Regression Risk *(mandatory)*

- **Blast radius**: Every drop into the canvas shares the path being changed: moving a block within a container, moving it to another container, adding content from the palette, and dropping an image file from the desktop. Container rules that reject a drop (content type not allowed, container full, duplicate content) must keep rejecting it. The selection outline, hover toolbar and other overlays that depend on the editor's knowledge of block positions must still hide and reappear correctly around scrolling.
- **Backward compatibility**: The smoother scrolling needs the editor and the page to agree on a new instruction. A newer editor must still drive a page on an older `@dotcms/uve`, and a newer `@dotcms/uve` must still work in an older editor. Existing instructions keep their current meaning. Nothing here is rollback-unsafe: no stored data, index mapping or server contract changes.
- **Data considerations**: None. Pages already reordered by accident are not repaired; editors move the block back.

## Acceptance & Verification *(mandatory)*

- **AC-001**: Dragging a block about 10px and releasing over itself leaves the container's order unchanged and triggers no save. The editor returns to its idle state.
- **AC-002**: Releasing a block on the insertion point directly above or directly below its original position leaves the order unchanged and triggers no save.
- **AC-003**: Releasing a block on the space between two other blocks places it between them. In a container with no other blocks, the block is added to that container.
- **AC-004**: While a block is dragged, a grayed-out placeholder covers its original position from drag start until drop or cancel, on both a traditional and a headless page. Hovering the placeholder shows no insertion line.
- **AC-005**: With the pointer held still in the edge zone, the page scrolls continuously without the pointer moving. Scroll speed at the inner boundary of the zone is visibly lower than at the edge.
- **AC-006**: Auto-scroll stops within one frame of the pointer returning to the middle of the preview, of a drop, and of the drag being cancelled. *(Amended 2026-11-12 after the developer's manual check: it originally said "leaving the edge zone", which also stopped the scroll when the pointer left outward.)*
- **AC-011**: During a drag that started inside the editor, auto-scroll continues at full speed while the pointer is past the top or bottom edge of the preview, continues while the pointer is beside the preview if the page was already scrolling, and continues at its last speed while the pointer is outside the application window; the drag and its drop targets are still there when the pointer returns. Auto-scroll does not start while the pointer is beside the preview. A file dragged in from the desktop stops scrolling when it leaves the window.
- **AC-007**: During auto-scroll the drop targets and insertion line remain visible, and a drop made while or right after scrolling lands at the position the insertion line showed.
- **AC-008**: With the page scrolled to its very top (or bottom), the first (or last) block accepts a drop while the pointer is in the edge zone.
- **AC-009**: A page running the previously published `@dotcms/uve` can still be auto-scrolled during a drag and accepts drops.
- **AC-010**: Regression: moving a block to another container, adding content from the palette, dropping an image file, and the container rejection messages behave as before.
- **Verification method**: Vitest/Spectator specs written first and confirmed failing: the drop-resolution logic (AC-001 to AC-003, AC-010), the drop-target overlay (AC-004, AC-007, AC-008), the drag service's scroll control (AC-005, AC-006, AC-009), and the SDK's scroll handling (AC-005, AC-006, AC-009). Manual check in the UVE on a traditional page with full-width blocks and on the Next.js example for the headless path, with a screen recording attached to the implementation PR.

## Assumptions

- "Row" in the original report means a full-width content block in the editing canvas, not a layout row in the Layers or Layout tabs. The pages used to reproduce have a single layout row holding several full-width blocks.
- The edge zone that triggers auto-scroll keeps roughly its current size; exact speeds and easing are a planning detail, judged by feel in the manual check.
- For pages on an older `@dotcms/uve`, keeping today's auto-scroll (made less jumpy where the editor alone can do so) is acceptable; the full improvement arrives when the site upgrades the SDK.
- A grayed-out overlay over the source is sufficient; the page's own rendering of the block is not altered during the drag.
