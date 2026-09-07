# Phase 1 Data Model: node shapes in play

**Feature**: `37340-emoji-text-node` · **Plan**: [plan.md](./plan.md)

No entity is created, renamed or removed. This document records the three inline-node shapes this
change moves between, because the whole defect and the whole fix are expressible as a change of
shape.

---

## Entities

### `emoji` — inline atom (registered, never created, healed away on load)

| Field | Type | Notes |
|---|---|---|
| `type` | `"emoji"` | Registered in the schema. **Must stay registered** — see Lifecycle below |
| `attrs.name` | `string` | A TipTap shortcode (`"copyright"`), **not** a character and not an HTML entity |
| `marks` | `Mark[]` | Present only when a mark was applied *over* an existing node. The defect path creates it with none |

**Validation rules**

- `attrs.name` is resolved to a character by `shortcodeToEmoji(name, emojis)` — a lookup that exists
  **only inside the editor**, because `emojis` is an npm-package export. No renderer, SDK or Java
  path can perform it. This single fact is why the node is the wrong shape for stored content, why
  v1 needed a build-generated map to make it work downstream, and why the heal belongs in the
  editor: it is the one place the table is already present.
- An unresolvable `name` is left untouched by the heal (AC-018) — never blanked, never rendered as
  a literal `:name:`. Unreachable from the shipped table; reachable through hand-crafted JSON via
  the Contentlet REST API, since no Story Block schema validation exists in `dotCMS/src/main/java`.

### `text` — the shape everything becomes

| Field | Type | Notes |
|---|---|---|
| `type` | `"text"` | |
| `text` | `string` | Carries the literal character (`"dotCMS Copyright © All rights reserved"`) |
| `marks` | `Mark[]` | Carried natively — this is the property the `emoji` node lacks |

### `link` — the mark the defect tears

| Field | Type | Notes |
|---|---|---|
| `type` | `"link"` | |
| `attrs` | `{ href, target?, rel?, title?, ariaLabel? }` | **Every** attr must match for the link-sandwich rule to fire (AC-015). Attribute equality is the whole boundary of the one inference this change permits |

---

## Relationships

An inline run is a sequence of sibling nodes inside a block (`paragraph`, `heading`, `listItem`,
`blockquote`, table cell). A mark spans a run only as long as consecutive nodes carry it. **A bare
inline atom inside a marked run ends the run** — that is the entire mechanism of this defect, and it
is ProseMirror behaving correctly, not a bug in ProseMirror.

---

## State transitions

```
BEFORE THE FIX — typing © inside linked text
  text(link, "dotCMS Copyright ")
+ emoji(name:"copyright", no marks)        ← bare atom ends the run
+ text(link, "All rights reserved")
  → two <a> elements, and © absent entirely from VTL output

AFTER THE FIX — typing © inside linked text
  text(link, "dotCMS Copyright © All rights reserved")
  → one <a>, character present in every renderer

AFTER THE FIX — the reported payload, healed on load
  text(link, "dotCMS Copyright ")
+ emoji(bare) between two identical link marks   ← the sandwich: a
                                                   fingerprint of this defect
  → healed text inherits that link mark, ProseMirror joins the run
  → text(link, "dotCMS Copyright © All rights reserved") — one <a>

AFTER THE FIX — a symbol between links to DIFFERENT urls
  text(link:A) + emoji(bare) + text(link:B)
+ heals to text(link:A) + text(no marks) + text(link:B)
  → © renders; two <a>, and the symbol stays outside both. Correct —
    no fingerprint, so no inference

AFTER THE FIX — a link applied OVER an existing node
  text(link) + emoji(link) + text(link)
  → heals to three text(link) nodes, joined by normalization
```

**The second transition is the fix; the rest are the heal.** Two of them rejoin, and in both cases
the joining is ProseMirror's — the transform only decides which marks the healed text carries.

---

## Lifecycle constraint (the one hard rule)

The `emoji` node type **must remain registered** with an **unfiltered** `emojis` option:

- **Registration** — `Node.fromJSON` resolves node types from the schema. Without the registration,
  stored `emoji` nodes are dropped on load; #37145 showed the same mechanism blank an entire field,
  and `new-block-editor/CLAUDE.md` documents it for `AIContent`.
- **Unfiltered `emojis`** — the option feeds `renderHTML`/`renderText`. Filtering an entry makes
  stored nodes of that name render as literal `:copyright:` text. Verified, not assumed.

Neutralizing `parseHTML` does **not** violate this rule: parse rules govern HTML entry points only,
and `Node.fromJSON` never consults them. AC-012 and AC-020 exist to keep that true.

The heal does not violate it either — it runs **after** parsing, on a document TipTap has already
resolved. Registration is what lets the node be read at all; the heal is what replaces it once read.
