---
name: dot-ui-angular-standards
description: >
  dotCMS Angular coding standards for the core-web Nx workspace. Use this skill for ANY frontend work
  under core-web/ — writing or editing a component, service, store, directive, pipe, guard, template,
  SCSS file, or Jest spec; reviewing a frontend diff; scaffolding new UI; or answering "how do we do X
  in Angular in this repo". Also trigger whenever any of these appear: `standalone: true`,
  `changeDetection`, `ChangeDetectionStrategy`, `ng new`/`ng generate`/`ng serve`/`ng build`/`ng test`,
  `angular.json`, Karma, `@ngneat/spectator`, `npm run`/`yarn` inside core-web, inline `template:` or
  `styles:` in a decorator, `*ngIf`/`*ngFor`, `ngClass`/`ngStyle`, `@Input()`/`@Output()` decorators,
  `@HostBinding`/`@HostListener`, `destroy$`/`takeUntil`, constructor injection, `dot-icon`, PrimeIcons,
  or "add an icon". This skill OVERRIDES the vendored `angular-developer` skill's generic Angular and
  Angular CLI guidance whenever the work is inside this repository.
owner: "@dotcms/falcon"
status: experimental
related: [dot-ui-vtl-migration]
---

# dotCMS Angular Standards (core-web)

**Precedence.** This skill overrides the vendored `angular-developer` skill for all work in this
repository. That skill is upstream, generic Angular guidance: it assumes `ng` CLI, `angular.json`,
Karma, and a greenfield app. None of those apply here. Where the two disagree, **this skill wins**.
Use `angular-developer` only for framework concepts it explains better (signals semantics, DI
resolution, routing APIs) — never for its tooling, project-creation, or `standalone`/`changeDetection`
advice.

That skill is vendored byte-for-byte from upstream Angular and must never be hand-edited — new dotCMS
rules belong here instead. See
[`.agents/skills/angular-developer/PROVENANCE.md`](../../../.agents/skills/angular-developer/PROVENANCE.md)
for its upstream source and re-sync procedure.

Canonical, longer-form source: **`docs/frontend/ANGULAR_STANDARDS.md`**. Read it when you need the
full rationale or examples. This file is the short override sheet.

## Workspace facts

- Nx monorepo rooted at `core-web/`. There is **no `angular.json`** — the workspace is driven by `nx.json`.
- Angular 22.x, PrimeNG 21.x, Tailwind 4.x, NgRx Signals 21.x, Jest 30, TypeScript 6, Node 22.22.3+.
- Package manager: **pnpm** (see `packageManager` in `core-web/package.json`).
- **`core-web/package.json` is the source of truth for versions.** Read it; never hardcode or assume a patch version.

## Non-negotiables

### 1. Never write `standalone: true`

Standalone is the Angular default. The property is forbidden in decorators here. Remove it when you
see it in a file you are already editing.

### 2. Never set `changeDetection` on a new component

`OnPush` is the framework default as of Angular v22
([docs](https://angular.dev/guide/components/advanced-configuration#changedetectionstrategy)), so an
explicit `ChangeDetectionStrategy.OnPush` is redundant noise.

Components explicitly marked `ChangeDetectionStrategy.Eager` (the opt-in eager mode, renamed from
`Default` in v22) opted in deliberately — **leave them alone.** Do not convert `Eager` to `OnPush`
when touching a file for unrelated work.

### 3. Build, test, and serve through `pnpm nx` — never `ng`

There is no Angular CLI workspace here. Never `yarn`, never `npm`.

| Task | Command (run from `core-web/`) |
| --- | --- |
| Install deps | `pnpm install` |
| Serve the UI | `pnpm nx run dotcms-ui:serve` |
| Test a project | `pnpm nx run dotcms-ui:test` |
| Build a project | `pnpm nx build dotcms-ui` |
| Scaffold code | Nx generators (`pnpm nx g …`) — see the `nx-generate` skill |

### 4. Testing is Jest 30 + `@openng/spectator`

- Never `@ngneat/spectator` — the workspace has fully migrated (0 occurrences remain). Never Karma, never Vitest.
- Select with `byTestId` and put `data-testid` on the elements you assert against.
- Set inputs with `spectator.setInput()`. Never assign to component properties directly.
- Mock stores in `componentProviders`, not `providers`.

### 5. Three files per component — `.ts` + `.html` + `.scss`

Inline `template:` and inline `styles:` in the decorator are forbidden. Reference the files with
`templateUrl` and `styleUrls` (plural — matching `docs/frontend/ANGULAR_STANDARDS.md` and the
dominant convention in the workspace).

### 6. Teardown uses `DestroyRef` + `takeUntilDestroyed()`

```ts
readonly #destroyRef = inject(DestroyRef);

this.#service.load().pipe(takeUntilDestroyed(this.#destroyRef)).subscribe(…);
```

The legacy `destroy$` + `takeUntil` pattern exists in older code. It is **not** for new work, and it is
**not** to be mass-migrated — only change it in code you are already rewriting.

### 7. Reuse before creating

Before creating a new component, in this order:

1. Check dotCMS's own components — `core-web/libs/ui` (`@dotcms/ui`) and the existing feature libs.
2. Check PrimeNG.
3. Only then create a new component — and say in the PR why neither of the above fit.

### 8. Every component that renders data handles all four states

**loading**, **empty**, **error**, **loaded** — explicitly. Never leave a blank render path. If a
template has no branch for a state, that state is a bug.

### 9. Error handling is mandatory

No silent failures. No unguarded `.subscribe()`. No empty `catchError`. Handle the error in the service
with `catchError`, surface a user-facing error state in the component, and log the diagnostic detail.

### 10. Icons are Material Symbols

```html
<span class="material-symbols-outlined">drag_indicator</span>
```

The icon name goes in the element's **text content**, not in the class. Fonts are self-hosted via
`core-web/libs/dotcms-scss/shared/_material-symbols-outlined.scss`.

- Existing PrimeIcons (`pi pi-*`) stay as they are — do **not** mass-migrate them.
- PrimeNG's internal icons are a theming concern and out of scope.
- Do **not** use the deprecated `dot-icon` component.

### 11. API and naming conventions

| Rule | Do | Don't |
| --- | --- | --- |
| Signals | `$mySignal` (**`$` prefix**) | `mySignal`, `mySignal$` |
| Observables | `myObservable$` (**`$` suffix**) | `$myObservable` |
| Inputs / outputs | `input()`, `output()` functions | `@Input()`, `@Output()` decorators |
| Injection | `inject()` | constructor parameter injection |
| Control flow | `@if`, `@for` (with `track`), `@switch` | `*ngIf`, `*ngFor`, `*ngSwitch` |
| Classes / styles | `[class.x]`, `[style.x]` bindings | `ngClass`, `ngStyle` |
| Host bindings | the `host: { … }` metadata object | `@HostBinding`, `@HostListener` |
| Selectors | `dot-` prefix | anything else |

### 12. Signal Forms first

New forms use **Signal Forms** (`@angular/forms/signals`, available since Angular v21 and shipped in
the Angular 22.x here):

```ts
import { form, Control } from '@angular/forms/signals';

protected readonly $model = signal({ name: '', email: '' });
protected readonly userForm = form(this.$model);
```

Existing **Reactive Forms stay as they are** — do NOT mass-migrate them; convert only when already
rewriting a form for other reasons. Template-driven forms are not for new work. Use
`@angular/forms/signals/compat` to interoperate with an existing Reactive Forms control.

### 13. TypeScript 6

`core-web/tsconfig.base.json` sets `"ignoreDeprecations": "6.0"` **transitionally**. It unblocks
compilation of existing code that still uses deprecated APIs. New code must not rely on it.

## Before you finish

- [ ] No `standalone: true` and no `changeDetection` added
- [ ] Any `Eager` component you touched is still `Eager`
- [ ] Three files exist for each new component; no inline template or styles
- [ ] Loading, empty, error, and loaded paths all render something
- [ ] Every stream is torn down with `takeUntilDestroyed(destroyRef)`; every failure path is handled
- [ ] Specs use `@openng/spectator`, `byTestId`, and `setInput()`
- [ ] Verified with `pnpm nx run <project>:test` and `pnpm nx run <project>:lint` — not `ng`
