# Edit EMA Portlet

Universal Visual Editor (UVE) portlet for dotCMS - enables in-context editing of pages with drag-and-drop content management.

## Architecture

This portlet follows a **Container/Presentational Component Pattern** to maintain clear separation of concerns:

- **Smart Containers**: Inject `UVEStore`, manage state, pass data down via `@Input`
- **Presentational Components**: No store injection, receive all data via `@Input`, emit events via `@Output`

### Component Tree

```
DotEmaShellComponent (Smart Container) [UVEStore]
├─ EditEmaNavigationBarComponent (Smart Container) [UVEStore]
│  └─ Receives navigation data via @Input + reads from UVEStore
│
├─ Route → EditEmaEditorComponent (Smart Container) [UVEStore]
   ├─ DotUveToolbarComponent (Smart Container) [UVEStore]
   │  ├─ DotToggleLockButtonComponent (Presentational) [NO STORE]
   │  │  └─ @Input: toggleLockOptions | @Output: toggleLockClick
   │  ├─ DotEmaInfoDisplayComponent (Presentational) [NO STORE]
   │  │  └─ @Input: options | @Output: actionClicked
   │  ├─ DotUveDeviceSelectorComponent (Presentational) [NO STORE]
   │  │  └─ @Input: state, devices | @Output: stateChange
   │  ├─ EditEmaLanguageSelectorComponent (Presentational) [NO STORE]
   │  │  └─ @Input: contentLanguageId, pageLanguageId | @Output: change
   │  └─ ... (most toolbar children are presentational)
   │
   ├─ DotUvePaletteComponent (Smart Container) [UVEStore]
   │  ├─ Uses local signalState for tab management (not global store)
   │  └─ DotUvePaletteListComponent (Smart Container) [DotPaletteListStore + GlobalStore]
   │     └─ @Input: listType, languageId, pagePath, variantId
   │     └─ Uses feature store for palette-specific state management
   │
   ├─ DotUveContentletQuickEditComponent (Presentational) [NO STORE]
   │  └─ @Input: data (ContentletEditData), loading | @Output: submit, cancel
   │  └─ Dynamic form generation based on field types
   │
   ├─ EmaPageDropzoneComponent (Presentational) [NO STORE]
   │  └─ @Input: containers, dragItem, zoomLevel
   │  └─ Pure canvas for drag-and-drop operations
   │
   └─ DotUveLockOverlayComponent (Smart Container) [UVEStore]
      └─ Reads toggle lock state directly from UVEStore
```

## Key Patterns

### Local vs Global State

**Local Component State** (use `signalState()`):
- UI-specific state (tab selection, form validation, etc.)
- State that doesn't need cross-component coordination
- Example: `DotUvePaletteComponent` tab management

**Global Store State** (use `UVEStore`):
- Shared feature state (page data, contentlet selection, etc.)
- Cross-component coordination required
- Example: `activeContentlet`, `pageAPIResponse`

**Feature Store State** (use component-specific store):
- Domain-specific state for a feature (palette list data, search params, pagination)
- Encapsulated within a feature boundary
- Example: `DotPaletteListStore` for palette list management

### Container Component Pattern (with UVEStore)

```typescript
// Smart Container (reads from global store, manages state)
@Component({ selector: 'dot-uve-palette' })
export class DotUvePaletteComponent {
    protected readonly uveStore = inject(UVEStore);

    // Read from global store
    readonly $languageId = computed(() => this.uveStore.$languageId());
    readonly $pagePath = computed(() => this.uveStore.$pageURI());

    // Local UI state
    readonly #localState = signalState({ currentTab: 0 });
}
```

### Container Component Pattern (with Feature Store)

```typescript
// Smart Container with feature store
@Component({ selector: 'dot-uve-palette-list' })
export class DotUvePaletteListComponent {
    readonly #paletteListStore = inject(DotPaletteListStore);
    readonly #globalStore = inject(GlobalStore);

    // Inputs from parent (for initialization)
    $type = input.required<DotUVEPaletteListTypes>({ alias: 'listType' });
    $languageId = input.required<number>({ alias: 'languageId' });

    // Read from feature store
    protected readonly $contenttypes = this.#paletteListStore.contenttypes;
    protected readonly $pagination = this.#paletteListStore.pagination;
}
```

### Presentational Component Pattern

```typescript
// Presentational (receives props, emits events, NO store injection)
@Component({ selector: 'dot-uve-contentlet-quick-edit' })
export class DotUveContentletQuickEditComponent {
    // NO store injection!

    // Inputs (data down from parent container)
    data = input.required<ContentletEditData>({ alias: 'data' });
    loading = input<boolean>(false, { alias: 'loading' });

    // Outputs (events up to parent container)
    submit = output<Record<string, unknown>>();
    cancel = output<void>();
}
```

## Benefits

### Testability
- **Presentational components**: Easier to test (no mock store needed, pure input/output testing)
- **Container components**: Clear boundaries (test store interactions separately)
- **Feature stores**: Isolated testing of domain logic separate from UI

### Reusability
- **Presentational components**: Can be reused in different contexts without store coupling
- **Feature stores**: Domain logic can be shared across multiple components
- **Clear interfaces**: Well-defined @Input/@Output contracts

### Maintainability
- **Clear separation of concerns**: Smart containers vs presentational components vs feature stores
- **Localized changes**:
  - Global store changes affect only global state consumers
  - Feature store changes affect only feature-specific components
  - Presentational component changes are isolated
- **Easier refactoring**: Components can be converted between patterns as needs evolve

## Running Tests

Run unit tests for this portlet:
```bash
nx test portlets-edit-ema-portlet
```

Run specific test file:
```bash
nx test portlets-edit-ema-portlet --testFile=path/to/test.spec.ts
```

## Development

This library uses:
- **Angular 19+** with standalone components
- **NgRx Signals** for state management
- **Modern Angular syntax**: `@if`, `@for`, `input()`, `output()`
- **PrimeNG** for UI components
- **Jest + Spectator** for testing
