# Edit EMA Portlet

Universal Visual Editor (UVE) portlet for dotCMS - enables in-context editing of pages with drag-and-drop content management.

## Architecture

This portlet follows a **Container/Presentational Component Pattern** to maintain clear separation of concerns:

- **Smart Containers**: Inject `UVEStore`, manage state, pass data down via `@Input`
- **Presentational Components**: No store injection, receive all data via `@Input`, emit events via `@Output`

### Component Tree

```
DotEmaShellComponent (Smart Container) [UVEStore]
├─ EditEmaNavigationBarComponent (Presentational) [NO STORE]
│  └─ Receives navigation data via @Input
│
├─ Route → EditEmaEditorComponent (Smart Container) [UVEStore]
   ├─ DotUveToolbarComponent (Smart Container) [UVEStore]
   │  ├─ DotToggleLockButtonComponent (Presentational) [NO STORE]
   │  │  └─ @Input: isLocked, isDisabled, loading | @Output: toggle
   │  ├─ DotEmaInfoDisplayComponent (Presentational) [NO STORE]
   │  │  └─ @Input: apiUrl, currentLanguage | @Output: openInfo
   │  ├─ DotUveDeviceSelectorComponent (Presentational) [NO STORE]
   │  │  └─ @Input: value, options | @Output: selected
   │  ├─ EditEmaLanguageSelectorComponent (Presentational) [NO STORE]
   │  │  └─ @Input: contentLanguageId, pageLanguageId | @Output: change
   │  └─ ... (all toolbar children are presentational)
   │
   ├─ DotUvePaletteComponent (Smart Container) [UVEStore]
   │  ├─ Uses local signalState for tab management (not global store)
   │  └─ DotUvePaletteListComponent (Presentational) [NO STORE]
   │     └─ @Input: listType, languageId, pagePath, variantId
   │
   ├─ DotUveContentletQuickEditComponent (Presentational) [NO STORE]
   │  └─ @Input: data (ContentletEditData), loading | @Output: submit, cancel
   │  └─ Dynamic form generation based on field types
   │
   ├─ EmaPageDropzoneComponent (Presentational) [NO STORE]
   │  └─ Pure canvas for drag-and-drop operations
   │
   └─ DotUveLockOverlayComponent (Presentational) [NO STORE]
      └─ @Input: isLocked, lockedByUser | @Output: unlock
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

### Container Component Pattern

```typescript
// Smart Container (reads from store, manages state)
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

### Presentational Component Pattern

```typescript
// Presentational (receives props, emits events)
@Component({ selector: 'dot-uve-palette-list' })
export class DotUvePaletteListComponent {
    // NO store injection!

    // Inputs (data down from parent container)
    $languageId = input.required<number>({ alias: 'languageId' });
    $pagePath = input.required<string>({ alias: 'pagePath' });

    // Outputs (events up to parent container)
    onSelect = output<string>();
}
```

## Benefits

### Testability
- Presentational components are easier to test (no mock store needed)
- Container components have clear boundaries (test store interactions separately)

### Reusability
- Presentational components can be reused in different contexts
- No coupling to specific store implementation

### Maintainability
- Clear separation of concerns (smart vs dumb components)
- Store changes only affect container components
- Easier to refactor and optimize

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
