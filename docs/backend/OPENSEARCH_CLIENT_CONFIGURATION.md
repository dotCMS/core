# OpenSearch Client Configuration Reference

Configuration properties consumed by `ConfigurableOpenSearchProvider` (and its
supporting classes). All properties are read via `IndexConfigHelper`, which
applies a two-step fallback chain:

1. Try the **`OS_*`** key in `dotmarketing-config.properties` (or any
   dotCMS `Config` source).
2. If absent **and** an ES fallback exists, try the corresponding **`ES_*`** key.
3. If still absent, use the **hard-coded default** shown in the table below.

Properties with no ES fallback column have no Elasticsearch equivalent; the
caller default is used directly if the OS key is missing.

---

## Endpoints

| Property | ES Fallback | Type | Default | Description |
|---|---|---|---|---|
| `OS_ENDPOINTS` | _(none)_ | `String[]` (comma-separated URLs) | Derived from `OS_HOSTNAME`/`OS_PROTOCOL`/`OS_PORT` | Full endpoint URLs, e.g. `http://localhost:9201,http://node2:9201`. **Takes priority over host/protocol/port properties when present.** |
| `OS_HOSTNAME` | `ES_HOSTNAME` | `String` | `localhost` | OpenSearch host used when `OS_ENDPOINTS` is not set. |
| `OS_PROTOCOL` | `ES_PROTOCOL` | `String` (`http` \| `https`) | `https` | Protocol used when building the default endpoint. |
| `OS_PORT` | `ES_PORT` | `int` | `9201` | Port used when building the default endpoint. |

> **Note:** `OS_ENDPOINTS` is the recommended way to configure multi-node
> clusters. When set, the `OS_HOSTNAME`/`OS_PROTOCOL`/`OS_PORT` properties are
> ignored.

---

## Authentication

| Property | ES Fallback | Type | Default | Description |
|---|---|---|---|---|
| `OS_AUTH_TYPE` | `ES_AUTH_TYPE` | `String` (`BASIC` \| `JWT` \| `CERT`) | `BASIC` | Authentication mechanism. |
| `OS_AUTH_BASIC_USER` | `ES_AUTH_BASIC_USER` | `String` | _(none)_ | Username for `BASIC` auth. Required when `OS_AUTH_TYPE=BASIC`. |
| `OS_AUTH_BASIC_PASSWORD` | `ES_AUTH_BASIC_PASSWORD` | `String` | _(none)_ | Password for `BASIC` auth. Required when `OS_AUTH_TYPE=BASIC`. |
| `OS_AUTH_JWT_TOKEN` | `ES_AUTH_JWT_TOKEN` | `String` | _(none)_ | Bearer token for `JWT` auth. Required when `OS_AUTH_TYPE=JWT`. Added as `Authorization: Bearer <token>` header on every request. |

> **`CERT` auth:** `OS_TLS_CLIENT_CERT` + `OS_TLS_CLIENT_KEY` are used (see TLS
> section). Certificate-based mTLS is partially implemented — currently falls
> back to trust-self-signed strategy.

---

## TLS / SSL

| Property | ES Fallback | Type | Default | Description |
|---|---|---|---|---|
| `OS_TLS_ENABLED` | `ES_TLS_ENABLED` | `boolean` | `false` | Explicitly enables TLS. Not required when `OS_ENDPOINTS` already uses `https://` — TLS is activated automatically from the endpoint scheme. |
| `OS_TLS_CERT_REQUIRED` | _(none)_ | `boolean` | `false` | When `true`, enforces certificate and hostname verification. Disabled by default — set to `true` only when the server certificate is trusted by the JVM truststore or `OS_TLS_CA_CERT` is configured. |
| `OS_TLS_TRUST_SELF_SIGNED` | _(none)_ | `boolean` | `false` | When `true`, accepts self-signed server certificates without a CA chain. Useful for local/dev clusters. |
| `OS_TLS_CLIENT_CERT` | _(none)_ | `String` (path) | _(none)_ | Path to the PEM client certificate for mutual TLS (mTLS / `CERT` auth). |
| `OS_TLS_CLIENT_KEY` | _(none)_ | `String` (path) | _(none)_ | Path to the PEM client private key for mutual TLS. |
| `OS_TLS_CA_CERT` | _(none)_ | `String` (path) | _(none)_ | Path to the PEM CA certificate used to verify the server. |

---

## Connection Pool & Timeouts

| Property | ES Fallback | Type | Default | Description |
|---|---|---|---|---|
| `OS_CONNECTION_TIMEOUT` | _(none)_ | `int` (milliseconds) | `10000` | Maximum time to establish a TCP connection. |
| `OS_SOCKET_TIMEOUT` | _(none)_ | `int` (milliseconds) | `30000` | Maximum time to wait for a server response (socket read timeout). |
| `OS_MAX_CONNECTIONS` | _(none)_ | `int` | `100` | Total maximum HTTP connections in the connection pool. |
| `OS_MAX_CONNECTIONS_PER_ROUTE` | _(none)_ | `int` | `50` | Maximum HTTP connections per route (per host:port pair). |
| `OS_CONNECTION_ATTEMPTS` | `ES_CONNECTION_ATTEMPTS` | `int` | `24` | Number of connection attempts before startup gives up waiting for the cluster to become available. |
| `OS_CONNECTION_RETRY_SLEEP_SECONDS` | _(none)_ | `int` (seconds) | `5` | Sleep duration between consecutive connection attempts during startup. |

---

## Index Management

| Property | ES Fallback | Type | Default | Description |
|---|---|---|---|---|
| `OS_INDEX_OPERATIONS_TIMEOUT` | `ES_INDEX_OPERATIONS_TIMEOUT` | `String` (time value, e.g. `15s`) | `15s` | Timeout for index management operations (create, delete, open, close). |
| `opensearch.index.number_of_shards` | `es.index.number_of_shards` | `int` | `1` | Number of primary shards for new indices. dotCMS recommends `1` unless distributing across multiple disks. |
| `OS_INDEX_AUTO_EXPAND_REPLICAS` | `ES_INDEX_AUTO_EXPAND_REPLICAS` | `String` (range, e.g. `0-1`) | `0-1` | Auto-expand replicas setting applied to new indices. |
| `OS_INDEX_REPLICAS` | `ES_INDEX_REPLICAS` | `int` | _(none)_ | Fixed replica count (overrides auto-expand when set). |
| `OS_INDEX_QUERY_DEFAULT_FIELD` | `ES_INDEX_QUERY_DEFAULT_FIELD` | `String` | `catchall` | Default field for queries that do not specify a field. |
| `OS_INDEX_NAME` | `ES_INDEX_NAME` | `String` | _(none)_ | Optional suffix appended to generated index names. |

---

## Scroll API

| Property | ES Fallback | Type | Default | Description |
|---|---|---|---|---|
| `OS_SCROLL_KEEP_ALIVE_MINUTES` | `ES_SCROLL_KEEP_ALIVE_MINUTES` | `int` (minutes) | `5` | How long the server keeps the scroll context alive between page fetches. |
| `OS_SCROLL_BATCH_SIZE` | `ES_SCROLL_BATCH_SIZE` | `int` | `1000` | Number of documents returned per scroll page. |

---

## Bulk API

| Property | ES Fallback | Type | Default | Description |
|---|---|---|---|---|
| `OS_BULK_TIMEOUT` | `ES_BULK_TIMEOUT` | `int` (milliseconds) | `30000` | Timeout for bulk indexing requests. |
| `OS_BULK_BATCH_SIZE` | `ES_BULK_BATCH_SIZE` | `int` | `100` | Maximum number of documents per bulk request. |

---

## Query Behaviour

| Property | ES Fallback | Type | Default | Description |
|---|---|---|---|---|
| `OS_USE_FILTERS_FOR_SEARCHING` | `ELASTICSEARCH_USE_FILTERS_FOR_SEARCHING` | `boolean` | `true` | Use filter context (no scoring) instead of query context for content searches. Improves performance when `_score` sorting is not needed. Disable only if you sort by relevance score. |
| `OS_TRACK_TOTAL_HITS` | _(none)_ | `int` | _(none)_ | Maximum number of hits to track accurately (`track_total_hits`). OpenSearch-specific; no ES equivalent. |
| `OS_CACHE_SEARCH_QUERIES` | _(none)_ | `boolean` | _(none)_ | Enable in-memory caching of search query results. OpenSearch-specific. |

---

## Minimal Configuration Example

```properties
# dotcms-config-cluster.properties (or dotmarketing-config.properties)

# ---- OpenSearch 3.x — separate instance ----
OS_ENDPOINTS=http://localhost:9201
OS_AUTH_TYPE=BASIC
OS_AUTH_BASIC_USER=admin
OS_AUTH_BASIC_PASSWORD=admin
OS_TLS_ENABLED=false
```

## HTTPS Without a Certificate (Private CA / Internal PKI)

Certificate verification is skipped by default (`OS_TLS_NO_CERT_CHECK=true`), so no additional
configuration is needed when the OpenSearch server uses HTTPS with a private CA or self-signed cert.

```properties
OS_ENDPOINTS=https://opensearch-host:9200
OS_AUTH_TYPE=BASIC
OS_AUTH_BASIC_USER=admin
OS_AUTH_BASIC_PASSWORD=secret
```

To enforce strict certificate validation, set `OS_TLS_CERT_REQUIRED=true` and ensure the server
certificate is trusted by the JVM truststore (or configure `OS_TLS_CA_CERT`).

> **Note:** `OS_TLS_CERT_REQUIRED=true` requires a valid CA chain in the JVM truststore or an
> explicit `OS_TLS_CA_CERT` path. Misconfigured trust will cause a `PKIX path building failed` error.

## Full Configuration Example (HTTPS + mTLS)

```properties
OS_ENDPOINTS=https://os-node1:9200,https://os-node2:9200
OS_AUTH_TYPE=CERT
OS_TLS_ENABLED=true
OS_TLS_CLIENT_CERT=certs/client.pem
OS_TLS_CLIENT_KEY=certs/client.key
OS_TLS_CA_CERT=certs/root-ca.pem

# Connection pool tuning
OS_CONNECTION_TIMEOUT=5000
OS_SOCKET_TIMEOUT=60000
OS_MAX_CONNECTIONS=200
OS_MAX_CONNECTIONS_PER_ROUTE=100

# Index tuning
OS_INDEX_OPERATIONS_TIMEOUT=30s
opensearch.index.number_of_shards=1
OS_INDEX_AUTO_EXPAND_REPLICAS=0-1
```

---

## Fallback Resolution Logic

```
IndexConfigHelper.getString(OSIndexProperty.AUTH_TYPE, "BASIC")
  └─ 1. Config.getStringProperty("OS_AUTH_TYPE", null)    → if set, return it
  └─ 2. Config.getStringProperty("ES_AUTH_TYPE", null)    → if set, return it
  └─ 3. return "BASIC"                                    → hard-coded default
```

Properties with `esFallback = null` (e.g. `OS_TLS_TRUST_SELF_SIGNED`) skip
step 2 and go straight to the caller default.

---

## Related Classes

| Class | Role |
|---|---|
| `ConfigurableOpenSearchProvider` | Reads all properties and builds the `OpenSearchClient` |
| `OSIndexProperty` | Enum — single source of truth for all `OS_*` / fallback key names |
| `IndexConfigHelper` | Fallback resolution engine (`getString`, `getInt`, `getBoolean`) |
| `OSClientConfig` / `ImmutableOSClientConfig` | Immutable value object carrying the resolved configuration |
| `IndexStartupValidator` | Validates OS version (3.x) and endpoint separation from ES at startup |
