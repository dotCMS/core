# Contract: `contentMatchesEditorDocument`

The comparator's observable contract. Each row is an input shape and the verdict the
implementation must return. This table is the source for the Red-phase tests — every row maps
to an acceptance criterion in [spec.md](../spec.md).

**Signature** (behavioural, not prescriptive of the final name):

```ts
function contentMatchesEditorDocument(
    editor: Editor,
    incoming: string | JSONContent | JSONContent[]
): boolean;
```

Returns `true` when `incoming` represents the document the editor already holds — meaning the
caller must **not** call `setContent`.

---

## Verdict table

| # | Incoming | Today | Required | AC |
|---|---|---|---|---|
| 1 | Legacy body: root `chartCount`, no `indent` on heading/paragraph | `false` | **`true`** | AC-001, AC-002 |
| 2 | Current-shape body (what the editor emits) | `true` | `true` | control |
| 3 | Same document expressed in the legacy shape vs the current shape | `false` | **`true`** | AC-001 |
| 4 | A genuinely different document | `false` | `false` | AC-004 |
| 5 | Body containing an unknown **node** type, via the object entry point | `true` | `true` | AC-007 |
| 6 | Body containing an unknown **node** type, via the string entry point | `false` | **`true`** | AC-007 |
| 7 | Body carrying an undeserializable **mark** | `false` | `false` | AC-004 |
| 8 | Bare array of nodes (UVE side panel) | `false` | **`true`** | AC-005 |
| 9 | The same equal-content value pushed repeatedly | — | `true` every time | AC-008 (partial — see below) |
| 10 | HTML string (JSP showdown fallback) | compares against `editor.getHTML()` | unchanged | — |

Rows 1, 3, 6 and **8** are the behaviour change — all four must FAIL at Red. Every other row
must keep its current verdict; they are there to stop an over-permissive fix. If row 4 or row 7
also fails at Red, the fixtures are wrong, not the implementation.

Row 8 is easy to misread as pre-existing behaviour. Measured: a bare array survives
`preserveUnknownNodesInDocument` as an array, so the comparison is
`[{"type":"paragraph",…}]` against `{"type":"doc","content":[…]}` — shapes that can never be
string-equal. Today `false`, required `true`.

**Row 9 is a comparator-purity check only.** The comparator is a pure function, so calling it
twice with equal content cannot fail — the row documents the intent but carries no risk.
AC-008's real assertion is at the *call site*: that the effect dispatches no `setContent`. That
is not reachable from a pure-function spec (the effect needs a mounted component with an
Angular injector for the node views), so it is verified manually against the BROKEN/CONTROL
fixtures in [quickstart.md](../quickstart.md) §2 — `docRebuilt: false` after the click. Recorded
here rather than left implicit, because ADR-0013 makes Jest the only automated gate on main and
a green row 9 must not be read as covering the call site.

---

## Behavioural requirements

1. **Normalize both sides through the same schema.** The incoming value is deserialized with
   `editor.schema` before comparison; the editor's document is already normalized. Neither side
   is compared as text — except the HTML fallback of row 10, which is not Block Editor JSON and
   keeps its existing `incoming === editor.getHTML()` string comparison.
2. **Ignore the document node's own attrs — compare with `Fragment.eq`, not `Node.eq`.** Use
   `PMNode.fromJSON(...).content.eq(editor.state.doc.content)`. This subsumes `stripDocStats`
   and covers root attrs it does not know about, including `chartCount`.

   The two are equivalent *today* and must not be treated as interchangeable. Measured:
   `schema.nodes.doc.spec.attrs` is `null`, so `fromJSON` discards root attrs outright —
   parsed `attrs` is `{}`, `toJSON().attrs` is absent, and `node.eq` and `content.eq` both
   return `true` for documents differing only in doc stats. The invariant is currently upheld
   by `fromJSON` dropping undeclared attrs, *not* by the choice of comparison. If anyone later
   declares doc attrs on the schema for the emit path, `Node.eq` would silently start failing
   while `Fragment.eq` would not. Pin `content.eq`.
3. **Preserve the unknown-node placeholder transform.** Run
   `preserveUnknownNodesInDocument` on the incoming value on **both** entry points — the
   string branch's omission is defect 3.
4. **Wrap bare arrays** as `{ type: 'doc', content: array }` before deserialization.
5. **Fail closed.** If deserialization throws, return `false` so the caller loads the content.
6. **Do not log document content** in the `catch`. Story Block bodies are customer content.
7. **No side effects.** The comparator must not dispatch, mutate the editor, or touch the
   incoming value.

---

## Non-contract (explicitly unchanged)

- `withDocStats` and the emitted value shape — the host still receives root
  `charCount` / `wordCount` / `readingTime`.
- `preserveUnknownNodesInDocument` / `restoreUnknownBlockNodes` behaviour.
- The three call sites' signatures in `editor.component.ts` (`:498`, `:625`, `:747`).
- The value-synchronisation effect itself.
