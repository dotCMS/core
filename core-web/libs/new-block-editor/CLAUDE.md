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

## Service Architecture

The lib follows a strict split: **data fetching** delegates to `@dotcms/data-access`; **state and orchestration** stays local. The legacy block-editor sets the precedent — use the data-access services directly rather than re-implementing them.

### Data services — consume from `@dotcms/data-access`

| Service | Used for |
|---|---|
| `DotContentTypeService` | Content type filtering for the slash-menu's content-type sub-picker (`filterContentTypes`) and per-type metadata reads (`getContentType`, used by `ContentletEditUrlService`). |
| `DotContentSearchService` | Lucene search behind the slash-menu's contentlet drill-down (`/api/content/_search`). The editor-flavoured query string (`+contentType:X +languageId:Y +deleted:false +working:true +catchall:** title:''^15`) is built inline at the call site (`buildContentletByTypeQuery` in `slash-menu-catalog.ts`); the service itself stays generic. |
| `DotLanguagesService` | Language metadata for the editor store (`getById`). |
| `DotAiService` | AI text generation, AI image generation + publish, plugin status check. Identical surface to legacy block-editor usage. |
| `DotUploadFileService` | Wrapped by the lib's local `DotUploadService` adapter (see below). |
| `DotMessageService` | i18n. Used everywhere. |

Do **not** create custom HTTP services in this lib for any of the above. If you need a method that doesn't exist on a data-access service, extend the data-access service rather than rolling a new one here.

### Local services — editor-specific

| Service | Why it stays local |
|---|---|
| `EditorPopoverService` | Caret-anchored popover state (active id, anchor rect, per-popover payloads). Editor-only concern. |
| `EditorModalService` | Lifecycle for centered `DialogService.open()` modals (AI content, AI image, image / video pickers). Editor-only concern. |
| `EditorToolbarStore` | Signal mirror of TipTap mark/block/alignment state for the toolbar. Editor-only concern. |
| `SlashMenuService` | Slash-menu catalog, filtering, sub-menu loading. Editor-only concern. |
| `ContentletEditUrlService` | Resolves the legacy-vs-new content editor URL via per-content-type feature-flag cache. Caches the metadata read so repeated contentlet edits within one session don't re-hit the network. The wrapper exists *for* the cache; without it, every "Edit contentlet" click would re-fetch. |
| `DotUploadService` (adapter) | Promise/async-await adapter around `DotUploadFileService.publishContent()`. Two responsibilities: bridge the async model for `handleMediaDrop` (which is `async` linear code) and unwrap the workflow PUBLISH endpoint's `Record<contentTypeKey, contentlet>` shape into the editor's narrower `UploadedImage` / `UploadedVideo` types. |

When in doubt: **state & orchestration → local. HTTP → data-access.**

---

## Overlay System Architecture

The editor uses two distinct overlay primitives. Pick by interaction model, not content type — the difference is whether the overlay anchors to the caret/trigger (popover) or sits centered over the page (modal dialog).

### Pick the right overlay primitive

| Primitive | Use | Anchored to | Modality | Examples |
|-----------|-----|-------------|----------|----------|
| `<dot-editor-popover>` shell | Compact, caret-anchored, single form | Caret / trigger rect via `@floating-ui/dom` | Non-modal — no backdrop, no focus trap, click-outside dismisses | link, table, image-properties, emoji |
| PrimeNG `DialogService.open()` | Centered modal — large content, multi-pane, embeddable, or external library component | Viewport center | Modal — backdrop, focus trap, explicit close | AI content, AI image, image picker, video picker |

When an overlay has both an input area AND a result/preview area, default to a centered modal — caret-anchored popovers get cramped.

### Caret-anchored popover shell (`<dot-editor-popover>`)

All compact popovers (link, table, image-properties, emoji) share a single `EditorPopoverService` and an `<editor-popover>` shell component:

- `EditorPopoverService` (`services/editor-popover.service.ts`) — central state: which popover is open, its anchor rect, and per-popover payloads (`imagePropertiesPayload`, `linkPayload`).
- `EditorPopoverComponent` (`components/editor-popover.component.ts`) — shell wrapper: absolute positioning via `@floating-ui/dom`, `display:none` toggle, Escape + click-outside dismiss (whitelisting body-portaled PrimeNG `.p-overlay` / `.p-select-overlay` so embedded `<p-select>` stays alive), `<ng-content>` projection, auto-focus on the first form control after first paint.

Each popover content component:
- Takes `editor = input.required<Editor>()` and calls editor commands directly.
- Wraps its form in `<dot-editor-popover popoverId="...">`.
- Injects `EditorPopoverService` for open/close state and payloads.

### Centered modals via `DialogService.open()` (`EditorModalService`)

Every centered modal in the editor — AI content, AI image, image picker, video picker — is opened through PrimeNG's `DialogService.open()`, surfaced by `EditorModalService` (`services/editor-modal.service.ts`). One pattern, one teardown story:

- The editor component provides `DialogService` at the component scope so each editor instance gets its own dynamic-dialog factory. Provided in `editor.component.ts`.
- `EditorModalService` keeps one private `DynamicDialogRef` per modal kind, set to `null` between opens.
- Each `openX(editor)` method calls `dialogService.open(Component, config)` with the right `data` and subscribes to `dialogRef.onClose` to apply the result (insert nodes, mutate state) into the editor.
- Modal components inject `DynamicDialogRef` and signal a result by calling `this.dialogRef.close(result)`. Cancel/Escape/X close with no value, which the `onClose` subscriber treats as "no-op".
- `ngOnDestroy()` on the service closes every live ref so an editor unmount mid-dialog doesn't orphan an overlay.

When adding a new centered modal:
1. Build the component as a normal standalone Angular component; inject `DynamicDialogRef` and call `this.dialogRef.close(result)` on confirm.
2. Add `openYourModal(editor)` to `EditorModalService` mirroring the existing methods (private ref + idempotent guard + `onClose` subscription).
3. Add a teardown line to `ngOnDestroy()`.

Do not embed `<p-dialog>` directly inside `editor.component.ts`. The pattern above gives consistent lifecycle, focus behavior, and per-editor isolation for free.

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
| `underline` | StarterKit | Underline | any text |
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

> **AI Content note:** the `aiContent` node has no custom commands. AI-generated HTML is inserted with the standard `commands.insertContent(html)` so each block becomes a normal editable node. The node registration only exists so legacy stored content (from the old block editor, which DID wrap in `aiContent`) still parses and renders — removing it would silently drop those blocks (see "TipTap Node Names Are Immutable").

### Slash-only "actions" (don't insert a single node)

These slash entries do not map 1:1 to a node — they trigger flows that mutate the editor:

| Slash entry | Trigger |
|-------------|---------|
| AI Image | Opens `DotAIImagePromptComponent` via `DialogService.open()` (centered modal). On accept, inserts a `dotImage` node. |
| AI Content | Opens `AiContentDialogComponent` via `DialogService.open()` (centered modal). On insert, the generated HTML is parsed against the editor schema so each block becomes a normal editable node (paragraphs / headings / lists). Does NOT wrap in an `aiContent` block. |
| Content type | Opens an in-place sub-menu of allowed content types, then a contentlet picker. Inserts a `dotContent` node. |
| Image / Video | Opens `DotBrowserSelectorComponent` via `DialogService.open()` (centered modal picker). Inserts the corresponding `dotImage` / `dotVideo` node. |
| Table / Link / Emoji | Opens a caret-anchored `<dot-editor-popover>`. Insert / mutate the corresponding node. |

### Customer-supplied remote commands (`customBlocks` field variable)

Each declared `Action` becomes a slash entry that calls `editor.commands[action.command]()` on selection. The TipTap extensions resolved from the remote URLs determine which commands actually exist; missing commands log a warning instead of throwing. See `extensions/remote-extensions.loader.ts`.

### Toolbar groups (visual order)

1. History — Undo, Redo
2. Block type — paragraph / heading 1–3 select
3. Inline format — Bold, Italic, Underline, Strike, Code, Superscript, Subscript
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