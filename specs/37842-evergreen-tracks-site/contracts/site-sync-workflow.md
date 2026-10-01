# Workflow contract: `cicd_comp_evergreen-site-sync.yml` and its three callers

## Reusable workflow `.github/workflows/cicd_comp_evergreen-site-sync.yml`

`on: workflow_call`, with one job `sync`.

| Input | Type | Default | Meaning |
|---|---|---|---|
| `trigger` | string | (required) | Human label for Slack, e.g. `evergreen-tracks-promote (schedule)`, `evergreen-tracks-admin taint 26.08.31-01`, `release 26.10.01-01` |
| `apply` | boolean | `false` | pass `--apply` to `sync-site` |
| `drift_check` | boolean | `false` | after the sync, re-run `sync-site` in dry-run on the same state file and post a drift notice if it still differs (FR-017). Callers set it only for the daily `schedule` run |
| `as_if_action` / `as_if_version` / `as_if_track` | string | `''` | forwarded to `evergreen-tracks state --as-if-*` (admin dry-run preview only) |
| `slack_channel_id` | string | `CE1TBQU00` | #dot-releases |

| Secret | Required | Used by |
|---|---|---|
| `DOCKER_USERNAME`, `DOCKER_TOKEN` | yes | `evergreen-tracks state` (Hub read) |
| `DOTCMS_DEVSITE_RELEASENOTES_TOKEN` | yes | `sync-site` |
| `SLACK_BOT_TOKEN` | yes | notices |

The repo variable `vars.DOTCMS_DEVSITE_URL` is read directly. The Hub repo is fixed to
`dotcms/dotcms` inside the workflow; callers decide *whether* to call it (production guard,
FR-015).

Job `sync`:

- `runs-on: ubuntu-${{ vars.UBUNTU_RUNNER_VERSION || '24.04' }}`, `permissions: contents: read`
- `continue-on-error: true`: never blocks the caller (FR-012, SC-005). The job still shows red on
  failure.
- `concurrency: { group: evergreen-tracks-site-sync, cancel-in-progress: false }`. This group is
  **not** `evergreen-tracks-registry` (research R3).
- Steps:
  1. checkout (pinned SHA, as in the other workflows) and `astral-sh/setup-uv@v5`
  2. `hub`: `evergreen-tracks state --repo dotcms/dotcms --out "$RUNNER_TEMP/hub-state.json"`
     plus any `--as-if-*`. Fails the job on a non-zero exit, so nothing is written.
  3. `sync`: `changelog-publisher sync-site --state-file … [--apply]`. Captures stdout + rc,
     echoes the output, and sets outputs `result` (`ok` | `missing-row` | `failure`) and
     `reason`. Exits with the tool's rc.
  4. `drift` (`if: always() && inputs.drift_check && steps.hub.outcome == 'success'`,
     `continue-on-error: true`): `sync-site` dry-run on the same file. Sets `drift=true` and
     `fields=…` when `::evergreen-sync::would-update` appears.
  5. `Notify failure` (`if: failure()`, `continue-on-error: true`) through
     `./.github/actions/core-cicd/notification/notify-slack`. The message names `inputs.trigger`,
     the reason (the `::evergreen-sync-error::` text, or the missing-row list, or "Docker Hub state
     read failed — nothing written"), says "Docker Hub is unaffected", and links the run.
  6. `Notify drift` (`if: always() && steps.drift.outputs.drift == 'true'`,
     `continue-on-error: true`): names the differing fields and links the run.
- Success posts nothing (FR-018).

## Caller wiring

### `cicd_evergreen-tracks-promote.yml`: new job after `apply`

```yaml
  site-sync:
    needs: [ apply ]
    if: >-
      always()
      && (needs.apply.result == 'success' || needs.apply.result == 'failure')
      && (github.event.inputs.repo || 'dotcms/dotcms') == 'dotcms/dotcms'
    uses: ./.github/workflows/cicd_comp_evergreen-site-sync.yml
    with:
      trigger: evergreen-tracks-promote (${{ github.event_name }})
      apply: true
      drift_check: ${{ github.event_name == 'schedule' }}
    secrets: { DOCKER_USERNAME, DOCKER_TOKEN, DOTCMS_DEVSITE_RELEASENOTES_TOKEN, SLACK_BOT_TOKEN }  # explicit mapping
```

This runs on every `apply`, including no-move days (daily heal, FR-011a). A *failed* apply still
syncs, because the mirror must match whatever actually landed on Hub. The existing `notify` job is
unchanged.

### `cicd_evergreen-tracks-admin.yml`

- Move `concurrency: evergreen-tracks-registry` from workflow level to the `admin` job, so the
  sync job never holds the registry lock.
- Add `permissions: contents: write` to the `admin` job. `github.token` needs it for `gh release edit`.
- Add steps to the `admin` job after "Run admin action" (default `success()` gate, so the title is
  touched only after the registry step succeeded):
  - `title` (`if: (inputs.action == 'taint' || inputs.action == 'untaint') && inputs.repo == 'dotcms/dotcms'`,
    `continue-on-error: true`, `working-directory: .github/actions/core-cicd/evergreen-tracks`,
    `GH_TOKEN: ${{ github.token }}`):
    1. `OLD=$(gh release view "v$VERSION" --repo "$GITHUB_REPOSITORY" --json name --jq .name)`.
       Fail if the command errors or `OLD` is empty.
    2. `NEW=$(uv run evergreen-tracks release-title --action "$ACTION" --title "$OLD")`.
    3. If `OLD == NEW`, print "title unchanged" and stop.
    4. If `MODE != apply`, print `DRY-RUN would retitle v$VERSION: '$OLD' -> '$NEW'` and stop.
    5. Otherwise run `gh release edit "v$VERSION" --repo "$GITHUB_REPOSITORY" --title "$NEW"`.
  - `Notify title failure` (`if: steps.title.outcome == 'failure'`, `continue-on-error: true`):
    a #dot-releases notice saying the registry taint/untaint **did** apply and the GitHub Release
    title did not, with the version and run link (FR-023).
- New job:

```yaml
  site-sync:
    needs: [ admin ]
    if: >-
      always()
      && inputs.repo == 'dotcms/dotcms'
      && (needs.admin.result == 'success' || needs.admin.result == 'failure')
    uses: ./.github/workflows/cicd_comp_evergreen-site-sync.yml
    with:
      trigger: evergreen-tracks-admin ${{ inputs.action }} ${{ inputs.track }} ${{ inputs.version }}
      apply: ${{ inputs.mode == 'apply' }}
      as_if_action:  ${{ inputs.mode != 'apply' && inputs.action  || '' }}
      as_if_version: ${{ inputs.mode != 'apply' && inputs.version || '' }}
      as_if_track:   ${{ inputs.mode != 'apply' && inputs.track   || '' }}
    secrets: …
```

### `cicd_6-release.yml`: new job after `promote-latest` and `changelog-site-publish`

```yaml
  evergreen-site-sync:
    name: Evergreen Site Sync
    needs: [ promote-latest, changelog-site-publish ]
    if: >-
      always()
      && (needs.promote-latest.result == 'success' || needs.promote-latest.result == 'failure')
    uses: ./.github/workflows/cicd_comp_evergreen-site-sync.yml
    with:
      trigger: release ${{ github.event.inputs.release_version }}
      apply: true
    secrets: …
```

`promote-latest` is already gated to `is_latest`, `main`, `dotcms/core` and not `skip_latest`.
When it is skipped (LTS, `skip_latest`) the sync is skipped too (FR-013). `finalize` and `report`
do **not** depend on this job, so it can't delay or fail the release (SC-005).
