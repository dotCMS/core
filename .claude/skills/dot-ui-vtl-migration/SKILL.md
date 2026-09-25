---
name: dot-ui-vtl-migration
owner: "@dotcms/falcon"
status: active
description: >
  Migrates VTL (Velocity Template Language) custom field templates from the legacy DotCMS Dojo/Dijit API to the modern DotCustomFieldApi. By default it returns a single inline file ready to paste into the Custom Field: the migrated code inside #if( $structures.isNewEditModeEnabled() ) and the original legacy code, untouched, inside #else, with no #parse and no extra files. On request it produces three files instead (_old.vtl, _new.vtl and a #parse router), and it converts existing migrations between the two shapes. Use this skill whenever a user asks to migrate, update, or convert a VTL file, custom field template, or dotCMS field that uses any of: DotCustomFieldApi.get(), DotCustomFieldApi.set(), DotCustomFieldApi.onChangeField(), dojo.ready(), dojo.byId(), dijit.byId(), dijit.form.*, dojoType attributes, or any Dojo/Dijit pattern. Also trigger when the user pastes a VTL snippet and asks "what needs to change" or "can you update this", asks for an inline, single-file or one-file migration ("un solo archivo"), wants to paste the result into the content type editor, or wants to collapse a _new/_old/router migration into one file or split an inline one. If in doubt, use this skill.
---

# VTL Migration: Legacy API → DotCustomFieldApi

You are migrating DotCMS VTL custom field templates from Dojo/Dijit-era APIs to the modern `DotCustomFieldApi`. The goal is **identical functionality with modern, clean code** and **semantic styling with DaisyUI**.

For the full migration rules, all code examples, the DaisyUI styling section, and the step-by-step checklist, read `references/migration-guide.md`.

## The Core API Swap (Quick Reference)

| Old (deprecated) | New |
|---|---|
| `DotCustomFieldApi.get('id')` | `DotCustomFieldApi.getField('id').getValue()` |
| `DotCustomFieldApi.set('id', val)` | `DotCustomFieldApi.getField('id').setValue(val)` |
| `DotCustomFieldApi.onChangeField('id', cb)` | `DotCustomFieldApi.getField('id').onChange(cb)` |
| Manual DOM show/hide of a field | `DotCustomFieldApi.getField('id').show()` / `.hide()` |
| Manual DOM enable/disable of a field | `DotCustomFieldApi.getField('id').enable()` / `.disable()` |
| Manual checks against dijit validation state | `DotCustomFieldApi.getField('id').getValidationState()` |
| No legacy equivalent | `DotCustomFieldApi.getField('id').onValidationChange(cb)` |
| `dojo.ready(fn)` | `DotCustomFieldApi.ready(fn)` |
| `dojo.byId('el')` | `document.getElementById('el')` |
| `dijit.byId('id')` | `DotCustomFieldApi.getField('id')` |
| `dijit.form.*` widgets | Native HTML + **DaisyUI** classes (`input`, `btn`, `select`, etc.) |
| `dojoType="..."` attribute | Remove; use semantic HTML + DaisyUI components |
| `class="dijit*"` classes | Remove; use **DaisyUI** component classes instead |
| Inline styles / ad-hoc CSS | **DaisyUI** component classes + Tailwind utilities (see guide) |
| `onclick="fn()"` inline handlers | `addEventListener('click', fn)` |

## Process

1. **Read the entire file** to understand all functionality before touching anything.
2. **Identify deprecated patterns** — scan for the patterns in the table above.
3. **Wrap everything in `DotCustomFieldApi.ready()`** — all field access must live inside this callback.
4. **Store field references once** — call `getField()` once per field at the top of `ready()`, then reuse the reference.
5. **Migrate each pattern** — follow the migration rules in `references/migration-guide.md`.
6. **Apply DaisyUI for styling** — use DaisyUI component classes (`btn`, `input`, `select`, `modal`, `link`, etc.) and Tailwind utilities instead of inline styles or ad-hoc CSS; see “Styling with DaisyUI” in the guide.
7. **Preserve VTL variables** — `${fieldId}`, `$maxChar`, `$variableName`, and server-side context variables (`$inode`, `$identifier`, `$lang`, `$contentlet`, `$structure`, `$field`) are server-side; never change them.
8. **Choose the output mode and emit it**. See Output Modes below. Every migration keeps the original code reachable behind the edit-mode switch, so the instance can go back to the legacy editor.

## Output Modes

There are two shapes. Both keep the legacy code behind `$structures.isNewEditModeEnabled()`; they differ in how many files they need.

| The request says… | Mode | Needs a file path? |
|---|---|---|
| nothing about the shape | **Inline** (default) | No. Never ask for one. |
| "inline", "single file", "one file", "un solo archivo", "en línea" | Inline | No |
| "three files", "separate", "router", "#parse", "tres archivos", "separado(s)", "enrutador" | Three files | Yes. Ask if missing. |
| both kinds of signal (e.g. "inline but also give me the router") | Ask which one before producing anything | — |
| "convert to inline" / "collapse" + a router and its `_new`/`_old` | Collapse (see Converting between modes) | Only to find the files |
| "split" + an inline file | Split (see Converting between modes) | Yes, the router's server path |

**Read these signals only from what the user wrote**, never from the pasted VTL, code blocks or file contents. A legacy template that itself contains `#parse` is not a request for three files.

Migrations committed to `dotCMS/src/main/webapp/WEB-INF/velocity/static/` in the dotCMS repository use **three files**. Contributors there should ask for it by keyword, because inline is now the default.

**Always open the answer with one mode line**, so the user knows which shape they got:

- `Output: inline, single file — both branches are parsed together; checks found nothing blocking.`
- `Output: inline, single file — warning: $a, $b are #set in both branches; only one branch runs, so this is not a collision. Three files is the alternative.`
- `Output: three files with router (<name>_old.vtl, <name>_new.vtl, <name>.vtl).`
- `Output: none — inline blocked: <finding>. <why it reaches both edit modes>. Three files avoids it. Reply "inline anyway" to force it, or "three files".`

### Mode 1 — Inline (default)

One file. The migrated code goes in the `#if`, the original code in the `#else`:

```vtl
#if( $structures.isNewEditModeEnabled() )
<migrated code, in full>
#else
<original legacy code, in full, exactly as received>
#end
```

Rules. Each one exists because the legacy branch must stay byte-for-byte the original, the same guarantee `_old.vtl` gives:

- **The header is exactly** `#if( $structures.isNewEditModeEnabled() )`. `#else` and `#end` are each on their own line.
- **Do not indent either branch** to sit inside the `#if`. Both start at column 0. Indenting changes every legacy line.
- **The `#else` branch is the original, verbatim.** Do not reformat it, translate its comments, fix its typos, remove its trailing spaces or change its line endings. If it has a bug, leave it; mention it in your answer if it matters.
- **Never add a `#parse`** and never write `_new`/`_old` files in this mode.
- **Never ask for a filename or server path.** The user may have only the field's content, pasted from the content type editor; inline needs no path. If a path *was* given, the single file keeps the original file's name and replaces it.
- **Run the checks in "Before emitting inline" first.** They can stop the output.

**When the original is a file on disk** (you were given a path), do not retype it. Write the migrated version to a temporary file, then assemble with the shell so the legacy bytes are copied, not regenerated. This is the only reliable way to keep CRLF line endings and a missing final newline:

```sh
{ printf '#if( $structures.isNewEditModeEnabled() )\n'; cat migrated.vtl; printf '\n#else\n'; cat original.vtl; printf '\n#end'; } > result.vtl
```

When the content was pasted, copy the pasted legacy code into the `#else` exactly as the user sent it.

### Before emitting inline

Velocity parses the **whole file** before it renders either branch. With the router, the branch not taken is never even parsed; inline, both are. So some problems that only affected the legacy editor in the three-file shape would break the new editor too. Before emitting inline, read both branches and check:

| Finding | Severity | What to do |
|---|---|---|
| A `#macro` with the same name in both branches | **Blocking** | Macros are registered at parse time, and the first definition wins. That is the migrated one, so the legacy editor would silently run the new macro. Emit nothing; name the macro; offer three files. |
| The legacy branch would not parse on its own: a `#if`/`#foreach`/`#macro`/`#define` without its `#end`, a stray `#end`, an `#else` outside an `#if` | **Blocking** | Inline would carry that parse error into the new edit mode. Emit nothing; give the line; offer three files. |
| The migrated branch would not parse on its own | **Blocking** | If you migrated it, that is a bug in your migration: fix it before emitting anything. If it came from the user (collapse or split), report the line like the legacy case and write nothing. |
| The same variable `#set` in both branches | Warning | `#set` runs at render time and only one branch renders, so this is not a collision. Emit, but list the names in the mode line. |

Count directives the way Velocity does. Text inside `##`, `#* *#` and `#[[ ]]#` is not code, and `\#end` is escaped. A directive name also ends at the first character that is not a letter, digit, `_` or `@`, so `$('#end-date')` in a script **is** an `#end`, while `#endDate` is not. The full rules and examples are in `references/migration-guide.md` → "Output Modes".

After a blocking finding, emit inline only if the user explicitly insists ("inline anyway"), and restate the finding in the mode line when you do.

### Mode 2 — Three files with a router

Use this mode when the request asks for it (see the table above). Given an original file at `/static/personas/keytag_custom_field.vtl`, the three outputs are:

---

#### File 1 — `keytag_custom_field_old.vtl`
The **original file content, completely unchanged**. Copy it verbatim — every deprecated API call, every dijit class, every dojo.ready. This is the fallback for the legacy editor.

---

#### File 2 — `keytag_custom_field_new.vtl`
The **fully migrated file** with all deprecated patterns replaced per the migration rules.

---

#### File 3 — `keytag_custom_field.vtl` (replaces the original)
The **conditional router** — this file takes the name of the original and delegates to `_new` or `_old` based on which edit mode is active:

```vtl
#if( $structures.isNewEditModeEnabled() )
	#parse('/static/personas/keytag_custom_field_new.vtl')
#else
	#parse('/static/personas/keytag_custom_field_old.vtl')
#end
```

The `#parse` paths must use the **full server path** of the file, not just the filename. Use the same directory as the original file. Indent the `#parse` lines with a tab and end the file right after `#end`, with no final newline, as the routers already in the repository do. On disk, write it with `printf` so no newline is added.

---

This pattern lets both legacy and new edit modes coexist safely — old editor users continue using the deprecated code, new editor users get the modernized version.

**When the user gives you a file path**, derive all three filenames automatically. In three-file mode only, if you receive file contents without a path, ask for the filename and its server path before outputting, because `#parse` cannot work without it.

### Converting between modes

Conversion only moves content. **Never apply a migration rule to either branch while converting.** It is a re-shaping, not a re-migration.

**Collapse (router + `_new` + `_old` → one inline file).** Read the router's two `#parse` paths and find the `_new` and `_old` files. The `_new` content goes in the `#if` and the `_old` content in the `#else`, using the Mode 1 layout. Run "Before emitting inline" first; the same findings apply. If the router's paths do not match the files you were given (different names or directories), say so and stop. Do not guess.

**Split (one inline file → router + `_new` + `_old`).** You need the router's full server path; ask for it if you do not have it.

1. Find where the branches end. Line 1 is the header. The legacy branch starts after the **top-level** `#else`: the first `#else` line reached while no block opened after line 1 is still open. Count block openers (`#if`, `#foreach`, `#macro`, `#define`, `#literal`, `#@name`) and `#end`s from line 2 on. **Never just take the first `#else`**, because branches often contain their own `#if … #else … #end`. The file ends with the matching `#end`.
2. `_new.vtl` is everything between the header and the top-level `#else`. `_old.vtl` is everything between that `#else` and the final `#end`. In both cases, drop the one newline the inline layout added before the `#else` and before the `#end`.
3. Write the router in Mode 2's shape: tab-indented `#parse` lines with full server paths.

If the file does not start with `#if( $structures.isNewEditModeEnabled() )`, or has no top-level `#else`, it is not an inline migration. Say so; do not guess.

**On disk, use the shell for both directions** so bytes are copied, never retyped. Collapse is the Mode 1 command, with `_new` in place of `migrated.vtl` and `_old` in place of `original.vtl`. Split takes line ranges, where `E` is the line number of the top-level `#else` and `L` is the line number of the final `#end`:

```sh
sed -n "2,$((E-1))p" inline.vtl | perl -pe 'chomp if eof' > name_new.vtl
sed -n "$((E+1)),$((L-1))p" inline.vtl | perl -pe 'chomp if eof' > name_old.vtl
```

## Non-Negotiables

- All `getField()` calls must be inside `DotCustomFieldApi.ready()`
- Never use `DotCustomFieldApi.get()` or `DotCustomFieldApi.set()` (the old short forms)
- VTL variables stay exactly as-is
- Business logic stays exactly as-is — only the API calls and styling approach change
- Dijit CSS classes (any `class="dijit*"`) must be removed
- **Styling:** Prefer DaisyUI component classes + Tailwind utilities; keep custom CSS only when the guide says so
- Translate non-English comments to English **in the migrated code only**
- The legacy code (`#else` branch or `_old.vtl`) is never edited, not even its comments or whitespace
- Server-side VTL variables (`$inode`, `$identifier`, `$lang`, `$contentlet`, `$structure`, `$field`) are resolved at render time — do not confuse them with `DotCustomFieldApi` JavaScript APIs

## Key Patterns to Know

**Field reference lifecycle:**
```js
DotCustomFieldApi.ready(() => {
  // Get once, reuse everywhere
  const titleField = DotCustomFieldApi.getField('title');
  const urlField = DotCustomFieldApi.getField('url');

  // Read
  const current = titleField.getValue() || '';

  // Write
  urlField.setValue(slugify(current));

  // Watch
  titleField.onChange((value) => {
    urlField.setValue(slugify(value));
  });
});
```

**Field visibility and state control:**
```js
DotCustomFieldApi.ready(() => {
  const mediaField = DotCustomFieldApi.getField('media');
  const mediaFileField = DotCustomFieldApi.getField('mediafile');

  // Show/hide based on current value
  if (mediaField.getValue() === 'upload') {
    mediaFileField.show();
  } else {
    mediaFileField.hide();
  }

  // React to changes
  mediaField.onChange((value) => {
    if (value === 'upload') {
      mediaFileField.show();
    } else {
      mediaFileField.hide();
    }
  });

  // Enable/disable a field
  mediaFileField.disable(); // blocks editing, applies disabled styling
  mediaFileField.enable();  // restores interactivity
});
```

**Reacting to validation state (required, errors, touched):**
```html
<style>
  /* Self-contained: legacy iframe pages do NOT load DaisyUI, so we ship the rule with the template. */
  #slugInput.is-invalid {
    border-color: #ef4444;
    outline-color: #ef4444;
  }
</style>

<script type="module">
  DotCustomFieldApi.ready(() => {
    const field = DotCustomFieldApi.getField('urlTitle');
    const input = document.getElementById('slugInput');

    const applyValidation = (state) => {
      // Only show the error after the user (or Save) has marked the control as touched —
      // mirrors how Angular's built-in fields paint the red border.
      const showError = state.invalid && state.touched;
      input.classList.toggle('is-invalid', showError);
    };

    // onValidationChange emits the initial state synchronously, so a separate
    // getValidationState() call up front is redundant. The bridge auto-cleans
    // on form destroy, so the unsubscribe return value can be ignored here.
    field.onValidationChange(applyValidation);
  });
</script>
```

> Use a self-contained `is-invalid` class with inline `<style>` instead of DaisyUI's `input-error`. The legacy iframe page (`legacy-custom-field.jsp`) does NOT load DaisyUI or Tailwind, so an `input-error` toggle would silently produce no visual feedback there. In iframe mode the callback also never fires (the Dojo bridge's `onValidationChange` is a no-op) — the legacy editor has its own validation surface. See Rule 13 in `references/migration-guide.md` for the full gotchas list.

`state` shape: `{ valid, invalid, touched, dirty, errors }` (mirrors Angular's `AbstractControl`). `errors` is `null` when valid, otherwise a record like `{ required: true }`.

**Multiple onChange for the same field** → combine into one handler:
```js
// Old: two separate onChangeField calls for 'title'
// New: one onChange that does both
titleField.onChange((value) => {
  updateURL(value);
  updateFriendlyName(value);
});
```

**Native dialog with DaisyUI modal** (replaces `dojoType="dijit.Dialog"`):
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

**Styling (DaisyUI):** Buttons → `btn`, `btn-primary`, `btn-ghost`, `btn-sm`. Inputs → `input input-bordered`. Selects → `select select-bordered`. Links → `link link-primary`. Use Tailwind for layout (`flex`, `gap`, `w-full`). Full reference in `references/migration-guide.md` → “Styling with DaisyUI”.

## Available Velocity Context Variables

Custom field templates can use **server-side VTL variables** injected by dotCMS when the field is rendered. These are resolved on the server before HTML reaches the browser — they are **not** available in JavaScript and must not be confused with `DotCustomFieldApi`.

| Variable | Type | Description |
|---|---|---|
| `$inode` | `String` | The contentlet's inode (version ID) |
| `$identifier` | `String` | The contentlet's persistent identifier |
| `$lang` | `long` | The contentlet's language ID |
| `$contentlet` | `Contentlet` | The full Contentlet object |
| `$structure` | `ContentType` | The content type (structure) |
| `$field` | `Field` | The current field being rendered |

**Availability:**
- `$structure` and `$field` are always available when the custom field is rendered.
- `$inode`, `$identifier`, `$lang`, and `$contentlet` are populated only when **editing an existing contentlet** (when an inode is known). On new content, those four variables are empty/unset.
- Both the new editor (REST API component mode and iframe mode) and the legacy editor expose the same variables.

**Example — display context variables in the template:**

```html
<p>
  <strong>inode:</strong> $inode
</p>
<p>
  <strong>identifier:</strong> $identifier
</p>
<p>
  <strong>lang:</strong> $lang
</p>
<p>
  <strong>contentlet:</strong> $contentlet
</p>
<p>
  <strong>structure:</strong> $structure
</p>
<p>
  <strong>field:</strong> $field
</p>
```

**Example — guard for new vs existing content:**

```html
#if($utilMethods.isSet($inode))
  <input type="hidden" id="contentInode" value="$inode" />
#else
  <p class="text-sm text-base-content/70">Save the content first to access inode-specific features.</p>
#end
```

For full details, availability rules, and practical examples → read `references/migration-guide.md` → “Server-Side Velocity Context Variables”.

## Before Outputting

Verify the migration passes this checklist (details in `references/migration-guide.md`):

**Answer:**
- [ ] Opens with the mode line (inline, three files, or none, with the reason)
- [ ] No filename or server path was asked for in inline mode

**Inline output (default):**
- [ ] One file; header exactly `#if( $structures.isNewEditModeEnabled() )`; `#else` and `#end` on their own lines
- [ ] `#else` branch is the original verbatim: not re-indented, not reformatted, comments untouched
- [ ] No `#parse` added; no `_new`/`_old` files written
- [ ] "Before emitting inline" checks run; no blocking finding, unless the user insisted, in which case the finding is restated
- [ ] Shared `#set` variables, if any, listed in the mode line

**Three-file output (on request):**
- [ ] `_old.vtl` — original file content, completely unchanged (deprecated code preserved intentionally)
- [ ] `_new.vtl` — fully migrated content
- [ ] Router file (original filename) — contains only the `#if( $structures.isNewEditModeEnabled() )` block with correct full-path `#parse` directives pointing to `_new` and `_old`

**Conversion (collapse / split):**
- [ ] Neither branch changed; no migration rule applied
- [ ] Split used the top-level `#else`, not the first one

**Migrated code (the `#if` branch or `_new.vtl`):**
- [ ] No `DotCustomFieldApi.get()` or `.set()` or `.onChangeField()` remaining
- [ ] No `dojo.*` or `dijit.*` references remaining
- [ ] No `dojoType` attributes remaining
- [ ] No `dijit*` CSS classes remaining
- [ ] Styling uses DaisyUI components where applicable (buttons, inputs, selects, modals, links) and Tailwind for layout; no inline styles unless necessary
- [ ] All field access inside `DotCustomFieldApi.ready()`
- [ ] All `getField()` calls stored in variables and reused
- [ ] Field visibility uses `field.show()` / `field.hide()` instead of manual DOM manipulation
- [ ] Field state uses `field.enable()` / `field.disable()` instead of manual DOM attribute changes
- [ ] VTL variables unchanged
- [ ] Business logic unchanged

For complete rules, **DaisyUI styling section**, all migration examples (character counter, title field, slug generator, dialogs, file browser), and edge cases → read `references/migration-guide.md`.
