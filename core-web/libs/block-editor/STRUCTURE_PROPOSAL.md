# Block Editor Library – Structure Proposal

**Status: Implemented.** The structure below has been applied; the library now uses `core/`, `ui/`, `extensions/`, `nodes/`, and `shared/` under `lib/src/lib/`.

This document describes the clearer, more consistent structure for the block-editor library.

---

## Current Pain Points

1. **Inconsistent naming** – `directive` (singular) at root vs `directives` under shared; mixed “components” (root vs shared vs inside extensions/nodes).
2. **Unclear boundaries** – “elements” (bubble menu, table, add button) vs “extensions” (bubble-form, floating-button) both hold editor UI; “shared” is a catch-all (components, directives, pipes, services, utils, plugins, steps, mocks).
3. **Duplicate concepts** – `utils/` at root and `shared/utils/`; “components” in multiple places.
4. **Hard to discover** – No single mental model: is the bubble menu under elements or extensions? Where do TipTap nodes vs extensions live?
5. **SharedModule** – Still used for suggestions/empty-message; couples shared UI to a module while the rest is standalone.

---

## Proposed Structure

Group by **domain** and use **plural folder names** and clear entry points.

```
libs/block-editor/src/lib/
├── core/                          # Editor core & bootstrapping
│   ├── provide-block-editor.ts
│   ├── dot-block-editor/          # Main editor component (current components/dot-block-editor)
│   │   ├── dot-block-editor.component.ts
│   │   ├── dot-block-editor.component.html
│   │   ├── dot-block-editor.component.css
│   │   └── styles/
│   ├── dot-editor-count-bar/      # Count bar (current components/dot-editor-count-bar)
│   ├── editor-directive/          # tiptap-editor CVA (from shared/directives/editor)
│   └── node-view/                 # Angular/TipTap bridge (AngularRenderer, NodeViewRenderer)
│       ├── angular-renderer.ts
│       └── node-view-renderer.ts
│
├── ui/                            # Editor chrome: menus, overlays, toolbars
│   ├── bubble-menu/               # (current elements/dot-bubble-menu)
│   │   ├── dot-bubble-menu.component.ts
│   │   └── components/            # link popover, image popover
│   ├── context-menu/             # (current elements/dot-context-menu)
│   ├── add-button/               # (current elements/dot-add-button)
│   ├── floating-button/          # (current extensions/floating-button) – move here
│   ├── drag-handle/              # (current directive/drag-handle) – move here
│   └── table/                    # (current elements/dot-table) – extension + plugin + utils
│
├── extensions/                    # TipTap extensions only (no UI components)
│   ├── index.ts
│   ├── action-button/            # ActionsMenu
│   ├── ai-content-prompt/
│   ├── ai-image-prompt/
│   ├── asset-form/                # Keep component here; extension + plugin
│   ├── asset-uploader/
│   ├── bubble-form/               # Form used by extensions; keep next to asset-form conceptually
│   ├── dot-commands/
│   ├── dot-config/
│   ├── freeze-scroll/
│   └── indent/
│
├── nodes/                         # TipTap custom nodes (unchanged layout)
│   ├── index.ts
│   ├── contentlet-block/
│   ├── image-node/
│   ├── video/
│   ├── ai-content/
│   ├── loader/
│   └── grid-block/
│
├── shared/                        # Truly shared: directives, pipes, services, utils
│   ├── index.ts
│   ├── directives/               # draggable, floating-menu, node-view-content (keep)
│   ├── pipes/                    # contentlet-state
│   ├── services/                 # dot-marketing-config, suggestions
│   ├── utils/                    # suggestion.utils, parser, constants, prosemirror, steps
│   ├── plugins/                  # floating.plugin (if still used)
│   └── mocks/                    # for tests/storybook
│
├── shared-ui/                     # Shared editor UI (suggestions, empty message) – optional name
│   ├── suggestions/              # (current shared/components: suggestions, suggestion-list, etc.)
│   ├── empty-message/
│   └── contentlet-state-pipe/     # or keep in shared/pipes
│
├── assets/                        # Icons, images (unchanged)
└── constants/                     # Optional: move shared/utils constants here if it grows
```

---

## Principles

| Principle | Application |
|-----------|-------------|
| **Domain over type** | Group by feature (bubble-menu, table, contentlet-block) instead of by type (all “components” in one place). |
| **Plural folders** | `directives/`, `components/`, `extensions/`, `nodes/` everywhere. |
| **Core vs UI vs Extensions** | **core**: editor shell, count bar, editor directive, node-view bridge. **ui**: any visible chrome (menus, add button, drag handle, table). **extensions**: TipTap extensions + their plugins; move “floating-button” into **ui** since it’s primarily UI. |
| **Shared = reusable primitives** | Only directives, pipes, services, utils, and small shared UI (suggestions, empty-message) in shared. Feature-specific code lives next to the feature. |
| **Single utils** | One place for lib-wide utils: e.g. `shared/utils/` and remove root `utils/` (merge prosemirror, icons into shared/utils or keep root utils and drop shared/utils duplicates after audit). |
| **Barrel files** | Keep `extensions/index.ts`, `nodes/index.ts`, `shared/index.ts`. Add `core/index.ts` and `ui/index.ts` for internal imports; public API stays in `public-api.ts`. |

---

## Naming Adjustments

- **`directive/` → `ui/drag-handle/`** and **`shared/directives/editor/` → `core/editor-directive/`** so “directives” are not scattered.
- **`elements/`** → **`ui/`** and give each subfolder a clear name (bubble-menu, context-menu, add-button, table, floating-button).
- **`dot-comands`** → **`dot-commands`** (typo fix).
- **Root `utils/`** – Merge into `shared/utils/` and use a single utils entry (e.g. `shared/utils/prosemirror.ts`, `shared/utils/icons.ts`) or keep a thin root `utils/` that re-exports from shared.

---

## SharedModule

- **Option A**: Convert shared components (SuggestionsComponent, SuggestionListComponent, etc.) to standalone and remove SharedModule; import them where needed (e.g. in extensions that use suggestions).
- **Option B**: Keep SharedModule only for these shared components and document that it’s used by the block editor internally (no need to export in public API).

Prefer **Option A** for consistency with the rest of the standalone block editor.

---

## Public API (no change in surface)

Keep exporting the same symbols from `public-api.ts`; only the internal paths would change:

- `provideBlockEditor` → from `core/provide-block-editor` (or stay at root).
- `DotBlockEditorComponent` → from `core/dot-block-editor/`.
- `EditorDirective` → from `core/editor-directive/`.
- `DragHandleDirective` → from `ui/drag-handle/`.
- `DotBubbleMenuComponent` → from `ui/bubble-menu/`.
- `BubbleFormComponent` → from `extensions/bubble-form/`.
- `DotEditorCountBarComponent` → from `core/dot-editor-count-bar/`.
- `getEditorBlockOptions` → from `shared/utils/`.

---

## Migration Order (if you implement)

1. **Low risk** – Fix typo `dot-comands` → `dot-commands`; rename root `directive/` → `directives/` and align imports.
2. **Barrels** – Add `core/index.ts`, `ui/index.ts`; keep re-exports so existing imports still work.
3. **Move core** – Move `dot-block-editor`, `dot-editor-count-bar`, `EditorDirective`, `NodeViewRenderer`, `AngularRenderer`, `provide-block-editor` into `core/` and update imports.
4. **Move ui** – Move `elements/*` and `extensions/floating-button`, `directive/drag-handle` into `ui/` and update imports.
5. **Merge utils** – Consolidate root `utils/` and `shared/utils/`; single source of truth.
6. **SharedModule** – Convert shared components to standalone and remove SharedModule.
7. **Cleanup** – Remove empty folders; update any remaining deep imports to use barrels.

---

## Summary

- **core/** – Editor shell, count bar, editor directive, node-view bridge, provider.
- **ui/** – All editor chrome (bubble menu, context menu, add button, floating button, drag handle, table).
- **extensions/** – TipTap extensions and their plugins (and bubble-form/asset-form as extension-related UI).
- **nodes/** – TipTap custom nodes (unchanged).
- **shared/** – Directives, pipes, services, utils, mocks.
- **shared-ui/** (optional) – Suggestions and empty-message components, or keep under shared/components with clearer names.

This keeps the public API stable while making the library easier to navigate and refactor.
