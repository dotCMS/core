# CLI contracts: `evergreen-tracks state` / `release-title` and `changelog-publisher sync-site`

All three are **additive** subcommands. The existing `evergreen-tracks promote|admin` and
`changelog-publisher publish` interfaces, exit codes and stdout are unchanged.

Run from the tool directory with `uv run …` (each tool is its own uv project under
`.github/actions/core-cicd/`).

---

## 1. `evergreen-tracks state` (read-only Hub snapshot)

```
uv run evergreen-tracks state --repo <namespace/name> --out <path>
    [--as-if-action taint|untaint|hold|release-hold] [--as-if-version <GA>] [--as-if-track latest|standard|trailing]
```

| Input | Required | Meaning |
|---|---|---|
| `--repo` | yes | Docker Hub repo, in production always `dotcms/dotcms` |
| `--out` | yes | file to write the [hub-state JSON](hub-state.schema.json) to (overwritten) |
| `--as-if-action` | no | admin dry-run preview: apply this action to the snapshot in memory (data-model §1) |
| `--as-if-version` | with taint / untaint / hold | GA version the action targets |
| `--as-if-track` | with hold / release-hold | track the action targets |
| env `DOCKER_USERNAME`, `DOCKER_TOKEN` | in practice yes | Hub auth. Anonymous paging stops at offset 1000 and `dotcms/dotcms` has ~7.8k tags |

Behavior: one full tag listing (`_state(repo)`, the same read `admin` uses). Each track is
resolved by digest (`_current_version`). `tainted` holds the GA versions behind `*_tainted`
markers, sorted oldest → newest. Holds are ignored. The command never mutates the registry.

| Exit | Meaning | `--out` written? |
|---|---|---|
| `0` | snapshot written | yes |
| `1` | Hub read failed (HTTP / auth / network), or **incomplete**: a track tag is missing or its digest matches no GA version | **no** |
| `2` | usage error: bad `--as-if-*` combination, non-GA `--as-if-version`, unknown track | no |

stdout: the usual log lines plus one `hub state: {...}` line on success. The file is the
contract; callers must not parse stdout.

## 2. `evergreen-tracks release-title` (pure title transform)

```
uv run evergreen-tracks release-title --action taint|untaint --title "<current GitHub Release title>"
```

Prints **only** the new title, plus a newline, on stdout. Nothing else may go to stdout. Exit `0`.
argparse usage errors exit `2`. No network.

| `--action` | Output |
|---|---|
| `taint` | `⚠️ TAINTED <title>`, or `<title>` unchanged if it already starts with the marker |
| `untaint` | `<title>` with the leading `⚠️ TAINTED ` removed, or unchanged if it has no marker |

## 3. `changelog-publisher sync-site` (reconcile the `EvergreenState` record)

```
uv run changelog-publisher sync-site --state-file <path> [--apply]
```

| Input | Required | Meaning |
|---|---|---|
| `--state-file` | yes | hub-state JSON from `evergreen-tracks state` |
| `--apply` | no | write + publish when the record differs; omitted = dry-run (default) |
| env `DOTCMS_DEVSITE_URL` | yes | corpsites-headless base URL (repo variable `vars.DOTCMS_DEVSITE_URL`) |
| env `DOTCMS_DEVSITE_RELEASENOTES_TOKEN` | yes | existing service-account bearer token; read once, never logged |

Behavior, per attempt:

1. Find the record (data-model §2 lookup).
2. Check each track's release row (data-model §3).
3. Build the desired record. A track with a missing row keeps its current value.
4. Diff (`tainted` compared as a set).
5. If there is a difference: with `--apply`, fire Publish once and then read it back (up to
   10 × 2 s, a warning on timeout); in dry-run, print it.

With `--apply`: up to **3 attempts**, sleeping **60 s, then 180 s** between them. Any
`requests` error or `RuntimeError` (record missing, ambiguous record, HTTP 4xx/5xx) is retried.
Dry-run makes 1 attempt.

| Exit | Meaning |
|---|---|
| `0` | record already matched (`unchanged`), or was written (`updated`), or dry-run computed (`would-update` / `unchanged`) |
| `1` | runtime failure after all attempts (or the token / URL is unset). Nothing is guaranteed to be written |
| `2` | usage / validation error: state file missing, not JSON, wrong keys, non-GA values. **Rejected before any network call** |
| `3` | record reconciled, but ≥1 track kept its previous value because its version has **no release row** (FR-007). Not retried |

stdout markers (one per line; the workflow greps them; other text may appear around them):

| Marker | When |
|---|---|
| `::evergreen-sync::unchanged` | record equals desired; no write |
| `::evergreen-sync::updated fields=<f1,f2>` | `--apply` wrote the listed fields |
| `::evergreen-sync::would-update fields=<f1,f2>` | dry-run: these fields differ |
| `::evergreen-sync-missing-row::<track>=<version>` | one line per track kept back (exit `3`) |
| `::evergreen-sync-error::<one-line reason>` | exit `1`; the reason never contains the token |

`fields` order is `latest,standard,trailing,tainted`. In dry-run, before the marker, the tool also
prints `desired: <compact json>` and one line per differing field:
`  <field>: <old> -> <new>` (for `tainted`: `  tainted: +<added…> -<removed…>`).
Logs go to stderr (the existing `logging.basicConfig` default).
