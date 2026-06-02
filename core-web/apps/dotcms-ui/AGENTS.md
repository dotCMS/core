# dotcms-ui — Agent Guide

Main admin UI application for dotCMS (`/dotAdmin`). Angular 19+ standalone components, `dot-` prefix, built and served through the Nx workspace. Agents working in this directory should read this file first, then refer to the parent guide for shared standards.

## UI Stack

| Layer | Technology | Notes |
|---|---|---|
| Component library | **PrimeNG** | Use `p-*` components (e.g. `p-button`, `p-select`, `p-dialog`) — check the PrimeNG MCP server for API/props |
| Utility CSS | **Tailwind CSS** | Use Tailwind utilities for layout, spacing, and typography — PrimeFlex has been removed |
| Global styles | `libs/dotcms-scss/angular/styles.scss` | Imported as a global stylesheet in the build |
| Component styles | Inline SCSS per component | `inlineStyleLanguage: scss` in `project.json` |

**Key rules:**
- Never use PrimeFlex classes (`p-col-*`, `p-grid`, etc.) — they are removed. Use Tailwind equivalents (`grid`, `flex`, `gap-*`, etc.)
- Always use PrimeNG components for interactive UI (inputs, dropdowns, dialogs, tables) rather than raw HTML equivalents
- PrimeNG icons ship via `primeicons` (`pi pi-*` class names); this package is included in the global styles

> **Standards reference**: All Angular syntax rules, component conventions, testing patterns (Jest + Spectator), and code-placement decisions are documented in **[../../CLAUDE.md](../../CLAUDE.md)**. Do not duplicate them here.

## Commands

All commands must be run from the `core-web/` workspace root and prefixed with `yarn nx` (Nx is not installed globally).

```bash
# Development
yarn nx serve dotcms-ui                              # Dev server at :4200, proxies /api/* → :8080
yarn nx build dotcms-ui                             # Production build → dist/apps/dotcms-ui/
yarn nx build dotcms-ui --configuration=development  # Dev build → ../../tomcat9/webapps/ROOT/dotAdmin

# Tests
yarn nx test dotcms-ui                              # Run all unit tests
yarn nx test dotcms-ui --testPathPattern=my.spec    # Run a specific spec file
yarn nx lint dotcms-ui                              # Lint

# Storybook
yarn nx storybook dotcms-ui                         # Interactive Storybook at :4400
yarn nx build-storybook dotcms-ui                   # Build Storybook to dist-docs/
```

### Deploy into a local Tomcat instance

```bash
# From core-web/:
npm run build:dev -- --output-path /path/to/tomcat9/webapps/ROOT/dotAdmin
```

The dev server's proxy config lives at `apps/dotcms-ui/proxy-dev.conf.mjs` — it forwards every `/api/*` request to `http://localhost:8080`.

## App Structure

```
apps/dotcms-ui/src/
├── app/
│   ├── portlets/            # Feature portlets hosted in this app shell
│   │   ├── dot-apps/        # Apps/integrations portlet
│   │   ├── dot-form-builder/
│   │   ├── dot-starter/     # Onboarding portlet
│   │   ├── dot-templates/
│   │   └── shared/          # Portlet-level shared code (content-types-edit, etc.)
│   ├── view/
│   │   └── components/      # App-level shared components
│   │       ├── _common/     # Generic shared widgets (alerts, wizards, dropdowns)
│   │       ├── dot-toolbar/
│   │       ├── dot-contentlet-editor/
│   │       ├── login/
│   │       └── ...
│   ├── api/
│   │   └── services/        # App-level Angular services
│   │       └── guards/      # Route guards
│   └── shared/
│       └── models/          # App-scoped TypeScript models/interfaces
├── assets/                  # Static assets (images, i18n, etc.)
├── environments/            # environment.ts / environment.prod.ts
└── main.ts                  # Bootstrap entry point
```

### Per-portlet module layout (legacy pattern still used in this app)

New portlets belong in `libs/portlets/` (see placement rules below). For legacy portlets already inside this app, each feature folder follows:

```
feature-name/
├── components/       # Presentational components
├── services/         # Feature services
├── models/           # Feature models
├── utils/            # Feature utilities
├── feature-name.module.ts
├── feature-name-routing.module.ts
└── feature-name.component.ts
```

## Where Code Goes

| Scope | Location |
|---|---|
| New portlet / feature | `libs/portlets/{feature}/` — **not** inside this app |
| UI component used by 2+ portlets | `libs/ui/` |
| Service used by 2+ portlets | `libs/data-access/` |
| TypeScript interfaces / types | `libs/dotcms-models/` |
| App-level component (toolbar, login) | `apps/dotcms-ui/src/app/view/components/` |
| App-level service / guard | `apps/dotcms-ui/src/app/api/services/` |

For the full decision tree, see [../../CLAUDE.md § Code Placement Rules](../../CLAUDE.md).

## Build Outputs

| Configuration | Output path |
|---|---|
| Production | `dist/apps/dotcms-ui/` |
| Development | `../../tomcat9/webapps/ROOT/dotAdmin` |

## Key Assets Bundled at Build Time

The build copies several vendor assets into the output:

- `node_modules/tinymce` → `/tinymce/`
- `node_modules/monaco-editor` → `assets/monaco-editor/`
- `libs/block-editor/src/lib/assets` → `assets/block-editor/`
- `libs/portlets/edit-ema/portlet/src/lib/assets` → `assets/edit-ema/`

If these paths change (e.g. the block-editor moves), update `assets` in `project.json`.

## Implicit Dependencies

This app declares `"implicitDependencies": ["dotcms-webcomponents"]` in `project.json`. Changes to web components will trigger a rebuild of this app in affected-mode CI runs.
