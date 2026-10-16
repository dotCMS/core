# Data Model: CSV import in Content Drive (#37896)

No database or index changes. These are request/response and in-memory shapes.

## Import form (multipart `form` part, JSON) — `ContentImportForm`

| Field | Type | Required | Notes |
|---|---|---|---|
| `contentType` | string (id or variable) | yes | must be writable by the user |
| `language` | string (id, ISO code, or `-1`) | yes (by the Drive) | `-1` or missing = multilingual file |
| `workflowActionId` | string | yes | from `initialactions/contenttype/{id}` |
| `fields` | string[] (ids or variables) | no; ≥1 when multilingual | key fields |
| `site` | string (id or hostname) | no (**new**) | Drive always sends it; replaces the session site |
| `stopOnError` | boolean | no (default false) | Drive leaves default |
| `commitGranularity` | int | no (default 100) | Drive leaves default |

Validation: unknown `site` → 400; `site` not readable → 403.

## ImportDestination (**new**, in `ImportResult.destinations`)

| Field | Type | Notes |
|---|---|---|
| `path` | string | `//host/folder/path/` (folder `/` = site root) |
| `rows` | int | rows without errors placed there |
| `source` | enum `CSV` \| `CONTENT_TYPE_DEFAULT` \| `EXISTING` | why the row lands there |

- `CSV`: the row's site/folder column.
- `CONTENT_TYPE_DEFAULT`: new content, CSV has no site/folder column → the content type's host/folder.
- `EXISTING`: content matched by key or identifier, CSV has no site/folder column → keeps its place
  (one bucket per current location).

Line-level: `LineImportResult.destination` (optional `{path, source}`), set once the row's location is
final; not set for rows with errors.

## ImportResult (existing, used by the Drive)

`type` (PREVIEW|PUBLISH), `contentTypeName`, `contentTypeVariableName`, `keyFields[]`,
`file.totalRows`, `file.headers.{valid,invalid,missing,special}`, `data.processed.{parsedRows,failedRows}`,
`data.summary.{created,updated,failed}`, `info[]`/`warning[]`/`error[]` of
`{code, message, row?, field?, value?, context?}`, `stoppedOnErrorAtLine?`, **`destinations[]` (new)**.

Derived in the Drive: rows read = `parsedRows - 1`; headers matched = `valid.length`; in PREVIEW,
`summary.created`/`updated` are the to-create/to-update counts.

## Job (existing `JobView`)

`id`, `queueName` (`importContentlets`), `state` (`PENDING`, `RUNNING`, `SUCCESS`, `FAILED`,
`FAILED_PERMANENTLY`, `ABANDONED`, `ABANDONED_PERMANENTLY`, `CANCEL_REQUESTED`, `CANCELLING`,
`CANCELED`), `parameters` (`cmd`: `preview`|`publish`, `userId`, `contentType`, …, `siteIdentifier`,
`siteName`), `result` (ImportResult flattened, plus `error[]` from `errorDetail` when failed).

Terminal for the Drive: `SUCCESS`, `FAILED`, `FAILED_PERMANENTLY`, `ABANDONED*`, `CANCELED`.

## ContentTypeSummaryView (**new**, list item of `GET /api/v1/contenttype/_writable`)

`{ id, variable, name, baseType, icon }`, sorted by name; only WRITE-permitted; the import excludes
Host through `exclude=Host`. Served a page at a time with `pagination` (see
contracts/content-import-api.md). The Drive hands each page to the shared `dot-content-type`
selector as `DotCMSContentType[]`: these are the only fields that selector reads. Frontend model:
`DotContentTypeSummary`, fetched by `DotContentTypeService.getWritableContentTypes`.

## IMPORT_CONTENTLETS_COMPLETED event payload (**new**) — see contracts/import-completed-event.md

## Drive state

- **Import run** (in `withActionExecution.runs`): `operation: 'import:' + jobId`, `targets: []`,
  `total: 1`, `labelKey: 'content-drive.import.indicator'`, `backgrounded: true`. Tracked jobs map
  `importJobs[jobId] → runId`; `settledImportJobs[]` against duplicate events.
- **Import outcome** (`DotContentDriveActionExecutionResult`): `outcomeKind: 'import'`,
  `successCount = created + updated`, `failedCount`, plus new `createdCount`, `updatedCount`,
  optional `message` (server error).
- **Dialog state** (local to the component): `step: 'form' | 'previewing' | 'preview'`, `preview?`
  (the finished job from `POST /_preview`), `error?`. While previewing, the form stays visible and
  disabled. Any form/file change from `preview` → back to `form`
  with the result discarded.
- **Working language**: the single language in `filters.languageId`, else `defaultLanguageId`.
