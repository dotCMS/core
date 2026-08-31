# Phase 1 Data Model: Redesigned Key/Value Field

**Feature**: `specs/37191-key-value-field-redesign` | **Date**: 2026-08-27

The shared editor works with one entity, and this feature does not change its shape.

---

## Entity: `DotKeyValue`

Declared in `libs/ui/src/lib/components/dot-key-value-ng/dot-key-value-ng.component.ts` and
re-exported from `@dotcms/ui`.

| Field | Type | Required | Notes |
|---|---|---|---|
| `key` | `string` | yes | Unique within one editor instance. Non-empty. **Immutable after creation** (FR-008) |
| `value` | `string` | yes | Non-empty (FR-011). May hold the literal `"null"` for legacy rows whose stored value was null |
| `hidden` | `boolean` | no | Masks the value in the UI. Only meaningful in the Apps consumer (FR-024) |

**Shape is unchanged by Phase 1.** No field is added, removed, or retyped. This is deliberate: it is
what allows all three consumers to keep their existing payloads (FR-033 to FR-036).

### Validation rules

| Rule | Enforced where | Requirement |
|---|---|---|
| `key` non-empty | Entry-row form, `Validators.required` | FR-011 |
| `value` non-empty | Entry-row form, `Validators.required` | FR-011 |
| `key` not already present | Entry-row `keyValidator()` against the container's `$forbiddenkeys` map | FR-010 |
| `key` not editable post-creation | No editable control bound to `key` on body rows | FR-008 |

Duplicate detection lives in the entry-row component's key validator, against the container's
`$forbiddenkeys` map. (`DotKeyValueUtil` was deleted — it was unused.)

### Position

Position is **implicit** — it is the index of the pair within the array the editor holds and emits.
There is no `order` field on `DotKeyValue`, and none is added.

An array carries position; a JavaScript **object does not**, for keys made only of digits. ECMAScript
enumerates integer-like keys first, ascending, in any object, so `{"123": …, "zzz": …}` is read back in
that order no matter how it was written. The pair list is therefore only ever converted to and from
**JSON text**, never through an intermediate object.

This constrains what the Edit Content form control may hold: **JSON text**, in the field's own order.

| Form control holds | Order kept | Save without editing |
|---|---|---|
| **JSON text** ← in use | yes | accepted (server takes a JSON string) |
| Object | **no** — digits hoisted | accepted |
| Array of pairs | yes | **rejected** — `KeyValueField.fieldValue` takes a `String` or `Map`, never a `List` |

Rationale, alternatives, and the safety guard: [research.md R-09](./research.md#r-09--key-order-survival-across-the-json-boundary).

**Paging does not touch any of this.** Long lists render 40 rows at a time, but the pair list itself is
never shortened or re-indexed — positions, emitted payloads and the JSON text are identical whether a
row is on screen or not (FR-040).

---

## Editor capability (per consumer)

One boolean input on the container selects the only affordance that varies between consumers.

| Input | Type | Default | Effect |
|---|---|---|---|
| `showHiddenField` | `boolean` | `false` | Renders the eye toggle and masks hidden values (FR-021 to FR-024) |

### Capability matrix

| Consumer | `showHiddenField` | Reordering |
|---|---|---|
| Edit Content field | `false` | always |
| Field Variables | `false` | always |
| Apps custom properties | **`true`** | always |

**Reordering is no longer a capability.** It began as a per-consumer switch (`dragAndDrop`); once
every consumer had the handle, the input had nothing left to decide and was removed.
