# Contract: `GET /api/v1/cluster/nodes` (consumed, not built, by this feature)

> **The authoritative contract is now the backend spec, PR #38032** (`specs/37875-network-cluster-nodes/spec.md`, "Response contract"). This file records what the Network (Beta) portlet relies on from it. Where the two differ, #38032 wins and this file is updated.

Differences from the earlier proposal that the portlet must handle (2026-10-15):

- A node that did not answer has `cache`, `search` and `assets` set to **`null`** (not objects with `null` fields).
- `name`, `clusterName`, `ipAddress` and `version` can be `null` when the server's database row has none.
- Each node has its own **`clusterName`** (from `cluster_server.cluster_id`), present even when it did not answer; the "Cluster" tile uses it (FR-016).
- `lastContactSeconds` is never `null`.
- `DOWN` also covers a node that answered but whose assets check failed.
- There is **no license check**: every install reports `PLATFORM` (`LicenseUtil.getLevel()`). Errors are 401 only.
- Access: `configuration` or `network-beta`, or a CMS Administrator. `network` is added at promotion (#37805).
- The heartbeat flag is `config.cluster.heartbeatEnabled` in `GET /api/v1/appconfiguration`, for backend users.

TypeScript definitions: `core-web/libs/dotcms-models/src/lib/dot-cluster-node.model.ts`.

## Request

```
GET /api/v1/cluster/nodes
```

No parameters. One call per load or refresh (FR-053).

**Access** (FR-003): a backend user with the `network-beta` or `configuration` portlet in a layout, or a CMS Administrator (`network` at promotion, #37805). Anyone else gets 401.

**Latency**: the server waits for nodes to answer, at least 2 s and about 1 s per node, whenever a node does not answer (`CLUSTER_SERVER_THREAD_SLEEP`). The node serving the request should be built in-process so it is never reported as not answering (#37877).

## Response `200`

See the sample response and the field / type / nullability table in #38032. The portlet reads `entity` only.

## Rules the UI relies on

| Rule | Why the UI needs it |
|---|---|
| Every node has the same shape, whether it answered or not | One model, no per-shape adapter (FR-008, FR-052) |
| A node that did not answer has `responded: false` and `cache`, `search`, `assets` set to `null` | Placeholders instead of errors (FR-035) |
| `status` is computed on the server and is the only source of the node badge | FR-011 |
| `null` means "unknown" or "does not apply"; empty strings and `-1` are never sent | FR-024, FR-052 |
| Numbers are numbers and booleans are booleans; no formatted strings (`"0s"`, `"true/"`) | The UI formats durations and percentages (FR-028, FR-029) |

## Errors

| Status | When | UI behaviour |
|---|---|---|
| `401` | Not a backend user, or no access to the portlets above | Standard error handling; error state on first load (FR-044) |
| `5xx` | Any server failure | Error state with retry on first load; toast and keep data on refresh (FR-041, FR-044) |
