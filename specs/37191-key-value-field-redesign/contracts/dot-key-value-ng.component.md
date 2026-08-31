# Contract: `dot-key-value-ng` shared editor

**Type**: Angular UI contract (public surface of a shared component)
**Exported from**: `@dotcms/ui` (`libs/ui/src/index.ts:42`)
**Consumers**: 3 (see `data-model.md`)

This is the contract three separate features depend on. Phase 1 **must not break it** — that is
what allows the redesign to land without three simultaneous consumer migrations.

---

## Public surface

### Inputs

| Alias | Signal | Type | Default | Status | Requirement |
|---|---|---|---|---|---|
| `variables` | `$variables` | `DotKeyValue[]` | `[]` | **unchanged** | FR-001 |
| `showHiddenField` | `$showHiddenField` | `boolean` | `false` | **unchanged** | FR-021 to FR-024 |

**`dragAndDrop` was removed.** It gated whether a consumer rendered the drag handle. Reordering is
now unconditional — every consumer has it — so the switch had nothing left to decide. `showHiddenField`
is the only remaining per-consumer capability.

> Note: `dot-apps-configuration-detail.component.html:36` binds `[autoFocus]="false"`, an input the
> component does not declare. It is inert today. Remove the stale binding while that template is
> being edited — but do not "fix" it by adding an `autoFocus` input, which would grow the contract
> for a consumer that never used it.

### Outputs

| Name | Payload | Emitted when | Status |
|---|---|---|---|
| `updatedList` | `DotKeyValue[]` | Any change — add, edit, remove, reorder | **unchanged** (FR-013) |
| `save` | `DotKeyValue` | A new pair is added | **unchanged** |
| `update` | `{ variable, oldVariable }` | An existing pair is edited | **unchanged** |
| `delete` | `DotKeyValue` | A pair is removed | **unchanged** |

The dual channel is intentional and must be preserved: Edit Content consumes `updatedList` (whole
array), while Field Variables consumes `save`/`update`/`delete` to persist per row. Collapsing them
would break consumer 2.

**`updatedList` is order-bearing, and the array is the only thing carrying that order.** A consumer
that funnels it through a JavaScript object — `reduce` into `{}`, spread, `Object.fromEntries` — loses
the position of every key made only of digits, silently and unrecoverably. Serialize from the array
directly. See [research.md R-09](../research.md#r-09--key-order-survival-across-the-json-boundary).

---

## Rendering contract

| Element | Condition | Visibility at rest | Requirement |
|---|---|---|---|
| Entry row (key input, value input, add button) | always | visible | FR-003 |
| Body row key | always | plain text, never an input | FR-005, FR-008 |
| Body row value | always | plain text; becomes an input on activation | FR-005, FR-006 |
| Drag handle | always | **`opacity-0`**, revealed on hover/focus-within | FR-017, FR-019 |
| Remove control | always | **`opacity-0`**, revealed on hover/focus-within | FR-017, FR-019 |
| Eye control (in-field) | `showHiddenField === true`, **entry row only** | visible within the value control | FR-021, FR-023a |
| Withheld indicator | `showHiddenField === true` **and** row's `hidden === true` | **permanently visible** — lock + "Value hidden", no value, no input | FR-018, FR-022, FR-023 |
| Any visibility control on an existing row | never | absent | FR-023a |
| Empty state | list is empty | icon + message, entry row still usable above | FR-014 |
| Load-more row | more than 40 rows remain unrendered | appended after the last row; **left-aligned** "Load more", no count | FR-037 to FR-039 |

### Paging must not shorten the bound array

Only 40 rows are rendered, but `[value]` still receives the **entire** list, with rows withheld in the
body template. PrimeNG reorders the array it is given, so binding a slice loses the drag silently. A
rendered prefix also keeps `rowIndex` a true index, which every row handler depends on. See
[research.md R-10](../research.md#r-10--row-paging-render-limit-not-a-data-limit).

### Visibility is chosen at creation only

The eye lives in the **entry row** and nowhere else. An existing row has no visibility control — not to
reveal, not to hide. This is not an oversight to be "fixed": the server sends `*****` instead of the
stored secret, so revealing shows the mask, and saving that mask — or flagging it as a new secret —
overwrites the real secret. Any change here must first establish that the true value actually reaches
the client.

### The one rule most likely to be implemented wrongly

Hidden actions **must** use `opacity-0`. They must **not** use `hidden`, `display: none`, or an
`@if` on hover state — all three remove the control from the tab order, breaking FR-019 for keyboard
and touch users. A test asserts this directly rather than asserting "is not visible".

---

## Icons (DC-003 — Material Symbols)

Icon name is the element's **text content**, not a class:

```html
<span class="material-symbols-outlined">drag_indicator</span>
```

| Purpose | Was | Is |
|---|---|---|
| Drag handle | `pi pi-bars` | `drag_indicator` |
| Remove row | `pi pi-times` | `close` |
| Withheld indicator | `pi pi-lock` | `lock` |
| Hide control (eye) | — | `visibility` |
| Load more | — | `add_circle` |
| Empty state | `pi pi-folder-open` | `key` |

Icons PrimeNG renders internally (e.g. inside `p-cellEditor`) are a theming concern and out of
scope.

---

## PrimeNG building blocks (DC-001, DC-002)

All verified present in `primeng@21.1.3`. **No new component is introduced.**

| Need | PrimeNG primitive |
|---|---|
| Table, header/body/empty templates | `p-table`, `#header`, `#body`, `#emptymessage` |
| Click-to-edit | `pEditableColumn`, `p-cellEditor` with `#input` / `#output` |
| Row reordering | `pReorderableRow`, `pReorderableRowHandle`, `(onRowReorder)` |
| Eye inside the value field | `p-iconfield`, `p-inputicon` |
| Inputs and buttons | `pInputText`, `p-button` |

**Removed**: the `p-toggleSwitch` hidden column. Its function moves into the value control.

---

## i18n keys

All already exist in `Language.properties` — **no new key is required**.

| Key | Value |
|---|---|
| `keyValue.key_input.placeholder` | Enter Key |
| `keyValue.value_input.placeholder` | Enter Value |
| `keyValue.key_input.required` / `value_input.required` | This field is required |
| `keyValue.key_input.duplicated` | This key already exists |
| `keyValue.value_hidden` | Value hidden |
| `keyValue.value_no_rows.label` | **Add your key and value here** — matches the mockup copy exactly |

One key **is** added: `keyValue.action.load_more` = *Load more*. Same wording as
`dot.file.field.host.folder.action.load.more`, kept as its own key because that one is namespaced to
the host/folder field.

---

## Test selectors

Existing `data-testId` values are part of the de-facto contract with the Playwright suite
(`apps/dotcms-ui-e2e/.../key-value-field/`, 241 lines). Preserve them, or update the e2e helpers in
the same change:

`dot-key-value-key`, `dot-key-value-editable-column`, `dot-key-value-input`,
`dot-key-value-delete-button`, `dot-key-value-hidden-switch`, `dot-key-value-label`, `no-rows`

Added by paging: `dot-key-value-load-more-row`, `dot-key-value-load-more`.

> `dot-key-value-hidden-switch` names a control being removed. Rename it to something the eye
> affordance actually is, and update `apps/dotcms-ui-e2e/.../helpers/key-value-field.ts` in the same
> commit so the suite never points at a selector that no longer exists.

---

## Breaking-change policy for Phase 1

| Change | Allowed? |
|---|---|
| Internal markup, styling, icons | ✅ yes |
| Row component internals (not exported) | ✅ yes |
| Removing the hidden-value **column** | ✅ yes — presentation, not contract |
| Removing `dragAndDrop` | ✅ done — the capability is now universal, so the input had nothing to gate |
| Adding row paging | ✅ done — internal rendering; no input, output or payload changed |
| Adding an input/output | ⚠️ only with a stated reason |
| Renaming or removing an input/output | ❌ no — breaks 3 consumers |
| Changing `DotKeyValue`'s shape | ❌ no — breaks persistence in all 3 |
