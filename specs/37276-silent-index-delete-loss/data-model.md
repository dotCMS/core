# Data Model: Silent index delete loss

No schema change. This feature uses `dist_reindex_journal` as it already exists; what changes
is which rows get written and how a batch of them is assembled.

## Entities

### `dist_reindex_journal` (existing table)

The durable record of index work owed. Written inside the transaction that changes content;
drained and retried by `ReindexThread` until the index acknowledges, then deleted.

| Column | Used by this feature | Notes |
|--------|----------------------|-------|
| `id` | yes — ordering | Monotonic per insert. The only reliable ordering signal between two entries for the same identifier. |
| `ident_to_index` | yes — batch key | Contentlet identifier. |
| `inode_to_index` | no | Set to the identifier by the existing enqueue paths. |
| `priority` | yes | Encodes both dispatch priority and retry count (`ReindexEntry.errorCount()` = `priority % 100`). |
| `dist_action` | **yes — the load-bearing field** | `ReindexAction.ordinal()`: `NONE=0`, `REINDEX=1`, `DELETE=2`. Already written by `addIdentifierDelete`, already read back, already consumed by both providers. |
| `index_val` | no | Failure cause, set by `markAsFailed`. |
| `serverid` | no | Cleared on failure so another node can pick it up. |
| `time_entered` | no | |

**Validation rule that must hold**: a row with `dist_action = DELETE` refers to content that no
longer exists in the database. Any query that assembles or filters journal rows must therefore
not join to `identifier` or `contentlet` — such a join would silently discard exactly these
rows. The current drain query (`ReindexQueueFactory:329-338`) is a plain select and satisfies
this; it must stay that way.

### `ReindexEntry` (existing immutable value object)

| Field | In equality? | Notes |
|-------|--------------|-------|
| `identToIndex` | yes | |
| `priority` | yes | |
| `isDelete` | **yes** | `@Value.Default false`. This is why a REINDEX and a DELETE entry for the same identifier are *not* equal. |
| `serverId` | yes | |
| `id` | no — `@Value.Auxiliary` | |
| `lastResult` | no — `@Value.Auxiliary` | |
| `timeEntered` | no — `@Value.Auxiliary` | |

No change to this type is required.

## The batch key — the one modelling decision

`ReindexQueueFactory.findContentToReindex` assembles a batch as
`Map<String, ReindexEntry>` keyed by `identToIndex` alone. Two entries for the same identifier
that differ only in `isDelete` are unequal (so the duplicate-drain loop below the `put` does
not collapse them) yet collide on the key, and the later `poll()` silently overwrites the
earlier.

**Decision: collapse to the newest entry per identifier, using `id` as the ordering signal.**

The rationale is semantic, not mechanical. Two journal entries for one identifier are not
independent work items — they are successive statements about what the index should hold for
that identifier, and only the last one is true. A DELETE written after a REINDEX means the
content is gone; applying the REINDEX afterwards would re-add a document for content that no
longer exists. The reverse order is equally meaningful: content destroyed and an identifier
later reused is a reindex, not a removal.

So the batch key stays the identifier — one outcome per identifier per batch is correct — and
the collision is resolved deterministically by `id` instead of by poll order. Entries that lose
are dropped from the batch, not from the journal; their rows remain and are collected on a
later pass, where they will lose again to the same newer entry until it is applied and removed.

| | Before | After |
|---|--------|-------|
| Key | `identToIndex` | `identToIndex` (unchanged) |
| Collision resolution | arbitrary — last `poll()` wins | deterministic — highest `id` wins |
| REINDEX after DELETE | can re-add a deleted document | cannot: DELETE has the higher `id` |
| Losing row | stays in journal, re-picked later | unchanged |

**Alternatives rejected**:

- *Key by `identToIndex` + `isDelete`.* Lets both entries into the same batch with no defined
  order between them. That does not fix the race; it relocates it into the bulk request, where
  ordering depends on iteration order of a `HashMap`.
- *Key by `id`.* Removes deduplication entirely. Every redundant REINDEX for a hot identifier
  becomes its own bulk operation — a throughput regression on the full-reindex path, which is
  the highest-volume consumer of this batch.

## State transitions

```
content saved       → REINDEX row  (dist_action = 1)  ─┐
content destroyed   → DELETE  row  (dist_action = 2)  ─┤  same ident_to_index
                                                        │
                     findContentToReindex ──────────────┘
                            │  highest id wins
                            ▼
                     ┌─────────────┐
        isDelete ────│  batch entry │──── !isDelete
             │       └─────────────┘            │
             ▼                                  ▼
   appendBulkRemoveRequest*            appendBulkRequest*
             │                                  │
             └──────────► bulk to index ◄───────┘
                            │
              ack ──────────┴────────── failure
               │                           │
     deleteReindexEntry(row)      markAsFailed(row)
                                  UPDATE priority/index_val
                                  dist_action preserved → retried as a delete
```

The failure edge is what the feature buys: the row survives, keeps its `dist_action`, and is
retried. That is the property the in-memory commit listener never had.
