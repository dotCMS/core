# Contract: `dot-search-scope-input` (shared, `libs/ui`)

Presentational search box with a Title / All Fields scope dropdown, extracted from Content Drive.

| Member | Type | Notes |
|--------|------|-------|
| `term` (input) | `string` | current term, owned by the host |
| `scope` (input) | `DotSearchScope` | active scope, owned by the host |
| `testId` (input) | `string` | `data-testid` of the text box; defaults to `search-input-field` (the picker passes `asset-picker-search-input`) |
| `search` (output) | `string` | debounced term (same debounce as `dot-search-input`) |
| `scopeChange` (output) | `DotSearchScope` | emitted only when the picked scope differs from `scope`; a toggle-off (`null`) from the listbox is ignored |
| `focus()` | method | focuses the text box (hosts bind their own shortcuts) |

**Behavior**: input group of search box + trigger button showing the active label; popover with a
listbox (checkmark on active option, tooltip on each option using the existing
`content-drive.search.scope.*` messages); closing the popover hides leftover tooltips; the
placeholder stays the shared "Search".

**Must not**: inject the Router, `DotContentDriveStore`, or the keyboard shortcut service; no URL access.

**Hosts**: Content Drive (wrapper keeps store calls and shortcuts) and the asset picker toolbar
(keeps its shortcuts and the Upload button).

**Test ids** preserved from Content Drive: `search-scope-trigger`, `search-scope-active`,
`search-scope-panel`, `search-scope-option-TITLE|ALL_FIELDS`; the picker keeps
`asset-picker-search-input` on the text box.

## Request contract (unchanged server API)

`DotContentDriveSearchRequest.filters.searchScope?: 'TITLE' | 'ALL_FIELDS'`, sent by the picker only
when a term is present.
