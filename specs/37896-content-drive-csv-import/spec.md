# Feature Specification: Import content from a CSV in Content Drive

**Feature Branch**: `issue-37896-content-drive-csv-import`

**Created**: 2026-10-12

**Status**: Draft

**Type**: New Feature

**Input**: GitHub issue dotCMS/core#37896 (epic #37928), adjusted by the developer's description of
the flow. Content Drive has no way to import content from a CSV, so users still go to the legacy
Content Search import. This feature adds an "Import CSV" option to Content Drive's Upload control.
It opens a dialog with the same capabilities as Content Search's import (content type, language,
workflow action, key fields, CSV file, sample CSV download), a preview that shows what will be
created or updated plus every warning and error in Content Drive's own UI, and an import the user
waits for inside the dialog. Design is not available yet.

## Clarifications

### Session 2026-10-12

The developer's description of the flow differs from the issue in four places. The developer's
version is the one this spec follows:

- **Non-indexed fields** are not listed as key fields at all. The issue (and legacy) list them
  disabled.
- **The import is synchronous for the user (proposed, open to review).** The user waits in the
  dialog until the import finishes, then chooses to close or import more. The issue had the import
  keep running after the dialog closes, with a toast at the end. See "Open for review: synchronous
  or asynchronous import" below.
- **The preview is shown with Content Drive's UI**, not as the plain text lists Content Search
  shows (summary, warnings, errors).
- **Failures are shown in the dialog**, and the user can fix the form or the file and try again.

Other decisions:

- **Content type selector**: the Drive's existing content type filter (base types plus the
  content types under each) is reused if it can be adapted to single selection. If that gets
  complicated, a plain single-select dropdown is used instead.
- Q: Can the user import when the preview has errors? → A: Yes, exactly as Content Search: rows
  with errors are skipped, after a warning and confirmation.
- Q: Which columns does the sample CSV have? → A: The same as legacy: every importable field of the
  content type, regardless of the selected key fields, plus `languageCode` and `countryCode` for a
  multilingual file.
- **Workflow actions**: the existing endpoint Content Search's import already uses,
  `GET /api/v1/workflow/initialactions/contenttype/{id}`, is enough. It returns the actions the user
  may run on new content of that type. No new endpoint is created.

### Open for review: synchronous or asynchronous import

The server always runs the import as a background job. The question is what the user sees while it
runs.

- **Proposed: synchronous.** The dialog waits for the job to finish and cannot be closed while it
  runs. The final result (created, updated and failed counts, plus every row error) is shown in the
  dialog, where the user can read it, go back to the form and retry. This is what User Story 1 and
  FR-016 describe.
- **Alternative: asynchronous.** The user may close the dialog and keep working while the import
  runs, and is told when it ends. This frees the user on large imports, but it needs a decision this
  spec does not cover: **where and how the final result is shown when the import had errors**,
  once the dialog is gone. A toast can carry the counts but not a list of row errors. Options
  include reopening the result in the dialog from the toast, or a place in the Drive that lists
  recent imports and their results. None of these is designed yet.

Reviewers are welcome to propose a way to show the final result asynchronously. Until then, this
spec follows the synchronous proposal.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Import a CSV from Content Drive (Priority: P1)

An editor has a spreadsheet of blog posts. In Content Drive they open Upload, choose "Import CSV",
pick Blog, a language and a workflow action, choose key fields, attach the CSV, preview it, and
import it. They wait in the dialog until it finishes, see how many items were created and updated,
and close the dialog. The Drive listing shows the new content.

**Why this priority**: This is the feature. It removes the last reason to go back to Content Search
for bulk loading.

**Independent Test**: In Content Drive, import a valid Blog CSV with two new rows. The dialog
reports two created, and the listing shows both blogs.

**Acceptance Scenarios**:

1. **Given** the user can upload in the current folder, **When** they open Upload, **Then** an
   "Import CSV" option is offered, including in folders that pin a default file type.
2. **Given** Upload is disabled (the user can't add children in the current folder), **When** they
   look at Upload, **Then** "Import CSV" is not available either.
3. **Given** the user chose "Import CSV", **When** the dialog opens, **Then** it shows Content Type,
   Language, Workflow Action, Key Fields, a CSV file picker, and a link to download a sample CSV.
4. **Given** a complete form and a valid file, **When** the user previews and then imports,
   **Then** the dialog shows the import running and cannot be closed until it finishes.
5. **Given** the import finished, **When** the dialog shows the result, **Then** it reports how
   many rows were created, updated and failed, and offers "Close" and "Import more".
6. **Given** the result is shown, **When** the user picks "Import more", **Then** the dialog
   returns to the form, ready for another import.
7. **Given** the import finished, **When** the user closes the dialog, **Then** the Drive listing
   refreshes and shows any new content in the current folder.
8. **Given** the user drops a `.csv` file onto the Drive, **When** the drop completes, **Then** it
   is uploaded as a file exactly as today, with no import.

---

### User Story 2 - The form only offers valid choices (Priority: P1)

The form only lists what the user can actually import with: content types they can write to, the
workflow actions they may run on new content of that type, and the fields that can serve as keys.

**Why this priority**: An option that can never work leads to a failed import after the user has
already uploaded a file. Legacy filters these lists, and the Drive must not offer more.

**Independent Test**: As a limited user, open the dialog: only content types they can write appear.
Pick one: only its permitted workflow actions and its indexed, importable fields appear.

**Acceptance Scenarios**:

1. **Given** the dialog is open, **When** the user opens Content Type, **Then** it lists only the
   content types they have write permission on, never Host, and lets them pick exactly one.
2. **Given** a content type is picked, **When** the user opens Workflow Action, **Then** it lists
   only the actions Content Search's import offers for that type and user, labelled with the action
   and its workflow scheme.
3. **Given** a content type is picked, **When** the user looks at Key Fields, **Then** only that
   type's indexed fields are listed; non-indexed fields, relationship fields, file and image fields,
   and layout fields are not shown.
4. **Given** the user changes the content type, **When** the form updates, **Then** the workflow
   action and key fields are reset to match the new type.
5. **Given** the dialog is open, **When** the user opens Language, **Then** it lists every language
   plus a "Multilingual file" option, and defaults to the default language.
6. **Given** the user picks "Multilingual file", **When** the form updates, **Then** it explains
   that each row needs `languageCode` and `countryCode` columns and that at least one key field is
   required.
7. **Given** any required input (content type, language, workflow action, file) is missing, or
   Multilingual is picked without a key field, **When** the user looks at Preview, **Then** it is
   disabled.
8. **Given** the user picks a file that doesn't end in `.csv`, **When** it is selected, **Then** the
   form shows an inline error and no preview starts.
9. **Given** the form, **When** the user reads it, **Then** it states that each row is placed by
   the CSV's site/folder column, and rows without one go to the site root, not the folder the Drive
   is showing.

---

### User Story 3 - Preview shows clearly what will happen (Priority: P1)

Before anything is written, the user previews the import and sees, in a readable layout, a summary
(rows read, headers matched, key fields, how many items would be created and updated) and every
warning and error, each with its line number, field and value when known.

**Why this priority**: The preview is how users catch mistakes before writing hundreds of items.
Content Search shows it as plain text, which is hard to scan.

**Independent Test**: Preview a CSV with one bad date on line 3 and an unknown column. The summary
shows the counts, the error names line 3 and the date field, and the warning names the unknown
column. Nothing is created.

**Acceptance Scenarios**:

1. **Given** a complete form, **When** the user clicks Preview, **Then** the dialog shows a loading
   state until the preview is ready, and no content is created.
2. **Given** the preview is ready, **When** the user reads it, **Then** it shows a summary with
   rows read, headers matched, key fields used, items to create and items to update.
3. **Given** the preview has warnings or errors, **When** the user reads them, **Then** warnings and
   errors are shown as separate, clearly distinguished groups with their counts, and each entry
   shows its line number, field and offending value when the result includes them.
4. **Given** the preview has no warnings or no errors, **When** the user reads it, **Then** the
   matching group says there are none.
5. **Given** the preview is ready, **When** the user changes any form input or the file, **Then**
   the preview is discarded and the user must preview again before importing.
6. **Given** the preview is ready, **When** the user cancels it, **Then** they return to the form
   with their inputs kept, and no content is created.
7. **Given** the preview has errors, **When** the user clicks Import, **Then** the import is
   allowed, as in Content Search: the dialog warns that rows with errors will be skipped, and the
   user confirms before it starts.
8. **Given** the preview has no errors, **When** the user clicks Import, **Then** the import starts
   after the same confirmation Content Search asks for.

---

### User Story 4 - Download a sample CSV (Priority: P2)

Users who don't know the expected columns download a sample CSV for the selected content type,
fill it in and import it.

**Why this priority**: It saves guessing column names, but users who already have a file don't need
it.

**Independent Test**: Pick Blog and click the sample link. A CSV downloads whose header row lists
Blog's importable fields and whose second row shows example values.

**Acceptance Scenarios**:

1. **Given** no content type is picked, **When** the user looks at the sample link, **Then** it is
   disabled.
2. **Given** a content type is picked, **When** the user clicks the sample link, **Then** a CSV
   downloads with one column per importable field of the content type, whatever key fields are
   selected (as Content Search produces it), and a second row of placeholder values that show each field's expected format (for example a
   date pattern for date fields).
3. **Given** "Multilingual file" is picked, **When** the user downloads the sample, **Then** it also
   has `languageCode` and `countryCode` columns.
4. **Given** a user who can't read the content type, **When** they request its sample, **Then** the
   request is refused.

---

### User Story 5 - Failures are shown in the dialog and the user can retry (Priority: P2)

When the preview or import can't run (the content type or language no longer exists, the file is
empty or unreadable, the server fails), the dialog shows what went wrong and keeps the user's
inputs, so they can fix the form or the file and try again.

**Why this priority**: Without it a failure leaves the user with a blank dialog or a stray toast
and no way to tell what to fix.

**Independent Test**: Preview with an empty CSV. The dialog shows an error explaining the file has
no data rows, the form is still filled in, and replacing the file lets the user preview again.

**Acceptance Scenarios**:

1. **Given** the preview fails, **When** the dialog shows the failure, **Then** it shows the
   server's error message and the form stays editable with the user's inputs kept.
2. **Given** the import fails part way or entirely, **When** it ends, **Then** the dialog shows the
   result (what was created and updated before the failure, and the errors), and lets the user go
   back to the form to try again.
3. **Given** a failure was shown, **When** the user changes inputs or replaces the file and
   previews again, **Then** the new preview runs normally.

---

### Edge Cases

- **Closing during the import.** The dialog cannot be closed while the import runs. If the user
  reloads or leaves the page anyway, the server finishes the import; the user sees its results in
  the content, not in the dialog.
- **Closing during the preview.** Allowed; nothing is created.
- **Drive site differs from the session site.** The import writes into the site the Drive is
  browsing, not another site the server remembers for the session. This needs checking in the plan.
- **Rows with a folder that doesn't exist.** They land at the site root with no error, as in
  legacy. Tracked separately, not fixed here.
- **Very large files.** No new size limit; the server's existing upload limit applies. The dialog
  keeps showing that the import is running for as long as it takes.
- **Per-row action column.** A CSV may name a workflow action per row; legacy honors it over the
  form's action, and so does this import.
- **Identifier column.** A CSV with an `Identifier` column updates the matching content, as in
  legacy.
- **Permissions per row.** Rows the user can't create in their target folder, or can't run the
  action on, show up as row errors in the preview and the result.
- **Encoding.** Files are read as UTF-8.

## Requirements *(mandatory)*

### Terms

- **Preview**: a dry run that reads the whole CSV with the chosen options and reports what would
  happen, without writing anything.
- **Key fields**: fields used to find existing content to update instead of creating new content.
- **Multilingual file**: a CSV where each row carries its own language in `languageCode` and
  `countryCode` columns.

### Functional Requirements

- **FR-001**: Content Drive's Upload control MUST offer an "Import CSV" option in every folder where
  Upload is available, including folders that pin a default file type, and MUST NOT offer it where
  Upload is disabled.
- **FR-002**: "Import CSV" MUST open a dialog containing Content Type, Language, Workflow Action,
  Key Fields, a CSV file picker and a sample CSV link.
- **FR-003**: Content Type MUST be a single selection listing only the content types the user has
  write permission on, excluding Host. The Drive's existing content type filter SHOULD be reused,
  adapted to single selection; if that proves too complex, a plain single-select dropdown MUST be
  used instead.
- **FR-004**: Language MUST list every language plus a "Multilingual file" option, default to the
  default language, and always send the chosen language to the import (never leave it empty).
- **FR-005**: Workflow Action MUST list exactly the actions Content Search's import offers for the
  selected content type and user, and MUST be required.
- **FR-006**: Key Fields MUST list only the selected type's indexed fields that can be used as keys
  (no relationship, file, image or layout fields). Non-indexed fields MUST NOT be shown.
- **FR-007**: Changing the content type MUST reset the workflow action and key fields.
- **FR-008**: Preview MUST stay disabled until content type, language, workflow action and a `.csv`
  file are set, and, for a multilingual file, at least one key field.
- **FR-009**: Picking a file that doesn't end in `.csv` MUST show an inline error.
- **FR-010**: The dialog MUST tell the user that rows are placed by the CSV's site/folder column and
  rows without one go to the site root.
- **FR-011**: Preview MUST run the import without writing anything and show a summary (rows read,
  headers matched, key fields, items to create, items to update), then warnings and errors as
  separate groups with counts. Each entry MUST show its line number, field and value when known.
- **FR-012**: The preview MUST be shown with the Drive's own visual components, not as plain text,
  and all its text MUST be translatable.
- **FR-013**: Any change to the form or the file after a preview MUST discard it and require a new
  preview before importing.
- **FR-014**: Import MUST use exactly the file and options of the last preview.
- **FR-015**: Import MUST be allowed whether or not the preview has errors, as in Content Search.
  When it has errors, the dialog MUST warn that rows with errors will be skipped and ask the user to
  confirm.
- **FR-016**: While the import runs, the dialog MUST show that it is running and MUST NOT be
  closable. When it ends, the dialog MUST show created, updated and failed counts and any errors,
  and offer "Close" and "Import more".
- **FR-017**: "Import more" MUST return to an empty form. "Close" MUST close the dialog and refresh
  the Drive listing.
- **FR-018**: When the preview or import fails, the dialog MUST show the error and keep the form
  editable with the user's inputs, so the user can retry.
- **FR-019**: A new server endpoint MUST return a sample CSV for a content type, as described in
  User Story 4, and MUST refuse users who can't read that content type. It replaces the
  sample download that today exists only inside Content Search.
- **FR-020**: The import endpoint's documentation MUST state that leaving `language` out, or sending
  `-1`, means a multilingual file, and the generated API description MUST be updated.
- **FR-021**: Dropping a `.csv` onto the Drive MUST keep uploading it as a file, as today.
- **FR-022**: All new visible text MUST be translatable, reusing Content Search's existing
  translation keys where the meaning is the same.

### Key Entities

- **Import request**: content type, language (or multilingual), workflow action, key fields, CSV
  file.
- **Import result**: summary counts (rows read, to create, to update, created, updated, failed),
  and lists of info, warning and error messages, each with line number, field and value when known.
- **Sample CSV**: a header row of the content type's importable fields (plus language columns for a
  multilingual file) and one row of format placeholders.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can import a CSV into any content type they can write to without leaving
  Content Drive.
- **SC-002**: For the same CSV and options, the preview reports the same counts, warnings and errors
  as Content Search's preview.
- **SC-003**: A user can find any row error in the preview by line number without reading the full
  list.
- **SC-004**: Every failed preview or import leaves the dialog with an error message and the user's
  inputs intact; no failure ends with a blank or stuck dialog.
- **SC-005**: A sample CSV filled in by a user and imported with the same options produces no
  header errors.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: Content Search's import (the legacy Struts flow and its JSP
  screens) is the reference. It is not changed or removed. The server's REST import, which already
  powers both validation and import, is reused; a sample CSV endpoint is added next to it, moving
  out of the legacy action the logic that only exists there today.
- **Backward-compatibility expectations**: Content Search's import keeps working unchanged. The
  REST import's behavior doesn't change; only its documentation of `language` is corrected. Drag and
  drop and file upload in the Drive keep working as today.
- **Known related decisions**: Import is one half of the Drive's import/export epic (#37928); the
  export (#37900) writes CSVs this import must read back. The plan phase will consult
  `dotCMS/platform-adrs`.

## Assumptions

- The server import runs as a background job; "synchronous" here means the user waits in the
  dialog for that job to finish (see "Open for review: synchronous or asynchronous import"). How the
  dialog waits is a plan decision.
- The import writes into the site the Drive is browsing.
- Language stays in the form even though the developer's description didn't mention it, because the
  import always needs one, and leaving it out silently turns on multilingual mode.
- The sample CSV's placeholder values follow Content Search's (date patterns, category key,
  site/folder identifier, and `XXX` for other fields), without its trailing-comma bug.
- Out of scope: importing by drag and drop, targeting the current folder, fixing rows whose folder
  doesn't exist, checking who owns an import job on the job endpoints (to be filed separately), and
  export (#37900).
