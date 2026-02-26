# Styling Standards

## Priority: Tailwind CSS + PrimeNG Theme

- **Use Tailwind utility classes** for layout, spacing, typography, colors, sizing, flexbox, and grid. Avoid custom SCSS when a Tailwind class exists.
- **Use PrimeNG components** instead of building custom UI (e.g. `p-button`, `p-inputText`, `p-card`, `p-dialog`, `p-table`). PrimeNG theme tokens handle component styling automatically.
- **Minimize custom CSS** — component `.scss` files should be the exception, not the default. Most components should need zero or near-zero custom styles.
- **PrimeFlex is deprecated and uninstalled** — do NOT use PrimeFlex classes (`flex`, `grid`, `col-*`, `p-m-*`, `gap-*`, `align-items-*`, `justify-content-*`). Replace with Tailwind equivalents.

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

<!-- ❌ NEVER: PrimeFlex (deprecated) -->
<div class="flex align-items-center gap-3 p-3">...</div>

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
- **No PrimeFlex** — it is deprecated and uninstalled. Replace any remaining usage with Tailwind.
- **NEVER hardcode** colors, spacing, or shadows in SCSS — use SCSS variables or Tailwind classes.
- **`::ng-deep` must be scoped** inside `:host` — never bare.
- **BEM naming** (`Block__Element--Modifier`) only when custom SCSS is truly needed.
- **Flat SCSS structure** — no deeply nested selectors (max 3 levels).
- **No `!important`** unless justified with a comment.

## PrimeFlex → Tailwind Migration Reference

| PrimeFlex (deprecated) | Tailwind |
|---|---|
| `flex` | `flex` |
| `align-items-center` | `items-center` |
| `justify-content-between` | `justify-between` |
| `gap-3` | `gap-3` |
| `p-3` (padding) | `p-3` |
| `m-2` (margin) | `m-2` |
| `col-6` | `w-1/2` or `grid grid-cols-2` |
| `text-center` | `text-center` |
| `font-bold` | `font-bold` |
| `w-full` | `w-full` |
| `hidden` | `hidden` |
| `grid` | `grid` |
| `flex-column` | `flex-col` |
| `flex-wrap` | `flex-wrap` |

## See also
- [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md) — Component rules, templates
- [docs/frontend/README.md](./README.md) — Index of all frontend docs
