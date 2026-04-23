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

## Dialog System Architecture

All block dialogs (table, image, video, link, emoji) share a single `EditorDialogManagerService` and an `<editor-dialog>` shell component:

- `EditorDialogManagerService` (`services/editor-dialog-manager.service.ts`) — central state: which dialog is open, its anchor rect, and per-dialog payloads (`imagePayload`, `linkPayload`).
- `EditorDialogComponent` (`components/editor-dialog/editor-dialog.component.ts`) — shell wrapper: absolute positioning via `@floating-ui/dom`, `display:none` toggle, Escape + click-outside dismiss, `<ng-content>` projection, `(opened)` output for auto-focus.

Each dialog content component:
- Takes `editor = input.required<Editor>()` and calls editor commands directly.
- Wraps its form in `<editor-dialog dialogId="...">` and uses `(opened)` to auto-focus the first input.
- Injects `EditorDialogManagerService` for open/close state and payloads.