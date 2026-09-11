# Phase 1 Data Model: Block Editor value shapes

No persisted entity changes. The "data model" for this fix is the set of **in-memory document
shapes** the load path must reconcile, the normalization TipTap applies between them, and the
one piece of component state the fix adds.

> **Revised 2026-08-31** alongside spec.md and the contract. Added the `#loadedValue` latch,
> the API key-order variance, and the corrected state-transition diagram.

---

## Entities

### 1. Stored value

What lives in the Story Block field and arrives from the host.

| Field | Type | Notes |
|---|---|---|
| `type` | `'doc'` | Absent when the host passes a bare array (UVE side panel). |
| `attrs` | `Record<string, unknown>` \| absent | Root document attrs. May carry `charCount` / `wordCount` / `readingTime`, or the legacy `chartCount` typo. |
| `content` | `JSONContent[]` | Block nodes. |

**Shape variance is the whole problem.** The same logical document can arrive as any of:

- current shape — every node carries the attrs the current schema declares, in schema order;
- legacy shape — attrs the schema has since added are absent (`indent`), and attrs it never
  declared are present (`chartCount`);
- live-divergence shape — attrs the *legacy editor still writes today* that the new schema does
  not declare (`listItem.textAlign`, `aiContent.loading`);
- API shape — semantically current, but with attrs re-serialized in **alphabetical key order**
  (`{"indent","level","textAlign"}`) rather than schema order;
- bare array — `content` without the wrapping doc node;
- string — the reactive-forms host stringifies before `writeValue`;
- HTML string — the JSP's showdown fallback when `JSON.parse` throws.

### 2. Load latch (`#loadedValue`) — new

Private component state in `editor.component.ts`. Not persisted, not an input, not a signal.

| Field | Type | Initial | Notes |
|---|---|---|---|
| `#loadedValue` | `unknown` | `undefined` | The **raw input reference** most recently loaded into the editor. |

Compared with `===`, never serialized. `undefined` is the "nothing loaded yet" sentinel and is
never a legitimate value, so the first non-empty push always loads. Written only when a load
actually happens — never on an early bail — otherwise content could never load. See Contract A.

### 3. Editor document (`editor.state.doc`)

A ProseMirror `Node`, always already normalized: schema defaults filled, undeclared attrs
dropped, attrs in schema-declaration order, adjacent identical text runs merged.

### 4. Unsupported-block placeholder (`dotUnsupportedBlock`)

| Attr | Type | Notes |
|---|---|---|
| `originalType` | `string \| null` | The node type the schema did not know. |
| `originalNode` | `JSONContent \| null` | The original node, preserved verbatim. |
| `originalNodeRaw` | `string \| null` | Set only when the HTML round-trip could not be parsed. |

Both sides of the comparison hold this placeholder for unknown nodes — the incoming side after
`preserveUnknownNodesInDocument`, the editor side because it was loaded through the same
transform. They are byte-identical; measured.

---

## Normalization applied by `Node.fromJSON`

| Input difference | Effect after `fromJSON` |
|---|---|
| Declared attr missing (`indent`) | Filled with the schema default (`0`) |
| Undeclared attr present (`chartCount`) | Silently dropped, no error |
| Attrs in a different key order | Re-emitted in schema-declaration order |
| Unknown **node** type | Never reaches `fromJSON` — rewritten to `dotUnsupportedBlock` first |
| Unknown **mark** type | Throws `RangeError: There is no mark type <name> in this schema` |

Observed schema attribute order, which is what a string comparison is hostage to:

- `heading` → `['textAlign', 'indent', 'level']`
- `paragraph` → `['textAlign', 'indent']`

Measured against the two live fixtures. `/api/v1/content/<id>` returns
`{"indent":0,"level":2,"textAlign":"left"}` — alphabetical, not schema order — so a body scored
from API JSON compares unequal even when it is otherwise current. The JSP avoids this only
because it reads the raw stored string and `JSON.parse` preserves insertion order.

---

## Invariants the fix must preserve

| Invariant | Why | Guarded by |
|---|---|---|
| Initial content always loads, on every host | A missed load renders an empty field — the most visible failure | AC-006 |
| Stored content is never rewritten by loading it | This is a load-gating fix, not a migration | Nothing in scope writes |
| A node selection dispatches no doc-changing transaction | The defect itself | **AC-004** |
| The same input reference loads exactly once | Immunity to the spurious effect re-run | AC-005 |
| A new input reference still loads | Keeps a genuine host swap working | AC-005 |
| The emitted value keeps root `charCount` / `wordCount` / `readingTime` | Legacy-parity contract read by server-side reporting and headless renderers | AC-010 |
| The placeholder round-trip is unchanged | Shared with #37149; breaking it blanks the field | AC-011 |
| An undeserializable document still loads via `setContent` | #37145's hardening | AC-008 |

---

## State transition — the defect, and the fix

```
click card
  → mousedown dispatches NodeSelection on dotContent        [contentlet.component.ts:23,121]
  → ProseMirror selects the node view
  → ngx-tiptap selectNode() → updateProps({selected:true})  [ngx-tiptap.mjs:337,403]
  → componentRef.setInput(...)                              [ngx-tiptap.mjs:258]
  → Angular markViewDirty walks ALL ancestor LViews         [_debug_node-chunk.mjs:9242,6205]
  → runEffectsInView re-runs the value effect               [_debug_node-chunk.mjs:5920]
       ┌─ TODAY ──────────────────────────────────────────────────────────────────────┐
       │  no latch → compares stored vs editor doc BYTE-WISE → "different"            │
       │  → setContent → tr.replaceWith(0, doc.content.size, …)   [tiptap:1148,1157]  │
       │  → whole document replaced (size 31 → 31, identical content)                 │
       │  → NodeSelection(23) maps to TextSelection(30) → ring gone, toolbar dark     │
       └─────────────────────────────────────────────────────────────────────────────-┘
       ┌─ AFTER FIX ─────────────────────────────────────────────────────────────────-┐
       │  v === #loadedValue → return immediately (O(1))                              │
       │  → no transaction → NodeSelection survives → toolbar enabled                 │
       └─────────────────────────────────────────────────────────────────────────────-┘
```

The effect reads only `value()` and `editor()`, and neither changes on a click. The re-entry is
Angular's view refresh, not the signal graph — measured by wrapping both reads in `untracked()`,
which left the effect with zero dependencies and did **not** stop it re-running. That is why the
fix latches instead of narrowing dependencies.
