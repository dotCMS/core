# Contract: Content import REST changes (#37896)

Base: `/api/v1/content/_import`. All endpoints: backend user required (`rejectWhenNoUser`).

## POST `/` and POST `/_validate` (changed: optional `site`)

multipart: `file` (`.csv`), `form` (JSON, see data-model.md).

- `site` absent → session site (unchanged behavior).
- `site` = id or hostname the user can read → used for `siteIdentifier`/`siteName` job parameters.
- 400: `site` doesn't exist (job-shaped error body, like other form errors).
- 403: `site` exists but isn't readable by the user.
- Response unchanged: `{entity: {jobId, statusUrl}}`.

## POST `/_preview` (new, synchronous)

multipart: `file` (`.csv`), `form` (JSON, see data-model.md). Runs the import in preview mode inside
the request — same parameters, temp file, validation and processor as a queued preview job — and
answers with the finished job. Nothing is queued and nothing is written. Content Drive uses this
instead of `/_validate`, whose queued job waited up to 30 s on the job queue's idle backoff.

- 200: `{entity: JobView}` with `state: SUCCESS` and `result` (ImportResult, including `destinations`).
- 400: invalid form, missing file, unknown `site`, or a file the processor can't read; the body is a
  failed job whose `result.error[]` names the problem.
- 403: `site` not readable by the user.

## GET `/{jobId}` (changed: result gains `destinations`)

`entity.result.destinations: [{path, rows, source}]`, present in PREVIEW and PUBLISH results when
there is at least one placed row. Additive; existing clients ignore it.

## GET `/contenttypes` (new)

- Query: none (the dialog filters client-side).
- 200: `{entity: [{id, variable, name, baseType, icon}]}` — WRITE-permitted types, Host excluded,
  sorted by name. Empty list when none.

## GET `/_sample?contentType={idOrVar}` (new)

- 200: `text/csv`, `Content-Disposition: attachment; filename="CSV_Template.csv"`, body
  byte-identical to Content Search's "download a CSV sample" for that type.
- 400: `contentType` missing. 403: type not readable. 404: type not found.

## Docs

`ContentImportDocs.FORM_FIELD_DOC`: `language` row says omitting it or `-1` means a multilingual file
(needs key fields and `languageCode`/`countryCode` columns); new `site` row. `openapi.yaml` regenerated.
