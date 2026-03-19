# VTL Migration Guide: Complete Reference

## Table of Contents
1. [The New DotCustomFieldApi](#the-new-dotcustomfieldapi)
2. [Migration Rules](#migration-rules)
3. [Styling with DaisyUI](#styling-with-daisyui)
4. [Best Practices](#best-practices)
5. [Step-by-Step Checklist](#step-by-step-checklist)
6. [Common Pitfalls](#common-pitfalls)
7. [Special Cases](#special-cases)
8. [Complete Migration Examples](#complete-migration-examples)

---

## The New DotCustomFieldApi

The new DotCustomFieldApi provides a cleaner, more maintainable approach to working with custom fields:

```js
DotCustomFieldApi.ready(() => {
  const field = DotCustomFieldApi.getField("variableName");

  // Get value
  const value = field.getValue();

  // Set value
  field.setValue("new value");

  // Watch for changes
  field.onChange((value) => {
    console.log(value);
  });

  // Visibility control
  field.hide();  // hides the field (input + label) from view
  field.show();  // restores the field's visibility

  // State control
  field.disable(); // prevents user interaction, applies disabled styling
  field.enable();  // restores interactivity
});
```

### Field Object Methods

| Method | Description |
|--------|-------------|
| `getValue()` | Returns the current value of the field |
| `setValue(value)` | Sets the field's value |
| `onChange(callback)` | Subscribes to value changes |
| `show()` | Makes the field visible (input and associated label) |
| `hide()` | Hides the field from view (input and associated label) |
| `enable()` | Restores interactivity to the field |
| `disable()` | Prevents user interaction and applies disabled styling |

Always wrap all field access code inside `DotCustomFieldApi.ready()` to ensure the API is initialized.

---

## Migration Rules

### Rule 1: Use `getField()` to obtain a field reference

Always use `DotCustomFieldApi.getField('variableName')` to get a field. This returns a field object with `getValue()`, `setValue()`, and `onChange()`.

Avoid the deprecated `DotCustomFieldApi.get('variableName')`.

### Rule 2: Use `field.getValue()` to read the field's value

**Old (deprecated):**
```js
const textEntered = DotCustomFieldApi.get("variableName");
```

**New:**
```js
const field = DotCustomFieldApi.getField("variableName");
const textEntered = field.getValue();
```

### Rule 3: Use `field.setValue()` to write a value

**Old (deprecated):**
```js
DotCustomFieldApi.set("variableName", "new value");
```

**New:**
```js
const field = DotCustomFieldApi.getField("variableName");
field.setValue("new value");
```

### Rule 4: Use `field.onChange()` to subscribe to changes

**Old (deprecated):**
```js
DotCustomFieldApi.onChangeField("variableName", (value) => {
  console.log(value);
});
```

**New:**
```js
const field = DotCustomFieldApi.getField("variableName");
field.onChange((value) => {
  console.log(value);
});
```

### Rule 5: Wrap field access in `DotCustomFieldApi.ready()`

All field access code must be inside this callback to prevent race conditions.

**Replace:** `dojo.ready()` → `DotCustomFieldApi.ready()`

**Old (deprecated):**
```js
dojo.ready(function () {
  // code here
});
```

**New:**
```js
DotCustomFieldApi.ready(() => {
  // code here
});
```

### Rule 6: Remove all Dojo/Dijit dependencies

| Deprecated | Replacement |
|---|---|
| `dojo.ready()` | `DotCustomFieldApi.ready()` |
| `dojo.byId()` | `document.getElementById()` |
| `dojo.require()` | Remove entirely |
| `dijit.byId()` | `DotCustomFieldApi.getField()` |
| `dijit.form.*` | Native HTML elements |

**Old (deprecated):**
```js
dojo.ready(function () {
  var url = dijit.byId("url");
  if (url && url.get("value").trim() === "") {
    url.set("value", "new-value");
  }
});
```

**New:**
```js
DotCustomFieldApi.ready(() => {
  const urlField = DotCustomFieldApi.getField("url");
  const urlValue = urlField.getValue() || "";
  if (urlValue.trim() === "") {
    urlField.setValue("new-value");
  }
});
```

### Rule 7: Remove Dijit CSS classes and use DaisyUI for styling

Remove any class starting with `dijit` from HTML elements. **When applying or replacing styles, use DaisyUI component classes** so the field looks consistent with the platform and respects the theme (see [Styling with DaisyUI](#styling-with-daisyui)).

**Classes to remove:** `dijitTextBox`, `dijitPlaceHolder`, `dijitSelect`, `dijitButton`, `dijitDropDownButton`, `dijitDialog`, and any other `dijit*` class.

**Old (deprecated):**
```html
<input type="text" id="slugInput" class="dijitTextBox" style="background:#FAFAFA" />
```

**New (prefer DaisyUI semantic components):**
```html
<input type="text" id="slugInput" class="input input-bordered w-full" />
```

Avoid inline styles when a DaisyUI component or Tailwind utility exists. Preserve custom CSS only when it implements behavior that DaisyUI + Tailwind cannot cover.

### Rule 8: Remove `dojoType` attributes — use semantic HTML and DaisyUI components

Use **DaisyUI component classes** for buttons, inputs, selects, modals, and links so the UI is semantic and themeable. The admin UI includes DaisyUI 5 + Tailwind CSS.

| Old (deprecated) | New (DaisyUI semantic) |
|---|---|
| `<input dojoType="dijit.form.TextBox" />` | `<input type="text" class="input input-bordered" />` |
| `<input dojoType="dijit.form.Button" />` | `<button type="button" class="btn">Label</button>` |
| `<select dojoType="dijit.form.FilteringSelect" />` | `<select class="select select-bordered">...</select>` |
| `<div dojoType="dijit.Dialog" />` | `<button class="btn" onclick="my_modal_1.showModal()">open modal</button> <dialog id="my_modal_1" class="modal"><div class="modal-box">...</div></dialog>` |
| `<input dojoType="dijit.form.RadioButton" />` | `<input type="radio" class="radio" />` |
| `<div dojoType="dojox.widget.ColorPicker" />` | `<input type="color" class="color-picker" />` |

**Old (deprecated):**
```html
<input type="text" id="slugInput" dojoType="dijit.form.TextBox" />
<div id="videoResultsDiv" dojoType="dijit.Dialog" style="display: none"></div>
```

**New (DaisyUI):**
```html
<input type="text" id="slugInput" class="input input-bordered" />
<dialog id="videoResultsDiv" class="modal"></dialog>
```

### Rule 9: Native Dialog Implementation (DaisyUI modal)

For `dojoType="dijit.Dialog"` elements, use the native HTML `<dialog>` element with **DaisyUI modal** classes: `modal`, `modal-box`, `modal-action`.

```html
<button type="button" id="openModalButton" class="btn btn-primary">Open modal</button>
<dialog id="myDialog" class="modal">
  <div class="modal-box">
    <h3 class="font-bold text-lg">Hello!</h3>
    <p class="py-4">Press ESC key or click the button below to close</p>
    <div class="modal-action">
      <form method="dialog">
        <button type="submit" class="btn">Close</button>
      </form>
    </div>
  </div>
</dialog>
<script>
  const myDialog = document.getElementById('myDialog');
  const openModalButton = document.getElementById('openModalButton');
  openModalButton?.addEventListener('click', () => {
    myDialog?.showModal();
  });
</script>
```

### Rule 10: Replace inline event handlers with `addEventListener()`

**Old (deprecated):**
```html
<button onclick="handleClick()">Click</button>
<input onkeyup="handleInput()" />
```

**New (DaisyUI for semantic styling):**
```html
<button type="button" id="myButton" class="btn">Click</button>
<input type="text" id="myInput" class="input input-bordered" />

<script>
  DotCustomFieldApi.ready(() => {
    document.getElementById("myButton").addEventListener("click", handleClick);
    document.getElementById("myInput").addEventListener("keyup", handleInput);
  });
</script>
```

### Rule 11: File and Page Browser Dialog

**Old (deprecated):**
```html
<script type="application/javascript">
  dojo.require("dotcms.dijit.FileBrowserDialog");
  function browseRedirectPage() {
    pageSelector.show();
  }
</script>
<div
  dojoAttachPoint="fileBrowser"
  jsId="pageSelector"
  onFileSelected="redirectPageSelected"
  mimeTypes="application/dotpage"
  dojoType="dotcms.dijit.FileBrowserDialog"
></div>
```

**New:**
```html
<script type="application/javascript">
  DotCustomFieldApi.ready(() => {
    // Select a Page
    const pageSelectorModal = DotCustomFieldApi.openBrowserModal({
      header: "Select a Page",
      params: {
        mimeTypes: ["application/dotpage"],
      },
      onClose: (result) => console.log(result),
    });
    pageSelectorModal.close(); // close programmatically if needed

    // Select an Image
    const imageSelectorModal = DotCustomFieldApi.openBrowserModal({
      header: "Select an Image",
      params: {
        mimeTypes: ["image"],
      },
      onClose: (result) => console.log(result),
    });

    // Select a File
    const fileSelectorModal = DotCustomFieldApi.openBrowserModal({
      header: "Select a File",
      params: {
        includeDotAssets: true,
      },
      onClose: (result) => console.log(result),
    });
  });
</script>
```

### Rule 12: Use `field.show()` / `field.hide()` and `field.enable()` / `field.disable()` for field visibility and state

When toggling a field's visibility or enabled/disabled state, use the built-in API methods instead of manipulating the DOM directly. These methods handle both the input element and its associated label.

**Old (manual DOM manipulation):**
```js
// Hiding a field by manipulating DOM
const fieldEl = document.getElementById("mediafile");
fieldEl.style.display = "none";
// or
fieldEl.closest(".field-wrapper").classList.add("hidden");

// Disabling a field by manipulating DOM
fieldEl.setAttribute("disabled", "true");
```

**New (API methods):**
```js
DotCustomFieldApi.ready(() => {
  const mediaFileField = DotCustomFieldApi.getField("mediafile");

  // Visibility
  mediaFileField.hide();  // hides the field + label
  mediaFileField.show();  // restores visibility

  // State
  mediaFileField.disable(); // blocks editing + disabled styling
  mediaFileField.enable();  // restores interactivity
});
```

**Conditional visibility based on another field's value:**
```js
DotCustomFieldApi.ready(() => {
  const mediaField = DotCustomFieldApi.getField("media");
  const mediaFileField = DotCustomFieldApi.getField("mediafile");

  const toggleVisibility = (value) => {
    if (value === "upload") {
      mediaFileField.show();
    } else {
      mediaFileField.hide();
    }
  };

  // Set initial visibility
  toggleVisibility(mediaField.getValue() || "external");

  // React to changes
  mediaField.onChange(toggleVisibility);
});
```

### Native Components and Platform Styles

Custom fields run inside the admin Angular app, which uses **DaisyUI 5** and **Tailwind CSS**. Prefer **DaisyUI component classes** for semantic styling so the field matches the platform and respects the theme.

**Use DaisyUI for UI components** (see [Styling with DaisyUI](#styling-with-daisyui)):
- Buttons: `btn`, `btn-primary`, `btn-ghost`, `btn-sm`, etc.
- Inputs: `input`, `input-bordered`
- Selects: `select`, `select-bordered`
- Modals: `modal`, `modal-box`, `modal-action`
- Links: `link`, `link-primary`
- Cards: `card`, `card-body`, `card-title`, `card-actions`

**Use PrimeIcons for icons** (no extra CSS):
```html
<button type="button" class="btn btn-ghost btn-sm" id="clearRedirectButton" aria-label="Clear redirect URL">
  <i class="pi pi-times"></i>
</button>
<button type="button" class="btn btn-outline" id="customSelectButton">
  <span id="templateSelectButtonLabel">All Sites</span>
  <i class="pi pi-chevron-down"></i>
</button>
```

When updating the label in a button with an icon, update only the `<span>` — never overwrite `innerHTML` of the whole button or the icon will disappear.

**Use Tailwind utilities** for layout (`flex`, `gap`, `py-4`, `w-full`) instead of custom CSS when possible.

---

## Styling with DaisyUI

Whenever you apply styles in a migrated custom field, use **DaisyUI component classes** so the UI is semantic, consistent with the admin, and theme-aware. The dotCMS admin UI uses DaisyUI 5 + Tailwind CSS 4.

### When to use DaisyUI

- **Replacing Dijit widgets:** Use the DaisyUI equivalent (e.g. `input`, `btn`, `select`, `modal`) instead of inline styles or generic HTML.
- **Adding new controls:** Prefer DaisyUI components first; only add custom CSS when DaisyUI + Tailwind are not enough.
- **Colors:** Use DaisyUI semantic colors (`btn-primary`, `text-primary`, `link-primary`, `bg-base-200`) so the field adapts to the theme; avoid hardcoded hex when possible.

### Component quick reference

| Element | DaisyUI classes | Notes |
|--------|------------------|--------|
| **Button** | `btn` | Add `btn-primary`, `btn-ghost`, `btn-sm`, `btn-lg`, etc. |
| **Text input** | `input input-bordered` | Optional: `input-sm`, `w-full` |
| **Select** | `select select-bordered` | Optional: `select-sm`, `w-full` |
| **Modal** | `modal`, `modal-box`, `modal-action` | Use with native `<dialog>` |
| **Link** | `link` or `link link-primary` | For text links and “Use: …” suggestions |
| **Card** | `card`, `card-body`, `card-title`, `card-actions` | For grouped content |
| **Radio** | `radio` | On `<input type="radio">` |
| **Checkbox** | `checkbox` | On `<input type="checkbox">` |
| **Label** | `label` | For form labels |

### Examples

**Input (replace inline style):**
```html
<!-- Avoid: style="background:#FAFAFA" -->
<input type="text" id="slugInput" class="input input-bordered w-full" />
```

**Suggestion link (semantic link):**
```html
<a href="#" class="link link-primary" id="suggestionLink">Use: my-slug</a>
```

**Button with icon:**
```html
<button type="button" class="btn btn-ghost btn-sm" aria-label="Clear">
  <i class="pi pi-times"></i>
</button>
```

**Dialog (modal):**
```html
<dialog id="myDialog" class="modal">
  <div class="modal-box">
    <h3 class="font-bold text-lg">Title</h3>
    <div class="modal-action">
      <form method="dialog">
        <button type="button" class="btn btn-primary">OK</button>
      </form>
    </div>
  </div>
</dialog>
```

Use **Tailwind utilities** for layout and spacing (`flex`, `gap-2`, `py-4`, `mt-2`, `w-full`). Prefer DaisyUI for components and semantic colors; use `!` only when you must override (e.g. `btn bg-error!`).

---

## Best Practices

### Code Organization

- Initialize field references once inside `DotCustomFieldApi.ready()` and reuse them
- Define helper functions outside of `DotCustomFieldApi.ready()` when they don't need field access
- Use meaningful variable names: `titleField`, `urlField` rather than just `field`
- Group related field references together at the top of `ready()`

**Good pattern:**
```js
// Helper outside ready()
function slugifyText(text) {
  return text.toLowerCase().trim().replace(/[^a-z0-9]+/g, "-");
}

DotCustomFieldApi.ready(() => {
  // All field refs at the top
  const titleField = DotCustomFieldApi.getField("title");
  const urlField = DotCustomFieldApi.getField("url");

  titleField.onChange((value) => {
    urlField.setValue(slugifyText(value));
  });
});
```

### Error Handling

Always guard against null/undefined values:
```js
DotCustomFieldApi.ready(() => {
  const field = DotCustomFieldApi.getField("fieldName");
  const value = field.getValue() || ""; // Default to empty string

  const element = document.getElementById("myElement");
  if (element) {
    element.textContent = value;
  }
});
```

### Security

**Never use `innerHTML` with string interpolation or inline event handlers** — they introduce XSS vulnerabilities if user-controlled values are involved:

```js
// BAD — XSS risk: quotes in newSlug can break out of the onclick string
suggestion.innerHTML = `
  <a href="#" onclick="applySuggestion('${newSlug}'); return false">
    Use: ${newSlug}
  </a>
`;

// GOOD — create elements programmatically; textContent is XSS-safe
suggestion.innerHTML = "";
const link = document.createElement("a");
link.textContent = `Use: ${newSlug}`; // textContent escapes HTML automatically
link.addEventListener("click", (e) => {
  e.preventDefault();
  applySuggestion(newSlug);
});
suggestion.appendChild(link);
```

This is also why Rule 10 mandates `addEventListener` over inline `onclick=` / `onkeyup=` attributes.

### Script Type Attributes

Be consistent when writing `<script>` tags in migrated VTL files:

| Type | When to use |
|------|-------------|
| `<script type="application/javascript">` | Standard scripts — the default convention in this codebase |
| `<script type="module">` | When you need ES module scope (top-level `const`/`let` not exposed globally) |
| `<script>` (no attribute) | Equivalent to `application/javascript`, but omitting the attribute is less explicit |

**Prefer `type="application/javascript"` for consistency** unless you specifically need module semantics. Do not mix bare `<script>` with typed variants within the same file or guide examples.

### What to Preserve

**DO NOT change:**
- VTL variables: `${fieldId}`, `$maxChar`, `$variableName`
- Business logic and algorithms
- HTML structure (except removing dojoType/dijit attributes)
- Comments (but translate to English if written in another language)
- Function names and variable names (unless they reference deprecated APIs)

**DO change:**
- API method calls: get/set/onChangeField → getField/getValue/setValue/onChange
- Manual DOM show/hide of fields → `field.show()` / `field.hide()`
- Manual DOM enable/disable of fields → `field.enable()` / `field.disable()`
- `dojo.ready` → `DotCustomFieldApi.ready`
- `dojo.byId` → `document.getElementById`
- `dijit.byId` → `DotCustomFieldApi.getField`
- Remove `dojoType` attributes
- Remove `dijit*` CSS classes
- Replace inline event handlers with `addEventListener`
- **Styling:** Prefer DaisyUI component classes + Tailwind utilities over inline styles and ad-hoc custom CSS; preserve custom CSS only when it cannot be replaced by DaisyUI/Tailwind

---

## Step-by-Step Checklist

### 1. Identify deprecated patterns
- [ ] `DotCustomFieldApi.get(` → Replace with `getField().getValue()`
- [ ] `DotCustomFieldApi.set(` → Replace with `getField().setValue()`
- [ ] `DotCustomFieldApi.onChangeField(` → Replace with `getField().onChange()`
- [ ] `dojo.ready` → Replace with `DotCustomFieldApi.ready()`
- [ ] `dojo.byId` → Replace with `document.getElementById()`
- [ ] `dijit.byId` → Replace with `DotCustomFieldApi.getField()`
- [ ] `dojoType=` → Remove attribute, update HTML element type
- [ ] `class="dijit` → Remove dijit classes
- [ ] `onclick=`, `onkeyup=`, etc. → Replace with `addEventListener()`

### 2. Wrap field access
- [ ] All `DotCustomFieldApi.getField()` calls are inside `DotCustomFieldApi.ready()`
- [ ] Field references initialized once and reused

### 3. Update API calls
- [ ] All `DotCustomFieldApi.get('name')` → `getField('name').getValue()`
- [ ] All `DotCustomFieldApi.set('name', value)` → `getField('name').setValue(value)`
- [ ] All `DotCustomFieldApi.onChangeField('name', cb)` → `getField('name').onChange(cb)`
- [ ] Manual DOM show/hide of fields → `getField('name').show()` / `.hide()`
- [ ] Manual DOM enable/disable of fields → `getField('name').enable()` / `.disable()`

### 4. Remove Dojo/Dijit
- [ ] Remove all `dojo.require()` statements
- [ ] Replace `dojo.ready()` with `DotCustomFieldApi.ready()`
- [ ] Replace `dojo.byId()` with `document.getElementById()`
- [ ] Replace `dijit.byId()` with `DotCustomFieldApi.getField()`

### 5. Update HTML elements
- [ ] Remove all `dojoType` attributes
- [ ] Replace `<div dojoType="dijit.Dialog">` with `<dialog class="modal">` and use `modal-box`, `modal-action`
- [ ] Remove all `class="dijit*"` classes
- [ ] Apply DaisyUI component classes for buttons, inputs, selects, modals, links (see [Styling with DaisyUI](#styling-with-daisyui))
- [ ] Prefer DaisyUI + Tailwind over inline styles; preserve custom CSS only when necessary

### 6. Update event handlers
- [ ] Move inline event handlers to `addEventListener()` calls inside `DotCustomFieldApi.ready()`

### 7. Verify preservation and styling
- [ ] All VTL variables (`${fieldId}`, `$variable`) remain unchanged
- [ ] Business logic unchanged
- [ ] Functionality remains identical
- [ ] Field visibility uses `field.show()` / `field.hide()` instead of manual DOM manipulation
- [ ] Field state uses `field.enable()` / `field.disable()` instead of manual DOM attribute changes
- [ ] Styling uses DaisyUI components where applicable; custom CSS only when DaisyUI/Tailwind are insufficient

---

## Common Pitfalls

### Don't call `getField()` multiple times for the same field

```js
// BAD
DotCustomFieldApi.ready(() => {
  DotCustomFieldApi.getField("title").setValue("New Title");
  DotCustomFieldApi.getField("title").getValue(); // redundant call
});

// GOOD
DotCustomFieldApi.ready(() => {
  const titleField = DotCustomFieldApi.getField("title");
  titleField.setValue("New Title");
  const value = titleField.getValue();
});
```

### Don't access fields outside `DotCustomFieldApi.ready()`

```js
// BAD — race condition
const field = DotCustomFieldApi.getField("title");
field.setValue("value");

// GOOD
DotCustomFieldApi.ready(() => {
  const field = DotCustomFieldApi.getField("title");
  field.setValue("value");
});
```

### Don't forget to handle null/undefined

```js
// BAD
const value = field.getValue();
const length = value.length; // Error if null

// GOOD
const value = field.getValue() || "";
const length = value.length;
```

### Don't manually manipulate DOM for field visibility or state

```js
// BAD — manual DOM manipulation
const el = document.querySelector('[data-field="mediafile"]');
el.style.display = "none";
el.setAttribute("disabled", "true");

// GOOD — use the API
const mediaFileField = DotCustomFieldApi.getField("mediafile");
mediaFileField.hide();
mediaFileField.disable();
```

### Don't mix old and new APIs

```js
// BAD
DotCustomFieldApi.ready(() => {
  const field = DotCustomFieldApi.getField("title");
  DotCustomFieldApi.set("url", "value"); // Old API!
});

// GOOD
DotCustomFieldApi.ready(() => {
  const titleField = DotCustomFieldApi.getField("title");
  const urlField = DotCustomFieldApi.getField("url");
  urlField.setValue("value");
});
```

---

## Special Cases

### Multiple `onChange` handlers for the same field

Combine them into one:

**Old:**
```js
DotCustomFieldApi.onChangeField("title", (value) => { updateURL(value); });
DotCustomFieldApi.onChangeField("title", (value) => { updateFriendlyName(value); });
```

**New:**
```js
DotCustomFieldApi.ready(() => {
  const titleField = DotCustomFieldApi.getField("title");
  titleField.onChange((value) => {
    updateURL(value);
    updateFriendlyName(value);
  });
});
```

### Preserving initial values

```js
DotCustomFieldApi.ready(() => {
  const field = DotCustomFieldApi.getField("fieldName");
  const initialValue = field.getValue() || "";
  let previousValue = initialValue;

  field.onChange((value) => {
    if (value !== previousValue) {
      previousValue = value;
    }
  });
});
```

---

## Complete Migration Examples

### Example 1: Character Counter Field

**Old (text-count.vtl):**
```html
<style>
  #legacy-custom-field-body .${fieldId}_countWrapper{
      margin: 0;
  }
  #${fieldId}Count_tag{
      display: none;
  }
  .${fieldId}_countWrapper {
      display: flex;
      justify-content: space-between;
      flex-wrap: wrap;
      color: #6c7389;
      font-size: 0.875rem;
      line-height: 0.875rem;
      margin-top: -1.1rem;
  }
  .${fieldId}_maxChar {
      padding: 0 5px;
  }
</style>

<div class="${fieldId}_countWrapper">
  <div id="${fieldId}-counter-text">
    <span id="charactersRemaining-${fieldId}">$maxChar</span> characters
  </div>
  <div>Recommended Max $maxChar characters</div>
</div>

<script>
  DotCustomFieldApi.ready(() => {
    function updateCharacterCount() {
      const textEntered = DotCustomFieldApi.get("${fieldId}") || "";
      const counter = textEntered.length;
      const countRemaining = document.getElementById("charactersRemaining-${fieldId}");
      const counterText = document.getElementById("${fieldId}-counter-text");

      countRemaining.textContent = counter;
      counterText.style.color = counter <= $maxChar ? "#6c7389" : "red";
    }

    updateCharacterCount();

    DotCustomFieldApi.onChangeField("${fieldId}", (value) => {
      updateCharacterCount();
    });
  });
</script>
```

**New (text-count.vtl):** Prefer DaisyUI/Tailwind for styling when replacing or adding styles. Example using semantic classes for the wrapper:
```html
<style>
  #legacy-custom-field-body .${fieldId}_countWrapper { margin: 0; }
  #${fieldId}Count_tag { display: none; }
</style>

<div class="${fieldId}_countWrapper flex justify-between flex-wrap text-base-content/70 text-sm mt-[-1.1rem]">
  <div id="${fieldId}-counter-text">
    <span id="charactersRemaining-${fieldId}">$maxChar</span> characters
  </div>
  <div class="px-1">Recommended Max $maxChar characters</div>
</div>

<script type="module">
  function updateCharacterCount(textEntered) {
    const counter = textEntered.length;
    const countRemaining = document.getElementById("charactersRemaining-${fieldId}");
    const counterText = document.getElementById("${fieldId}-counter-text");

    countRemaining.textContent = counter;
    counterText.className = counter <= $maxChar ? "text-base-content/70" : "text-error";
  }

  DotCustomFieldApi.ready(() => {
    const field = DotCustomFieldApi.getField("${fieldId}");
    field.onChange((value) => {
      updateCharacterCount(value);
    });
    updateCharacterCount(field.getValue() || "");
  });
</script>
```

---

### Example 2: Title Field with Auto-generated URL and Friendly Name

**Old (title_custom_field.vtl — using dijit.form API):**
```html
<script type="application/javascript">
  dojo.ready(function () {
    var titleBox = new dijit.form.TextBox(
      {
        name: "titleBox",
        value: dojo.byId("title").value,
        onChange: function () {
          // dojo.byId("title") refers to the hidden input dotCMS generates
          // for the actual field value (id matches the field variable name).
          // "titleBox" is the visible Dijit widget wrapping it.
          dojo.byId("title").value = this.get("value");

          var url = dijit.byId("url");
          if (url && url.get("value").trim() === "") {
            url.set(
              "value",
              this.get("value")
                .toLowerCase()
                .trim()
                .replace(/[^a-zA-Z0-9]+/g, "-")
                .replace(/-+$|^-+/g, ""),
            );
          }

          var fname = dijit.byId("friendlyName");
          if (fname && fname.get("value").trim() === "") {
            fname.set("value", this.get("value"));
          }
        },
        onKeyDown: function () {
          dojo.byId("title").value = this.get("value");
        },
      },
      "titleBox",
    );
  });
</script>
<!-- Hidden input managed by dotCMS for the actual field value -->
<input type="hidden" id="title" />
<!-- Visible Dijit widget -->
<input id="titleBox" />
```

> **Note:** In the old pattern, `dojo.byId("title")` refers to a hidden input element that dotCMS generates for each field (its `id` matches the field's variable name). The visible `<input id="titleBox">` is the Dijit widget overlay. In the new pattern, `DotCustomFieldApi.getField("title")` replaces both — there is no need to manage hidden inputs directly.

**New (title_custom_field.vtl):** Use DaisyUI `input` for semantic styling:
```html
<script type="application/javascript">
  DotCustomFieldApi.ready(() => {
    const titleField = DotCustomFieldApi.getField("title");
    const urlField = DotCustomFieldApi.getField("url");
    const friendlyNameField = DotCustomFieldApi.getField("friendlyName");

    const titleBox = document.getElementById("titleBox");
    titleBox.value = titleField.getValue() || "";

    titleBox.addEventListener("blur", () => {
      const currentTitleValue = titleBox.value;

      const urlValue = urlField.getValue() || "";
      if (urlValue.trim() === "") {
        const slugValue = currentTitleValue
          .toLowerCase()
          .trim()
          .replace(/[^a-zA-Z0-9]+/g, "-")
          .replace(/-+$|^-+/g, "");
        urlField.setValue(slugValue);
      }

      const friendlyNameValue = friendlyNameField.getValue() || "";
      if (friendlyNameValue.trim() === "") {
        friendlyNameField.setValue(currentTitleValue);
      }
    });
  });
</script>
<input type="text" id="titleBox" class="input input-bordered w-full" />
```

---

### Example 3: Slug Generator with Suggestions

**Old (slug-generator.vtl):**
```html
<script>
  const SOURCE_FIELD = "title";
  const TARGET_FIELD = "urlTitle";
  const SLUG_INPUT = "slugInput";
  const SUGGESTION_DIV = "slugSuggestion";

  let isLocked = false;
  let currentValue = "";

  const slugifyText = (text) =>
    text
      .toLowerCase()
      .replace(/[àáäâ]/g, "a")
      .replace(/[èéëê]/g, "e")
      .replace(/[ìíïî]/g, "i")
      .replace(/[òóöô]/g, "o")
      .replace(/[ùúüû]/g, "u")
      .replace(/[ñ]/g, "n")
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-+|-+$/g, "");

  const showSuggestion = (newSlug) => {
    const suggestion = document.getElementById(SUGGESTION_DIV);

    if (!newSlug || newSlug === currentValue) {
      suggestion.style.display = "none";
      return;
    }

    // ⚠️ XSS risk: string interpolation inside onclick and innerHTML can allow
    // script injection if newSlug contains quotes or HTML special characters.
    // This is one of the reasons this pattern is being replaced.
    suggestion.innerHTML = `
      <a href="#" onclick="applySuggestion('${newSlug}'); return false">
        Use: ${newSlug}
      </a>
    `;
    suggestion.style.display = "block";
  };

  const applySuggestion = (slug) => {
    const input = document.getElementById(SLUG_INPUT);
    input.value = slug;
    currentValue = slug;
    isLocked = true;
    DotCustomFieldApi.set(TARGET_FIELD, slug);
    document.getElementById(SUGGESTION_DIV).style.display = "none";
  };

  const handleInput = () => {
    const input = document.getElementById(SLUG_INPUT);
    const newSlug = slugifyText(input.value);
    input.value = newSlug;
    currentValue = newSlug;
    isLocked = true;
    DotCustomFieldApi.set(TARGET_FIELD, newSlug);
  };

  DotCustomFieldApi.ready(() => {
    const input = document.getElementById(SLUG_INPUT);
    const savedValue = DotCustomFieldApi.get(TARGET_FIELD);

    if (savedValue) {
      input.value = savedValue;
      currentValue = savedValue;
    }

    DotCustomFieldApi.onChangeField(SOURCE_FIELD, (value) => {
      const newSlug = slugifyText(value);
      showSuggestion(newSlug);
    });
  });
</script>

<input
  type="text"
  id="slugInput"
  onkeyup="handleInput()"
  class="dijitTextBox"
  style="background:#FAFAFA"
/>
<div
  id="slugSuggestion"
  style="margin-top:8px; display:none; color:#2196F3;"
></div>
```

**New (slug-generator.vtl):**
```html
<script>
  const TARGET_FIELD = "urlTitle";
  const SLUG_INPUT = "slugInput";
  const SUGGESTION_DIV = "slugSuggestion";

  let isLocked = false;
  let currentValue = "";

  const slugifyText = (text) =>
    text
      .toLowerCase()
      .replace(/[àáäâ]/g, "a")
      .replace(/[èéëê]/g, "e")
      .replace(/[ìíïî]/g, "i")
      .replace(/[òóöô]/g, "o")
      .replace(/[ùúüû]/g, "u")
      .replace(/[ñ]/g, "n")
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-+|-+$/g, "");

  DotCustomFieldApi.ready(() => {
    const input = document.getElementById(SLUG_INPUT);
    const urlTitleField = DotCustomFieldApi.getField("urlTitle");
    const savedValue = urlTitleField.getValue();

    if (savedValue) {
      input.value = savedValue;
      currentValue = savedValue;
    }

    // Field reference captured once inside ready() and reused by both helpers
    const applySuggestion = (slug) => {
      input.value = slug;
      currentValue = slug;
      isLocked = true;
      urlTitleField.setValue(slug);
      document.getElementById(SUGGESTION_DIV).classList.add("hidden");
    };

    const showSuggestion = (newSlug) => {
      const suggestion = document.getElementById(SUGGESTION_DIV);

      if (!newSlug || newSlug === currentValue) {
        suggestion.classList.add("hidden");
        suggestion.classList.remove("block");
        return;
      }

      suggestion.innerHTML = "";

      const link = document.createElement("a");
      link.href = "#";
      link.className = "link link-primary";
      link.textContent = `Use: ${newSlug}`;
      link.addEventListener("click", (e) => {
        e.preventDefault();
        applySuggestion(newSlug);
      });

      suggestion.appendChild(link);
      suggestion.classList.remove("hidden");
      suggestion.classList.add("block", "mt-2");
    };

    const handleInput = () => {
      const newSlug = slugifyText(input.value);
      input.value = newSlug;
      currentValue = newSlug;
      isLocked = true;
      urlTitleField.setValue(newSlug);
    };

    const titleField = DotCustomFieldApi.getField("title");
    titleField.onChange((value) => {
      const newSlug = slugifyText(value);
      showSuggestion(newSlug);
    });
    input.addEventListener("keyup", handleInput);
  });
</script>

<input type="text" id="slugInput" class="input input-bordered w-full" />
<div id="slugSuggestion" class="hidden mt-2 text-primary" role="region" aria-live="polite"></div>
```

---

### Example 4: Conditional Field Visibility (show/hide)

This example shows a "Media" form where the "Media File" field is shown or hidden based on the "Media Location" radio selection. The custom field stores the selection in one field and toggles visibility of another.

**New (show_hide.vtl):**
```html
<script>
  DotCustomFieldApi.ready(() => {
    const mediaField = DotCustomFieldApi.getField("media");
    const mediaFileField = DotCustomFieldApi.getField("mediafile");

    const currentValue = mediaField.getValue() || "external";
    const radios = document.querySelectorAll('input[name="mediaLocation"]');

    const initialRadio = Array.from(radios).find((radio) => radio.value === currentValue);
    if (initialRadio) {
      initialRadio.checked = true;
    }

    if (currentValue === "upload") {
      mediaFileField.show();
    } else {
      mediaFileField.hide();
    }

    radios.forEach((radio) => {
      radio.addEventListener("change", (e) => {
        const value = e.target.value;
        mediaField.setValue(value);

        if (value === "upload") {
          mediaFileField.show();
        } else {
          mediaFileField.hide();
        }
      });
    });
  });
</script>

<fieldset class="fieldset">
  <legend class="fieldset-legend">Media Location</legend>
  <div class="flex gap-4">
    <label class="flex items-center gap-2 cursor-pointer">
      <input type="radio" name="mediaLocation" value="upload" class="radio radio-primary" />
      <span>Upload File</span>
    </label>
    <label class="flex items-center gap-2 cursor-pointer">
      <input type="radio" name="mediaLocation" value="external" class="radio radio-primary" checked />
      <span>Link to External File</span>
    </label>
  </div>
</fieldset>
```

Key points:
- `mediaFileField.show()` and `mediaFileField.hide()` control the visibility of the entire "mediafile" field (input + label) — no manual DOM manipulation needed.
- The radio buttons use DaisyUI classes (`radio radio-primary`) and are wrapped in a `fieldset` with `fieldset-legend`.
- The initial visibility is set based on `mediaField.getValue()` before attaching the change listener.

---

### Example 5: Conditional Field Enable/Disable

This example disables an "Expiration Date" field until the user checks an "Enable Expiration" checkbox.

**New (expiration_toggle.vtl):**
```html
<script>
  DotCustomFieldApi.ready(() => {
    const enableExpirationField = DotCustomFieldApi.getField("enableExpiration");
    const expirationDateField = DotCustomFieldApi.getField("expirationDate");

    const toggle = (value) => {
      if (value === "true" || value === true) {
        expirationDateField.enable();
      } else {
        expirationDateField.disable();
      }
    };

    toggle(enableExpirationField.getValue());
    enableExpirationField.onChange(toggle);
  });
</script>
```

Key points:
- `expirationDateField.disable()` prevents editing and applies disabled styling; `enable()` restores interactivity.
- No manual DOM attribute manipulation (`setAttribute("disabled", ...)`) is needed.
- The pattern is the same as show/hide: set initial state, then react to changes via `onChange`.
