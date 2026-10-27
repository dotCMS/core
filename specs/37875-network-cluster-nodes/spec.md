# Issue Resolution Specification: Cluster nodes endpoint and status report fixes for the Network (Beta) portlet

**Feature Branch**: `37875-network-cluster-nodes`

**Created**: 2026-10-12

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37875](https://github.com/dotCMS/core/issues/37875), [#37876](https://github.com/dotCMS/core/issues/37876), and part of [#37877](https://github.com/dotCMS/core/issues/37877). All three back the Network (Beta) portlet (epic [#37801](https://github.com/dotCMS/core/issues/37801)). Frontend requirements come from the portlet's spec (PR [#37927](https://github.com/dotCMS/core/pull/37927), `specs/37803-network-portlet/spec.md` at `dea8ed179c`).

**Input**: User description: "Give the Network (Beta) portlet a typed endpoint that covers its data needs. Fix the backend gaps the data depends on. Don't break the old endpoints, the old Configuration → Network tab, other callers of the shared code, or the OpenSearch migration."

## Problem Statement *(mandatory)*

The Network (Beta) portlet lists every dotCMS server in the cluster with its status, and its
cache, search and assets details. Two things stop it:

1. **No typed endpoint (#37875).** The only source is `GET /api/cluster/getNodesStatus/`, built
   for the old JSP tab:
   - servers that answered and servers that didn't use different keys and loose types (`""` for
     numbers, `"true/"` for a boolean);
   - it has no up / lagging / down status and no `@Schema`;
   - it only admits users with the `configuration` portlet.

   The old tab reads those keys by name and stays as the Beta's fallback, so the response can't
   be reshaped in place.
2. **The status report is wrong or missing in common setups (#37876, #37877).**
   - **OpenSearch-only installs (phase 3):** the report asks Elasticsearch directly, the call
     fails, and no server answers. Every server shows as down, and the same failure drops cache
     invalidations from other servers that arrived in the same batch.
   - **Phase 2:** the report shows Elasticsearch health while OpenSearch serves reads.
   - **A second Refresh within 1.5 s:** every status request is identical, and the pub/sub queue
     drops an identical message sent within 1.5 s. The request is never sent, and every server
     shows as not answering.
   - **`NullTransport`:** the old endpoint fails with a 500.

**Severity / Impact**:

- The portlet can't ship without the endpoint.
- On OpenSearch-only installs the old tab shows every server as down today, and cache
  invalidations can be lost while it is open. This gets worse as installs move through the
  migration.

## Reproduction *(mandatory)*

**Environment**: dotCMS `main`, two or more servers on the default PubSub cache transport,
logged in as a CMS Administrator.

**Steps to Reproduce**:

1. In migration phase 3 with `ES_ENDPOINTS` unset, open Configuration → Network, or call
   `GET /api/cluster/getNodesStatus/`.
2. In phase 2, call the same endpoint and read the search values.
3. Call the endpoint twice within 1.5 s.
4. With `CACHE_INVALIDATION_TRANSPORT_CLASS=NullTransport`, call the endpoint.
5. Look for a typed endpoint that lists the cluster's servers with a status, reachable with the
   `network-beta` portlet.

**Expected Behavior**:

- Steps 1 and 3: every running server answers.
- Step 2: the search values come from OpenSearch.
- Step 4: 200, with every server listed as not responding.
- Step 5: a typed endpoint exists.

**Actual Behavior**:

- Step 1: no server answers and all show as down; cache invalidations in the same batch are
  dropped.
- Step 2: the values come from Elasticsearch.
- Step 3: the second call shows every server as not answering.
- Step 4: 500 (`NullPointerException`).
- Step 5: none exists.

**Reproducibility**: Always, in the setups above.

## Scope of Investigation *(mandatory)*

- **Affected area**: cluster status reporting and its REST API.
  - **REST:** `ClusterResource` (`/api/cluster`), and a new v1 resource.
  - **Report:** `ClusterUtil.getNodeInfo` (enterprise).
  - **Request and reply:** `CacheTransportTopic` (the `CLUSTER_REQ` handler), `PubSubCacheTransport.validateCacheInCluster`.
  - **Pub/sub:** `JDBCPubSubImpl` notification handling.
  - **Search:** `IndexAPI`, `ESIndexAPI`, `OSIndexAPIImpl`, `IndexAPIImpl`.
  - **App configuration:** `ConfigurationHelper` (`cluster` section of `/api/v1/appconfiguration`).
  - **Portlet ids:** `PortletID` (legacy `com.dotmarketing.util`).
- **Suspected surface**: mostly modern `com.dotcms.*`. Legacy: `PortletID` only, by adding one
  value.
- **Live clients that must keep working**:
  - the old Configuration → Network tab and `GET /api/cluster/getNodesStatus/`;
  - the other callers of `getNodeInfo`: `NodeStatusServerAction` and the `x-dot-server` header;
  - every `CacheTransport` and `IndexAPI` implementation, including the ones in tests;
  - `/api/v1/appconfiguration` clients;
  - `PortletAPIImpl.isCustomContentPortlet`, which treats every `PortletID` value as a product
    portlet.
- **Related known decisions**:
  - The OpenSearch migration owns `IndexAPI`, `PhaseRouter`, the search clients and the phase
    configuration. The search change must fit into it without changing them.
  - Licensing: `LicenseUtil.getLevel()` always returns `PLATFORM` (since `7189abfb60`), so every
    install is licensed for clustering.

## Root-Cause Hypothesis

- **Search:** `ClusterUtil` calls `DotRestHighLevelClientProvider` directly instead of going
  through `IndexAPI`, so it ignores the migration phase.
- **No reply on failure:** the `CLUSTER_REQ` handler builds the report a second time outside its
  guard, and its fallback is an immutable map that fails when written to and carries the
  requesting server's id instead of its own (`CacheTransportTopic.java:121-128`). The error escapes to
  `JDBCPubSubImpl`, which has no per-notification guard, so the rest of the batch is dropped.
- **Dropped Refresh:** `DotPubSubEvent` has no unique field, and `QueuingPubSubWrapper` drops
  identical events within `PUBSUB_QUEUE_DEDUPE_TTL_MILLIS` (1.5 s). The handler also clears all
  collected replies on every request, so two overlapping requests wipe each other's replies.
- **`NullTransport`:** `validateCacheInCluster` returns `null`, and `ClusterResource` uses it
  without a check.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- **New endpoint `GET /api/v1/cluster/nodes`.** Its entity is `currentServerId` plus `nodes[]`,
  the alive servers in database order (`startup_time`, then `server_id`).
  - **Identity, for every node, from the server's database row:** `serverId`, `displayServerId`
    (`LicenseManager.getDisplayServerId`), `name`, `clusterName` (`cluster_server.cluster_id`),
    `ipAddress`, `version` (`cluster_server.key_`), `lastContactSeconds` (never null: the alive
    list only holds servers with a heartbeat; never negative: when server or database clocks
    differ and the age comes out negative, it is reported as 0).
  - **`responded`:** the server answered this call's status request within the wait window.
  - **`status`**, with `threshold` = `CLUSTER_NODE_STALE_HEARTBEAT_SECONDS` (default
    3 × `SERVER_HEARTBEAT_RUN_EVERY_SECONDS`, 180 s):
    - `UP`: responded, and `lastContactSeconds` ≤ threshold;
    - `LAGGING`: responded with `lastContactSeconds` > threshold, or not responded with
      `lastContactSeconds` ≤ threshold;
    - `DOWN`: not responded, and `lastContactSeconds` > threshold; or responded with a failed
      assets check (`assetsStatus` red), whatever the heartbeat. The legacy Network tab shows
      that server red, so the new screen must not show it as up.

    A server that keeps its heartbeat but can't build its report never replies, so it stays
    `LAGGING`; it becomes `DOWN` only once its heartbeat is older than the threshold.
  - **Typed sections:**
    - `cache`: `clusterName`, `transport`, `numberOfNodes`, `open`, `address`, `port`,
      `receivedBytes`, `sentBytes`.
      - `transport` comes from the report's `cacheTransportClass` (for example `PubSub`).
      - `address` is never read from the report's `ipAddress`, which holds the node IP. Neither
        existing transport (PubSub, Null) has an address, so it is `null`.
      - `port` comes from `port`, with `-1` becoming `null`.
    - `search`: `engine` (`ELASTICSEARCH` / `OPENSEARCH`), `clusterName`, `timedOut`, the
      node and shard counts, `numberOfPendingTasks`, `numberOfInFlightFetch`,
      `taskMaxWaitingInQueueMillis` (milliseconds), `activeShardsPercent`;
    - `assets`: `path`, `canRead`, `canWrite`.

    `null` means unknown; there are no empty strings and no `-1`.
  - **Access:** `configuration` or `network-beta` in a layout, or a CMS Administrator. No
    license check. Every other caller gets 401: anonymous, front-end-only, and back-end users who
    have neither portlet and aren't CMS Administrators.
  - **Errors:** if the alive servers can't be read from the database, the call fails with 500; it
    never returns 200 with an empty `nodes`.
  - **One cluster status request per call.**
  - **Docs:** `@Schema` matching the return type.
- **`PortletID.NETWORK_BETA`** (`network-beta`), documented as the Beta-period alias.
- **`heartbeatEnabled`** (from `ENABLE_SERVER_HEARTBEAT`, default `true`) added to the `cluster`
  section of `/api/v1/appconfiguration`, for backend users only. The anonymous and front-end
  response is unchanged.
- **Search health through `IndexAPI`:**
  - one new `default` read method, overridden in `ESIndexAPI`, `OSIndexAPIImpl` and
    `IndexAPIImpl`;
  - routed with `router.read(...)` like other reads;
  - both engines ask for the health of this install's indices only (`getClusterPrefix() + "*"`,
    the filter `OSIndexAPIImpl.getClusterHealth()` already uses), so another install's indices on
    a shared cluster can't change our status or shard counts. The status and the active, primary,
    relocating, initializing and unassigned shard counts cover only our indices; the cluster name,
    node and data-node counts, pending tasks, task wait and delayed unassigned shards come from
    the whole cluster. The active-shards percentage is the engine's own figure: 100 while our
    indices are green, otherwise computed over the whole cluster. Elasticsearch keeps the same
    `ES_INDEX_OPERATIONS_TIMEOUT` parameter;
  - when no index matches yet, the health result is `green` (the legacy `esStatus`) with zero
    shard counts;
  - **by migration phase:** `nodes` always lists the dotCMS servers, never search-engine nodes. The
    `search` section shows the engine that serves reads: Elasticsearch in phases 0–1, OpenSearch in
    phases 2–3, and Elasticsearch when the phase 2 fallback answered (`search.engine` says which).
    The engine that only receives dual writes is not shown, for any user. In phase 2 this
    deliberately differs from the Indices portlet, which keeps showing Elasticsearch indices until
    phase 3; the Indices portlet is unchanged;
  - each engine names itself in its result and throws on failure;
  - each attempt of an engine's health call has a client-side time limit,
    `CLUSTER_SEARCH_HEALTH_TIMEOUT_MILLIS` (default 750 ms). With one host per engine, a hung
    engine still leaves the reply inside the default wait window (at least 2 s), including the
    phase 2 fallback to Elasticsearch. The limit is per attempt: the search client tries each
    configured host in turn, so when every configured host hangs, or both engines hang in phase 2,
    the reply can arrive after the wait window and the server is listed as not responding for that
    check. The search client providers and `PhaseRouter` are unchanged.
- **`ClusterUtil.getNodeInfo`:**
  - uses that method;
  - on a search failure, empties only the search keys (`esEngine` included), sets
    `esStatus=red` and logs a warning;
  - adds `esEngine` and `esTaskMaxWaitingTimeMillis` (milliseconds). Existing keys and values
    are unchanged, apart from the index scoping above (AC-020).
- **`CLUSTER_REQ` handler:** builds the report once. If building fails, the failure is logged
  and no reply is sent, so the requester lists the server as not responding, as it always has.
- **Request id:** each status request carries one, and replies are collected by it.
  - A server only collects replies for request ids it registered before sending. Replies with no
    id or an unknown id are dropped.
  - The request's entry is removed when the wait ends.
  - A request no longer clears collected replies.
- **`JDBCPubSubImpl`:** one handler error no longer drops the rest of a notification batch.
- **Old endpoint:** a `null` from `validateCacheInCluster` is treated as no replies.
- **Docs and tests:** `openapi.yaml` regenerated; unit, integration and Postman tests.

**Response contract** (`GET /api/v1/cluster/nodes`, standard `ResponseEntityView` envelope):

```json
{
  "entity": {
    "currentServerId": "ed54e574-717a-4b47-ba6c-758ff23b3e1b",
    "nodes": [
      {
        "serverId": "ed54e574-717a-4b47-ba6c-758ff23b3e1b",
        "displayServerId": "ed54e574",
        "name": "dotcms-node-1",
        "clusterName": "79bd147f91",
        "ipAddress": "172.17.0.5",
        "version": "26.10.01",
        "lastContactSeconds": 34,
        "responded": true,
        "status": "UP",
        "cache": {
          "clusterName": "79bd147f91", "transport": "PubSub", "numberOfNodes": 2, "open": true,
          "address": null, "port": null, "receivedBytes": 2275, "sentBytes": 2275
        },
        "search": {
          "engine": "OPENSEARCH", "clusterName": "os-cluster", "timedOut": false,
          "numberOfNodes": 1, "numberOfDataNodes": 1, "activePrimaryShards": 2, "activeShards": 2,
          "relocatingShards": 0, "initializingShards": 0, "unassignedShards": 0,
          "delayedUnassignedShards": 0, "numberOfPendingTasks": 0, "numberOfInFlightFetch": 0,
          "taskMaxWaitingInQueueMillis": 0, "activeShardsPercent": 100.0
        },
        "assets": { "path": "/data/shared/assets", "canRead": true, "canWrite": true }
      },
      {
        "serverId": "1c2d3e4f-0000-4000-8000-000000000002",
        "displayServerId": "1c2d3e4f",
        "name": "dotcms-node-2",
        "clusterName": "79bd147f91",
        "ipAddress": "172.17.0.6",
        "version": "26.10.01",
        "lastContactSeconds": 302,
        "responded": false,
        "status": "DOWN",
        "cache": null,
        "search": null,
        "assets": null
      }
    ]
  },
  "errors": [], "i18nMessagesMap": {}, "messages": [], "pagination": null, "permissions": []
}
```

| Field | Type | Null? |
|---|---|---|
| `currentServerId` | string | never |
| `nodes` | array, alive servers in database order | never (may be empty) |
| `serverId`, `displayServerId` | string | never |
| `name`, `clusterName`, `ipAddress`, `version` | string | when the database row has none |
| `lastContactSeconds` | integer (seconds, ≥ 0) | never |
| `responded` | boolean | never |
| `status` | `UP` / `LAGGING` / `DOWN` | never |
| `cache`, `search`, `assets` | object | when `responded` is `false` |
| `cache.clusterName`, `cache.transport`, `cache.address` | string | yes; `address` is always `null` on PubSub |
| `cache.numberOfNodes`, `cache.port` | integer | yes; `port` is always `null` on PubSub |
| `cache.open` | boolean | yes |
| `cache.receivedBytes`, `cache.sentBytes` | integer | yes |
| `search.engine` | `ELASTICSEARCH` / `OPENSEARCH` | yes (unknown, or the search check failed) |
| `search.clusterName` | string | yes |
| `search.timedOut` | boolean | yes |
| `search` shard and node counts, `numberOfPendingTasks`, `numberOfInFlightFetch` | integer | yes |
| `search.taskMaxWaitingInQueueMillis` | integer (milliseconds) | yes |
| `search.activeShardsPercent` | number (0–100) | yes |
| `assets.path` | string | yes |
| `assets.canRead`, `assets.canWrite` | boolean | yes |

Inside a section, `null` means unknown; when a server answered but its search check failed, every
`search` value is `null`. Errors: 401 for a caller without access, and 500 if the servers can't be
read.

**Explicitly out of scope / non-goals**:

- **The old endpoint's response:** its types, keys and id algorithms stay as they are, including
  `esTaskMaxWaitingInQueueMillis` (`"0s"`). The old tab reads them.
- **A license check** and an unlicensed state.
- **`PortletID.NETWORK`** and accepting `network`. Added when the Network portlet is promoted.
- **The rest of #37877:** building the report off the pub/sub listener thread, the serving server
  reporting on itself in-process, and the `startup` report value. The Network portlet doesn't
  need them.
- **Data the frontend spec doesn't show:**
  - `licenseId`, `host`, `clusterHealth`;
  - per-section status;
  - cache provider and message counters;
  - node count;
  - `uptime`, `contacted`, `heartbeat`, `startup`, `assetsTestPath`.
- **Migration code:** `PhaseRouter`, the search client providers and their timeouts,
  `IndexConfigHelper`, startup validation, and the existing `getClusterHealth()` and
  `getClusterStats()` are not changed.

## Regression Risk *(mandatory)*

- **Blast radius**:
  - the old Network tab and `GET /api/cluster/getNodesStatus/`;
  - cache invalidation, which shares the pub/sub channel and the `CacheTransportTopic` handler
    thread;
  - the other `JDBCPubSubImpl` topics (`ClusterManagementTopic`, `OsgiRestartTopic`);
  - `NodeStatusServerAction` and the `x-dot-server` header;
  - search reads in each migration phase;
  - `/api/v1/appconfiguration`;
  - `PortletAPIImpl.isCustomContentPortlet`.
- **Backward compatibility**:
  - **Old endpoint:** same access and the same keys and values, except that `esStatus`, the
    shard values and the active-shards percentage now cover only this install's indices, which differs from before only on a
    cluster shared with other installs. Each answered server's entry
    gains `esEngine`, `esTaskMaxWaitingTimeMillis` and `clusterRequestId` (a reply carries the
    request's payload, as it already carries the pub/sub keys `t`, `top` and `o`). The old tab
    reads keys by name and ignores them.
  - **`CacheTransport.validateCacheInCluster(int)`:** keeps its signature and return shape.
  - **Rolling upgrades:**
    - a reply is built from the request, so older servers echo the request id unchanged;
    - newer servers answer requests from older ones;
    - old-format values the new endpoint can't convert become `null`.
  - **`IndexAPI`:** the new method is a `default`, so every other implementation compiles
    unchanged.
  - **`appconfiguration`:** only adds a key. `clusterId`, `companyKeyDigest` and their
    visibility rules are unchanged.
  - **`PortletID`:** only `network-beta` is added, so no existing id is reclassified.
  - **Rollback:** no schema, index-mapping or stored-data change, so the change is
    rollback-safe.
- **Data considerations**: none.

## Acceptance & Verification *(mandatory)*

*New endpoint `GET /api/v1/cluster/nodes` (#37875):*

- **AC-001**: The endpoint returns 200 with a `ResponseEntityView` whose entity has
  `currentServerId` (the server that handled the request) and `nodes`. If the alive servers
  can't be read from the database, it returns 500, never 200 with an empty `nodes`.
- **AC-002**: `nodes` holds exactly the servers from `ServerAPI.getAliveServers`, in that order,
  whether or not they answered. A reply from a server not in that list does not appear.
- **AC-003**: Every node, answered or not, has `serverId`, `displayServerId`, `name`,
  `clusterName`, `ipAddress`, `version` and `lastContactSeconds`, all from its database row.
  - `displayServerId` comes from `LicenseManager.getDisplayServerId`.
  - `lastContactSeconds` is never negative: an age below zero, caused by server or database
    clocks differing, is reported as 0.
- **AC-004**: `responded` is `true` only when the server's reply to this call's request arrived
  within the wait window.
- **AC-005**: `status` follows the rule in Fix Scope at its boundaries. A server that answered
  with a failed assets check is `DOWN`; no other report value changes `status`.
- **AC-006**: The sections have the fields and types in Fix Scope.
  - A server that didn't answer has `cache`, `search` and `assets` set to `null`.
  - A server that answered never has an empty string or `-1`.
  - `cache.transport` is `PubSub` on the PubSub transport.
  - `cache.address` and `cache.port` are `null` on PubSub; `cache.address` is never the node IP.
  - `assets.canRead` and `assets.canWrite` are booleans.
  - `search.taskMaxWaitingInQueueMillis` is milliseconds.
- **AC-007**: When a server answered but its search health failed:
  - every `search` value is `null`, `engine` included;
  - `cache` and `assets` are filled;
  - `status` is unaffected.
- **AC-008**: A value from an older server that can't be converted (for example `"0s"`) becomes
  `null`, and the call still succeeds.
- **AC-009**: Each call sends exactly one cluster status request.
- **AC-010**: The `@Schema` matches the return type, and `openapi.yaml` is regenerated and
  committed.

*Access:*

- **AC-011**: Access works as follows; there is no license check.
  - A backend user with `configuration` or `network-beta` in a layout gets 200.
  - A CMS Administrator with neither gets 200.
  - Any other backend user gets 401.
  - Anonymous and front-end-only users get 401.

*App configuration and portlet ids:*

- **AC-012**: For a backend user, `GET /api/v1/appconfiguration` has `cluster.heartbeatEnabled`,
  a boolean from `ENABLE_SERVER_HEARTBEAT` (default `true`). With `ENABLE_SERVER_HEARTBEAT=false`,
  `cluster.heartbeatEnabled` is `false` and `GET /api/v1/cluster/nodes` still lists the alive
  servers, built the same way.
- **AC-013**: The rest of that response is unchanged, including `cluster.clusterId`,
  `cluster.companyKeyDigest` and who sees the digest. For anonymous and front-end users the
  `cluster` section is exactly as before, without `heartbeatEnabled`.
- **AC-014**: `PortletID` has `NETWORK_BETA` (`network-beta`) and not `NETWORK`.
- **AC-015**: `PortletAPIImpl.isCustomContentPortlet` gives the same result as before for every
  existing portlet id.

*Status report (#37876):*

- **AC-016**: The search values come from the engine serving reads in the current phase, through
  `IndexAPI`, and `esEngine` names the engine that answered.
  - Phases 0 and 1: Elasticsearch.
  - Phases 2 and 3: OpenSearch.
  - Elasticsearch when the phase 2 fallback answered.

  The engine that only receives dual writes is never shown.

  Both engines report the health of this install's indices only, as described in Fix Scope.
  `DotRestHighLevelClientProvider` is no longer used by `ClusterUtil`.
- **AC-017**: In phase 3 with `ES_ENDPOINTS` unset, every server answers, with search values
  from OpenSearch.
- **AC-018**: When the search health call fails, including when it takes longer than the time
  limit:
  - only the search keys are emptied, `esEngine` included, and `esStatus` is `red`;
  - the cache and assets keys are filled;
  - the server still answers.
- **AC-019**: The migration code listed in the non-goals is unchanged in behaviour, and the new
  `IndexAPI` method is a `default`. The status report logs one warning when the search health
  call fails (the failure used to be logged when it escaped the report). In phase 2, an OpenSearch
  failure or timeout also produces `PhaseRouter`'s existing fallback ERROR before Elasticsearch is
  asked: the router's existing logging, which this call now goes through like every other routed
  read.
- **AC-020**: The existing report keys keep their names and values. Only `esEngine` and
  `esTaskMaxWaitingTimeMillis` are added to the report. One exception: `esStatus`, the shard
  values and the active-shards percentage cover only this install's indices, so on a cluster shared with other installs they can
  differ from before.
- **AC-021**: The `CLUSTER_REQ` handler builds the report once and its reply echoes the request
  id. If building fails, the failure is logged and no reply is sent, so the server is listed as
  not responding on both screens.
- **AC-022**: If a pub/sub topic handler throws, `JDBCPubSubImpl` still processes the rest of the
  batch, including cache invalidations.
- **AC-023**: `NodeStatusServerAction` and the `x-dot-server` header keep working.

*Request correlation (#37877):*

- **AC-024**: Each status request carries a unique request id, and replies are collected per
  request id.
  - Only request ids this server registered before sending are collected; replies with no id or
    an unknown id are dropped.
  - The request's entry is removed when the wait ends.
  - A request no longer clears collected replies.
- **AC-025**: Two requests from the same server less than 1.5 s apart are both sent, and each
  gets a reply from every answering server. The same holds for simultaneous requests from
  different servers. This holds once every server runs the new version: until then, an old
  server still clears its collected replies on every request.
- **AC-026**: Mid-upgrade, servers on the old version answer new requests, and servers on the
  new version answer old ones.

*Old endpoint `GET /api/cluster/getNodesStatus/`:*

- **AC-027**: With `NullTransport`, it returns 200 and lists every alive server as not
  responding.
- **AC-028**: Otherwise it returns the same structure (`myServerId`, `clusterHealth`,
  `serverInfo`) with every existing key and value unchanged, apart from the index scoping in
  AC-020. Each answered server's entry only
  gains `esEngine`, `esTaskMaxWaitingTimeMillis` and `clusterRequestId`.
- **AC-029**: Its access rule (`configuration` portlet) is unchanged. The other `/api/cluster`
  endpoints are untouched.
- **AC-030**: `CacheTransport.validateCacheInCluster(int)` keeps its signature and return shape.

**Verification method**:

- **Unit tests** for:
  - the status rule at its boundaries, including the failed-assets case (AC-005);
  - the conversion of current and old-format reports (AC-006 to AC-008);
  - the answered and unanswered node shape, and one status request per call (AC-002 to AC-004,
    AC-009);
  - a database failure reading the alive servers gives 500, not an empty list (AC-001);
  - the phase 2 fallback, and OpenSearch failures surfacing as unchecked exceptions so the
    fallback can fire (AC-016). That `IndexAPIImpl` never calls the Elasticsearch implementation
    in phase 3 proves AC-017 without `ES_ENDPOINTS`;
  - the handler: one build, the request id echoed, and no reply when the report fails (AC-021);
  - the per-notification guard (AC-022);
  - each engine's health request is limited to this install's indices, and with no matching
    index it reports `green` with zero shard counts (AC-016, AC-020);
  - a search engine that accepts the connection but never answers: the call gives up at the time
    limit, and the server still answers with an empty search section (AC-018);
  - request id collection, and two status checks under 1.5 s apart answered the way an
    older-version server answers (AC-024 to AC-026).
- **Integration tests**, appended to a `MainSuite` free of `Config.CONTEXT` leakers:
  - access for an admin, a `network-beta` user, a user with neither portlet, and an anonymous
    caller (AC-011);
  - the endpoint's shape for a server that didn't answer (AC-001, AC-003, AC-006);
  - `heartbeatEnabled` for back-end users only, and `false` when the flag is off (AC-012, AC-013);
  - `getNodeInfo` in phase 0 (AC-016, AC-020), and with the search engine unreachable (AC-018);
  - the old endpoint with `NullTransport` (AC-027).

  In the integration environment no server answers status requests (its cache transport is never
  started), and this server has no `cluster_server` row until a test creates it the way a
  running server does. Tests that change the migration phase restore it afterwards.
- **Postman** collection for `GET /api/v1/cluster/nodes`.
- **CI OpenAPI check** for AC-010.
- **Code review** for:
  - AC-014 and AC-015: only `network-beta` is added;
  - AC-012: the endpoint doesn't read `ENABLE_SERVER_HEARTBEAT`, so `nodes` is built the same way
    with the flag off;
  - AC-019: the migration code is unchanged, and the report adds only the one warning;
  - AC-028 to AC-030: the old endpoint is unchanged;
  - AC-023: the other callers.

## Assumptions

- Every install reports `PLATFORM` (`LicenseUtil.getLevel()`), so the endpoint has no license
  check. If licensing is restored, this needs revisiting.
- `CLUSTER_NODE_STALE_HEARTBEAT_SECONDS` is configured below `HEARTBEAT_TIMEOUT` (default 600 s), the
  age after which a server leaves the alive list. Otherwise a server that stops answering never
  becomes `DOWN`.
- `CLUSTER_SERVER_THREAD_SLEEP`, if configured, stays at least `CLUSTER_SEARCH_HEALTH_TIMEOUT_MILLIS`
  plus 1 s (the pub/sub listener picks up the request and the reply up to 0.5 s late each), so a
  single hung search engine still fits in the wait window.
- Each server's `sitelic` row is its own heartbeat row (a placeholder license under the server's
  id); nothing checks its content. Every server creates it on startup and the heartbeat recreates it
  if it's missing, so a running server is missing from `nodes` only while it has no recent
  heartbeat.
- The wait window for replies is the one the legacy endpoint already uses:
  `CLUSTER_SERVER_THREAD_SLEEP`, default `max(2000 ms, 1000 ms × alive servers)`. A call returns
  early once it has as many replies as there are alive servers.
- The server heartbeat runs whether or not `ENABLE_SERVER_HEARTBEAT` is on: since #19291 (2020)
  the heartbeat job is scheduled unconditionally, so `nodes` is built the same way either way. The
  flag now turns off the dead-server cleanup, the `heartbeat.dat` file and the old Network tab, and
  `cluster.heartbeatEnabled` reports it.
- The Network portlet ships as `network-beta` during the Beta; `network` is added when it is
  promoted.
- A reply keeps being built from its request (`new DotPubSubEvent.Builder(event)`), so servers
  on the previous version echo the request id during a rolling upgrade.
