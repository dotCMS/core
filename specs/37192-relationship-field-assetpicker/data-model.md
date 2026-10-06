# Data Model: Relationship field picker + related-list (#37192)

**Date**: 2026-09-10 · **Plan**: [plan.md](./plan.md) · **Spec**: [spec.md](./spec.md)

No persisted data changes. Everything here is **client-side state**: what the picker and the field
hold while open, and the shapes that cross between them. Verified against `main` at `a4b54a947e`.

---

## 1. What the field hands the picker

Today the field passes `currentItemsIds: store.data().map(i => i.inode)` — identifiers only. The
picker then reconstructs the pre-selection by filtering its **first search response**, which is the
defect in research R7.

**The contract changes to pass the contentlets themselves.**

```ts
interface AddRelationshipsInput {
    /** The relationship's target content type. Pins every search (FR-004). */
    contentTypeId: string;

    /**
     * The field's currently related contentlets — the objects, not their ids.
     *
     * This is what makes FR-011 structural. The field already holds them in
     * `RelationshipFieldStore.data()`, so the picker never has to find them
     * again through a search that may not return them (ADR-0018: text search
     * is index-backed and lags).
     */
    selected: DotCMSContentlet[];

    /** 'single' | 'multiple' — from the field's cardinality (FR-007). */
    selectionMode: SelectionMode;

    /**
     * Edit-time constraint context. Absent for the filter-time consumer,
     * which is why these are optional — see contracts/relationship-picker.contract.md.
     */
    cardinality?: number;
    parentContentTypeId?: string;
    fieldVariable?: string;
    isParentField?: boolean;
    currentContentIdentifier?: string | null;

    /** Seeds the locale and site chips. Not a restriction — the editor can change both. */
    contentletContext?: ContentletContext;
}
```

**Output**: `DotCMSContentlet[]` — the confirmed set, in selection order. `[]` is a valid,
meaningful result (FR-013: it unrelates everything). Cancel yields `undefined`, which the field
distinguishes from `[]` and ignores.

> The `[]` vs `undefined` distinction is load-bearing and easy to lose. `[]` means "the editor
> confirmed an empty selection"; `undefined` means "the editor cancelled". Collapsing them either
> makes cancel destructive or makes FR-013 unreachable.

---

## 2. Picker state

```ts
interface AddRelationshipsState {
    /** Pinned target content type — never changes while the dialog is open. */
    contentTypeId: string;

    /** The current page of results from api/v1/drive/search. Server-paged (R7). */
    items: DotContentDriveBrowseItem[];

    /**
     * THE ACCUMULATED SELECTION (FR-011).
     *
     * A Map keyed by identifier, not an array and not a set of the visible rows.
     *
     * - Keyed by **identifier**, not inode: identifiers are stable across saves,
     *   inodes are not. The relationship field's own `refreshItem` already relies
     *   on this for the same reason.
     * - Holds the whole contentlet, so an item selected on page 1 can be returned
     *   at confirm without re-fetching it — which a server-paged list could not
     *   otherwise guarantee.
     * - Seeded from `AddRelationshipsInput.selected`, never from a search response.
     */
    selection: Map<string, DotCMSContentlet>;

    /** Filters, read and written only through DOT_FILTER_FACADE. */
    filters: DotAddRelationshipsFilters;

    /** Cursor paging, as api/v1/drive/search defines it. */
    page: { contentCursor: number; hasMore: boolean; limit: number };

    sort: { field: string; order: 'asc' | 'desc' };

    /** Identifiers already claimed by another parent — listed, not selectable (FR-008). */
    constrainedIdentifiers: Set<string>;

    /** Show-selected-only review (FR-014) — reads from `selection`, never from `items`. */
    viewMode: 'all' | 'selected';

    status: ComponentStatus;
}
```

### Why a `Map`, and why that is the whole feature

| Approach | What happens with server paging |
|---|---|
| Read the table's checked rows | Page 2 cannot see page 1's picks. Confirm drops them. **This is the bug FR-011 prevents.** |
| Array of contentlets | Works, but every add/remove is O(n) and de-duplication is manual. |
| **`Map<identifier, contentlet>`** | De-duplication is free, membership is O(1), the whole object is available at confirm, and "is this row checked?" is a lookup that does not care whether the row is on screen. |

### Derived

| Signal | Definition | Serves |
|---|---|---|
| `$selectedCount` | `selection.size` | Footer label |
| `$isSelected(id)` | `selection.has(id)` | Row checkbox state — **independent of the current page** |
| `$selectedItems` | `[...selection.values()]` | The "selected" review (FR-014) and the confirm payload |
| `$isConstrained(id)` | `constrainedIdentifiers.has(id)` | Row disabled state + tooltip (FR-008) |
| `$allVisibleSelected` | every selectable row in `items` is in `selection` | Header select-all, which operates on the visible page while preserving off-page picks |

### Transitions

| Action | Effect on `selection` |
|---|---|
| Open | Seeded from `input.selected`. **Never** from a search response. |
| Check a row | `set(identifier, contentlet)`. `'single'` mode clears first (FR-007). |
| Uncheck a row | `delete(identifier)` — this is what unrelates (FR-010). |
| Select-all on a page | Adds every selectable row **of that page**; off-page entries untouched. |
| Page / search / filter / sort | **Unchanged.** Only `items` and `page` move (FR-011). |
| Confirm | Emits `$selectedItems`. |
| Cancel | Discarded; emits `undefined`. |

---

## 3. Related-content list state (the form)

Changes to `RelationshipFieldState`:

```diff
- pagination: { offset: number; currentPage: number; rowsPerPage: number };
+ /**
+  * Rows currently rendered. NOT derived from `data` — deriving it collapses the
+  * table to the first page on every edit. `DotKeyValueComponent` learned this and
+  * records it; FR-023 restates it.
+  */
+ visibleCount: number;   // initial: RELATED_PAGE_SIZE (40)
```

| Removed | Replaced by |
|---|---|
| `totalPages` | — |
| `paginatedData` | `$visibleItems = data.slice(0, visibleCount)` |
| `nextPage`, `previousPage` | `loadMore()` → `visibleCount += 40` |
| `deleteItem`'s page-clamping branch | a plain filter |

**Unchanged and load-bearing**: `lastChangeSource: 'load' | 'user'`. A programmatic load must not
dirty the form, or the unsaved-changes guard fires on content the editor never touched (FR-034).
It is the easiest thing to break while deleting the paging state next to it.

Derived: `$remaining = max(0, data.length - visibleCount)`; the *Load more* row renders only when
`$remaining > 0` (FR-020).

---

## 4. Entities

| Entity | Identity | Notes |
|---|---|---|
| **Related content item** | `identifier` (stable) — `inode` changes on save | Title, optional thumbnail, locale, status, plus content-type-configured columns |
| **Constrained item** | `identifier` | Of the target type, already claimed by another parent through a one-parent relationship. Listed, never selectable |
| **Picker selection** | `Map<identifier, contentlet>` | §2. Survives paging, search and filters by construction |
| **Withheld row** | — | In the field's value, deliberately not in the DOM. Invisible to the editor, fully present to every operation on the value |

---

## 5. Invariants worth a test each

1. `selection` is never rebuilt from a search response. *(R7 — the defect being designed out.)*
2. Paging, searching, filtering and sorting leave `selection` byte-identical. *(FR-011)*
3. Confirm emits every entry in `selection`, including entries no search ever returned. *(FR-011)*
4. `[]` on confirm and `undefined` on cancel stay distinguishable. *(FR-013 vs FR-015)*
5. `visibleCount` never decreases as a side effect of an edit. *(FR-023)*
6. Every item in `data` is in the emitted value regardless of `visibleCount`. *(FR-022)*
7. A programmatic load leaves `lastChangeSource === 'load'`. *(FR-034)*
