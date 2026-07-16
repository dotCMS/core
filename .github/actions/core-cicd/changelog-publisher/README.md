# changelog-publisher

Upserts a dotCMS release's changelog entry onto the public site
(dev.dotcms.com/docs/changelogs) by writing a `Dotcmsbuilds` contentlet on the
corpsites-headless backend and firing the System Workflow Publish action so it goes live —
idempotently, protecting human edits, and never blocking the release.

This is the *delivery* half of the feature. *Generation* (site-format markdown) is the
`.github/scripts/gather-release-data` Node gatherer plus `prompt-template-site.md`.

## Run locally (dry-run is the default)

    uv run changelog-publisher publish \
      --version 26.07.10-01 \
      --notes-file /tmp/site-release-notes.md \
      --docker-image dotcms/dotcms:26.07.10-01_<sha> \
      --released-date 2026-07-16

Pass `--apply` to actually search + fire the Publish action. Without it, the command prints
the intended action and issues no write.

### Arguments

| Flag | Required | Purpose |
|------|----------|---------|
| `--version` | yes | Current-track version / `minor` field, e.g. `26.07.10-01` (no `v`). Out-of-scope forms (`_lts_`, `dotcms-cli-*`) are rejected before any network call. |
| `--notes-file` | yes | Path to the site-format markdown notes to store in `releaseNotes`. |
| `--docker-image` | yes | Docker image tag stored in `dockerImage`. |
| `--released-date` | no | Availability date `yyyy-MM-dd`; defaults to today (UTC). |
| `--apply` | no | Actually search + fire. Omitted = dry-run (default). |
| `--service-account` | no | The service account's `modUser` id for human-edit protection; or set `DOTCMS_DEVSITE_SERVICE_ACCOUNT`. |
| `--force` | no | **Operator override**: update in place even when the entry was last modified by a human (FR-011). The release workflow never passes this. |

### Credentials

The bearer token is read once from the `DOTCMS_DEVSITE_TOKEN` environment variable and is
never logged or echoed. The service-account identity (a `modUser` id) is read from
`--service-account` or `DOTCMS_DEVSITE_SERVICE_ACCOUNT`; without it, an existing entry with
any last-modifier is treated protectively (skipped), never overwritten.

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
- This split is what lets `cicd_comp_changelog-site-publish-phase.yml` post **failure**
  wording for a non-zero exit and **skip** wording when the marker is present (FR-008, US3).

## Golden-file placement

The site-format markdown golden-file test lives in the existing
`.github/scripts/gather-release-data/` **jest** suite (alongside `categorize.test.ts` /
`github.test.ts`), **not** in this Python package. Rationale: the golden file guards the
*generation* output (the `prompt-template-site.md` editorial format), which is a
Node/markdown concern; keeping it with the generator it tests leaves this delivery tool free
of markdown-generation concerns.

## Manual backfill (explicit, per-version — never automatic)

There is **no automatic backfill** (FR-012): each release publishes only the version that
triggered it. To catch up a version that was missed (e.g. published before this automation
went live, or after an outage), run the tool **once per version** by hand:

    export DOTCMS_DEVSITE_TOKEN=...            # service-account token
    export DOTCMS_DEVSITE_SERVICE_ACCOUNT=...  # service-account modUser id
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

There is no non-prod corpsites instance reachable from CI, so a true end-to-end publish is
validated **once, manually** (recorded here rather than as an automated test):

1. Dry-run against a throwaway version string to confirm search + intended action:
   `uv run changelog-publisher publish --version <throwaway> ... ` (no `--apply`).
2. `--apply` against that throwaway version; confirm the entry appears live on the changelog
   page and the downloads listing is intact.
3. Re-run with `--apply` and edited notes; confirm exactly one row exists with the latest
   notes (idempotency, SC-003).
4. Hand-edit the entry in the admin UI, re-run; confirm it is skipped with the
   `::changelog-skip::` marker (FR-011).
5. Remove the throwaway entry.

## Test

    uv run pytest
