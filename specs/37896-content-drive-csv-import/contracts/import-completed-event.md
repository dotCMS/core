# Contract: IMPORT_CONTENTLETS_COMPLETED system event (#37896)

Content Drive listens to it for the background import (`cmd: publish`) only; previews use the
synchronous `POST /_preview` and never wait on this event.

Pushed by `ImportContentletsCompletionListener` on `JobCompletedEvent` and `JobFailedEvent` for queue
`importContentlets`. Visibility: `USER`, the job's submitting user (`parameters.userId`). Delivered over
the existing websocket (`DotEventsSocket`). The same job may be reported more than once (e.g. `FAILED`
then `FAILED_PERMANENTLY`); consumers must de-duplicate by `jobId` (see docs/backend/SYSTEM_EVENTS.md).

Payload:

| Field | Type | Notes |
|---|---|---|
| `jobId` | string | |
| `state` | string | job state at the time of the event |
| `cmd` | `preview` \| `publish` | from job parameters |
| `created` | int? | `result.data.summary.created`, when the result exists |
| `updated` | int? | `result.data.summary.updated` |
| `failed` | int? | `result.data.summary.failed` |
| `message` | string? | first error message when the job failed |

No payload when the job has no submitting user (logged, nothing sent), matching
`BulkUploadCompletionListener`.
