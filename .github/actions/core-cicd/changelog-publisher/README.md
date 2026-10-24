# changelog-publisher

Upserts a dotCMS release's changelog entry onto the public site
(dev.dotcms.com/docs/changelogs) by writing a `Dotcmsbuilds` contentlet on the dev-site
authoring (**auth**) instance and firing the configured workflow action — in CI, **Push
Publish**, which publishes on auth and then pushes the change to prod — idempotently,
protecting human edits, and never blocking the release. Nothing is written to prod directly.

There is no separate site-notes generation: the **GitHub release body is the changelog
content** (one shared artifact, written once by the ai-release-notes phase from
`gather-release-data/prompt-template.md`). This tool injects the site's per-version
heading anchors (`### Fixes` → `### Fixes {#Fixes-<version>}`) and strips GitHub's
auto-appended compare footer mechanically at publish time — so a hand-edit to the GitHub
release followed by a re-run re-syncs the site.

## Run locally (dry-run is the default)

    uv run changelog-publisher publish \
      --version 26.07.10-01 \
      --notes-file /tmp/site-release-notes.md \
      --docker-image dotcms/dotcms:26.07.10-01_<sha> \
      --released-date 2026-07-16

Pass `--apply` to actually search + fire the configured workflow action. Without it, the command prints
the intended action and issues no write.

### Arguments

| Flag | Required | Purpose |
|------|----------|---------|
| `--version` | yes | Current-track version / `minor` field, e.g. `26.07.10-01` (no `v`). Out-of-scope forms (`_lts_`, `dotcms-cli-*`) are rejected before any network call. |
| `--notes-file` | yes | Path to the release-notes markdown (normally the GitHub release body) to store in `releaseNotes`; heading anchors are injected automatically. |
| `--docker-image` | yes | Docker image tag stored in `dockerImage`. |
| `--released-date` | no | Availability date `yyyy-MM-dd`; defaults to today (UTC). |
| `--apply` | no | Actually search + fire. Omitted = dry-run (default). |
| `--service-account` | no | The service account's `modUser` id for human-edit protection; or set `DOTCMS_DEVSITE_RELEASENOTES_ACCOUNT`. |
| `--force` | no | **Operator override**: update in place even when the entry was last modified by a human (FR-011). The release workflow never passes this. |

### Configuration

| Env var | Required | CI source | Purpose |
|---------|----------|-----------|---------|
| `DOTCMS_DEVSITE_URL` | yes | repo var `AUTODOC_DOTCMS_BASE_URL` | Base URL of the instance to write to (auth). |
| `DOTCMS_DEVSITE_RELEASENOTES_TOKEN` | yes | secret `DOTCMS_DEVSITEAUTH_RELEASENOTES_TOKEN` | Service-account bearer token for that instance. |
| `DOTCMS_DEVSITE_RELEASENOTES_ACTION` | yes | repo var `DOTCMS_DEVSITE_RELEASENOTES_ACTION` | Workflow action id fired for every write (Push Publish on auth). |
| `DOTCMS_DEVSITE_RELEASENOTES_ACCOUNT` | no | repo var `DOTCMS_DEVSITE_RELEASENOTES_ACCOUNT` | Service-account `modUser` id for human-edit protection. |

The env var names are the tool's own contract; the workflows choose which repo variable or
secret feeds them. In CI, `DOTCMS_DEVSITE_URL` is fed from `AUTODOC_DOTCMS_BASE_URL` (shared with
`issue_autodoc.yml`), so writes land on **auth** and Push Publish carries them to prod. None of
these values is hardcoded, so an instance or action change is a settings change, not a code
change. The URL, token and action are all required: if any is unset or empty (an unset repo
variable renders as `""` in Actions) the tool exits 1 with a one-line error and makes no request.
It never falls back to a default action, because a wrong action could publish on auth and never
reach prod. The action id is logged with each fire so runs are auditable.

The bearer token is read once and is never logged or echoed. The service-account identity is
read from `--service-account` or `DOTCMS_DEVSITE_RELEASENOTES_ACCOUNT`; without it, an existing
entry with any last-modifier is treated protectively (skipped), never overwritten.

**Read-back verifies auth only.** Push Publish is asynchronous: a successful fire and the
read-back both check auth, so a push that later fails on its way to prod does not fail the run.
Check the push-publishing queue on auth when prod looks stale.

## Exit-code / stdout contract

The workflow branches Slack wording on this contract, so it is fixed here before any CLI code:

| Exit | Meaning | stdout marker |
|------|---------|---------------|
| `0` | **Success** — entry created/updated and published | *(none)* |
| `0` | **Protective skip** — entry last modified by a human; left untouched (FR-011) | `::changelog-skip::<version> reason=<reason>` on its own line |
| `2` | **Usage/validation error** — e.g. out-of-scope version string rejected before any network call (FR-007), missing required argument | *(none)* |
| `1` | **Runtime failure** — network/auth/payload error, or `>1` search hit (ambiguous, never guess) | *(none)* |

- A **protective skip keeps the run green** (exit 0) and is distinguished from success only
  by the `::changelog-skip::` marker line on stdout.
- Any **non-zero exit** is a real failure — never a skip. The marker never appears on a
  non-zero exit.
- This is what lets `cicd_comp_changelog-site-publish-phase.yml`'s single Slack notice say
  **FAILED** for a non-zero exit and **SKIPPED** when the marker is present (FR-008, US3).

## Site sync (`sync-site`)

Reconciles the auth instance's `EvergreenState` record (latest / standard / trailing / tainted) with a
Docker Hub snapshot written by `evergreen-tracks state`:

    uv run changelog-publisher sync-site --state-file hub-state.json [--apply]

Dry-run is the default; `--apply` writes, publishes and reads back (3 attempts, 60 s then 180 s
apart). Needs `DOTCMS_DEVSITE_URL`, `DOTCMS_DEVSITE_RELEASENOTES_TOKEN` and
`DOTCMS_DEVSITE_RELEASENOTES_ACTION` — even a dry-run, since the client requires all three.

| Exit | Meaning | stdout marker |
|------|---------|---------------|
| `0` | Record already matched | `::evergreen-sync::unchanged` |
| `0` | Written (`--apply`) | `::evergreen-sync::updated fields=<f1,f2>` |
| `0` | Dry-run difference | `::evergreen-sync::would-update fields=<f1,f2>` |
| `1` | Runtime failure after all attempts, or token / URL / action unset | `::evergreen-sync-error::<reason>` |
| `2` | Usage / validation error (bad state file), rejected before any network call | *(none)* |
| `3` | Reconciled, but a track kept its old value because its version has no release row | `::evergreen-sync-missing-row::<track>=<version>` (one per track) |

`fields` order is `latest,standard,trailing,tainted`. Run by the reusable workflow
`cicd_comp_evergreen-site-sync.yml`, after every registry mutation.

**Prerequisite (FR-044, manual):** the `EvergreenState` content type (`title` + JSON `state`) and
its single record exist on auth with the same identifier as on prod; the service-account token has
read / edit / Push Publish on it; the devsite's GraphQL reader has read access on prod.

## Manual backfill (explicit, per-version — never automatic)

There is **no automatic backfill** (FR-012): each release publishes only the version that
triggered it. To catch up a version that was missed (e.g. published before this automation
went live, or after an outage), run the tool **once per version** by hand:

    export DOTCMS_DEVSITE_URL=...                  # auth instance base URL (AUTODOC_DOTCMS_BASE_URL in CI)
    export DOTCMS_DEVSITE_RELEASENOTES_TOKEN=...   # service-account token for auth
    export DOTCMS_DEVSITE_RELEASENOTES_ACTION=...  # Push Publish action id (repo variable in CI)
    export DOTCMS_DEVSITE_RELEASENOTES_ACCOUNT=... # service-account modUser id
    # generate the site notes for that version, then:
    uv run changelog-publisher publish --version 26.06.30-01 \
      --notes-file /tmp/site-release-notes.md \
      --docker-image dotcms/dotcms:26.06.30-01_<sha> \
      --released-date 2026-06-30            # dry-run first
    # add --apply once the dry-run looks right

Human-edit protection (FR-011) applies during backfill too: an entry a person has polished
in the admin UI is skipped, not overwritten. Use `--force` only to deliberately override a
specific hand-edited entry.

## One-time end-to-end validation (operational check)

A true end-to-end publish (auth → Push Publish → prod) is validated **once, manually**
against auth (recorded here rather than as an automated test):

1. Dry-run against a throwaway version string to confirm search + intended action:
   `uv run changelog-publisher publish --version <throwaway> ... ` (no `--apply`).
2. `--apply` against that throwaway version; once the push completes, confirm exactly one row
   appears **on prod**, live on the changelog page, and the downloads listing is intact.
3. Re-run with `--apply` and edited notes; confirm exactly one row exists with the latest
   notes (idempotency, SC-003).
4. Hand-edit the entry in the admin UI, re-run; confirm it is skipped with the
   `::changelog-skip::` marker (FR-011).
5. Remove the throwaway entry on auth and push the removal to prod.

## Test

    uv run pytest
