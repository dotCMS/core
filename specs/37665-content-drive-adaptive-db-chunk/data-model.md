# Data Model: Content Drive Adaptive DB Chunk

**Feature**: [spec.md](./spec.md) | **Date**: 2026-09-29

No persisted data changes. There is no schema, index-mapping or response-shape change. The model
below is the in-memory state of one `getContentByChunks` call, and the rule that moves it from one
iteration to the next.

## Loop state (per request)

| Field | Meaning | Initial value | Changes |
|---|---|---|---|
| `floor` | The caller's chunk size | `min(max(maxRows × BROWSER_DB_CHUNK_FACTOR, BROWSER_DB_CHUNK_MIN_SIZE), BROWSER_DB_MAX_SCAN_ROWS)`, today's `effectiveChunkSize` | Never |
| `requestedChunkSize` | Rows requested for the current iteration | `floor` (FR-002) | After each non-exit iteration: `nextChunkSize(...)` when adaptive, otherwise `floor` |
| `dbOffset` | Absolute row position of the next read | `browserQuery.contentCursor` | `+= rowsReturned` |
| `rowsRead` | Rows read in this request so far | 0 | `+= rowsReturned` |
| `visibleSoFar` | Permission-visible items accumulated | 0 | `= accumulatedContent.size()` |
| `stillNeeded` | Items missing to fill the page | `maxRows` | `= maxRows - visibleSoFar` |

`rowsRead` is `dbOffset - contentCursor`. It is kept separate because the ratio covers this request
only, while the remaining budget is measured from the absolute `dbOffset` (spec Assumptions).

## Iteration outcomes

The exits are checked in today's order. Only the comparisons change.

1. **No rows returned**: the folder is exhausted. `nextCursor = dbOffset`, `hasMore = false`.
2. **Page filled** (`visibleSoFar >= maxRows`): `hasMore = (rowsReturned == requestedChunkSize)`.
   It compares against the size requested this iteration, not `floor` (FR-004).
   `nextCursor = generateNextContentCursor(...)`, which is unchanged (FR-005).
3. **Partial chunk** (`rowsReturned < requestedChunkSize`): the folder is exhausted (FR-004).
   `nextCursor = dbOffset`, `hasMore = false`.
4. **Scan budget exhausted** (`dbOffset >= BROWSER_DB_MAX_SCAN_ROWS`): unchanged.
   `hasMore = true`, with a warning log.
5. **Otherwise**: compute the next `requestedChunkSize` and loop.

## Sizing rule: `nextChunkSize`

Inputs: `previousRequested`, `floor`, `rowsRead`, `visibleSoFar`, `stillNeeded`, `scanLimit`,
`dbOffset`, `ceiling`, `maxGrowth`, `safetyFactor`.

```text
projected = visibleSoFar > 0
          ? ceil(stillNeeded × rowsRead × safetyFactor / visibleSoFar)   # ratio branch
          : previousRequested × 2                                         # doubling branch
capped    = min(projected, previousRequested × maxGrowth, ceiling, scanLimit − dbOffset)
next      = max(floor, capped)                                            # floor wins
```

The arithmetic is done in `double` and `long`, then clamped to `int`, so it cannot overflow (see
research R4). The limits are applied in this order for the reasons in research R5.

### Worked examples (defaults: floor 400, ceiling 2,000, max growth 4, safety factor 1.5)

The ceiling default was lowered from the spec's 7,000 to 2,000 after the PR review benchmark:
7,000 saturated a 2 GB heap under 10 concurrent permission-limited listings (see
dotCMS/core#37838).

| Situation | Projected | Capped by | Next |
|---|---|---|---|
| 0 visible of 400, 40 needed | 800 | — | 800 |
| 0 visible, previous 800 | 1,600 | — | 1,600 |
| 0 visible, previous 1,600 | 3,200 | ceiling | 2,000 |
| 1 visible of 400, 39 needed (clustered) | 23,400 | step cap 1,600 | 1,600 |
| 5 visible of 400, 35 needed | 4,200 | step cap 1,600 | 1,600 |
| 27 visible of 2,000, 13 needed | 1,445 | — | 1,445 |
| 20 visible of 400, 20 needed | 600 | — | 600 |
| 39 visible of 400, 1 needed | 16 | floor | 400 |
| Any, `scanLimit − dbOffset` = 150 | — | budget 150 | 400 (floor wins) |
| Floor 7,500 (page of 750) | — | ceiling 2,000 | 7,500 (floor wins) |

## Configuration entities

See [contracts/configuration.md](./contracts/configuration.md).
