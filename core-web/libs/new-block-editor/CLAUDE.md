## Interaction Preferences

Act with constructive skepticism. You are a collaborator with strong reasoning ability.

Make decisions based on evidence. Do not assume you must agree with me.

You should:

- Question weak premises
- Point out flaws in reasoning
- Propose new approaches or mental models

If I am approaching a problem from the wrong perspective or with incorrect assumptions, explain it clearly and suggest a better starting point.

Be direct.  
Avoid unnecessary validation language, emojis, or marketing tone.

## Expected Response Format

Your responses should focus on:

- **Core insight**
- **Key tradeoffs**
- **Major risks**
- **Recommended next move**

## TipTap Node Names Are Immutable

TipTap serializes editor content to JSON using the node's `name` as the `type` key:

```json
{ "type": "dotImage", "attrs": { ... } }
{ "type": "dotContent", "attrs": { ... } }
```

dotCMS customers store this JSON in their database. **If a node name changes, TipTap will not recognize stored content and will silently drop those blocks on load — permanently destroying customer data.**

### Rule

**Never rename an existing node's `name` field without explicit approval from the developer.** This applies to any `.extension.ts` or node file where `name:` is set.

If asked to rename a node, you must:
1. Refuse and explain the data-loss risk
2. Present the trade-off: renaming requires a database migration to rewrite every stored document that contains that node type — not just a code change
3. Wait for explicit developer confirmation before proceeding

### Creating new nodes

When creating a new node, you may choose any name — but choose carefully, because **that name can never be changed** once real content has been written with it. Prefer descriptive, namespaced names (e.g. `dotVideo`, `dotContent`) over generic ones.

### Current node name registry

| Node | Name | File |
|------|------|------|
| Image | `dotImage` | `extensions/image.extension.ts` |
| Video | `dotVideo` | `extensions/video.extension.ts` |
| Contentlet | `dotContent` | `extensions/contentlet.extension.ts` |
| Grid block | `gridBlock` | `extensions/grid.extension.ts` |
| Grid column | `gridColumn` | `extensions/grid.extension.ts` |

Standard TipTap/StarterKit names (`paragraph`, `heading`, `bulletList`, `orderedList`, `blockquote`, `codeBlock`, `horizontalRule`, `table`, etc.) are owned by TipTap upstream and must not be changed either.

---

## Deferred Refactors

### Floating dialog abstraction
All three block dialogs (table, image, video) duplicate the same component-level logic:
- `floatX`, `floatY`, `positioned` signals
- `effect((onCleanup))` for document-level Escape + click-outside dismiss
- `afterRenderEffect` with `computePosition(flip(), shift())` for positioning

And the same service-level pattern:
- `isOpen` + `clientRectFn` signals
- `zone.run()` wrapping in `open()` / `close()`

**Trigger:** Extract into a `FloatingPanelDirective` + generic base service when a 4th block type with a dialog is added, or when the duplication actively causes a bug/inconsistency. Not worth doing at 3 blocks.