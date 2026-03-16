---
paths:
  - "core-web/**/*.ts"
  - "core-web/**/*.tsx"
  - "core-web/**/*.html"
  - "core-web/**/*.scss"
  - "core-web/**/*.css"
---

# Frontend Context (core-web)

**Nx monorepo** in `core-web/`: TypeScript, Angular apps/libs, and SDK for **Angular** and **React**. Full standards live in **`docs/frontend/`**. See `core-web/AGENTS.md` for commands, conventions, and structure.

## Stack
- **Angular**: standalone, signals, `inject()`, `input()`/`output()`, `@if`/`@for`, OnPush, PrimeNG/PrimeFlex. Check `core-web/package.json` for current version.
- **SDK**: `sdk-angular`, `sdk-react`, `sdk-client`, `sdk-types`, etc.

## Quick reminders (details in docs)
- **Angular**: `$` prefix for signals, `$` suffix for observables; no `standalone: true`; OnPush; `[class.x]` / `[style.x]` (no ngClass/ngStyle); host bindings in `host: {}`.
- **Testing**: Spectator + Jest; `data-testid` on elements; `byTestId()`, `spectator.setInput()`, `mockProvider()`; never set component inputs directly. Use `@dotcms/utils-testing` createFake functions.
- **TypeScript**: Strict types, no `any` (use `unknown`), `as const` instead of enums, `#` for private.
- **Services**: Single responsibility; `providedIn: 'root'`; use `inject()` instead of constructor injection.

## On-demand
- `docs/frontend/ANGULAR_STANDARDS.md`
- `docs/frontend/COMPONENT_ARCHITECTURE.md`
- `docs/frontend/TESTING_FRONTEND.md`
- `docs/frontend/STATE_MANAGEMENT.md`
- `docs/frontend/TYPESCRIPT_STANDARDS.md`
- `docs/frontend/STYLING_STANDARDS.md`
