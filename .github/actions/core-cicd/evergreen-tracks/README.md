# evergreen-tracks

Advances floating Docker track tags (`latest`, `standard`, `trailing`) across the dotCMS
GA CalVer release stream by release age. State lives entirely in registry tags (the track
tags plus `<version>_tainted` / `<track>_hold` markers).

## Cadence

- **`latest`** moves automatically on every GA release cut (the release pipeline calls
  `promote --tracks latest --apply`).
- **`standard` / `trailing`** advance on a **daily cron at 06:00 ET**, unattended, via the
  `evergreen-tracks-promote` GitHub Action (`10:00 UTC` — GitHub cron has no DST, so it's
  05:00 ET in winter). Each track lands on the newest GA older than its age threshold
  (`--standard-days` 14, `--trailing-days` 28). Daily promotion does not mean daily tag
  movement: the planner is forward-only and age-gated, so a track moves only on the day a
  release actually crosses its threshold.

  The same workflow can be **dispatched manually as the break-glass path** — to review a
  plan before it lands, or to move tags off-cycle. On that path a `gate` job waits on the
  `evergreen-tracks-apply` environment's required-reviewer rule before `apply` runs. The
  scheduled path skips `gate` entirely, which is what makes it unattended. (One-time repo
  setup: Settings > Environments > `evergreen-tracks-apply` > Required reviewers.)

  Both paths are scoped to `--tracks standard,trailing` — this workflow never moves
  `latest` (the release pipeline owns that). `apply` re-derives its plan from live registry
  state and **fails if it no longer matches what `plan` produced** (e.g. a hold/taint
  changed), so it can never move tags nobody planned; the next morning's run re-plans.
  Because the plan excludes `latest`, a `latest` move by the release pipeline never trips
  the drift check. Only `apply` takes the shared registry-mutation lock, so a pending
  approval on the manual path never blocks the release from moving `latest`.

### Notifications

The workflow posts to **#dot-releases**, deliberately only when there's something to say:

| Outcome | Posts |
|---|---|
| `plan` or `apply` failed | 🚨 yes — the tags did **not** move (unchanged, not half-moved) |
| A track advanced | 🌲 yes — with what moved where |
| Nothing to do (most days) | nothing |

A rejected break-glass approval leaves `apply` *skipped* rather than failed, so declining a
plan never pages the channel. Notification failures are `continue-on-error` — Slack being
down never fails a promotion.

### Why unattended daily promotion is safe

Moving a floating tag deploys nothing. Every customer manifest in
`dotCMS/infrastructure-as-code` pins an immutable `<version>@sha256:<digest>`, and the
in-cluster evergreen-tracks reconciler is the only thing that rewrites those pins — it
resolves the track tag at run time, inside its biweekly on-parity Wednesday maintenance
window. A tag moved on a Tuesday has no effect until that window. Nothing in the clusters
watches the tags continuously (no Argo CD Image Updater / Keel, no floating-tag references,
no `imagePullPolicy: Always` on customer pods).

## Operator procedures

See [RUNBOOK.md](RUNBOOK.md) for tainting a release, holding a track, and holding a single
customer environment.

## Run locally (dry-run is the default)

    uv run evergreen-tracks promote --repo dotcms/dotcms-test
    uv run evergreen-tracks admin --repo dotcms/dotcms-test --action taint --version 26.03.12-01

Pass `--apply` to actually move tags. Without it, the command prints the plan and exits.

## Test

    uv run pytest
