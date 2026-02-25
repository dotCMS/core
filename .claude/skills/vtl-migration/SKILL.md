---
name: vtl-migration
description: >
  Migrates VTL (Velocity Template Language) custom field templates from the legacy DotCMS Dojo/Dijit API to the modern DotCustomFieldApi. Use this skill whenever a user asks to migrate, update, or convert a VTL file, custom field template, or dotCMS field that uses any of: DotCustomFieldApi.get(), DotCustomFieldApi.set(), DotCustomFieldApi.onChangeField(), dojo.ready(), dojo.byId(), dijit.byId(), dijit.form.*, dojoType attributes, or any Dojo/Dijit pattern. Also trigger when the user pastes a VTL snippet and asks "what needs to change" or "can you update this". If in doubt, use this skill.
---

# VTL Migration: Legacy API → DotCustomFieldApi

You are migrating DotCMS VTL custom field templates from Dojo/Dijit-era APIs to the modern `DotCustomFieldApi`. The goal is **identical functionality with modern, clean code**.

For the full migration rules, all code examples, and the step-by-step checklist, read `references/migration-guide.md`.

## The Core API Swap (Quick Reference)

| Old (deprecated) | New |
|---|---|
| `DotCustomFieldApi.get('id')` | `DotCustomFieldApi.getField('id').getValue()` |
| `DotCustomFieldApi.set('id', val)` | `DotCustomFieldApi.getField('id').setValue(val)` |
| `DotCustomFieldApi.onChangeField('id', cb)` | `DotCustomFieldApi.getField('id').onChange(cb)` |
| `dojo.ready(fn)` | `DotCustomFieldApi.ready(fn)` |
| `dojo.byId('el')` | `document.getElementById('el')` |
| `dijit.byId('id')` | `DotCustomFieldApi.getField('id')` |
| `dijit.form.*` widgets | Native HTML elements |
| `dojoType="..."` attribute | Remove; use semantic HTML |
| `class="dijit*"` classes | Remove dijit classes entirely |
| `onclick="fn()"` inline handlers | `addEventListener('click', fn)` |

## Process

1. **Read the entire file** to understand all functionality before touching anything.
2. **Identify deprecated patterns** — scan for the patterns in the table above.
3. **Wrap everything in `DotCustomFieldApi.ready()`** — all field access must live inside this callback.
4. **Store field references once** — call `getField()` once per field at the top of `ready()`, then reuse the reference.
5. **Migrate each pattern** — follow the migration rules in `references/migration-guide.md`.
6. **Preserve VTL variables** — `${fieldId}`, `$maxChar`, `$variableName` are server-side; never change them.
7. **Output three files** — see the File Output Pattern below.

## File Output Pattern

Every migration produces **three files**. You must write all three — not just the migrated version.

Given an original file at `/static/personas/keytag_custom_field.vtl`, the three outputs are:

---

### File 1 — `keytag_custom_field_old.vtl`
The **original file content, completely unchanged**. Copy it verbatim — every deprecated API call, every dijit class, every dojo.ready. This is the fallback for the legacy editor.

---

### File 2 — `keytag_custom_field_new.vtl`
The **fully migrated file** with all deprecated patterns replaced per the migration rules.

---

### File 3 — `keytag_custom_field.vtl` (replaces the original)
The **conditional router** — this file takes the name of the original and delegates to `_new` or `_old` based on which edit mode is active:

```vtl
#if( $structures.isNewEditModeEnabled() )
    #parse('/static/personas/keytag_custom_field_new.vtl')
#else
    #parse('/static/personas/keytag_custom_field_old.vtl')
#end
```

The `#parse` paths must use the **full server path** of the file, not just the filename. Use the same directory as the original file.

---

This pattern lets both legacy and new edit modes coexist safely — old editor users continue using the deprecated code, new editor users get the modernized version.

**When the user gives you a file path**, derive all three filenames automatically. If you only receive file contents without a path, ask for the filename and its server path before outputting.

## Non-Negotiables

- All `getField()` calls must be inside `DotCustomFieldApi.ready()`
- Never use `DotCustomFieldApi.get()` or `DotCustomFieldApi.set()` (the old short forms)
- VTL variables stay exactly as-is
- Business logic stays exactly as-is — only the API calls change
- Dijit CSS classes (any `class="dijit*"`) must be removed; custom CSS classes must be kept
- Translate non-English comments to English

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

**Multiple onChange for the same field** → combine into one handler:
```js
// Old: two separate onChangeField calls for 'title'
// New: one onChange that does both
titleField.onChange((value) => {
  updateURL(value);
  updateFriendlyName(value);
});
```

**Native dialog** (replaces `dojoType="dijit.Dialog"`):
```html
<dialog id="myDialog">...</dialog>
<script>
  DotCustomFieldApi.ready(() => {
    document.getElementById('openBtn').addEventListener('click', () => {
      document.getElementById('myDialog').showModal();
    });
  });
</script>
```

## Before Outputting

Verify the migration passes this checklist (details in `references/migration-guide.md`):

**Three-file output:**
- [ ] `_old.vtl` — original file content, completely unchanged (deprecated code preserved intentionally)
- [ ] `_new.vtl` — fully migrated content
- [ ] Router file (original filename) — contains only the `#if( $structures.isNewEditModeEnabled() )` block with correct full-path `#parse` directives pointing to `_new` and `_old`

**Migrated file (`_new.vtl`):**
- [ ] No `DotCustomFieldApi.get()` or `.set()` or `.onChangeField()` remaining
- [ ] No `dojo.*` or `dijit.*` references remaining
- [ ] No `dojoType` attributes remaining
- [ ] No `dijit*` CSS classes remaining
- [ ] All field access inside `DotCustomFieldApi.ready()`
- [ ] All `getField()` calls stored in variables and reused
- [ ] VTL variables unchanged
- [ ] Business logic unchanged

For complete rules, all migration examples (character counter, title field, slug generator, dialogs, file browser), and edge cases → read `references/migration-guide.md`.
