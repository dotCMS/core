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
- **Styling**: Tailwind CSS + PrimeFlex utilities
- **Testing**: Jest + Spectator, use `data-testid` for selectors

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

## Creating New Portlets

New portlets go in `libs/portlets/`. Use Nx generators:

```bash
# Generate portlet library
yarn nx generate @nx/angular:library portlet \
  --directory=libs/portlets/dot-{feature} \
  --tags=type:feature,scope:dotcms-ui,portlet:{feature} \
  --prefix=dotcms --standalone

# Optional: data-access library
yarn nx generate @nx/angular:library data-access \
  --directory=libs/portlets/dot-{feature} \
  --tags=type:data-access,scope:portlets --prefix=dotcms
```

### Standard Structure

```
libs/portlets/dot-{feature}/
├── portlet/
│   ├── src/
│   │   ├── index.ts              # export * from './lib/lib.routes'
│   │   └── lib/
│   │       ├── lib.routes.ts     # Route definitions (Nx convention)
│   │       ├── {feature}-shell.component.ts
│   │       ├── {feature}-list.component.ts
│   │       └── {feature}-edit.component.ts
│   └── project.json
└── data-access/                   # Optional
    └── src/lib/
        └── services/
```

### Route Definition

```typescript
// libs/portlets/dot-{feature}/portlet/src/lib/lib.routes.ts

// ✅ CORRECT: camelCase with 'Routes' suffix
export const dotFeatureRoutes: Routes = [
  {
    path: '',
    component: DotFeatureShellComponent,
    children: [
      { path: '', component: DotFeatureListComponent },
      {
        path: ':id',
        loadComponent: () =>
          import('./dot-feature-edit.component').then((m) => m.DotFeatureEditComponent)
      }
    ]
  }
];

// ❌ WRONG: PascalCase or verbose
export const DotFeatureRoutes: Routes = [...]
export const DotFeaturePortletRoutes: Routes = [...]
```

### Register in App Routes

```typescript
// apps/dotcms-ui/src/app/app.routes.ts
{
  path: '{feature-slug}',
  canActivate: [MenuGuardService],
  canActivateChild: [MenuGuardService],
  data: { reuseRoute: false },
  loadChildren: () =>
    import('@dotcms/portlets/dot-{feature}/portlet').then((m) => m.dotFeatureRoutes)
}
```

### Import Alias Conventions

```typescript
// New portlets (libs/portlets/) — use @dotcms/portlets/*
import('@dotcms/portlets/dot-{feature}/portlet').then((m) => m.dotFeatureRoutes)

// Legacy portlets (apps/dotcms-ui/src/app/portlets/) — use @portlets/*
import('@portlets/dot-{feature}/dot-{feature}.routes').then((m) => m.dotFeatureRoutes)
```

### Reference Portlets

- **`dot-locales`** — Simple list/edit (good starting point)
- **`dot-experiments`** — Full CRUD with guards, resolvers, shell
- **`dot-analytics`** — Enterprise license checking, lazy loading
- **`dot-content-drive`** — Complex nested routing

## Backend Integration

- Dev proxy: `proxy-dev.conf.mjs` routes `/api/*` to port 8080
- API services: `libs/data-access/` via `DotHttpService`
- OpenAPI spec: Use `http://localhost:8080/api/openapi.json` (local dev instance), fallback to `https://demo.dotcms.com/api/openapi.json`. Fetch this to understand available endpoints, request/response schemas, and parameters before building API integrations.

## For Backend/Java Development

See **[../CLAUDE.md](../CLAUDE.md)** for Java, Maven, REST API, and Git workflow standards.

<!-- nx configuration start-->
<!-- Leave the start & end comments to automatically receive updates. -->

# General Guidelines for working with Nx

- When running tasks (for example build, lint, test, e2e, etc.), always prefer running the task through `yarn nx` (i.e. `yarn nx run`, `yarn nx run-many`, `yarn nx affected`) instead of using the underlying tooling directly
- You have access to the Nx MCP server and its tools, use them to help the user
- When answering questions about the repository, use the `nx_workspace` tool first to gain an understanding of the workspace architecture where applicable.
- When working in individual projects, use the `nx_project_details` mcp tool to analyze and understand the specific project structure and dependencies
- For questions around nx configuration, best practices or if you're unsure, use the `nx_docs` tool to get relevant, up-to-date docs. Always use this instead of assuming things about nx configuration
- If the user needs help with an Nx configuration or project graph error, use the `nx_workspace` tool to get any errors

<!-- nx configuration end-->
