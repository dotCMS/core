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

The bearer token is read once from the `DOTCMS_DEVSITE_TOKEN` environment variable and is
never logged or echoed.

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
of markdown-generation concerns. (Recorded in `research.md` D2.)

## Test

    uv run pytest
