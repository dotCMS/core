# Contract: value-load gating

Two separable contracts, because the fix has two parts. **Contract A** is the new one and the
one that actually fixes the reported bug; **Contract B** is the comparator, which survives from
the previous revision with its call sites narrowed.

Every row maps to an acceptance criterion in [spec.md](../spec.md).

> **Revised 2026-08-31.** The previous version of this file had only Contract B, on the theory
> that a smarter comparator was the whole fix. Measurement showed the effect re-runs with zero
> signal dependencies, so the comparator alone leaves the coupling in place. Contract A is new.

---

## Contract A — the identity latch

The effect that syncs the `value` input into the editor must load a given value **at most once**,
and must not do measurable work when re-run with a value it has already loaded.

**Behavioural signature:**

```ts
// Loads when: value is non-empty AND is not the reference already loaded.
// Never loads twice for the same reference, however many times the effect re-runs.
```

### Verdict table

| # | Sequence | Loads content? | Notes | AC |
|---|---|---|---|---|
| A1 | `value` set once to object `X`, effect runs once | **yes** | first load | AC-006 |
| A2 | Same reference `X`, effect re-runs N times | **no**, for all N | the spurious-re-run case | AC-005 |
| A3 | New reference `Y`, content differs | **yes** | genuine host swap | AC-005 |
| A4 | New reference `Y`, content *equal* to `X` | yes | acceptable: identity, not content, is the gate. No host does this — see spec Assumptions | AC-005 |
| A5 | `value` is `''` / `null` / `undefined` | **no** | reactive-forms host; must not latch on the empty value | AC-006 |
| A6 | `editor()` still `null` | **no**, and must not latch | retry once the editor mounts | AC-006 |
| A7 | `ed.view.dragging` is true | **no**, and must not latch | preserves the #36976 guard | regression |
| A8 | Node selection created on a `dotContent` card, BROKEN body | **no** | **the bug.** Today: loads, rebuilding the doc | **AC-004** |

**A8 is the Red-phase test that matters.** It must fail today and pass after the fix. Assert it at
the call site — no doc-changing transaction dispatched, and `state.selection` is still a
`NodeSelection` on `dotContent` after change detection — not on any function's return value.

**A2 is what makes A8 work.** Angular's `setInput` skips when the value is `Object.is`-equal
(`@angular/core@22.1.0/fesm2022/_debug_node-chunk.mjs:9235`), so a spurious re-run sees the same
reference. Measured in the browser as `valueIdentity=true`.

### Requirements

1. **Check emptiness before identity.** `if (!v) return;` must precede the latch comparison, so
   `''` is never recorded as loaded (A5).
2. **Only latch when the load actually happens.** Bailing for a missing editor or an in-flight
   drag must leave the latch untouched (A6, A7), or the value would never load.
3. **Compare by reference, not by content.** `===` on the raw input. Not `JSON.stringify`, not a
   structural compare. The point is O(1) and immunity to serialization.
4. **One load path for both hosts.** Initial content loads from `commitEditor`
   (`editor.component.ts:488`), which drains `pendingValue` (reactive forms) *or* `value()`
   (web component) — never both. No host detection, no `isJsp` branch.
5. **Do not use `editor.on('create')`.** It fires inside `new Editor(...)`, before
   `this.editor.set(editor)` at `:489`, so the signal is unavailable.

---

## Contract B — the structural comparator

**Signature** (behavioural, not prescriptive of the final name):

```ts
function contentMatchesEditorDocument(
    editor: Editor,
    incoming: string | JSONContent | JSONContent[]
): boolean;
```

Returns `true` when `incoming` represents the document the editor already holds — meaning the
caller must **not** call `setContent`.

**Call sites after this change:** `writeValue` (`editor.component.ts:738`) **only**. Removed from
`commitEditor` (one-shot drain, redundant) and from the value effect (Contract A supersedes it).

### Verdict table

| # | Incoming | Today | Required | AC |
|---|---|---|---|---|
| B1 | Legacy body: root `chartCount`, no `indent` on heading/paragraph | `false` | **`true`** | AC-007 |
| B2 | Current-shape body (what the editor emits) | `true` | `true` | control |
| B3 | Same document, legacy shape vs current shape | `false` | **`true`** | AC-007 |
| B4 | Body with a bullet list carrying `listItem.textAlign` | `false` | **`true`** | AC-003, AC-007 |
| B5 | Same document with attrs re-serialized in **alphabetical key order** (as `/api/v1/content` returns them) | `false` | **`true`** | AC-007 |
| B6 | A genuinely different document | `false` | `false` | AC-007 |
| B7 | Unknown **node** type, object entry point | `true` | `true` | AC-011 |
| B8 | Unknown **node** type, string entry point | `false` | **`true`** | AC-011 |
| B9 | Body carrying an unknown **mark** | `false` | **`true`** | AC-008 |
| B9a | A document that cannot be deserialized at all | `false` | `false` | AC-008 |
| B10 | Bare array of nodes (UVE side panel) | `false` | **`true`** | AC-009 |
| B11 | HTML string (JSP showdown fallback) | compares against `editor.getHTML()` | unchanged | — |

**Rows B1, B3, B4, B5, B8, B9 and B10 are the behaviour change — all seven must FAIL at Red.**
Every other row must keep its current verdict; they exist to stop an over-permissive fix. If B6 or
**B9a** also fails at Red, the fixtures are wrong, not the implementation.

> **B9 flipped after this contract was written.** It originally required `false`, on the grounds
> that an unknown mark aborts `Node.fromJSON` with
> `RangeError: There is no mark type X in this schema`. That was true when written. #37175
> (`53f5ba760f`, merged to `main` 2026-08-31) gave marks the same placeholder treatment nodes
> already had — `dotUnsupportedMark` — so an unknown mark now round-trips and must compare
> **equal**, exactly like an unknown node in B7/B8. Measured after merging: parsing the raw JSON
> still throws, but the comparator preserves first, so it parses and matches.
>
> The fail-closed path is still required, so it moved to **B9a** with input that genuinely cannot
> deserialize. The `catch` is now defensive rather than routine.

Two rows are easy to misread as pre-existing behaviour:

- **B5** is new to this revision. Measured against the two live fixtures: fetching a body through
  `/api/v1/content/<id>` returns `{"indent":0,"level":2,"textAlign":"left"}` where the schema
  order is `{"textAlign","indent","level"}`, so **both** the working and the broken contentlet
  return `false` when scored from API JSON. The JSP escapes this only because it reads the raw
  stored string. Any host or tool that normalizes key order breaks everything at once.
- **B10** — a bare array survives `preserveUnknownNodesInDocument` as an array, so the comparison
  is `[{"type":"paragraph",…}]` against `{"type":"doc","content":[…]}` — shapes that can never be
  string-equal. Today `false`, required `true`.

### Requirements

1. **Normalize both sides through the same schema.** The incoming value is deserialized with
   `editor.schema` before comparison; the editor's document is already normalized. Neither side is
   compared as text — except the HTML fallback of B11, which is not Block Editor JSON and keeps its
   existing `incoming === editor.getHTML()` string comparison.
2. **Ignore the document node's own attrs — compare with `Fragment.eq`, not `Node.eq`.** Use
   `PMNode.fromJSON(...).content.eq(editor.state.doc.content)`. This subsumes `stripDocStats` and
   covers root attrs it does not know about, including `chartCount`.

   The two are equivalent *today* and must not be treated as interchangeable. Measured:
   `schema.nodes.doc.spec.attrs` is `null`, so `fromJSON` discards root attrs outright — parsed
   `attrs` is `{}`, `toJSON().attrs` is absent, and both `node.eq` and `content.eq` return `true`
   for documents differing only in doc stats. The invariant is currently upheld by `fromJSON`
   dropping undeclared attrs, *not* by the choice of comparison. If anyone later declares doc attrs
   on the schema for the emit path, `Node.eq` would silently start failing while `Fragment.eq`
   would not. Pin `content.eq`.
3. **Preserve the unknown-node AND unknown-mark placeholder transforms.** Run
   `preserveUnknownNodesInDocument` on the incoming value on **both** entry points — the string
   branch's omission is why B8 fails today. Since #37175 that helper takes a third argument,
   `knownMarkNames`, and applies **nodes first, then marks**: an unknown node is swallowed whole
   into the placeholder's `originalNode` payload, which must stay byte-for-byte as stored, so the
   mark pass may only walk what is left of the real tree.
4. **Wrap bare arrays** as `{ type: 'doc', content: array }` before deserialization.
5. **Fail closed.** If deserialization throws, return `false` so the caller loads the content.
6. **Do not log document content** in the `catch`. Story Block bodies are customer content.
7. **No side effects.** The comparator must not dispatch, mutate the editor, or touch the incoming
   value.

### Performance envelope

Measured against the real schema, 200 iterations, documents with nested blockquotes, lists and
3×3 tables:

| Document | Current `JSON.stringify` ×2 | `fromJSON` + `Fragment.eq` | `Fragment.eq` alone |
|---|---|---|---|
| 12 KB / 956 nodes | 0.048 ms | 0.095 ms | 0.0002 ms |
| 119 KB / 9,808 nodes | 0.408 ms | 0.702 ms | 0.0005 ms |
| 1.19 MB / 100,848 nodes | 4.19 ms | 7.03 ms | 0.0049 ms |

`Fragment.eq` is effectively free — it short-circuits on child identity, so nesting doesn't hurt
it. All the cost is `Node.fromJSON`, and that parse happens anyway whenever the verdict is
"changed". No caching, memoization or debouncing is warranted; do not add any.

---

## Non-contract (explicitly unchanged)

- `withDocStats` and the emitted value shape — the host still receives root `charCount` /
  `wordCount` / `readingTime` (`editor.component.ts:713`).
- `preserveUnknownNodesInDocument` / `restoreUnknownBlockNodes` behaviour.
- The card's `mousedown` → `setNodeSelection` handler (`contentlet.component.ts:23,121`).
- `ngx-tiptap`'s node-view behaviour, TipTap's `setContent` semantics, and Angular's view-dirtying.
  The fix stops depending on them; it does not change them.
- The `dotContent` and `codeBlock` node views stay Angular components.
