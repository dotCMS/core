# Portlet Development Guide

> **Parent node**: [`core-web/CLAUDE.md`](../../CLAUDE.md) (Angular rules, commands, testing)
> **Reference portlet**: `libs/portlets/dot-tags/` — read the source when in doubt
> **SignalStore docs**: https://ngrx.io/guide/signals/signal-store

## Architecture Pieces

Every CRUD portlet has these parts:

| Piece | What to build | Reference file (`dot-tags`) |
|-------|--------------|----------------------------|
| **Shell** | Minimal wrapper, renders the list component | `dot-tags-shell/dot-tags-shell.component.ts` |
| **List + Store** | Data table with pagination, search, sort; store manages state & HTTP | `dot-tags-list/` and `dot-tags-list/store/` |
| **Create/Edit dialog** | Single component, two modes via `DynamicDialogConfig.data` | `dot-tags-create/dot-tags-create.component.ts` |
| **Routes** | `dotFeatureRoutes` exported from `lib.routes.ts`, registered in `app.routes.ts` | `lib.routes.ts` |

Optional: Import dialog (CSV/file upload) — see `dot-tags-import/`.

## Separation of Concerns (Critical Rule)

| Layer | Responsibility | Owns |
|-------|---------------|------|
| **Store** | Data fetching, state mutations, API calls | HTTP calls, `patchState`, error handling via `DotHttpErrorManagerService` |
| **List Component** | UI orchestration | Opens dialogs, shows confirmations, translates `TableLazyLoadEvent`, debounces search |
| **Create Component** | Form logic | Reactive form, validation, `DynamicDialogRef.close(formValue)` |
| **Shell Component** | Routing wrapper | Just renders the list component |

**Store MUST NOT** open dialogs, inject `DialogService`, or interact with UI. Store is data only.

## Key Rules

- `untracked()` inside `effect()` to prevent infinite loops
- `take(1)` on every HTTP call (signal store — no long-lived subscriptions)
- Error handling: always `catchError` → `httpErrorManager.handle(error)` → `return EMPTY`
- On error from CRUD actions, set status back to `'loaded'` (not `'error'`) so the list stays usable
- `DotHttpErrorManagerService.handle(error)` for all HTTP errors — no custom error UI
- All user-facing text uses i18n keys via `DotMessagePipe` (`| dm`) or `DotMessageService.get()`
- Key naming: `{feature}.{context}.{element}` (e.g., `tags.confirm.delete.header`)
- `data-testid` on every interactive element; `[attr.aria-label]` on inputs and icon-only buttons

## CRUD Patterns

**Modal dialogs (default)**: List component opens `DialogService.open(CreateComponent, ...)`. The dialog closes with the form value; the list component passes it to the store. This is the pattern used in `dot-tags` and should be the default for new portlets.

**Routed CRUD (rare)**: Separate route for create/edit pages. Use only when the form is too complex for a dialog (many tabs, nested data). See `dot-experiments` for this pattern.

## Nx Generator Post-Setup

After running the generator:

```bash
yarn nx generate @nx/angular:library --name=portlet \
  --directory=libs/portlets/dot-{feature} \
  --tags=type:feature,scope:dotcms-ui,portlet:{feature} \
  --prefix=dot --standalone --no-interactive
```

**Required fixes**:

1. **tsconfig alias** in `core-web/tsconfig.base.json`: change generated `"portlet"` → `"@dotcms/portlets/dot-{feature}/portlet"`
2. **project.json** `name`: change to `portlets-dot-{feature}-portlet`
3. **jest.config.ts** `displayName`: change to `portlets-dot-{feature}-portlet`
4. **jest.config.ts**: add `isolatedModules: true` in transform options (required for transitive deps)
5. **tsconfig.spec.json**: keep minimal — only `module`, `target`, `types`
6. **Delete** generated `README.md` and boilerplate component in `src/lib/portlet/`

## Anti-Patterns

| Do NOT | Do Instead |
|--------|-----------|
| Store opens dialogs or injects DialogService | Component opens dialogs, passes result to store |
| Missing `untracked()` in effect | Wrap store method calls in `untracked()` |
| Missing `isolatedModules: true` in jest config | Add it — transitive deps fail without it |
| Adding `"strict": true` to tsconfig.json | Omit — causes issues with Angular compiler |
| Adding `"module": "preserve"` to tsconfig.spec.json | Use `"module": "commonjs"` |
| Hardcoded text in templates | Use `DotMessagePipe` (`| dm`) for all user-facing text |
| Custom error dialogs | Use `DotHttpErrorManagerService.handle(error)` everywhere |
| `@Input()` / `@Output()` decorators | Use `input()` / `output()` signal functions |
| `*ngIf` / `*ngFor` structural directives | Use `@if` / `@for` control flow |

## Other Reference Portlets

- **`dot-locales`** — Simple list/edit (uses legacy ComponentStore)
- **`dot-experiments`** — Full CRUD with guards, resolvers, shell, routed create/edit
- **`dot-analytics`** — Enterprise license checking, lazy loading
- **`dot-content-drive`** — Complex nested routing, reference for test config
