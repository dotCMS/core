---
name: qa
description: QA a dotCMS GitHub issue or PR end-to-end against a local dotCMS instance, producing a recorded video as proof of verification. Use when the user asks to QA, verify, or validate an issue/PR fix (e.g. "/qa 36369", "qa this issue", "verify #36369 with video"). Handles UI issues (browser video) and API issues (recorded evidence harness).
---

# /qa — QA a GitHub Issue with Video Verification

Verify that a merged fix actually satisfies its issue's acceptance criteria, by exercising
the real running application — not just reading the diff — and deliver a recorded video
as proof, plus a terse QA brief and a draft GitHub comment.

## Usage

```bash
/qa 36369                                        # issue number
/qa 36371                                        # PR number (resolves the linked issue)
/qa https://github.com/dotCMS/core/issues/36369  # full URL
```

The run is **fully autonomous**: derive the test plan from the issue and execute it without
asking for confirmation. Only stop for genuine blockers (no merged fix, environment cannot
start, acceptance criteria absent AND undeducible).

## Pipeline

### STEP 1 — Resolve input → issue + merged PR + fix commit

`$ARGUMENTS` may be an issue number, a PR number, or a URL to either.

- Try as issue first: `gh issue view <n> --repo dotCMS/core --json title,body,state,labels`.
  If it's actually a PR, `gh` errors — retry as PR and resolve its linked issue from the body
  (`Fixes #...`).
- Find the merged PR + merge commit via GraphQL timeline:

```bash
gh api graphql -f query='{ repository(owner:"dotCMS", name:"core") { issue(number:N) {
  timelineItems(itemTypes:[CROSS_REFERENCED_EVENT], first:20) { nodes {
    ... on CrossReferencedEvent { source { ... on PullRequest {
      number state merged mergeCommit { oid } } } } } } } } }'
```

- Extract from the issue body: **acceptance criteria** (checklist items), steps to reproduce,
  and any explicit out-of-scope statements. From the PR: changed files
  (`gh pr view <n> --json files`) and test plan claims.
- If there is **no merged PR**, stop: report that the issue has nothing to QA yet.

### STEP 2 — Classify the issue and choose evidence mode

- **UI issue** (changed files under `core-web/`, behavior visible in dotAdmin):
  browser-driven checks recorded as video. This is the primary mode.
- **API issue** (REST/backend behavior): drive the UI surface that reflects the behavior
  when one exists; otherwise use the **evidence-harness page** (a generated HTML page that
  executes the API calls via fetch and renders PASS/FAIL rows, recorded on video) — see
  `references/api-evidence.md`.
- **Mixed**: do both in one recording session.

### STEP 3 — Provenance: prove the fix is what you're testing

Never trust "the PR is merged". Follow `references/environment.md` § Provenance:

1. `git merge-base --is-ancestor <fixCommit> HEAD` — fix must be in the local tree.
2. If an instance is already running: confirm the served bundle was built from a tree
   containing the fix (container `Created` time + bundle mtimes + `git reflog --date=iso`
   + `merge-base --is-ancestor <fixCommit> <commit-at-build-time>`).
3. If the bundle predates the fix: rebuild (`./mvnw install -pl :dotcms-core --am -DskipTests`)
   and restart the environment before testing.

### STEP 4 — Cheapest ground truth first: the PR's own tests

Run the spec files the PR added or changed (frontend example):

```bash
cd core-web && corepack pnpm nx test <project> -- <spec-file-pattern>
```

- Suite-level failure with **0 tests run** (`Cannot find module ...`) = stale
  `node_modules` → `corepack pnpm install` and retry. Not a product failure.
- Backend: run the specific IT class only (`-Dit.test=Class#method`) — never the full suite.

### STEP 5 — Environment: detect, else start

```bash
curl -sk -o /dev/null -w "%{http_code}" https://localhost:8443/dotAdmin/ --max-time 5
```

- `200|302` → reuse it (after the STEP 3 provenance check).
- Otherwise: `just dev-run-fixed` (background) then `just dev-wait-ready`.
  Login `admin@dotcms.com` / `admin`. Full recipes and gotchas: `references/environment.md`.

### STEP 6 — Seed test data if the checks need it

Seed via API, never by driving forms. Recipes: `references/api-seeding.md`.

- **Prefix every seeded object `qa-<issue#>-<epoch>`** and **leave it in place** after the
  run — the video references real visible names and the data stays inspectable.

### STEP 7 — Execute the checks, recorded

Map **each acceptance criterion to one or more concrete assertions** (DOM state, network
request, API response). Write a throwaway Playwright script in `core-web/`
(module resolution requires it), importing the shared helpers:

```js
import { chromium } from 'playwright-core';   // MUST be imported here, not in the helpers
import { launchRecorded, openLogin, login, navigateMenu, gotoHash, introCard, showCursor,
         criteriaPanel, humanClick, banner, visible, visibleArea, waitFor, makeChecker,
         parkCursor, finalize, PACE }
    from '../.claude/skills/qa/scripts/qa-helpers.mjs';
const { browser, context, page } = await launchRecorded(chromium, VIDEO_DIR);
```

**Test like a human QA engineer — the full journey, on camera** (complete flow + gotchas
in `references/browser-video.md`):

- Record 1280×720 to a temp directory outside the repo (`mktemp -d` or the session
  scratchpad — never inside the working tree).
- Flow: `openLogin()` → `introCard()` (issue, numbered acceptance criteria, env, fix
  commit, **seeded-data declaration**, held ~5 s) → narrated `login()` → reach the screen
  via `navigateMenu()` clicks like a real user (`gotoHash()` only when no menu entry
  exists, disclosed on its banner) → criteria with `humanClick()` + `banner()` →
  `finalize(..., { criteria })` for a verdict that counts criteria, not raw assertions.
- Live checklist (`criteriaPanel()`) shows **only the issue's acceptance criteria**;
  each flips `✔`/`✘` strictly after its assertion returns. Extra assertions stay in the
  log and brief, never on camera.
- Visibility honesty: `visible()` for absence/presence; `visibleArea()` for anything the
  viewer must SEE — an attached-but-empty container must fail, not pass.
- Pacing: `slowMo: 150`, one `PACE.beat` after each state change (not before —
  announcement-then-frozen-screen is dead air). `suppressToasts()` keeps system noise off
  camera. `parkCursor()` before the verdict.
- **One unedited take** — the delivered video is the exact run that produced the results.
  Full integrity rules: `references/browser-video.md` § Integrity.
- Poll with `waitFor()` for anything animation-gated — `@if` blocks leave the DOM only
  after their leave animation.
- **One selector per element set** — never comma-alternatives that can match the same
  element twice. Sanity-check handle count against the expected row count before index math.
- Delete the throwaway script when done. Never commit it.

### STEP 8 — Self-verify before reporting any FAIL

An automated FAIL is a claim, not a fact. Before reporting it:

1. Extract frames (`ffmpeg -vf fps=1`) and **Read the PNGs** around the failing step —
   determine whether the product misbehaved or the script did.
2. Script artifact (wrong selector, timing, stale handle) → fix the script, re-run once.
3. Reproducible product failure → it's a genuine FAIL. **Report only**: brief + video
   clearly marked FAIL with the exact failing criterion. No root-cause hunt, no draft
   comment — the human decides what happens next.

### STEP 9 — Deliver

1. Convert and place the video:
   `ffmpeg -i <run>.webm -c:v libx264 -pix_fmt yuv420p -movflags +faststart out.mp4`
   → copy to the user's Downloads folder if one exists (`$HOME/Downloads`), otherwise leave
   it in the temp dir — either way name it `qa-<issue#>-<short-slug>.mp4` and state the
   full path in the brief.
2. QA brief in chat — terse one-liner bullets, exactly three sections:
   - **Tested** — ✅/❌ per scenario, one line each, mapped to acceptance criteria
   - **Notes** — environment, provenance, anything nonstandard
   - **Issue** — genuine product problems found, or "none"
3. **Draft GitHub comment** (markdown: verdict, criteria checklist with evidence, video
   mention). Show it in chat.

**Posting gate — hard rule:** never post to GitHub automatically. Ask the human to
(a) watch the video and (b) approve the draft. Only after **explicit approval in this
session** post it with `gh issue comment`. The video cannot be attached via `gh` — tell
the user to drag the mp4 into the comment on github.com if they want it attached.
On FAIL, skip the draft entirely (report only).

## Hard rules

- Never commit anything as part of a QA run.
- Never run the full integration suite.
- Seeded data: prefix `qa-<issue#>-<epoch>`, leave in place.
- **Zero footprint beyond declared seed data.** Use Basic auth locally; never leave API
  tokens behind (they trip the expiry warning that pollutes later recordings) — revoke
  any minted token: `PUT /api/v1/apitoken/<tokenId>/revoke`.
- Throwaway scripts: create in `core-web/`, delete after the run.
- Screenshots/frames beat assumptions: when automated results and expectations disagree,
  the frames are the tiebreaker.
