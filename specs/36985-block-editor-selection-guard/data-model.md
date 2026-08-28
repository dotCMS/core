# Phase 1 Data Model: Block Editor value shapes

No persisted entity changes. The "data model" for this fix is the set of **in-memory document
shapes** the comparator must reconcile, and the normalization TipTap applies between them.

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
- bare array — `content` without the wrapping doc node;
- string — the reactive-forms host stringifies before `writeValue`;
- HTML string — the JSP's showdown fallback when `JSON.parse` throws.

### 2. Editor document (`editor.state.doc`)

A ProseMirror `Node`, always already normalized: schema defaults filled, undeclared attrs
dropped, attrs in schema-declaration order, adjacent identical text runs merged.

### 3. Unsupported-block placeholder (`dotUnsupportedBlock`)

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

---

## Invariants the fix must preserve

| Invariant | Why | Guarded by |
|---|---|---|
| Stored content is never rewritten by loading it | This is a comparison fix, not a migration | Nothing in scope writes; AC-003 asserts no `setContent` |
| The emitted value keeps root `charCount` / `wordCount` / `readingTime` | Legacy-parity contract read by server-side reporting and headless renderers | AC-006 |
| The placeholder round-trip is unchanged | Shared with #37149; breaking it blanks the field | AC-007 |
| An undeserializable document still loads via `setContent` | #37145's hardening | AC-004 |
| An equal-content push is a no-op | Makes the unidentified re-push harmless | AC-008 |

---

## State transition — the defect, and the fix

```
click card
  → mousedown handler dispatches NodeSelection          [contentlet.component.ts:130]
  → value effect re-evaluates                           [editor.component.ts:619]
      → guard: stored vs editor document
          BYTE-WISE  → "different" → setContent → doc rebuilt → NodeSelection lost   ← today
          STRUCTURAL → "unchanged" → no transaction   → NodeSelection survives        ← after fix
```
