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

## Run locally (dry-run is the default)

    uv run evergreen-tracks promote --repo dotcms/dotcms-test
    uv run evergreen-tracks admin --repo dotcms/dotcms-test --action taint --version 26.03.12-01

Pass `--apply` to actually move tags. Without it, the command prints the plan and exits.

## Test

    uv run pytest
