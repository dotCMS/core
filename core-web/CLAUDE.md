# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Overview

DotCMS Core-Web monorepo — Angular + Nx. Uses **Yarn** as package manager. Nx is not installed globally — always use `yarn nx`.

### MCP Servers

Configured in `/.mcp.json`. Use these instead of guessing:

- **`angular-cli`** — Angular best practices, documentation search, code examples. Use before writing Angular code.
- **`primeng`** — PrimeNG component API, props, events, examples. Use when building UI.
- **`chrome-devtools`** — Browser automation, screenshots, network debugging, performance tracing.

## Essential Commands

```bash
yarn nx serve dotcms-ui                    # Dev server (proxies /api/* to port 8080)
yarn nx build dotcms-ui                    # Build
yarn nx test {project}                     # Test specific project
yarn nx test {project} --testPathPattern=  # Test specific file
yarn nx lint {project}                     # Lint
yarn nx affected:test                      # Test only changed projects
yarn run test:dotcms                       # Test all
yarn run lint:dotcms                       # Lint all
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

## Portlet Development

New portlets go in `libs/portlets/`. For full patterns, architecture, testing, and Nx generator setup:

> **See [`libs/portlets/CLAUDE.md`](libs/portlets/CLAUDE.md)** — the complete portlet development guide with `dot-tags` as canonical reference.

## Testing (Jest + Spectator)

### Config

- Use `dot-content-drive` portlet as reference for test config
- `jest.config.ts` must have `isolatedModules: true` in jest-preset-angular transform options — without it, transitive deps (`@angular/common/http`, `@primeuix/themes/lara`) fail with TS2307
- `tsconfig.json` — do NOT add `"strict": true` or `"module": "preserve"`
- `tsconfig.spec.json` — keep minimal (only `module`, `target`, `types`)
- Import `mockProvider` from `@ngneat/spectator/jest` (not `@ngneat/spectator`)

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
