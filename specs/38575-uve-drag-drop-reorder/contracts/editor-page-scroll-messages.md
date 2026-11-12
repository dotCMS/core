# Contract: editor ↔ page scroll messages during a drag

The editor (dotAdmin) and the page (`@dotcms/uve`) talk through `postMessage`. This contract covers only the messages involved in auto-scroll. Everything else is unchanged.

## Editor → page

### `uve-scroll-velocity` (new)

```json
{ "name": "uve-scroll-velocity", "velocity": 12 }
```

- `velocity`: signed pixels per 16.67ms frame. Negative scrolls up. `0` stops.
- Sent when the value changes while a drag is in progress, and once with `0` when the pointer leaves the edge zone, on drop, on drag end and on drag leave.
- Page behavior: runs a frame loop scrolling the window instantly by `velocity × (frameTime / 16.67)`; stops the loop at `0` or when the page limit is reached. After receiving this message once, the page ignores `uve-scroll-inside-iframe` for the rest of the page's life.
- Non-finite values are treated as `0`. Magnitude is clamped to 40.

### `uve-scroll-inside-iframe` (existing, unchanged meaning)

```json
{ "name": "uve-scroll-inside-iframe", "direction": "down" }
```

- Still sent while the pointer is in the edge zone, at most once every 250ms, for pages that do not understand `uve-scroll-velocity`.
- Page behavior on older SDKs: unchanged (one smooth 120px step).

## Page → editor

### `scroll` (existing, one optional field added)

```json
{ "action": "scroll", "payload": { "scrollY": 840 } }
```

- `payload.scrollY`: the window's vertical scroll offset. Older SDKs send no payload.
- Editor behavior during a drag: with `scrollY`, keep drop bounds and shift them by the delta; without it, clear bounds and wait for `set-bounds` as today.
- Editor behavior outside a drag: unchanged.

### `scroll-end`, `set-bounds` (existing, unchanged)

Fresh bounds replace any shifted bounds.

## Compatibility matrix

| Editor | Page SDK | Result |
|--------|----------|--------|
| new | new | Velocity loop, drop targets stay aligned while scrolling |
| new | old | Legacy stepped scroll, throttled; drop targets hide while scrolling, as today; no dead zone at page limits |
| old | new | Legacy stepped scroll, as today; the extra `scrollY` is ignored |
| old | old | Unchanged |
