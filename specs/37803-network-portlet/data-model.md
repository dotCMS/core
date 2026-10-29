# Data Model: Network (Beta) Portlet

Client-side entities, their fields, and the display rule the spec attaches to each. Updated 2026-10-08 for the spec change (no section health, no count, unlicensed = license 403); only fields the UI shows are listed. The wire format is in [contracts/cluster-nodes.md](./contracts/cluster-nodes.md); the TypeScript types are in `core-web/libs/dotcms-models/src/lib/dot-cluster-node.model.ts`. Nothing is stored; every load replaces the previous data.

## Entities

### `DotClusterNodes` — one cluster status snapshot

| Field | Type | Display rule |
|---|---|---|
| `currentServerId` | string | The node that gets the "This node" marker, is listed first and is selected first (FR-012, FR-013) |
| `nodes` | `DotClusterNode[]` | Empty → "No nodes found" (FR-043) |

### `DotClusterNode`

| Field | Type | Display rule |
|---|---|---|
| `serverId` | string | Identity and selection key; not displayed |
| `displayServerId` | string | Short server ID on the list entry, header and summary (FR-009, FR-015, FR-016) |
| `name` | string | Host name on the list entry and header (FR-009, FR-015) |
| `ipAddress` | string | Summary (FR-016) |
| `version` | string | Summary (FR-016) |
| `lastContactSeconds` | number \| null | "Contacted N … ago"; `null` → "no heartbeat recorded" (FR-010) |
| `responded` | boolean | `false` → not-answering notice; placeholders in every section (FR-035) |
| `status` | `UP` \| `LAGGING` \| `DOWN` | Tag on the entry and header; shown as-is (FR-011) |
| `cache` | `DotClusterCacheInfo` | Cache Transport section |
| `search` | `DotClusterSearchInfo` | Search Cluster section |
| `assets` | `DotClusterAssetsInfo` | Assets section |

### `DotClusterCacheInfo`

| Field | Row (FR-020 order) | Display rule |
|---|---|---|
| `clusterName` | Cluster Name; also the header subtitle and the "Cluster" tile | FR-016, FR-023 |
| `transport` | Cache Transport | |
| `numberOfNodes` | Number of Nodes | |
| `open` | Channel Open | true / false (FR-022) |
| `address` | Cluster Address | `null` → "N/A" if the node answered, else placeholder (FR-024) |
| `receivedBytes`, `sentBytes` | Received/Sent | `received / sent` (FR-021) |
| `port` | Cache Port | Same rule as `address` (FR-024) |

### `DotClusterSearchInfo`

| Field | Row (FR-026 order) | Display rule |
|---|---|---|
| `engine` | header subtitle | "Elasticsearch" / "OpenSearch"; `null` → omitted (FR-025) |
| `clusterName` | Cluster Name | |
| `timedOut` | Timed Out | true / false — its own value (FR-027) |
| `numberOfNodes` | Number of Nodes | |
| `numberOfDataNodes` | Number of Data Nodes | its own value (FR-027) |
| `activePrimaryShards` … `numberOfInFlightFetch` | the eight shard and task rows | whole numbers |
| `taskMaxWaitingInQueueMillis` | Maximum task queue wait | `N ms` (FR-028) |
| `activeShardsPercent` | Active shards percent | at most one decimal, `%` (FR-029) |

### `DotClusterAssetsInfo`

| Field | Row (FR-030 order) | Display rule |
|---|---|---|
| `path` | Shared path | FR-031 |
| `canRead` | Read | true / false (FR-032) |
| `canWrite` | Write | true / false (FR-032) |

**Every section**: a `null` value renders as the "no value" placeholder `—`, never as `null`, `undefined`, `-1` or blank (FR-052).

## Client state (`DotNetworkStore`)

| Field | Meaning |
|---|---|
| `nodes` | Last successfully loaded nodes; kept when a refresh fails (FR-041) |
| `currentServerId` | From the last successful load |
| `selectedServerId` | The selected node; kept across refreshes while still reported, else the current node, else the first (FR-013, FR-037) |
| `licensed` | `false` after a license 403 (`error-key: dotcms.api.error.license.required`) |
| `heartbeatEnabled` | From `appconfiguration` → `config.cluster.heartbeatEnabled` (GlobalStore `systemCluster`); `true` when absent (older instances) |
| `status` | `ComponentStatus`: `INIT` → `LOADING` → `LOADED` / `ERROR` |

**Derived**: `sortedNodes` (current node first, then backend order), `selectedNode`, `showSkeleton` (no data yet and loading), `isRefreshing` (data shown and loading), `showError` (failed with no data), `showEmpty`, `showUnlicensed`, `showHeartbeatNotice`.

## State transitions

```text
INIT ──load──▶ LOADING ──ok──▶ LOADED ──refresh──▶ LOADING ──ok──▶ LOADED
                  │                                   │
                  └─fail─▶ ERROR (no data: error state)└─fail─▶ ERROR (data kept, toast)
LOADING ──load──▶ (ignored; one request in flight — FR-039)
```

Exactly one of these renders (FR-048): skeleton (`INIT`/`LOADING`, no data) · unlicensed · error (no data) · empty · list + detail (with the heartbeat notice above it when applicable).
