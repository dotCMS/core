# Block Editor Library

TipTap-based rich text editor for dotCMS. Built on ProseMirror + Angular.

## Structure

```
src/lib/
├── nodes/          # Custom block types
├── extensions/     # TipTap extensions
├── components/     # Editor shell (DotBlockEditorComponent)
├── shared/         # Directives, services, utilities reused across nodes/extensions
├── elements/       # Low-level UI (menus, tables, buttons)
└── assets/         # SVG icons
```

## Adding a New Node

1. Create `nodes/{name}/{name}.node.ts` — use DOM-based `addNodeView()` for simple blocks, or `AngularNodeViewRenderer` + a component (extending `AngularNodeViewComponent`) when Angular DI or complex UI is needed. See `contentlet-block` as the reference for Angular-based nodes.
2. Export from `nodes/index.ts`.
3. Register in `DotBlockEditorComponent._customNodes` map (`components/dot-block-editor/dot-block-editor.component.ts`).
4. If the block should appear in the slash-command palette, add it to `shared/utils/suggestion.utils.ts`.

## Node `content` Expressions

Never hardcode a static list of node types in a node's `content` expression. TipTap's `StarterKit` removes disabled nodes from the schema at init time — any `content` that references a removed type throws a schema validation error and the editor renders broken.

When a node's allowed children depend on editor configuration, build the content expression dynamically from the active allowed list. See `createGridColumn(allowedBlocks)` in `grid-column.node.ts` as the pattern to follow.

## GridColumn Rules

- Always instantiate via `createGridColumn(this.allowedBlocks)` inside `DotBlockEditorComponent`, never use the bare `GridColumn` export directly.
- To allow a new node type inside grid columns, add it to `GRID_COLUMN_CONTENT_MAP` in `grid-column.node.ts`.
- `aiContent` and `loader` are intentionally absent from `GRID_COLUMN_CONTENT_MAP` — they are editor-level utilities, not column content.
- Nested grids are intentionally disabled. Do not add `gridBlock` to `GRID_COLUMN_CONTENT_MAP` — the resize plugin does not support nesting.
