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
- `take(1)` on one-shot HTTP calls (e.g. `loadById`, dialog saves). **Do NOT add `take(1)` inside `rxMethod`** — `rxMethod` manages subscription lifetime automatically; adding `take(1)` there breaks cancellation
- Error handling: always `catchError` → `httpErrorManager.handle(error)` → `return EMPTY`
- On error from CRUD actions, set status back to `'loaded'` (not `'error'`) so the list stays usable
- `DotHttpErrorManagerService.handle(error)` for all HTTP errors — no custom error UI
- All user-facing text uses i18n keys via `DotMessagePipe` (`| dm`) or `DotMessageService.get()`
- Key naming: `{feature}.{context}.{element}` (e.g., `tags.confirm.delete.header`)
- `data-testid` (all lowercase) on every interactive element; `[attr.aria-label]` on inputs and icon-only buttons

## Dialog Sizing Standards

| Dialog type | Width |
|-------------|-------|
| Form / add / edit / import | `700px` |
| Confirmation / warning / delete | `500px` |
| Special (iframes, full-screen) | responsive — `min(92vw, 75rem)` or as needed |

Apply `width` on `DialogService.open()` config. For `p-confirmDialog`, set `[style]` on the template element (PrimeNG's `Confirmation` type does not expose `style` as a confirm-call option):

```typescript
// Form dialog (TypeScript)
this.dialogService.open(MyFormComponent, { width: '700px', ... });
```

```html
<!-- Confirmation dialog (template) -->
<p-confirmDialog [draggable]="false" [style]="{ width: '500px' }" />
```

**Exception**: upload/import dialogs also set `contentStyle: { height: '460px' }` to keep a fixed layout while showing inline errors.

## CRUD Patterns

**Modal dialogs (default)**: List component opens `DialogService.open(CreateComponent, ...)`. The dialog closes with the form value; the list component passes it to the store. This is the pattern used in `dot-tags` and should be the default for new portlets.

**Routed CRUD (rare)**: Separate route for create/edit pages. Use only when the form is too complex for a dialog (many tabs, nested data). See `dot-experiments` for this pattern.

## When the CRUD Pattern Is Not Enough

Standard CRUD portlets (Shell + List + Store) cover most cases. For portlets with complex domain logic, multiple interconnected features, or large state surfaces, decompose the store into feature slices using `signalStoreFeature()`:

```typescript
export const UVEStore = signalStore(
    withUve(),        // system lifecycle
    withFlags(),      // feature flags
    withPage(),       // page asset domain
    withPageApi(),    // backend interactions
    withWorkflow(),   // workflow/lock
    withEditor(),     // editor UI state
);
```

Each feature slice owns a named prefix in the flat state (e.g. `editor*`, `view*`, `page*`) and exposes only the methods and computeds relevant to its domain. See `libs/portlets/edit-ema/portlet/README.md` for a full example.

## Events-Plugin Pattern (NgRx Signals)

`withState` + store methods stays the default for simple CRUD (`dot-tags`). Reach for the events plugin when:

- Many components dispatch into one store and you do not want to thread method calls through inputs/outputs
- You want an auditable action log (every state change has a named, typed event)
- State transitions and async work should be separated so each can be reasoned about (and tested) on its own

### The pieces

| Piece | Where | Rule |
|-------|-------|------|
| `eventGroup({ source, events: { name: type<Payload>() } })` | `*.events.ts` | One group per source; async flows use `Requested → Succeeded → Failed` triples |
| `withReducer(on(event, ({ payload }, state) => newState))` | store | **Only** place state changes |
| `withEventHandlers` | store | **Only** place for async/HTTP; `switchMap` so a re-trigger cancels the in-flight request |
| `injectDispatch(eventGroup)` | component | The store exposes **no methods** for state changes |

### Version note (critical)

The async hook in the installed `@ngrx/signals` (**21.1.1**) is **`withEventHandlers`**. **`withEffects` does not exist** and will not compile — many online examples use that name. Verified exports of `@ngrx/signals/events`:

`event`, `eventGroup`, `on`, `withReducer`, `withEventHandlers`, `injectDispatch`, `Dispatcher`, `Events`, `ReducerEvents`, `mapToScope`, `provideDispatcher`, `toScope`

### Error handling

Same rules as the rest of this guide: the `Failed` handler routes through `DotHttpErrorManagerService.handle(error)` — no custom error UI. A failed LOAD sets `status: 'error'`; a failed CRUD action returns `status` to `'loaded'` so the list stays usable.

```typescript
// experiments-list.events.ts
export const experimentsListEvents = eventGroup({
    source: 'Experiments List',
    events: {
        listRequested: type<void>(),
        listSucceeded: type<DotExperiment[]>(),
        listFailed: type<unknown>()
    }
});

// experiments-list.store.ts
export const ExperimentsListStore = signalStore(
    withState(initialState),
    withReducer(
        on(experimentsListEvents.listRequested, (_event, state) => ({
            ...state,
            status: 'loading' as const
        })),
        on(experimentsListEvents.listSucceeded, ({ payload }, state) => ({
            ...state,
            experiments: payload,
            status: 'loaded' as const
        })),
        on(experimentsListEvents.listFailed, (_event, state) => ({
            ...state,
            status: 'error' as const
        }))
    ),
    withEventHandlers(() => {
        const events = inject(Events);
        const dispatcher = inject(Dispatcher);
        const service = inject(DotExperimentsService);
        const httpErrorManager = inject(DotHttpErrorManagerService);

        return {
            loadList$: events.on(experimentsListEvents.listRequested).pipe(
                switchMap(() =>
                    service.getAll().pipe(
                        tapResponse({
                            next: (experiments) =>
                                dispatcher.dispatch(
                                    experimentsListEvents.listSucceeded(experiments)
                                ),
                            error: (error: HttpErrorResponse) => {
                                httpErrorManager.handle(error);
                                dispatcher.dispatch(experimentsListEvents.listFailed(error));
                            }
                        })
                    )
                )
            )
        };
    })
);

// component
readonly #dispatch = injectDispatch(experimentsListEvents);
```

### Reference implementations

- `libs/image-editor/src/lib/store/` — feature-sliced: `image-editor.events.ts` + `features/with-*.feature.ts`
- `libs/portlets/dot-experiments/portlet/src/lib/site-wide/store/` — single-store list example

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
4. **tsconfig.spec.json**: add `isolatedModules: true` in transform options (required for transitive deps)
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

- **`dot-tags`** — Canonical reference for the standard CRUD pattern (modal dialogs, SignalStore)
- **`dot-experiments`** — Full CRUD with guards, resolvers, shell, routed create/edit
- **`dot-analytics`** — Enterprise license checking, lazy loading
- **`dot-content-drive`** — Complex nested routing, reference for test config
- **`edit-ema/portlet`** — Reference for complex portlets: feature slice decomposition, Container/Presentational pattern, flat prefixed state
- **`dot-locales`** — Legacy pattern only (uses `ComponentStore`). Do not use as a model for new work
