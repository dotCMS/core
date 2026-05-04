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
| Image | `dotImage` | `extensions/nodes/image.extension.ts` |
| Video | `dotVideo` | `extensions/nodes/video.extension.ts` |
| Contentlet | `dotContent` | `extensions/nodes/contentlet/contentlet.extension.ts` |
| Grid block | `gridBlock` | `extensions/nodes/grid.extension.ts` |
| Grid column | `gridColumn` | `extensions/nodes/grid.extension.ts` |
| AI content | `aiContent` | `extensions/nodes/ai-content.extension.ts` |

Standard TipTap/StarterKit names (`paragraph`, `heading`, `bulletList`, `orderedList`, `blockquote`, `codeBlock`, `horizontalRule`, `table`, etc.) are owned by TipTap upstream and must not be changed either.

---

## Overlay System Architecture

The editor uses two distinct overlay primitives. Pick by interaction model, not content type — the difference is whether the overlay anchors to the caret/trigger (popover) or sits centered over the page (modal dialog).

### Pick the right overlay primitive

| Primitive | Use | Anchored to | Modality | Examples |
|-----------|-----|-------------|----------|----------|
| `<dot-editor-popover>` shell | Compact, caret-anchored, single form | Caret / trigger rect via `@floating-ui/dom` | Non-modal — no backdrop, no focus trap, click-outside dismisses | link, table, image-properties, emoji |
| PrimeNG `<p-dialog [modal]>` | Large content, textarea + preview, multi-pane | Viewport center | Modal — backdrop, focus trap, explicit close | AI content |
| PrimeNG `DialogService.open()` | Reusing an external component that depends on `DynamicDialogRef` | Viewport center | Modal | Image picker, video picker, AI image |

When an overlay has both an input area AND a result/preview area, default to a modal dialog — caret-anchored popovers get cramped.

### Caret-anchored popover shell (`<dot-editor-popover>`)

All compact popovers (link, table, image-properties, emoji) share a single `EditorPopoverService` and an `<editor-popover>` shell component:

- `EditorPopoverService` (`services/editor-popover.service.ts`) — central state: which popover is open, its anchor rect, and per-popover payloads (`imagePropertiesPayload`, `linkPayload`).
- `EditorPopoverComponent` (`components/editor-popover.component.ts`) — shell wrapper: absolute positioning via `@floating-ui/dom`, `display:none` toggle, Escape + click-outside dismiss (whitelisting body-portaled PrimeNG `.p-overlay` / `.p-select-overlay` so embedded `<p-select>` stays alive), `<ng-content>` projection, auto-focus on the first form control after first paint.

Each popover content component:
- Takes `editor = input.required<Editor>()` and calls editor commands directly.
- Wraps its form in `<dot-editor-popover popoverId="...">`.
- Injects `EditorPopoverService` for open/close state and payloads.

### Embedded centered modal (PrimeNG `<p-dialog [modal]>`)

The AI Content dialog renders its own PrimeNG `<p-dialog>` inside the editor's component tree — no shell. Visibility lives on `EditorModalService`:

- `aiContentOpen` signal + `openAiContent()` / `closeAiContent()` methods on `EditorModalService`.
- The dialog binds `[visible]="manager.aiContentOpen()"` and emits `(visibleChange)` to propagate Escape / X clicks back through `closeAiContent()`.
- Auto-focus happens inside the dialog component on the textarea — PrimeNG handles modal scroll-lock and overlay rendering.

### Reusing an external component via `DialogService.open()`

When the dialog content is owned by another library (e.g. `DotAIImagePromptComponent` from `@dotcms/ui`, or `DotBrowserSelectorComponent` for image/video picking), the component depends on `DynamicDialogRef` injection and cannot be embedded as a normal Angular template. Use PrimeNG's `DialogService.open()` via `EditorModalService` instead:

- The editor component provides `DialogService` at the component scope (so each editor instance has its own dynamic-dialog factory). Provided in `editor.component.ts`.
- `EditorModalService` keeps a private `DynamicDialogRef` per modal kind, plus an open-state signal where useful (e.g. `aiImageOpen`).
- Each `openX(editor)` method opens the dialog with the appropriate `data` and subscribes to `dialogRef.onClose` to apply the result (inserting nodes, etc.) into the editor.
- `ngOnDestroy()` on the service closes any live ref so an editor unmount mid-dialog doesn't orphan the overlay.

---

## Node + action inventory

What actions are available on each node type. **Slash** = appears in `/` menu (`slash-menu-catalog.ts`). **Toolbar** = button in `toolbar.component.ts`. **Marks** = inline marks that can be applied to text inside this node. **Commands** = TipTap commands declared on the node's extension. **Node-scoped** = appears only when the node is selected/active.

### Block-level nodes

| Node (`type`) | Source | Slash | Toolbar | Allowed-block key |
|---------------|--------|-------|---------|-------------------|
| `paragraph` | StarterKit | Text | Block-type select | always allowed |
| `heading` (levels 1–6) | StarterKit | Heading 1 / 2 / 3 | Block-type select (1–3) | `heading1`…`heading6` |
| `bulletList` | StarterKit | Bullet List | Bullet List | `bulletList` |
| `orderedList` | StarterKit | Ordered List | Ordered List | `orderedList` |
| `listItem` | StarterKit | — | indent / outdent | inherits list parent |
| `blockquote` | StarterKit | Blockquote | Blockquote | `blockquote` |
| `codeBlock` | StarterKit | Code Block | Code Block | `codeBlock` |
| `horizontalRule` | StarterKit | — | Horizontal rule | `horizontalRule` |
| `table` | TableKit | Table (popover) | Insert table + table sub-toolbar | `table` |
| `dotImage` | `image.extension.ts` | Image (modal picker) | Insert image, wrap-left/right (node-scoped), align, image properties popover (node-scoped) | `image` |
| `dotVideo` | `video.extension.ts` | Video (modal picker) | Insert video | `video` |
| `youtube` | `@tiptap/extension-youtube` | — (legacy slash entry) | — | `youtube` |
| `dotContent` | `contentlet/contentlet.extension.ts` | Content type → submenu | Edit contentlet (node-scoped) | `dotContent` |
| `gridBlock` | `grid.extension.ts` | Grid (2 columns) | — | `gridBlock` |
| `gridColumn` | `grid.extension.ts` | — (created by `insertGrid`) | — | inherits gridBlock |
| `aiContent` | `ai-content.extension.ts` | Ask AI (centered modal) | — | `aiContent` |
| `uploadPlaceholder` | `upload-placeholder.extension.ts` | — (transient) | — | always (transient) |

### Marks

| Mark | Source | Toolbar | Applies to |
|------|--------|---------|------------|
| `bold` | StarterKit | Bold | any text |
| `italic` | StarterKit | Italic | any text |
| `strike` | StarterKit | Strike | any text |
| `code` | StarterKit | Inline code | any text |
| `superscript` | `@tiptap/extension-superscript` | Sup | any text |
| `subscript` | `@tiptap/extension-subscript` | Sub | any text |
| `link` | `@tiptap/extension-link` | Link popover | any text (gated by `link` allowed-block) |
| `textAlign` | `@tiptap/extension-text-align` | Align L/C/R/Justify | configured for `paragraph` + `heading` only |

### Special / node-scoped commands

| Command | Owner | What it does |
|---------|-------|--------------|
| `setImageTextWrap('left' \| 'right')` | `dotImage` | Toggles `image-wrap-left/right` class on the wrapping `<figure>`. Mutually exclusive with `setImageTextAlign`. |
| `setImageTextAlign('left' \| 'center' \| 'right')` | `dotImage` | Sets `image-align-*` class on the wrapping `<figure>`. Clears `textWrap`. |
| `insertGrid()` | `gridBlock` | Inserts a 2-column grid block at the selection. Equal default widths. |
| `setGridColumns(columns: number[])` | `gridBlock` | Updates column-fraction widths for the active grid block. Used by the grid resize plugin. |
| `insertAINode(content?: string)` | `aiContent` | Inserts a new `aiContent` block, or replaces the existing one's HTML content. |
| `setLoadingAIContentNode(loading: boolean)` | `aiContent` | Toggles the `is-loading` class on the existing `aiContent` block. |

### Slash-only "actions" (don't insert a single node)

These slash entries do not map 1:1 to a node — they trigger flows that mutate the editor:

| Slash entry | Trigger |
|-------------|---------|
| AI Image | Opens `DotAIImagePromptComponent` via `DialogService.open()` (centered modal). On accept, inserts a `dotImage` node. |
| AI Content | Opens the embedded `<p-dialog>` AI Content modal. On insert, places an `aiContent` node. |
| Content type | Opens an in-place sub-menu of allowed content types, then a contentlet picker. Inserts a `dotContent` node. |
| Image / Video | Opens `DotBrowserSelectorComponent` via `DialogService.open()` (centered modal picker). Inserts the corresponding `dotImage` / `dotVideo` node. |
| Table / Link / Emoji | Opens a caret-anchored `<dot-editor-popover>`. Insert / mutate the corresponding node. |

### Customer-supplied remote commands (`customBlocks` field variable)

Each declared `Action` becomes a slash entry that calls `editor.commands[action.command]()` on selection. The TipTap extensions resolved from the remote URLs determine which commands actually exist; missing commands log a warning instead of throwing. See `extensions/remote-extensions.loader.ts`.

### Toolbar groups (visual order)

1. History — Undo, Redo
2. Block type — paragraph / heading 1–3 select
3. Inline format — Bold, Italic, Strike, Code, Superscript, Subscript
4. Alignment — Left, Center, Right, Justify (heading + paragraph only)
5. Image-only (visible when an image is selected) — Wrap L/R, Image properties
6. Contentlet-only (visible when a contentlet is selected) — Edit
7. Block formats — Bullet List, Ordered List, Blockquote, Code Block
8. Indent / Outdent / Clear formatting
9. Horizontal rule
10. Insert dialogs — Link, Image, Video, Table
11. Table sub-toolbar (when inside a table) — Insert row/col, Merge/Split, Toggle row/col header, Delete row/col/table
12. Emoji
13. Markdown copy / paste
14. Fullscreen toggle

The `showInsertGroup`/`showBlockFormatsGroup` computeds and the `@if (allow*)` guards collapse dividers when a group is empty. See `toolbar.component.ts`.