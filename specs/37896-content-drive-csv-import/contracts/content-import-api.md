# Contract: Content import REST changes (#37896)

Base: `/api/v1/content/_import`. All endpoints: backend user required (`rejectWhenNoUser`).

## POST `/` and POST `/_validate` (changed: optional `site`)

multipart: `file` (`.csv`), `form` (JSON, see data-model.md).

- `site` absent → session site (unchanged behavior).
- `site` = id or hostname the user can read → used for `siteIdentifier`/`siteName` job parameters.
- 400: `site` doesn't exist (job-shaped error body, like other form errors).
- 403: `site` exists but isn't readable by the user.
- Response unchanged: `{entity: {jobId, statusUrl}}`.

## POST `/_validate` (existing; the Drive's preview)

multipart: `file` (`.csv`) or `tempFileId`, and `form` (JSON, see data-model.md). Queues the import
in preview mode and answers `{entity: {jobId, statusUrl}}`; form errors are a 400 before anything is
queued. Content Drive waits for the job: the `IMPORT_CONTENTLETS_COMPLETED` event for that `jobId`,
or `GET /{jobId}` every second as a fallback, then reads the finished job.

A synchronous `POST /_preview` existed for a while because a queued job waited up to 30 s on the
job queue's idle backoff. The PR #38054 review asked to use the queue instead, so the backoff was
fixed (`JobQueueManagerAPIImpl`: creating a job wakes an idle worker once it commits) and
`/_preview` was removed.

## GET `/{jobId}` (changed: result gains `destinations`)

`entity.result.destinations: [{path, rows, source}]`, present in PREVIEW and PUBLISH results when
there is at least one placed row. Additive; existing clients ignore it.

## GET `/api/v1/contenttype/_writable` (new, in `ContentTypeResource`)

Moved out of the import resource in the PR #38054 review (nollymar), so any selector can use it.

- Query: `filter` (optional; kept when the type's name or variable contains it, ignoring case),
  `page` (default 1), `per_page` (default 40, as `/api/v1/contenttype`), `exclude` (optional,
  comma-separated variables to leave out, ignoring case).
- 200: `{entity: [{id, variable, name, baseType, icon}], pagination: {currentPage, perPage,
  totalEntries}}`: one page of the WRITE-permitted types, sorted by name. The permission rule and
  the exclusions are applied before paging, so `totalEntries` counts matching types exactly.
- 400: `page` or `per_page` below 1.
- Consumer: the import dialog, through the shared `dot-content-type` selector's `pageLoader`, sends
  `exclude=Host` (Host is never offered, as in Content Search's import).

## GET `/_sample?contentType={idOrVar}` (new)

- 200: `text/csv`, `Content-Disposition: attachment; filename="CSV_Template.csv"`, body
  byte-identical to Content Search's "download a CSV sample" for that type.
- 400: `contentType` missing. 403: type not readable. 404: type not found.

## Docs

`ContentImportDocs.FORM_FIELD_DOC`: `language` row says omitting it or `-1` means a multilingual file
(needs key fields and `languageCode`/`countryCode` columns); new `site` row. `openapi.yaml` regenerated.
