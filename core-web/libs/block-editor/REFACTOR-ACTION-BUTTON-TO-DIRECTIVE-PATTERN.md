# Refactor: Replace ActionsMenu with directive + component-in-template pattern

## Goal

Remove the **action-button (ActionsMenu) extension** and use the same pattern as the reference: **directive provides element**, **plugin only positions/shows that element**, **SuggestionsComponent lives in the template**. No `createComponent()` in plugins/extensions.

---

## Current state

- **+ button**: `DotAddButtonComponent` already uses `TiptapFloatingMenuDirective` (ngx-tiptap) and on click does `editor.chain().focus().insertContent('/').run()` so "/" triggers the suggestion.
- **ActionsMenu**: Factory `(viewContainerRef, injector, ...)`. Adds two plugins:
  1. **FloatingActionsPlugin** – provides `render()` that returns `onStart`, `onKeyDown`, `onExit`.
  2. **Suggestion** (@tiptap/suggestion) – trigger char `/`. When "/" is typed (or inserted by +), Suggestion calls `onStart({ editor, range, clientRect })` → ActionsMenu creates `SuggestionsComponent` via `viewContainerRef.createComponent()`, shows it with **Floating UI** at `clientRect`, and wires commands.

So today: **+ click** → insert "/" → **Suggestion** → **onStart** → **createComponent(SuggestionsComponent)** + Floating UI. We want: **+ click** or **"/"** → plugin shows **existing** element (from directive) and notifies a service → **SuggestionsComponent in template** reacts to service and shows the list.

---

## Plan

### 1. Block suggestion service

- **New**: `BlockSuggestionService` (e.g. in `lib/shared/services/` or next to the directive).
- **State**: `{ visible: boolean, editor: Editor | null, range: Range | null, clientRect: (() => DOMRect) | null, query: string }`.
- **Methods**: `open({ editor, range, clientRect })`, `close()`, `setQuery(query)`.
- **Stream**: `state$` or a signal so the UI can react.
- **Provided**: In the block editor component (or a parent) so the host and the suggestions component share the same instance.

This is the only bridge between the (pure) plugin and the Angular suggestions UI.

### 2. + button: keep floating directive, optionally open suggestions directly

- **Keep**: `DotAddButtonComponent` with `TiptapFloatingMenuDirective` and the + icon.
- **Option A (minimal change)**: Keep `onClick()` as `insertContent('/')`. The new "/" plugin (below) will open the suggestions when "/" appears (same as now).
- **Option B (clearer UX)**: Inject `BlockSuggestionService` and on click call `service.open({ editor, range, clientRect })` with the current selection (and `posToDOMRect(editor.view, from, to)`). Then we don’t need to insert "/" for the + button; only typing "/" goes through the plugin. Choose one; both are compatible with the rest.

No need to pass a component into the + button; it stays a simple button + floating directive.

### 3. Suggestions in the HTML (template)

- In **dot-block-editor.component.html** add a **host** for the block suggestions, e.g.:

```html
<div dotBlockSuggestionHost [editor]="editor" class="block-suggestion-host">
  <dot-suggestions
    *ngIf="blockSuggestionService.visible()"
    [editor]="blockSuggestionService.editor()"
    [range]="blockSuggestionService.range()"
    [items]="..."
    (select)="onBlockSelect($event)"
  />
</div>
```

- `dotBlockSuggestionHost` is the **directive** that owns the plugin (see below). The content inside is your existing (or adapted) `SuggestionsComponent`; you can use a small wrapper component that reads from `BlockSuggestionService` and computes `items` and calls the same commands as today if that’s easier.
- So: **SuggestionsComponent is in the template**, not created by the plugin. The plugin only shows/positions the host and updates the service.

### 4. Directive + plugin (no component creation)

- **New directive**: `DotBlockSuggestionHostDirective` (e.g. selector `dotBlockSuggestionHost`).
  - **Inputs**: `editor: Editor`.
  - **ngOnInit**: Create a plugin that:
    - Uses **@tiptap/suggestion** `Suggestion({ editor, char: '/', ... })` with a `render()` that returns:
      - **onStart**: receive `{ editor, range, clientRect }`. Position the **directive host element** with **Floating UI** (`createFloatingUI(clientRect, this.hostElement, { ... })`), then call `BlockSuggestionService.open({ editor, range, clientRect })`. Do **not** call `createComponent`.
      - **onExit** / **onHide**: hide the floating element, call `BlockSuggestionService.close()`.
      - **onKeyDown**: forward to the suggestions UI (e.g. service or a callback so the component can handle ArrowUp/Down/Enter/Escape). You can keep the same keyboard behavior as today by delegating to a handler that the service or component provides.
  - **ngOnDestroy**: destroy the Floating UI instance (if any), `editor.unregisterPlugin(pluginKey)`, `BlockSuggestionService.close()`.

- The **plugin** receives only **editor**, **element** (the directive’s host), and **BlockSuggestionService** (or a minimal callback interface). No `ViewContainerRef`, no `Injector`, no `ComponentRef`. The plugin only:
  - Runs the Suggestion logic for "/",
  - Positions/shows the given **element** (via Floating UI),
  - Calls service `open` / `close` (and optionally forwards key events).

So: **directive provides the element**, **plugin uses that element and the service**. Component stays in the template.

### 5. "/" trigger only via plugin (no ActionsMenu)

- **Remove** the **ActionsMenu** extension from the editor extensions list (and delete or archive the action-button extension code once the new flow works).
- The **only** thing that handles "/" is the plugin registered by **DotBlockSuggestionHostDirective**. That plugin uses TipTap’s `Suggestion` under the hood so "/" is already handled; we just change what `onStart` does (position host element with Floating UI + service.open instead of createComponent + Floating UI with component).

### 6. Commands (addHeading, addContentletBlock, addNextLine)

- Today these are added by **ActionsMenu** via `addCommands()`. They are **editor commands**, not tied to the component.
- **Move** these commands to a **pure TipTap extension** (e.g. `DotBlockCommandsExtension` or reuse `DotComands` / a new small extension) that only does `addCommands()` and has no Angular, no plugin that creates components. The SuggestionsComponent (or wrapper) in the template will call `editor.chain().addHeading(...).run()` etc. when the user picks an item, using the same API as today.

---

## What to add vs remove

| Add | Remove / replace |
|-----|-------------------|
| `BlockSuggestionService` | ActionsMenu from extensions |
| `DotBlockSuggestionHostDirective` + plugin that uses `element` + service | `createComponent(SuggestionsComponent)` and all ViewContainerRef/Injector in action-button |
| Suggestions (or wrapper) in block editor template, bound to service | action-button extension folder (after migration) |
| Optional: small pure extension for `addHeading` / `addContentletBlock` / `addNextLine` if not already elsewhere | FloatingActionsPlugin usage inside ActionsMenu (the new plugin replaces its role for "/") |

---

## Floating directive

- **+ button**: Already uses **TiptapFloatingMenuDirective** from **ngx-tiptap**. No need to install `@tiptap/extension-floating-menu` separately unless you want to own the plugin code; ngx-tiptap already provides the directive and plugin.
- **Block suggestion host**: Does **not** use the floating menu extension for the "/" popup; it uses **Suggestion** + **Floating UI** to show the host element at the cursor. So no extra floating-menu install is required for this refactor.

---

## Summary

1. **+ button**: Keep component + floating directive; either keep "insert /" or switch to "open service at selection".
2. **SuggestionsComponent**: Rendered in the block editor template inside a host that has **DotBlockSuggestionHostDirective**.
3. **Plugin**: Registered by the directive; receives **element** (host) and **editor** (and service/callbacks); uses **Suggestion** for "/" and only positions the host and calls **BlockSuggestionService.open/close** (no `createComponent`).
4. **Commands**: Moved to a pure extension so the template-backed SuggestionsComponent can run the same commands on select.
5. **Remove**: ActionsMenu and any action-button code that creates or injects the suggestions component.

Result: **No component is passed into plugins or extensions.** The plugin is used in the **directive way**: directive provides the element and registers the plugin; the component lives in the template and reacts to the service.
