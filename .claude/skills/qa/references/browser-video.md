# Browser automation + video recording (Playwright)

## Setup

- Only `playwright-core` is installed (no `@playwright/test`). Import from `'playwright-core'`.
- Scripts must run **from `core-web/`** (module resolution). Create them there, delete after the run.
- Browsers: `npx playwright install chromium` (once per machine).
- Always `ignoreHTTPSErrors: true` (self-signed cert) on both browser and context.

## Recording

```js
const context = await browser.newContext({
    ignoreHTTPSErrors: true,
    viewport: { width: 1280, height: 720 },
    recordVideo: { dir: VIDEO_DIR, size: { width: 1280, height: 720 } }
});
```

- The video file is only finalized on `context.close()`. Capture `page.video()` **before**
  closing, then `await video.path()` **after**. (`finalize()` in qa-helpers does this.)

## The human QA flow — every recording follows the full journey

The video must read like a competent QA engineer's screen share. No teleporting, no
unexplained activity, nothing on screen before the viewer knows what they're watching:

1. **Context before activity** — `openLogin(page)`, then `introCard()` over the login
   screen: issue number/title, the numbered acceptance criteria, environment, fix commit,
   and **what data this run seeded** (`seeded: [...]` or "none" — so on-screen content
   can't be mistaken for this run's fixtures). Hold ~5 s (`PACE.intro`).
2. **Narrated login** — `login(page)`: banner says who is logging in, the cursor is
   visible from the first click, credentials are typed on camera.
3. **Navigate like a user** — `navigateMenu(page, group, item)`: click through the left
   menu by visible labels, exactly as a human reaches the screen. `gotoHash()` is a
   teleport — last resort for surfaces with no menu entry, and its banner must disclose
   it on camera. (Its error lists available menu labels — use them, don't fall back
   silently.)
4. **Visible cursor everywhere** (`showCursor()` + `humanClick()`): every click travels
   the pointer to the target. Menu navigation keeps overlays alive (SPA route change);
   a `gotoHash()` full load kills them — re-apply cursor/checklist/toast-suppression after.
5. **Live criteria checklist** (`criteriaPanel()`): every acceptance criterion on screen,
   `○ pending` → `◐ running` when its steps begin → `✔`/`✘` **only after the assertion
   returns**. The panel documents the run; it never predicts it.
6. **Step banner** (`banner()`): what's happening right now, in plain words.
7. **Park + verdict** — `parkCursor()` moves the dot to neutral space (a dot resting on a
   checkbox reads as state), then `finalize(..., { criteria })` shows
   `PASS — 4/4 criteria verified (8 assertions)` — the count matches what the intro
   promised, held ~3 s (with tiny cursor moves: the paint-driven recorder truncates a
   fully static tail).

Pacing: `slowMo: 150` on launch, and one `PACE.beat` (~1.2 s) **after** each state change
so the viewer sees the result settle. Don't also pad before the action — a banner staring
at a static screen for seconds is dead air. Pacing changes watchability only; assertions
stay DOM-based and unaffected.

## What may go on the checklist

Only the issue's acceptance criteria. Extra assertions you find useful (intermediate
states, adjacent behavior) live in the console log and the QA brief — putting out-of-scope
claims on camera invites "why is that there?" and dilutes trust.

## Integrity rules — the video must be beyond doubt

- **One unedited take.** The delivered video is a single continuous recording of the run
  that produced the SUMMARY results. Never splice, trim failures out, or re-shoot a single
  step; if the script needs fixing, re-run the whole session and deliver the new take.
- **Verdicts follow assertions.** Nothing on screen may claim PASS/FAIL before the
  corresponding DOM/network assertion has actually returned in the script.
- **Overlays narrate, never obscure — and never intercept.** Banner top, checklist
  bottom-right, cursor dot — none may cover the UI element under verification, and every
  overlay MUST have `pointer-events: none`. An overlay that eats a click silently changes
  what the run actually did (learned the hard way: the banner sat over the nav toggle and
  swallowed its click, so raw `page.mouse.click` hit the banner while the video looked
  like a click on the app).
- **No staged state.** Only the declared seeded data (visible `qa-<issue#>-...` names on
  camera) — no hidden fixtures, no pre-toggled UI, no mocked responses. The intro card
  declares what this run seeded; leftover `qa-*` data from earlier runs must not be
  passed off as this run's.
- **Visually-true assertions.** `visible()` (offsetParent) is for absence/presence.
  Any claim the viewer should SEE uses `visibleArea()` — a DOM-attached but visually
  empty container (e.g. an actions bar with no actions for the selection) must FAIL,
  not pass. A technically-true-but-invisible PASS is a report finding, never a video claim.
- **System noise is suppressed, not cropped.** `suppressToasts()` keeps unrelated toasts
  (token expiry etc.) off camera for the whole session — cosmetic cleanup, disclosed here,
  touching nothing under verification.

## Convert + deliver

```bash
ffmpeg -i run.webm -c:v libx264 -pix_fmt yuv420p -movflags +faststart out.mp4
# ffmpeg missing? install via the machine's package manager (brew / apt / dnf)
cp out.mp4 "$HOME/Downloads/qa-<issue#>-<short-slug>.mp4"   # or leave in temp dir if no Downloads
```

## Frame forensics — mandatory before reporting a FAIL

```bash
ffmpeg -i run.webm -vf fps=1 frames/f_%03d.png
```

Then **Read the PNGs** around the failing step. A FAIL can be the script's fault:
on #36369 the "2 selected: Upload still hidden" check failed because the click had
*deselected* a row — visible instantly in the frames, invisible in the logs.

## Hard-won gotchas (each cost a full re-run once)

### Login: the submit button stays disabled
`page.fill` immediately after load does not enable Angular's login form. Do:

```js
await page.waitForSelector('[data-testid="userNameInput"]');
await page.waitForTimeout(1000);                                   // let Angular wire the form
await page.type('[data-testid="userNameInput"]', user, { delay: 20 });
await page.type('[data-testid="password"]', pass, { delay: 20 });
await page.waitForSelector('[data-testid="submitButton"]:not([disabled])');
await page.click('[data-testid="submitButton"]');
```

Older `playwright-core` has **no `pressSequentially`** — `page.type` is the portable call.

### Comma-selector double-match
`page.$$('td .p-checkbox, td p-tablecheckbox')` matched every row **twice** (40 handles for
20 rows), so `rows[1]` was row 0's checkbox again and clicking it toggled the selection OFF
→ false FAIL. Rules:
- One selector per element set, never comma-alternatives that can hit the same node.
- Sanity-check `handles.length` against the expected count (paginator rows-per-page)
  before any index math.

### Animation-gated `@if`
Elements inside `@if` leave the DOM only after their leave animation completes. Never assert
at a fixed delay after the trigger — poll with `waitFor(fn)` (200 ms interval, ~8 s budget).

### Toast overlays block clicks
System toasts cover top-right buttons → `click` times out at 30 s. `login()`/`navigateMenu()`
install `suppressToasts()` (MutationObserver) automatically; re-call it after a `gotoHash()`
full load. The worst offender — "API Tokens about to expire" — is self-inflicted: QA runs
that mint short-lived API tokens trip it for every later recording. Fix the cause, not the
symptom: Basic auth for seeding, revoke any minted token (environment.md § API auth).

### Legacy editor = iframe
The legacy content editor renders inside an iframe (struts JSP). Find the right frame by
iterating `page.frames()` for one where the target selector exists; interact via
`frame.evaluate(...)`.

### Which component actually rendered?
A URL being "legacy" (`#/c/content/...`) doesn't mean the field inside is the legacy
component — feature flags swap them. Confirm by DOM signature before hunting UI elements.

## Assertion patterns stronger than screenshots

- **Visibility**: `el.offsetParent !== null` by data-testid (see `visible()` helper).
- **Focus**: `page.evaluate(() => document.activeElement === document.querySelector(sel))`.
- **A request really left the browser**: `page.on('request', ...)` and filter the URL.
- **Downloaded file bytes**: `newContext({ acceptDownloads: true })`, register
  `page.waitForEvent('download')` **before** the click, then read `await dl.path()`.

## Automate-vs-human boundary

Deterministic state (visibility, focus, counts, network params, file bytes) → automate and
assert. Transient CSS (a fade that must not flash) → report as the one residual manual
check; never fake-certify it with fixed-delay screenshots.
