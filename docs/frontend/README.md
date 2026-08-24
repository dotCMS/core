# Frontend documentation (`docs/frontend/`)

**This is the entry point for Angular/TypeScript frontend standards in `core-web`.** Start here to find the right doc, then load that doc directly — you do not need to read the index first if you already know which one you want.

`ANGULAR_STANDARDS.md` is the **single source of truth** for Angular rules. Where any other file here disagrees with it, it is the one that is wrong.

**Loading a doc**: Cursor uses `.cursor/rules/frontend-context.mdc`, which points here — reference `@docs/frontend/README.md` for this index, or `@docs/frontend/<file>` to load one doc. Claude Code agents working in `core-web/` get the dotCMS rules through the `dot-ui-angular-standards` skill, which defers to `ANGULAR_STANDARDS.md`.

## Documents

| Doc | When to load |
|-----|--------------|
| [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md) | **Start here.** Components, templates, signals, change detection, forms, icons, reuse, state and error handling, build commands |
| [COMPONENT_ARCHITECTURE.md](./COMPONENT_ARCHITECTURE.md) | Component structure, file layout, data flow, parent-child communication |
| [STATE_MANAGEMENT.md](./STATE_MANAGEMENT.md) | NgRx Signal Store, `rxMethod`, `patchState` — **prefer over manual state** |
| [STYLING_STANDARDS.md](./STYLING_STANDARDS.md) | Tailwind CSS, PrimeNG theme, BEM (when needed), SCSS variables |
| [TYPESCRIPT_STANDARDS.md](./TYPESCRIPT_STANDARDS.md) | Strict types, inference, `unknown`, `as const`, `#` private |
| [TESTING_FRONTEND.md](./TESTING_FRONTEND.md) | Writing tests: Spectator, Jest, `byTestId`, `setInput`, `data-testid` |
| [TESTING_REVIEW_RULES.md](./TESTING_REVIEW_RULES.md) | **Reviewing** test files: condensed violation checklist, severity-ranked |
| [BREADCRUMBS.md](./BREADCRUMBS.md) | GlobalStore breadcrumbs: `addNewBreadcrumb`, `setBreadcrumbs`, `id`/`url` for tabs, duplicate prevention |

## Picking between the two testing docs

- **Writing or fixing a test** → `TESTING_FRONTEND.md` (full patterns and examples)
- **Reviewing someone else's test** → `TESTING_REVIEW_RULES.md` (condensed pass/fail rules)

## Cross-cutting conventions

These hold across every doc here — if you see a violation, it is a defect regardless of which file you were reading:

- **Signals**: `$` prefix (`$loading`); **observables**: `$` suffix (`vm$`) — ANGULAR_STANDARDS, COMPONENT_ARCHITECTURE, TESTING_FRONTEND
- **Change detection**: `OnPush` is the Angular v22 default — never set `changeDetection` on a new component; leave existing `Eager` components alone — ANGULAR_STANDARDS
- **Components**: three separate files (`.ts` / `.html` / `.scss`); reuse `libs/ui` then PrimeNG before creating a new one — ANGULAR_STANDARDS, COMPONENT_ARCHITECTURE
- **State**: NgRx Signal Store for feature state; avoid manual signal soup — STATE_MANAGEMENT, COMPONENT_ARCHITECTURE
- **Testing**: Jest + `@openng/spectator`, `byTestId`, `setInput` — TESTING_FRONTEND, TESTING_REVIEW_RULES, ANGULAR_STANDARDS
- **TypeScript**: strict, no `any`, `as const`, `#` private — TYPESCRIPT_STANDARDS
- **Commands**: `pnpm nx …` — Nx is not installed globally and the package manager is pnpm — ANGULAR_STANDARDS

## Versions

Do not hardcode versions in these docs. `core-web/package.json` is the source of truth; express versions as major-only (for example "Angular 22.x") when they must be mentioned at all.
