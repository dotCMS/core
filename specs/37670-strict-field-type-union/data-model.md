# Phase 1 Data Model: Strict per-field-type typing of the content model

**Feature**: `specs/37670-strict-field-type-union` | **Date**: 2026-09-22

This feature changes no stored data, no REST payload and no database schema. The "data model"
here is the **compile-time model** of a content type field in `core-web` — the shapes the
frontend uses to describe JSON it already receives unchanged. Every entity below lives in
`core-web/libs/dotcms-models/src/lib/dot-content-types.model.ts` unless stated otherwise.

---

## Entity: field type vocabulary

The closed set of field-type discriminant values.

| State | Declaration | Members | Location |
|---|---|---|---|
| **Before** | `enum FIELD_TYPES` | 24 | `libs/edit-content/src/lib/models/dot-edit-content-field.enum.ts` |
| **Before** | `const FIELD_TYPES_CONST` | 24 | same file |
| **Before** | `const DotCMSFieldTypes` + `type DotCMSFieldType` | 28 | `dot-content-types.model.ts:87` |
| **After** | `const DotCMSFieldTypes` + `type DotCMSFieldType` | 28 | `dot-content-types.model.ts:87` (unchanged) |

The two `edit-content` declarations are deleted (FR-005). Their string values are identical to
the survivor's where they overlap, so no runtime comparison changes result.

The four members only the survivor has — `ROW`, `COLUMN`, `TAB_DIVIDER`, `COLUMN_BREAK` — are
the layout types. Any map keyed by the vocabulary gains four entries when it is repointed.

**Migration rule**: a reference in *value* position (`FIELD_TYPES.TEXT`) becomes
`DotCMSFieldTypes.TEXT` and keeps working. A reference in *type* position (`: FIELD_TYPES`)
becomes `: DotCMSFieldType` — the `enum`-to-`const object` change splits one name into two.
Measured: 278 references across 18 files, of which 51 are in type position.

---

## Entity: content type field

The central entity. One field on a content type.

### Before

A single flat interface, `DotCMSContentTypeField` (`dot-content-types.model.ts:540`), with:

- `fieldType: string` — unconstrained, so no narrowing is possible
- `dataType: string` — likewise
- every per-type property optional: `categories?`, `relationships?`, `values?`, `regexCheck?`

Any code needing to know the kind of field must assert.

### After

A discriminated union of the 28 per-type interfaces, discriminated on `fieldType`:

```
DotCMSContentTypeField
├── layout arms (5)
│   ContentTypeRowField · ContentTypeColumnField · ContentTypeTabDividerField
│   ContentTypeColumnBreakField · ContentTypeLineDividerField
└── content arms (23)
    ContentTypeBinaryField · ContentTypeBlockEditorField · ContentTypeCategoryField
    ContentTypeCheckboxField · ContentTypeConstantField · ContentTypeCustomField
    ContentTypeDateField · ContentTypeDateTimeField · ContentTypeFileField
    ContentTypeHiddenField · ContentTypeImageField · ContentTypeJSONField
    ContentTypeKeyValueField · ContentTypeMultiSelectField · ContentTypeRadioField
    ContentTypeRelationshipField · ContentTypeSelectField · ContentTypeHostFolderField
    ContentTypeTagField · ContentTypeTextField · ContentTypeTextAreaField
    ContentTypeTimeField · ContentTypeWYSIWYGField
```

All 28 arms already exist and are already merged (PR #33056). None is authored by this
feature; the feature connects them.

Each arm extends `DotCMSContentTypeBaseField` (`dot-content-types.model.ts:172`) and pins
three properties to single literal types:

| Property | Type in an arm | Example (`ContentTypeCategoryField`) |
|---|---|---|
| `fieldType` | `typeof DotCMSFieldTypes.X` | `typeof DotCMSFieldTypes.CATEGORY` |
| `dataType` | `typeof DotCMSDataTypes.Y` | `typeof DotCMSDataTypes.SYSTEM` |
| `clazz` | `typeof DotCMSClazzes.Z` | `typeof DotCMSClazzes.CATEGORY` |

`fieldType` is the discriminant. `dataType` and `clazz` are correlated, not independent — an
arm fixes all three together, so narrowing on any one of them narrows the other two.

### Validation rules (from the requirements)

- **FR-001**: narrowing on `fieldType` yields the arm, with its required properties reachable
  without assertion.
- **FR-002**: the arms are consumed by production code, not only by the test-mock file.
- **FR-003**: the flat shape is deleted in the same change that introduces the union; no
  deprecated alias coexists with it at any point, and the union is the only content-type-field
  type (see "No transitional shape" below).
- A value is a valid member only if `fieldType`, `dataType` and `clazz` agree with one arm.
  Partial literals are not members — this is the source of the 89 `as DotCMSContentTypeField`
  sites that must move to factories (FR-014).

### Derived shape used by behavior maps

```ts
type FieldOf<K extends DotCMSFieldType> = Extract<DotCMSContentTypeField, { fieldType: K }>;
```

The projection from a discriminant value to its arm. It is what makes per-key narrowing
expressible (FR-007) and is used by every map in the feature.

### No transitional shape

No compatibility alias is introduced. The flat interface is deleted in the same change that
adds the union, and all 149 consumers move with it (FR-003, FR-017). The alternative — a
`@deprecated` alias carrying the old shape through a batched migration — was considered and
dropped: `@dotcms/dotcms-models` is not published (research R-03), so every consumer lives in
this repository and an alias would only let new code keep writing the loose shape.

---

## Entity: field draft *(new — content type editor only)*

**Status: shape deliberately undecided.** Recorded here because the need for it is certain and
its form is not.

The content type editor builds a field from live form state and returns it as a field
(`content-type-fields-properties-form.component.ts:182,184`). A field under construction is
genuinely not yet a valid union member — the user has not chosen everything the discriminant
needs. Asserting form state into the union would reintroduce exactly what this feature removes.

This entity is the acknowledgment that "a field being edited" and "a field" are different
things that the flat interface conflated. The plan reserves the decision for the implementer,
with the code in front of them; see research R-06, site 3, for the alternatives and why
pre-deciding was rejected.

**Consumers**: the content type editor in `apps/dotcms-ui/.../dot-content-types-edit/` only.
Nothing in `libs/edit-content` needs it.

---

## Entity: field transformation

The mapping from a stored content value to a form control value.

### Before — two paths that both assert

| Path | Location | Asserts |
|---|---|---|
| `resolutionValue` | `dot-edit-content-form-resolutions.ts:283` — `Record<FIELD_TYPES, FnResolutionValue<…>>`, 24 entries | at lookup: `dot-edit-content-form.component.ts:743` |
| `getFinalCastedValue` | `functions.util.ts:40` | three times, at lines 44, 48, 52 |

### After — one path, narrowed per key

```ts
type ResolutionMap = {
    [K in DotCMSFieldType]: (
        contentlet: DotCMSContentlet,
        field: FieldOf<K>,
        queryParams?: EditContentQueryParams,
        isManualTranslation?: boolean
    ) => ResolvedValue;
};
```

28 entries, exhaustive by construction (FR-008). Each handler declares the arm it handles
(FR-010). Lookup goes through one generic dispatch helper (FR-011) instead of 16 scattered
assertions.

**Behavior preserved (FR-012)**: the arguments and resolvers in the signature above are
`main`'s, not the abandoned branch's. `queryParams`, `isManualTranslation`, the generic return
type, and the key-value / block-editor / text resolvers all postdate the abandoned file and
must survive. The four layout entries the current map lacks are the one thing taken from it.

---

## Entity: layout structure

Tabs → rows → columns → fields.

| Shape | Property | Before | After |
|---|---|---|---|
| `DotCMSContentTypeLayoutRow` | `divider` | `DotCMSContentTypeField` | unchanged — see below |
| `DotCMSContentTypeLayoutColumn` | `columnDivider` | `DotCMSContentTypeField` | unchanged — see below |
| `DotCMSContentTypeLayoutColumn` | `fields` | `DotCMSContentTypeField[]` | unchanged — a column holds any field |

Narrowing these to the layout arms is a constraint the model could now state, and this change
does **not** take it: both properties stay `DotCMSContentTypeField`. It is where the migration
is most likely to surface consumers relying on the loose type — 31 production references to
`.divider` / `columnDivider` were measured — and separating that from the field-type union
keeps this change reviewable. `transformLayoutToTabs` (`functions.util.ts:63`) reads
`row.divider.clazz` and compares against a tab-field class; that comparison becomes a real
narrowing only once `divider` is typed, which remains open work.

---

## Entity: field test factory

`createFake*Field` helpers in `libs/utils-testing/src/lib/dot-content-types.mock.ts`.

- **Now**: roughly nine, returning per-type interfaces. Already correct; already the only
  non-model importer of the arms.
- **After**: extended toward all 28 as each area migrates. Each returns a complete, valid arm,
  which is what lets a spec drop its `as DotCMSContentTypeField`.

These are the mechanism for migrating 52 spec files (FR-014). Reused, never duplicated; new
ones land beside the existing ones. Coverage must not drop in the process (FR-015) — a spec
that is awkward to type gets rewritten, not deleted or loosened.

---

## Entity relationships

```
DotCMSContentType
  └── layout: DotCMSContentTypeLayoutRow[]
        ├── divider: DotCMSContentTypeField          ← not narrowed yet (see above)
        └── columns: DotCMSContentTypeLayoutColumn[]
              ├── columnDivider: DotCMSContentTypeField   ← not narrowed yet (see above)
              └── fields: DotCMSContentTypeField[]   ← the union

DotCMSContentTypeField (union of 28)
  ├── discriminated by  fieldType: DotCMSFieldType
  ├── correlated with   dataType, clazz
  ├── every arm extends DotCMSContentTypeBaseField
  ├── projected by      FieldOf<K> = Extract<…, { fieldType: K }>
  ├── keys              ResolutionMap and every other per-type behavior map
  └── built in tests by createFake*Field
```

---

## Boundaries where untyped data becomes typed

The union is a compile-time claim about runtime JSON. Two services make that claim, both in
`libs/data-access`, and neither validates it today:

| Service | Line | Claim |
|---|---|---|
| `dot-field.service.ts` | 34 | `getFields(): Observable<DotCMSContentTypeField[]>` — straight from `DotCMSAPIResponse` |
| `dot-content-type.service.ts` | 48, 65 | `getContentType()` / `getContentTypeWithRender()` — fields arrive inside `layout` |

A `fieldType` the frontend does not model — including one contributed by a customer plugin,
which is a real dotCMS scenario — makes that claim false. The flat interface accepted any
string, so the problem was invisible; the union makes it visible without creating it.

FR-013 is implemented here: an unrecognized field type degrades visibly, does not throw, and
leaves the rest of the content type editable. This is the only part of the feature with
runtime behavior to verify rather than types to check.
