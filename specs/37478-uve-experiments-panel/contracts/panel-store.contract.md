# Contract: `DotExperimentsPanelStore`

**Consumers**: the UVE shell (provides + opens/closes), the panel component (renders), the three
experiments screens and their stores (read it to know they are in a panel).

This is the one new public surface this work adds. It is the *only* thing `edit-ema` learns about
experiments beyond the component symbol, and it is the *only* thing the experiments screens learn
about the editor.

**Location**: `libs/portlets/dot-experiments/portlet/src/lib/store/dot-experiments-panel.store.ts`,
exported from `@dotcms/portlets/dot-experiments/portlet`.

**Provided by**: `DotEmaShellComponent` (`providers: [DotExperimentsPanelStore]`) — shell scope,
so it outlives the panel across the variant round trip and dies with the editor.

---

## Inputs the shell must supply

Set once, at provide time, from the shell's own signals. **Signals, not values** — a captured
page or language goes stale against the canvas (FR-034, FR-034a, D10).

```ts
setContext({
    pageId:     Signal<string | null>,   // uveStore.pageAsset()?.page?.identifier
    languageId: Signal<number | null>,   // uveStore.pageLanguageId()
}): void
```

| Rule | Requirement |
|---|---|
| A change in `pageId` re-scopes: view → `list`, `experimentId` → `null`, list view state reset, refetch. | FR-034, FR-034b |
| A change in `languageId` changes nothing observable. No refetch, no reset. | FR-034a |

## Read surface

| Member | Type | Contract |
|---|---|---|
| `isOpen` | `Signal<boolean>` | The shell's `@if` binds to this. |
| `view` | `Signal<'list' \| 'create' \| 'configure' \| 'results'>` | Exactly one at a time. |
| `experimentId` | `Signal<string \| null>` | Non-null iff `view` is `configure` or `results`. |
| `pageId` | `Signal<string \| null>` | The page in hand. Read live. |
| `languageId` | `Signal<number \| null>` | Return context only. Never narrows, never saved. |

## Write surface

| Method | Effect | Requirement |
|---|---|---|
| `open()` | `isOpen = true`, `view = 'list'`, `experimentId = null`. Always the list, whatever the page has. | FR-001, FR-006, FR-006a, D11 |
| `openResults(experimentId)` | `isOpen = true`, `view = 'results'`. Replaces whatever was showing; never opens a second surface. | FR-025c, Edge Cases |
| `showCreate()` | `view = 'create'`. | FR-007 |
| `showConfigure(experimentId)` | `view = 'configure'`. | FR-008, FR-016 |
| `showResults(experimentId)` | `view = 'results'`. | FR-025a, FR-025b |
| `backToList()` | `view = 'list'`, `experimentId = null`. | FR-018 (US4 scenario 3) |
| `close()` | Reset to initial state. Discards everything. | FR-003, FR-039, SC-016 |
| `suspendForVariant()` | `isOpen = false`, `suspendedForVariant = true`, `view`/`experimentId` **retained**. | FR-021, FR-025, D13 |
| `resumeFromVariant()` | `isOpen = true`, `suspendedForVariant = false`, `view`/`experimentId` restored. No-op when not suspended. | FR-023 |

### Guarantees this contract makes

1. **Nothing here touches the router, `Location`, or `window.history`.** Not the fact it is open,
   not the view, not the experiment. Verifiable by grep and by test. — FR-002, FR-031, FR-035
2. **`close()` and `suspendForVariant()` are not the same operation** and must not be collapsed
   into one with a flag. — FR-039 vs FR-025
3. `resumeFromVariant()` on a store that was never suspended does nothing rather than opening the
   list. A reload mid-trip therefore leaves the editor on the page, not on a phantom panel.
   — Edge Cases

---

## The mode contract, from a screen's point of view

Each screen and each screen store asks one question, one way:

```ts
readonly #panel = inject(DotExperimentsPanelStore, { optional: true });
```

| `#panel` | Meaning | Behaviour |
|---|---|---|
| `null` | Full-screen portlet, reached from the main navigation | **Everything exactly as today.** Address-backed view state, page column, seven-column table, breadcrumbs, `router.navigate` between screens. — FR-042 |
| non-null | Rendered in the panel | Address-free view state; page-scoped read; no page column; compact layout; no breadcrumb; navigation through the store above. |

**Branch inline at each concern.** There is no adapter, no strategy object and no second
implementation of any screen — that is what FR-040 and SC-015 require, and what D7 says the
failure mode is when it is ignored.
