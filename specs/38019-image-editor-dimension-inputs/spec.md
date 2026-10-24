# Issue Resolution Specification: Image editor Width/Height inputs apply on every keystroke and are not linked by default

**Feature Branch**: `issue-38019-image-editor-dimension-inputs`

**Created**: 2026-10-13

**Status**: Draft

**Type**: Issue / Bug Resolution

**Work item**: [#38019](https://github.com/dotCMS/core/issues/38019) (image editor dimension inputs)

**Input**: User description: "https://github.com/dotCMS/core/issues/38019"

## Problem Statement *(mandatory)*

In the new image editor (Edit Content > image or file field > edit image), the dimension inputs
apply their value on every keystroke instead of when the user finishes editing, and the output
Width and Height are not proportionally linked.

1. **Applied on every keystroke.** Clearing a Width of `4128` to type `500` applies every
   intermediate value (`412`, `41`, `4`) to the image. The canvas collapses into a thin strip, a
   resize render and its spinner start on every key, and the field is practically impossible to
   clear with the keyboard. The crop Width/Height inputs in the canvas toolbar also apply on
   every keystroke, resizing the crop box as the user types.
2. **Width and Height are not linked.** Setting Width to `500` on a `4128 x 3096` image leaves
   Height at `3096` and distorts the image. The `×` between the two fields reads as a link
   control (the reporter clicked it expecting to link the values), but it is plain text.

**Severity / Impact**: Medium. Every editor who resizes an image in the new image editor hits
it: each edit of a dimension renders broken intermediate sizes, and a proportional resize needs
manual arithmetic. Reported in internal product feedback on the new editor, whose overall
reception was positive.

## Reproduction *(mandatory)*

**Environment**: Latest `main`; any browser; any contentlet with an image or file field holding
an image (reported with a `4128 x 3096` JPEG).

**Steps to Reproduce**:

1. Open a contentlet with an image field in Edit Content and open the image editor.
2. In Transform > Dimensions, click into Width (`4128`) and delete its digits with Backspace.
3. Type `500` in Width and press Enter.
4. Look at Height and at the canvas.
5. Click the `×` between Width and Height.
6. Select the crop tool and the 1:1 preset, clear the crop Width input in the canvas toolbar,
   type `300`, and press Enter.

**Expected Behavior**:

- Steps 2-3: nothing is applied while typing; Width `500` is applied once, on Enter (leaving the
  field would apply it too).
- Step 4: Height reads `375`, keeping the image's proportion, because Width and Height are
  linked by default.
- Step 5: the `×` is a link/unlink toggle; once unlinked, each dimension changes on its own.
- Step 6: the crop box changes once, on Enter, to `300 x 300`.

**Actual Behavior**:

- Steps 2-3: every keystroke is applied; the canvas collapses into a strip, a render starts on
  every key, and the result is `500 x 3096`.
- Step 4: Height still reads `3096`; the image is distorted.
- Step 5: nothing happens; the `×` is plain text.
- Step 6: the crop box resizes on every keystroke while the field is cleared and retyped.

**Reproducibility**: Always.

## Scope of Investigation *(mandatory)*

- **Affected area**: Admin UI, Edit Content image editor (`core-web/libs/image-editor`): the
  Dimensions group of the Transform panel and the crop size inputs in the canvas toolbar.
- **Suspected surface**: Frontend only (Angular). No `com.dotcms.*` or `com.dotmarketing.*` code
  is involved, and the resize request the editor sends to the server keeps its shape.
- **Related known decisions**: Applying a resize discards an applied crop, mirroring the legacy
  editor (documented on the crop store feature, `withCrop`); this fix keeps that rule. The
  transform state already carries `lockAspectRatio` (initially `true`); the fix gives it a
  meaning instead of adding new state. The plan formally consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

- The output Width/Height inputs of the Transform panel (`DotImageEditorTransformPanelComponent`,
  `outputWidthChanged` / `outputHeightChanged`) dispatch `outputDimsChanged` from the input's
  `onInput` event, so every keystroke becomes a committed transform and a preview render.
- Their `onBlur` handler re-dispatches the dimensions already displayed, not the typed value.
  Once `onInput` stops dispatching, blur alone would never commit what the user typed. It also
  means that today, focusing and leaving a field without typing records a Resize and starts a
  render; with a crop applied, the fields show the crop's size, so that re-dispatch resizes the
  whole image to the crop's size and discards the crop.
- The handlers pair the edited dimension with the other one as displayed and never read
  `lockAspectRatio`; nothing in the library reads it, so no proportional recalculation exists.
- The crop size inputs (`DotImageEditorCanvasComponent`, `onCropWidthChange` /
  `onCropHeightChange`) follow the same `onInput` plus `onBlur` pattern. They are enabled only
  under an aspect preset and already derive the other dimension from the preset ratio, so only
  their commit timing is wrong.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Output Width and Height commit on blur or Enter instead of on every keystroke. An empty, `0`,
  or otherwise invalid value reverts the field to the last committed value and applies nothing.
  The revert is the only feedback: the inline minimum-value message shown under the fields today
  is removed.
- The crop Width and Height inputs in the canvas toolbar follow the same commit-and-revert rule,
  keeping their preset ratio lock; they stay disabled under Free, as today.
- The `×` between output Width and Height becomes a link/unlink toggle backed by the existing
  `lockAspectRatio`, linked each time the editor opens. The link is an editor setting, not an
  image edit: toggling it adds no history entry and starts no render, and undo, redo, reset, and
  removing an edit leave it as it is.
- While linked, committing one dimension recalculates the other from the displayed proportion,
  and both are applied as one change. While unlinked, each dimension changes on its own.
- Unit tests for the behaviors above.

**Explicitly out of scope / non-goals**:

- Scale and Rotate number fields: they already commit on `change`, not on every keystroke.
- A debounce timer: values commit on blur or Enter, not after a delay.
- Changing what Escape does while a dimension field has focus: it keeps its editor-wide meaning
  (close the editor through the unsaved-changes prompt), and a typed value that was never
  committed is not applied.
- Remembering the linked/unlinked choice across editor sessions or users; it resets to linked on
  every open.
- Using the original image's proportion for linked edits: the proportion is that of the
  dimensions currently displayed, so relinking after an unlinked edit keeps the new proportion.
- How a resize and an applied crop interact: committing a resize still discards the crop. While
  a crop is applied the fields show the crop's size, so a linked edit applies the crop's
  proportion to the whole image, much as an unlinked edit today applies the crop's other
  dimension.
- An upper limit on the output dimensions; there is none today.
- Crop under Free (no preset), the crop presets, and dragging the crop box.
- The server-side resize, the filter URL it builds, and the Save flow.

## Regression Risk *(mandatory)*

- **Blast radius**: The `outputDimsChanged` transform and the effective `outputDimensions` that
  the preview, the file size readout, and Save consume; the history list, where consecutive
  resizes already coalesce into a single Resize entry and must keep doing so; undo, redo, reset,
  and removing an edit, which restore the whole transform state from a snapshot and must now keep
  the current `lockAspectRatio` instead of the saved one; the crop reset that a resize triggers;
  the crop size readout under Free, which must keep updating live while the box is dragged.
- **Backward compatibility**: No API, persisted content, or serialized state change.
  `lockAspectRatio` already exists in the transform state with the same default; only its use is
  new. Not rollback-unsafe.
- **Data considerations**: None. Existing images and their stored metadata are untouched.

## Acceptance & Verification *(mandatory)*

- **AC-001**: Typing or deleting in the output Width or Height input applies nothing to the image
  and starts no render while the user types.
- **AC-002**: The typed output value is applied when the input loses focus or the user presses
  Enter. Leaving a field whose value did not change applies nothing and starts no render,
  including leaving it right after Enter applied the value.
- **AC-003**: If the committed output value is empty, `0`, or otherwise invalid, the field shows
  the last committed value again, nothing is applied, and no error message is shown.
- **AC-004**: Typing in the crop Width or Height input does not change the crop box; the value is
  applied on blur or Enter, an empty or invalid value reverts the field, and under an aspect
  preset the other crop dimension still follows the preset ratio.
- **AC-005**: The `×` between output Width and Height is a toggle button with an accessible label
  and a visible linked/unlinked state.
- **AC-006**: Width and Height are linked each time the editor opens.
- **AC-007**: While linked, committing a new Width sets Height to the displayed height/width
  proportion applied to the new Width, rounded to the nearest integer and at least 1
  (`4128 x 3096` with Width `500` gives `500 x 375`), and committing a new Height sets Width the
  same way. Both values are applied as one change.
- **AC-008**: While unlinked, committing one dimension leaves the other unchanged.
- **AC-009**: Toggling between linked and unlinked changes neither dimension nor the image, adds
  no history entry, and starts no render; undo, redo, reset, and removing an edit leave the link
  state as it is.
- **AC-010**: Regression: the Scale and Rotate inputs, the Free crop readout, and the Resize
  history entry behave as before (consecutive resizes coalesce into one Resize entry).
- **Verification method**:
  - Vitest/Spectator specs for the Transform panel component, driven through the input's real
    typing, blur, and Enter events rather than by calling the handlers: no dispatch while typing;
    one dispatch on blur and on Enter; Enter then blur dispatches once; leaving an unchanged
    field dispatches nothing; revert with no dispatch and no error message on empty, `0`, and
    invalid; proportional recalculation when linked (both directions, rounding, minimum 1);
    independent edit when unlinked; the toggle's accessible label and state; toggling dispatches
    no dimension change. The existing case that expects a `null` width and the minimum-value
    message for a value below 1 asserts the defect and is replaced.
  - Vitest/Spectator specs for the canvas component's crop size inputs: no crop change while
    typing; commit on blur and on Enter; revert on empty or invalid; preset ratio preserved.
  - Store spec for the transform feature: `lockAspectRatio` starts `true`; toggling it keeps the
    output dimensions, adds no history entry, and starts no render.
  - Store spec for the history feature: undo, redo, reset, and removing an edit keep the current
    `lockAspectRatio`.
  - Manual: repeat the Reproduction steps in the browser and confirm the Expected Behavior.

## Assumptions

- "Otherwise invalid" means any value below 1. The inputs accept digits only, so an empty field
  and `0` are the invalid values a user can actually type.
