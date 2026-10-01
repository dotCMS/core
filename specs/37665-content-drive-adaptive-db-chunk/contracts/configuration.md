# Contract: Configuration Properties

The only external interface this feature adds is operator configuration. No REST, GraphQL or
response-shape change.

## New properties

All are read per request, so a change takes effect on the next request with no restart (FR-007).

| Key | Type | Default | Valid range | On invalid value |
|---|---|---|---|---|
| `BROWSER_DB_CHUNK_ADAPTIVE` | boolean | `true` | — | — |
| `BROWSER_DB_CHUNK_ADAPTIVE_MAX_SIZE` | int | `2000` | `> 0` | default, debug log |
| `BROWSER_DB_CHUNK_ADAPTIVE_MAX_GROWTH` | int | `4` | `>= 1` | default, debug log |
| `BROWSER_DB_CHUNK_ADAPTIVE_SAFETY_FACTOR` | float | `1.5` | `> 0`, finite | default, debug log |

The Java constants follow the existing `*_KEY` / `*_DEFAULT` pattern next to
`BROWSER_DB_MAX_SCAN_ROWS_KEY` in `BrowserAPIImpl`.

## Existing properties: meaning unchanged

| Key | Role in this feature |
|---|---|
| `BROWSER_DB_CHUNK_FACTOR` (10), `BROWSER_DB_CHUNK_MIN_SIZE` (200) | Define the floor and the first chunk. Still read once per `BrowserAPIImpl` instance. |
| `BROWSER_DB_MAX_SCAN_ROWS` (50,000) | Clamps the floor as today, bounds growth (remaining budget), and still ends the scan. |
| `BROWSER_CONTENT_CHUNK_SIZE`, `BROWSER_SINGLE_PASS_CHUNK_SIZE`, `BROWSER_DB_MAX_SCAN_TIME_MILLIS`, `BROWSER_DB_MAX_SCAN_ROWS_ES_HARD_CAP` | ES-narrowed path only. Not read by the adaptive logic. |

## Guarantee

With `BROWSER_DB_CHUNK_ADAPTIVE=false`, every chunk request on every path equals today's
`effectiveChunkSize` (SC-006).

## Rollback

Adding config keys is rollback-safe. The previous version ignores them.
