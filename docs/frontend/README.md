# Frontend documentation (`docs/frontend/`)

**This is the entry point for Angular/TypeScript frontend standards in `core-web`.** Start here to find the right doc, then load that doc directly — you do not need to read the index first if you already know which one you want.

`ANGULAR_STANDARDS.md` is the **single source of truth** for Angular rules. Where any other file here disagrees with it, it is the one that is wrong.

**Loading a doc**: Cursor uses `.cursor/rules/frontend-context.mdc`, which points here — reference `@docs/frontend/README.md` for this index, or `@docs/frontend/<file>` to load one doc. Claude Code agents working in `core-web/` get the dotCMS rules through the `dot-ui-angular-standards` skill, which defers to `ANGULAR_STANDARDS.md`.

## Documents

| Doc | When to load |
|-----|--------------|
| [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md) | **Start here.** Components, templates, signals, change detection, reactive forms, icons, reuse, state and error handling, build commands |
| [COMPONENT_ARCHITECTURE.md](./COMPONENT_ARCHITECTURE.md) | Component structure, file layout, data flow, parent-child communication |
| [STATE_MANAGEMENT.md](./STATE_MANAGEMENT.md) | NgRx Signal Store, `rxMethod`, `patchState` — **prefer over manual state** |
| [STYLING_STANDARDS.md](./STYLING_STANDARDS.md) | Tailwind CSS, PrimeNG theme, BEM (when needed), SCSS variables — **and form markup**: the global `.form` / `.field` system, labels, hints and errors, `dotFieldRequired`, naming a field for assistive technology |
| [TYPESCRIPT_STANDARDS.md](./TYPESCRIPT_STANDARDS.md) | Strict types, inference, `unknown`, `as const`, `#` private |
| [TESTING_FRONTEND.md](./TESTING_FRONTEND.md) | Writing tests: Spectator, Vitest, `byTestId`, `setInput`, `data-testid` |
| [TESTING_REVIEW_RULES.md](./TESTING_REVIEW_RULES.md) | **Reviewing** test files: condensed violation checklist, severity-ranked |
| [TESTING_PERFORMANCE.md](./TESTING_PERFORMANCE.md) | Suite **run time**: `pnpm test:profile`, the five-phase breakdown, which Vitest options are measured regressions, `isolate` policy |
| [BREADCRUMBS.md](./BREADCRUMBS.md) | GlobalStore breadcrumbs: `addNewBreadcrumb`, `setBreadcrumbs`, `id`/`url` for tabs, duplicate prevention |
| [KEYBOARD_SHORTCUTS.md](./KEYBOARD_SHORTCUTS.md) | Shortcut registry: per-combination last-in-wins, bubble phase, adding one, selection semantics, event-synthesis testing hazards |
| [PNPM_GLOBAL_STORE.md](./PNPM_GLOBAL_STORE.md) | Optional per-developer store layout for multiple worktrees: ~2.1 GB → ~2.2 MB per checkout, the trust-boundary limit, why undeclared imports break and how to correct them |

## Picking between the two testing docs

- **Writing or fixing a test** → `TESTING_FRONTEND.md` (full patterns and examples)
- **Reviewing someone else's test** → `TESTING_REVIEW_RULES.md` (condensed pass/fail rules)
- **Making the suite faster** → `TESTING_PERFORMANCE.md` (measure first; several obvious knobs are measured regressions)

## Cross-cutting conventions

These hold across every doc here — if you see a violation, it is a defect regardless of which file you were reading:

- **Signals**: `$` prefix (`$loading`); **observables**: `$` suffix (`vm$`) — ANGULAR_STANDARDS, COMPONENT_ARCHITECTURE, TESTING_FRONTEND
- **Change detection**: `OnPush` is the Angular v22 default — never set `changeDetection` on a new component; leave existing `Eager` components alone — ANGULAR_STANDARDS
- **Components**: three separate files (`.ts` / `.html` / `.scss`); reuse `libs/ui` then PrimeNG before creating a new one — ANGULAR_STANDARDS, COMPONENT_ARCHITECTURE
- **State**: NgRx Signal Store for feature state; avoid manual signal soup — STATE_MANAGEMENT, COMPONENT_ARCHITECTURE
- **Testing**: Vitest + `@openng/spectator`, `byTestId`, `setInput` — TESTING_FRONTEND, TESTING_REVIEW_RULES, ANGULAR_STANDARDS
- **TypeScript**: strict, no `any`, `as const`, `#` private — TYPESCRIPT_STANDARDS
- **Commands**: `pnpm nx …` — Nx is not installed globally and the package manager is pnpm — ANGULAR_STANDARDS
- **Form markup**: the root `<form>` carries `class="form"`; field layout, label typography and hint/error colour come from the global system, never from local utilities — STYLING_STANDARDS

## Where forms are split between two docs

`ANGULAR_STANDARDS.md` owns the **TypeScript** side of forms — `FormBuilder`, typed controls, validation signals. It says nothing about markup. Everything you can see — the `.form` / `.field` structure, label typography, where a hint or an error renders, the required asterisk, how a field is named for a screen reader — is in `STYLING_STANDARDS.md` under **Form Fields**. Going to the "single source of truth" for a form question and finding no answer is the expected outcome, not a gap: load the other one.

## Versions

Do not hardcode versions in these docs. `core-web/package.json` is the source of truth; express versions as major-only (for example "Angular 22.x") when they must be mentioned at all.
