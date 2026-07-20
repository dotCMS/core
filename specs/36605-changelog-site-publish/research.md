# Phase 0 Research: Changelog Site Publishing

Decisions below are grounded in live investigation of the `dotCMS/core` release pipeline
and the corpsites-headless backend (2026-07-16). Each records the choice, the alternatives
weighed, and why.

## D1 — Delivery tool: standalone Python CLI (mirror `evergreen-tracks`)

**Decision**: A new `changelog-publisher` Python package under
`.github/actions/core-cicd/`, using `requests` + `argparse`, `uv run` entrypoint, and
`pytest`+`responses` tests — structurally identical to the existing `evergreen-tracks`
package (pyproject with `[project.scripts]`, `src/<pkg>/` layout, `tests/`).

**Why**: The task brief names `evergreen-tracks` as the house pattern for new CI Python
tooling, and the release workflow already sets up `astral-sh/setup-uv@v5` + `uv run` with
`working-directory` pointing at that package. Reusing the exact idiom means the wiring, the
test stack (`responses`-mocked HTTP is the proven way `evergreen-tracks/executor.py` is
tested), and reviewer familiarity all transfer for free. Dry-run-by-default with `--apply`
is `evergreen-tracks`' safety convention and maps perfectly onto "preview the upsert, then
commit it."

**Alternatives rejected**:
- *Inline shell/`gh api` in the workflow* — the upsert decision, human-edit protection, and
  `disabledWYSIWYG` payload are real logic that must be unit-tested (Constitution V). Untestable
  bash fails TDD.
- *A TypeScript tool beside `gather-release-data`* — would co-locate with generation, but the
  delivery HTTP logic belongs in the mirrored Python house pattern the brief mandates, and
  `evergreen-tracks` proves the `responses` test idiom for exactly this shape of work.

## D2 — Generation: one data source, add a site-format prompt template

**Decision**: Reuse the existing deterministic `.github/scripts/gather-release-data` Node
gatherer (unchanged) and add a new `prompt-template-site.md` that renders the site's
editorial format. The site notes are produced by a `claude-code-action` step against the
same gathered JSON that feeds the GitHub release notes.

**Why**: The spec's drift-prevention strategy is explicitly "one generator feeding both."
The gatherer is deterministic given two tags, so regenerating for the site cannot diverge
from what GitHub got. FR-010 requires the *site* editorial format (section headings
Features / Enhancements & Adjustments / Fixes, per-item `[#N](url)` links, heading anchors
like `{#Fixes-<version>}`, a short prose intro, no emoji) which differs from the GitHub
template — so a distinct prompt template is the right seam, not a fork of the data path.

**Alternatives rejected**:
- *Transform the GitHub `/tmp/release-notes.md` into site markdown in Python* — turns the
  delivery tool into a markdown re-formatter and couples it to the GitHub template's exact
  output; brittle and mixes concerns.
- *A brand-new generator* — duplicates the gatherer and reintroduces the drift the spec is
  trying to kill.

**Golden-file placement (D2, resolved at T006)**: the site-format markdown golden-file test
lives in the existing `.github/scripts/gather-release-data/` jest suite (beside
`categorize.test.ts` / `github.test.ts`), **not** in the Python package — the golden file
guards the generation output (the `prompt-template-site.md` editorial format), a
Node/markdown concern, so it belongs with the generator it tests and keeps the delivery tool
free of markdown-generation concerns.

**Open question carried to tasks (D2a)**: whether one Claude invocation emits both
`/tmp/release-notes.md` and `/tmp/site-release-notes.md` (cheaper, single call) or the site
notes are a second lightweight `claude-code-action` step against `prompt-template-site.md`.
Recommendation: second step — keeps the existing GitHub-notes step untouched and the site
path independently retryable. Tasks phase decides.

## D3 — Exclusion of CLI + LTS releases (FR-007): reuse `is_latest`

**Decision**: Gate the publish job on the release-prepare output
`needs.release-prepare.outputs.is_latest == 'true'`. Additionally, the Python tool
independently validates the version string is current-track CalVer (`yy.mm.dd-##`) and
refuses `_lts_` / CLI forms (defense-in-depth).

**Why**: `cicd_comp_release-prepare-phase.yml` already computes `is_latest=true` only when
`release_version` matches `^[0-9]{2}.[0-9]{2}.[0-9]{2}-[0-9]{1,2}$`, and `is_lts=true` for
`_lts_v##`. That is precisely the deliberate current-track filter FR-007 asks for, already
used to gate the `promote-latest` job — reusing it means the exclusion is a real,
maintained signal, not fragile tag-pattern matching invented here. The AI release-notes
phase and the Slack report step use the equivalent guard (`!contains(_lts_)`,
`!contains(dotcms-cli-)`, `startsWith('v')`), so the new phase is consistent with its
siblings.

**Note on FR-013 (older-line patch)**: `is_latest` is a *format* match, not a recency
check — it is `true` for a `-04` hotfix of an older line too. So an older-version patch
still triggers a publish and gets its own entry with its actual (today's) date, exactly as
FR-013 requires; the tool never touches other rows.

## D4 — Idempotent upsert by `minor` (FR-003/004, SC-003)

**Decision**: Locate the existing entry with
`POST /api/content/_search`, query `+contentType:Dotcmsbuilds +Dotcmsbuilds.minor_dotraw:<version>`
(exact-match `_dotraw`). Zero hits → create; one hit → update-in-place by its identifier;
>1 hit → error (must never happen; surfaces as a Slack failure rather than guessing).

**Why**: The type doubles as the downloads registry; a duplicate corrupts a second
customer-facing surface (US2). Searching by the exact `minor_dotraw` value and keying the
fire call on the found identifier makes re-runs converge — the defining property of
idempotency. A partial failure mid-run leaves at most an already-correct or absent row, so
a retry reaches the same end state.

## D5 — Publish via System Workflow Publish action, markdown preserved

**Decision**: Fire `PUT /api/v1/workflow/actions/b9d89c80-3d88-4311-8365-187323c96436/fire`
with the full field set and `"disabledWYSIWYG": ["releaseNotes"]` in the payload.

**Why**: The `Dotcmsbuilds` type uses System Workflow (scheme
d61a59e1-a49c-46f2-a929-db2b4bfa88b2) which has no approval step, so a single fire call
makes the entry live with no human gate (FR-001). Without `disabledWYSIWYG` on the
WYSIWYG `releaseNotes` field, dotCMS collapses the markdown into a single `<p>` and the
site renders it wrong — this is the known storage gotcha the spec calls out (FR-005) and
gets its own regression unit test (assert the key is always present in the payload).

## D6 — Human-edit protection (FR-011, SC-006)

**Decision**: Read `modUserName` from the found entry's metadata. If it is not the
automation's service account, **skip** the write, return a distinct skip status, and let
the workflow post a skip notice to Slack. `--force` overrides and is only reachable via a
manual operator re-run — the release pipeline never passes it.

**Why**: The automation authenticates as a dedicated service account, so "last modified by
someone else" means a human polished the entry in the admin UI; overwriting would silently
destroy their work (SC-006 says zero such losses). Skip-and-notify (not fail) keeps the run
green while handing the decision to the team (US3 scenario 3).

## D7 — Slack notifications to `#dot-releases` (FR-008)

**Decision**: Reuse the existing `.github/actions/core-cicd/notification/notify-slack`
composite action (takes `channel-id`, `payload`, `slack-bot-token`). Post on the publish
job's failure OR on a protective skip, naming the version and reason. The publish job is
`continue-on-error`/`allow_failure: true` so its failure produces a notification without
failing the release.

**Why**: The spec requires reusing the established Slack mechanism, not inventing one. The
`notify-slack` action already backs the release pipeline (`SLACK_BOT_TOKEN` is already a
release secret). A silent failure would recreate today's stale-changelog problem minus the
human who used to notice (US3), so every failure/skip must surface (SC-005). Blocking the
release on a changelog hiccup is explicitly forbidden.

**Open question (D7a)**: the numeric channel-id for `#dot-releases` must be supplied
(the action takes an id, not a name). Tasks phase resolves the id (or a repo/org variable
holding it).

## D8 — Credential (FR-009, Constitution III)

**Decision**: New managed GitHub secret `DOTCMS_DEVSITE_TOKEN`, passed to the publish job
as env, read once by the tool, never logged and never written to `GITHUB_OUTPUT` or the
step summary.

**Why**: FR-009 forbids personal tokens; the token authenticates the service account whose
identity also powers human-edit protection (D6). Confirmed the existing `DEV_REQUEST_TOKEN`
secret is a Docker *build-arg* for the `dotcms/dotcms-dev` image, unrelated to the corpsites
CMS — so a distinct secret is genuinely needed, not a reuse. Provisioning the token (and
granting its service account publish rights on `Dotcmsbuilds`) is an operational
prerequisite, not part of this feature's code.

## Ground-truth references (verified 2026-07-16)

- `cicd_comp_release-prepare-phase.yml`: outputs `release_version` (e.g. `26.03.13-02`, no
  `v` → the `minor` field), `release_tag` (`v…`), `is_latest`, `is_lts`.
- `cicd_comp_ai-release-notes-phase.yml`: existing generation phase; skips CLI/LTS via
  `if:` guard; runs `gather-release-data` (Node/TS) → JSON → `claude-code-action` writes
  `/tmp/release-notes.md` → `gh release edit`. This is the sibling the new phase mirrors.
- `cicd_6-release.yml`: `release-notes` job runs after `release` with `allow_failure: true`;
  `promote-latest` job gates on `is_latest == 'true'` — the guard pattern reused for D3.
- `.github/scripts/gather-release-data/`: deterministic TS gatherer + `prompt-template.md`;
  categories feature / fix / deprecation / infrastructure.
- `.github/actions/core-cicd/evergreen-tracks/`: house pattern (pyproject, `requests`,
  `responses` tests, dry-run/`--apply`).
- `.github/actions/core-cicd/notification/notify-slack/action.yaml`: reusable Slack action.
- corpsites backend: `Dotcmsbuilds` content type, System Workflow (no approval), Publish
  action id `b9d89c80-3d88-4311-8365-187323c96436`, `_search` by `minor_dotraw`, entry
  metadata carries `modUserName`.
