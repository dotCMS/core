# Feature Specification: Import content from a CSV in Content Drive

**Feature Branch**: `issue-37896-content-drive-csv-import`

**Created**: 2026-10-12

**Status**: Draft

**Type**: New Feature

**Input**: GitHub issue dotCMS/core#37896 (epic #37928), adjusted by the developer's description of
the flow and by the review of PR #37995. Content Drive has no way to import content from a CSV, so
users still go to the legacy Content Search import. This feature adds an "Import CSV" option to
Content Drive's Upload control. It opens a dialog with the same capabilities as Content Search's
import (content type, language, workflow action, key fields, CSV file, sample CSV download) and a
preview that shows what will be created or updated, where the rows will land, and every warning and
error, in Content Drive's own UI. The import then runs in the background: the Drive's status toast
shows it running and, when it ends, a summary of what was created, updated and failed. Design is not
available yet.

## Clarifications

### Session 2026-10-12

The developer's description of the flow differs from the issue in these places. The developer's
version is the one this spec follows:

- **Non-indexed fields** are not listed as key fields at all. The issue (and legacy) list them
  disabled.
- **The preview is shown with Content Drive's UI**, not as the plain text lists Content Search
  shows (summary, warnings, errors).
- **Failures are shown to the user with what went wrong**, and the user can fix the form or the
  file and try again.

Other decisions:

- **Content type selector**: the Drive's existing content type filter (base types plus the
  content types under each) is reused if it can be adapted to single selection. If that gets
  complicated, a plain single-select dropdown is used instead.
- Q: Can the user import when the preview has errors? → A: Yes, exactly as Content Search: rows
  with errors are skipped, after a warning and confirmation.
- Q: Which columns does the sample CSV have? → A: The same as legacy. The new endpoint returns the
  file Content Search produces for that content type.
- **Workflow actions**: the existing endpoint Content Search's import already uses,
  `GET /api/v1/workflow/initialactions/contenttype/{id}`, is enough. It returns the actions the user
  may run on new content of that type. No new endpoint is created.

### Session 2026-10-12 (PR #37995 review)

- Q: Should the import be synchronous (the user waits in the dialog) or asynchronous? → A:
  **Asynchronous.** After the user confirms, the dialog closes and the import keeps running in the
  Drive's status toast, the one upload and folder copy already use. Like those, it is restored after
  a reload or after leaving the Drive and coming back.
- Q: Which site does the import use? → A: **The site the Drive is browsing, sent with the
  request.** The import endpoint currently takes it from the server session, which two browser tabs
  on different sites share. The endpoint gains an optional site parameter, and the Drive always
  sends it. Callers that don't send it keep today's behavior. (Narrowed during planning: see the
  next session.)
- **Drag-and-drop import stays out of scope**, as the issue states: its "Out of scope" section
  lists "Drag and drop import", and one of its acceptance criteria says dropping a `.csv` uploads it
  as a file with no import prompt. The reason given there still holds: each row lands where its CSV
  says, not in the folder the file was dropped on, which would be confusing.
- The preview summary shows **where the rows will land**: how many rows go to each site/folder.
  (Corrected during planning: see the next session.)
- **Language is prefilled with the language the user is working in** in the Drive, not the system
  default language.
- Warnings and errors in the preview are listed **in line-number order**, so a row's problems can be
  found by its line.
- The sample CSV endpoint is a **REST equivalent** of Content Search's sample download. Content
  Search keeps its own download unchanged.

### Session 2026-10-12 (PR #37995 review, second round)

- **No progress, no Cancel, no full result after the import.** The import's progress can't be
  tracked, so the status toast shows an indefinite loading state while it runs, as it does for
  upload and folder copy. Cancel is dropped; it needs a better design first. When the import ends,
  the toast shows only a summary (created, updated and failed counts), like upload and folder copy.
  The "View results" dialog with the full report, and the "Import more" action that lived in it,
  are dropped; a better way to show a full report after the import is left for later. Row-level
  detail is available in the preview.
- **Upload is always available**, whatever the user's permissions on the current folder, because
  the import is not tied to that folder. Its options depend on the folder: the asset and file
  upload options appear only when the user can upload into the current folder, and "Import CSV" is
  always there. In a folder that pins a default file type, Upload offers that type plus "Import
  CSV", instead of opening the file picker straight away.
- **Preview is never disabled.** Clicking it with something missing shows a clear message for each
  missing or invalid input.
- **The file picker only accepts `.csv` files**, so there's no error for other file types.
- **The sample CSV reuses the existing behavior** as it is, so this spec doesn't restate its
  content. It only requires the link to be disabled until a content type is picked.
- **Retrying means submitting again.** The import endpoint has no retry operation, and the import
  job is never retried by the server. A user retries by fixing the form or the file and previewing
  or importing again.

### Session 2026-10-13 (planning: corrections from the code)

Planning traced the import code and found that the spec described row placement wrongly. The
developer chose to keep the legacy behavior:

- **Rows are placed exactly as in Content Search today.** A row goes to the site/folder in its
  CSV's site/folder column. If the CSV has no such column, the row goes to the content type's
  default site/folder (often System Host), not to the root of any site. A row whose site/folder
  cell is empty is a row error. The earlier wording ("rows without one go to the site root")
  was wrong and is replaced throughout.
- **What the site parameter changes.** The server session's site is not used to place rows. It is
  used to resolve references inside the row, such as File and Image field values. The site sent by
  the Drive replaces the session's site for exactly that. This keeps two tabs on different sites
  from resolving each other's references, without changing where rows land.
- **Content type selector.** Adapting the Drive's content type filter to single selection proved
  too costly for a shared component, so the form uses a plain single-select dropdown, as the spec
  allowed. No existing API lists only the content types a user can write to, so the server gains a
  way to list them.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Import a CSV from Content Drive (Priority: P1)

An editor has a spreadsheet of items for one content type. In Content Drive they open Upload,
choose "Import CSV", pick the content type, a language and a workflow action, choose key fields,
attach the CSV, preview it, and confirm the import. The dialog closes and they keep working while
the status toast shows the import running. When it ends, the toast reports how many items were
created, updated and failed, and the Drive listing shows the new content.

**Why this priority**: This is the feature. It removes the last reason to go back to Content Search
for bulk loading.

**Independent Test**: In Content Drive, import a valid CSV with two new rows for any content type.
The dialog closes, the status toast shows the import running and then two created, and the listing
shows both items.

**Acceptance Scenarios**:

1. **Given** any folder in the Drive, **When** the user opens Upload, **Then** "Import CSV" is
   offered, whatever the user's permissions on that folder.
2. **Given** the user can't upload into the current folder, **When** they open Upload, **Then** the
   asset and file upload options are not offered, and "Import CSV" is.
3. **Given** a folder that pins a default file type, **When** the user opens Upload, **Then** it
   offers that file type and "Import CSV", instead of opening the file picker straight away.
4. **Given** the user chose "Import CSV", **When** the dialog opens, **Then** it shows Content Type,
   Language, Workflow Action, Key Fields, a CSV file picker, and a link to download a sample CSV.
5. **Given** a complete form and a preview, **When** the user confirms the import and the import
   starts, **Then** the dialog closes and the status toast shows the import running with an
   indefinite loading state.
6. **Given** an import is running, **When** the user reloads the page, or leaves the Drive and comes
   back, **Then** the status toast is shown again with the same import, as it is for upload and
   folder copy.
7. **Given** the import ended, **When** the user looks at the status toast, **Then** it shows a
   summary with the created, updated and failed counts.
8. **Given** the import ended while the user is in the Drive, **When** the toast reports it,
   **Then** the Drive listing refreshes and shows any new content in the current folder.
9. **Given** the user drops a `.csv` file onto the Drive, **When** the drop completes, **Then** it
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
5. **Given** the dialog opens, **When** the user looks at Language, **Then** it is prefilled with the
   language the user is working in in the Drive, and its list offers every language plus a
   "Multilingual file" option.
6. **Given** the user picks "Multilingual file", **When** the form updates, **Then** it explains
   that each row needs `languageCode` and `countryCode` columns and that at least one key field is
   required.
7. **Given** a required input (content type, language, workflow action, file) is missing, or
   Multilingual is picked without a key field, **When** the user clicks Preview, **Then** no preview
   starts and the form shows a clear message on each input that needs attention.
8. **Given** the user opens the file picker, **When** they browse for a file, **Then** only `.csv`
   files can be chosen.
9. **Given** the form, **When** the user reads it, **Then** it states that each row is placed by
   the CSV's site/folder column, or, when the CSV has no such column, in the content type's default
   site/folder, and never in the folder the Drive is showing.

---

### User Story 3 - Preview shows clearly what will happen (Priority: P1)

Before anything is written, the user previews the import and sees, in a readable layout, a summary
(rows read, headers matched, key fields, how many items would be created and updated, and where the
rows will land) and every warning and error, each with its line number, field and value when known.

**Why this priority**: The preview is how users catch mistakes before writing hundreds of items,
including sending rows to the wrong place. It is also the only place that shows problems row by
row. Content Search shows it as plain text, which is hard to scan.

**Independent Test**: Preview a CSV for any content type with a date field, where line 3 has a bad
date and the file has an unknown column, with rows spread over two folders. The summary shows the
counts and how many rows go to each folder, the error names line 3 and the date field, and the
warning names the unknown column. Nothing is created.

**Acceptance Scenarios**:

1. **Given** a complete form, **When** the user clicks Preview, **Then** the dialog shows a loading
   state until the preview is ready, and no content is created.
2. **Given** the preview is ready, **When** the user reads it, **Then** it shows a summary with
   rows read, headers matched, key fields used, items to create and items to update.
3. **Given** the preview is ready, **When** the user reads the summary, **Then** it shows where the
   rows will land: how many rows go to each site/folder (for example
   "12 rows → //demo.dotcms.com/news/"), including rows going to the content type's default
   site/folder because the CSV has no site/folder column.
4. **Given** the preview has warnings or errors, **When** the user reads them, **Then** warnings and
   errors are shown as separate, clearly distinguished groups with their counts, each listed in
   line-number order, and each entry shows its line number, field and offending value when the
   result includes them.
5. **Given** the preview has no warnings or no errors, **When** the user reads it, **Then** the
   matching group says there are none.
6. **Given** the preview is ready, **When** the user changes any form input or the file, **Then**
   the preview is discarded and the user must preview again before importing.
7. **Given** the preview is ready, **When** the user cancels it, **Then** they return to the form
   with their inputs kept, and no content is created.
8. **Given** the preview has errors, **When** the user clicks Import, **Then** the import is
   allowed, as in Content Search: the dialog warns that rows with errors will be skipped, and the
   user confirms before it starts.
9. **Given** the preview has no errors, **When** the user clicks Import, **Then** the import starts
   after the same confirmation Content Search asks for.

---

### User Story 4 - Download a sample CSV (Priority: P2)

Users who don't know the expected columns download a sample CSV for the selected content type,
fill it in and import it. The file is the same one Content Search's import offers today.

**Why this priority**: It saves guessing column names, but users who already have a file don't need
it.

**Independent Test**: Pick any content type and click the sample link. The CSV that downloads is
the same one Content Search's import produces for that type.

**Acceptance Scenarios**:

1. **Given** no content type is picked, **When** the user looks at the sample link, **Then** it is
   disabled.
2. **Given** a content type is picked, **When** the user clicks the sample link, **Then** the same
   sample CSV Content Search's import produces for that type downloads.

---

### User Story 5 - Failures are reported and the user can retry (Priority: P2)

When the preview or import can't run (the content type or language no longer exists, the file is
empty or unreadable, the server fails), the user sees what went wrong and can fix the form or the
file and try again.

**Why this priority**: Without it a failure leaves the user with a blank dialog or a toast that
doesn't say what to fix.

**Independent Test**: Preview with an empty CSV. The dialog shows an error explaining the file has
no data rows, the form is still filled in, and replacing the file lets the user preview again.

**Acceptance Scenarios**:

1. **Given** the preview fails, **When** the dialog shows the failure, **Then** it shows the
   server's error message and the form stays editable with the user's inputs kept.
2. **Given** the user confirmed the import, **When** the import can't be started, **Then** the
   dialog stays open, shows the error, and keeps the user's inputs.
3. **Given** the import started, **When** it fails part way or entirely, **Then** the status toast
   reports the failure with the server's message and the created, updated and failed counts.
4. **Given** a failure was shown, **When** the user changes inputs or replaces the file and
   previews again, **Then** the new preview runs normally.

---

### Edge Cases

- **Closing during the preview.** Allowed; nothing is created.
- **No content type to import into.** Since Import CSV is always offered, a user with write
  permission on no content type can open the dialog. Content Type then shows that there is nothing
  they can import into, and no preview can start.
- **Two tabs on different sites.** Each tab's import resolves in-row references (such as File and
  Image field values) against the site that tab's Drive is browsing, because the site is sent with
  the request, not taken from the shared server session.
- **CSV without a site/folder column.** Rows go to the content type's default site/folder, as in
  Content Search, and the preview says so.
- **Empty site/folder cell.** The row is an error, as in Content Search.
- **Rows with a folder that doesn't exist.** They land at the site root with no error, as in
  legacy. Tracked separately, not fixed here.
- **Very large files.** No new size limit; the server's existing upload limit applies. The status
  toast keeps showing the import as running for as long as it takes.
- **Per-row action column.** A CSV may name a workflow action per row; legacy honors it over the
  form's action, and so does this import.
- **Identifier column.** A CSV with an `Identifier` column updates the matching content, as in
  legacy.
- **Permissions per row.** Rows the user can't create in their target folder, or can't run the
  action on, show up as row errors in the preview and are counted as failed in the summary.
- **Encoding.** Files are read as UTF-8.

## Requirements *(mandatory)*

### Terms

- **Preview**: a dry run that reads the whole CSV with the chosen options and reports what would
  happen, without writing anything.
- **Key fields**: fields used to find existing content to update instead of creating new content.
- **Multilingual file**: a CSV where each row carries its own language in `languageCode` and
  `countryCode` columns.
- **Status toast**: the Drive's persistent notice that upload and folder copy already use. It shows
  an indefinite loading state while a run is in progress, a summary when it ends, and is restored
  after a reload or after coming back to the Drive.
- **Working language**: the language the Drive's language filter is set to. When it holds no single
  language, the default language.

### Functional Requirements

- **FR-001**: Content Drive's Upload control MUST always be available, whatever the user's
  permissions on the current folder, and MUST always offer "Import CSV".
- **FR-002**: Upload MUST offer the asset and file upload options only when the user can upload
  into the current folder. In a folder that pins a default file type, Upload MUST offer that type
  and "Import CSV" instead of opening the file picker straight away.
- **FR-003**: "Import CSV" MUST open a dialog containing Content Type, Language, Workflow Action,
  Key Fields, a CSV file picker and a sample CSV link.
- **FR-004**: Content Type MUST be a plain single-select dropdown, with a text filter, listing only
  the content types the user has write permission on, excluding Host, as Content Search's import
  does. The server MUST provide that list.
- **FR-005**: Language MUST list every language plus a "Multilingual file" option, MUST be
  prefilled with the working language, and the chosen language MUST always be sent to the import
  (never left empty).
- **FR-006**: Workflow Action MUST list exactly the actions Content Search's import offers for the
  selected content type and user, using the endpoint Content Search already calls, and MUST be
  required.
- **FR-007**: Key Fields MUST list only the selected type's indexed fields that can be used as keys
  (no relationship, file, image or layout fields). Non-indexed fields MUST NOT be shown.
- **FR-008**: Changing the content type MUST reset the workflow action and key fields.
- **FR-009**: Preview MUST NOT be disabled. When the user clicks it with content type, language,
  workflow action or file missing, or with a multilingual file and no key field, no preview MUST
  start and the form MUST show a clear message on each input that needs attention.
- **FR-010**: The file picker MUST only accept `.csv` files.
- **FR-011**: The dialog MUST tell the user that rows are placed by the CSV's site/folder column, or
  in the content type's default site/folder when the CSV has no such column, and not in the folder
  the Drive is showing.
- **FR-012**: Preview MUST run the import without writing anything and show a summary (rows read,
  headers matched, key fields, items to create, items to update), then warnings and errors as
  separate groups with counts. Each entry MUST show its line number, field and value when known.
- **FR-013**: The preview summary MUST show where the rows will land: the number of rows per
  site/folder, computed the same way the import places them, including rows that go to the content
  type's default site/folder.
- **FR-014**: Warnings and errors in the preview MUST be listed in ascending line-number order.
  Messages without a line number MUST come first.
- **FR-015**: The preview MUST be shown with the Drive's own visual components, not as plain text.
- **FR-016**: Any change to the form or the file after a preview MUST discard it and require a new
  preview before importing.
- **FR-017**: Import MUST use exactly the file and options of the last preview.
- **FR-018**: Import MUST be allowed whether or not the preview has errors, as in Content Search.
  When it has errors, the dialog MUST warn that rows with errors will be skipped and ask the user to
  confirm.
- **FR-019**: Once the import has started, the dialog MUST close and the import MUST run in the
  background. The status toast MUST show it running with an indefinite loading state, through the
  same mechanism upload and folder copy use, and MUST be restored after a reload or after leaving
  the Drive and coming back.
- **FR-020**: When the import ends, the status toast MUST show a summary with the created, updated
  and failed counts, and the server's message when the import failed.
- **FR-021**: When an import ends while the user is in the Drive, the listing MUST refresh.
- **FR-022**: When the preview fails, or the import can't be started, the dialog MUST show the error
  and keep the form editable with the user's inputs, so the user can retry by submitting again.
- **FR-023**: The import endpoint MUST accept an optional site. When sent, it MUST be used wherever
  the import uses the session's site today (resolving in-row references such as File and Image
  field values); it MUST NOT change where rows are placed. Content Drive MUST always send the site
  it is browsing. When no site is sent, the endpoint MUST behave as today. A site that doesn't
  exist or that the user can't read MUST be refused.
- **FR-024**: A new server endpoint MUST return, for a content type, the same sample CSV Content
  Search's import produces, and MUST refuse users who can't read that content type. Content Search
  keeps its own download unchanged.
- **FR-025**: The sample CSV link MUST be disabled until a content type is picked.
- **FR-026**: The import endpoint's documentation MUST state that leaving `language` out, or sending
  `-1`, means a multilingual file, MUST describe the new site parameter, and the generated API
  description MUST be updated.
- **FR-027**: Dropping a `.csv` onto the Drive MUST keep uploading it as a file, as today.
- **FR-028**: All new visible text MUST be translatable, reusing Content Search's existing
  translation keys where the meaning is the same.

### Key Entities

- **Import request**: content type, language (or multilingual), workflow action, key fields, CSV
  file, site.
- **Import run**: a running or finished import, identified by the server's job, with its final state
  (succeeded or failed). The browser remembers the runs it started so the status toast can be
  restored.
- **Preview result**: summary counts (rows read, to create, to update), where rows will land (rows
  per site/folder), and lists of info, warning and error messages, each with
  line number, field and value when known.
- **Import summary**: created, updated and failed counts, and the server's message when it failed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can import a CSV into any content type they can write to without leaving
  Content Drive.
- **SC-002**: For the same CSV and options, the preview reports the same counts, warnings and errors
  as Content Search's preview.
- **SC-003**: In every preview, warnings and errors appear in ascending line-number order.
- **SC-004**: A user can keep working in the Drive while an import runs, and after a reload still
  sees the import running and, when it ends, its summary.
- **SC-005**: Every failed preview, failed start or failed import tells the user the server's error;
  no failure ends with a blank or stuck dialog or a toast with no message.
- **SC-006**: In two tabs browsing different sites, each import resolves File and Image references
  against its own tab's site, and rows land where Content Search would place them.
- **SC-007**: The sample CSV downloaded from the Drive is identical to the one Content Search
  produces for the same content type.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: Content Search's import (the legacy Struts flow and its JSP
  screens) is the reference. It is not changed or removed, and it keeps its own sample download.
  The server's REST import, which already powers both validation and import as background jobs, is
  reused and gains an optional site parameter. A sample CSV endpoint is added next to it, with the
  logic that today exists only inside the legacy action made reusable. The Drive's Upload control
  changes: it is always available and becomes a menu in every folder.
- **Backward-compatibility expectations**: Content Search's import keeps working unchanged. Callers
  of the REST import that don't send a site keep today's behavior; its documentation of `language`
  is corrected. Uploading files from Upload, and drag and drop in the Drive, keep working as today
  for users who can upload into the folder.
- **Known related decisions**: Import is one half of the Drive's import/export epic (#37928); the
  export (#37900) writes CSVs this import must read back. The status toast and its restore after a
  reload already exist for upload and folder copy, and import plugs into them. The plan phase will
  consult `dotCMS/platform-adrs`.

## Assumptions

- The server import runs as a background job and keeps its result; the toast's summary is read
  from there, including after a reload.
- Where rows will land is worked out by the server during the preview, using the same placement
  logic as the import. The preview result doesn't carry it today, so the server adds it.
- Language stays in the form even though the developer's description didn't mention it, because the
  import always needs one, and leaving it out silently turns on multilingual mode.
- Out of scope: importing by drag and drop (see Clarifications), cancelling a running import, a full
  row-by-row report after the import, targeting the current folder, fixing rows whose folder doesn't
  exist, checking who owns an import job on the job endpoints (to be filed separately), a "recent
  imports" page, and export (#37900).
