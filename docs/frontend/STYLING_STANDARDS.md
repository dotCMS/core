# Styling Standards

## Priority: Tailwind CSS + PrimeNG Theme

- **Use Tailwind utility classes** for layout, spacing, typography, colors, sizing, flexbox, and grid. Avoid custom SCSS when a Tailwind class exists.
- **Use PrimeNG components** instead of building custom UI (e.g. `p-button`, `p-inputText`, `p-card`, `p-dialog`, `p-table`). PrimeNG theme tokens handle component styling automatically.
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

<!-- ❌ NEVER: hand-rolled "card" — this is p-card reinvented -->
<div class="flex flex-col rounded-md border border-surface">
  <div class="flex items-start gap-3 rounded-t-md bg-surface-50 px-5 py-4.5">...</div>
  <div class="border-t border-surface px-5 pt-4.5 pb-5">...</div>
</div>
```

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

The codebase currently has **more than one field-markup convention** (e.g. the global `.form`/
`.field` classes in `apps/dotcms-ui/src/style.css`, and `edit-content`'s own
`dot-card-field`/`dot-card-field-label` components). They do not render identically — before
writing a new form, check how the most relevant *existing* surface (usually `edit-content`, the
most actively maintained field UI) actually looks, don't just grep for a `.form`/`.field` example
and copy it. Do not extend the global `.form`/`.field` classes to new features.

Regardless of which markup you use, these rules apply to every field:

- **No `text-*` size class on body copy or labels.** Labels and normal UI text inherit the
  PrimeNG default size/weight — do not add `text-sm`, `text-base`, `font-medium`, etc. to make a
  label "look right." The **only** text allowed `text-sm` is hint/error text under a field, and it
  pairs with a muted/semantic color (`text-gray-500` for hints, `text-red-500` for errors).
- **Label-to-control gap is `gap-2`.** A field wrapper is `flex flex-col gap-2` — label, then
  control, nothing wider.
- **A field reserves space for its hint only when it has one.** Render the hint conditionally
  (`@if (field.hint) { <small class="text-sm text-gray-500">...</small> }`) — never a permanent
  empty slot or a fixed `min-h-*` "for alignment" when there is no hint to show.
- **Required marker is a literal `*`**, shown only `@if (field.required)`, styled `text-red-500`,
  placed next to the label. Don't introduce a second, CSS-class-driven required mechanism for new
  forms unless you are already extending a surface that uses one.
- **Prefer PrimeNG components over hand-rolled equivalents**: `p-card` (not a bordered div with a
  hand-built header bar), `p-panel`/`p-accordion` for collapsible sections, `p-toggleswitch` for
  on/off toggles, `p-button` for actions. See the anti-pattern example above.

```html
<!-- ✅ Field -->
<div class="flex flex-col gap-2">
  <label for="name">Name</label>
  <input pInputText id="name" [formControlName]="'name'" />
  @if (hint) {
    <small class="text-sm text-gray-500">{{ hint }}</small>
  }
</div>

<!-- ❌ NEVER: sized/weighted label, permanently reserved hint slot -->
<div class="flex flex-col gap-1">
  <label for="name" class="text-sm font-medium">Name</label>
  <input pInputText id="name" [formControlName]="'name'" />
  <small class="text-sm text-gray-500 min-h-5">{{ hint }}</small>
</div>
```

## See also
- [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md) — Component rules, templates
- [docs/frontend/README.md](./README.md) — Index of all frontend docs
