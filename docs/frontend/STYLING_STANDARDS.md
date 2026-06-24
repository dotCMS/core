# Styling Standards

## Priority: Tailwind CSS + PrimeNG Theme

- **Use Tailwind utility classes** for layout, spacing, typography, colors, sizing, flexbox, and grid. Avoid custom SCSS when a Tailwind class exists.
- **Use PrimeNG components** instead of building custom UI (e.g. `p-button`, `p-inputText`, `p-card`, `p-dialog`, `p-table`). PrimeNG theme tokens handle component styling automatically.
- **Minimize custom CSS** — component `.scss` files should be the exception, not the default. Most components should need zero or near-zero custom styles.

## Tailwind Usage

```html
<!-- ✅ Layout with Tailwind -->
<div class="flex items-center gap-4 p-4">
  <span class="text-sm font-semibold text-color">Title</span>
  <p-button label="Save" />
</div>

<!-- ✅ Responsive grid -->
<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
  <p-card *ngFor="..." />
</div>

<!-- ❌ NEVER: custom CSS for what Tailwind handles -->
<div class="my-custom-flex-container">...</div>
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

## See also
- [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md) — Component rules, templates
- [docs/frontend/README.md](./README.md) — Index of all frontend docs
