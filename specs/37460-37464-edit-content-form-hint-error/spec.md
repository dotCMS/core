# Issue Resolution Specification: Adopt the global `.form` system and standardize hint / required-error presentation in the new Edit Contentlet

**Feature Branch**: `37460-37464-edit-content-form-hint-error`

**Created**: 2026-09-16

**Status**: Draft — all clarifications resolved; ready for `/speckit-plan`

**Type**: Issue / Bug Resolution

**Related GitHub Issues**:
[#37460](https://github.com/dotCMS/core/issues/37460) (Backfill: adopt the global `.form` / `.field` form system) and
[#37464](https://github.com/dotCMS/core/issues/37464) (Standardize field hint and required-error presentation) — resolved together, in that order.

**Depends on**: [#37465](https://github.com/dotCMS/core/issues/37465) (Date/Time field width, clearing and picker footer) — already implemented on
`issue-37465-calendar-field-width-clear-picker-footer-impl`. See [Dependency on #37465](#dependency-on-37465).

**Input**: User description: *"Atacarlos como un solo esfuerzo en dos PRs secuenciales sobre la misma rama base, en este orden: 1. #37460 primero — `.form` en el form raíz, `.field` en `dot-card-field`, label como hijo directo real, `dotFieldRequired` bare mode. 2. #37464 después — consolidar las tres variantes de markup contra un sistema que ya funciona, quitar el tooltip del label, mover el error a "solo tras save/publish", hint+error juntos."*

---

## Problem Statement *(mandatory)*

Two issues describe the same defect from opposite ends, and neither can be closed correctly on its own.

A content author editing a contentlet in the new Angular Edit Contentlet sees:

- **Field hints and validation messages rendered as plain unstyled body text.** A required-field error is *not red*. It reads as ordinary prose next to the field it is meant to flag.
- **Inconsistent presentation between field types.** A hint is plain text under a Text field, a hover-only tooltip icon next to a Block Editor label, and differently-sized text under a Tag field.
- **A red error that appears too early.** Tabbing through an empty required field turns it red immediately, before the author has attempted to save anything.
- **A hint that disappears exactly when it is most needed.** When a field goes into error, its hint is suppressed — the author is told "This field is required" and simultaneously loses the sentence explaining what to put there.
- **Labels that do not match any other admin form.** Edit Contentlet labels render at 14px/400 while every other dotCMS admin form (tags, locales, users, push publish) renders at 12.25px/500.

The underlying cause of the first two is a single missing CSS class, and the underlying cause of the last three is a mixture of duplicated markup and a validation signal wired to the wrong event.

**Severity / Impact**: Medium. Every content author, on every content type with a required or hinted field, on every save — which is the primary daily workflow of the product. No data loss and no functional block (saving is still correctly prevented); the damage is that validation feedback does not read as validation feedback. Reported directly by Design: *"the edit content screen is not taking the classes from the `.form > .field > label`"*.

---

## Reproduction *(mandatory)*

**Environment**: dotCMS `main`, new Angular Edit Contentlet (`core-web/libs/edit-content`), any browser, any site. Requires a content type with at least one required field that also carries a hint.

### Repro A — hints and errors render unstyled *(the reported bug)*

1. Create a content type with a **required** Text field whose **Hint** is `Use the customer's legal name`.
2. Open **Content → Add Content** for that content type in the new editor.
3. Observe the hint under the field.
4. Click into the field and tab out without typing.

**Expected**: the hint renders small and grey (`text-sm text-gray-500`); the required message renders small and **red** (`text-sm text-red-500`).

**Actual**: both render in the default body colour and the inherited body size. The error is indistinguishable from the hint. Verified cause: the seven templates that already carry `p-field-hint` / `p-field-error` get nothing, because those classes exist only as `.form .p-field-hint` / `.form .p-field-error` in `core-web/apps/dotcms-ui/src/style.css:86-94`, and the editor's root `<form>` (`dot-edit-content-form.component.html:9`) carries `class="p-fluid h-full"` — no `.form`.

**Reproducibility**: Always.

### Repro B — the error fires on blur, before any save

1. Same content type, same screen. Do **not** press any workflow action.
2. Click into the required field and press Tab.

**Expected**: nothing changes. The author has not attempted to save.

**Actual**: the control gets a red border and the required message appears. Verified cause: `BaseWrapperField` (`fields/shared/base-wrapper-field.ts:33-35`) sets `$hasError` from `control.invalid && control.touched`; blur marks the control touched.

**Reproducibility**: Always.

### Repro C — the hint vanishes when the field is in error

1. Same field, now in error (via Repro B, or by firing a workflow action with the field empty).

**Expected**: `This field is required` in red, and the hint `Use the customer's legal name` immediately below it.

**Actual**: the hint is gone. Verified cause: every field template except Date/Time guards the hint with `@if (!fieldHasError && field.hint)`.

**Reproducibility**: Always.

### Repro D — the hint is a hover-only tooltip on Block Editor

1. Add a **Block Editor** field with a hint to the same content type. Open the editor.

**Expected**: the hint is readable text under the control, like every other field.

**Actual**: a `pi pi-info-circle` icon sits next to the label; the hint is only reachable by hovering. Verified cause: `dot-edit-content-block-editor.component.html:10` passes `[hint]="field.hint"` into `dot-card-field-label`, which renders it as a `pTooltip`.

**Reproducibility**: Always. **Block Editor is now the only caller of that tooltip** — see [Correction 2](#corrections-to-the-issue-descriptions).

### Repro E — label typography does not match other admin forms

1. Open Edit Contentlet in one tab and **Site → Tags → Add Tag** in another. Compare field labels.

**Expected**: identical typography — both are dotCMS admin forms.

**Actual**: Edit Contentlet labels are larger and lighter. Verified cause: `.form .field > label { @apply text-sm font-medium }` never applies, for the same reason as Repro A.

**Reproducibility**: Always.

---

## Scope of Investigation *(mandatory)*

- **Affected area**: Admin UI — content editing. Specifically the new Angular Edit Contentlet library `core-web/libs/edit-content`, and one line of the global stylesheet `core-web/apps/dotcms-ui/src/style.css`.
- **Suspected surface**: **Modern frontend only.** No Java. Not `com.dotcms.*` and not `com.dotmarketing.*`. Legacy exposure is indirect but real: part of this library is compiled into the `dotcms-binary-field-builder` custom-element bundle consumed by the legacy Dojo editor — see [Regression Risk](#regression-risk-mandatory) and [Decision 1](#decisions).
- **Related known decisions**: the global form system is documented in `docs/frontend/STYLING_STANDARDS.md` (form system table). `dotCMS/dotcms-claude-plugins#33` already enforces it for *new* code; this work is the backfill. `/speckit-plan` will consult `dotCMS/platform-adrs` for any binding decision on global-vs-component styling.

---

## Root-Cause Hypothesis

Three distinct causes, each verified in the code:

1. **Missing `.form` ancestor.** The dotCMS form system is global CSS, and every rule is descendant-scoped to `.form` (`apps/dotcms-ui/src/style.css:35-94`). The editor's root `<form>` never carries the class, so `.field`, `.form .field > label`, `.form .p-field-hint`, `.form .p-field-error`, `.form .form-checkbox` and `.form .form-radio` are all inert across the entire screen. This alone explains Repro A and Repro E.

2. **A parallel abstraction that never adopted the canonical markup.** `dot-card-field` reimplements `.field` as `<div class="flex flex-col gap-2">` and puts the `<label>` inside a child component (`dot-card-field-label`). Because `.form .field > label` is a **direct-child** selector, adding `.form` and `.field` alone is *not sufficient* — the `dot-card-field-label` host element sits between `.field` and the `<label>` and the rule still will not match. On top of that, the hint/error slot was written three different ways across the field templates, so there is no single place to fix.

3. **The error signal is wired to `touched`, not to a save attempt.** `$hasError = control.invalid && control.touched` turns a field red on blur. The save path (`dot-edit-content-form.component.ts:530-537`, `fireWorkflowAction`) already calls `markAllAsTouched()` and scrolls to `.field-error-marker`, so "after save" is *implied* by touched-after-submit but not *gated* on it. A separate submit-attempted signal is needed.

---

## Dependency on #37465

#37465 is implemented on `issue-37465-calendar-field-width-clear-picker-footer-impl` and its **FR-016** requires four passages to be withdrawn from #37464's description on merge: FR-009 of #37465 removes the Date/Time timezone line from under the input, so those passages instruct a future implementer to reconcile an element that will no longer be on the screen.

Two of them are acceptance criteria. **This spec treats both as WITHDRAWN and does not carry them forward:**

| Withdrawn AC from #37464 | Why |
|---|---|
| *"When a Date/Time field has both a timezone line and a hint, only the hint is shown — the timezone line is dropped."* | There is no timezone line under the input to collide with. |
| *"When a Date/Time field has a timezone and no hint, the timezone line renders as it does today."* | Same. |

This spec does **not** edit any GitHub issue. Withdrawing those passages from #37464's description is a separate, developer-approved action.

The dependency runs the other way too, and in our favour: #37465 already rebuilt the Date/Time footer into exactly the shape #37464 asks for — error first, hint second, both plain text, no tooltip, no icon (`dot-edit-content-calendar-field.component.html:22-56`). It is the **reference implementation for Phase 2**, and it carries an in-code TODO naming both issues:

> *"The utilities duplicate what `.form .p-field-error` / `.p-field-hint` in `apps/dotcms-ui/src/style.css` already declare, because those rules only apply under a `.form` ancestor and the new Edit Content editor has none — so the classes alone render as unstyled text. Adopting `.form` here is #37460; making these rules stand on their own is an AC of #37464. Until one of those lands this field states its own colours. Drop the utilities, keeping the semantic classes, once either does."*

Dropping those two Tailwind utility pairs is an explicit deliverable of Phase 1.

---

## Corrections to the issue descriptions

Verified against the code on `main` + the #37465 branch. Both issue bodies are stale in four places; the spec follows the code, not the issue text.

**Correction 1 — #37464: "Category and File/Image/Binary fields, which currently have no hint slot at all, render hints."** *(false)*
Both already render hints today, via the `.error-message` / `.hint-message` variant (`dot-edit-content-category-field.component.html:30-36`, `dot-edit-content-file-field.component.html:28-34`). Their real gaps are the same as everyone else's: the classes are unstyled, and the hint is suppressed on error. **The field that genuinely has no hint and no error slot at all is Text Area** — see [Decision 2](#decisions). Line Divider has none legitimately.

**Correction 2 — #37464: "The Date/Time field routes the hint into that tooltip whenever a timezone is shown."** *(no longer true)*
#37465 already removed it. `dot-edit-content-calendar-field.component.html` passes no `[hint]`. **Block Editor is the only remaining caller** of the `dot-card-field-label` tooltip.

**Correction 3 — #37464's three-variant census is inaccurate.** Verified census of the 18 field templates:

| Variant | Field templates |
|---|---|
| `p-field-error` + `p-field-hint` | calendar, checkbox, radio, select, text |
| `.error-message` + `.hint-message` | category, file, host-folder, json, key-value, multi-select, tag, wysiwyg |
| mixed | custom (`p-field-error` + `.hint-message`), relationship (`p-field-error` + `.error-message` + own `data-testid="relationship-field-error"`) |
| no hint/error slot at all | block-editor, **text-area**, line-divider |

`.error-message` also appears **outside** the field templates, in `dot-create-content-dialog`, `dot-select-existing-content`, `dot-form-import-url` and `dot-form-file-editor`. #37464's AC *"`.error-message` / `.hint-message` no longer appear in `libs/edit-content`"* therefore reaches further than the field templates — see [Decision 1](#decisions).

**Correction 4 — #37460's path for `site-field` / `language-field` is wrong.** Actual path is
`fields/dot-edit-content-relationship-field/components/dot-select-existing-content/components/**search/**components/{site,language}-field/`. The copy-paste a11y bug is real and confirmed: both templates open with `<label for="language-field" class="mb-2 inline-block">`.

---

## Fix Scope & Non-Goals *(mandatory)*

Delivered as **two sequential PRs on the same base branch**. The ordering is a hard dependency, not a preference:

> If Phase 2 shipped first, its consolidated markup would target `p-field-hint` / `p-field-error` — classes that still resolve to nothing — so it would have to carry local Tailwind utilities to be visible, which is precisely what Phase 1 forbids and then has to unpick. Both phases also rewrite `dot-card-field-label`: Phase 1 changes how its `<label>` participates in the layout, Phase 2 deletes its `hint` input. Out of order, that component is touched twice with conflicting intent, and the second pass invalidates the first pass's browser verification.

### Phase 1 — #37460: make the global form system reach the editor *(PR 1)*

**In scope**:

- `class="form"` on the root `<form>` in `dot-edit-content-form.component.html:9`, dropping the hand-rolled layout classes the global system already covers.
- `dot-card-field` emits `<div class="field">` in place of `<div class="flex flex-col gap-2">`, keeping both things it exists for: the `field-error-marker` anchor and `:host ::ng-deep dot-card-field-footer:empty { display: none }`. The component is **kept**, not deleted.
- Make `.form .field > label` actually match in the rendered DOM. `dot-card-field-label`'s host element currently breaks the direct-child relationship; resolve it (e.g. `display: contents` on that host, or making the `<label>` a real direct child of `.field`). **Verified in the browser, not in the template.**
- `dotFieldRequired` in **bare mode** on the label, replacing the hand-written `p-label-input-required` in `dot-card-field-label.component.html:3` and `dot-form-file-editor.component.html:47`. **Not** `checkIsRequiredControl`: that mode reads `Validators.required` off the `FormGroup`, but a required Block Editor uses `blockEditorRequiredValidator()` (`dot-edit-content-form.component.ts:773`) instead, so the asterisk would silently vanish. The source of truth is the content type's `field.required`, exposed as the `isRequired` getter in `base-wrapper-field.ts:57-70`.
- Checkbox and radio option rows use the global `.form-checkbox` / `.form-radio` in place of hand-rolled `flex items-center gap-2`.
- Clean `<label>` elements: no typography or spacing utilities. Fix `site-field`'s `for="language-field"` to point at its own control id.
- Drop the duplicated Tailwind utilities from the Date/Time footer (`text-sm text-red-500` / `text-sm text-gray-500`), keeping the semantic classes — the TODO the #37465 work left behind.
- Adopt the canonical markup in the in-library dialogs and sub-forms that *do* sit in the Angular app: the sidebar workflow dialog's dead `<div class="field">` (`dot-edit-content-sidebar-workflow.component.html:75`) and the relationship search fields.
- **Leave the component SCSS under `dot-edit-content-file-field` in place** — the legacy-bundle exception of [Decision 1](#decisions). #37460's AC to delete it is withdrawn. The one change there is `dot-form-file-editor.component.html:47` adopting `dotFieldRequired`, which is safe in both bundles.
- **No local CSS or Tailwind utility is added anywhere in `libs/edit-content` to preserve the previous label typography or field gap.** The global values win.

**Accepted visual deltas** (intentional, not to be compensated; `html { font-size: 14px }` so `1rem = 14px`):

| Property | Today | After | Delta |
|---|---|---|---|
| label font-size | 14px inherited | `text-sm` = 12.25px | −1.75px |
| label font-weight | 400 | `font-medium` = 500 | +100 |
| gap label ↔ control | `gap-2` = 7px | `gap-1` = 3.5px | −3.5px |
| hint colour | default body colour | `text-gray-500` | fixed |
| error colour | default body colour | `text-red-500` | **fixed — this is the bug** |
| content-type row/column grid | `mb-5 gap-9` / `gap-8` | unchanged | 0 |
| checkbox/radio rows | `flex items-center gap-2` | `flex flex-row items-center gap-2` | 0 — identical |
| required asterisk | class written by hand | same class via the directive | 0 |
| `.form { space-y-5 }` | — | inert (`<p-tabs>` is the form's only direct child) | 0 |

Nothing reflows: no field changes width, no column moves, no tab re-lays-out.

### Phase 2 — #37464: one hint/error presentation, gated on save *(PR 2, builds on PR 1)*

**In scope**:

- **Consolidate the three markup variants into one shared presentation** used by every field type, modelled on the Date/Time footer #37465 already built. `.error-message` / `.hint-message` disappear from the field templates and from the in-app dialogs (`dot-create-content-dialog`, `dot-select-existing-content`). They remain in the two legacy-bundle dialogs — see [Decision 1](#decisions).
- **Text Area gains the shared hint/error slot**, which it has today for neither. Its template carries no `dot-card-field-footer` at all. This is new behavior, not restyling: a Text Area hint becomes visible for the first time, and an empty required Text Area starts explaining why the save is blocked ([Decision 2](#decisions)).
- **The hint is always plain text below the control.** Remove the `hint` input and the `pi pi-info-circle` tooltip from `dot-card-field-label` and update its one remaining caller, Block Editor. Never a tooltip, never an icon.
- **Error and hint shown together** — required message first, hint immediately below. Remove the `!fieldHasError` guard everywhere. When the error clears, the hint stays in place with no layout jump.
- **The required error surfaces only after a save or publish attempt.** Blur and tab do not turn a field red. Requires a submit-attempted signal feeding `$hasError`, replacing `control.invalid && control.touched`.
- **The error clears as soon as the field holds a valid value** — no second save attempt.
- **The error message is text only** — no icon, red, rendered from the existing `dot.edit.content.form.field.required` key.
- Save/publish stays blocked while a required field is empty, and the scroll-to-first-error `.field-error-marker` anchor keeps working.
- Where a red border on the control is not meaningful for a composite widget (Block Editor, WYSIWYG, File/Image/Binary, Relationship, Category, Key-Value, JSON, Tag, Host/Folder), the error border wraps the widget's outer container.
- A required field renders a red asterisk immediately after the label text, decorative only — not announced separately by screen readers; the control carries `required` / `aria-required`. A non-required field renders no asterisk.
- No empty hint element when a field has no hint (no phantom spacing).

**Explicitly out of scope / non-goals** *(both phases)*:

- Other portlets, and the legacy JSP/Dojo Edit Contentlet screen.
- The content-type row/column grid (`mb-5 grid gap-9` / `flex flex-col gap-8`) — that grid expresses the content type's own layout, which the global `.form` system cannot represent, and `.form`'s rhythm never reaches it.
- Moving the Date/Time timezone line (settled by #37465).
- Validation messages other than *required* — min/max, regex, custom validators keep their current presentation and may adopt the same slot in follow-up work.
- Backend, REST, or content type definition changes. Nothing in Java.
- Creating or editing an ADR. Editing the text of #37460 or #37464 on GitHub.

---

## Regression Risk *(mandatory)*

- **Blast radius — the whole editor at once.** Adding one class to the root `<form>` switches on five global rules across every field on every content type simultaneously. This is exactly the intent, and it is why the accepted-deltas table above is measured rather than estimated, and why manual exercise against an all-field-types content type is an acceptance criterion rather than a nicety.
- **Blast radius — the legacy Dojo bundle.** `DotBinaryFieldCeBridgeComponent` → `DotFileFieldComponent` → `DotFormFileEditorComponent` + `DotFormImportUrlComponent` all compile into `apps/dotcms-binary-field-builder`, the custom element the legacy editor loads. Verified: that app's `project.json` styles list does **not** include `apps/dotcms-ui/src/style.css`, so `.form`, `.field`, `.p-field-hint` and `.p-field-error` **do not exist in that bundle**. Both components' SCSS carries an explicit `STYLE EXCEPTION` header documenting this. Independently, both dialogs open with `appendTo: 'body'`, so even inside the Angular app they are never DOM descendants of the editor's `.form`. #37460's AC to delete that SCSS "in favour of the global system" is **not achievable as written** → withdrawn, see [Decision 1](#decisions). The one part that *is* safe: `.p-label-input-required::after` lives in `libs/dotcms-scss/angular/dotcms-theme/_misc.scss:56`, which that bundle **does** load, so switching line 47 to `dotFieldRequired` changes nothing there.
- **Silent loss of the required asterisk on Block Editor.** The `checkIsRequiredControl` trap described above. Guarded by a dedicated test.
- **Scroll-to-first-error.** `scrollToFirstError()` queries `.field-error-marker` from the document. Phase 1 restructures the element that renders it and Phase 2 changes when it renders. Both phases must keep it working.
- **Empty-footer collapse.** `dot-card-field-footer:empty { display: none }` prevents phantom spacing. Phase 2 rewrites what goes in that footer; a footer that is "empty" but contains whitespace or a comment node is no longer `:empty` and would reintroduce the gap.
- **Backward compatibility**: none at risk. No API, no serialized state, no DB or ES mapping, no content. Presentation only; fully revertible by removing one class and reverting the templates. Not a rollback-unsafe category.
- **Data considerations**: none. No migration, no data repair.
- **Test fallout**: existing specs assert on `small.p-field-error`, `hint-<variable>` testids, `relationship-field-error` and `category-field-required`. Phase 2 changes that markup, so those specs are updated as part of the change, not after it.

---

## Acceptance & Verification *(mandatory)*

### Phase 1 — #37460

- **AC-101**: Repro A produces the expected behavior: the hint renders `text-sm text-gray-500` and the required message `text-sm text-red-500` in the running editor.
- **AC-102**: Repro E produces the expected behavior: Edit Contentlet labels are typographically identical to `dot-tags-create`.
- **AC-103**: The root `<form>` carries `class="form"`, and any hand-rolled layout class the global system already covers is removed from it.
- **AC-104**: `dot-card-field` emits `<div class="field">`, and still renders `field-error-marker` and still hides an empty `dot-card-field-footer`.
- **AC-105**: `.form .field > label` matches in the **rendered DOM** — verified in the browser with devtools, on at least one field of each `dot-card-field` consumer shape, not by reading the template.
- **AC-106**: No `<label>` in `libs/edit-content` carries a `class` attribute with typography or spacing utilities.
- **AC-107**: `dot-card-field-label` and `dot-form-file-editor.component.html:47` use `dotFieldRequired` in bare mode; `p-label-input-required` is written by hand nowhere in the library.
- **AC-108**: A **required Block Editor field still shows its asterisk** (the `blockEditorRequiredValidator` case).
- **AC-109**: Checkbox and radio option rows use `.form-checkbox` / `.form-radio`; the rendered layout is byte-identical to today.
- **AC-110**: `site-field`'s `for` points at its own control id, not `"language-field"`.
- **AC-111**: The Date/Time footer no longer carries the duplicated `text-sm text-red-500` / `text-sm text-gray-500` utilities, and the TODO comment naming #37460/#37464 is removed. The field still renders exactly as #37465 specified.
- **AC-112**: **No local CSS or Tailwind utility is added anywhere in `libs/edit-content`** to preserve the previous label typography or field gap. A reviewer can diff for added `text-sm`, `font-medium`, `gap-1`, `gap-2` on labels and fields and find none.
- **AC-113**: The editor is exercised manually against a content type covering every field type (text, textarea, select, radio, checkbox, date/time, tags, block editor, wysiwyg, relationship, category, binary/file/image, key-value, json, custom field, host-folder, line divider) with **no visual regression beyond the accepted-deltas table**.
- **AC-114**: The legacy Dojo editor's binary field (the `dotcms-binary-field-builder` custom element) renders unchanged — file editor dialog and import-URL dialog included.
- **AC-115**: The SCSS under `dot-edit-content-file-field` is untouched except for its `STYLE EXCEPTION` header, which is extended to name this decision. No other SCSS in `libs/edit-content` reimplements form layout — verified: those three files are the only ones in the library's 24 `.scss` files that do, and all three sit in that subtree.

### Phase 2 — #37464

Phase 1 owns the hint/error *styling* criterion (AC-101). Phase 2 references it and does not redefine it.

- **AC-201** *(Repro C)*: When a field in error also has a hint, both are shown — required message first, hint immediately below. When the error clears, the hint stays in place with no layout jump.
- **AC-202** *(Repro B)*: Leaving an empty required field via blur or tab shows **no** red border and **no** message. Firing a save or publish action with that field empty shows both.
- **AC-203**: A field's error clears as soon as it holds a valid value — no second save attempt.
- **AC-204** *(Repro D)*: The hint renders as plain text below the control for **every** field type. The `pi pi-info-circle` tooltip and the `hint` input are gone from `dot-card-field-label`, and Block Editor is updated.
- **AC-205**: When a field has no hint, no empty hint element is rendered — no phantom spacing.
- **AC-206**: The error message contains no icon — text only, red, from the `dot.edit.content.form.field.required` key.
- **AC-207**: One shared presentation is used by all field types, composite widgets included (Block Editor, WYSIWYG, File/Image/Binary, Relationship, Category, Key-Value, JSON, Tag, Host/Folder). Where a red border on the control is not meaningful, the error border wraps the widget's outer container.
- **AC-208**: `.error-message` / `.hint-message` no longer appear in any field template, nor in `dot-create-content-dialog` or `dot-select-existing-content`. The only remaining occurrences in `libs/edit-content` are inside `dot-edit-content-file-field`, which is the legacy-bundle exception of [Decision 1](#decisions).
- **AC-209**: A required field renders a red asterisk immediately after the label text; a non-required field renders none, and the asterisk is decorative — not announced separately by screen readers. **The control carries `required` / `aria-required` on every field type ARIA permits it on**: native controls, `combobox` (Host/Folder, Select, Multi-Select) and `radiogroup` (Radio). Composite widgets — checkbox sets, Key/Value, Category, Relationship, File/Image/Binary and Custom Field — take the `group` role instead, which ARIA does not define `aria-required` on; they are given an accessible name from the field label so the control announces which field it is.
- **AC-210**: Save/publish is still blocked while any required field is empty, and the form still scrolls to the first field in error via `.field-error-marker`.
- **AC-211**: A Text Area renders its hint, and an empty required Text Area shows the required message after a save attempt — neither of which it does today.

### Verification method

**Automated** — `pnpm nx test edit-content` and `pnpm nx lint edit-content` must pass for each PR independently.

Per the constitution's Principle V, every test below is written, developer-approved, and **confirmed failing (Red)** before any implementation code:

| # | Test | Phase |
|---|---|---|
| T-01 | Rendered DOM: the `<label>` is an effective direct child of `.field` (asserts the computed relationship, not the template) | 1 |
| T-02 | A required Block Editor renders the asterisk; a non-required one does not | 1 |
| T-03 | Checkbox/radio option rows carry `.form-checkbox` / `.form-radio` | 1 |
| T-04 | `site-field`'s `for` matches its own control id | 1 |
| T-05 | All four states on a representative field — (no hint, no error), (hint, no error), (error, no hint), (error + hint) | 2 |
| T-06 | Asterisk present / absent | 2 |
| T-07 | Blur alone does **not** produce the required error; a save/publish attempt **does** | 2 |
| T-08 | The error clears on a valid value without a second save | 2 |
| T-09 | The hint renders as text, and no tooltip/`pi pi-info-circle` exists for any field | 2 |
| T-10 | Existing specs referencing `small.p-field-error`, `hint-<variable>` testids, `relationship-field-error` and `category-field-required` are migrated to the consolidated markup | 2 |
| T-11 | Text Area renders its hint, and shows the required message after a save attempt (new behavior — this test fails today for a reason no other field's does: the element does not exist) | 2 |

**Manual** — AC-113 and AC-114 are browser checks. AC-105 is a devtools check and cannot be satisfied by a passing unit test alone.

---

## Decisions

All three open questions are resolved. Recorded here because each one either contradicts an acceptance criterion as written in the issues, or adds scope the issues do not mention.

### Decision 1 — the legacy-bundle exception is kept, and why is documented on the PR

**Decision: the component SCSS under `dot-edit-content-file-field` stays. Only `dot-form-file-editor.component.html:47` changes, adopting `dotFieldRequired`.**

The global form system cannot reach those components, for two independent reasons:

1. **They compile into a different Angular application.** `dotcms-binary-field-builder` is a full Nx app (`projectType: "application"`) whose `ngDoBootstrap` registers `<dotcms-binary-field>` as a custom element via `createCustomElement` instead of rendering a page; the legacy Dojo editor loads it from `edit_contentlet.jsp`, `edit_field.jsp`, `binary-field.js` and `top_inc.jsp`. The chain `DotBinaryFieldCeBridgeComponent → DotFileFieldComponent → DotFormFileEditorComponent + DotFormImportUrlComponent` pulls both dialogs into that bundle. Global stylesheets are configured **per application build**, and `libs/edit-content` is a library with no `styles` array of its own, so it inherits whichever app compiles it. The two apps load two different files that happen to share a name — `apps/dotcms-ui/src/style.css` (the form system) and `apps/dotcms-binary-field-builder/src/style.css` (14 lines: a dialog backdrop fix and a button-icon reset). Copying the rules over would not help either: they are written with Tailwind `@apply`, and Tailwind only runs where a PostCSS config exists — `apps/dotcms-ui/.postcssrc.json` has one, the binary-field-builder has none.
2. **Both dialogs open with `appendTo: 'body'`** (`dot-file-field.component.ts:670` and `:799`). `.form .field` is a descendant selector; PrimeNG moves the dialog out of the `<form>`, so even inside dotCMS-UI — with Phase 1's `.form` in place — the rule would not match.

This is not an arbitrary carve-out: of the library's 24 `.scss` files, exactly three reimplement form layout, and all three sit inside `dot-edit-content-file-field`. The exception is co-extensive with the legacy bundle. Those files already carry a `STYLE EXCEPTION` header written for precisely this reason.

The one part that **is** safe there: `.p-label-input-required::after` lives in `libs/dotcms-scss/angular/dotcms-theme/_misc.scss:56`, which the binary-field-builder's `project.json` **does** load, and the chain's encapsulation is `Emulated` rather than `ShadowDom`, so global stylesheets of that bundle do reach the components. Switching line 47 from a hand-written class to the directive produces the same class and renders identically in both bundles.

**The AC stays in #37460 as written.** Rather than withdraw it, the PR records why the SCSS is retained: these two dialogs ship inside the `dotcms-binary-field` custom element that the legacy Dojo editor loads, and that bundle has neither the global stylesheet nor Tailwind. Deleting the SCSS would leave both dialogs unstyled there. Documenting the constraint is more useful to the next reader than removing the criterion that surfaced it.

### Decision 2 — Text Area gains the shared hint/error slot

**Decision: in scope for Phase 2.**

Contrary to #37464's gap list, the field with no slot is Text Area, not Category or File — its template contains no `dot-card-field-footer` at all. Adding it is the only way #37464's stated goal ("one shared presentation is used by all field types") is actually true.

This is **new behavior, not restyling**, and should be called out in the PR description and in QA: a Text Area hint becomes visible for the first time, and an empty required Text Area starts showing the message explaining why the save is blocked. Covered by AC-211 and T-11.

### Decision 3 — `.form` on the editor root is non-negotiable; the global stylesheet is not touched

#37464 says it once, in a single AC under *Consistency across field types*:

> - [ ] Error and hint styles resolve without depending on an ancestor `.form` class.

It never says "do not use `.form`". Read against its own finding #4 — *"Only `p-field-error` / `p-field-hint` are styled (`apps/dotcms-ui/src/style.css`, **scoped under `.form`**)"* — the intent matches #37460 exactly: both issues diagnosed the same root cause and both want the styles to reach the editor. The two were opened two hours apart by different authors (#37460 at 19:10, #37464 at 21:09 on 2026-09-08) and **neither references the other**, so that AC was written without knowing a separate issue was about to supply the missing `.form`.

Phase 1 satisfies its intent. **`apps/dotcms-ui/src/style.css` is not edited at all** — not to un-scope the hint/error rules, and not to relax `.form .field > label` into a descendant selector (a one-line alternative to the label restructuring, verified harmless but declined on the same grounds: the global stylesheet stays as it is, and the editor adopts the convention rather than the convention loosening around the editor — see [`research.md`](./research.md) R1). The convention documented in `docs/frontend/STYLING_STANDARDS.md` and the stylesheet's stated intent at lines 82-85 both stand. The AC's literal wording is superseded; rewording it in #37464's description is bookkeeping and changes nothing in this repository.

---

## Assumptions

- **#37465 lands first, or at least its branch is the base.** This spec is written against the post-#37465 code: the Date/Time footer as reference implementation, no timezone line under the input, and Block Editor as the tooltip's only caller. If #37465 is reverted, [Correction 2](#corrections-to-the-issue-descriptions) and the two withdrawn ACs must be revisited.
- **"Same base branch" means both PRs branch from this feature branch**, PR 2 stacks on PR 1, and PR 1 is reviewed and approved before PR 2 opens — per the repo's two-PR spec-gated flow.
- **`html { font-size: 14px }` still holds** (`libs/dotcms-scss/angular/styles.scss:9` — #37460's body cites `:38`, which is wrong; verified, along with the absence of any typography scale override in `libs/dotcms-scss/tailwind/theme.css`), and no typography scale override exists in `libs/dotcms-scss/tailwind/theme.css`. The accepted-deltas table is computed from that; if it changes, the table is recomputed, not the ACs.
- **"Save or publish attempt" means firing any workflow action** through `fireWorkflowAction`. There is no separate save button with different semantics.
- **Design approved these changes.** Confirmed by the developer. The original report that opened #37460 was Design's own, and they are on board with the resulting deltas, including the checkbox/radio option labels dropping to 12.25px (research R3), which #37460's measured-impact table does not list.
- **Design asked for this change; the label deltas are what it does.** #37460 was opened on Design's own report — *"the edit content screen is not taking the classes from the `.form > .field > label`"* — so the label deltas (14px→12.25px, weight 400→500, gap 7px→3.5px) are the requested outcome, not a side effect awaiting separate approval. **One delta is genuinely unreviewed**: the checkbox/radio *option* labels also drop to 12.25px (research R3), which #37460's measured-impact table does not list. Surface that one when PR 1 is up (task T005). Note that AC-112 forbids a local override to walk any of it back.
