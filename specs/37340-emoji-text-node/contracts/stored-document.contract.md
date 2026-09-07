# Contract: what the Block Editor promises to emit

**Feature**: `37340-emoji-text-node` · **Plan**: [plan.md](./plan.md)

The new Block Editor's only outward interface is the Story Block JSON it writes to a contentlet
field, consumed by the VTL renderer, four JS SDK renderers, and the legacy Block Editor. This
document states what changes about that value — and, just as importantly, what does not.

---

## Guarantee added

> **No document emitted by the new Block Editor contains an `emoji` node.**

Holds for every authoring path: typing, pasting plain text, pasting HTML, the toolbar picker,
`:shortcode:` entry, and `:)` emoticons. It holds regardless of the field's `allowedBlocks`.

**Scope of the guarantee**: documents *emitted*. A document *loaded* may still contain `emoji` nodes
and will still contain them after a save, untouched — see below.

## Second guarantee

> **A document the editor has loaded contains no `emoji` node — each has become `text` carrying the
> same marks and the resolved character.**

The healed shape reaches storage on the author's next save, along the same path any edit takes.

## Third guarantee — the reported payload repairs itself

> **A bare `emoji` node between two `text` nodes carrying attribute-identical `link` marks heals
> into that link, and the run renders as a single `<a>`.**

No re-save, no author action, no renderer change. This meets #37340's criterion that already-split
content render as a single link without a re-save.

## Guarantee explicitly NOT made

> **Marks are inherited only for that one signature.**

A symbol between links to **different** URLs, next to a `hardBreak`, at a block boundary, or beside
a `text` node with no link at all heals to unmarked text and stays outside both anchors. A node that
already carries marks of its own keeps them and is never re-marked from a neighbour.

Those shapes carry no fingerprint of this defect, so the transform declines to guess at them.
Deliberate (spec Resolved Decision 3), and bounded by AC-016, which asserts each negative case
separately.

---

## Consumer impact

| Consumer | Impact |
|---|---|
| VTL (`VM_global_library.vm`, `render.vtl`) | **None.** Untouched. New *and* healed content arrives as `text` nodes it already handles; content in fields nobody has opened behaves exactly as before |
| `@dotcms/react`, `@dotcms/vue`, `@dotcms/angular` | **None.** No code change, no version change, no changelog entry. ADR-0019 (date-lockstep versioning) is satisfied by touching nothing |
| Legacy Block Editor (`libs/block-editor`) | **Improved.** New content contains no `emoji` node, and healed content loses the node on its next save, so the pre-existing drop-on-load path is closed for everything the new editor touches |
| Java (`StoryBlockUtil`, required-field validation, char count, ES text extraction) | **Improved with no code change.** These paths count only `"text"` nodes; healed content becomes visible to all of them. Three live blind spots resolve themselves |
| Contentlet REST API | **None.** No schema, no validation, no contract change |

**No consumer needs to ship anything for this fix to take effect.** That is the property v2 was
reduced in scope to obtain: v1 required coordinated changes in five renderer implementations and
three published npm packages before the fix was complete.

---

## Backward-compatibility obligations on the editor

Non-negotiable, guarded by AC-013 and AC-014:

1. The `emoji` node type stays registered, so `Node.fromJSON` resolves stored nodes.
2. The `emojis` option stays unfiltered, so `renderHTML`/`renderText` resolve a stored `name` to its
   character rather than to a literal `:copyright:`.
3. Neutralizing `parseHTML` must not affect (1) — parse rules govern HTML entry points only. AC-012
   and AC-020 are asserted together precisely so this pair cannot drift apart.
4. **The heal must not touch a document that has no `emoji` node** (AC-019), must not inherit marks
   outside the link sandwich (AC-014), and must not fire the sandwich rule in any of the negative
   cases (AC-016). Those three bound the entire risk of the transform.
