# Styling Standards

## Priority: PrimeFlex & PrimeNG First

- **Prefer PrimeFlex utility classes** for layout, spacing, typography, and colors. Avoid creating custom SCSS when a utility exists (e.g. `p-flex`, `p-m-3`, `p-text-primary`, `p-shadow-2`).
- **Use PrimeNG components** in most cases instead of building custom UI from scratch (e.g. `p-button`, `p-inputText`, `p-card`, `p-dialog`, `p-table`). Custom styles should be the exception, not the default.
- When you do need custom styles, follow BEM and the rules below.

## BEM Methodology (when custom styles are needed)
```scss
// ALWAYS import variables first
@use "variables" as *;

// Use global variables, NEVER hardcoded values
.feature-list {
    padding: $spacing-3;
    color: $color-palette-primary;
    background: $color-palette-gray-100;
    box-shadow: $shadow-m;
}

// BEM with flat structure (no nesting)
.feature-list { }
.feature-list__header { }
.feature-list__item { }
.feature-list__item--active { }
```

## Required Variables
- **Spacing**: `$spacing-1` through `$spacing-5`
- **Colors**: `$color-palette-primary`, `$color-palette-gray-100`
- **Shadows**: `$shadow-s`, `$shadow-m`, `$shadow-l`

## Rules
- **Prefer PrimeFlex utilities and PrimeNG components** over custom SCSS; use BEM only when utilities/components are insufficient.
- **NEVER hardcode**: colors, spacing, shadows (use variables or PrimeFlex tokens).
- **BEM naming**: Block__Element--Modifier (for custom blocks only).
- **Flat structure**: No nested SCSS selectors.
- **Component scoping**: Use component-specific classes when writing custom styles.

## See also
- [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md) — Component rules, templates
- [docs/frontend/README.md](./README.md) — Index of all frontend docs