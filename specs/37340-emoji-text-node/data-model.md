# Phase 1 Data Model: #37340 emoji text node

**Plan**: [plan.md](./plan.md) · **Contracts**: [contracts/](./contracts/)

No database entities. The "data model" here is the **stored Story Block JSON** — the read-only
contract every renderer consumes — plus the one new build artifact.

---

## 1. Story Block inline nodes

### `text` node — the target representation

```json
{ "type": "text", "marks": [ { "type": "link", "attrs": { "href": "…" } } ], "text": "dotCMS © 2026" }
```

- Carries marks natively. This is why moving emoji into text nodes removes the defect structurally.
- **After this change, every newly authored emoji lives here**, as ordinary characters.

### `emoji` node — legacy only, never newly created

```json
{ "type": "emoji", "attrs": { "name": "copyright" }, "marks": [ … ] }
```

| Field | Type | Notes |
|---|---|---|
| `attrs.name` | string | Shortcode. **User-controllable** via the Contentlet REST API — no schema validation exists. Must be HTML-escaped wherever echoed. |
| `attrs.text` | string? | Not written by the editor today. Renderers read it first when present. |
| `marks` | array? | Present in Shape 2, absent in Shape 1. |

- Inline, `selectable: false`, leaf (therefore an atom).
- **No literal character is stored** — only the shortcode, which is why renderers cannot recover
  the character without a lookup table.
- Registration is preserved so TipTap can still parse it; nothing creates it any more.

### Lifecycle

| Transition | When | Result |
|---|---|---|
| character typed / pasted | after this change | stays in the `text` node — no `emoji` node |
| picker insertion | already `insertContent(emoji.native)` | stays in the `text` node |
| existing `emoji` node loaded | any edit session | parses, renders, is **not** rewritten (render-only) |
| existing `emoji` node re-saved | author edits the field | persists as an `emoji` node — mixed representation is accepted |

> **Mixed representation is expected and supported.** One field may hold legacy `emoji` nodes and
> literal characters side by side. Both must render identically — asserted by the shared fixture.

---

## 2. Link run

The unit Gap B operates on. Not stored — derived at render time.

**Definition.** A maximal sequence of sibling inline nodes where:

1. every `text` node carries a `link` mark whose attributes are **all** equal (`href`, `target`,
   `rel`, `title`, `aria-label`), and
2. any intervening node is an **`emoji` node carrying no marks** — which is absorbed into the run.

Any other node — a differing `link`, an unmarked `text`, a `hardBreak`, any non-`emoji` atom —
**terminates** the run.

### The two stored shapes

| Shape | Stored JSON | Authored by | Today | Required |
|---|---|---|---|---|
| **1** | `text(link) + emoji(no marks) + text(link)` | typing an emoji **into** linked text | 2 anchors | 1 anchor — needs absorption |
| **2** | `text(link) + emoji(link) + text(link)` | applying a link **over** an existing emoji node | 1 anchor | unchanged |

---

## 3. No shortcode lookup table

Deliberately not carried. Resolving `attrs.name` back to a character would need a ~1900-entry
table in three published npm packages and on the Java classpath. Nothing creates `emoji` nodes any
more, so the table's coverage requirement is frozen and it would exist solely to rescue a single
reported case.

Renderers emit `attrs.text` when present, otherwise a visible `:name:` — see
[contracts/renderer.contract.md](./contracts/renderer.contract.md) C1. The editor is unaffected:
it bundles the emoji list already, so legacy nodes still display their character there.
