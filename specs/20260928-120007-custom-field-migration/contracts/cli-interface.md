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
| `DOTCMS_USER` | no | `admin@dotcms.com` | Basic auth username |
| `DOTCMS_PASS` | no | `admin` | Basic auth password — **never** printed, logged, or included in any error/JSON output |

Missing or rejected credentials are a hard failure (exit 3) before anything is written to disk
(FR-014).

## `pull`

```bash
uv run scripts/migrate_custom_fields.py pull [--workdir PATH]
```

- Read-only against the instance (FR-008). Writes `<workdir>/original/**`,
  `<workdir>/migrated/{assets,fields}/` (empty, ready for the agent to fill), and
  `<workdir>/manifest.json`.
- Human-readable classification report to stdout during the run (the existing draft's
  `report(...)` sections); the final line on stdout is the CLI Result JSON (see
  data-model.md).
- Progress lines (`[HH:MM:SS] ...`) and warnings go to stderr.

## `push`

```bash
uv run scripts/migrate_custom_fields.py push [--workdir PATH] [--dry-run] [--only KEY [KEY ...]]
```

- `--dry-run`: describes every prospective change; **issues zero PUT/POST/DELETE requests**
  (FR-006, FR-008). Safe to run any number of times.
- `--only KEY [KEY ...]`: restricts the run to entries whose `key` (dA id or `Type.field`) or
  `identifier` matches one of the given values; everything else in the manifest is left
  untouched (FR-007).
- Per-entry safety gates, each independent of the others (FR-011):
  1. **Conflict check** (FR-009): re-fetch the live asset/field; if its inode (asset) or
     `values` (field) differs from what `pull` recorded, skip with reason `"changed on the
     server since pull"`.
  2. **Inline-validity check** (FR-010): the migrated file must contain the modern-editor
     marker and must preserve the original bytes verbatim in the legacy branch; otherwise skip
     with a reason naming which check failed.
  3. Otherwise: publish (asset) or update the field (field kind).
- Writes `manifest.json` back to disk after **every** entry's outcome, not only at the end of
  the batch (see research.md) — so an interrupted run's manifest still answers "what happened."
- Final line on stdout is the CLI Result JSON.

## Exit codes

| Code | Meaning | When |
|---|---|---|
| `0` | Success | Every attempted entry either published or was intentionally skipped for a reported reason; for `pull`, discovery completed |
| `1` | Some entries failed | At least one entry's *publish/update request itself* failed (not merely skipped by a safety gate) |
| `2` | Invalid arguments or configuration | Bad CLI flags, unreadable `--workdir`, `push` run with no `manifest.json` present, unreachable `BASE_URL` (non-auth network/config failure) |
| `3` | Authentication failure | The instance rejected the given `DOTCMS_USER`/`DOTCMS_PASS`, or basic auth appears to be disabled on the instance — message names the two env vars and mentions this possibility; nothing is written to disk |

These are documented in `--help` (Packaging AC) as well as here.

## stdout / stderr contract

- **stdout**: human-readable progress/report text during the run (kept for a developer running
  it by hand), followed by exactly one final JSON object — the CLI Result from data-model.md —
  as the very last line. An agent parsing programmatically should read the last line of stdout
  as JSON.
- **stderr**: everything else — timestamps progress log lines, warnings, and the
  `FAILED: {method} {path} -> {error}` failure lines. Never contains `DOTCMS_PASS`.
