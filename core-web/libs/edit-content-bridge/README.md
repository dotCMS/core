# Edit Content Bridge

The JavaScript API custom fields use to talk to the content form, exposed to templates as `window.DotCustomFieldApi`.

The same API is backed by two implementations, picked by where the field renders:

| Where the field renders | Implementation | What backs a field |
| --- | --- | --- |
| New Edit Content, `newRenderMode=component` | `AngularFormBridge` | The Angular `FormGroup` control |
| New Edit Content, iframe (default) | `AngularFormBridge`, handed to the iframe | The Angular `FormGroup` control |
| Legacy edit contentlet (Dojo) | `DojoFormBridge` (IIFE build) | The page's `<input>` / `<textarea>` |

Inside the iframe, `$structures.isNewEditModeEnabled()` is `false`, so a template written with the `#if( $structures.isNewEditModeEnabled() ) … #else … #end` pattern runs its legacy branch there. The migrated branch runs only in component mode.

## Usage

Put every call inside `ready()`, and get each field once:

```javascript
DotCustomFieldApi.ready(() => {
    const title = DotCustomFieldApi.getField('title');
    const slug = DotCustomFieldApi.getField('urlTitle');

    slug.setValue(slugify(title.getValue() || ''));

    title.onChange((value) => slug.setValue(slugify(value || '')));
});
```

### `DotCustomFieldApi`

| Method | Notes |
| --- | --- |
| `ready(callback)` | Calls back once the bridge is usable. Synchronous in the new editor; in the legacy editor it waits for `load`, or calls back right away if the page already loaded. |
| `getField(variable)` | Returns the field object below. |
| `openBrowserModal(options)` | Opens the asset picker (files, dotAssets, pages, menu links) and reports the pick through `onClose`. New editor only. |
| `get` / `set` / `onChangeField` | Older shortcuts for `getField(id).getValue()` / `.setValue()` / `.onChange()`. Prefer the field object. |

### Field object

| Method | Notes |
| --- | --- |
| `getValue()` | The control's value. Most fields hold a string; checkbox, multi-select, tag and category fields hold `string[]`, the Block Editor an object, dates a timestamp. In the legacy editor every value is a string (an array comma-separated, an object as JSON). |
| `setValue(value, { markDirty? })` | Sets the value without converting it, so pass the shape the field holds. Marks the field touched and dirty unless `markDirty: false`, which is for values the template derives rather than ones the user entered. |
| `onChange(callback)` | Fires once per change, including changes made with `setValue`. It does not fire for the initial value, so read that with `getValue()`. Returns an unsubscribe function. |
| `getValidationState()` / `onValidationChange(callback)` | `{ valid, invalid, touched, dirty, errors }`. `onValidationChange` emits the current state right away. A no-op in the legacy editor. |
| `show()` / `hide()` / `enable()` / `disable()` | Visibility and interactivity of the field. |

## How component mode mounts a template

`NativeFieldComponent` appends the rendered markup, copies `<style>` blocks into `document.head` (unscoped, removed on destroy) and then inserts the scripts in document order. An external `<script src>` is awaited before the scripts after it run, unless it is marked `async` or is a module, so an inline script can use a library loaded just above it.

All component-mode fields share one `window` and `document`: element ids and top-level declarations of classic scripts are global. Keep code inside the `ready()` callback, and derive ids from `$field.variable()` when a template can appear more than once on a form.

Only daisyUI classes and the Tailwind utilities listed in `apps/dotcms-ui/src/daisyui-theme.css` exist for custom fields; anything else silently produces no CSS.

## Running the tests

```bash
pnpm nx test edit-content-bridge
```
