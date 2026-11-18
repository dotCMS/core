# Contract: `migrate_custom_fields.py` CLI

This is the interface the `dot-ui-vtl-migration` skill (and, ultimately, an AI agent acting on
a customer's behalf) programs against. No human is assumed to be reading a terminal — every
input is a flag or env var, every output an agent can parse is on stdout as one final JSON
object, and every exit code has one fixed meaning (FR-015, and the issue's own Agent-Friendly-
Interface AC).

## Invocation

```bash
uv run scripts/migrate_custom_fields.py [--workdir PATH] <pull|push> [subcommand flags]
```

No other install step. `uv` resolves `httpx` from the script's own PEP 723 metadata on first
run and caches it.

## Configuration (env vars — never flags, never prompted)

| Var | Required | Default | Notes |
|---|---|---|---|
| `BASE_URL` | no | `http://localhost:8080` | The dotCMS instance to talk to |
| `DOTCMS_TOKEN` | **yes**, no default | — | A dotCMS API access token, sent as `Authorization: Bearer <token>`; mint one from the instance's admin UI (Users → select user → API Access Tokens) or `POST /api/v1/authentication/api-token`. **Never** printed, logged, or included in any error/JSON output |

A missing or rejected token is a hard failure (exit 3) before anything is written to disk, or
any request is made (FR-014).

## `pull`

```bash
uv run scripts/migrate_custom_fields.py pull [--workdir PATH]
```

- Read-only against the instance (FR-008). Writes `<workdir>/original/**`,
  `<workdir>/migrated/{assets,fields}/` (empty, ready for the agent to fill), and
  `<workdir>/manifest.json`.
- Human-readable classification report to **stderr** during the run (the existing draft's
  `report(...)` sections); the final line on **stdout** is the CLI Result JSON (see
  data-model.md) — stdout carries only that one JSON line.
- Progress lines (`[HH:MM:SS] ...`) and warnings also go to stderr.

## `push`

```bash
uv run scripts/migrate_custom_fields.py push [--workdir PATH] [--dry-run] [--only KEY [KEY ...]]
```

- `--dry-run`: describes every prospective change; **issues zero PUT/POST/DELETE requests**
  (FR-006, FR-008). Safe to run any number of times.
- `--only KEY [KEY ...]`: restricts the run to entries whose `key` (dA id or `Type.field`) or
  `identifier` matches one of the given values; everything else in the manifest is left
  untouched (FR-007). Naming a `/dA/` asset also includes the `renderMode` entries that require
  it (the fields that load it), unless they already finished. This is how the skill pushes one
  customer-picked batch at a time.
- Per-entry safety gates, each independent of the others (FR-011):
  1. **Conflict check** (FR-009): re-fetch the live asset/field; if its inode (asset) or
     `values` (field) differs from what `pull` recorded, skip with reason `"changed on the
     server since pull"`.
  2. **Inline-validity check** (FR-010): the migrated file must contain the modern-editor
     marker and must preserve the original bytes verbatim in the legacy branch; otherwise skip
     with a reason naming which check failed.
  3. Otherwise: publish (asset) or update the field (field kind). A field update is a
     `PUT /api/v3/contenttype/{typeId}/fields/{fieldId}` with the full field, its new `values`
     and `newRenderMode=component`, keeping every other field variable.
- `renderMode` entries only switch a field to `newRenderMode=component` (same v3 PUT, values
  untouched). One that depends on a `/dA/` asset (`requires`) runs only once that asset is
  published.
- **Waiting**: an asset/field entry with no migrated file yet (not part of this batch), or a
  `renderMode` entry whose asset isn't published, gets outcome `waiting: ...`, stays `pending`
  for a later `push`, and is counted under `counts.waiting` (present only when non-zero).
- Writes `manifest.json` back to disk after **every** entry's outcome, not only at the end of
  the batch (see research.md) — so an interrupted run's manifest still answers "what happened."
- Final line on stdout is the CLI Result JSON.

## Exit codes

| Code | Meaning | When |
|---|---|---|
| `0` | Success | Every attempted entry either published or was intentionally skipped for a reported reason; for `pull`, discovery completed |
| `1` | Some entries failed | For `push`: at least one entry's *publish/update request itself* failed (not merely skipped by a safety gate). For `pull`: at least one `/dA/` reference couldn't be resolved or downloaded |
| `2` | Invalid arguments or configuration | Bad CLI flags, unreadable `--workdir`, `push` run with no `manifest.json` present, unreachable `BASE_URL` (non-auth network/config failure) |
| `3` | Authentication failure | `DOTCMS_TOKEN` is unset, or the instance rejected it — message names the env var and, on rejection, mentions the token may be revoked/expired or Bearer auth may be disabled; nothing is written to disk |

These are documented in `--help` (Packaging AC) as well as here.

## stdout / stderr contract

- **stdout**: human-readable progress/report text during the run (kept for a developer running
  it by hand), followed by exactly one final JSON object — the CLI Result from data-model.md —
  as the very last line. An agent parsing programmatically should read the last line of stdout
  as JSON.
- **stderr**: everything else — timestamped progress lines, the classification report, warnings,
  and the `FAILED: {method} {path} -> {error}` failure lines. Never contains `DOTCMS_TOKEN`.
