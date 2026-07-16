# evergreen-tracks

Advances floating Docker track tags (`latest`, `standard`, `trailing`) across the dotCMS
GA CalVer release stream by release age. State lives entirely in registry tags (the track
tags plus `<version>_tainted` / `<track>_hold` markers).

## Run locally (dry-run is the default)

    uv run evergreen-tracks promote --repo dotcms/dotcms-test
    uv run evergreen-tracks admin --repo dotcms/dotcms-test --action taint --version 26.03.12-01

Pass `--apply` to actually move tags. Without it, the command prints the plan and exits.

## Test

    uv run pytest
