# Data model: OpenSearch reindex byte limit (#37905)

No persisted data changes. Two in-memory models:

## Byte limit (resolved once per `createBulkProcessor`)

| Field | Type | Meaning |
|---|---|---|
| `maxBytes` | `long` | Limit in characters-as-bytes; `≤ 0` means disabled (count-only) |
| source | — | Resolution table in [research.md](research.md) R2 |

Property: `OS_REINDEX_BULK_SIZE_MB` (int, MB) → fallback `REINDEX_THREAD_ELASTICSEARCH_BULK_SIZE`
(int, MB, shipped 10) → default 10. Only an explicit OS value ≤ 0 disables; it logs one `WARN`
naming the property and value.

## Pending batch (`OSIndexBulkProcessor`)

| Field | Type | Meaning |
|---|---|---|
| `pending` | `List<BulkOperation>` | Operations not yet sent (existing) |
| `pendingSize` | `long` | Sum of estimated sizes of `pending` (new) |
| `maxActions` | `int` | Count limit (existing, `REINDEX_THREAD_ELASTICSEARCH_BULK_ACTIONS` ÷ servers) |
| `maxBytes` | `long` | Byte limit (new) |

Transitions: add → (flush first if it would overflow) → append and add size → flush if count or
bytes reached. `flush()` sends `pending`, clears it and resets `pendingSize` to 0.
