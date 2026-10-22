# Issue Resolution Specification: File field doesn't export as a header in CSV

**Feature Branch**: `20261021-084222-file-field-missing`

**Created**: 2026-10-21

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#33944](https://github.com/dotCMS/core/issues/33944) (Support ticket: Freshdesk 34234)

**Input**: User description: "File field doesn't export as a header in CSV" (issue #33944)

## Problem Statement *(mandatory)*

When a content type has a **File** field, exporting its contents to CSV leaves that field out
entirely: there is no column header for it and no value in any row. The same content then
cannot be re-imported when the File field is required, because the import rejects every row
with `Field: "file" is required and was not found in the header`. The export therefore loses
data and breaks the export-then-import round trip for any content type that uses a File field.

**Severity / Impact**: Customer-reported (support ticket). Priority 3 – Average. Affects every
content type with a File field; data is silently omitted from exports, and required File
fields make re-import impossible without hand-editing the CSV.

## Reproduction *(mandatory)*

**Environment**: Latest / Evergreen dotCMS, any site, default configuration.

**Steps to Reproduce**:

1. Create a content type with a File field (required or not) and a Text field.
2. Create several contentlets of that type, with a file selected in the File field.
3. Export the contents of that content type to CSV (Content portlet → export).
4. Open the CSV and look at the header row.

**Expected Behavior**: The CSV contains a column for the File field, populated with the
selected file's value, like every other exported field.

**Actual Behavior**: The File field has no column. Importing that CSV into a content type
whose File field is required fails every row with
`Field: "file" is required and was not found in the header`.

**Reproducibility**: Always, for any content type containing a File field.

## Scope of Investigation *(mandatory)*

- **Affected area**: Content export to CSV (Content portlet export action), and its
  interaction with Content import.
- **Suspected surface**: Both. `FileField` is a modern type (`com.dotcms.contenttype.model.field`),
  but the export routine lives in legacy code (`com.dotmarketing.portlets.contentlet.action.EditContentletAction`,
  `downloadToExcel`) and the `isNewFieldTypeAllowedOnImportExport` gate is in
  `com.dotmarketing.portlets.contentlet.util.ContentletUtil`. Import is in legacy
  `com.dotmarketing.util.ImportUtil`. Confirmed during planning.
- **Related known decisions**: The `@Unexportable` annotation (`com.dotcms.business.Unexportable`)
  is the explicit mechanism that excludes a field from CSV export. The plan formally consults
  `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

`FileField` (`dotCMS/src/main/java/com/dotcms/contenttype/model/field/FileField.java:20`) is
annotated `@Unexportable`. `ContentletUtil.isNewFieldTypeAllowedOnImportExport` returns false
for it, so `EditContentletAction` skips it when printing the header row (~line 1918) and again
when printing each data row (~line 1941). Even without the annotation, the value-formatting
chain in the export (~lines 1990–1999) has branches for `ImageField` and `BinaryField` but
none for `FileField`, so the raw stored value would be written instead of a usable reference.

Findings from the investigation already done:

- **Import already supports File fields.** `ImportUtil` has `isFileField`, `validateFileField`
  and `fetchAndSetFileField`: it accepts a dotCMS path (resolved to an identifier) or an
  external URL (fetched into a File Asset), and otherwise stores the value as given. So the
  gap is on the export side only, and an exported path round-trips through import.
- **Why `@Unexportable` was applied is not recoverable from git.** `FileField.java` appears as a
  newly added file in a large squashed commit (`94d01becbd`), so history gives no rationale.
  The annotation's own Javadoc says only that annotated fields are omitted from exports.

[NEEDS CLARIFICATION: Was `@Unexportable` on `FileField` a deliberate product decision (for
example, File fields being considered unsupported in import/export when introduced) or an
oversight? Import support suggests oversight, but a product owner should confirm.]

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Include File fields in the CSV export: a header column named after the field variable, and
  a value in each row.
- Export the value in the same form the existing Image field uses (the file's dotCMS path),
  which import already accepts.
- Handle an empty or unresolvable File field value without failing the export (empty cell,
  consistent with Image field behavior).

**Explicitly out of scope / non-goals**:

- Changing how Image, Binary or any other field type is exported.
- Changing the import logic for File fields (already supported), unless the round-trip
  verification shows a defect.
- Removing or redesigning the `@Unexportable` mechanism; the other annotated types
  (Row, Column, Line Divider, Tab Divider) keep their behavior.
- Exporting file *contents* or binaries; only the reference is exported.
- Rewriting the legacy export routine beyond the minimal change and progressive enhancement.

## Regression Risk *(mandatory)*

- **Blast radius**: Every CSV export now has one more column for content types with File
  fields. Same code path as all other field exports (`downloadToExcel`); a defect in the new
  branch could break export for content types with other fields too, so a failure in
  resolving one File value must not abort the whole export.
- **Backward compatibility**: Consumers of exported CSVs (external scripts, spreadsheets)
  will see a new column. Position follows content-type field order, so existing columns keep
  their relative order. No API, REST contract, DB schema or ES mapping change, so no
  rollback-unsafe category applies. Previously exported CSVs (without the column) still
  import, except where the File field is required (the current bug).
- **Data considerations**: No data migration. Existing content is unaffected.

## Acceptance & Verification *(mandatory)*

- **AC-001**: Exporting a content type with a File field to CSV produces a header column for
  the field (named by its variable) and, for each contentlet with a file set, the file's
  dotCMS path; for contentlets with no file the cell is empty. The reproduction steps no
  longer produce the missing-column behavior.
- **AC-002**: Re-importing that exported CSV into a content type whose File field is required
  succeeds without the "required and was not found in the header" error, and the imported
  contentlets reference the same files.
- **AC-003**: Regression: export of Image, Binary, Text and other field types, and of content
  types with unexportable layout fields (Row, Column, dividers), is unchanged. A File field
  whose referenced file was deleted does not break the export of the remaining rows.
- **Verification method**: Integration tests (to be written first and confirmed failing before
  implementation, per Principle V): export header and value for a File field; empty and dangling
  File values; export-then-import round trip. Registered in the appropriate `MainSuite*` per
  `docs/testing/INTEGRATION_TESTS.md`. Run with
  `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dmaven.build.cache.enabled=false -Dit.test=<Class>`
  and confirm `Tests run: N` in `target/failsafe-reports`. Manual check via the Content
  portlet export as a final sanity step.

## Assumptions

- The exported value for a File field is the file's dotCMS path (same as Image field), because
  import already resolves paths to identifiers; an identifier or URL form is not needed.
- The CSV column name is the field variable, matching all other exported fields and the
  header lookup the import performs.
- The `@Unexportable` annotation on `FileField` is removed or bypassed for File fields only;
  the choice is a planning decision.
