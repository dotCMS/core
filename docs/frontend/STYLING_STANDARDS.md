# Styling Standards

## BEM Methodology (Required)
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
- **NEVER hardcode**: colors, spacing, shadows
- **BEM naming**: Block__Element--Modifier
- **Flat structure**: No nested SCSS selectors
- **Component scoping**: Use component-specific classes