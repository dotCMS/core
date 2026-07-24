# S3 — Accessibility Studio MVP (picker → run → report)

> **Goal:** the thinnest end-to-end Studio: a full-screen route that lists/searches pages,
> runs the agent on the selected one (via the proxy), and renders the final JSON report.
> Ugly is fine — this is the first clickable, demoable version. No streaming, no overlays.
> **Plan:** [§7 UX spec](../a11y-agent-plan.md), [§6 report](../a11y-agent-plan.md), [§8.7 active-run](../a11y-agent-plan.md), [core-web/CLAUDE.md](../../../core-web/CLAUDE.md).

## Entry state
- S1 done: §6 report schema frozen.
- S2 done (proxy reachable) **or** use the dev shortcut: Studio → `localhost:3000` directly
  (env-switched URL, allowlist on, CORS in dev — §10 Phase 2 dev shortcut).

## Tasks (in order)
1. **New full-screen route/portlet** `accessibility-studio` in `libs/portlets/`. Standalone
   Angular, `dot-` prefix, signals, Tailwind + PrimeNG — follow core-web conventions.
2. **Page picker** (§7): `POST /api/content/_search` with
   `+working:true +(urlmap:* OR basetype:5) +deleted:false +conhost:<currentHost>`,
   `sort: modDate DESC`, paginated (`limit`/`offset`). Search box → adds
   `+(title:<q>* OR path:*<q>* OR urlmap:*<q>*)`. Single-select.
3. **On select → run**: call the proxy `POST /api/v1/a11y-agent/fix { page:{identifier,languageId}, options:{skipCss} }`.
   Add the **"skip CSS" toggle** (default off).
4. **On load → check `GET /active-run`** (§8.7): if a run exists, re-attach (show its
   in-progress state or finished report) instead of the picker. Covers reload / leave-and-return.
5. **Render the §6 report**: per-violation rows with status (`fixed-to-working / reported /
   skipped / regressed / failed`), file, and the diff (expandable). Footer shows
   `N fixed · M reported` and a **Publish** button (whole-batch). **No Discard** (§3).
6. Loading/empty/error states: a run in progress shows a simple spinner+status (real
   streaming is S4); empty picker invites a search; a failed run shows the error plainly.

## Contract to honor
- Consume the §6 report exactly — don't reshape it client-side.
- Studio holds **no token**; the same-origin call carries the cookie, the proxy mints (§8.1).
- Page context comes from the **picker selection**, not a URL handoff (§7, no UVE dependency).

## Scope fences (do NOT)
- **No SSE consumer** — the run is request/response here; a spinner is fine. (S4+S5 add the
  live stream, score widget, overlays, working/live toggle.)
- No score animation, no iframe overlays, no preview toggle yet.
- No UVE button (future deep-link).

## Definition of done
- From the Studio: search → pick a page → run → see the rendered report → Publish promotes working.
- Reloading mid-run re-attaches via `GET /active-run` (no duplicate run).
- Tests: picker query/search, report rendering per status, the `GET /active-run` re-attach path
  (Spectator/Jest, `data-testid` selectors per core-web standards).
