# Feature Specification: Content Drive — export content to CSV

**Feature Branch**: `37900-content-drive-add-content-export`

**Created**: 2026-10-13

**Status**: Draft

**Type**: New Feature

**Related GitHub Issue**: [#37900](https://github.com/dotCMS/core/issues/37900)

**Input**: [#37900](https://github.com/dotCMS/core/issues/37900) — Content Drive: Add content export.
Bring the Content Search CSV export to Content Drive with feature parity first (filter mode: exactly
one content type, every result of the listing's location and filters, as one CSV), then add
selection as new capability (selection mode: up to 60 folders and contentlets of any content types,
as a zip with one CSV per content type). Both reuse the Content Search CSV writer and file names
(`<ContentType>_contents_<M_d_yyyy>.csv`; the zip is `<SiteName>_export_<M_d_yyyy>.zip`), keep the
100,000-row limit but refuse bigger exports instead of truncating them, and let the selection win
over the filter.

## Context

Content Drive cannot export content today. The only CSV export in dotCMS is in the legacy Content
Search portlet. It exports every result of the current search filters in one go (no paging), but
only when exactly one content type is chosen. Its filters include site (with or without System
Host), folder (only what sits directly in it), language, workflow and text. It stops at 100,000
rows without telling the user.

This feature brings that export to Drive, through one new endpoint, in two modes:

- **Filter mode (feature parity).** Nothing is selected and the Drive listing is filtered to exactly
  one content type. The export holds every contentlet the listing would show with the current
  location and filters, across all pages, as one CSV. Drive's views map onto Content Search's
  filters: All Site Content is the site filter, a folder is the folder filter (direct content only,
  in both), and the Include System Host chip is the System Host option.
- **Selection mode (new).** The user selects folders and contentlets, of any content types. A
  selected contentlet is exported in the language version the user selected, and only that one. A
  selected folder brings every contentlet below it, at any depth, in every language version and
  including archived ones. Folders themselves are never rows. The result is a zip with one CSV per content type. Drive's filters do not apply: **what you
  select is what you export.**

Each CSV has the same columns, values and file name as the Content Search export, so it can be
imported again (the import is a separate ticket, #37896). In both modes the only thing that removes
an item is permission: if the user cannot read it, it is not exported.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Export everything the listing shows for one content type (Priority: P1)

An editor filters the Drive listing to one content type, optionally narrowing it with the location
(All Site Content, the site root, a folder, System Host) and more filters (language, workflow,
status, text, Include System Host). With nothing selected, the editor clicks **Export**. One CSV
downloads with every contentlet the listing would show, across all pages, not only the page that
is loaded.

**Why this priority**: This is feature parity with the Content Search export, the only export
editors have today. It must exist in Drive before anything new is added.

**Independent Test**: In All Site Content, filter to content type "Blog" and language Spanish. The
listing reports 2,500 contentlets over several pages. Export with nothing selected.
`Blog_contents_<date>.csv` downloads with 2,500 data rows, all Spanish. Then open folder `/blog/`
with the same filters and export again: only the Blog contentlets directly in `/blog/` are in the
CSV.

**Acceptance Scenarios**:

1. **Given** nothing is selected and the listing is filtered to exactly one content type that is not
   a Form, **When** the toolbar renders, **Then** the Export button is shown, in every Drive view
   (All Site Content, site root, a folder, System Host).
2. **Given** the same, but the listing shows no contentlets for the current filters (none exist, or
   none the user can read), **When** the toolbar renders, **Then** the Export button is disabled.
3. **Given** nothing is selected and the listing is filtered to no content type or to more than one,
   **When** the toolbar renders, **Then** the Export button is not shown.
4. **Given** a filter-mode request names a content type that does not exist or that the user
   cannot read, **When** it reaches the server, **Then** it is refused with the same bad-request
   message in both cases, and no other content type is exported in its place.
5. **Given** the listing is filtered to one content type plus language, workflow, status, text or
   Include System Host, **When** the user exports, **Then** the CSV holds exactly the contentlets
   the listing shows with those filters, across every page.
6. **Given** the user is in All Site Content, **When** the user exports, **Then** matching
   contentlets from the whole site, at any depth, are exported, plus System Host content when the
   listing includes it.
7. **Given** the user is inside a folder, **When** the user exports, **Then** only the matching
   contentlets directly in that folder are exported, as the listing shows them and as Content
   Search's folder filter does.
8. **Given** the user is in the site-root view or the System Host view, **When** the user exports,
   **Then** exactly the matching contentlets that view lists are exported.
9. **Given** a filter-mode export, **When** it finishes, **Then** a single CSV (not a zip) named
   `<ContentType>_contents_<M_d_yyyy>.csv` downloads, and a toast reports the number of rows.
10. **Given** an export is running, **When** the user looks at the Export button, **Then** it shows a
   loading state and cannot be clicked again until the export ends.
11. **Given** a filter-mode export with a text filter whose search has to scan more rows than the
   database scan budget (`BROWSER_DB_MAX_SCAN_ROWS`, 50,000 by default), **When** it is exported,
   **Then** every matching row is in the CSV. The integration test can lower that property instead
   of creating 50,000 contentlets.

---

### User Story 2 - Export selected contentlets of several content types (Priority: P2)

An editor selects contentlets in Drive and clicks **Export**. A zip downloads with one CSV per
content type among them. Each selected row is exported as it was selected: the contentlet in that
language, and no other language version of it.

**Why this priority**: It is new capability beyond Content Search. It reuses the CSV writer, the
endpoint and the download from User Story 1, and adds the selection, the zip and the grouping by
content type.

**Independent Test**: On site `demo.dotcms.com`, select two contentlets of type "Blog" and one of
type "News", in English. One of the Blog contentlets also has a Spanish version, which is not
selected. Export. The zip is `demo.dotcms.com_export_<date>.zip` and holds
`Blog_contents_<date>.csv` with 2 data rows, both English, and `News_contents_<date>.csv` with 1
data row. The toast reports 3 items and 2 files.

**Acceptance Scenarios**:

1. **Given** one or more items are selected in Drive, **When** the toolbar renders, **Then** an
   Export button is shown beside the workflow actions, and the Action Center does not list an
   Export row.
2. **Given** contentlets of two content types are selected, **When** the user clicks Export,
   **Then** a zip downloads that holds one CSV per content type, and the toast reports the number
   of rows and CSV files exported.
3. **Given** a contentlet has versions in two languages and only its English row is selected,
   **When** it is exported, **Then** its CSV has only the English row. **Given** both language rows
   are selected, **Then** both are exported, each with its languageCode and countryCode.
4. **Given** a selected contentlet is archived and readable by the user, **When** it is exported,
   **Then** it appears in its CSV.
5. **Given** Drive's filters are active (content type, text, workflow, status, language), **When**
   the user exports a selection, **Then** the filters do not change which items are exported.
6. **Given** items are selected and the listing is filtered to one content type, **When** the user
   exports, **Then** the selection is exported and the filter is ignored.
7. **Given** more than 60 items are selected (folders and contentlets together), **When** the
   toolbar renders, **Then** the Export button is disabled and explains that at most 60 items can be
   exported at once.
8. **Given** a request whose selection has more than 60 entries, **When** it reaches the server,
   **Then** it is refused as a bad request and no file is produced.

---

### User Story 3 - Export whole folders (Priority: P2)

An editor selects one or more folders, optionally together with contentlets, and exports them. The
zip holds every contentlet inside the folders at any depth, plus the selected contentlets, each
exported once. Folders are only walked to reach their content; they are never rows.

**Why this priority**: Exporting a whole section of a site, of every content type, is what
selection adds over Content Search, which has no recursive folder export.

**Independent Test**: Build `/a/` holding contentlet X and subfolder `/a/b/c/` holding contentlet
Y. Select folder `/a/`, folder `/a/b/`, and contentlet X directly. Export. X and Y each appear once
and no row stands for a folder.

**Acceptance Scenarios**:

1. **Given** a folder is selected, **When** it is exported, **Then** the contentlets directly in it
   and in all of its subfolders, at any depth, are exported, and no folder is a row.
2. **Given** a selected folder holds a contentlet with versions in two languages, and an archived
   contentlet, **When** it is exported, **Then** both language versions and the archived contentlet
   are exported, each as its own row.
3. **Given** a selected folder holds files and pages, **When** it is exported, **Then** they are
   exported in their own content types' CSVs, with binary fields as a download URL.
4. **Given** a folder and a contentlet outside it are selected, **When** they are exported, **Then**
   both are in the same zip.
5. **Given** a contentlet is selected directly and is also inside a selected folder, **When** they
   are exported, **Then** the contentlet appears in its CSV once.
6. **Given** a folder and one of its subfolders are both selected, **When** they are exported,
   **Then** each contentlet appears once.
7. **Given** a selected folder contains Form or Host content, **When** it is exported, **Then** that
   content is not exported.

---

### User Story 4 - Exports never reveal what the user cannot read (Priority: P1)

A limited user exports, in either mode. Only content the user can read is exported, and nothing in
the response says how much was left out.

**Why this priority**: Both modes read past what is on screen (every page of the listing, or deep
folder trees). Without this, the export would hand out content the user cannot see in Drive.

**Independent Test**: With a limited user who can read `/a/` but not `/a/secret/`, and cannot read
contentlet Z in `/a/`, export folder `/a/` (selection mode), then filter to Z's content type in
All Site Content and export (filter mode). Neither file holds anything from `/a/secret/` or below,
nor Z. Neither response gives a count of skipped items.

**Acceptance Scenarios**:

1. **Given** a contentlet the user cannot read, in a filter-mode result, selected, or inside a
   selected folder, **When** the export runs, **Then** that contentlet is not exported.
2. **Given** a selected folder has a subfolder the user cannot read, **When** it is exported,
   **Then** no content from that subfolder or anything below it is exported.
3. **Given** a contentlet the user can read lives in a folder the user cannot read, **When** the
   user selects it directly and exports, **Then** it is exported.
4. **Given** the request names a folder the user cannot read, or a folder that does not exist,
   **When** it is submitted, **Then** both are refused with the same "not found" response, so a
   limited user cannot tell whether the folder exists, and no file is produced.
5. **Given** a hand-built request names a folder from a site other than the one being browsed,
   **When** it is submitted, **Then** it is refused with the same "not found" response.
6. **Given** a selection holds a contentlet that does not exist, one the user cannot read, and one
   that is outside the browsed site and System Host, **When** it is exported, **Then** all three
   are skipped the same way, and the response is the same in all three cases (the rest of the
   selection is exported, or "no content" if nothing is left), so it never reveals which of the
   three applied.
7. **Given** some items were skipped for permissions, **When** the export finishes, **Then** neither
   the file, the counts, nor the toast reveals how many were skipped.
8. **Given** a contentlet stops being readable between the moment the export counts it and the
   moment its row is written, **When** the export runs, **Then** that row is skipped, the export
   still succeeds, and the reported row count matches the rows actually written.

---

### User Story 5 - Exports over the limit are refused with a clear message (Priority: P1)

**Why this priority**: The Content Search export cuts at 100,000 rows without saying so, which
leaves the user with a file that looks complete and is not. In selection mode a silent cut would
drop arbitrary rows.

**Independent Test**: Filter to a content type with 100,001 readable contentlets and export. Nothing
downloads, and the toast says the export has more than 100,000 rows, that 100,000 is the limit, and
asks the user to narrow the filters. Repeat with a selection of folders holding 100,001 readable
rows.

**Acceptance Scenarios**:

1. **Given** a filter-mode export producing more than 100,000 rows the user can read, **When** it is
   exported, **Then** it is refused, no file downloads, and the message says the export has more
   than 100,000 rows and that 100,000 is the limit.
2. **Given** a selection producing more than 100,000 readable rows, **When** it is exported,
   **Then** it is refused in the same way.
3. **Given** an export of 1 to 100,000 rows, in either mode, **When** it is exported, **Then** it
   succeeds with every row. This includes exports of more than 10,000 rows, where a single search
   page would stop.

---

### User Story 6 - CSVs can be imported back (Priority: P1)

An editor exports content, edits a CSV, and imports it back to update the same contentlets.

**Why this priority**: Keeping the legacy format is what makes the files importable. This ticket
moves the CSV writer and changes one column (host/folder), and neither may break importing. It is
checked as part of this ticket, without waiting for the Drive import ticket (#37896).

**Independent Test**: Export a filter-mode CSV and a selection-mode zip that together hold content in
a folder, content at a site root and content in System Host. Import one CSV of each with Identifier
as the key field and no language (multilingual file). The same contentlets are updated with no row
errors, and each one is still on the same site and in the same folder.

**Acceptance Scenarios**:

1. **Given** a CSV from either mode, **When** it is imported back with Identifier as the key field
   and no language, **Then** the same contentlets are updated with no row errors.
2. **Given** a CSV holding a contentlet in a folder, one at a site root and one in System Host,
   **When** it is imported back, **Then** each contentlet ends up on the same site and in the same
   folder it was exported from. A missing row error is not enough: the location itself is checked.
3. **Given** the same content exported by the Content Search export and by Drive, **When** the two
   CSVs are compared, **Then** the columns and values match, except for the host/folder column
   (FR-022).

---

### User Story 7 - Clear outcome when there is nothing to export or the export fails (Priority: P1)

**Why this priority**: Without it the user cannot tell an empty export from a failed one.

In filter mode the user never reaches an empty export, because the button is disabled when the
listing is empty (FR-034). A selection can still turn out empty: a limited user can select a folder
they can read whose content they cannot read, or a folder with no content at all.

**Independent Test**: As a limited user, select a folder you can read whose contentlets you cannot
read, and export. Then force a server-side failure. Each shows its own toast and neither downloads a
file.

**Acceptance Scenarios**:

1. **Given** the selection holds nothing the user can read, **When** the user exports, **Then** the
   toast says there is nothing to export and no file downloads.
2. **Given** the export is refused or fails on the server, **When** it ends, **Then** the toast
   shows the server's message and no file downloads.

### Edge Cases

- **Same content type reached several ways.** In selection mode, a content type's rows can be
  reached through several folders, direct selections and query batches. The zip still has exactly
  one CSV per content type with one header row (FR-018).
- **Two content types with the same name.** Content type names are not unique, but file names in a
  zip must be. Both CSVs are kept, under distinct names (FR-019).
- **Content type name with characters not allowed in a file name** (`/`, `:`, …). The file name is
  made safe and the CSV is still produced (FR-019).
- **Same contentlet and language sent twice in a selection.** Exported once (FR-017).
- **Contentlet edited after it was selected, and also inside a selected folder.** Its working
  version is now a different inode from the one selected. It is still exported once, as the
  current working version (FR-002, FR-017).
- **A contentlet selected in one language that is also inside a selected folder.** The folder brings
  every language version of its content (FR-006), so all of them are exported, and the selected
  language still appears once (FR-017).
- **Content type with no exportable fields.** Its CSV still has the Identifier, languageCode and
  countryCode columns.
- **A field's value cannot be formatted.** As in legacy, the cell is left empty and the export
  continues.
- **Relationship to content the user cannot read.** The relationship column lists every related
  identifier, without filtering by the user's permissions, as legacy does (FR-024).
- **Filter mode on a Form type.** Forms are never exported, so the Export button is not offered for
  a Form type filter (FR-007).
- **Listing changes while it is being exported.** Content created or deleted during a filter-mode
  export may or may not be in the file; the export does not lock the listing.
- **60 items selected, one of them a large folder.** The 60-item limit counts what the user selected,
  not what is below it: a folder is one item. The 100,000-row limit still applies to what the
  selection expands to (FR-010).
- **Content at a site root.** Its host/folder column is the bare hostname, not `//site/`. Written as
  `//site/`, the import would resolve the path `/` to the system folder and silently move the
  contentlet to System Host, with no row error (FR-022).
- **Published contentlet edited afterwards.** It has a live and a working version in the same
  language. Only the working version is exported, so the file never holds two rows with the same
  Identifier and language (FR-017a).
- **Selected contentlet deleted, made unreadable or outside the site.** Skipped silently, all three
  the same way; the rest of the selection is still exported (FR-002a).
- **Selected folder deleted between selection and export.** The path no longer resolves and the
  request is refused with the same "not found" response as an unreadable folder (FR-014).
- **Permissions change while the export runs.** A contentlet that stops being readable after it was
  counted is skipped, not turned into an error (FR-026a).
- **Failure halfway through.** The user gets an error status, not a truncated file, and no temporary
  file stays on the server (FR-031, FR-032).

## Requirements *(mandatory)*

### Functional Requirements

**Modes**

- **FR-001**: The system MUST export in **filter mode** when the request carries no selection and
  instead carries the Drive listing query (location and filters) restricted to exactly one content
  type. Any Drive view is accepted: All Site Content, the site root, a folder, or System Host.
- **FR-002**: The system MUST export in **selection mode** when the request carries a selection: a
  list of site-qualified folder paths (`//site/path/`, the form Drive sends for bulk duplicate)
  and/or a list of selected contentlets, each entry naming an identifier and a language. An entry
  always resolves to the working version of that identifier in that language at export time, not
  to the version that was on screen when it was selected. The request MUST also name the site being
  browsed in Drive.
- **FR-002a**: Every selected folder MUST belong to the site being browsed; a folder outside it MUST
  be refused with the same "not found" response as an unreadable folder (FR-014). Every selected
  contentlet MUST belong to that site or to System Host (which All Site Content can list). A
  selected contentlet that does not exist, that the user cannot read, or that is outside the
  browsed site and System Host MUST be skipped silently, all three in the same way (FR-012), so the
  response never reveals that an identifier exists or which site it belongs to. A stale entry, such
  as content deleted by someone else after it was selected, therefore never fails the whole export.
- **FR-003**: When a request carries both a selection and a listing query, the selection MUST win
  and the listing query MUST be ignored.
- **FR-004**: A filter-mode request whose listing query names no content type or more than one MUST
  be refused as a bad request. Filter mode with several content types is not supported, so that the
  Content Search behavior it mirrors is not changed. The check MUST be made on the content type as
  resolved for the user, before any content is read: a content type that does not exist and one the
  user cannot read MUST both be refused, with the same bad-request message. An unresolved type MUST
  never fall through to a query with no content type restriction.
- **FR-004a**: A selection MUST hold at most 60 items, counting folders and contentlets together. A
  folder counts as one item, whatever it contains. A request whose selection has more than 60
  entries MUST be refused as a bad request.

**What gets exported**

- **FR-005**: In filter mode, the export MUST hold the complete set of readable contentlets that the
  same query the Drive listing uses would return, with the listing's location rules: All Site
  Content spans the whole site at any depth; a folder, the site root and System Host give only the
  contentlets that view lists. The export MUST obtain that set through an operation that guarantees
  it is complete. It MUST NOT depend on how the listing pages its results for the screen (page
  sizes, cursors, scan budgets), so that a change to the listing's paging cannot silently drop
  rows from an export.
- **FR-006**: In selection mode, a selected folder MUST export the contentlets directly in it and in
  every subfolder at any depth, in every language version and including archived contentlets, each
  version as its own row. Folders MUST never be rows.
- **FR-007**: Form and Host content MUST never be exported in either mode.
- **FR-008**: In selection mode, a selected contentlet MUST be exported only in the language version
  the user selected; its other language versions MUST NOT be exported unless they were selected
  too. A selected archived contentlet MUST be exported. Drive's filters (content type, text,
  workflow, status, language, Include System Host) MUST NOT change what is exported.
- **FR-009**: In selection mode, a selected contentlet MUST be exported when the user can read it,
  even if the user cannot read the folder it lives in.

**Size limit**

- **FR-010**: An export MUST hold at most 100,000 rows. In both modes, an export of more than
  100,000 rows the user can read MUST be refused before any file is produced, with a bad-request
  response whose message says the export has more than 100,000 rows and that 100,000 is the limit.
  The message MUST NOT need the exact total: counting MAY stop at the first row past the limit, so
  rejecting a very large export (for example a content type with millions of contentlets) costs no
  more than resolving 100,001 rows.
- **FR-011**: Any export of 1 to 100,000 rows MUST include every row, including exports of more than
  10,000 rows.

**Permissions**

- **FR-012**: A contentlet the user cannot read MUST NOT be exported in either mode. In selection
  mode it is skipped silently, exactly like a selected contentlet that does not exist or is outside
  the browsed site (FR-002a).
- **FR-013**: In selection mode, content in a subfolder the user cannot read MUST NOT be exported, at
  any depth below that subfolder.
- **FR-014**: A request naming a folder the user cannot read, or a folder that does not exist, MUST
  be refused with the same "not found" (404) response and message, so the response never reveals
  whether a folder exists.
- **FR-015**: The response MUST NOT reveal how many items were skipped for permissions. Only rows
  the user can read count toward the 100,000 limit (FR-010).

**Files and names**

- **FR-016**: A filter-mode export MUST produce a single CSV, not a zip. A selection-mode export MUST
  produce a zip named `<SiteName>_export_<M_d_yyyy>.zip`, where `<SiteName>` is the site the user is
  browsing in Drive and the date is the export date.
- **FR-017**: Each contentlet MUST appear at most once per language: duplicates are removed by
  identifier and language, not by inode. This applies when a contentlet is selected and also inside
  a selected folder, when nested folders are both selected, when the same entry is sent twice, and
  when the contentlet was edited after it was selected (its working version is then a different
  inode from the one the user saw). Together with FR-017a, every identifier and language gives
  exactly one row, the working version.
- **FR-017a**: Every row MUST be the working version of its contentlet in its language, in both
  modes, as the Content Search export does (its search adds `+working:true`,
  `ContentletAjax.java:1049`). There MUST be at most one row per identifier and language. A live
  version that differs from the working one MUST NOT be exported. This MUST be proven with a
  contentlet that was published and then edited: the export holds one row with the edited values.
- **FR-018**: The zip MUST hold exactly one CSV per content type found, with exactly one header row.
- **FR-019**: Every CSV, alone or inside the zip, MUST be named
  `<ContentType>_contents_<M_d_yyyy>.csv`, the name the Content Search export uses, where
  `<ContentType>` is the content type's name. Names within one zip MUST be distinct and safe as file
  names. When two content types would produce the same name, both CSVs MUST be kept under distinct
  names.

**CSV format**

- **FR-020**: The CSV MUST be written by the same code as the Content Search export, moved into a
  reusable piece that both call. The shared piece MUST only write the CSV and MUST NOT push websocket
  system messages. Today `downloadToExcel` pushes the "A total of N…" and error messages itself, so
  the extraction moves them to its caller: Content Search keeps sending them (FR-023), and Drive
  does not (FR-033).
- **FR-020a**: The shared writer MUST change how `downloadToExcel` handles failures today, while
  keeping its output:
  - A failure of the whole file MUST propagate to the caller, not be caught and followed by a
    success message. Drive turns it into a server error (FR-029); Content Search's caller keeps
    catching it and pushing its messages (FR-023).
  - A failure formatting one field MUST still leave that cell empty and continue, as today.
  - A contentlet that can no longer be read when its row is written MUST be skipped, not abort the
    export (FR-026a).
  - It MUST write to an output it is given, not to the HTTP response, so the file is complete before
    anything is sent (FR-031).
  - It MUST report the number of rows it wrote, which is the count Drive returns (FR-026a).
- **FR-021**: Each CSV's columns MUST be Identifier, languageCode, countryCode, then the content
  type's exportable fields in field order, the same columns the Content Search export writes for the
  same content type.
- **FR-022**: Field values MUST be formatted as the Content Search export formats them (category
  keys, tag names, related identifiers, key-value fields as JSON, a page's URL as its path, image
  paths, binary fields as a download URL, the legacy import/export date, datetime and time formats,
  and the legacy quoting and line-ending rules), with one change: the host/folder field MUST be
  written as a readable location instead of the raw identifier, in the form the existing import
  already resolves correctly for each case:
  - content in a folder: `//site/path/`;
  - content at a site root: the bare hostname, for example `demo.dotcms.com`;
  - content in System Host: `SYSTEM_HOST`.

  The column is deliberately not uniform, and MUST NOT be "normalized" to `//site/path/` for every
  row. The import splits `//site/` into the site and the path `/`, and resolving `/` always gives
  the system folder, whose host is System Host (`ImportUtil.getSiteAndFolderFromIdOrName`,
  `FolderFactoryImpl.findFolderByPath`). Content at a site root written that way would be moved to
  System Host on re-import, with no row error. A bare hostname is resolved by name, and
  `SYSTEM_HOST` by identifier, with no folder lookup. This ticket does not change the import.
- **FR-023**: The Content Search export MUST keep producing the same output it does today, including
  its file name, its 100,000-row cut and its messages.
- **FR-024**: The relationship column MUST list every related identifier, without filtering by the
  user's permissions, as legacy does. This is an explicit feature-parity decision, not inherited
  behavior: filtering related identifiers by read permission changes the Content Search export too,
  so it is handled in #38055, together with keeping hidden relationships on re-import.

**Endpoint contract**

- **FR-025**: Both modes MUST be served by one new Drive export endpoint.
- **FR-026**: On success the system MUST return the file as a download, with headers giving the
  number of rows exported (`X-Export-Contentlets`), the number of CSV files (`X-Export-Files`) and
  the file's size (`Content-Length`), all matching the file.
- **FR-026a**: A contentlet that was counted for the export but cannot be read when its row is
  written (its permissions changed in between) MUST be skipped, not turned into an error. The
  row count in `X-Export-Contentlets` and in the toast MUST be the number of rows actually written.
- **FR-027**: When there is nothing the user can read to export, the system MUST return "no
  content" (204) and no file.
- **FR-028**: An empty selection with no listing query, a malformed folder path, a filter-mode query
  not naming exactly one readable content type, a selection of more than 60 entries, and an export
  over 100,000 rows MUST return "bad request" (400) with a message the UI can show. A folder that
  does not resolve is "not found" (404), as an unreadable one is (FR-014).
- **FR-029**: When the export fails, the system MUST return a server error (500) with a JSON error
  body carrying a message the UI can show.
- **FR-030**: The endpoint MUST be documented in the generated API documentation.
- **FR-031**: The system MUST NOT send a partial file. The whole file is built before any of it is
  sent, so a failure halfway is reported as an error status.
- **FR-032**: The system MUST delete any temporary file it created for the export once the response
  ends, whether the export succeeded or failed.
- **FR-033**: The export MUST NOT push websocket system messages ("Please wait…", "A total of N…").
  The Drive UI reports start, end and failure itself.

**User interface**

- **FR-034**: When nothing is selected and the listing is filtered to exactly one content type that
  is not a Form, Drive MUST show the Export button in the toolbar, which exports in filter mode. If
  the listing shows no contentlets for the current filters, the button MUST be disabled. Otherwise,
  with nothing selected, it MUST NOT be shown.
- **FR-035**: When one or more items are selected, Drive MUST show the Export button beside the
  workflow actions in the toolbar, which exports the selection. The Action Center MUST NOT list an
  Export row. With more than 60 items selected, the button MUST be disabled and tell the user that
  at most 60 items can be exported at once.
- **FR-036**: While an export runs, the Export button MUST show a loading state and be disabled.
- **FR-037**: On success, the browser MUST download the file under the server's file name, and a
  toast MUST report the number of items and CSV files exported.
- **FR-038**: On a "no content" response the toast MUST say there is nothing to export. On a
  bad-request or error response the toast MUST show the server's message. In none of these cases MAY
  a file download.

### Key Entities

- **Listing query**: The Drive listing's location (All Site Content, site root, a folder, System
  Host) and filters, restricted to one content type.
- **Export selection**: The site being browsed, plus the folders (as site-qualified paths) and
  contentlets (each an identifier and a language) the user selected in Drive.
- **Readable folder set**: In selection mode, the selected folders plus every subfolder the user can
  read, reached only through folders the user can read, without duplicates.
- **Export row**: The working version of one contentlet in one language, written once per
  identifier and language.
- **Content-type CSV**: All export rows of one content type, with one header row, named
  `<ContentType>_contents_<M_d_yyyy>.csv`.
- **Export file**: A single content-type CSV (filter mode) or a zip of content-type CSVs (selection
  mode), with its row and file counts.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every export an editor can make today from Content Search (one content type, filtered
  by site or folder, language, workflow, text and System Host) can be made from Drive in one
  action, without paging through the listing first.
- **SC-002**: A user can export any mix of up to 60 selected folders and contentlets they can read,
  of any number of content types, in one action, up to 100,000 rows.
- **SC-003**: For the same content, 100% of columns and values in a Drive CSV match the Content
  Search export, except the host/folder column.
- **SC-004**: A CSV from either mode, imported back with Identifier as key and no language, updates
  the same contentlets with 0 row errors, and 100% of them stay on the same site and in the same
  folder, for content in a folder, at a site root and in System Host.
- **SC-005**: With a limited user, 0 items the user cannot read appear in any export, across every
  permission scenario in User Story 4.
- **SC-006**: In every export, each contentlet appears exactly once per language and each content
  type has exactly one CSV with one header row.
- **SC-007**: 0 exports are silently truncated. Every export over 100,000 rows is refused with a
  message saying it has more than 100,000 rows and that 100,000 is the limit.
- **SC-008**: After any export, successful or failed, 0 temporary export files remain on the server.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The CSV export of the legacy Content Search portlet
  (`EditContentletAction`, `cmd=export`, written by `downloadToExcel`). Its row formatting is moved
  into a reusable piece so Drive and Content Search write the same CSV (FR-020). Content Search
  itself keeps its filters, single-type limit, silent 100,000-row cut and websocket messages
  (FR-023). Drive's toolbar gains the Export button. Filter mode uses the same query as the Drive
  listing, so it exports what the listing shows rather than reimplementing its filters, but reads it
  through an operation that returns the complete set (FR-005).
- **Backward-compatibility expectations**: The Content Search export's file name, columns, values
  and messages stay the same. The new endpoint is additive. No database schema or index mapping
  changes. The Drive listing's search keeps its current behavior for the screen.
- **Known related decisions**:
  - Feature parity with Content Search is the first goal; selection is additional (team decision,
    2026-10-14).
  - The Export button sits in the toolbar, beside the workflow actions when there is a selection,
    not in the Action Center. This was decided in the team meeting.
  - The relationship column keeps legacy behavior and is not filtered by the user's permissions
    (FR-024). This was decided explicitly in the spec review, as feature parity: today's Content
    Search export already writes every related identifier unchecked (`loadRelationships`, with
    records from `getFieldValue`, which takes no user). Filtering by read permission, and keeping
    relationships the importing user cannot read on re-import, is tracked in #38055, which builds on
    the shared writer this ticket creates.
  - Content Search's folder filter and Drive's folder view both give only what sits directly in the
    folder (`conFolder`). The Drive listing never treats a named folder as recursive
    (`ContentDriveHelper.skipsFolderConstraint`), so filter mode in a folder matches Content Search
    (FR-005).
  - Folder paths use the same site-qualified form Drive already sends to bulk duplicate.
  - The plan will formally consult `dotCMS/platform-adrs`.

## Out of Scope

- **Filter mode with more than one content type.** Content Search supports exactly one, and the
  team chose not to change that behavior. Several content types are exported through selection.
- **Exporting across all sites at once.** Content Search's "all hosts" option has no Drive
  equivalent, because Drive always works inside one site.
- **Recursive filter mode in a folder.** Content Search's folder filter is not recursive either.
  Selecting the folder exports everything below it.
- **Filtering related identifiers by read permission** in the relationship column, and keeping
  hidden relationships on re-import (#38055). This ticket keeps the legacy behavior (FR-024).
- Import (separate ticket, #37896).
- Background-job exports with a download link, for exports too large to hold in browser memory.
- Exporting file contents. Binary fields export as a download URL, as in legacy, so a folder of
  files exports as metadata plus links, not a backup.
- Speeding up the legacy row writer (see Assumptions).

## Assumptions

- **Delivery order.** Filter mode (User Stories 1, 4, 5 and 7 for filter mode) is delivered first,
  because it is parity. Selection mode (User Stories 2 and 3, and the selection cases of 4 and 5)
  follows. They can ship as separate implementation PRs against this one spec.
- **Direction for the plan: resolve before writing, in both modes.** First, gather the distinct
  contentlet versions the user can read, each with its content type: in filter mode from the
  complete result of the listing's query (FR-005), in selection mode by resolving the folders
  (recursively) and the selected versions. Keep this step light: only identifiers, languages and
  content types, with permissions checked in batches (`permissionAPI.filterCollection`), and no
  loading or formatting of contentlets, and stop as soon as the count passes 100,000. That gives
  the row count, so "nothing to export" (FR-027) and "over the limit" (FR-010) are decided before
  any file is written. Then write one content type
  at a time, handing each type's versions to the reused Content Search writer, which already works
  from a content type and a list of versions. In selection mode this also avoids interleaving zip
  entries. The plan confirms or changes this; the spec only fixes the outcomes.
- **A complete listing result is the main risk of filter mode.** The listing's search is built to
  serve the screen. Walking its pages to the end is not safe: past the database scan budget
  (`BROWSER_DB_MAX_SCAN_ROWS`, 50,000 by default, compared against the absolute offset in
  `BrowserAPIImpl.getContentByChunks`) nearly every page comes back short while still reporting more
  results, so an exporter that stops on a short page would drop rows silently. The existing
  fetch-everything path (`maxRows <= 0`) only covers searches without a text filter. The plan must
  choose an operation that returns the complete set for the same query and hides the chunking, for
  example a dedicated `BrowserAPI` method.
- **Readable contentlet in an unreadable folder (FR-009) comes from reading the code, not from a
  reproduction.** A contentlet whose own permissions allow reading can show up in Drive's site-wide
  listing, so it can be selected, and what is selected is exported. This has not yet been
  reproduced with a limited user. The integration tests for User Story 4, scenario 3 are the first
  confirmation.
- **Performance is out of scope beyond correctness.** The legacy row writer looks up categories,
  tags, relationships and identifiers once per row, and the recursive folder walk checks
  permissions one folder at a time. Parity with legacy output is required. Speeding these up is
  not, but the plan should measure an export near 100,000 rows and a deep folder tree so the cost is
  known.
- **That measurement decides the limit.** The export builds the whole file before sending anything
  (FR-031), so the connection stays silent while it is built. If an export near 100,000 rows takes
  longer than the idle timeout of the load balancer or proxy in front of dotCMS, the user would get
  a generic gateway error instead of the server's message (FR-038), and the server would finish a
  file nobody downloads. If the measurement does not fit within that timeout, the plan MUST either
  lower the row limit to what fits or bring the background-job export (currently out of scope) into
  scope. It must not ship a limit that the proxy cannot serve.
- **The browser holds the file in memory.** The 100,000-row limit bounds its size.
- **Files and pages are content.** File assets, dotAssets and pages are contentlets and are exported
  in their own types' CSVs. Menu links are not contentlets and are not exported.
- **Importing is not changed by this ticket.** The export changes must not affect how CSVs import.
  User Story 6 is checked within this ticket; the Drive import (#37896) is a separate ticket and is
  not needed to verify it.
