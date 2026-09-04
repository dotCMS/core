# Phase 1 Data Model: `dotcms agent setup`

No database and no persisted state of the tool's own. The "data model" is the in-memory shape of a run plus the documents it reads and writes on disk. Entities below map to the spec's Key Entities; validation rules cite the requirement they enforce.

---

## `AgentTarget`

One per supported editor. **The only place an editor is described** — `commands/setup.ts` branches on none of these fields (FR-013).

| Field | Type | Notes |
|---|---|---|
| `id` | `'claude-code' \| 'cursor' \| 'vscode' \| 'codex' \| 'antigravity' \| 'devin' \| 'opencode'` | Stable; accepted by `--agent`. The union type is what makes an unknown id a compile-time error and a named runtime error (FR-032). |
| `displayName` | `string` | Summary and prompt label. |
| `skillsAgentId` | `string \| null` | The `skills -a` identifier. `null` means skills are not installable for this target, which the summary must reflect rather than imply success (FR-027). |
| `format` | `'json' \| 'toml'` | Selects the writer. Only `codex` is `toml`. |
| `containerKey` | `string` | Where server entries live: `mcpServers` for most, `servers` for VS Code, `mcp` for OpenCode, `mcp_servers` for Codex. |
| `entryShape` | `'stdio' \| 'opencode-local'` | OpenCode's entry differs structurally, not just by key. |
| `detect()` | `() => Promise<boolean>` | Probes a marker directory. Advisory only — an undetected editor is still selectable (spec Edge Cases). |
| `configPath(scope)` | `(scope: Scope) => string \| null` | `null` means the target has no file at that scope. |

**Rules**

- Exactly seven entries at ship (SC-006).
- Two targets may resolve to the same path; the run de-duplicates by resolved path and writes once (spec Edge Cases).
- Adding an eighth is one object literal and no flow change (FR-013 — verified by code review, not by test).

---

## `Scope`

```
'folder'  — the current working directory. THE DEFAULT.
'global'  — the developer's user account, selected by -g/--global.
```

**Rules**

- Defaults to `folder` (FR-011). This is the reverse of the pre-clarify design; implementing the intuitive "global default" is a requirement violation, not a preference.
- A folder's configuration names **one** dotCMS instance. Two instances means two folders (FR-011a).
- `folder` carries version-control risk and drives the `.gitignore` flow (FR-023, FR-023a).

---

## `ServerRegistration`

The single entry written into a config file, under the canonical key `dotcms` (FR-011b).

Standard shape — six JSON targets:

```json
{
  "type": "stdio",
  "command": "npx",
  "args": ["-y", "@dotcms/mcp-server@latest"],
  "env": { "DOTCMS_URL": "…", "AUTH_TOKEN": "…" }
}
```

OpenCode shape:

```json
{
  "type": "local",
  "command": ["npx", "-y", "@dotcms/mcp-server@latest"],
  "enabled": true,
  "environment": { "DOTCMS_URL": "…", "AUTH_TOKEN": "…" }
}
```

Codex is the standard shape expressed as TOML tables: `[mcp_servers.dotcms]` plus `[mcp_servers.dotcms.env]`.

**Rules**

- Env var names are exactly `DOTCMS_URL` and `AUTH_TOKEN` — what `runtimeFromEnv()` reads in `apps/mcp-server/src/lib/runtime.ts` (FR-020). Not `DOTCMS_TOKEN`.
- The server reference is unpinned (FR-020e). See plan Complexity Tracking for the ADR-0019 deviation.
- Exactly one such entry per file. Writing replaces only this key; every sibling survives byte-for-byte (FR-016).

---

## `Token`

The only secret ever written into a configuration.

| Field | Type | Notes |
|---|---|---|
| `value` | `string` | Held in memory for the run. Never logged in full, never in argv (FR-022). |
| `origin` | `'minted' \| 'supplied'` | Minted from a username and password, or given via `--authToken`. |
| `verified` | `boolean` | Set only by a successful `GET /api/v1/users/current`. **No file is opened for writing while this is false** (FR-008a). |

**Rules**

- Redacted to first 6 + last 4 wherever displayed (FR-022).
- A password is used only to mint and is never persisted, echoed, or included in any message (FR-022a).
- Minted tokens: 365-day lifetime, labelled with their origin. No renewal (spec Assumptions).

**State transitions** — the ordering FR-008a depends on:

```
resolved (option | env | prompt | minted)
   → verifying   GET /api/v1/users/current
       ├─ rejected → FAIL. Nothing written: no file, no directory, no skills install.
       └─ verified → eligible to be written
```

---

## `TargetOutcome`

One per selected target. Drives the summary (FR-020b) and the exit code (FR-020c).

| Field | Type | Notes |
|---|---|---|
| `targetId` | `AgentTarget['id']` | |
| `scope` | `Scope` | |
| `path` | `string \| null` | The file written, or `null` when nothing was. |
| `result` | `'written' \| 'replaced' \| 'skipped' \| 'failed'` | |
| `reason` | `string \| null` | Required when `failed` — must be self-sufficient (FR-032a). |
| `permissionsApplied` | `boolean` | `false` on Windows; the summary must say so rather than imply protection (research R5). |
| `skillsInstalled` | `'yes' \| 'no' \| 'unverified'` | `unverified` for a target whose skills location is unconfirmed — never reported as installed (FR-027). |

**Rules**

- A failure on one target does not stop the others and does not roll back what succeeded (FR-020a, FR-020d).
- Any `failed` outcome ⇒ process exits non-zero (FR-020c).

---

## `RunOptions`

Resolved inputs for a run. Resolution order per input: **option → environment → prompt** (FR-003e).

| Field | Required | Source |
|---|---|---|
| `url` | **yes** | `--url` → `DOTCMS_URL` → prompt (default `http://localhost:8082`); normalized and validated (FR-004) |
| `user` / `password` | one auth mode required | `--user` / `--password`, password also via environment |
| `authToken` | one auth mode required | `--authToken`, also via environment |
| `agents` | no | `--agent` (repeatable); defaults to every detected target (FR-003j) |
| `scope` | no | `-g/--global`; defaults to `folder` |
| `skipMcp` / `skipSkills` / `skipVerify` | no | flags |
| `yes` / `force` | no | **confirmations only** (FR-003l) |

**Rules**

- `url` plus exactly one auth mode are the only required inputs. Both supplied ⇒ the run completes unprompted, TTY or not (FR-003i).
- `authToken` together with `user` or `password` is a usage error: nothing minted, nothing written (FR-003b).
- Half an auth pair prompts for the other half on a TTY, fails by name without one (FR-003c).
- `yes`/`force` never suppress a prompt for a missing required input and never change which inputs are required (FR-003l).

---

## `ConfigDocument`

An existing editor config, read before writing. Not owned by this tool.

**Rules**

- Read → parse → mutate one key → serialize → write. Never truncate-and-rewrite from scratch (FR-016).
- Unparseable input is a named error identifying the file and the remedy; the file is left byte-for-byte untouched and is **never** overwritten (FR-018).
- Missing file or parent directory is created (FR-019), owner-only where the platform supports it (FR-021, research R5).
- JSON is written with 2-space indentation. TOML round-trips through `smol-toml`, preserving unrelated tables (research R6).
