# Data Model: #37151

There are no new tables, columns or persisted state. This file describes the in-memory
shapes the fix works with.

## Embedding chunk (`EmbeddingsDTO`, one `dot_embeddings` row)

| Field | Used for |
|-------|----------|
| `inode` | Permission key. Chunks are grouped by inode, and the content is loaded by inode |
| `identifier`, `language`, `host`, `contentType`, `indexName` | Existing SQL filters (unchanged) |
| `title`, `extractedText` | Returned to the caller and sent to the model only if the inode passes READ |
| `threshold` (holds the row's distance on results) | Ranking |
| `id` (newly selected, not exposed) | Tie-break for stable order |

**Rows with no content behind them**: cache rows (`inode = 'cache'`), deleted content and
rows from fake test data. `findContentlets` returns no contentlet for these, so they are
dropped (FR-002).

## Retrieval request (`EmbeddingsDTO` searcher)

| Field | Meaning after the fix |
|-------|-----------------------|
| `user` | Permission subject. `null` means Anonymous. The system user and CMS admins are unrestricted |
| `limit` | Readable chunks per page |
| `offset` | Readable chunks to skip |
| `threshold`, `operator`, filters | Unchanged |

## Candidate cap (new app setting)

| Key | Setting | Default | Scope |
|-----|---------|---------|-------|
| `AppKeys.EMBEDDINGS_SEARCH_CANDIDATE_CAP` | `embeddingsSearchCandidateCap` (providerConfig `settings`) | `1000` | Per site, falls back like other dotAI settings. A value ≤ 0 means default |

## Filter state (inside `ReadableChunkFilter`, per request)

- `decided: Map<inode, Boolean>`: READ decisions made so far.
- `result: List<EmbeddingsDTO>`: readable chunks in rank order. Collection stops at
  `offset + limit`.
- **Batching**: when the walk reaches an undecided inode, it decides the next ≤ 200
  undecided distinct inodes, looking ahead from that point. Each batch is one
  `findContentlets` plus one `filterCollection(List…)`.

## Response fields (`reduceChunksToContent`)

| Field | Before | After |
|-------|--------|-------|
| `dotCMSResults` | Items from unfiltered chunks | Items from readable chunks |
| `total` | Chunks in the (unfiltered) page | Chunks returned (readable) |
| `count` | `countEmbeddings`: distinct inodes over **all** matches, unfiltered | Distinct items in `dotCMSResults` |
| `offset`, `limit` | Echoed | Echoed (now in readable-chunk units) |
