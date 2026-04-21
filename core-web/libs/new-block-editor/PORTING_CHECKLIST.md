# new-block-editor — Porting Checklist

Tracks everything still missing from `new-block-editor` compared to the legacy `block-editor` lib.
Check an item off when the equivalent has been implemented (does not need to be a 1:1 copy — modern patterns are expected).

---

## Already Ported

- [x] Image extension (with text-wrap support) + image dialog (Upload / URL / dotCMS tabs)
- [x] Video extension + video dialog (Upload / URL / dotCMS tabs)
- [x] Table creation dialog (dimensions + header row toggle) + TableKit extension
- [x] Link dialog
- [x] GridBlock + GridColumn extensions
- [x] Grid column resize plugin
- [x] Contentlet node (`dotContentlet`)
- [x] Upload placeholder node (icon + label + animated progress bar during uploads)
- [x] Slash command extension + slash menu component + slash menu service
- [x] Block gutter extension (drag handle + add block button)
- [x] Emoji picker component + service
- [x] Toolbar component + toolbar state service (covers bold, italic, strikethrough, code, link, image wrap, image properties — no bubble menu needed)
- [x] Character count, word count, and reading time (shown in editor footer via `editor-character-stats.ts`)
- [x] DotCMS upload service
- [x] DotCMS content type service
- [x] DotCMS contentlet service (image + video search with pagination)

---

## Still To Port

### Nodes

- [ ] **`aiContent`** — block node for AI-generated text with loading state support
  > `block-editor/src/lib/nodes/ai-content/ai-content.node.ts`

- [ ] **`loader`** — generic spinner node for in-progress async editor operations (not upload-specific; used e.g. while waiting for AI responses)
  > `block-editor/src/lib/nodes/loader/loader.node.ts`

---

### Extensions

- [ ] **`aiContentPrompt`** — extension + component + store that opens a prompt modal for AI text generation
  > `block-editor/src/lib/extensions/ai-content-prompt/`

- [ ] **`aiImagePrompt`** — extension that opens a dialog for AI image generation
  > `block-editor/src/lib/extensions/ai-image-prompt/ai-image-prompt.extension.ts`

- [ ] **`indent`** — indent/outdent commands with configurable min/max levels for block nodes
  > `block-editor/src/lib/extensions/indent/indent.extension.ts`

- [ ] **`freezeScroll`** — toggleable extension that prevents editor scroll during modal interactions
  > `block-editor/src/lib/extensions/freeze-scroll/freeze-scroll.extension.ts`

- [ ] **`dotConfig`** — storage extension that holds editor feature configuration (readable via `editor.storage.dotConfig`)
  > `block-editor/src/lib/extensions/dot-config/dot-config.extension.ts`

- [ ] **`dotComands`** — utility extension exposing custom editor commands (e.g. `isNodeRegistered`)
  > `block-editor/src/lib/extensions/dot-comands/dot-comands.extension.ts`

---

### Table Operations UI

TableKit is already registered and handles the table structure. What's missing is a UI for in-table operations.

- [ ] **Table operations menu/dialog** — UI for merge/split cells, add/delete rows and columns, toggle header row on an existing table
  > Reference: `block-editor/src/lib/elements/dot-table/dot-table.extension.ts` and `dot-table-cell-context-menu.plugin.ts`
  > Note: Does not need to be a 1:1 copy — a toolbar, context menu, or floating panel are all valid approaches.

---

### Context Menu

- [ ] **`DotContextMenuComponent`** — right-click context menu with clipboard operations (copy/paste/cut) and HTML ↔ Markdown conversion
  > `block-editor/src/lib/elements/dot-context-menu/dot-context-menu.component.ts`

---

### Directives

- [ ] **`FloatingMenuDirective`** — registers TipTap `FloatingMenuPlugin` on a host element and manages its lifecycle
  > `block-editor/src/lib/shared/directives/floating/floating-menu.directive.ts`

- [ ] **`NodeViewContentDirective`** — marks the DOM slot for TipTap node view content (`[tiptapNodeViewContent]`)
  > `block-editor/src/lib/shared/directives/node-view-content/node-view-content.directive.ts`

---

### Pipes

- [ ] **`ContentletStatePipe`** — extracts live/working/deleted/hasLiveVersion state flags from a contentlet object
  > `block-editor/src/lib/shared/pipes/contentlet-state/contentlet-state.pipe.ts`

---

### Utilities

- [ ] **`prosemirror.utils.ts`** — helpers: `findNodeByType`, `findParentNode`, `getNodeCoords`, node position utilities
  > `block-editor/src/lib/shared/utils/prosemirror.utils.ts`

- [ ] **`parser.utils.ts`** — `contentletToJSON` serializer for mapping node data to JSON output
  > `block-editor/src/lib/shared/utils/parser.utils.ts`

- [ ] **`constants.utils.ts`** — `NodeTypes` enum, `DEFAULT_LANG_ID`, `ContentletFilters` interface, block dependency map
  > `block-editor/src/lib/shared/utils/constants.utils.ts`

---

### Angular Node View Renderer

- [ ] **`AngularNodeViewRenderer` / `AngularRenderer`** — bridge that mounts Angular components (with full DI) inside TipTap node views; needed for complex Angular-based nodes
  > `block-editor/src/lib/AngularRenderer.ts`
  > `block-editor/src/lib/NodeViewRenderer.ts`
