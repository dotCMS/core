# VTL Migration Guide: Complete Reference

## Table of Contents
1. [The New DotCustomFieldApi](#the-new-dotcustomfieldapi)
2. [Migration Rules](#migration-rules)
3. [Best Practices](#best-practices)
4. [Step-by-Step Checklist](#step-by-step-checklist)
5. [Common Pitfalls](#common-pitfalls)
6. [Special Cases](#special-cases)
7. [Complete Migration Examples](#complete-migration-examples)

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
});
```

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

### Rule 7: Remove Dijit CSS classes

Remove any class starting with `dijit` from HTML elements. Keep all other custom classes and inline styles.

**Classes to remove:** `dijitTextBox`, `dijitPlaceHolder`, `dijitSelect`, `dijitButton`, `dijitDropDownButton`, `dijitDialog`, and any other `dijit*` class.

**Old (deprecated):**
```html
<input type="text" id="slugInput" class="dijitTextBox" style="background:#FAFAFA" />
```

**New:**
```html
<input type="text" id="slugInput" style="background:#FAFAFA" />
```

Custom CSS classes in `<style>` tags and inline styles are preserved as-is.

### Rule 8: Remove `dojoType` attributes — use semantic HTML

| Old (deprecated) | New |
|---|---|
| `<input dojoType="dijit.form.TextBox" />` | `<input type="text" />` |
| `<input dojoType="dijit.form.Button" />` | `<button type="button"></button>` |
| `<select dojoType="dijit.form.FilteringSelect" />` | `<select></select>` |
| `<div dojoType="dijit.Dialog" />` | `<dialog></dialog>` |
| `<input dojoType="dijit.form.RadioButton" />` | `<input type="radio" />` |
| `<div dojoType="dojox.widget.ColorPicker" />` | `<input type="color" />` |

**Old (deprecated):**
```html
<input type="text" id="slugInput" dojoType="dijit.form.TextBox" />
<div id="videoResultsDiv" dojoType="dijit.Dialog" style="display: none"></div>
```

**New:**
```html
<input type="text" id="slugInput" />
<dialog id="videoResultsDiv"></dialog>
```

### Rule 9: Native Dialog Implementation

For `dojoType="dijit.Dialog"` elements, use the native HTML `<dialog>` element.

```html
<style>
  #myDialog::backdrop {
    background-color: rgba(0, 0, 0, 0.5);
    opacity: 0.75;
  }
</style>

<dialog id="myDialog">
  <div>
    <h2>Dialog Title</h2>
    <p>Dialog content goes here</p>
    <button id="closeDialog">Close</button>
  </div>
</dialog>

<button id="showDialog">Show Dialog</button>

<script>
  DotCustomFieldApi.ready(() => {
    const dialog = document.getElementById("myDialog");
    const showButton = document.getElementById("showDialog");
    const closeButton = document.getElementById("closeDialog");

    showButton.addEventListener("click", () => {
      dialog.showModal();
    });

    closeButton.addEventListener("click", () => {
      dialog.close();
    });

    // Close when clicking outside
    dialog.addEventListener("click", (e) => {
      if (e.target === dialog) {
        dialog.close();
      }
    });
  });
</script>
```

### Rule 10: Replace inline event handlers with `addEventListener()`

**Old (deprecated):**
```html
<button onclick="handleClick()">Click</button>
<input onkeyup="handleInput()" />
```

**New:**
```html
<button id="myButton">Click</button>
<input id="myInput" />

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
    const pageSelectorModal = bridge.openBrowserModal({
      header: "Select a Page",
      mimeTypes: ["application/dotpage"],
      onClose: (result) => console.log(result),
    });
    pageSelectorModal.close(); // close programmatically if needed

    // Select an Image
    const imageSelectorModal = bridge.openBrowserModal({
      header: "Select an Image",
      mimeTypes: ["image"],
      onClose: (result) => console.log(result),
    });

    // Select a File
    const fileSelectorModal = bridge.openBrowserModal({
      header: "Select a File",
      includeDotAssets: true,
      onClose: (result) => console.log(result),
    });
  });
</script>
```

### Native Components and Platform Styles

Custom fields are inserted inside the Angular app, so they can use platform styles and PrimeIcons.

**Use PrimeIcons for icons** — no custom CSS required:
```html
<!-- Clear / close button -->
<button type="button" id="clearRedirectButton" aria-label="Clear redirect URL">
  <i class="pi pi-times"></i>
</button>

<!-- Dropdown trigger -->
<button type="button" id="customSelectButton" class="custom-select-button">
  <span id="templateSelectButtonLabel">All Sites</span>
  <i class="pi pi-chevron-down"></i>
</button>
```

When updating the label in a button with an icon, update only the `<span>` — never overwrite `innerHTML` of the whole button or the icon will disappear.

**Use platform CSS classes** for layout (flex, gap, padding) instead of writing new custom CSS when possible.

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

### What to Preserve

**DO NOT change:**
- VTL variables: `${fieldId}`, `$maxChar`, `$variableName`
- Business logic and algorithms
- Custom CSS classes (non-dijit)
- Inline styles
- HTML structure (except removing dojoType/dijit attributes)
- Comments (but translate to English if written in another language)
- Function names and variable names (unless they reference deprecated APIs)

**DO change:**
- API method calls: get/set/onChangeField → getField/getValue/setValue/onChange
- `dojo.ready` → `DotCustomFieldApi.ready`
- `dojo.byId` → `document.getElementById`
- `dijit.byId` → `DotCustomFieldApi.getField`
- Remove `dojoType` attributes
- Remove `dijit*` CSS classes
- Replace inline event handlers with `addEventListener`

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

### 4. Remove Dojo/Dijit
- [ ] Remove all `dojo.require()` statements
- [ ] Replace `dojo.ready()` with `DotCustomFieldApi.ready()`
- [ ] Replace `dojo.byId()` with `document.getElementById()`
- [ ] Replace `dijit.byId()` with `DotCustomFieldApi.getField()`

### 5. Update HTML elements
- [ ] Remove all `dojoType` attributes
- [ ] Replace `<div dojoType="dijit.Dialog">` with `<dialog>`
- [ ] Remove all `class="dijit*"` classes
- [ ] Preserve custom CSS classes and inline styles

### 6. Update event handlers
- [ ] Move inline event handlers to `addEventListener()` calls inside `DotCustomFieldApi.ready()`

### 7. Verify preservation
- [ ] All VTL variables (`${fieldId}`, `$variable`) remain unchanged
- [ ] Business logic unchanged
- [ ] Functionality remains identical
- [ ] Custom CSS styles (non-dijit) are preserved

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

**New (text-count.vtl):**
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

<script type="module">
  function updateCharacterCount(textEntered) {
    const counter = textEntered.length;
    const countRemaining = document.getElementById("charactersRemaining-${fieldId}");
    const counterText = document.getElementById("${fieldId}-counter-text");

    countRemaining.textContent = counter;
    counterText.style.color = counter <= $maxChar ? "#6c7389" : "red";
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
<input id="titleBox" />
```

**New (title_custom_field.vtl):**
```html
<script>
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
<input type="text" id="titleBox" />
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

  const applySuggestion = (slug) => {
    const input = document.getElementById(SLUG_INPUT);
    input.value = slug;
    currentValue = slug;
    isLocked = true;
    const field = DotCustomFieldApi.getField(TARGET_FIELD);
    field.setValue(slug);
    document.getElementById(SUGGESTION_DIV).style.display = "none";
  };

  const showSuggestion = (newSlug) => {
    const suggestion = document.getElementById(SUGGESTION_DIV);

    if (!newSlug || newSlug === currentValue) {
      suggestion.style.display = "none";
      return;
    }

    suggestion.innerHTML = "";

    const link = document.createElement("a");
    link.textContent = `Use: ${newSlug}`;
    link.addEventListener("click", (e) => {
      e.preventDefault();
      applySuggestion(newSlug);
    });

    suggestion.appendChild(link);
    suggestion.style.display = "block";
    suggestion.style.cursor = "pointer";
  };

  const handleInput = () => {
    const input = document.getElementById(SLUG_INPUT);
    const newSlug = slugifyText(input.value);
    input.value = newSlug;
    currentValue = newSlug;
    isLocked = true;
    const field = DotCustomFieldApi.getField(TARGET_FIELD);
    field.setValue(newSlug);
  };

  DotCustomFieldApi.ready(() => {
    const input = document.getElementById(SLUG_INPUT);
    const urlTitleField = DotCustomFieldApi.getField("urlTitle");
    const savedValue = urlTitleField.getValue();

    if (savedValue) {
      input.value = savedValue;
      currentValue = savedValue;
    }

    const titleField = DotCustomFieldApi.getField("title");
    titleField.onChange((value) => {
      const newSlug = slugifyText(value);
      showSuggestion(newSlug);
    });
    input.addEventListener("keyup", handleInput);
  });
</script>

<input type="text" id="slugInput" style="background:#FAFAFA" />
<div id="slugSuggestion" style="margin-top:8px; display:none; color:#2196F3;"></div>
```
