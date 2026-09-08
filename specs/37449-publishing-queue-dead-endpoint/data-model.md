# Data Model: #37449

No schema change. This documents the existing entities the fix reads and writes, and the state
transitions that change.

## Entities

### publishing_queue (row per asset per bundle)

| Column | Meaning | Touched by this fix |
|---|---|---|
| `bundle_id` | Bundle the asset belongs to | read |
| `asset` | Asset identifier | read |
| `operation` | 1 publish, 2 unpublish | read |
| `entered_date` | When the asset was added to the bundle | read only; no longer reported as `createDate` for due bundles |
| `publish_date` | Requested publish time; null for drafts | read; drives the due/future split |

A bundle is **queue-only** when it has rows here and no row in `publishing_queue_audit`.

### publishing_queue_audit (row per bundle)

| Column | Meaning | Touched by this fix |
|---|---|---|
| `bundle_id` | Bundle | read/write |
| `status` | `PublishAuditStatus.Status` code | written on finalize (F2) |
| `status_pojo` | Serialized `PublishAuditHistory`: `numTries`, `endpointsMap[environmentId][endpointId] -> EndpointDetail{status, info, stackTrace}`, bundle/publish start and end | written on finalize (F2); `info` carries the timeout message (F1) |
| `create_date`, `status_updated` | Audit timestamps | unchanged |

### Config property (new)

| Key | Type | Default | Scope |
|---|---|---|---|
| `PUSH_PUBLISH_CONNECT_TIMEOUT_MS` | int, milliseconds | 10000 | Read once per client creation in the publisher client factory. 0 would mean infinite and is allowed for operators who want today's behavior. |

### PublishingJobView / PublishingJobDetailView (v1 API, read model)

Fields relevant here: `status`, `createDate`, `statusUpdated`, `scheduledPublishDate`. See
`contracts/publishing-api.md` for the new mapping.

## State transitions

### Queue-only bundle as reported by the v1 API (F3)

```
publish_date > now   -> SCHEDULED        createDate = MIN(entered_date)   scheduledPublishDate = publish_date
publish_date <= now  -> BUNDLE_REQUESTED createDate = publish_date        scheduledPublishDate = null
```

Once the job inserts the audit row, the audit status is reported and this mapping no longer
applies. The job's first write is `BUNDLE_REQUESTED`, so the visible status does not change at
pickup.

### Send attempt against an endpoint that does not answer (F1)

```
today:  connect blocks until OS gives up (75 to 135 s) -> ConnectException -> endpoint FAILED_TO_SENT
after:  connect blocks <= PUSH_PUBLISH_CONNECT_TIMEOUT_MS -> SocketTimeoutException -> endpoint FAILED_TO_SENT
```

Bundle status after the attempt is unchanged: `FAILED_TO_SEND_TO_ALL_GROUPS` or
`FAILED_TO_SEND_TO_SOME_GROUPS`, `numTries` incremented, retried on the next tick, finalized as
`FAILED_TO_PUBLISH` after `PUBLISHER_QUEUE_MAX_TRIES`.

### Unexpected error while processing one bundle (F2)

```
today:  exception escapes -> run ends -> bundle left with audit BUNDLE_REQUESTED/BUNDLING and queue rows -> retried first next tick, run ends again
after:  exception caught  -> audit FAILED_TO_PUBLISH + message, queue rows deleted (one transaction) -> loop continues with next bundle
```
