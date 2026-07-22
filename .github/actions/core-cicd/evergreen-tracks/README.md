# evergreen-tracks

Advances floating Docker track tags (`latest`, `standard`, `trailing`) across the dotCMS
GA CalVer release stream by release age. State lives entirely in registry tags (the track
tags plus `<version>_tainted` / `<track>_hold` markers).

## Cadence

- **`latest`** moves automatically on every GA release cut (the release pipeline calls
  `promote --tracks latest --apply`).
- **`standard` / `trailing`** move **only when an operator manually dispatches** the
  `evergreen-tracks-promote` GitHub Action. There is no cron — a human running the action
  at a maintenance-window tag update is the cadence gate, so tracks never re-point
  off-window. Automatic (e.g. daily) promotion stays off until the customer-experience
  team green-lights it. When run, each track lands on the newest GA older than its age
  threshold (`--standard-days` 14, `--trailing-days` 28).

  The dispatch runs in three jobs: `plan` prints the intended moves (dry-run), `gate`
  waits on the `evergreen-tracks-apply` environment's required-reviewer gate, and `apply`
  runs after approval — nothing moves until a human reviews the plan and approves.
  (One-time repo setup: Settings > Environments > `evergreen-tracks-apply` > Required
  reviewers.) The dispatch is scoped to `--tracks standard,trailing` — it never moves
  `latest` (the release pipeline owns that). `apply` re-derives its plan from live
  registry state at run time and **fails if it no longer matches the approved plan**
  (e.g. a hold/taint changed, or a release aged past a threshold during a long approval),
  so it can never move tags nobody reviewed — just re-dispatch to review the new plan.
  Because the plan excludes `latest`, an unattended `latest` move by the release pipeline
  mid-approval doesn't trip the drift check. Only `apply` takes the shared
  registry-mutation lock, so a pending approval never blocks the release from moving
  `latest`.

## Run locally (dry-run is the default)

    uv run evergreen-tracks promote --repo dotcms/dotcms-test
    uv run evergreen-tracks admin --repo dotcms/dotcms-test --action taint --version 26.03.12-01

Pass `--apply` to actually move tags. Without it, the command prints the plan and exits.

## Test

    uv run pytest
