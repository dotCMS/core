# Styling Standards

## Priority: Tailwind CSS + PrimeNG Theme

- **Use Tailwind utility classes** for layout, spacing, typography, colors, sizing, flexbox, and grid. Avoid custom SCSS when a Tailwind class exists.
- **Use PrimeNG components** instead of building custom UI (e.g. `p-button`, `p-card`, `p-dialog`, `p-table`; text inputs are the `pInputText` *directive* on a native `<input>`, not a component). PrimeNG theme tokens handle component styling automatically.
- **Minimize custom CSS** — component `.scss` files should be the exception, not the default. Most components should need zero or near-zero custom styles.

## Tailwind Usage

```html
<!-- ✅ Layout with Tailwind -->
<div class="flex items-center gap-4 p-4">
  <span class="font-semibold">Title</span>
  <p-button label="Save" />
</div>

<!-- ✅ Responsive grid -->
<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
  @for (item of items; track item.id) {
    <p-card [header]="item.title" />
  }
</div>

<!-- ❌ NEVER: custom CSS for what Tailwind handles -->
<div class="my-custom-flex-container">...</div>
```

### Rounding a bordered container

A rounded parent does **not** clip its children. A child with its own background — a card's tinted
header band — therefore paints into the rounded corners, and the symptom depends on its radius:

- **Child corner is square** (e.g. the header's *bottom* corners when it is the last child): the
  fill covers the corner arc and **erases the border stroke** for several pixels. This reads as a
  card whose border is broken or missing along part of the curve.
- **Child radius equals the parent's**: the border eats 1px of it, so the child curves more
  sharply than the space available and leaves a wedge of the page showing through.

```html
<!-- ✅ Preferred: the parent clips the fill to the border's inner curve, all four corners -->
<div class="flex flex-col overflow-hidden rounded-md border border-surface">
  <div class="bg-surface-50 px-5 py-4.5">...</div>
</div>

<!-- ✅ When you cannot clip: give the child the border's inner radius explicitly -->
<div class="flex flex-col rounded-md border border-surface">
  <div class="rounded-t-[calc(var(--radius-md)-1px)] bg-surface-50 px-5 py-4.5">...</div>
  <div class="border-t border-surface px-5 pt-4.5 pb-5">...</div>
</div>
```

`overflow-hidden` is the better fix — it is state-independent, so a header that is sometimes the
last child (a collapsed card) stays correct. Reach for the explicit inner radius **only** when the
container holds a PrimeNG overlay component (`p-select`, `p-datepicker`, `p-autocomplete`…): this
app runs with `overlayAppendTo: 'self'` (PrimeNG's default), those panels render inline, and an
`overflow-hidden` ancestor clips them. In that case the child must also round every corner it can
be adjacent to — an unrounded corner against the parent's arc is the erased-border case above.

Both cards in `dot-ai-config-detail` show the two branches, with the reason in a comment.

## When Custom SCSS Is Acceptable

Custom SCSS is only allowed for:
- **PrimeNG component overrides** (via `::ng-deep` inside `:host`)
- **Complex animations** or pseudo-element styles Tailwind cannot express
- **Third-party library integration** overrides

```scss
// ✅ PrimeNG override (scoped)
:host {
  ::ng-deep .p-datatable .p-datatable-header {
    border-radius: var(--border-radius);
  }
}

// ❌ NEVER: unscoped ::ng-deep
::ng-deep .p-button { color: red; }

// ❌ NEVER: custom SCSS for layout/spacing/colors
.my-container {
  display: flex;
  gap: 1rem;
  padding: 16px;
  color: #333;
}
```

## SCSS Variables (when custom styles are needed)

```scss
@use "variables" as *;

.feature-overlay {
    box-shadow: $shadow-m;
    border: 1px solid $color-palette-gray-200;
}
```

### Available Variables
- **Spacing**: `$spacing-0` through `$spacing-9`
- **Colors**: `$color-palette-primary`, `$color-palette-gray-*`, etc.
- **Shadows**: `$shadow-s`, `$shadow-m`, `$shadow-l`

## Rules

- **Tailwind first** — use utility classes for layout, spacing, colors, typography, sizing.
- **PrimeNG theme** — rely on the theme for component styling; avoid overriding PrimeNG styles unless necessary.
- **NEVER hardcode** colors, spacing, or shadows in SCSS — use SCSS variables or Tailwind classes.
- **`::ng-deep` must be scoped** inside `:host` — never bare.
- **BEM naming** (`Block__Element--Modifier`) only when custom SCSS is truly needed.
- **Flat SCSS structure** — no deeply nested selectors (max 3 levels).
- **No `!important`** unless justified with a comment.

## Tags vs Chips

PrimeNG ships two visually similar but semantically different components. Pick by intent, not appearance.

- **`p-tag`** — informative, read-only status display. Use it for anything that communicates state the user does not interact with directly: content status badges, locale labels, etc. Tags carry a `severity`, so colors come from native severity states (configured once in the `tag` block of the theme preset), never from per-template classes.
- **`p-chip`** — interactive or removable elements: filters, removable selections, anything the user can click or dismiss. Chips are mostly neutral gray and do not express severity.

### Decision rule

1. **Showing a contentlet status?** Use the shared **`<dot-contentlet-status-badge>`** component (`libs/ui`). It takes the `DotContentState` and resolves the label, severity, and translation internally — do not hand-roll a `p-tag` for contentlet statuses.

   ```html
   <dot-contentlet-status-badge [state]="contentlet" />
   ```

2. **Showing any other status / read-only state?** Use `p-tag` with the matching `severity` (see mapping below). Example: the version-history timeline shows *per-version* states — not a contentlet `DotContentState` — so it uses raw tags: this version is live → `success`, working copy → `warn`, experiment variant → `info`.
3. **Anything without a status** — interactive, removable, or clickable items (filters, selections) — use `p-chip`.

### Severity mapping convention

| Severity  | Status |
|-----------|--------|
| `success` | Published / live |
| `danger`  | Archived / deleted |
| `info`    | Revision / new |
| `warn`    | Draft |

### Rules

- **Always use `<dot-contentlet-status-badge>` for contentlet statuses** — never a raw `p-tag` or `p-chip`.
- **Locale/language labels use `p-tag severity="info"`** — locales are informative, never chips (e.g. the Locale column in Content Drive, the asset card language label). This applies to read-only locale *display*; interactive locale *selectors* are designed per area.
- **Never use `p-chip` for purely informational status** — use `p-tag` with a `severity`.
- **Never add Tailwind `!important` color overrides** (`bg-green-100!`, `text-red-700!`, etc.) to PrimeNG components. Rely on native `severity` plus the preset color tokens in `theme.config.ts`.

## Form Fields

**The global `.form` / `.field` classes in `apps/dotcms-ui/src/style.css` are the convention.**
Use them. They carry label typography, the label-to-control gap, and hint/error colors, so a form
that adopts them needs no local CSS for any of it.

> This reverses earlier guidance here, which called them legacy and told you not to extend them.
> `edit-content` was the one surface that had never adopted them — its labels rendered larger and
> lighter than every other admin form, and its hints and errors rendered unstyled, because the
> editor's root `<form>` carried no `.form` class and every rule below it is scoped to one
> (#37460). It now uses them like the rest of the product.

`edit-content` still wraps fields in `dot-card-field` / `dot-card-field-label`, but those now
*emit* the global markup rather than reimplementing it — see
[Reaching a direct-child rule from a component](#reaching-a-direct-child-rule-from-a-component).

These rules apply to every field:

- **No `text-*` size class on the label of a form control.** A control's label inherits the
  PrimeNG default size/weight — do not add `text-sm`, `text-base`, `font-medium`, etc. to make it
  "look right." The **only** text allowed `text-sm` is hint/error text under a field, and it pairs
  with a muted/semantic color (`text-gray-500` for hints, `text-red-500` for errors).

  This rule is about control labels. Section headings, group labels over a set of controls, and
  card/section descriptions are a different typographic level and may size themselves
  (`text-sm font-semibold` for a group label, `text-sm text-gray-600` for secondary copy).
- **Use one gray scale.** `text-gray-*` and `text-surface-*` are separate scales
  (`--color-gray-*` maps to `--p-gray-*`, not to surface). `text-gray-*` is the app's majority
  convention — don't mix the two in one screen.
- **Do not write the label-to-control gap yourself.** `.form .field` is `flex flex-col gap-1`;
  adding your own `gap-*` to a `.field` overrides the system for no reason. Outside a `.form` — a
  dialog appended to `body`, a bundle that does not load the app stylesheet — `flex flex-col gap-1`
  is the equivalent to hand-roll.
- **A field reserves space for its hint only when it has one.** Render the hint conditionally
  (`@if (field.hint) { <small class="text-sm text-gray-500">...</small> }`) — never a permanent
  empty slot or a fixed `min-h-*` "for alignment" when there is no hint to show.
- **Required marker.** For a form whose required fields are fixed, use the `dotFieldRequired`
  directive (see below). For a form built from runtime metadata, use a literal `*` shown
  `@if (field.required)` and styled `text-red-500` — the directive cannot track a field whose
  requiredness changes after the first render.
- **Prefer PrimeNG components over hand-rolled equivalents**: `p-panel`/`p-accordion` for
  collapsible sections, `p-toggleswitch` for on/off toggles, `p-button` for actions. For a card
  container, check what each one actually gives you before hand-rolling: `p-panel` is a border +
  radius + a `surface.50` header band with its own divider, while `p-card` is overridden in
  `theme.config.ts` to a plain bordered box (`shadow: none`, `1rem` padding) with **no** header
  band — they are not interchangeable.
- **A custom icon in a `p-button` goes in an `<ng-template #icon>`, not as bare content.**
  `p-button-icon-only` — the square, label-less shape — is derived from the `icon` input or an
  `#icon` template; bare projected content satisfies neither, so an icon-only button silently
  renders with the padding of a labelled one.

```html
<!-- ✅ Field: the global system supplies typography, gap and hint color -->
<form class="form" [formGroup]="form">
  <div class="field">
    <label for="name" dotFieldRequired>Name</label>
    <input pInputText id="name" formControlName="name" class="w-full" />
    @if (hint) {
      <small class="p-field-hint">{{ hint }}</small>
    }
  </div>
</form>

<!-- ❌ NEVER: sized/weighted label, hand-rolled gap, permanently reserved hint slot -->
<div class="flex flex-col gap-2">
  <label for="name" class="text-sm font-medium">Name</label>
  <input pInputText id="name" formControlName="name" />
  <small class="text-sm text-gray-500 min-h-5">{{ hint }}</small>
</div>
```

### What the global classes actually define

Two of these class names silently do nothing outside their scope, which is worth knowing before
you copy a template that uses them.

`apps/dotcms-ui/src/style.css` defines:

| Class | What it does | Scope |
|-------|--------------|-------|
| `.form` | `w-full`, 5-unit rhythm between direct children | the form element |
| `.form .field` | column layout, `gap-1` between label and control | one field |
| `.form .field > label` | `text-sm font-medium` | labels, automatically |
| `.form .p-field-hint` | `text-sm text-gray-500` | a field's hint |
| `.form .p-field-error` | `text-sm text-red-500` | a field's error |

Three things to know before you copy a template that uses them:

- **`p-field-hint` and `p-field-error` only work inside `.form`.** Both rules are
  descendant-scoped. On an element with no `.form` ancestor the class matches nothing and the text
  renders at inherited size and color — no error, no warning, just wrong. Outside a `.form`, write
  the utilities directly: `<small class="text-sm text-gray-500">`.
- **The `p-` on those two is ours, not PrimeNG's**, as it is on `p-label-input-required`. PrimeNG
  21 claims neither name. Not every `p-` class in a template comes from the library.
- **`.p-error` is dead — never add it, and replace it when you touch a file that has it.** It is
  defined only in `libs/dotcms-scss/angular/dotcms-theme/utils/_validation.scss`, reachable only
  through `dotcms-theme/theme.scss`, and that import is **commented out** in
  `libs/dotcms-scss/angular/styles.scss`. Nothing else the app builds defines it, so an element
  carrying it renders as plain inherited text rather than red. There are live usages in
  `dot-experiments` and `dot-content-types-edit` today whose validation errors do not look like
  errors.

For a field-level error use `<p-message severity="error" variant="simple">` — the `simple` variant
renders as colored text with no box — or `<small class="text-sm text-red-500">`. A message that is
*not* about a field (a dialog reporting a failed action, a page-wide warning) is a full `p-message`
with a `severity`, not a field error borrowed for the occasion.

### The `dotFieldRequired` directive

`libs/ui/src/lib/dot-field-required/dot-field-required.directive.ts` puts the required asterisk on
a label without markup. Four usages:

```html
<!-- 1. Always required -->
<label dotFieldRequired for="name">Name</label>

<!-- 2. Required only if the named control carries Validators.required -->
<label dotFieldRequired checkIsRequiredControl="name" for="name">Name</label>

<!-- 3. Signal forms: follows the field's own required() -->
<label [dotFieldRequired]="field" for="name">Name</label>

<!-- 4. Boolean: for a caller that already knows, from content type metadata rather than a form -->
<label [dotFieldRequired]="isRequired" for="name">Name</label>
```

Mode 4 exists because a component cannot apply a directive to its own host from its template. A
component whose host *is* the label declares it through `hostDirectives` and forwards the flag:

```ts
hostDirectives: [{ directive: DotFieldRequiredDirective, inputs: ['dotFieldRequired: isRequired'] }]
```

It adds the class `p-label-input-required`, whose `::after { content: '*' }` rule lives at the top
level of `apps/dotcms-ui/src/style.css` — **not** scoped to `.form`, so unlike `p-field-hint` it
works anywhere.

**Modes 1 and 2 are for a form whose required fields are fixed.** Do not use *mode 2* for a form
built from runtime metadata — use mode 3 or 4, which recompute. The reasons are visible in the
source:

- **It is a one-way latch.** The constructor adds the class unconditionally; `checkIsRequiredControl`
  is a plain setter that only ever *removes* it. Once removed it never comes back, and the setter
  re-runs only when the bound string changes — so a control that becomes required again keeps no
  asterisk.
- **It only recognises `Validators.required` by reference** (`hasValidator(Validators.required)`).
  A custom or cross-field validator reads as "not required".
- **It requires a `FormGroupDirective` ancestor** (`inject(FormGroupDirective)`, not optional), so
  it throws outside a reactive form.

`dot-ai-config-detail` is the counter-example: its fields come from provider metadata and the
`FormGroup` is rebuilt on every provider switch, while `@for (… track field.name)` reuses the DOM
node. The same field name can change requiredness between providers — `model` is
`ProviderField.required` for Vertex AI and `ProviderField.optionalUnless` for Azure — which the
latch above would render stale. That screen predates mode 4 and uses a literal `*` bound to the
metadata flag; mode 4 now covers the same case through the directive.

### Reaching a direct-child rule from a component

`.form .field > label` is a **direct-child** selector. A component that renders its own `<label>`
inside its element makes that label a *grandchild* of `.field`, and the rule never matches — the
label keeps the inherited size and weight.

**`display: contents` does not fix this.** `display` governs box generation; selectors match the
**DOM tree**, which `display` never changes. It collapses the layout correctly and leaves the
typography untouched, so the gap looks right while the rule still does not apply — a failure that
looks like success. Verify in devtools that the rule appears as **matched**, not merely that the
spacing looks correct.

The fix is to remove the intermediate element: give the component an **attribute selector on the
element it is meant to be**, so its host *is* the label.

```ts
// ❌ renders <dot-card-field-label><label>…</label></dot-card-field-label>
@Component({ selector: 'dot-card-field-label', template: '<label …><ng-content /></label>' })

// ✅ the host IS the label; nothing sits between it and .field
@Component({
    selector: 'label[dotCardFieldLabel]',
    template: '<ng-content />',
    host: { '[attr.for]': '$variableName()' }
})
```

```html
<label dotCardFieldLabel [variableName]="field.variable">{{ field.name }}</label>
```

This applies to any component that must satisfy a global rule written with `>`. Wrapper components
whose element no global rule targets — `dot-card-field-content`, `dot-card-field-footer` — need no
such treatment.

### Naming a field for assistive technology

The required asterisk is `::after` content and is deliberately **not** in the label's accessible
name, so a screen reader never announces "star". That means the control has to carry the
information itself — and before it can, it has to *have* a name. Three shapes, by widget:

| Widget | Name it with | Required |
|---|---|---|
| A labelable control (`input`, `textarea`, `select`, `button`) | `<label for>` + matching `id` | `aria-required` on the control |
| A group or collection (`div` of options, a chips list, a dropzone) | `role` + `aria-labelledby` → the label's `id` | only where the role allows it |
| A third-party editor that owns its DOM | the editor's own option | — |

- **`<label for>` only associates with *labelable* elements.** A `<div role="group">` can never be
  named that way however many roles it carries, and neither can PrimeNG's select, which puts
  `inputId` on a `<span role="combobox">`. Those take `aria-labelledby` — PrimeNG exposes
  `ariaLabelledBy` for exactly this.
- **Verify with `label.control`, not by checking an id exists.** The browser's own resolution is
  the only thing that proves the association; an id that is present but on a non-labelable element
  looks correct and associates nothing.
- **ARIA defines `aria-required` on `combobox`, `listbox`, `radiogroup`, `spinbutton`, `textbox`
  and `tree` — not on plain `group`,** and there is no `checkboxgroup` role. For a widget that
  takes `group`, leave it off rather than write invalid ARIA; the asterisk and the required message
  carry it visually.
- **Declare the role a widget already behaves as, do not invent one.** A button that carries
  `aria-expanded` and opens a listbox-like overlay *is* a combobox; saying so makes `aria-required`
  available to it.
- **Never infer the control from "the first focusable descendant".** Text Area and WYSIWYG both
  render an editor-mode dropdown above their real control, so that heuristic marks the dropdown as
  the mandatory field. A screen reader announcing the wrong control is worse than announcing none.
  Mark only what the label declares.
- **Third-party editors** take their own option: Monaco `ariaLabel`, TinyMCE `iframe_aria_text`
  (the body inside the frame) plus `iframe_attrs.title` (the frame itself), the block editor a
  field-derived label in place of its generic one.

## See also
- [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md) — Component rules, templates
- [docs/frontend/README.md](./README.md) — Index of all frontend docs
