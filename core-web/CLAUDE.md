# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Overview

DotCMS Core-Web monorepo — Angular + Nx. Uses **pnpm** as package manager. Nx is not installed globally — always use `pnpm nx`.

### MCP Servers

Configured in `/.mcp.json`. Use these instead of guessing:

- **`angular-cli`** — Angular best practices, documentation search, code examples. Use before writing Angular code.
- **`primeng`** — PrimeNG component API, props, events, examples. Use when building UI.
- **`chrome-devtools`** — Browser automation, screenshots, network debugging, performance tracing.

## Essential Commands

```bash
pnpm nx serve dotcms-ui                    # Dev server (proxies /api/* to port 8080)
pnpm nx build dotcms-ui                    # Build
pnpm nx test {project}                     # Test specific project
pnpm nx test {project} --testPathPatterns=  # Test specific file (note the plural — Jest renamed it)
pnpm nx lint {project}                     # Lint
pnpm nx affected:test                      # Test only changed projects
pnpm run test:dotcms                       # Test all
pnpm run lint:dotcms                       # Lint all
```

## Architecture

### Where Code Goes

```
apps/dotcms-ui/              # Main admin UI application
libs/portlets/               # Feature portlets (new portlets go HERE)
libs/ui/                     # Shared UI components (multi-portlet)
libs/data-access/            # Shared services (multi-portlet)
libs/dotcms-models/          # TypeScript interfaces and types
libs/edit-content/           # Content editing library
libs/block-editor/           # TipTap rich text editor
libs/sdk/                    # External SDKs (client, react, angular)
```

### Code Placement Rules

```
Is this component/service used by multiple portlets?
├─ NO  → libs/portlets/{feature}/
└─ YES → Is it domain-agnostic?
    ├─ YES (UI)      → libs/ui/
    ├─ YES (Service)  → libs/data-access/
    └─ NO             → libs/portlets/shared/ or refactor
```

## Angular Rules (REQUIRED)

### Modern Syntax — Always Use

```typescript
// Control flow
@if (condition()) { <content /> }           // NOT *ngIf
@for (item of items(); track item.id) { }   // NOT *ngFor

// Inputs/Outputs
data = input<string>();                      // NOT @Input()
onChange = output<string>();                  // NOT @Output()

// Testing selectors
<button data-testid="submit-btn">Submit</button>
spectator.query('[data-testid="submit-btn"]');
spectator.setInput('prop', value);           // ALWAYS use setInput
```

### Component Conventions

- **Prefix**: All components use `dot-` prefix
- **Standalone**: All new components must be standalone
- **State**: Use NgRx signals (`@ngrx/signals`) for state management
- **Styling**: Tailwind CSS + PrimeNG theme (PrimeFlex deprecated/removed — use Tailwind utilities instead)
- **Testing**: Jest + Spectator, use `data-testid` for selectors
- **Dialogs**: All dialogs must have `closable: true` and `closeOnEscape: true` to allow closing via X button and ESC key

### Form Markup

Always wrap form fields with this structure for consistent styling:

```html
<form class="form">
  <div class="field">
    <label for="name">Name</label>
    <input pInputText id="name" />
  </div>
  <div class="field">
    <label for="site">Site</label>
    <p-select id="site" [options]="sites()" />
  </div>
</form>
```

## TypeScript Strict Mode

The per-project rollout (epic #35932) is **done**. `tsconfig.base.json` now carries strict mode
for the whole workspace, so **a new project needs no flags of its own** — it inherits them:

```json
"compilerOptions": {
    "forceConsistentCasingInFileNames": true,
    "strict": true,
    "noImplicitOverride": true,
    "noPropertyAccessFromIndexSignature": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true
},
"angularCompilerOptions": {
    "enableI18nLegacyMessageIdFormat": false,
    "strictInjectionParameters": true,
    "strictInputAccessModifiers": true,
    "typeCheckHostBindings": true,
    "strictTemplates": true
}
```

Do **not** re-declare any of these in a project tsconfig — that is the duplication the base block
replaced. `angularCompilerOptions` lives in the base too: it is a root-level key that TypeScript
ignores entirely, so the non-Angular projects that inherit it (`libs/sdk/react`, `libs/sdk/vue`,
`apps/mcp-server`, …) are unaffected. Both blocks are inherited through the `extends` chain,
verified against a deliberately broken host binding.

`@nx/angular:application` and `@nx/angular:library` default to `strict: true` and `nx.json` now
pins it explicitly, so generated projects arrive strict. The generator still writes a local copy
of the TS flags — delete it, base already has them.

Two projects opt out **narrowly**, and only from the two non-`strict` flags:

| Project | Opted out of | Why |
|---|---|---|
| `libs/sdk/ai` | `noPropertyAccessFromIndexSignature`, `noImplicitOverride` | 56 `TS4111`/`TS4114`. `strict` itself passes. |
| `apps/ai-evals` | same | Same errors — it compiles `sdk/ai` sources through a path mapping. |

Both carry a `// TODO(#35932)`. Nothing opts out of `strict` itself.

When you do hit an error: no new `any` — use explicit types. To silence something unavoidable, use
`@ts-expect-error` with a `// TODO(#issue):` note, never a blanket `@ts-ignore`.

**What enforces this:** for Rollup libs that emit declarations (`"declaration": true`), `@rollup/plugin-typescript` is in the build chain and reports type errors, so the `build` target is the gate — CI runs `nx run-many -t build` (the `build-test` execution in `core-web/pom.xml`). Do **not** add a separate `typecheck` target to those projects; it is redundant. `lint` does not catch type errors — ESLint reports lint rules, not TS diagnostics.

Vite-based projects are the exception: their builds use esbuild and skip type checking, which is why the Nx Vite plugin infers a separate `typecheck` target for them.

Verify locally:

```bash
pnpm exec tsc -p <projectRoot>/tsconfig.lib.json --noEmit
pnpm exec nx run <project>:build
pnpm exec nx affected -t build,lint --base=origin/main   # check you didn't break consumers
```

`<projectRoot>` is the path from `project.json`, which is often nested — e.g. `libs/sdk/create-app`, not `libs/create-app`. Two caveats on the tsconfig name:

- **Apps** use `tsconfig.app.json`.
- **Some projects have no `tsconfig.lib.json`** (`libs/sdk/create-app` is one); use their `tsconfig.json` instead.

Also check `tsconfig.spec.json` — the flags live in `tsconfig.json`, which the spec config extends, so specs go strict too and their errors are yours to fix.

> **Watch out for masked results.** If a tsconfig declares a `types` entry that is not installed, `tsc` reports `TS2688: Cannot find type definition file for '<name>'` and **stops before semantic checking** — you get one error and no type checking at all. A stable error count across a change proves nothing in that case. `libs/dotcms-js/tsconfig.spec.json` is affected today (`"types": ["jasmine"]`, and `@types/jasmine` is not installed in the workspace); check it with `--types node` to see real diagnostics. `apps/dotcms-block-editor` had the same defect in **three** of its configs — worth checking `tsconfig.editor.json` too, not just `spec`.
>
> A **`files` entry pointing at a file that does not exist** masks results the same way: `tsc` reports `TS6053: File '<path>' not found` and aborts before semantic checking. Unlike a non-matching `include` glob — which is harmless — a missing `files` entry is fatal. This is what hid `libs/sdk/angular/tsconfig.spec.json` (it listed a `next/test-setup.ts` left over from a deleted directory), so that config had never completed a single semantic pass. Before trusting any error count, confirm `tsc` actually reached the code: a config-level error means it did not.
>
> **Deprecated options abort too — which is why the workspace has none left.** TypeScript 6.x raises `TS5101`/`TS5107` for options that TS 7.0 removes, and these are config-level errors, so they abort before semantic checking just like the two above. Do **not** guess the list — read it off the compiler, because it is longer than it looks and a partial list will let a config slip through:
>
> ```bash
> node -e "const fs=require('fs');const s=fs.readFileSync(require.resolve('typescript'),'utf8');
> const i=s.indexOf('function verifyDeprecatedCompilerOptions');const seg=s.slice(i,i+14000);
> console.log(seg.slice(seg.indexOf('checkDeprecations(\"6.0\"'),4000))"
> ```
>
> As of TypeScript 6.0.3 that window covers: `baseUrl` (**any** value), `downlevelIteration` (any value, even `false`), `esModuleInterop: false`, `allowSyntheticDefaultImports: false`, `alwaysStrict: false`, `target: "es5"`, `moduleResolution: "node"`/`"node10"`/`"classic"`, `module: "none"`/`"amd"`/`"umd"`/`"system"`, and `outFile`. Sweeping with a hand-written subset of that list missed `downlevelIteration` in `libs/edit-content-bridge/tsconfig.lib.json`, which only surfaced from `nx run-many -t typecheck`. Sweep resolved configs (`tsc --showConfig -p <config>`), not the raw files — most of these arrive through `extends`. `tsconfig.base.json` used to silence all of them with `ignoreDeprecations: "6.0"` (added by the Nx migration `23-1-0-add-ignore-deprecations-for-ts6`), and because every config extends the base, **that one flag was the only thing keeping all ~168 configs compiling** — deleting it alone would have made strict mode silently stop checking anything, everywhere. The flag and `baseUrl` are both gone from the base now: `paths` values carry a leading `./` instead, which TypeScript resolves against the config that declares them, so no `baseUrl` is needed. **Never add `baseUrl` or `ignoreDeprecations` to a project tsconfig**, and write new `paths` entries `./`-relative. TS 7.0 removes these options outright, so this is a deadline, not housekeeping.
>
> There is exactly **one** exception, `libs/sdk/experiments/tsconfig.lib.json`, and it is an Nx limitation rather than a choice. It declares its own `paths` so SDK peers resolve to their built `dist` (`.d.ts`) rather than to source (without that, `@rollup/plugin-typescript` pulls cross-project sources into the program and the build fails with `TS6059`). @nx/rollup 23.1.1 cannot cope with a project-level `paths` block when no `baseUrl` exists: `updatePaths` (`@nx/js` `buildable-libs-utils`) overwrites each dependency key with its output path relative to the **workspace root** (`dist/libs/sdk/react`), and `with-nx` then resolves that against `resolvePathsBaseUrl()`, which — finding no `baseUrl` in the `extends` chain — falls back to the directory of the config that declared `paths`. The result is `libs/sdk/experiments/dist/libs/sdk/react` and a `TS2307`. Anchoring `baseUrl` at the workspace root in that one file is the only fix; its `ignoreDeprecations` is scoped to the same file, so it masks nothing anywhere else and `tsc -p` on it still completes a full strict semantic pass.
>
> `libs/dotcms-webcomponents` no longer needs the `--ignoreDeprecations 6.0` CLI workaround this section used to prescribe. It carried `baseUrl` + `moduleResolution: "node"` and could not set the flag itself (Stencil bundles TypeScript 5.8.3, which only accepts `"5.0"`), so no single value satisfied both compilers. Dropping `baseUrl` — its `paths` are now `../../`-relative — removed the conflict: TS 5.8 supports `paths` without `baseUrl`, so plain `tsc -p libs/dotcms-webcomponents/tsconfig.json --noEmit` now reaches the code, and the Stencil build still passes.
>
> **The general rule:** any error whose code starts `TS5` or `TS6`, or `TS2688`, is a *configuration* error. `tsc` never reached your code, so the count that follows means nothing. Read the first error before trusting the last number.
>
> **A silent fake zero: `include: []`.** All the aborts above at least *report* something. This one does not. Many project tsconfigs hold only `references` and delegate the real work to `tsconfig.lib.json` / `tsconfig.app.json` — `apps/dotcms-binary-field-builder/tsconfig.json` is one. Pointing `tsc -p` at that file compiles **nothing** and prints nothing, which reads exactly like a clean project. That app had *none* of the six flags while appearing to be at zero. **Measure `tsconfig.lib.json` for libraries and `tsconfig.app.json` for apps — never the project tsconfig that only holds `references`.**
>
> **A project with no `build` target has never had its templates checked.** `libs/edit-content` and `libs/block-editor` have only a `test` target. Both declare `strictTemplates` in `angularCompilerOptions`, and in both it is inert: nothing ever compiles their templates. Since `tsc -p` does not check templates either (see above), a library like this can be at 0 errors on both its configs and still have template type errors — a manual `nx run <app>:build` of a consuming app found a real one in `block-editor`. Treat "0 errors" on a build-less library as covering its TypeScript only.
>
> **The repo's TypeScript is not always the strictest compiler in the build.** `libs/dotcms-webcomponents` type-checks twice: once by the workspace's `tsc` (6.0.3) and once by Stencil, which bundles its own 5.8.3. TypeScript 6 re-declared `Node.textContent` as an asymmetric accessor — `get(): string`, `set(value: string | null)` — so `element.textContent.replace(...)` is clean under 6 and `Object is possibly 'null'` under 5.8. The project reached **0** on `tsc -p` and the Stencil build still failed. Where two compilers check the same sources, the build is the gate; `tsc -p` at zero is a necessary condition, not a sufficient one.
>
> Related, and the reason that took two attempts to find: **verify a build by its exit status, never by grepping its output.** Stencil prints `transpile finished` and `build finished` for the phases that did succeed, so a grep for `finished` matches on a run that ends in `build failed` and exits non-zero. Four commits went in claiming a passing build on that basis.
>
> **`moduleResolution: node10` breaks Angular too, not just `@dotcms/*`.** A blast-radius sweep reported `libs/image-editor` at **996** spec errors; 335 of them were `TS2307: Cannot find module '@angular/common/http'`. Its `tsconfig.spec.json` carried `module: commonjs` + `moduleResolution: node10`, which cannot resolve subpath exports from *any* package. The real count was **8**. Unlike the aborts above this one produces a plausible-looking flood of code errors, so the tell is the first error, not the count: `TS2307` on a package that is obviously installed means the resolver, not the code.
>
> No config carries `node10` any more. The last three were spec configs — `libs/portlets/dot-agents`, `libs/portlets/dot-auth` and `libs/sdk/create-app` — and moving them to the workspace's normal spec pattern (`module: "preserve"` + `moduleResolution: "bundler"`) took `dot-agents` from **1079** reported errors to **10** real ones. Treat a four-digit error count on a spec config as a resolver symptom until proven otherwise.
>
> **A Vite virtual module reads as one error per consumer.** `libs/sdk/client` imports `virtual:sdk-version`, which its Vite build injects and `tsc -p` cannot resolve. `libs/sdk/angular`, `libs/sdk/react` and `libs/sdk/vue` therefore each report exactly one `TS2307` that their real `build` target does not. Subtract it before comparing counts.
>
> **`SpyObject<T>` cannot re-implement a union-returning method.** `@openng/spectator`'s mapped type routes every member through `T[P] extends (...args: any[]) => infer R ? ... : ...`. When `R` is a union — `string | string[] | undefined`, say — that conditional *distributes*, so the member's type becomes an **intersection** of `jest.Mock`s whose `mockImplementation` overloads demand `=> undefined`, `=> string` and `=> string[]` simultaneously. No implementation satisfies all three, and the error names an intersection the source never wrote. Reach the spy through one explicit signature (`store.m as unknown as jest.Mock<R, [A]>`) rather than trying to satisfy it.
>
> **Flags interact across projects.** `libs/portlets/dot-query-tool` has `noImplicitReturns` *without* `strict`, and that combination caught a `TS7030` that `libs/edit-content` — which has `noImplicitReturns` too — did not, because its `strict` changes how a mixed `void`/teardown return is inferred. A clean `tsc -p` on the project you changed is not sufficient: re-measure the strict consumers as well.
>
> **A duplicate key in a tsconfig silently wins, and `tsc` does not warn.** `"strict": true` followed later in the same object by `"strict": false` leaves the project non-strict, with no diagnostic of any kind. Two projects on this branch (`libs/new-block-editor`, `libs/edit-content-bridge`) were closed as strict while being nothing of the kind, because the flags were added above a pre-existing `"strict": false`. The only thing that reported it was the **Angular compiler**, as a `Duplicate key "strict" in object literal` warning in an app build's output. After adding flags, read the whole `compilerOptions` block — do not just append.
>
> **The app build is the template gate even when `strictTemplates` is off.** It is `false` in all four apps (#35930), but a library's own `strictNullChecks` still applies to the expressions in its templates, and the Angular compiler is the only thing that evaluates them. `dotcms-binary-field-builder:build:production` found eight real errors in `libs/edit-content` and `libs/portlets/edit-ema/ui` after both measured 0 on `tsc -p`. For any library whose types feed a template, run a consuming app's production build before calling it done.
>
> **An ambient `.d.ts` only protects the project that includes it.** `libs/portlets/dot-experiments` declared the untyped `jstat` module in `src/jstat.d.ts`, reached through its own `include`. A strict consumer compiling those sources through a path mapping pulls in the import graph but *not* that sibling declaration, so `edit-ema/portlet` inherited a `TS7016` from a dependency measuring 0. Put such declarations in `core-web/types/` and wire them with a `paths` entry in `tsconfig.base.json`, which every project inherits (`htmldiff-js` and `jstat` are the precedents). A triple-slash reference also works but `@typescript-eslint/triple-slash-reference` forbids it.
>
> **An unresolved type name becomes `any` and inflates the count downstream.** Annotating a parameter with a type you forgot to import gives you one `TS2304` *and* a cascade of `TS7006`/`TS7031` on everything that reads it, because the annotation itself is `any`. Check `grep -cE 'TS2304|TS2552'` after every batch of annotations; a count that went *up* is usually this. Related: **an intersection with `any` is `any`** — one `declare global { interface Window { x: any } }` defeated every annotation downstream of it.
>
> **Removing an `any` raises the count before it lowers it.** Typing two parameters in `edit-ema/portlet` took it 213 → 239 → 194. Do not judge a fix by the immediate delta.
>
> **Nx silently runs a subset when a project name is wrong.** `nx run-many -t test -p edit-ema-portlet edit-ema-ui` ran only `edit-ema-ui` and exited **0** — the real name is `portlets-edit-ema-portlet`. There is no warning about the name that matched nothing. Confirm the summary names every project you asked for; `nx show projects | grep <fragment>` gets the real names.
>
> **`nx run <project>:test` does not type-check — anywhere.** `jest-preset-angular` runs on ts-jest, and ts-jest copies TypeScript's `isolatedModules` into its own transpile-only switch (`config-set.js:229`), which stops it from building the language-service host it needs for diagnostics (`ts-compiler.js:74`). Since the Jest guidance below requires `isolatedModules: true` in every `tsconfig.spec.json`, **passing tests are never evidence that specs type-check.** `libs/data-access` proved it: 84 `tsc` errors alongside 754 green tests. Always verify specs with `tsc -p <projectRoot>/tsconfig.spec.json --noEmit`.

## Portlet Development

New portlets go in `libs/portlets/`. For full patterns, architecture, testing, and Nx generator setup:

> **See [`libs/portlets/CLAUDE.md`](libs/portlets/CLAUDE.md)** — the complete portlet development guide with `dot-tags` as canonical reference.

## Testing (Jest + Spectator)

### Config

- Use `dot-content-drive` portlet as reference for test config
- `tsconfig.spec.json` tsconfig.spec.json must have "isolatedModules": true in compilerOptions
- `tsconfig.json` — do NOT add `"module": "preserve"`
- `tsconfig.spec.json` — keep minimal (only `module`, `target`, `types`); do NOT add `"strict": true` here, it belongs in the project's `tsconfig.json` (see [TypeScript Strict Mode](#typescript-strict-mode))
- Import `mockProvider` from `@openng/spectator/jest` (not `@openng/spectator`)

### SignalStore Tests

- Use `createServiceFactory` from Spectator
- Call `spectator.flushEffects()` in `beforeEach` to trigger the `withHooks` `onInit` effect
- Mock services with `mockProvider(Service, { method: jest.fn().mockReturnValue(of(...)) })`
- Test error paths: mock service to `throwError(() => error)`, assert `httpErrorManager.handle` was called
- For `jest.mock()` of utilities: place the mock **before** the import

### Component Tests (with Mocked Store)

- Use `createComponentFactory` from Spectator
- Store goes in `componentProviders` (component-level injection), not `providers`
- Mock all signal getters as `jest.fn().mockReturnValue(...)` and all methods as `jest.fn()`
- PrimeNG button clicks: `spectator.query(byTestId('btn'))?.querySelector('button')` then `spectator.click(el)`

### Dialog Tests

- Mock `DialogService.open` to return `{ onClose: new Subject() }`, then emit a value and complete the subject
- Two `describe` blocks for create/edit dialog: one with `DynamicDialogConfig.data: {}`, one with `data: { item }`
- Test that dialogs are configured with `closable: true` and `closeOnEscape: true`

### DotSiteComponent Mocking

- Use `jest.mock('@dotcms/ui', ...)` with a stub implementing `ControlValueAccessor`
- Add `CUSTOM_ELEMENTS_SCHEMA` when mocking complex child components

### Debounce / Timer Tests

- Use `jest.useFakeTimers()` in `beforeEach`, `jest.useRealTimers()` in `afterEach`
- Advance with `jest.advanceTimersByTime(300)` to trigger debounced actions

## Backend Integration

- Dev proxy: `proxy-dev.conf.mjs` routes `/api/*` to port 8080
- API services: `libs/data-access/` via `DotHttpService`
- OpenAPI spec: Use `http://localhost:8080/api/openapi.json` (local dev instance), fallback to `https://demo.dotcms.com/api/openapi.json`. Fetch this to understand available endpoints, request/response schemas, and parameters before building API integrations.

## For Backend/Java Development

See **[../CLAUDE.md](../CLAUDE.md)** for Java, Maven, REST API, and Git workflow standards.

<!-- nx configuration start-->
<!-- Leave the start & end comments to automatically receive updates. -->

## General Guidelines for working with Nx

- For navigating/exploring the workspace, invoke the `nx-workspace` skill first - it has patterns for querying projects, targets, and dependencies
- When running tasks (for example build, lint, test, e2e, etc.), always prefer running the task through `nx` (i.e. `nx run`, `nx run-many`, `nx affected`) instead of using the underlying tooling directly
- Prefix nx commands with the workspace's package manager (e.g., `pnpm nx build`, `npm exec nx test`) - avoids using globally installed CLI
- You have access to the Nx MCP server and its tools, use them to help the user
- For Nx plugin best practices, check `node_modules/@nx/<plugin>/PLUGIN.md`. Not all plugins have this file - proceed without it if unavailable.
- NEVER guess CLI flags - always check nx_docs or `--help` first when unsure

## Scaffolding & Generators

- For scaffolding tasks (creating apps, libs, project structure, setup), ALWAYS invoke the `nx-generate` skill FIRST before exploring or calling MCP tools

## When to use nx_docs

- USE for: advanced config options, unfamiliar flags, migration guides, plugin configuration, edge cases
- DON'T USE for: basic generator syntax (`nx g @nx/react:app`), standard commands, things you already know
- The `nx-generate` skill handles generator discovery internally - don't call nx_docs just to look up generator syntax

<!-- nx configuration end-->
