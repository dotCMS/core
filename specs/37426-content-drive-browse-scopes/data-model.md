# Phase 1 Data Model: Content Drive browse scopes

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-09-11

No persisted data changes: no table, no column, no index mapping. Everything here is request-time state.

---

## BrowseScope (new, request-level)

Which slice of content the listing is asked for.

| Value | Folder constraint | Host clause | Folders listed |
|---|---|---|---|
| *(absent)* | today's behavior: dropped at the site root, applied inside a folder | today's behavior | caller's choice |
| `ALL` | dropped, so every depth of the site | site, plus System Host when the toggle is on | no (client asks for none) |
| `ROOT` | applied at `/`, so only what sits at the root | site only | yes, the top-level folders |
| `SYSTEM_HOST` | applied at `/` | System Host only | no (System Host has none) |

**Validation**: a value other than absent is accepted only when the request's path is the site root. With a folder path, any explicit value is a 400. Refused, never resolved by precedence.

**Relationship to the existing `includeSystemHost`**: read only when the scope is `ALL` or absent. `ROOT` excludes System Host regardless; `SYSTEM_HOST` is System Host regardless.

---

## SystemHostMode (new, internal to `BrowserQuery`)

Replaces the `forceSystemHost` boolean. Not part of the REST contract.

| Value | SQL emitted | Lucene emitted |
|---|---|---|
| `EXCLUDE` *(default)* | `and (id.host_inode = ?)` | `+conhost:<site>` |
| `INCLUDE` | `and (id.host_inode = ? or id.host_inode = 'SYSTEM_HOST')` | `+(conhost:<site> OR conhost:SYSTEM_HOST)` |
| `ONLY` | `and (id.host_inode = 'SYSTEM_HOST')` | `+conhost:SYSTEM_HOST` |

`EXCLUDE` as the default is the whole backward-compatibility story: it reproduces exactly what the boolean `false` produces today, and no caller outside Content Drive ever set the flag. `ONLY` reaches `appendSystemHostQuery`, which exists today and is unreachable because it fires only when the query carries no site.

**State transitions**: none. The mode is derived per request from the browse scope and the System Host toggle, never mutated after the query is built.

---

## Location (frontend, one value)

The store's existing `path` becomes the single statement of where the user is. It is the only thing the URL carries about location.

| Value | Selection | Sent as |
|---|---|---|
| *(absent)* | All | `assetPath: //<site>/`, `browseScope: ALL` |
| `/` | the site root | `assetPath: //<site>/`, `browseScope: ROOT` |
| `/folder/…` | that folder | `assetPath: //<site>/folder/…`, no scope |
| `SYSTEM_HOST` | System Host | `assetPath: //<site>/`, `browseScope: SYSTEM_HOST` |

Two rules protect this table. Reserved words can never collide with a folder, because every real path begins with `/` and no reserved word does. And the mapping to `assetPath` must be explicit rather than template interpolation: the current expression at `dot-content-drive.store.ts:129` would produce `//demo.dotcms.comSYSTEM_HOST`.

**Why absent rather than an explicit `ALL` token**: the URL writer already removes the `path` parameter when the path is empty (`DEFAULT_PATH = undefined`), so links already in circulation carry no path and must keep meaning "the whole site". Absent is not a gap in the model, it is the back-compatible spelling of All.

---

## Sidebar selection (frontend)

Exactly one of four things is selected: the All row, the site row, a folder node, or the System Host row. The tree's existing `selectedNode` continues to represent the middle two; the two new rows live outside the tree and must clear it when chosen, and be cleared by it.

| Selection | Drop target | Add content |
|---|---|---|
| All | no | no |
| site row | yes, as today | yes, as today |
| folder | yes, as today | yes, as today |
| System Host | yes, moves content there | yes, gated against System Host itself |
