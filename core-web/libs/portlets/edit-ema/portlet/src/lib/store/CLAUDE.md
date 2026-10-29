# UVEStore — Architecture & Scaling Rules

## Structure

The store is built with `@ngrx/signals` and split into feature slices using `signalStoreFeature`.

Features are listed in the order `dot-uve.store.ts` composes them, after `withState(initialState)`:

```
store/
├── dot-uve.store.ts                       # Root store — composes all features
├── models.ts                              # UVEState and top-level types
└── features/
    ├── uve/withUve.ts                     # Global editor system state (status, user)
    │                                      # + withFlags(UVE_FEATURE_FLAGS) from @dotcms/store, in the root
    ├── page/withPage.ts                   # Page data and the shared page context (PageComputed)
    ├── history/withHistory.ts             # Undo history, composed inside withPage
    ├── track/withTrack.ts                 # Analytics tracking
    ├── workflow/withWorkflow.ts           # Workflow actions and page lock
    ├── layout/withLayout.ts               # Layout tab computeds
    ├── editor/
    │   ├── toolbar/withView.ts            # View modes, device and SEO preview, zoom
    │   ├── withEditor.ts                  # Editor UI state and methods
    │   ├── withSelectionAnchor.ts         # Selection anchor (needs withEditor)
    │   └── save/withSave.ts               # Not composed: superseded by withPageApi
    ├── content-type-cache/withContentTypeCache.ts
    └── page-api/withPageApi.ts            # Load, reload and save; composed last through withFeature
```

The editor dialog (`dot-ema-dialog`, its `DotEmaDialogStore` and `DotEmaWorkflowActionsService`) was removed in #38303: the editor opens content through `@dotcms/edit-content`'s opener (`DotEditContentOpenerService`), in the side panel, and the store keeps no dialog state.

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

**`withPage` is the shared computed hub** (it absorbed the former `withPageContext`). It is composed before every feature that reads the page. Features that need common computed values (language, URI, variant, etc.) declare `props: type<PageComputed>()` in their type constraints.

**State mutations go through `patchState` only.** Never mutate state directly inside `withComputed` or `withMethods`.

## Rules for Scaling

### 1. One domain per feature
Each `with*` function owns one domain (editor UI, layout, workflow, etc.). Do not add state or methods to an existing feature if they belong to a different concern — create a new feature instead.

### 2. Shared computeds belong in `withPage`
If a computed signal is needed by more than one feature, add it to `withPage` and expose it via `PageComputed`. Features consume it through `props: type<PageComputed>()`. Do not duplicate computed logic across features.

### 3. Feature-local types live next to the feature
Each feature keeps its own `models.ts` for state shape, prop types, and enums. Do not import feature-local types into `store/models.ts` — that file is for `UVEState` only.

### 4. Declare type constraints explicitly
Always declare `{ state: type<UVEState>(), props: type<PageComputed>() }` at the top of `signalStoreFeature`. This enforces that the feature is only composed in the right context and makes dependencies explicit and compiler-checked.

### 5. Never add logic to the root store
`dot-uve.store.ts` is a composition file only. It holds `initialState`, wires features together, and adds top-level computeds or helpers that genuinely span multiple features. Business logic goes inside the feature, not the root.

### 6. Sub-features for nested concerns
Features can nest sub-features (e.g. `withPage` composes `withHistory`). Use this when a feature grows large enough that its computeds or methods split into a clearly separate sub-concern. Keep nesting shallow (max 2 levels).

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
const page = store.pageAsset();
const canEdit = store.editorCanEditContent();

// ❌ Do not access internal state signals directly
const page = store.pageAssetResponse()?.pageAsset.page;
```

### `rxMethod` for async operations
The async flows in `withPageApi` (`pageLoad`, `pageReload`, `editorSave`, `updateRows`) are wrapped in `rxMethod`. It binds an RxJS pipeline to the store lifecycle and handles cancellation automatically. Use it for any method that triggers an HTTP call or a side-effect stream.

```ts
pageLoad: rxMethod<Partial<DotPageAssetParams>>(
    pipe(
        tap(() => patchState(store, { status: UVE_STATUS.LOADING })),
        switchMap((params) => dotPageApiService.get(params).pipe(...))
    )
)
```

### Optimistic update + rollback via `rollbackPageAssetResponse`
`saveStyleEditor` in `withPageApi` expects the page asset to be updated optimistically before it is called. On save failure it calls `rollbackPageAssetResponse()` (from `withPage`, backed by `withHistory`) to restore the previous page asset. `withClient` and `withTimeMachine`, which used to do this, no longer exist.

### `withPageApi` is composed last, with its dependencies passed in
`withPageApi` consolidates what `withLoad` and `withSave` used to do. The root store composes it last through `withFeature`, handing it an explicit object of what it needs from the features above (client configuration, request metadata, page asset and history management, page access), rather than relying on an implicit composition chain.

### Split `withMethods` blocks for DI ordering
`withPageApi` uses two `withMethods` calls: the first adds `pageUpdateParams`, the second injects the API services and defines the methods that call it. This is a workaround for ngrx/signals requiring methods to be available before they are used inside other methods. Do not collapse them into one block.

### Feature flag signals as a typed map
`withFlags` fetches all feature flags from `DotPropertiesService` once and exposes them as a `flags()` signal, typed from `UVE_FEATURE_FLAGS` (must be declared `as const` for the narrowing to apply). Consuming features read individual flags via `flags()[FeaturedFlags.FEATURE_FLAG_*]`. Never inject `DotPropertiesService` inside other features to read flags — always go through `flags()`.

### Debounced analytics
`withTrack` wraps tracking calls in `DEBOUNCE_FOR_TRACKING` (5000ms) to avoid noise on rapid state changes. Always apply this wrapper to new tracking methods — raw analytics events on every signal change will flood the analytics backend.

### `forkJoin` for parallel page bootstrap
`pageLoad` fetches the page asset, then, inside its `switchMap`, fetches `experiment + languages` in parallel with one `forkJoin` that has its own `catchError`. Do not flatten these into sequential calls.

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

### 2. Vanity URL redirect inside `withPageApi` (tracked: existing TODO in code)
The `router.navigate()` call in `pageLoad` is inside the store. The comment already says it should move to a Shell component effect. Store features should not navigate — they should expose an event or computed that the component reacts to.

### 3. `isEnterprise` and `currentUser` re-fetched on every page load
**Resolved.** The store no longer fetches either per load: `withUve` takes the current user from the `GlobalStore`, and `isEnterprise` is no longer read by the store.

### 4. `$shellProps` computed is too large for the root store
**Resolved.** `$shellProps` is no longer in the store.

### 5. `saveStyleEditor` does not use `rxMethod`
Unlike `savePage`, `saveStyleEditor` returns a raw `Observable` that the caller must subscribe to. This breaks the uniform pattern and shifts error handling responsibility to the component. It should be converted to `rxMethod`.

### 6. Dead reactive dependency in `$editorProps`
**Resolved.** `$editorProps` is no longer in `withEditor`.

### 7. `isTraditionalPage` detection is an implicit convention
`isTraditionalPage` is set as `!pageParams.clientHost` — a boolean derived from the absence of a URL parameter. There is no explicit type guard or enum. If the convention ever changes, this silently breaks every feature that branches on it.

### 8. `console.error` / `console.warn` scattered across features
Error handling across `withPageApi` and `withContentTypeCache` logs directly to the console with no consistent strategy. A store-level error handler or error bus would centralize this.

### 9. Double `withFlags` composition
**Resolved.** `withFlags(UVE_FEATURE_FLAGS)` is composed once, in the root store; `withPageContext`, which also composed it, is gone.

### 10. `reloadPageAfterLockChange()` uses raw `.subscribe()`
Inside `withWorkflow`, the method that re-fetches the page after a lock/unlock uses a manual `.subscribe()` call instead of `rxMethod`. There is no automatic cancellation — if the user navigates away mid-lock, the subscription keeps running. Fix: convert to `rxMethod` or add `takeUntilDestroyed()`.

### 11. Lock/unlock concern mixed into `withWorkflow`
`withWorkflow` manages two unrelated things: workflow action fetching (`DotWorkflowsActionsService`) and page lock toggling (`DotContentletLockerService`). The feature's own comments flag this. Lock state and behavior belong in a dedicated feature that focuses only on lock ownership and transitions.

### 12. `withTimeMachine` is unused
**Resolved.** `withTimeMachine` was deleted; `withHistory`, composed in `withPage`, is the only undo implementation. The uncomposed `features/editor/save/withSave.ts` is the same kind of leftover.

### 13. Commented-out permission logic in `withEditor`
A block of style-editor permission logic in `withEditor` is commented out with no explanation of whether it is WIP or abandoned. It adds noise and creates ambiguity about the intended permission model. Delete it if it is dead code, or activate and document it if it is needed.

---

## Next Move

These are ordered by impact-to-effort ratio:

1. ~~**Remove the redundant `withFlags` in the root store.**~~ Done: it is composed once.

2. **Convert `saveStyleEditor` to `rxMethod`.** Straightforward refactor. Removes component-side subscription responsibility and aligns with the `savePage` pattern.

3. ~~**Extract `$shellProps` nav items into a `withShell` feature.**~~ Done: `$shellProps` is no longer in the store.

4. **Move the vanity URL redirect to the Shell component.** Replace the `router.navigate` inside `withPageApi`'s `pageLoad` with a dedicated computed or event that the component listens to. This is already documented as a TODO — execute it.

5. **Fix `protectedState: false`.** Requires updating test setup. High effort but it's the foundation for reliable signal store testing going forward.

6. ~~**Hoist `isEnterprise` and `currentUser` to `onInit`.**~~ Done: neither is fetched per page load any more.

7. **Extract lock/unlock to a dedicated `withLock` feature.** The code and its own comments already say this should not live inside `withWorkflow`. Separate the two services (`DotWorkflowsActionsService` vs `DotContentletLockerService`) into their own features for single-responsibility compliance.

8. **Convert `reloadPageAfterLockChange()` to `rxMethod`.** The raw `.subscribe()` inside `withWorkflow` has no cancellation. It should use `rxMethod` like every other async operation in the store.

9. ~~**Delete `withTimeMachine` or replace `withHistory` with it.**~~ Done: `withTimeMachine` was deleted. The uncomposed `features/editor/save/withSave.ts` (it still imports the removed `withLoad`) can go the same way.
