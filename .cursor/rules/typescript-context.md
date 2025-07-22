---
description: TypeScript/Angular-specific context for dotCMS frontend
globs: ["core-web/**/*.ts", "core-web/**/*.html", "core-web/**/*.scss"]
alwaysApply: false
---

# Angular/TypeScript Context

**Quick Navigation:**
- Tech Stack: Angular 18.2.3, PrimeNG 17.18.11, NgRx Signals, Jest + Spectator
- File Structure: Separate .ts/.html/.scss files required
- Testing: ALWAYS use `data-testid` and `spectator.setInput()`

**Critical Reminders:**
- Use `@if/@for/@switch` instead of `*ngIf/*ngFor/*ngSwitch`
- Use `input()` and `output()` functions instead of decorators
- Use `ChangeDetectionStrategy.OnPush` for all components
- Import variables with `@use "variables" as *;` in SCSS
- BEM methodology with flat structure (no nesting)

**Comprehensive Documentation:**
- [Angular Standards](../docs/frontend/ANGULAR_STANDARDS.md)
- [Testing Frontend](../docs/frontend/TESTING_FRONTEND.md)
- [Styling Standards](../docs/frontend/STYLING_STANDARDS.md)
- [Component Architecture](../docs/frontend/COMPONENT_ARCHITECTURE.md)