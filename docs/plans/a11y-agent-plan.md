# dotCMS Accessibility-Fix Agent + Accessibility Studio — Plan

> **Status:** Design locked, not yet implemented.
> **Date:** 2026-06
> **Owner:** Freddy Montes
> **North-star framing:** a11y is the **wedge**, not the destination. The long-term
> vision is "dotCMS-Lovable" — conversation-driven page editing with live preview,
> where an agent makes changes you watch and approve. This builds the first slice of
> that shell, themed as accessibility; every decision is chosen to survive into the
> general experience (§7 "Why the spine survives").

---

## 1. Problem & goal

**Problem.** Fixing a11y violations on dotCMS pages is manual: run a scan, read the
violations, hunt for the source file (theme VTL, container VTL, widget) behind each
offending element, edit it, re-check — across many pages. The scan exists (Page
Scanner / Page Health app); everything after it is hand work.

**Goal.** A dedicated agent service (Node/TS Nx app in `core-web`, reached via a
dotCMS proxy — §3/§4) that, given a page URL, runs the full loop:

```
SCAN → LOCATE → READ → FIX-TO-WORKING → RE-SCAN(working) → REPORT
```

It auto-fixes the safely fixable violations — in **VTL templates and CSS** (including
contrast) — to the **working** version (never publishes), triages the rest, and returns
a structured report. The full-screen
**Accessibility Studio** opens to a **page picker** (list + search); the user selects a
page, watches the agent's progress stream beside a live preview, then reviews and decides
whether to publish.

**Why now.** The enabling endpoints just merged (SCAN was live; LOCATE #36102 and
READ/WRITE #36112 landed), so the loop is mechanically reachable for the first time (§2).

**Primary user (v1).** Whoever edits pages in UVE and owns a11y compliance — content
editors and front-end developers.

> **Reviewers:** decisions are locked in §3 (reasoning in-line + **Alternatives
> considered**). Highest-leverage targets to poke: **§9 (risks)** and **§11 (open
> questions)**. Challenge the *reasoning*, not just the decision.

---

## 2. What already exists (the loop's backing)

The loop `SCAN → LOCATE → READ/WRITE → RE-SCAN` is called out explicitly in PR #36102.
**All backing endpoints are merged** — no PR dependency blocks us. (The one mechanic that
was *unverified* — the minted-JWT's acceptance on locate/assets — is now **CONFIRMED ✅**
by the S0 spike, §9. The loop composes end-to-end through the real sandbox.)

| Step | Endpoint | Status |
|------|----------|--------|
| **SCAN** | `POST /api/v1/page-scanner/a11y/check` `{ url }` | ✅ merged & working. Returns axe-style violations. The agent POSTs `{ url }`; the proxy handles auth/rendering internally (token mechanics in §3 Tool surface + §8.2). Response shape → §6. |
| **LOCATE** | `GET /api/v1/page/_render-sources/{uri}` | ✅ merged (PR #36102). Maps a page → exact source file refs (theme VTLs, container VTLs by content-type, widget VTL/CODE refs, urlContentMap). References only, no content. Host via `host_id` query param (the `//host/...` qualified-path form is rejected by `NormalizationFilter`). |
| **READ/WRITE** | `GET/PUT /api/v2/assets` | ✅ merged (PR #36112). Read raw VTL/CSS/JS by path or id; `PUT /save` (working only) / `PUT /publish`. Returns persisted `fileSize`. Working-save preserves live; only publish promotes. |
| **RE-SCAN** | (SCAN again, against a preview URL) | ✅ (reuses SCAN) |

Every call composes through one generic authenticated API runner, so the loop is
reachable today. The agent service adds what doesn't exist yet: the autonomous loop,
triage, preview re-scan confirmation, the structured report, and the Studio UX.

### `agentic-tools` — the shared runtime (existing)

`core-web/libs/agentic-tools` exports runtime primitives (`createExecutor`,
`createApiAdapter`, `createSandbox`, `loadDotCMSContext`): a dual-runtime sandbox that
runs JS against the dotCMS API via an `api.request(...)` adapter, with the auth token
injected by the host and **never** exposed to sandbox code. Its one consumer today is
`apps/mcp-server` ([xmcp](https://xmcp.dev), `search` + `execute` tools). **The agent
service is a second, independent consumer** — not built on the MCP server.

---

## 3. Locked decisions

| Decision | Choice | Notes |
|----------|--------|-------|
| **Form factor** | Standalone agent service | `core-web/apps/a11y-agent` (Nx app, sibling to `mcp-server`). Productionizable; triggered via REST. |
| **Autonomy** | Auto-fix to **working**, never publish | Matches #36112 working-save contract. Human publishes. |
| **Rollback / undo** | **Out of scope** — no batch undo; rely on dotCMS version history | No snapshots/manifests/Discard in v1. The agent only writes *working* (never live), so the worst case is reverting working versions via dotCMS's per-asset version history (every `save-working` creates a new version). The report still flags **`failed`/`regressed`** results (§6) so nothing publishes blind. |
| **Fix scope** | Triage everything; **fix VTL *and* CSS** (incl. contrast) | Auto-fix source-VTL issues **and CSS contrast** — contrast is the most common violation class, so punting on CSS punts on most of the real work. CSS edits go to the same `/api/v2/assets` working version. **Per-run "skip CSS" opt-out** for users who don't want visual changes. Shared-rule/token changes fan out site-wide — **by design** (a theme-level issue should be fixed everywhere); the report flags the fan-out (`blastRadius`, §9) so the human sees scope before publishing. Still report-only: content-field / third-party / JS-injected / ambiguous. |
| **LLM brain** | **Vercel AI SDK** (`ai`) + `@ai-sdk/anthropic` *(v1)* | Claude Opus/Sonnet 4.x for v1. **The provider is not locked** — production may use a different model/provider (Azure, Bedrock, dotAI-backed). The SDK is chosen precisely so that swap needs no loop rewrite; only the v1 default + Anthropic key are committed here. |
| **Agent loop** | `generateText` + `tools` + `maxSteps` | Built-in agentic tool-use loop. |
| **Tool surface** | **3 stages: prompt-guard (solo spike) → path allowlist (MVP) → typed tools (pre-GA)** | Every dotCMS call is an `api.request(...)` through the `@dotcms/agentic-tools` sandbox. The minted token is short-*lived* but not short-*scoped* — it carries the user's full permissions (can publish/delete), so the **boundary is what the agent can call, not the token**. Three stages: **(A) solo spike** — raw `execute` + system prompt + non-dotCMS-origin guard (fine because it's only us). **(B) MVP** — keep raw `execute` but the adapter interceptor enforces a **path allowlist**: permit only `page-scanner/a11y/check`, `_render-sources` GET, `/api/v2/assets` GET + `/save`; **hard-reject** publish/delete/workflow/config at the wire. Cheap (one function at the chokepoint), structural (blocks prompt-injection → destructive call), forward-compatible. **(C) pre-GA** — wrap in ~4 typed tools (`scanA11y`, `locateSources`, `readAsset`, `saveWorking`) so destructive calls aren't even *expressible*. The allowlist (B) is the spec for (C). |
| **Re-scan confirmation** | Scan the **`?mode=EDIT_MODE` URL** | After save-working, re-scan `https://<instance>/<path>?host_id=<id>&mode=EDIT_MODE` (§8.2) — renders the working version. Scanner reaches & authenticates to it (`debug-auth.mjs`), so no static fallback needed. |
| **Trigger** | Self-contained **Studio** with a page picker → dotCMS proxy → agent service REST | Studio opens to a page list/search (dotCMS `_search`); user picks **one page** (v1) to run. No UVE dependency. Proxy **reuses `PageScannerResource`'s auth + short-lived-JWT-mint half**; forward half is a streaming SSE relay (§8). A UVE "Fix accessibility" deep-link is a *future* convenience (§ deferred). |
| **Reporting** | Structured JSON run report | Per-violation status, file + diff, before/after scan deltas, guidance for unfixable. |
| **Experience spine** | Live preview + streaming agent status | Single "Fix accessibility" action (no free chat yet). Layout *is* the future-vision spine; the conversation brain is deferred. |
| **Placement** | Dedicated full-screen **Studio** route | Self-contained: opens to a page picker, no UVE handoff/context-serialization needed. |
| **v1 reach** | Watch → review → **Publish** (whole batch) | No per-fix accept/reject, no free-form steering, **no Discard/rollback** (undo via dotCMS version history — §3). |

### Alternatives considered (and why not)

| Decision | Alternatives weighed | Why the chosen one |
|----------|---------------------|--------------------|
| **LLM brain** *(v1 only; provider not locked)* | Anthropic SDK directly · Claude Agent SDK · dotAI/LangChain4j (in-core, no external key) | Vercel AI SDK is the v1 pick *because* it's provider-portable — production can repoint at Azure/Bedrock/dotAI with no loop rewrite. Agent SDK was heavier than needed; raw Anthropic SDK less portable; dotAI-in-core would mean building the loop in Java against whatever model the customer wired up. The SDK keeps that door open. |
| **Tool surface** | Raw `execute` sandbox throughout · typed tools from day one | **Phased** (see §3): raw `execute` for the spike — zero wrapper, fastest discovery; **typed tools before production** — safety by construction (unsafe calls aren't expressible) and auditable run logs. Typed-from-day-one was rejected only because the loop's exact endpoint set is still being discovered; raw-throughout was rejected because arbitrary JS is too broad a boundary for users (Codex). The phase boundary captures both. |
| **Autonomy** | Propose-only (human approves every diff) · fully autonomous incl. publish | Auto-fix to **working, never publish** matches the #36112 working-save contract: live keeps serving old bytes, the human publishes. Propose-only is more timid than the working-version safety net warrants; auto-publish touches live pages — too far for v1. |
| **Fix CSS contrast?** | Skip CSS, report-only · element-scoped CSS only | **Fix CSS too** (with per-run opt-out). Contrast is the most common violation class — skipping CSS means skipping most of the real work and likely never returning to it. Shared-rule/token fan-out is accepted by design (a theme issue *should* be fixed everywhere) and surfaced via the report's `blastRadius` flag + human-publish (§9). Heavier governance (approval, chat-to-steer) comes later. |
| **UX shape** | Report-first checklist · per-violation pin inspector (Figma-comments) | The conversation-column + live-preview **spine** is the only shape that generalizes to the dotCMS-Lovable north star; report-first and pins are a11y-specific dead-ends for the broader vision (§7). |
| **v1 control** | Per-fix accept/reject · free-form chat steering · batch rollback | v1 is watch → review → publish. Per-fix control, chat, and a true batch-undo all sidestep the hardest state problem (snapshots, per-fix/partial-batch undo); deferred. Undo in v1 = dotCMS's per-asset version history (§3). |
| **Form factor** | Claude Code subagent+skill · dedicated MCP tools | A standalone service is productionizable (UVE button, later cron/batch) rather than tied to an AI client. |

---

## 4. Architecture

*Target (shipped v1, with streaming). Phase 2 (§10) is the same shape with a plain
JSON response instead of SSE — see §8 naming note.*

```
                                        ┌────────────────────────────────┐
   user opens Studio,                   │  Accessibility Studio route     │
   picks a page from the     ──────────▶│  (full-screen Angular portlet;  │
   list/search (_search)                │   page picker → select one)     │
                                        └──────────────┬──────────────────┘
                                                       │ start run (SSE)
                                        ┌──────────────▼──────────────────┐
                                        │ dotCMS proxy REST resource       │
                                        │ (reuse PageScannerResource auth + │
                                        │  short-lived-JWT mint; forward    │
                                        │  half rewritten as SSE relay)     │
                                        └──────────────┬───────────────────┘
                                                       │ POST /fix (contract: §8.2) (stream)
                                        ┌──────────────▼───────────────────┐
                                        │  apps/a11y-agent (Node/TS)        │
                                        │  Vercel AI SDK + @ai-sdk/anthropic│
                                        │  generateText + tools + maxSteps  │
                                        │  tool: agentic-tools execute sandbox│
                                        │  emits step events + final report │
                                        └──────────────┬───────────────────┘
                                                       │ api.request(...) w/ injected short-lived JWT
                                                       ▼
                                  dotCMS REST: page-scanner/a11y/check,
                                  /page/_render-sources, /api/v2/assets
```

---

## 5. The loop (per page)

1. **SCAN** — `POST /api/v1/page-scanner/a11y/check { url: liveUrl }` → violations (axe-style).
2. **LOCATE** — `GET /api/v1/page/_render-sources/{uri}` (PR #36102) → theme VTLs,
   container VTLs per content-type, widget VTL/CODE refs, urlContentMap.
3. **TRIAGE + ATTRIBUTE** — for each violation, (a) classify it and (b) **attribute
   the offending DOM node to the exact source fragment**:
   - **VTL/template** (missing `alt`, ARIA, `lang`, heading order, `label`/`for`,
     inline-style contrast) → fix path.
   - **CSS contrast** (a stylesheet rule or a `--token`) → fix path *unless the run opted
     out of CSS*. Estimate/flag blast radius (element-scoped vs shared rule vs token).
   - **content-field / third-party / JS-injected / ambiguous** → report-only with guidance.
   - **attribution** — `/_render-sources` gives candidate source refs and experiments show the
     model maps axe selector/HTML → the right file reliably (§9). **Gate on evidence:** confirm
     the READ file (step 4) actually contains the offending markup/selector before editing;
     if not (deep nesting, JS-injected DOM, cascade ambiguity) → report, don't guess-edit.
4. **READ** — `GET /api/v2/assets?path=...` (PR #36112) → raw VTL **or CSS** (`.css`/
   `.dotsass`) for each source ref tied to a fixable violation.
5. **FIX-TO-WORKING** — model produces a minimal diff; `PUT /api/v2/assets/save` (working
   only). Verify persisted `fileSize`. **Never `/publish`.** The agent does **not** guard
   against pre-existing unpublished edits — the goal is to fix the a11y issue, and working-
   save is non-destructive (dotCMS keeps per-asset version history, §3), so overwriting an
   in-progress working edit is acceptable and recoverable. (An earlier "refuse-if-dirty"
   guard was **removed** — it misfired on the agent's *own* in-run edits: the second
   violation in a file saw working≠live from the first fix and skipped. The loop now keeps
   one progressively-improved working copy per file; later violations build on earlier edits.)
   Do **not fabricate semantic content** — generic alt/ARIA can pass axe while being poor
   a11y; report instead.
   For **contrast**: nudge the *existing* color to clear the WCAG threshold (AA 4.5:1 normal
   / 3:1 large), don't invent a new brand color; if the threshold can't be met without a
   design decision, report. Flag shared-rule/token edits (wide blast radius) in the report.
6. **RE-SCAN (working)** — scan `…?host_id=<id>&mode=EDIT_MODE` (§8.2) → confirm the
   violation cleared; compute before/after delta. Axe-pass is necessary but **not
   sufficient** — also confirm the VTL still parses/renders (a non-error working render is the
   minimum signal). The scanner reaches it, so this always runs. **If the re-scan proves the
   edit made things worse** (more violations, or the page errors), **auto-revert that asset to
   its prior version** and report `regressed (reverted)` — don't leave a known-bad edit on
   working for the human to trip over.
7. **REPORT** — structured JSON (see §6).

> **All steps are plain `api.request(...)` calls through the one `agentic-tools`
> sandbox — no adapter change needed.** Scan/rescan POST `{ url }` to the page-scanner
> proxy (re-scan uses the preview/working URL, §8.2); the proxy adds both tokens
> server-side, so the agent holds none (see §2 SCAN row + §3 "Tool surface").

---

## 6. Output contract (structured JSON run report)

```jsonc
{
  "runId": "r_01J…",                                        // §8.2; echoes the request
  "page": { "uri": "...", "host": "...", "languageId": 1 },
  "scan": { "before": { "violations": 12 }, "after": { "violations": 5 } },
  "results": [
    { "ruleId": "image-alt", "status": "fixed-to-working",
      "file": "//.../travel/header.vtl", "identifier": "a56e…",
      "diff": "…" },
    { "ruleId": "color-contrast", "status": "fixed-to-working",
      "file": "//.../theme/styles.css", "identifier": "c1d2…",
      "diff": "…", "blastRadius": "shared-rule", "review": "affects .btn site-wide" },
    { "ruleId": "link-name", "status": "skipped",
      "reason": "Text lives in a contentlet field; out of v1 scope" },
    { "ruleId": "heading-order", "status": "regressed",
      "file": "//.../travel/nav.vtl", "identifier": "f00d…",
      "reverted": true,
      "reason": "Re-scan showed +1 violation — reverted to prior version" },
    { "ruleId": "label", "status": "failed",
      "reason": "save returned 0 bytes; not applied" }
  ],
  "publishRequired": true   // human publishes from the Studio
}
```

`status` values: `fixed-to-working` | `reported` | `skipped` | `regressed` | `failed`.
A `regressed` edit (proven worse by re-scan) is **auto-reverted** to its prior version
(§5 step 6) — it never lingers on working. `skipped` is reserved for violations the loop
chooses not to act on (e.g. cap reached); the refuse-if-dirty use of it was removed (§5 step 5).

---

## 7. Accessibility Studio — v1 UX spec

### Entry: the page picker
- Studio opens with **no page selected** — it shows a list of the site's pages
  (`POST /api/content/_search`, `+basetype:5 OR urlmap:*`, `+working:true +deleted:false`,
  scoped to the current host), paginated (`limit`/`offset`, sort `modDate DESC`).
- **Search** filters by title/path/urlmap wildcard — instances can have thousands of pages.
- User selects **one page** (v1) → the run starts. No UVE dependency, no context handoff.
- A **"skip CSS"** toggle on the run (default off): when set, the agent fixes only VTL and
  reports CSS contrast instead of editing it — for users who don't want visual changes (§3).
- *(Future: a UVE "Fix accessibility" button becomes a deep-link that opens Studio with
  that page pre-selected — a convenience, not a separate handoff protocol.)*

### Layout (full-screen route)
```
┌─ Agent column ──────────┬─ Live preview ───────────────────────────┐
│  Accessibility Studio   │   ┌─ score ──────────────┐               │
│  page: /about-us        │   │  Issues  12 ▼ 5       │ ← animates    │
│                         │   └──────────────────────┘               │
│  ▸ Scanning…            │                                           │
│  ▸ Found 12 issues      │   [ rendered page in iframe ]             │
│  ▸ Locating sources…    │     ⚠──img (no alt)                       │
│  ▸ Reading header.vtl   │          ⚠──low contrast                  │
│  ▸ ✓ Added alt text     │                                           │
│  ▸ Re-scanning preview… │   overlays pin each violation             │
│  ▸ Done: 7 fixed, 5     │                                           │
│    reported             │   [ ● Working   ○ Live ]  ← diff toggle   │
│                         │                                           │
├─────────────────────────┴───────────────────────────────────────────┤
│   7 fixed to working · 5 need attention              [ Publish ]     │
└───────────────────────────────────────────────────────────────────────┘
```

### Three a11y-specific elements (the only non-generic parts)
1. **Score widget** — `before → after` issue count, animating down as fixes confirm.
2. **Violation overlays** — pins on the iframe at each violation's DOM location;
   toggleable. *Reported* (unfixable) issues stay visible in a distinct color.
3. **Step log** — the streaming loop, in human language.

### Behavior
- **Streaming:** steps stream into the column live (SSE through the dotCMS proxy).
- **Preview:** one render iframe — **Live** = published URL, **Working** =
  preview/working URL (§8.2). The toggle is the rendered before/after diff.
- **End state:** fixes sit on **working**; footer shows `N fixed · M reported`.
  **Publish** promotes working → live (the only publish, human-triggered). **No Discard
  in v1** — undo is via dotCMS's per-asset version history (§3); the agent never
  touched live.
- **Review artifact:** before publishing, the user inspects the per-file diff from the
  report (§6) — publishing isn't blind. Per-fix accept/reject and free chat are deferred
  (the transaction is the whole batch).

### Why the spine survives the deferrals
- **Multi-page / batch sweep** → the picker is already a page *list*; v1 selects one,
  multi-select + a per-page run queue is the natural next step (the true "compliance
  sweep" workflow).
- **UVE deep-link** → a future "Fix accessibility" button just opens Studio with a page
  pre-selected; additive, no context-serialization protocol.
- **Free-form chat** → the column is already shaped like a conversation; v1 fills it
  with status. The input box is additive, no relayout.
- **Per-fix accept/reject + batch undo** → working + whole-batch publish (with version-
  history undo) is the v1 safety model; granular control and a true batch-rollback are additive.
- **Other recipes (full Lovable)** → the shell is recipe-agnostic. Strip the three
  a11y-specific elements and it hosts any agent recipe.

---

## 8. Service contract & streaming protocol

The seam across all three layers. Three hops: **Studio → proxy → agent**, then the
response back.

> **Naming:** "**v1**" = the shipped product (it *includes* streaming). "**Phase**" =
> a build-order milestone (§10). Streaming arrives in **Phase 3**; **Phase 2** is an
> intermediate non-streaming build. So below, "Phase 2 shape" = plain JSON response,
> "Phase 3 shape" = SSE — both are steps toward the same v1 product.

The *request* shape is identical in both — only the response media type changes
(`application/json` → `text/event-stream`), so Phase 2 → 3 is a response-only change, no
contract churn.

### 8.1 Hop 1 — Studio → proxy

Same-origin; the dotCMS auth **cookie** rides along automatically. Browser holds no token.

```
POST /api/v1/a11y-agent/fix        (the new proxy resource)
Content-Type: application/json
{
  "page":    { "identifier": "a9f3…", "languageId": 1 },  // from the picker selection
  "options": { "skipCss": false }                          // §3 per-run opt-out
}
```

The proxy: authenticates the backend user from the cookie → resolves the page
(identifier → live URL, host, uri) → **mints the short-lived JWT** → generates a
`runId` → calls hop 2.

### 8.2 Hop 2 — proxy → agent (`POST /fix`)

The proxy is the trust boundary; it injects the token as a **header** (not body) and
hands the agent everything resolved, so the agent never re-resolves or holds a session token.

```
POST http://<agent>/fix
Authorization: Bearer <minted short-lived JWT>   // the agent's api.request credential
Content-Type: application/json
{
  "runId":   "r_01J…",                 // proxy-generated; for idempotency/reconnect (§8.7)
  "dotcmsBaseUrl": "https://host",      // the agent's api.request base
  "page": {
    "identifier": "a9f3…",
    "uri":        "/about-us",          // for /page/_render-sources
    "liveUrl":    "https://host/about-us",
    "host":       "demo.dotcms.com",
    "hostId":     "48190c8c-…",         // _render-sources host_id
    "languageId": 1
  },
  "options": { "skipCss": false }
}
```

Note: the proxy resolves `liveUrl`/`uri`/`hostId` from the identifier so the agent isn't
guessing URL construction. From these fields the agent builds the two scan URLs (string
assembly, no extra resolution):

- **initial (live):** `https://<instance>/<uri>?host_id=<hostId>`
- **re-scan (working):** `https://<instance>/<uri>?host_id=<hostId>&mode=EDIT_MODE` (+
  `language_id` if multilingual)

`host_id` disambiguates which site's copy of the path renders; `mode=EDIT_MODE` flips
dotCMS to the working version (`debug-auth.mjs` proves the scanner can auth+render it).

> **CONFIRMED in S0 spike (2026-06):** `mode=EDIT_MODE` does render the working edit
> — a save-working VTL change is reflected on the re-scan (verified by read-back of the
> working version *and* a non-error EDIT_MODE render). **But EDIT_MODE injects the editor
> chrome (drag handles, toolbars, add-content buttons), which axe flags as ~48 extra
> violations** — on the demo `/index`, live = 83 issues vs the *same page with no edits*
> in EDIT_MODE = 131 (16→25 errors, 67→106 warnings). **Consequence for §5 step 6: the
> before/after delta MUST be EDIT_MODE-before vs EDIT_MODE-after** (same chrome on both
> sides cancels out), never `live-before` vs `EDIT_MODE-after` — the latter makes every
> fix look like a regression and would mis-fire the auto-revert. So the loop scans
> EDIT_MODE **twice** (a pre-edit EDIT_MODE baseline + the post-edit re-scan) in addition
> to the initial live scan. `PREVIEW_MODE` renders chrome-free (83, matches live) and is a
> candidate cleaner basis, but whether it reflects working edits was not proven in S0 —
> EDIT_MODE-vs-EDIT_MODE is the locked v1 basis. (`mode=WORKING` is **not** a real mode —
> dotCMS ignores unknown values and renders a degenerate page; do not use it.)

### 8.3 Hop 3 — response (agent → proxy → Studio)

- **Phase 2 shape:** `200 application/json` = the §6 run report (with `runId`).
- **Phase 3 shape (the v1 product):** `200 text/event-stream` = the SSE events below,
  terminating in a `done` event carrying the same §6 report. The proxy relays frames as they
  arrive (§8.6 caveat).

Errors: the proxy maps agent/upstream failures to clean statuses (no stacktraces),
mirroring `PageScannerResource`'s error handling.

### 8.4 SSE event shape (Phase 3 / shipped v1)

The event shape (**agent → proxy → Studio**); finalize before emitting. Sketch:

```jsonc
// event: step
{ "type": "step", "phase": "scan|locate|read|fix|rescan",
  "message": "Reading header.vtl", "ts": "..." }

// event: score
{ "type": "score", "before": 12, "current": 7 }

// event: violation
{ "type": "violation", "ruleId": "image-alt", "status": "fixed-to-working",
  "node": { "selector": "...", "html": "..." } }
// NOTE: the scanner returns axe `selector` + `html`, NOT pixel bounds. The Studio
// computes overlay positions by resolving the selector against the live iframe.

// event: done
{ "type": "done", "report": { /* §6 run report */ } }

// event: error
{ "type": "error", "message": "..." }
```

The proxy must forward these as a stream, not buffer-then-return. Studio consumes
them to drive the step log, score widget, and overlays live.

### 8.5 Stack support — VERIFIED ✅ (SSE runs in production here)

- **Jersey 2.47** with `jersey-media-sse` already a dependency (`EventOutput`,
  `OutboundEvent`, `SseFeature.SERVER_SENT_EVENTS`) — `bom/application/pom.xml`, `dotCMS/pom.xml`.
- **Tomcat 9.0.118 / Servlet 3.1**; every `web.xml` filter + the Jersey servlet set
  `<async-supported>true</async-supported>` — the usual streaming blocker is cleared.
- **Precedents:** `ContentImportResource` (`/content/import/{jobId}/monitor`,
  `@Produces(SERVER_SENT_EVENTS)` → `EventOutput`) is the closest model;
  `WorkflowResource` bulk fire uses the same pattern; `CompletionsResource` /
  `BundleResource` / `IntegrityResource` use `StreamingOutput` if generic chunked is preferred.
- **No buffering risk:** the one buffering filter (`GZIPFilter`) exists but is **not
  registered** in `web.xml`. Gzip is handled at the container/LB (streaming-safe).

### 8.6 Proxy caveat — NOT a literal clone for SSE

`PageScannerResource.forwardRequest` blocks on `HttpClient.send(..., ofString())`,
buffering the whole body — can't pass a stream through. The a11y proxy instead:
consume the agent stream with `BodyHandlers.ofInputStream()` / `ofLines()`, and return
`EventOutput` (`@Produces(SERVER_SENT_EVENTS)`), relaying frames as they arrive
(modeled on `ContentImportResource`). The auth + JWT-mint half of `PageScannerResource`
is reusable as-is; only `forwardRequest` becomes a streaming relay.

### 8.7 Active-run slot & re-entry (idempotency)

The agent service keeps a **per-user active-run slot** — `Map<userId, { runId, status,
reportSoFar }>`, `userId` from the minted JWT. One extra endpoint:

```
GET /active-run        → { runId, status, reportSoFar } | null   (for the calling user)
```

- **Studio load** calls it: if a run exists, the Studio **re-attaches** (resumes the live
  SSE view, or shows the finished report); else it shows the picker. One mechanism covers
  double-click, SSE reconnect, and leave-and-return.
- **Re-trigger while active = replace** (cancel old, start new). Because there's no rollback
  (§3), cancellation happens only at a **loop-step boundary** (after a re-scan, never
  mid-`save`) so a replaced run never abandons a half-written working file.
- Shares its run-registry machinery with the deferred concurrency work (§12).

---

## 9. Residual risks / open unknowns

Unknowns that could still break — not restatements of the §3 decisions.

- **DOM→source attribution — VALIDATED in experiments (was the #1 worry).** Mapping an
  axe-flagged DOM node to the source VTL/CSS file was the feared central risk, but early
  experiments show the model discovers the right files reliably, and `/_render-sources`
  hands over candidate source refs — a force-multiplier, not a guess. **But "candidate ref" ≠
  "proven origin":** `/_render-sources` doesn't pin the exact include / macro / conditional
  branch / CSS cascade winner. So gate the edit on **evidence** — the agent already READs the
  file (§5 step 4), so require it to confirm the file actually contains the offending
  markup/selector before writing; if it can't, → report, don't guess-edit. *Caveat:*
  validated on the pages/violation types tried so far, not proven universal (deep nesting,
  JS-injected DOM are the long tail).
- **VTL edit quality + semantic over-reach (now the top remaining risk).** Beyond
  correct/minimal diffs (no template breakage): generic alt/ARIA/heading fixes can *pass axe
  yet be bad accessibility*. Needs the "don't fabricate semantic content" policy (§5) —
  unproven until run on real pages (Phase 1).
- **Axe-pass ≠ correct.** Clearing a violation doesn't prove the VTL still renders or page
  behavior survived. v1 leans on human review before publish + a VTL parse/render check (§5).
- **Shared-template / CSS blast radius — accepted, not a risk to gate.** A fix to
  `header.vtl` (or a shared CSS rule/`--token`) changes every page using it — and that's
  *correct*: a theme-level a11y issue *should* be fixed everywhere at once. v1 doesn't gate
  it; it just **flags fan-out in the report** (`blastRadius`) so the human knows the scope
  before publishing. Optional dependent-page re-scan is a later nicety, not a blocker (§3).
- **Minted-JWT across all loop endpoints — RESOLVED ✅ (S0 spike, 2026-06).** The minted
  JWT was proven for the scanner callback; S0 confirmed it is **also accepted by
  `/page/_render-sources` and `/api/v2/assets`** (GET read + PUT `/save`), driven through
  the real `@dotcms/agentic-tools` sandbox (not curl) — the loop's #1 mechanical worry is
  cleared. (The auth *flow* was already settled — the sandbox JS never sees a token; the
  agent-service runtime injects it — §2/§3.)
- **EDIT_MODE re-scan inflates counts by editor chrome — RESOLVED ✅ (S0 spike).** See the
  §8.2 callout: EDIT_MODE renders editor UI that axe flags (~+48 on demo `/index`), so the
  §5 step-6 delta must be **EDIT_MODE-before vs EDIT_MODE-after**, not live-vs-EDIT_MODE.
  Locked as the v1 confirmation basis. New surprise from S0, not a pre-existing risk.
- **Scan-via-proxy is a hard invariant.** The token model holds only if scan/rescan go
  through `PageScannerResource` (which adds both tokens server-side). A direct call to
  `microservice-accessability` would need both tokens in the sandbox. The §3 safety boundary
  enforces this: the **MVP path allowlist** (and later **typed tools**) make the microservice
  unreachable. Only the solo-spike window relies on prompt + origin guard alone.
- **`maxSteps` / cost vs. "triage everything".** High-violation pages can hit the cap
  mid-run, conflicting with the §3 "triage everything" decision — reconciliation in §11 Q1.
- **Anthropic key/cost ownership.** v1 owns an `ANTHROPIC_API_KEY`; dotCMS owns or
  passes the billing. (Long-term mitigated by the provider-portable SDK, §3.)

---

## 10. Suggested build order

Ordered by **risk, not layer**: de-risk the unknowns first, get an end-to-end thread
demoable early over plain request/response, add streaming + polish last onto a working
system. Three phases: **prove → thread → polish.** All endpoints are merged — nothing blocks.

### Phase 0 — Prove the loop (no app, no UI; ~hours) — DONE ✅ (2026-06)

0. **Sandbox spike** (real runtime path, not curl): with a hand-minted token, drive
   the full loop through the `agentic-tools` executor —
   `scan(live)` → `render-sources` → `read` → `save-working` → `scan(preview)`. Goals:
   (a) endpoints compose end-to-end; (b) the minted JWT is accepted by
   `/page/_render-sources` and `/api/v2/assets`, not just the scanner (§9); (c) confirm the
   `?mode=EDIT_MODE` re-scan (§8.2) actually reflects the working edit (and whether
   `language_id` is needed for multilingual pages).

   **Outcome:** all five calls ran green through the sandbox on demo `/index`.
   (a) ✅ JWT accepted on scan/locate/read/save. (b) ✅ EDIT_MODE reflects the working
   edit — **with the chrome-inflation caveat** now locked into §8.2/§5 (re-scan delta is
   EDIT_MODE-before vs EDIT_MODE-after). (c) page is `languageId 1`; single-language pages
   need no `language_id`, multilingual still do. Throwaway script lived in
   `core-web/scratch/` (gitignored). Pre-req discovered: the Page Health app must point at a
   **locally-run** `microservice-accessability` (`ALLOW_PRIVATE_HOSTS=true`,
   `apiUrl=http://host.docker.internal:3000`) so it can render the dockerized dotCMS.

### Phase 1 — Headless agent service (no streaming/UI)

1. Scaffold `apps/a11y-agent` (Nx); wire Vercel AI SDK + `@ai-sdk/anthropic`; import the
   `agentic-tools` executor + `createApiAdapter` — raw `execute` is the only tool for now
   (cheap while discovering the loop's real endpoint set, §3).
2. Implement the loop as a **single-shot run** → final JSON report (§6): SCAN + triage →
   READ → FIX-TO-WORKING → working re-scan. Add the **path allowlist** in the adapter
   interceptor as soon as it's more than solo (§3 stage B — the MVP safety boundary): permit
   only the loop's endpoints, hard-reject the rest. Exercises the edit-quality risk (§9). Note
   which endpoints the loop touches — that's the allowlist *and* the later typed-tool set.
3. **Lock the report schema + tests (§6)** — proxy and Studio both depend on it; freezing
   it unblocks parallel work.
4. Expose as a plain HTTP endpoint (`POST /fix` → JSON). No SSE yet.

### Phase 2 — Thread through dotCMS (still no streaming)

5. **Tool surface (§3):** the path allowlist (step 2) is the MVP boundary that gates user
   exposure here. Optionally graduate to ~4 typed tools (`scanA11y` / `locateSources` /
   `readAsset` / `saveWorking`) — the pre-GA hardening (§3 stage C); the allowlist is its spec.
6. dotCMS proxy resource: reuse `PageScannerResource`'s auth + JWT-mint half; forward as
   a single request/response. Add the **App config** for the agent URL/token (model on
   `dotPageScanner-config.yml`).
   - **Dev shortcuts** (skip the Java rebuild cycle while iterating; the full Java SSE relay
     lands in Phase 3 with streaming, since its hard part *is* the relay):
     - *Fastest:* the **Studio UI calls the agent at `localhost:3000` directly**, bypassing
       the proxy entirely. The agent service holds a hand-minted short-lived token via env
       (the Phase 0 token) — the browser sends only `{ pageId }`, no token client-side.
       Guardrails: (a) the URL **must be env-switched** (`environment.ts` dev vs prod), never
       a literal in committed code — prod Studio always calls the same-origin proxy; (b) the
       agent service needs permissive **CORS** in dev; (c) **the path allowlist (§3 stage B)
       must be active** here — this shortcut is write-capable with a real token, so it must
       not run on prompt-guard alone, or "only us" becomes a destructive bypass.
     - *Closer to prod:* keep the proxy in the path but point it at `localhost:3000` —
       exercises the proxy's auth/JWT-mint, only the relay is local.
7. Thinnest **Studio route**: page picker (list + search via `_search`, §7) → select one
   page → trigger via the proxy → render the final JSON report. Ugly but **end-to-end
   demoable** — first clickable version, no UVE dependency.

### Phase 3 — Stream + polish

8. **Freeze the streaming protocol (§8)** before emitting events.
9. Agent service: single-shot → **SSE step emission** (`step`/`score`/`violation`/`done`).
10. Proxy: rewrite `forwardRequest` as a streaming relay (§8 caveat); confirm `GZIPFilter`
    stays unregistered for this path.
11. Studio UX, in order: stream consumer → score widget → iframe overlays → working/live
    toggle → publish footer.

---

## 11. Open design questions (decide before / during build)

Undecided contracts and behaviors the build hits immediately. Each names where to resolve it.

> **Already resolved or deferred (no longer open):** proxy→agent contract → §8.1–8.3;
> working/preview URL recipe → §8.2; rollback/undo → §3 + §6 `failed`/`regressed`;
> idempotency / re-runs → §8.7; safety boundary → §3 Tool surface (3-stage); DOM→source
> attribution → §9 (validated in experiments; `/_render-sources` helps); shared-template /
> CSS blast radius → §3 + §9 (accepted by design — fix everywhere, flag fan-out);
> **concurrency on the shared working version → §12 (deferred, must fix).**

- **Q1 — Cap behavior on high-violation pages.** The approach is set (both `maxSteps` and
  per-run caps — max files/bytes/calls — §3 Tool surface); the open part is *behavior when a
  cap is hit mid-page*: prioritize violations by impact (critical/serious first)? emit a
  partial report flagging what wasn't reached? Plus the actual numbers. Decide/tune in Phase 1.

---

## 12. Out of scope for v1 (deferred — must fix later)

Understood, deliberately not built in v1. **Not** open questions (we know the problem) and
**not** permanent decisions — they are known debt to address before wider rollout.

- **Full concurrency safety — MUST FIX before GA.** dotCMS keeps one *working* version per
  asset per language — a **shared slot**, not a per-run draft. The agent's read→fix→save can
  **silently clobber** an in-progress human edit or another run (last-write-wins), and the
  report would still say "fixed." Invisible in a demo; likely in production, since a11y fixes
  cluster on shared templates.
  - **In v1:** explicitly **accepted** — the agent does not guard against pre-existing
    unpublished edits. Working-save is non-destructive (per-asset version history, §3), so the
    worst case is recoverable. The earlier "refuse-if-dirty" guard was **removed** because it
    misfired on the agent's own in-run edits (the second violation in a file saw working≠live
    from the first fix and skipped), defeating multi-fix files; correctly distinguishing a
    *human's* edit from the *agent's own* prior edit needs version tracking we don't have in v1.
  - **Deferred (must fix before GA):** the *race* between the agent's own read and write
    (optimistic lock — record the read version, abort + report `failed` if working changed
    since), distinguishing a foreign working edit from the agent's own, and **two concurrent
    agent runs** on the same shared asset. Until built, **document "don't run concurrent runs"
    and "the agent may overwrite unpublished working edits."**

---

## 13. Reference files (existing code to model on)

- `dotCMS/src/main/java/com/dotcms/rest/api/v1/pagescanner/PageScannerResource.java`
  — proxy pattern (authn, short-lived JWT mint, upstream forward, error mapping).
- `dotCMS/src/main/java/com/dotcms/rest/api/v1/pagescanner/CheckType.java`
- `dotCMS/src/main/resources/apps/dotPageScanner-config.yml` — App config pattern
  (holds `apiUrl` + `apiAuthToken` — the microservice **service secret**, NOT a user token).
- `microservice-accessability/` — the external scanner (Hono + Puppeteer + axe-core).
  - `src/middleware/auth.ts` — `requireAuthToken`: timing-safe check of the static
    `auth-token` **service secret**.
  - `src/routes/accessibility.ts` — `/a11y/check`: takes `shortLivedToken` +
    `authHeaderMode` in the body; injects the JWT as `Authorization: Bearer` on
    *same-origin* Chrome requests (`attachAuthInterceptor`) to render authenticated pages.
  - `debug-auth.mjs` — proves the `mode=EDIT_MODE` + `shortLivedToken` working-page
    render (the preview re-scan mechanism).
- `dotCMS/src/main/java/com/dotcms/rest/api/v1/content/dotimport/ContentImportResource.java`
  — **SSE relay pattern** (`@Produces(SseFeature.SERVER_SENT_EVENTS)` + `EventOutput`);
  the model for the proxy's rewritten forward half (§8).
- `core-web/libs/agentic-tools/` — executor, http-client, sandbox primitives (the
  agent's runtime; exports `createExecutor` / `createApiAdapter`).
- `core-web/apps/mcp-server/src/tools/execute.ts` — example of `agentic-tools`
  adapter wiring to model on (a sibling consumer, not a dependency).
- PR #36102 — `GET /api/v1/page/_render-sources/{uri}` (LOCATE).
- PR #36112 — `GET/PUT /api/v2/assets` (READ/WRITE).
