# Contract changes: v1 Publishing API and configuration

## GET /api/v1/publishing and GET /api/v1/publishing/{bundleId}

Response schema unchanged. Semantics of three fields change for bundles that are queued and
not yet picked up by the publisher job.

| Situation | `status` | `createDate` | `statusUpdated` | `scheduledPublishDate` |
|---|---|---|---|---|
| Queued, `publish_date` in the future | `SCHEDULED` (unchanged) | oldest `entered_date` (unchanged) | null | `publish_date` (unchanged) |
| Queued, `publish_date` already due | `BUNDLE_REQUESTED` (was `SCHEDULED`) | `publish_date` (was oldest `entered_date`) | null | null (was `publish_date`) |
| Has an audit row | audit status (unchanged) | audit `create_date` (unchanged) | audit `status_updated` | null |

### `status` query parameter

- `status=SCHEDULED` returns only queued bundles whose publish date is in the future.
- `status=BUNDLE_REQUESTED` returns audit rows in that status plus queued bundles whose publish
  date is due.
- No `status` parameter returns everything, as today.

OpenAPI: the `@Operation` description of the `status` parameter in `PublishingResource` gains
the sentence "BUNDLE_REQUESTED also includes queued bundles whose publish date is due but that
the publisher has not picked up yet." `openapi.yaml` is regenerated and committed with the change.

### Compatibility

- Additive narrowing of `SCHEDULED`. No field added or removed. No status code change.
- Known consumers: Publishing Queue (Beta) portlet, which already renders `BUNDLE_REQUESTED` as
  "Pending". External scripts filtering on `SCHEDULED` to find waiting bundles must also query
  `BUNDLE_REQUESTED`. Release-note line required.

## Endpoint detail message on connect timeout

`PublishingJobDetailView.environments[].endpoints[].statusMessage` for a send that timed out
reads, as today for connection errors:

```
An error occurred for the endpoint <name> with address <host>:<port>. Error: java.net.SocketTimeoutException: Connect timed out
```

No new field. The `status` of that endpoint entry is `FAILED_TO_SENT`, as for a refused
connection.

## Configuration

| Property | Default | Effect |
|---|---|---|
| `PUSH_PUBLISH_CONNECT_TIMEOUT_MS` | `10000` | Connect timeout applied to the publisher's bundle-upload client and status-poll client. `0` restores the unbounded behavior. |

Set through any `Config` source (`dotmarketing-config.properties`, `DOT_PUSH_PUBLISH_CONNECT_TIMEOUT_MS` environment variable). Read when the client is created, so a change applies to the next job run without restart.
