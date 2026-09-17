# Phase 1 Data Model: #37460 + #37464

No persisted entities, no API payloads, no DB or index changes. The "model" here is **client-side validation-presentation state** — what each field knows, where it learns it, and what it renders as a result.

## State

### Form level — `DotEditContentStore`

| Field | Type | Today | After |
|---|---|---|---|
| `formStatus` | `'init' \| 'valid' \| 'invalid'` | set in exactly one place (`dot-edit-content-form.component.ts:533`), never reset | unchanged — left alone |
| `hasAttemptedSubmit` | `boolean` | — | **new**. `false` initially; set `true` in `fireWorkflowAction` when a save/publish is attempted. One-way: never reset while the editor is open. |

`hasAttemptedSubmit` is added rather than reusing `formStatus === 'invalid'` because the two must be independent: after the author fixes one field the form can still be invalid because of another, yet the fixed field must clear (AC-203). Reasoning in [`research.md#r2`](./research.md).

### Field level — `BaseWrapperField`

| Signal / getter | Today | After |
|---|---|---|
| `$hasError` | `signal(false)`, updated to `control.invalid && control.touched` | `hasAttemptedSubmit && control.invalid` |
| `isRequired` | `field.required`, falling back to `control.hasValidator(Validators.required)` | unchanged — already the right source of truth |
| `$showLabel` | `hideLabel` field variable | unchanged |
| `formControl`, `isDisabled`, `statusChanges$` | — | unchanged |

`control.invalid` already flows through the existing `merge(valueChanges, statusChanges, events)` subscription, so the error clears reactively the moment the value becomes valid — AC-203 needs no additional wiring.

The store is read with `inject(DotEditContentStore, { optional: true })` and a `false` fallback, matching `dot-file-field.component.ts:172`, so nothing breaks if a subclass is ever rendered outside the editor.

## Presentation model

Every field resolves to exactly one of four states. Today only three are reachable — state 4 is unreachable because every template guards the hint with `!fieldHasError`.

| # | `hasError` | `hint` | Renders |
|---|---|---|---|
| 1 | false | — | label + asterisk (if required), control |
| 2 | false | yes | …plus hint text below the control |
| 3 | **true** | — | red border on the control, `This field is required` in red below it, **no icon** |
| 4 | **true** | yes | red border, red required message, **then** the hint below it |

Rendering rules:

- The hint is **always** plain text below the control. Never a tooltip, never an icon (AC-204).
- The error message is text only — no icon — from the `dot.edit.content.form.field.required` key (AC-206).
- Nothing renders when a field has neither, so no empty element and no phantom spacing (AC-205). `dot-card-field`'s `dot-card-field-footer:empty { display: none }` backstops this; a footer containing whitespace or a comment node is no longer `:empty`, so the consolidated template must emit genuinely nothing.
- The asterisk is decorative `::after` content, absent from the accessible name; the control carries `required` / `aria-required` (AC-209).

Reference implementation for states 3 and 4: `dot-edit-content-calendar-field.component.html:22-56`, built by #37465.

## Structural model — the rendered DOM

What `.form .field > label` requires, and what makes each global rule apply.

```
<form class="form">                              ← PR 1. Unlocks every rule below.
  …
  <dot-card-field>
    <div class="field-error-marker">             ← kept; the scroll anchor
    <div class="field">                          ← PR 1, replaces flex flex-col gap-2
      <label dotCardFieldLabel …>                ← PR 1. DIRECT child: this is what makes
                                                     `.form .field > label` match.
      <dot-card-field-content>
        …control…                                ← option rows carry .form-checkbox / .form-radio
      <dot-card-field-footer>                    ← hidden when :empty
        <small class="p-field-error">            ← state 3 and 4
        <small class="p-field-hint">             ← state 2 and 4
```

| Global rule | Selector kind | Applies because |
|---|---|---|
| `.form .field` | descendant | `.form` on the root form |
| `.form .field > label` | **direct child** | the label is now a real DOM child — the whole point of the R1 restructuring |
| `.form .form-checkbox` / `.form-radio` | descendant | class swapped on the option row |
| `.form .form-checkbox label` | descendant | matches option labels at their current depth, no restructuring needed |
| `.form .p-field-hint` / `.p-field-error` | descendant | `.form` on the root form |
| `.p-label-input-required::after` | unscoped, in `_misc.scss` | loaded by **both** bundles — which is what keeps the legacy editor safe |

## Transitions

```
        ┌─ author edits, control becomes valid ─┐
        ↓                                       │
   [no error] ──── save/publish attempted ────→ [error]  (only if control.invalid)
        ↑           (hasAttemptedSubmit=true)     │
        └──────── control becomes valid ──────────┘
```

- **Blur / tab does not appear in this diagram.** That is the behavioral fix: today it is the only trigger (AC-202).
- `hasAttemptedSubmit` is one-way. Once the author has tried to save, subsequent errors surface immediately — which is the intent: the editor has switched from "being filled in" to "being corrected".
- Save stays blocked while any required field is empty, and the form scrolls to the first `.field-error-marker` (AC-210).
