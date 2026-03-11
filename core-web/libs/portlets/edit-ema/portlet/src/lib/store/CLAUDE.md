# UVEStore — Architecture & Scaling Rules

## Structure

The store is built with `@ngrx/signals` and split into feature slices using `signalStoreFeature`.

```
store/
├── dot-uve.store.ts          # Root store — composes all features
├── models.ts                 # UVEState and top-level types
└── features/
    ├── withPageContext.ts     # Shared computed hub (loaded first)
    ├── editor/
    │   ├── withEditor.ts     # Editor UI state and methods
    │   ├── withLock.ts       # Lock management
    │   ├── save/withSave.ts  # Save operations
    │   └── toolbar/withUVEToolbar.ts
    ├── flags/withFlags.ts    # Feature flag signals
    ├── layout/withLayout.ts  # Layout tab computeds
    ├── track/withTrack.ts    # Analytics tracking
    ├── workflow/withWorkflow.ts
    ├── timeMachine/withTimeMachine.ts
    ├── client/withClient.ts
    └── load/withLoad.ts
```

## State Design Rules

### Flat State Structure (Non-Negotiable)

NgRx Signal Store wraps each root-level property in its own Signal. Nested objects break fine-grained reactivity.

```typescript
// ✅ CORRECT: Flat with prefixes
interface UVEState {
  uveStatus: UVE_STATUS;
  pageParams: DotPageAssetParams | null;
  editorDragItem: EmaDragItem | null;
  viewZoomLevel: number;
}

// ❌ WRONG: Nested objects break reactivity
interface UVEState {
  uve: { status: UVE_STATUS };
  editor: { panels: { palette: boolean } };
}
```

### Domain Prefixes (Required)

All state properties must use a domain prefix matching their feature file:

| Prefix | Domain |
|--------|--------|
| `uve*` | Global editor system (status, user, enterprise) |
| `page*` | Page content and metadata |
| `workflow*` | Workflow and lock state |
| `editor*` | Editor UI state |
| `view*` | View modes and preview |

Cross-domain state (a property with the wrong prefix for its feature) is not allowed.

### Property Naming Conventions

| Pattern | Example | Usage |
|---------|---------|-------|
| `{domain}{Property}` | `editorDragItem` | State property |
| `{domain}Can{Action}` | `editorCanEditContent()` | Boolean computed |
| `${domain}{Computed}` | `$showContentletControls()` | UI-specific computed |
| `{domain}{Action}` | `viewZoomIn()` | Action method |

---

## Key Conventions

**Computed signals are prefixed with `$`**
```ts
$isEditMode: computed(() => pageParams()?.mode === UVE_MODE.EDIT)
```

**Each feature exports a `with*()` function** using `signalStoreFeature` with explicit type constraints:
```ts
export function withLayout() {
    return signalStoreFeature(
        { state: type<UVEState>() },
        withComputed(...),
        withMethods(...)
    );
}
```

**`withPageContext` is the shared computed hub.** It is composed first in the root store, before any other feature. Features that need common computed values (mode, page lock, variant, etc.) declare `props: type<PageContextComputed>()` in their type constraints.

**State mutations go through `patchState` only.** Never mutate state directly inside `withComputed` or `withMethods`.

## Rules for Scaling

### 1. One domain per feature
Each `with*` function owns one domain (editor UI, layout, workflow, etc.). Do not add state or methods to an existing feature if they belong to a different concern — create a new feature instead.

### 2. Shared computeds belong in `withPageContext`
If a computed signal is needed by more than one feature, add it to `withPageContext` and expose it via `PageContextComputed`. Features consume it through `props: type<PageContextComputed>()`. Do not duplicate computed logic across features.

### 3. Feature-local types live next to the feature
Each feature keeps its own `models.ts` for state shape, prop types, and enums. Do not import feature-local types into `store/models.ts` — that file is for `UVEState` only.

### 4. Declare type constraints explicitly
Always declare `{ state: type<UVEState>(), props: type<PageContextComputed>() }` at the top of `signalStoreFeature`. This enforces that the feature is only composed in the right context and makes dependencies explicit and compiler-checked.

### 5. Never add logic to the root store
`dot-uve.store.ts` is a composition file only. It holds `initialState`, wires features together, and adds top-level computeds that genuinely span multiple features (e.g. `$shellProps`). Business logic goes inside the feature, not the root.

### 6. Sub-features for nested concerns
Features can nest sub-features (e.g. `withEditor` composes `withUVEToolbar`). Use this when a feature grows large enough that its computeds or methods split into a clearly separate sub-concern. Keep nesting shallow (max 2 levels).

### 7. `untracked` for side-effect reads
When a `computed` or method needs to read a signal without creating a reactive dependency, use `untracked()`. Only do this when you have a deliberate reason to break the dependency chain — document why inline.

### 8. `protectedState: false` is temporary
The root store has `{ protectedState: false }` to allow state access in unit tests. This must be removed once the tests are updated to use proper store testing patterns. Do not add new tests that rely on direct state mutation.

---

## Patterns in Use

### Accessing State

Always use computed signals — they are the public API. Never reach into internal raw state.

```typescript
// ✅ Use computeds
const page = store.pageData();
const canEdit = store.editorCanEditContent();

// ❌ Do not access internal state signals directly
const page = store.pageAssetResponse()?.pageAsset.page;
```

### `rxMethod` for async operations
All async flows (`loadPageAsset`, `reloadCurrentPage`, `savePage`, `getWorkflowActions`, `trackUVEModeChange`) are wrapped in `rxMethod`. It binds an RxJS pipeline to the store lifecycle and handles cancellation automatically. Use it for any method that triggers an HTTP call or a side-effect stream.

```ts
loadPageAsset: rxMethod<Partial<DotPageAssetParams>>(
    pipe(
        tap(() => patchState(store, { status: UVE_STATUS.LOADING })),
        switchMap((params) => dotPageApiService.get(params).pipe(...))
    )
)
```

### Optimistic update + rollback via `withTimeMachine`
`withClient` saves a GraphQL response snapshot before mutating it (`setGraphqlResponseOptimistic`). On save failure, `rollbackGraphqlResponse` restores the previous snapshot using `withTimeMachine`. This is the only place `withTimeMachine` is used — it was designed as a generic undo/redo primitive.

### Implicit feature composition chain
`withLoad` internally composes `withClient` and `withWorkflow`. `withSave` internally composes `withLoad`. The root store only calls `withSave`, but it silently gets the full chain. This is intentional but invisible from the root store.

```
withSave → withLoad → withClient
                    → withWorkflow
```

### Split `withMethods` blocks for DI ordering
`withLoad` uses two `withMethods` calls: the first adds `updatePageParams` (no DI needed), the second injects all services. This is a workaround for ngrx/signals requiring methods to be available before they are used inside other methods. Do not collapse them into one block.

### Feature flag signals as a typed map
`withFlags` fetches all feature flags from `DotPropertiesService` once and exposes them as a `flags()` signal: a strongly-typed record `UVEFlags`. Consuming features read individual flags via `flags().FEATURE_FLAG_*`. Never inject `DotPropertiesService` inside other features to read flags — always go through `flags()`.

### Debounced analytics
`withTrack` wraps tracking calls in `DEBOUNCE_FOR_TRACKING` (5000ms) to avoid noise on rapid state changes. Always apply this wrapper to new tracking methods — raw analytics events on every signal change will flood the analytics backend.

### `forkJoin` for parallel page bootstrap
`loadPageAsset` uses two nested `forkJoin` blocks: the first fetches `pageAsset + isEnterprise + currentUser` in parallel, the second (inside the first's `switchMap`) fetches `experiment + languages` in parallel. Each has its own `catchError`. Do not flatten these into sequential calls.

### `tapResponse` for RxJS next/error handling
`withWorkflow` uses `tapResponse` instead of `tap` + `catchError` when both the success and error paths need clean inline handling inside a pipeline. Use `tapResponse` when you want to handle both cases close together without breaking the observable chain.

### `new String('')` for forced iframe refresh
In `withEditor.$iframeURL`, TRADITIONAL pages return `new String('')` (a String object, not a primitive) instead of a plain `''`. This forces Angular to treat it as a new reference on every recompute, triggering an iframe reload. HEADLESS pages build a full URL with `clientHost`. This distinction is intentional — do not normalize to a plain string.

### Device and SEO mode are mutually exclusive
`withView` enforces that device preview and SEO social-media preview cannot be active at the same time. `viewSetDevice()` clears `viewSocialMedia`; `viewSetSEO()` clears `viewDevice`. When adding new preview modes, maintain this mutual exclusion via the same clear-on-set pattern.

### Type assertion for cross-feature method access
Features that depend on methods from sibling features (not yet in scope at composition time) use a typed cast: `store as StoreWithDeps<typeof store>`. This is only acceptable for documented circular or out-of-order dependencies. Always define a dedicated `StoreWith*Deps` interface — do not use `as any` except for the documented `withWorkflow` ↔ `withPageApi` circular case.

---

## Things to Improve

### 1. `protectedState: false` (tracked: test infrastructure)
Root store disables state protection so tests can directly mutate state. Every new test written against this is technical debt. Fix: migrate tests to use `patchState` or store method calls, then remove this flag.

### 2. Vanity URL redirect inside `withLoad` (tracked: existing TODO in code)
The `router.navigate()` call in `loadPageAsset` is inside the store. The comment already says it should move to a Shell component effect. Store features should not navigate — they should expose an event or computed that the component reacts to.

### 3. `isEnterprise` and `currentUser` re-fetched on every page load
These are session-level facts fetched inside `loadPageAsset`'s `forkJoin`. Every page navigation re-fetches them. The comment in the code references issue #30760 and suggests moving this to an `onInit` lifecycle hook. Until then, every soft navigation pays an unnecessary HTTP round-trip.

### 4. `$shellProps` computed is too large for the root store
The root store's `$shellProps` contains the full navigation bar items array with all conditional business logic (enterprise check, layout disabled, rules visibility, experiments). This belongs in a dedicated `withShell` feature, not in the composition root.

### 5. `saveStyleEditor` does not use `rxMethod`
Unlike `savePage`, `saveStyleEditor` returns a raw `Observable` that the caller must subscribe to. This breaks the uniform pattern and shifts error handling responsibility to the component. It should be converted to `rxMethod`.

### 6. Dead reactive dependency in `$editorProps`
Inside `withEditor`, `$editorProps` calls `store.pageAPIResponse()` at the top of the computed body without using the return value — just to create a reactive dependency. The inline comment says "need more testing before removing this dependency." This is a smell: computed dependencies should be explicit and intentional.

### 7. `isTraditionalPage` detection is an implicit convention
`isTraditionalPage` is set as `!pageParams.clientHost` — a boolean derived from the absence of a URL parameter. There is no explicit type guard or enum. If the convention ever changes, this silently breaks every feature that branches on it.

### 8. `console.error` / `console.warn` scattered across features
Error handling across `withLoad`, `withSave`, `withWorkflow`, and `withClient` logs directly to the console with no consistent strategy. A store-level error handler or error bus would centralize this.

### 9. Double `withFlags` composition
`withFlags(UVE_FEATURE_FLAGS)` is called inside `withPageContext` **and** again in the root store (`dot-uve.store.ts`). The root store call is redundant and confusing — `withPageContext` already includes it.

### 10. `reloadPageAfterLockChange()` uses raw `.subscribe()`
Inside `withWorkflow`, the method that re-fetches the page after a lock/unlock uses a manual `.subscribe()` call instead of `rxMethod`. There is no automatic cancellation — if the user navigates away mid-lock, the subscription keeps running. Fix: convert to `rxMethod` or add `takeUntilDestroyed()`.

### 11. Lock/unlock concern mixed into `withWorkflow`
`withWorkflow` manages two unrelated things: workflow action fetching (`DotWorkflowsActionsService`) and page lock toggling (`DotContentletLockerService`). The feature's own comments flag this. Lock state and behavior belong in a dedicated feature that focuses only on lock ownership and transitions.

### 12. `withTimeMachine` is unused
A generic undo/redo feature exists at `features/timeMachine/withTimeMachine.ts` and is never composed into the store. `withPage` uses `withHistory` instead, which has nearly identical logic. Either delete `withTimeMachine` or replace `withHistory` with it. Having two implementations of the same primitive is maintenance debt.

### 13. Commented-out permission logic in `withEditor`
A block of style-editor permission logic in `withEditor` is commented out with no explanation of whether it is WIP or abandoned. It adds noise and creates ambiguity about the intended permission model. Delete it if it is dead code, or activate and document it if it is needed.

---

## Next Move

These are ordered by impact-to-effort ratio:

1. **Remove the redundant `withFlags` in the root store.** One-line deletion, no behavior change, reduces confusion.

2. **Convert `saveStyleEditor` to `rxMethod`.** Straightforward refactor. Removes component-side subscription responsibility and aligns with the `savePage` pattern.

3. **Extract `$shellProps` nav items into a `withShell` feature.** The root store should be a thin composition file. This is the largest source of business logic leaking into the root.

4. **Move the vanity URL redirect to the Shell component.** Replace the `router.navigate` inside `withLoad` with a dedicated computed or event that the component listens to. This is already documented as a TODO — execute it.

5. **Fix `protectedState: false`.** Requires updating test setup. High effort but it's the foundation for reliable signal store testing going forward.

6. **Hoist `isEnterprise` and `currentUser` to `onInit`.** Requires the ngrx/signals lifecycle hook support (issue #30760). When that ships, move these out of `loadPageAsset` to eliminate the per-navigation round-trips.

7. **Extract lock/unlock to a dedicated `withLock` feature.** The code and its own comments already say this should not live inside `withWorkflow`. Separate the two services (`DotWorkflowsActionsService` vs `DotContentletLockerService`) into their own features for single-responsibility compliance.

8. **Convert `reloadPageAfterLockChange()` to `rxMethod`.** The raw `.subscribe()` inside `withWorkflow` has no cancellation. It should use `rxMethod` like every other async operation in the store.

9. **Delete `withTimeMachine` or replace `withHistory` with it.** Two nearly identical undo/redo implementations exist. Pick one and remove the other. `withTimeMachine` has better JSDoc but is unused; `withHistory` is actively composed. Consolidate before the divergence grows.
