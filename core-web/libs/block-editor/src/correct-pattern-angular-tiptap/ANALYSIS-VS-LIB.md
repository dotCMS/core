# Block Editor: Correct Pattern vs Current Extensions/Nodes

This document compares `correct-pattern-angular-tiptap/` with the extensions and nodes in `lib/` and lists **which ones do not follow** the reference pattern.

---

## Reference pattern (from `correct-pattern-angular-tiptap/`)

### 1. Extension (TipTap)

- **Pure TipTap** – no Angular imports, no `ViewContainerRef`, `Injector`, or `ComponentRef`.
- **Options only** – e.g. `element: HTMLElement | null`; the extension does **not** add a ProseMirror plugin that needs an element at `Extension.create()` time. The plugin is **not** registered inside `addProseMirrorPlugins()` with Angular-provided options.
- The extension can define a plugin that receives an **element** and **editor** (and serializable options), but that plugin is **registered elsewhere** (see Angular below).

### 2. Plugin (ProseMirror)

- **Pure ProseMirror** – receives only:
  - `editor: Editor`
  - `element: HTMLElement` (the menu/UI container)
  - Optional serializable options (e.g. `pluginKey`, `shouldShow`, Floating UI options).
- **No Angular** – no `ViewContainerRef`, `Injector`, `ComponentRef`, and no subscribing to Angular `OutputRef` or component instances.
- **Menu/UI content** = whatever is inside `element` (typically provided by the Angular template that owns the element).

### 3. Angular

- A **directive** is placed on the **host element** that will act as the menu (e.g. `<tiptap-floating-menu [editor]="editor">...</tiptap-floating-menu>`).
- The **template** of that host defines the menu UI (buttons, etc.); no dynamic `createComponent()` for the menu content.
- **`ngOnInit`**: `editor.registerPlugin(Plugin({ element: this.elRef.nativeElement, editor, ... }))`.
- **`ngOnDestroy`**: `editor.unregisterPlugin(pluginKey)` and DOM cleanup.
- So: **directive owns lifecycle**, provides **one** `HTMLElement`, and the plugin only deals with that element and editor.

---

## Extensions / features that **do not follow** the standard

These either inject Angular/PrimeNG into the extension or plugin, or create Angular components inside the plugin instead of using a directive-host element.

| Extension / feature | Location | How it diverges |
|--------------------|----------|------------------|
| **ActionsMenu** (slash menu) | `lib/extensions/action-button/actions-menu.extension.ts` | Factory `(viewContainerRef, injector, ...)`. Registers plugins in `addProseMirrorPlugins()`. Plugin creates `SuggestionsComponent` via `viewContainerRef.createComponent()` and holds `ComponentRef`. No directive host for the menu; menu is a dynamically created Angular component. |
| **Floating button** (Import to dotCMS) | `lib/ui/floating-button/` | Directive creates `FloatingButtonComponent` and passes **element + ComponentRef + onClick$** to the plugin. Plugin uses `ComponentRef.setInput()` and subscribes to `onClick$`. Correct pattern would be: directive host = menu element, plugin receives only `element` (and editor); menu content would be template inside the directive, no ComponentRef in the plugin. |
| **DotTableCellContextMenu** | `lib/ui/table/dot-table-cell-context-menu.plugin.ts` | Factory `(viewContainerRef, injector)`. Plugin creates `SuggestionsComponent` via `viewContainerRef.createComponent(..., { injector })`. No directive host; table menu is a dynamically created component. |
| **BubbleFormExtension** | `lib/extensions/bubble-form/bubble-form.extension.ts` | Factory `(viewContainerRef)`. In `addProseMirrorPlugins()` the plugin calls `viewContainerRef.createComponent(BubbleFormComponent)` and holds `ComponentRef<BubbleFormComponent>`. |
| **BubbleAssetFormExtension** | `lib/extensions/asset-form/asset-form.extension.ts` | Same idea: factory `(viewContainerRef)`, plugin creates `AssetFormComponent` via `viewContainerRef.createComponent()` and keeps a `ComponentRef`. |
| **AIContentPromptExtension** | `lib/extensions/ai-content-prompt/ai-content-prompt.extension.ts` | Factory `(viewContainerRef)`. Registers a plugin that creates `AIContentPromptComponent` with `viewContainerRef.createComponent()`. Plugin holds `ComponentRef<AIContentPromptComponent>`. |
| **AssetUploader** | `lib/extensions/asset-uploader/asset-uploader.extension.ts` | Factory `(injector, viewContainerRef)`. Plugin creates `UploadPlaceholderComponent` via `viewContainerRef.createComponent()`. Uses Angular `Injector` and `ComponentRef`. |
| **AIImagePromptExtension** | `lib/extensions/ai-image-prompt/ai-image-prompt.extension.ts` | Factory `(dialogService, dotMessageService)`. Plugin receives **Angular/PrimeNG services** and opens `DotAIImagePromptComponent` via `DialogService`. Plugin is Angular- and PrimeNG-aware. |

---

## Extensions that **follow** the standard (pure TipTap / no Angular in plugin)

| Extension | Location | Why it matches |
|-----------|----------|----------------|
| **DotConfigExtension** | `lib/extensions/dot-config/dot-config.extension.ts` | Pure TipTap extension; only storage/data, no plugin with DOM or Angular. |
| **DotComands** | `lib/extensions/dot-commands/dot-commands.extension.ts` | Pure TipTap; only commands, no Angular. |
| **IndentExtension** | `lib/extensions/indent/indent.extension.ts` | Pure TipTap; commands + PM plugin, no Angular. |
| **FreezeScroll** | `lib/extensions/freeze-scroll/freeze-scroll.extension.ts` | Pure TipTap; plugin is ProseMirror-only, no Angular. |

---

## Directive that **follows** the pattern

| Directive | Location | Why it matches |
|------------|----------|----------------|
| **FloatingMenuDirective** | `lib/shared/directives/floating/floating-menu.directive.ts` | Uses `editor.registerPlugin(FloatingMenuPlugin({ element: this._el.nativeElement, editor, ... }))`. Menu content is the directive host content. No `createComponent()`, no `ComponentRef` in the plugin. |

Note: The main block editor UI uses **ActionsMenu** (and other custom extensions) for the slash menu, not this `FloatingMenuDirective`.

---

## Nodes

| Node | Location | Follows? | Notes |
|------|----------|----------|--------|
| **ContentletBlock** | `lib/nodes/contentlet-block/contentlet-block.node.ts` | Different pattern | Factory `(injector)`. Uses `AngularNodeViewRenderer(ContentletBlockComponent)`. Node views that render Angular components require an injector; this is the standard TipTap+Angular pattern for **node views**, not for floating/overlay UI. |
| **GridBlock** | `lib/nodes/grid-block/grid-block.node.ts` | Yes | Node + `GridResizePlugin(editor)`. Plugin is pure ProseMirror (vanilla DOM), no Angular. |
| **ImageNode** | `lib/nodes/image-node/image.node.ts` | Yes | Pure TipTap node (no Angular in plugin). |
| **VideoNode** | `lib/nodes/video/video.node.ts` | Yes | Pure TipTap node. |
| **AIContentNode** | `lib/nodes/ai-content/ai-content.node.ts` | Yes | Pure TipTap node. |
| **LoaderNode** | `lib/nodes/loader/loader.node.ts` | Yes | Pure TipTap node. |
| **GridColumn** | `lib/nodes/grid-block/grid-column.node.ts` | Yes | Pure TipTap node. |

So: the only node that is “Angular-aware” is **ContentletBlock** (injector for `AngularNodeViewRenderer`), which is the expected pattern for custom **node views** with Angular components. The reference in `correct-pattern-angular-tiptap/` only shows a **floating menu** (overlay UI), not node views.

---

## Summary table

| Category | Follow pattern | Do not follow |
|----------|----------------|---------------|
| **Extensions** | DotConfig, DotComands, Indent, FreezeScroll | ActionsMenu, Floating button (plugin/directive), DotTableCellContextMenu, BubbleForm, BubbleAssetForm, AIContentPrompt, AssetUploader, AIImagePrompt |
| **Directives** | FloatingMenuDirective (shared) | DotFloatingButtonDirective (plugin receives ComponentRef + Observable) |
| **Nodes** | GridBlock, ImageNode, VideoNode, AIContentNode, LoaderNode, GridColumn | ContentletBlock (injector for node view only; acceptable for node-view pattern) |

---

## What “following the standard” would imply (for the non‑conforming ones)

1. **Floating / overlay UI (menus, buttons, forms)**  
   - A **directive** on a host element provides the **single** `HTMLElement` and calls `editor.registerPlugin(Plugin({ element, editor, ... }))`.  
   - The **plugin** never receives `ViewContainerRef`, `Injector`, or `ComponentRef`; it only receives `element` and editor (and optional serializable options).  
   - Menu/button/form content would be implemented as the **template inside the directive host** (or as vanilla DOM created by the plugin), not as dynamically created Angular components.

2. **Where dynamic Angular components are required**  
   - If the menu/content truly must be an Angular component (e.g. for forms, suggestions, dialogs), the reference pattern in `correct-pattern-angular-tiptap/` does not cover that. Aligning with it would mean either:  
     - Moving that UI into the directive host template and passing only `element` to the plugin, or  
     - Explicitly documenting these as “Angular-integration extensions” that intentionally diverge from the reference pattern.

3. **Node views**  
   - Using `Injector` + `AngularNodeViewRenderer(Component)` for **node views** is the standard approach in TipTap + Angular and is a different pattern from the floating-menu example; **ContentletBlock** fits that node-view pattern rather than the floating-menu pattern.
