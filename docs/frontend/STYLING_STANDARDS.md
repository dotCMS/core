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

## Tailwind Tooling Stack

Three complementary layers enforce Tailwind quality — each serves a different purpose and they do not overlap.

| Layer | Tool | Where | What it enforces |
|-------|------|--------|-----------------|
| Class order | [`prettier-plugin-tailwindcss`](https://github.com/tailwindlabs/prettier-plugin-tailwindcss) (Tailwind Labs official) | CI (`nx format:check`) + pre-commit | Canonical Tailwind sort order in HTML/TS |
| Correctness & style | [`eslint-plugin-better-tailwindcss`](https://github.com/schoero/eslint-plugin-better-tailwindcss) | CI (`nx affected -t lint`) + ESLint IDE extension | Invalid classes, conflicts, deprecated usage, shorthand equivalents |
| IDE feedback | [`tailwindcss-intellisense`](https://github.com/tailwindlabs/tailwindcss-intellisense) (`bradlc.vscode-tailwindcss`) | VS Code only | Autocomplete, hover previews, real-time validation |

**Tailwind Labs does not publish a standalone CI linter.** Their official recommendation is `prettier-plugin-tailwindcss` for class ordering (CI + editor) and `tailwindcss-intellisense` for the IDE. The ESLint plugin fills the gap for correctness rules that Prettier cannot enforce.

### Why not `eslint-plugin-tailwindcss`?

[`eslint-plugin-tailwindcss`](https://github.com/francoismassart/eslint-plugin-tailwindcss) was built around `tailwind.config.js` (v3 and earlier). With Tailwind v4's CSS-first configuration (`@import "tailwindcss"` in `style.css`), it lacks native v4 support and is not actively maintained for v4. `eslint-plugin-better-tailwindcss` supports v4 entry-point configuration.

### Active ESLint rules (`.eslintrc.base.json`, `*.html` override)

```jsonc
// Error — have autofix; will be corrected by `lint --fix` or pre-commit
"better-tailwindcss/no-duplicate-classes": "error",
"better-tailwindcss/no-unnecessary-whitespace": "error",
"better-tailwindcss/no-deprecated-classes": "error",       // e.g. "rounded" → "rounded-sm"
"better-tailwindcss/enforce-canonical-classes": "error",    // e.g. "w-12 h-12" → "size-12"
"better-tailwindcss/enforce-consistent-variable-syntax": "error",
"better-tailwindcss/enforce-consistent-important-position": "error",

// Warning — no autofix; kept as warn due to false positives from BEM/PrimeNG classes
"better-tailwindcss/no-unknown-classes": "warn",
"better-tailwindcss/no-conflicting-classes": "warn",

// Disabled — Prettier handles class order authoritatively
"better-tailwindcss/enforce-consistent-class-order": "off",
"better-tailwindcss/enforce-consistent-line-wrapping": "off",
```

### Known false positives for `no-unknown-classes`

The `no-unknown-classes` rule emits warnings for CSS classes that are not registered with Tailwind — this is expected in this codebase because templates use a mix of:

- BEM component classes (`dot-apps-configuration__container`, `dot-nav__list-item--active`, etc.)
- PrimeNG icon classes (`pi`, `pi-exclamation-triangle`, etc.)
- PrimeNG utility classes (`p-button-outlined`, `p-fluid`, `p-field-hint`)
- Component-scoped helper classes (`form`, `field`, `collapsed`)

These warnings are informational and **do not block CI**. ESLint exits 0 on warnings. Do not suppress them with `eslint-disable` — let them serve as a reminder to prefer Tailwind utilities over custom classes. The rule remains useful for catching genuine typos in Tailwind class names.

When the codebase has migrated away from legacy BEM classes, `no-unknown-classes` and `no-conflicting-classes` can be promoted to `"error"`.

### Prettier configuration (`core-web/.prettierrc`)

```json
{
  "plugins": ["prettier-plugin-tailwindcss"],
  "tailwindStylesheet": "./apps/dotcms-ui/src/style.css"
}
```

Prettier sorts Tailwind classes in the same order the compiler emits CSS, resolving all ordering debates automatically.

### VS Code setup (`.vscode/extensions.json` + `.vscode/settings.json`)

Recommended extension `bradlc.vscode-tailwindcss` is listed in [`core-web/.vscode/extensions.json`](../../core-web/.vscode/extensions.json). The Tailwind language server is pointed at the CSS entry file:

```json
{
  "tailwindCSS.experimental.configFile": "apps/dotcms-ui/src/style.css",
  "files.associations": { "*.css": "tailwindcss" },
  "editor.quickSuggestions": { "strings": "on" }
}
```

## See also
- [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md) — Component rules, templates
- [docs/frontend/README.md](./README.md) — Index of all frontend docs
