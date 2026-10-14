# Contract: `GET /api/v1/cluster/nodes` (proposed — consumed, not built, by this feature)

The response the Network (Beta) portlet is coded against. It comes from the Network spike (#37802 §2.8), as now specified in #37875 (rewritten 2026-10-08). The heartbeat flag (FR-047) is **not** in this response: it is `config.cluster.heartbeatEnabled` in `GET /api/v1/appconfiguration`. Updated 2026-10-08: no section `health` (FR-034), no `licensed` field (unlicensed = license 403), and no fields the UI never shows (`clusterHealth`, `licenseId`, `host`, `provider`, message counters). **This feature does not build the endpoint.** It is input to the backend task, which owns the Java view classes, `@Schema` annotations and OpenAPI entry. Until the endpoint ships, `DotClusterService` serves this exact shape from `DOT_CLUSTER_NODES_MOCK`.

TypeScript definitions: `core-web/libs/dotcms-models/src/lib/dot-cluster-node.model.ts`.

## Request

```
GET /api/v1/cluster/nodes
```

No parameters. One call per load or refresh (FR-053).

**Access** (FR-003): a backend user with the `network`, `network-beta` or `configuration` portlet in a layout, or a CMS Administrator. Anyone else gets `401`/`403` (#37875).

**Latency**: the server waits for nodes to answer, at least 2 s and about 1 s per node, whenever a node does not answer (`CLUSTER_SERVER_THREAD_SLEEP`). The node serving the request should be built in-process so it is never reported as not answering (#37877).

## Response `200`

Standard dotCMS envelope; the portlet reads `entity` only.

```jsonc
{
  "entity": {
    "currentServerId": "10b10fa5-…",  // the node that served this request (FR-012, FR-013)
    "nodes": [
      {
        "serverId": "10b10fa5-…",
        "displayServerId": "10b10fa5",
        "name": "dotcms-demo-56cc78659b-kgzws",
        "ipAddress": "10.2.66.19",
        "version": "26.09.18-01",
        "lastContactSeconds": 4,       // null when no heartbeat was ever recorded
        "responded": true,             // false → identity only; every section value null (FR-035)
        "status": "UP",                // UP | LAGGING | DOWN — computed on the server (FR-011)
        "cache": {
          "clusterName": "dotcms-demo",
          "transport": "PubSub",
          "numberOfNodes": 3,
          "open": true,
          "address": null,             // null when the transport has none (PubSub)
          "port": null,                // null when the transport has none (PubSub, Null)
          "receivedBytes": 181314,
          "sentBytes": 175002
        },
        "search": {
          "engine": "OPENSEARCH",      // ELASTICSEARCH | OPENSEARCH — the engine serving reads (ADR-0009, #37876)
          "clusterName": "opensearch-ovh-east",
          "timedOut": false,
          "numberOfNodes": 3,
          "numberOfDataNodes": 3,
          "activePrimaryShards": 293,
          "activeShards": 589,
          "relocatingShards": 0,
          "initializingShards": 0,
          "unassignedShards": 0,
          "delayedUnassignedShards": 0,
          "numberOfPendingTasks": 0,
          "numberOfInFlightFetch": 0,
          "taskMaxWaitingInQueueMillis": 0,
          "activeShardsPercent": 100.0
        },
        "assets": {
          "path": "/data/shared/assets",
          "canRead": true,
          "canWrite": true
        }
      }
    ]
  }
}
```

## Rules the UI relies on

| Rule | Why the UI needs it |
|---|---|
| Every node has the same shape, whether it answered or not | One model, no per-shape adapter (FR-008, FR-052) |
| A node that did not answer has `responded: false` and every section value `null` | Placeholders instead of errors (FR-035) |
| `status` is computed on the server and is the only source of the node badge | FR-011 |
| `null` means "unknown" or "does not apply"; empty strings and `-1` are never sent | FR-024, FR-052 |
| Unlicensed installs get **403** via `InvalidLicenseException` (header `error-key: dotcms.api.error.license.required`); no `licensed` field | The license 403 is the backend's explicit report; any other failure is an error (FR-045) |
| Numbers are numbers and booleans are booleans; no formatted strings (`"0s"`, `"true/"`) | The UI formats durations and percentages (FR-028, FR-029) |

## Errors

| Status | When | UI behaviour |
|---|---|---|
| `403` + `error-key: dotcms.api.error.license.required` | License below Professional | Unlicensed state: Clustering upsell (FR-045, FR-046); not passed to the shared error handler |
| `401` / other `403` | Not a backend user, or no access to the portlets above | Standard error handling; error state on first load (FR-044) |
| `5xx` | Any server failure | Error state with retry on first load; toast and keep data on refresh (FR-041, FR-044) |
