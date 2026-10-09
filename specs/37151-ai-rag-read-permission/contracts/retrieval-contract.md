# Contract: dotAI retrieval (`EmbeddingsAPI.getEmbeddingResults`) and AI search/completions responses

The JSON shape of every response stays the same. This file states the semantics that change,
which are what the Javadoc and the release note must say.

## Java API: `EmbeddingsAPI.getEmbeddingResults(EmbeddingsDTO searcher)`

- **Returns** only chunks whose source contentlet `searcher.user` can READ, decided by
  `PermissionAPI.filterCollection(List, PERMISSION_READ, true, user)`. Results are in
  ascending distance order, with ties broken by row id.
- **Users**:

  | `searcher.user` | Behaviour |
  |---|---|
  | `null` | Checked as Anonymous. No bypass |
  | System user or CMS admin | Unrestricted; `offset`/`limit` applied in SQL as before |
  | Anyone else | Filtered |

- **Paging**: `offset`/`limit` count readable chunks, taken from at most `max(limit, cap)`
  matching chunks per call. The cap is `embeddingsSearchCandidateCap` (default 1000) for
  logged-in callers and at most 200 for anonymous callers. `offset` does not grow that number,
  so a page beyond it comes back short or empty.
- **Dropped**: chunks whose inode has no loadable contentlet. That includes deleted content
  and `cache` rows.
- **Errors**: a failure loading content or checking permissions throws (unchecked) and returns
  nothing.
- **Implementations**: alternative `EmbeddingsAPI` implementations must honour the same
  contract.

## REST: `GET|POST /api/v1/ai/search`, `GET|POST /api/v1/ai/search/related`

| Field | Semantics |
|-------|-----------|
| `dotCMSResults` | Readable content only |
| `total` | Number of readable chunks returned |
| `count` | Number of items in `dotCMSResults` (was: unfiltered count of every match) |
| `offset`, `limit` | Echo the request; units are readable chunks |

## REST: `POST /api/v1/ai/completions` (streaming and non-streaming)

- The prompt sent to the model contains only readable chunks.
- If no readable chunk matches, the response is
  `{"error":"no matching content found in the index for your query"}` and no model call is
  made.
- Non-streaming `dotCMSResults`, `total` and `count` follow the search semantics above.

## Viewtools: `$ai.search.query/related`, `$ai.completions.summarize`

These follow the same semantics. An anonymous visitor is checked as Anonymous, which means
live content the CMS Anonymous role can read.

## Unchanged

- `/api/v1/ai/embeddings/*`, including `/count`, which stays unfiltered and is a known
  residual risk.
- `rawPrompt`, text and image endpoints.
