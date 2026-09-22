# Contract: the content-type-field type surface

**Feature**: `specs/37670-strict-field-type-union` | **Date**: 2026-09-22

This feature exposes no REST endpoint, no CLI and no wire format. Its contract is the
**TypeScript surface** that `@dotcms/dotcms-models` publishes to the rest of the `core-web`
workspace, and the rules any consumer must be able to rely on.

Scope note: `@dotcms/dotcms-models` is **not published to npm** (verified — the registry
returns 404; see research R-03). This contract is internal to this repository and binds no
external consumer. [ADR-0019](https://github.com/dotCMS/platform-adrs/blob/main/decisions/0019-sdk-cms-date-lockstep-versioning.md)'s
date-lockstep rules govern the SDKs in `libs/sdk/`, not this library.

---

## C-1: The discriminant

`DotCMSContentTypeField` is a discriminated union over `fieldType`.

**A consumer may rely on**: narrowing on `fieldType` against a `DotCMSFieldTypes` member
yields exactly one arm, with that arm's required properties reachable without an assertion.

```ts
// guaranteed to compile
if (field.fieldType === DotCMSFieldTypes.CATEGORY) {
    field.categories;        // required on ContentTypeCategoryField
}

// guaranteed NOT to compile
if (field.fieldType === DotCMSFieldTypes.CATEGORY) {
    field.relationships;     // belongs to a different arm
}
```

**Correlated discriminants**: `dataType` and `clazz` are pinned by the same arm, so narrowing
on any one of the three narrows all three. A consumer may narrow on `clazz` where that reads
better and get the same guarantee.

---

## C-2: Exhaustiveness

The vocabulary `DotCMSFieldTypes` has 28 members and the union has 28 arms, one per member.

**A consumer may rely on**: a `switch` over `fieldType` that handles every member leaves
`never` in the default branch, and any map declared over the vocabulary is complete or does
not compile.

**The contract's obligation to its maintainer**: adding a member to `DotCMSFieldTypes` without
adding the matching arm, or without extending every exhaustive map, **must break the build**
(FR-008). This is the property that keeps the union honest over time, and SC-011 requires it
be demonstrated once rather than assumed.

---

## C-3: Per-key narrowing for behavior maps

Any map from field type to behavior is declared as a mapped type over the discriminant, not
as a `Record` with one union-wide handler signature:

```ts
type FieldOf<K extends DotCMSFieldType> = Extract<DotCMSContentTypeField, { fieldType: K }>;

type BehaviorMap<R> = {
    [K in DotCMSFieldType]: (field: FieldOf<K>, ...rest: never[]) => R;
};
```

**A consumer may rely on**: the handler registered under a key receives that key's arm, and
the compiler enforces it.

**Why the stricter form is required, not merely preferred**: `libs/edit-content` compiles with
`strict: false`, which disables `strictFunctionTypes` and makes parameter positions compare
bivariantly. Under that setting a `Record<DotCMSFieldType, (f: DotCMSContentTypeField) => R>`
happily accepts a handler declaring a narrowed parameter — the narrowing becomes documentation
with no check behind it. That is the shape the abandoned PR #31964 used. It is rejected here
(FR-007), and independently it would now **fail CI**: the strict gate forces `strict` on every
line a pull request writes (research R-02), and the map assignment is such a line.

**The one sanctioned assertion**: TypeScript cannot correlate a key's narrowing with an
argument's narrowing at the call site (TypeScript issue #30581). Dispatch therefore goes
through a single generic helper that contains one internal assertion:

```ts
const dispatch = <K extends DotCMSFieldType>(map: BehaviorMap<R>, field: FieldOf<K>, ...rest) =>
    (map[field.fieldType as K] as BehaviorMap<R>[K])(field, ...rest);
```

One assertion, in one helper, replacing 16 scattered ones — and every call site outside it is
fully checked. FR-006 forbids swapping a cast for another cast *at the sites being fixed*;
this is a dispatch primitive, and it is declared here rather than buried so review can see it.

---

## C-4: Construction

**A consumer may rely on**: `createFake*Field` factories in
`libs/utils-testing/src/lib/dot-content-types.mock.ts` return complete, valid arms.

**A consumer may not**: assert a partial literal into the type. Partials are not members of
any arm, and an assertion that makes one compile is a defect under FR-006/FR-014, not a
workaround. Measured at branch point: 89 `as DotCMSContentTypeField`, 33
`as unknown as DotCMSContentTypeField`, 15 `Partial<DotCMSContentTypeField>`, 7
`as DotCMSContentTypeField[]` — all of which this feature removes or replaces.

---

## C-5: The boundary claim

Two services in `libs/data-access` turn server JSON into this type by declaration:

| Service | Line | Entry point |
|---|---|---|
| `dot-field.service.ts` | 34 | `getFields(): Observable<DotCMSContentTypeField[]>` |
| `dot-content-type.service.ts` | 48, 65 | `getContentType()`, `getContentTypeWithRender()` — fields arrive inside `layout` |

**A consumer may rely on**: a field whose `fieldType` the frontend does not model does not
throw, does not prevent the surrounding content type from rendering, and is distinguishable
from a modelled field rather than silently mistyped as one (FR-013).

**A consumer may not** assume the union is closed over what the *server* can send. It is closed
over what the *frontend models*. Customer plugins can contribute field types, so the two sets
are not the same, and this is the one place in the feature where that gap is handled rather
than assumed away.

---

## C-6: No compatibility alias

The flat interface is replaced outright — there is no deprecated alias standing in for it at
any point. The migration moves all 149 consuming files in one change rather than in batches
(FR-003, FR-017).

**A consumer may rely on**: `DotCMSContentTypeField` being the union and nothing else. Code
that needs the old loose shape has no supported way to keep it.

**A consumer may not** expect a deprecation window. The library is unpublished (R-03), so
every consumer lives in this repository and moves with the change.

---

## Contract verification

| Contract | How it is proven | Success criterion |
|---|---|---|
| C-1 | Type tests: narrow to each of the 28 arms, reach its required property; a wrong-arm property fails to compile | SC-002 |
| C-2 | A deliberate vocabulary addition breaks the build, demonstrated once | SC-011 |
| C-3 | A handler registered under the wrong key fails to compile; the strict gate passes on the map's lines | SC-005 |
| C-4 | No assertion into the type remains in the workspace | SC-003, SC-005 |
| C-5 | Unit test at each of the two services with an unmodelled `fieldType` | SC-009 |
| C-6 | Searching the model for the flat shape returns no declaration, deprecated or otherwise | SC-001 |

Type-level contracts (C-1, C-2, C-3) are proven by **compilation**, not by runtime assertions:
the test is that a wrong usage fails to build. Per constitution Principle V these still come
first — the failing compile is the Red phase, and "it does not compile yet" is the correct
initial state for a type test.
