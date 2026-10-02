# Feature Specification: Complete the strict per-field-type typing of the content model

**Feature Branch**: `nicobytes/complete-the-strict-per-field-type-typing-of-the`

**Created**: 2026-09-22

**Status**: Draft

**Type**: Refactoring

**Input**: GitHub issue [#37670](https://github.com/dotCMS/core/issues/37670) — "Complete the strict per-field-type typing of the content model (revive PR #31964)", with the abandoned [PR #31964](https://github.com/dotCMS/core/pull/31964) (branch `31911-refactor-unify-field-type-transformation-logic-by-aligning-resolutionvalue-and-getfinalcastedvalue`) as the reference port source.

---

## Context

The frontend content model describes a content type field with a single flat interface,
`DotCMSContentTypeField` (`core-web/libs/dotcms-models/src/lib/dot-content-types.model.ts:540`),
whose `fieldType` is a bare `string` and whose per-type properties (`categories`,
`relationships`, `values`, `regexCheck`) are all optional. Any code that needs to know
*which kind* of field it holds must therefore assert the type by hand.

A strict per-type replacement already exists in the same file and is already merged: the
`DotCMSFieldTypes` / `DotCMSDataTypes` / `DotCMSClazzes` constant objects, the
`DotCMSContentTypeBaseField` base interface, and **28 per-type interfaces**
(`ContentTypeRowField`, `ContentTypeCategoryField`, … `ContentTypeWYSIWYGField`), each
pinning `fieldType`, `dataType` and `clazz` to literal types and declaring its own required
properties. Those landed in [PR #33056](https://github.com/dotCMS/core/pull/33056), which
salvaged the type layer of the abandoned #31964.

**The keystone was never placed.** `DotCMSContentTypeField` was never converted into the
discriminated union of those 28 interfaces. Verified in this worktree: the only two files
that reference any `ContentType*Field` interface are the model file that declares them and
`core-web/libs/utils-testing/src/lib/dot-content-types.mock.ts`. The 28 interfaces are
orphans — documented, exported, and typing nothing that ships.

### Verified current state

The issue's figures were measured before later merges; the numbers below were re-counted in
this worktree at branch point `85f6976346` and supersede them.

| Measure | Issue #37670 says | Verified now |
|---|---|---|
| Files referencing `DotCMSContentTypeField` | 140 | **149** |
| …of which spec files | 47 | **52** |
| …of which production files | 93 | **97** |
| `as FIELD_TYPES` / `fieldType as` casts in `libs/edit-content/src` (production) | 15 | **16** |
| Per-type interfaces defined and unused | 28 | **28** (confirmed orphaned) |

Distribution of the 149 files: `libs/edit-content` 68, `apps/dotcms-ui` 37, `libs/ui` 13,
`libs/portlets/dot-content-drive` 9, `libs/portlets/edit-ema` 6,
`libs/dotcms-webcomponents` 5, `libs/new-block-editor` 4, `libs/dotcms-models` 2,
`libs/data-access` 2, and one each in `libs/utils`, `libs/utils-testing`, `libs/block-editor`.

### Two findings the issue does not record

Both were discovered while verifying the issue and both change what the work is.

**1. The casts are caused by a duplicated enum, not only by the flat type.**
`libs/edit-content` declares its own `FIELD_TYPES` enum *and* a `FIELD_TYPES_CONST` object
(`core-web/libs/edit-content/src/lib/models/dot-edit-content-field.enum.ts`) carrying the
same string values as `DotCMSFieldTypes` in `dotcms-models` — three declarations of one set
of values. `FIELD_TYPES` has 24 members; `DotCMSFieldTypes` has 28 (it adds the layout types
`ROW`, `COLUMN`, `TAB_DIVIDER`, `COLUMN_BREAK`). Because the union discriminates on
`DotCMSFieldTypes` while `edit-content` compares against its own enum, flipping the union
alone would **not** remove a single `as FIELD_TYPES` cast — the two remain distinct types.
Collapsing the duplication is therefore in scope (see FR-005), and it is what makes the
issue's own "remove the casts" acceptance criterion achievable rather than cosmetic.

**2. The abandoned `resolution-values.utils.ts` cannot be ported verbatim — `main` has moved past it.**
The file on the abandoned branch (236 lines) predates roughly fourteen months of work on
`main`'s `dot-edit-content-form-resolutions.ts` (11 KB), which the branch version lacks:
a `queryParams` argument, an `isManualTranslation` argument, a generic
`FnResolutionValue<T>` return type, and dedicated `keyValueResolutionFn`,
`blockEditorResolutionFn` and `textFieldResolutionFn` resolvers. Copying the old file over
the new one would silently delete shipped behavior. What transfers is the **pattern** — each
resolver declaring the narrowed field interface it actually needs — applied to `main`'s
current resolvers. The old file is a design reference, not a source to restore.

There is a related soundness trap. `libs/edit-content/tsconfig.json` sets `strict: false`,
so `strictFunctionTypes` is off and function parameters compare bivariantly. That means the
abandoned branch's shape — resolvers with narrowed parameters stored in a
`Record<FieldType, (c, f: DotCMSContentTypeField) => …>` — compiles without complaint while
providing no actual guarantee that the resolver receives the field type it declares. A
narrowed parameter that the compiler never checks is a cast wearing better clothes. The
mapping must be structured so each key's resolver signature is narrowed *per key*, so the
guarantee survives regardless of the strictness flag (FR-007). Note that
`libs/dotcms-models/tsconfig.json` is already `strict: true`, so the union declaration
itself is checked strictly.

### Prior art and why it failed

PR #31964 was never rejected on technical grounds. The only comments on it are the bot
asking for a Conventional Commit title; it was auto-closed by the stale bot on 2025-07-20
after 30 days without review. Its failure mode was its size: +2785/−1512 across 75 files, a
diff nobody picked up. This specification does not repeat that shape. Issue
[#31911](https://github.com/dotCMS/core/issues/31911), the original driver, is marked
CLOSED/COMPLETED as of 2026-04-30 although the unification it asked for never happened —
both `getFinalCastedValue` and `resolutionValue` still exist separately and both still cast.

---

## User Scenarios & Testing *(mandatory)*

The "user" throughout is a dotCMS frontend developer working in `core-web`, plus the
reviewers who must be able to approve this work. The stories below are **units of work inside
one pull request** (FR-017), not separate deliveries — they are ordered by dependency, and
each remains independently *testable* even though they ship together.

### User Story 1 - The union exists and the 28 interfaces stop being orphans (Priority: P1)

A developer writes `if (field.fieldType === DotCMSFieldTypes.CATEGORY)` and, inside that
branch, the compiler knows the field has `categories`. The 28 per-type interfaces become
reachable from ordinary code for the first time. The flat shape is replaced outright — no
deprecated alias — because measurement showed only 19 files actually break, so there is no
batch of stragglers for an alias to shelter.

**Why this priority**: Nothing else in the feature is possible before the union exists — it
is the dependency root, so it is done first within the PR.

**Independent Test**: Declare a value of each of the 28 field types, narrow on `fieldType`,
and confirm the per-type property is reachable without an assertion and that a wrong-type
property is a compile error. The whole workspace still builds untouched.

**Acceptance Scenarios**:

1. **Given** a value typed as the content-type-field union, **When** the code narrows on
   `fieldType` to the category type, **Then** `categories` is available as a required
   property and no type assertion is written.
2. **Given** the same narrowed value, **When** the code reads a property belonging to a
   different field type, **Then** compilation fails.
3. **Given** the whole `core-web` workspace, **When** type-checking and unit tests run,
   **Then** the 19 files the union breaks are fixed by narrowing and nothing else regressed.
4. **Given** the codebase after this change, **When** a developer searches for the flat
   content-type-field shape, **Then** it does not exist — there is exactly one such type.

---

### User Story 2 - One vocabulary for field types (Priority: P1)

A developer stops choosing between three names for the same thing. The duplicate field-type
enum and companion constant object in `edit-content` are gone; the whole workspace speaks
`DotCMSFieldTypes`. Comparisons against `fieldType` type-check on their own, so the
assertions that existed only to bridge the two vocabularies disappear.

**Why this priority**: This is the precondition for removing the casts. Without it, the
casts survive the union and the headline acceptance criterion of issue #37670 cannot be met
honestly. It is P1 rather than P2 because Story 3's cast removal is blocked on it.

**Independent Test**: Search `libs/edit-content` for the local field-type enum and constant
object and find no declaration of either; confirm every field-type comparison in that
library resolves without an assertion; run the library's unit tests.

**Acceptance Scenarios**:

1. **Given** the edit-content library, **When** a developer looks for a field-type
   vocabulary, **Then** exactly one exists and it is the one published by the content model
   library.
2. **Given** a mapping that must cover every field type, **When** a field type is added to
   the shared vocabulary, **Then** the mapping fails to compile until the new type is
   handled — including the four layout types the local enum never carried.
3. **Given** the runtime behavior of the edit content form, **When** the vocabulary is
   swapped, **Then** every field type renders and saves exactly as before, because the
   string values are identical.

---

### User Story 3 - The edit-content library narrows instead of asserting (Priority: P2)

Every file in `libs/edit-content` — production and spec — consumes the union. The 16
production assertions are gone, replaced by real narrowing. Nothing is swapped for a
double-assertion or an escape to `any`, and no suppression comment is added to get the
build green.

**Why this priority**: `edit-content` holds 68 of the 149 files and all 16 casts; it is
where the union pays for itself. It follows Stories 1 and 2 because both are hard
preconditions.

**Independent Test**: Type-check and run the unit tests for `edit-content` with the
deprecated flat shape removed from that library's imports; grep the library for assertions
on `fieldType` and `clazz` and find none.

**Acceptance Scenarios**:

1. **Given** a function that behaves differently per field type, **When** it is read after
   this change, **Then** it narrows on `fieldType` and the compiler — not the author —
   guarantees which properties are present.
2. **Given** the diff for this story, **When** it is reviewed, **Then** it contains no new
   double-assertion, no new `any`, and no new compiler-suppression comment.
3. **Given** the edit-content unit tests, **When** they run, **Then** they pass with no loss
   of coverage relative to the branch point.
4. **Given** a spec that needs a field of a specific type, **When** it is read, **Then** it
   uses an existing shared test factory rather than a hand-built literal.

---

### User Story 4 - The remaining consumers migrate (Priority: P2)

The other 81 files — `dotcms-ui`, `ui`, the portlets, the web components, the block editors,
data access, utils — consume the union. Layout structures that were typed as "some field"
now say which kind of field they actually hold.

**Why this priority**: Necessary to retire the deprecated flat shape, but each area is
independent and lower-risk than `edit-content`. Splitting it across more than one pull
request is acceptable and expected.

**Independent Test**: Type-check and unit-test each area with the deprecated shape removed
from its imports.

**Acceptance Scenarios**:

1. **Given** a layout row, **When** its divider is read, **Then** the type says it is a
   divider-kind field rather than any field.
2. **Given** each migrated area, **When** its lint and unit tests run, **Then** they pass.
3. **Given** a consumer that genuinely does not care which field type it holds, **When** it
   is migrated, **Then** it depends on the shared base shape rather than asserting.

---

### User Story 5 - One transformation path, and the flat shape is gone (Priority: P3)

The deprecated flat interface is deleted — the union is the only content-type-field type in
the codebase. In the same step the transformation logic is unified as issue #31911 asked:
each resolver declares the narrowed field it handles, the form no longer asserts a field
type in order to look up a resolver, and `getFinalCastedValue` and `resolutionValue` become
a single path instead of two that both cast.

**Why this priority**: The payoff, and the step that makes the migration irreversible. It is
last because it depends on every consumer already having moved, and because a half-migrated
codebase cannot lose the compatibility alias.

**Independent Test**: Search the workspace for the deprecated shape and find no declaration;
exercise the transformation path for every field type through its unit tests; edit and save
content of each field type in a running instance.

**Acceptance Scenarios**:

1. **Given** the codebase after this story, **When** a developer searches for the flat
   content-type-field shape, **Then** it does not exist.
2. **Given** the form building a control for a field, **When** it selects the transformation
   for that field, **Then** it does so without asserting the field's type.
3. **Given** a value round-tripping from stored content to form control and back, **When**
   it is traced, **Then** it passes through one transformation path, not two.
4. **Given** issue #31911, **When** this work lands, **Then** the issue is explicitly
   resolved — reopened and closed against this work, or closed as superseded by #37670 with
   a comment saying so — rather than left closed-as-completed over work that had not
   happened.

---

### Edge Cases

- **A field arrives with a `fieldType` the union does not contain.** The backend can send a
  field type the frontend model has not been taught, including from a customer plugin. The
  union makes such a value unassignable, so the boundary where backend data enters the model
  must have a defined behavior — the field is handled as an unknown kind and the form
  degrades visibly rather than the application failing to render the content type. This is
  the single highest-risk behavioral edge in the feature.
- **Test fixtures built as partial literals.** Many specs construct a field as a partial
  object and assert it into place. Under the union a partial is not assignable to any member,
  so each such fixture must move to a shared factory. The risk is a fixture being "fixed" by
  a fresh assertion, which would reintroduce exactly what the feature removes.
- **A field type in the union that no resolver handles.** Going from 24 to 28 field types
  means any exhaustive per-type mapping gains four layout entries it never had. Each must get
  a deliberate behavior, not a copied placeholder.
- **Layout dividers.** A layout row's divider and a column's divider are structurally
  specific field kinds; typing them as the full union keeps a distinction the model could
  now express. Narrowing them may surface consumers that were relying on the loose type.
- **Content saved before the migration.** Stored content is untouched by this work, but the
  transformation path is not: a value that previously flowed through `getFinalCastedValue`
  and a value that flowed through `resolutionValue` must produce the same form control after
  unification as before it.
- **`main` moving under a long-lived branch.** The feature spans several pull requests over
  time; new code merged in the meantime will use the deprecated flat shape. Each story must
  re-count its consumers rather than trusting the numbers in this document.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The content-type-field type published by the content model library MUST be a
  discriminated union of the 28 existing per-type interfaces, discriminated on the field's
  type property, such that narrowing on that property yields the per-type interface with its
  required properties.
- **FR-002**: The 28 per-type interfaces MUST stop being orphans — after this feature, the
  per-type interfaces are what production code consumes, not only what the test-mock file
  imports.
- **FR-003**: The flat legacy interface MUST NOT survive the feature, and MUST NOT be kept as
  a deprecated compatibility alias either. Single-PR delivery (FR-017) removes the reason an
  alias existed — there is no window during which some consumers have migrated and others have
  not — so the interface is replaced outright, leaving exactly one content-type-field type.
- **FR-004**: Every file that consumes the content-type-field type MUST compile under the
  union. The count is to be re-measured at the start of each story rather than taken from
  this document; at branch point `85f6976346` it was 149 files (97 production, 52 spec).
- **FR-005**: The workspace MUST have a single field-type vocabulary. The duplicate enum and
  companion constant object in `libs/edit-content` are removed and their consumers use the
  one published by the content model library, which carries four additional layout types the
  local enum lacked.
- **FR-006**: The assertions of the form `fieldType as …` / `clazz as …` in
  `libs/edit-content/src` (16 in production at branch point) MUST be removed and replaced by
  narrowing the discriminated union. No assertion may be replaced by a double assertion, by
  an escape to `any`, or by a compiler-suppression comment; these are failures of the
  requirement, not alternative ways to satisfy it.
- **FR-007**: Per-field-type behavior mappings MUST be narrowed **per entry**, so that each
  entry's handler is checked against the specific field type it is registered under. A single
  union-wide handler signature does not satisfy this requirement: `libs/edit-content` compiles
  with `strict: false`, under which such a signature is accepted without being verified, which
  would make the narrowing decorative. Soundness must not depend on a strictness flag.
- **FR-008**: Any mapping that must cover every field type MUST be exhaustive by
  construction — adding a field type to the shared vocabulary breaks compilation until the
  new type is handled.
- **FR-009**: The transformation of a stored content value into its form representation MUST
  go through one path. `getFinalCastedValue` and `resolutionValue` are reconciled rather than
  left as two paths that both assert, closing what issue #31911 asked for.
- **FR-010**: Each transformation function MUST declare the narrowed field interface it
  actually handles, rather than the full union plus an assertion.
- **FR-011**: The form MUST select a field's transformation without asserting the field's
  type.
- **FR-012**: Transformation behavior MUST be preserved. `main`'s current resolution logic is
  a superset of the version on the abandoned branch — it carries query-parameter and
  manual-translation arguments, a generic return type, and dedicated key-value, block-editor
  and text resolvers that the older file does not have. The abandoned file is a design
  reference for how to narrow; it MUST NOT be restored over the current implementation, which
  would delete shipped behavior.
- **FR-013**: The behavior when a field arrives with a type outside the union MUST be defined
  and tested at the boundary where backend data enters the model. An unrecognized field type
  degrades visibly — the rest of the content type still renders — and does not throw.
- **FR-014**: Test fixtures MUST use the shared `createFake*Field` factories rather than
  partial literals. Factories are reused, not duplicated; any new factory lands beside the
  existing ones in the shared test-mock file.
- **FR-015**: Spec migration MUST NOT reduce coverage. A spec that becomes hard to type is
  rewritten to the narrowed type, not deleted, disabled, or loosened with an assertion.
- **FR-016**: Issue #31911 MUST be explicitly resolved against this work — reopened and closed
  against it, or closed as superseded by #37670 with a comment recording that its stated work
  had not been done when it was marked completed.
- **FR-017**: The feature is delivered as a **single pull request**. This reverses the staged
  sequence an earlier draft of this specification required, and the reversal is evidence-based:
  the staging existed to break up an estimated 149 consuming files, but measurement (research
  R-01) shows the union breaks **19** — the other 130 only pass a field through and never read
  a per-type property. A 19-file type change plus the vocabulary collapse is reviewable in one
  diff, and a single PR keeps `main` from ever carrying two content-type-field types.
  **The residual risk is unchanged and is not a diff-size problem**: PR #31964 drew no
  technical objection and was closed by the stale bot after 30 days without review. A reviewer
  must be agreed before the PR is opened.

### Key Entities

- **Content type field**: One field on a content type, as the frontend models it. Carries a
  discriminating type, a data type, a class, and identity and display attributes. Today one
  flat shape for all kinds; after this feature, a union whose member is determined by the
  discriminating type.
- **Field type vocabulary**: The closed set of field-type values the product understands.
  Currently declared three times — twice in the edit-content library, once in the content
  model library — with the edit-content copies missing the four layout types. Reduced to one.
- **Per-type field interface**: One of the 28 shapes, each pinning the discriminating type,
  data type and class to a single literal value and declaring the properties that only that
  kind of field has. Already merged; currently unused by production code.
- **Field transformation**: The mapping from a stored content value to the value a form
  control holds, and back. Currently two implementations that both assert the field type;
  becomes one, per-type-narrowed path.
- **Layout structure**: Tabs, rows and columns, whose dividers are themselves fields of
  specific layout kinds. Currently typed as any field.
- **Field test factory**: The shared helpers that build a valid field of a given kind for
  tests. Already exist for roughly nine types; the migration extends the set.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Exactly one content-type-field type exists in the codebase at the end of the
  feature, and it is the discriminated union; a search for the flat shape returns no
  declaration.
- **SC-002**: All 28 per-type interfaces are reachable by narrowing from production code —
  zero remain referenced only by the test-mock file.
- **SC-003**: Zero field-type or class assertions remain in `libs/edit-content/src`, down
  from 16 in production at branch point.
- **SC-004**: Exactly one field-type vocabulary is declared in the workspace, down from three.
- **SC-005**: Type-checking the workspace produces no new errors, and the change introduces
  no new compiler-suppression comments and no new escapes to `any` relative to the branch
  point.
- **SC-006**: Lint and unit tests pass for every affected area — edit-content, dotcms-ui, ui,
  portlets, web components, block editors, content model, data access, utils and test
  utilities — with coverage no lower than at branch point.
- **SC-007**: A value stored for any field type produces the same form control after the
  change as before it, verified per field type by test.
- **SC-008**: Content of every affected field type can be edited and saved in a running
  instance with no behavioral difference — the manual check that unit tests cannot replace.
- **SC-009**: A field arriving with an unrecognized type leaves the rest of the content type
  editable, and the behavior is covered by a test.
- **SC-010**: The pull request leaves `main` type-checking and green on its own, with no
  follow-up required to compile — one atomic change, no transitional state in which two
  content-type-field types coexist.
- **SC-011**: Adding a field type to the shared vocabulary without handling it breaks
  compilation, demonstrated once.
- **SC-012**: Issue #31911 is in a state that reflects reality — either closed against this
  work or explicitly superseded, with the discrepancy recorded.

---

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The content edit form and the content type editor — the
  screens where an editor fills in a content type's fields and where an administrator defines
  them. Also the older web-component and block-editor field surfaces, which are part of the
  legacy frontend product surface and are migrated because they consume the same model type,
  not because they are being modernized. No backend code, no REST contract, no database
  schema, and no stored content is touched; this is a compile-time change to how the frontend
  describes data it already receives unchanged.

- **Backward-compatibility expectations**: Total behavioral compatibility is required. Every
  field type must render, validate, transform and save exactly as it does today; the field
  type values themselves are byte-identical strings across the vocabularies being merged, so
  no runtime comparison changes result. Existing content requires no migration. The one
  deliberate deprecation is internal to the frontend codebase: the flat content-type-field
  interface is deprecated and then deleted within this feature. Because it is exported from
  the content model library, any consumer outside this repository that imports it would break
  at the final story — whether that surface is published and whether a longer deprecation
  window is owed is a question for the plan phase, not an assumption to make here.

- **Known related decisions**: Issue #31911 asked for this unification and is recorded as
  completed although it was not done; #31964 attempted it and was closed unreviewed by the
  stale bot; #33056 landed the type layer this feature completes. The constitution's
  legacy-awareness principle applies directly and constrains the work: touching 149 files is
  an invitation to rewrite things, and this feature does not take it. Progressive enhancement
  only — each file is brought onto the union and left otherwise as it is. The plan phase will
  formally consult `dotCMS/platform-adrs` for decisions bearing on the frontend content model
  and on deprecating an exported type.

---

## Assumptions

- **The delivery shape is staged, and that was a decision, not a default.** The issue asks
  for one pull request while documenting that the same shape killed #31964. The staged
  sequence — union plus deprecated alias, then the vocabulary merge, then edit-content, then
  the remaining consumers, then alias removal plus transformation unification — is the
  mitigation the issue itself suggests, chosen by the developer. The cost is accepted
  knowingly: `main` carries two content-type-field types for the duration, which this feature
  ends rather than leaves behind.
- **The duplicated field-type vocabulary is in scope**, chosen by the developer, although
  issue #37670 does not mention it. Without it the issue's "remove the casts" criterion is
  unreachable, since the union discriminates on one vocabulary while `edit-content` compares
  against another.
- **The abandoned branch is a design reference, not a source to restore.** Its
  `resolution-values.utils.ts` is roughly fourteen months behind `main`'s equivalent and
  lacks behavior that has shipped since. What is taken from it is the narrowing pattern.
- **Type-level guarantees must hold independently of the strictness flag.** `libs/edit-content`
  compiles with `strict: false`; the design must not rely on checks that flag disables.
  `libs/dotcms-models` is already `strict: true`.
- **Runtime behavior is unchanged by construction**, because the field-type string values are
  identical across the vocabularies being merged. This is an assumption the tests must
  confirm per field type, not one to rely on silently.
- **Test factories are the migration mechanism for specs.** Roughly nine `createFake*Field`
  factories exist; the remainder are added beside them as each area migrates.
- **No backend, REST or database change is required.** If one turns out to be needed, it is
  outside this feature and gets its own issue.
- **The numbers in this document are a snapshot** taken at `85f6976346` and will drift as
  `main` moves. Each story re-measures its own surface; the counts here establish the shape of
  the work, not its contract.
