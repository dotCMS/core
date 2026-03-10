# Edit EMA Portlet

Universal Visual Editor (UVE) portlet for dotCMS — enables in-context editing of pages with drag-and-drop content management.

## Architecture

This portlet follows a **Container/Presentational Component Pattern** to maintain clear separation of concerns:

- **Smart Containers**: Inject `UVEStore`, manage state, pass data down
- **Presentational Components**: No store injection, receive all data via `input()`, emit events via `output()`

### Component Tree

```
DotEmaShellComponent (Smart Container) [UVEStore]
├─ EditEmaNavigationBarComponent (Smart Container) [UVEStore]
│
└─ Route → EditEmaEditorComponent (Smart Container) [UVEStore]
   ├─ DotUveToolbarComponent (Smart Container) [UVEStore]
   │  ├─ DotToggleLockButtonComponent (Presentational)
   │  ├─ DotEmaInfoDisplayComponent (Presentational)
   │  ├─ DotUveDeviceSelectorComponent (Presentational)
   │  ├─ DotEditorModeSelectorComponent (Presentational)
   │  ├─ DotUveWorkflowActionsComponent (Presentational)
   │  ├─ DotEmaRunningExperimentComponent (Presentational)
   │  ├─ EditEmaPersonaSelectorComponent (Presentational)
   │  └─ DotEmaBookmarksComponent (Presentational)
   │
   ├─ DotUvePaletteComponent (Smart Container) [UVEStore]         [left panel]
   │  ├─ DotUvePaletteListComponent (Smart Container) [DotPaletteListStore]
   │  ├─ DotRowReorderComponent (Presentational)
   │  └─ DotUveStyleEditorFormComponent + DotUveStyleEditorEmptyStateComponent
   │
   ├─ Browser toolbar (inline in EditEmaEditorComponent)
   │  └─ DotUveZoomControlsComponent (Smart Container) [UVEStore]
   │
   ├─ DotUveIframeComponent (Presentational)
   ├─ DotUveContentletToolsComponent (Presentational)
   ├─ EmaPageDropzoneComponent (Presentational)
   ├─ DotUveLockOverlayComponent (Smart Container) [UVEStore]
   ├─ DotUvePageVersionNotFoundComponent (Presentational)
   │
   ├─ Right edit panel (inline, conditionally open)          [right panel]
   │  ├─ DotUveContentletQuickEditComponent (Presentational)
   │  └─ DotUveStyleEditorFormComponent (Presentational)
   │
   ├─ DotEditEmaDialogComponent (Smart Container)
   └─ DotBlockEditorSidebarComponent (Presentational)       [full-screen drawer]
```

## Key Patterns

### Local vs Global State

**Global Store State** (`UVEStore`):
- All shared feature state: page data, editor selections, view modes, workflow
- Cross-component coordination
- Example: `editorActiveContentlet`, `viewDevice`, `editorPaletteOpen`

**Feature Store State** (component-specific store):
- Domain-specific state for a feature (palette list data, search params, pagination)
- Encapsulated within a feature boundary
- Example: `DotPaletteListStore` for palette list management

### Smart Container Pattern

```typescript
@Component({ selector: 'dot-uve-palette' })
export class DotUvePaletteComponent {
    protected readonly uveStore = inject(UVEStore);

    // Read from global store
    readonly $languageId = computed(() => this.uveStore.$languageId());
    readonly $pagePath = computed(() => this.uveStore.$pageURI());
}
```

### Presentational Component Pattern

```typescript
@Component({ selector: 'dot-uve-contentlet-quick-edit' })
export class DotUveContentletQuickEditComponent {
    // NO store injection

    data = input.required<ContentletEditData>();
    loading = input<boolean>(false);

    submit = output<Record<string, unknown>>();
    cancel = output<void>();
}
```

## State Management Architecture

### UVEStore State Structure

The `UVEStore` uses a **flat state structure** following NgRx Signal Store best practices. Each property gets its own Signal for fine-grained reactivity. State is organized by domain using prefixes:

```typescript
interface UVEState {
    // uve* — Global editor system state (withUve)
    uveStatus: UVE_STATUS;
    uveCurrentUser: CurrentUser | null;

    // flags — Feature flags (withFlags)
    flags?: UVEFlags;

    // page* — Page asset data and metadata (withPage)
    pageParams: DotPageAssetParams | null;
    pageLanguages: DotLanguage[];
    pageType: PageType;           // TRADITIONAL | HEADLESS
    pageExperiment: DotExperiment | null;
    pageErrorCode: number | null;

    // workflow* — Workflow actions and lock state (withWorkflow)
    workflowActions: DotCMSWorkflowAction[];
    workflowIsLoading: boolean;
    workflowLockIsLoading: boolean;

    // editor* — Editor UI state (withEditor)
    editorDragItem: EmaDragItem | null;
    editorBounds: Container[];
    editorState: EDITOR_STATE;
    editorActiveContentlet: ActionPayload | null;
    editorContentArea: ContentletArea | null;
    editorPaletteOpen: boolean;
    editorEditPanelOpen: boolean;
    editorOgTags: SeoMetaTags | null;
    editorStyleSchemas: StyleEditorFormSchema[];

    // view* — View modes and preview state (withView)
    viewDevice: DotDeviceListItem | null;
    viewDeviceOrientation: Orientation | null;
    viewSocialMedia: string | null;
    viewParams: DotUveViewParams | null;
    viewOgTagsResults: SeoMetaTagsResult[] | null;
    viewZoomLevel: number;
    viewZoomIframeDocHeight: number;
}
```

### Accessing State

Each state property exposes its own Signal. Access them directly from the store:

```typescript
// Reading state
const paletteOpen = this.uveStore.editorPaletteOpen();
const device = this.uveStore.viewDevice();
const status = this.uveStore.uveStatus();

// Computed signals (derived state)
const canEdit = this.uveStore.editorCanEditContent();
const pageUrl = this.uveStore.$pageURL();
```

### Updating State

Use `patchState` with the flat property name — no nested spreading required:

```typescript
setPaletteOpen(open: boolean) {
    patchState(store, { editorPaletteOpen: open });
}

setViewDevice(device: DotDeviceListItem) {
    patchState(store, { viewDevice: device });
}
```

### Store Feature Decomposition

`UVEStore` is composed from focused feature slices, each owning a specific domain:

| Feature | File | Responsibility |
|---|---|---|
| `withUve` | `features/uve/` | System lifecycle, current user, init effects |
| `withFlags` | `features/flags/` | Feature flag loading and access |
| `withPage` | `features/page/` | Page asset, history, client config |
| `withPageApi` | `features/page-api/` | Backend interactions: load, reload, save |
| `withLayout` | `features/layout/` | Layout/template editing |
| `withWorkflow` | `features/workflow/` | Workflow actions, lock management |
| `withEditor` | `features/editor/` | Editor UI state, edit capabilities, drag/drop |
| `withHistory` | `features/history/` | Undo/redo history for optimistic updates |
| `withTimeMachine` | `features/timeMachine/` | Time-machine/snapshot functionality |
| `withTrack` | `features/track/` | Analytics tracking |

### Page Types

The store distinguishes two page types, which affect iframe behavior and API calls:

- **`PageType.TRADITIONAL`**: dotCMS server-side rendered pages. Iframe always reloads on changes.
- **`PageType.HEADLESS`**: External headless apps. Iframe sends a `CLIENT_READY` postMessage with optional GraphQL config. Page data is fetched via REST or GraphQL depending on client config.

## Running Tests

```bash
# Run all tests for this portlet
nx test portlets-edit-ema-portlet

# Run a specific test file
nx test portlets-edit-ema-portlet --testFile=path/to/test.spec.ts
```

## Development

- **Angular 19+** with standalone components
- **NgRx Signals** (`@ngrx/signals`) for state management
- **Modern Angular syntax**: `@if`, `@for`, `input()`, `output()`, `computed()`
- **PrimeNG 21** for UI components
- **Jest + Spectator** for testing
