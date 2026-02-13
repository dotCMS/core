# UVE Store Architecture

> NgRx Signal Store implementation for the Universal Visual Editor

## Core Principles

### 1. Flat State Structure (Non-Negotiable)

**Why**: NgRx Signal Store wraps each root-level property in its own Signal for fine-grained reactivity.

```typescript
// ✅ CORRECT: Flat with prefixes
interface UVEState {
  uveStatus: UVE_STATUS;
  uveCurrentUser: CurrentUser | null;

  pageParams: DotPageAssetParams | null;
  pageLanguages: DotLanguage[];

  editorDragItem: EmaDragItem | null;
  editorPaletteOpen: boolean;

  viewZoomLevel: number;
  viewZoomIsActive: boolean;
}

// ❌ WRONG: Nested objects break reactivity
interface UVEState {
  uve: { status: UVE_STATUS };  // Don't nest!
  editor: { panels: { palette: boolean } };  // Definitely don't!
}
```

### 2. Domain Prefixes (Required for Scaling)

All properties use domain prefixes to indicate ownership:

- `uve*` - Global editor system (status, user, enterprise)
- `page*` - Page content and metadata
- `workflow*` - Workflow and lock state
- `editor*` - Editor UI state
- `view*` - View modes and preview

**Why**: Clear ownership + scalability. Adding new properties is obvious: `editor*` for editor features, `view*` for view features, etc.

### 3. Single Responsibility per Feature

Each feature does **one thing**:

| Feature | Responsibility | State Count |
|---------|---------------|-------------|
| `withUve` | Global system state | 3 props |
| `withPage` | Page data + history | 9 props |
| `withWorkflow` | Workflow + lock | 3 props |
| `withEditor` | Editor UI | 9 props |
| `withView` | View modes + zoom | 8 props |
| `withPageApi` | Backend API calls | 0 props (orchestrates) |

**Why**: Easy to find where things belong. Need workflow state? → `withWorkflow`. Need page data? → `withPage`.

## Feature Composition Order

⚠️ **CRITICAL**: Features must be composed in this exact order due to dependencies.

```typescript
export const UVEStore = signalStore(
  { protectedState: false },

  withState<UVEState>(initialState),      // 1. Base state
  withUve(),                               // 2. System (status, user)
  withFlags(UVE_FEATURE_FLAGS),           // 3. Feature flags
  withPage(),                              // 4. Page data (composes withHistory)
  withTrack(),                             // 5. Analytics
  withWorkflow(),                          // 6. Workflow (⚠️ needs pageReload via type assertion)
  withMethods(...),                        // 7. Helper methods
  withLayout(),                            // 8. Layout operations
  withView(),                              // 9. View modes
  withEditor(),                            // 10. Editor UI
  withFeature((store) => withPageApi(...)) // 11. Backend API (must be last)
);
```

**Dependencies**:
- `withWorkflow` needs `pageData()` from `withPage` ✅
- `withWorkflow` needs `pageReload()` from `withPageApi` ⚠️ (circular - type assertion used)
- `withEditor` needs computeds from `withWorkflow`, `withView`, `withPage` ✅
- `withPageApi` needs methods from ALL features above ✅

**Circular Dependency**: `withWorkflow` ↔ `withPageApi`
- `withWorkflow` calls `(store as any).pageReload()` - type assertion required
- `withPageApi` receives `workflowFetch()` as dependency
- **Why acceptable**: Composition order can't change, runtime is safe, alternative just moves the problem

## Scaling Rules

### Adding New State

**Rule**: Match property prefix to domain, add to appropriate feature.

```typescript
// Need new editor state?
// → Add to withEditor with editor* prefix
interface EditorState {
  editorNewProperty: string;  // ✅ Correct prefix
}

// Need new page state?
// → Add to withPage with page* prefix
interface PageState {
  pageNewMetadata: any;  // ✅ Correct prefix
}
```

### Creating New Features

**When**: Only when you have 5+ related properties that form a clear domain.

**How**:
1. Create `features/{domain}/with{Domain}.ts`
2. Define state with `{domain}*` prefix
3. Export computeds and methods
4. Add to composition order (review dependencies!)

```typescript
// Example: withSearch feature
export function withSearch() {
  return signalStoreFeature(
    { state: type<UVEState>() },
    withState<{
      searchQuery: string;      // ✅ Prefix
      searchResults: Content[]; // ✅ Prefix
      searchIsLoading: boolean; // ✅ Prefix
    }>({ ... }),
    withComputed(...),
    withMethods(...)
  );
}
```

### Composition Patterns

**Reusable utilities** (like `withHistory`) use generic composition:

```typescript
// withHistory is composed INTO features, not added to main store
export function withPage() {
  return signalStoreFeature(
    withState<PageState>({ ... }),

    withHistory<PageAssetResponse>({  // ← Compose utility
      selector: (store) => store.pageAssetResponse(),
      maxHistory: 50
    }),

    withMethods(...)
  );
}
```

## Common Patterns

### Accessing State

```typescript
// ✅ Use computed signals
const page = store.pageData();
const canEdit = store.editorCanEditContent();

// ❌ Don't access internal state
const page = store.pageAssetResponse()?.pageAsset.page;  // Bad!
```

### Updating State

```typescript
// Simple update
store.setPaletteOpen(true);

// Optimistic update with history
store.setPageAssetResponseOptimistic(newResponse);

// Undo/redo
if (store.canUndo()) store.undo();
```

### Checking Permissions

```typescript
if (store.editorCanEditContent() && !store.workflowIsPageLocked()) {
  // Show edit UI
}
```

## Property Naming

| Pattern | Example | Usage |
|---------|---------|-------|
| `{domain}{Property}` | `editorDragItem` | State property |
| `{domain}Can{Action}` | `editorCanEditContent()` | Boolean computed |
| `${domain}{Computed}` | `$showContentletControls()` | UI-specific computed |
| `{domain}{Action}` | `viewZoomIn()` | Action method |

## References

- [NgRx Signal Store Docs](https://ngrx.io/guide/signals/signal-store)
- [Why Flat State](https://www.angulararchitects.io/blog/the-new-ngrx-signal-store-for-angular-2-1-flavors/)
- `/store/features/` - Feature implementations
- `/store/models.ts` - Complete state interface
