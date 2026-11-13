# Data Model: title status, preview rule, search scope

No persisted data. These are in-memory UI models.

## DotTitleStatus (`libs/dotcms-models`)

```
DotTitleStatus = null | { state: DotContentState | null }
```

| Value | Meaning | Rendered as |
|-------|---------|-------------|
| `null` | nothing to show (loading, historical version, any non-Edit-Content page) | no chip |
| `{ state: null }` | unsaved content | badge "New" |
| `{ state: DotContentState }` | existing content | badge derived from `live`/`working`/`archived`/`deleted`/`hasLiveVersion` (Published, Draft, Archived, Revision) |

**Derivation (edit-content store, `titleStatus`)**:

| Editor condition | Result |
|------------------|--------|
| loading or reloading with no loaded content | `null` |
| viewing a historical version | `null` |
| new content (`initialContentletState === 'new'`) | `{ state: null }` |
| otherwise | `{ state: contentlet }` |

**Lifecycle**: set by the host while Edit Content is mounted; cleared when the editor is destroyed
and, for the global header, on any navigation away from Edit Content.

## Preview rule (`showPreviewLink`, edit-content store)

True when: content is not new, not a historical version, and (base type is HTML page or the
contentlet has a URL map). Click opens `generatePageEditUrl` (HTML page) or `generatePreviewUrl`
(otherwise) in a new tab; a missing URL logs the existing warning and does nothing.

## DotSearchScope (`libs/dotcms-models`)

```
DOT_SEARCH_SCOPE = { TITLE: 'TITLE', ALL_FIELDS: 'ALL_FIELDS' } as const
DotSearchScope   = 'TITLE' | 'ALL_FIELDS'
DEFAULT          = TITLE
```

Held by each surface: Content Drive in its URL/filter state (stored only when not default); the
picker in its filter bag under `searchScope` (stored only when `ALL_FIELDS`). Sent as
`filters.searchScope` only when a term is present.

## State transitions

- Picker: open → scope Title (absent key); user picks All Fields → `searchScope: 'ALL_FIELDS'`,
  paging reset, search re-runs if a term exists; Clear filters → key removed (Title); close → state
  discarded.
- Title status: loading (`null`) → loaded (`{ state }`) → workflow action (`{ state }` updated) →
  historical view (`null`) → exit (`{ state }`).
