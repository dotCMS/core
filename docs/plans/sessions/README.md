# Accessibility-Fix Agent — Coding Sessions

Each session below is a self-contained brief for one coding session. Open **only** that
session's file plus the master plan section it links to — you don't need to re-read the
whole plan. Run them in order; each lists the entry state it assumes from the previous one.

**Master plan:** [`../a11y-agent-plan.md`](../a11y-agent-plan.md) — the single source of
truth for *why*. Session briefs are the *how* and *what*; when they conflict, the master
plan's decisions (§3) win. If a session changes a decision, update the master plan too.

| # | Session | Deliverable | Plan phase |
|---|---------|-------------|------------|
| **S0** | [Spike](S0-spike.md) | Prove the loop composes end-to-end (throwaway script) | Phase 0 |
| **S1** | [Agent loop](S1-agent-loop.md) | Headless service: `POST /fix` → JSON report | Phase 1 |
| **S1.5** | [CSS attribution](S1.5-css-attribution.md) | Deterministic contrast attribution (parse CSS in code, not LLM) | Phase 1 |
| **S2** | [dotCMS proxy](S2-proxy.md) | Java SSE-capable proxy + App config (non-streaming first) | Phase 2 |
| **S3** | [Studio MVP](S3-studio-mvp.md) | Angular route: picker → run → render report | Phase 2 |
| **S4** | [Streaming](S4-streaming.md) | Agent SSE emission + proxy streaming relay | Phase 3 |
| **S5** | [Studio polish](S5-studio-polish.md) | Score widget, overlays, working/live toggle, publish | Phase 3 |

## Dependency graph

```
S0 ──▶ S1 ──▶ S1.5 ──▶ S2 ──▶ S3 ──▶ S4 ──▶ S5
        │                     │
        └─ report ────────────┘   (S3 consumes the §6 report schema S1 locks)
```

- **S0** has no dependencies (endpoints are merged). Pure de-risk; throwaway code.
- **S1** is the spine — everything downstream depends on its **report schema** (master plan §6).
  Lock that schema early; S2 and S3 both code against it.
- **S1.5** makes CSS contrast affordable & correct (attribute in code, edit SCSS not the
  compiled artifact). It only touches the agent loop, not the §6 report, so S2/S3 are
  unaffected — but contrast fixes aren't real until it lands.
- **S2** and **S3** can overlap once S1's schema is frozen (one person each), but S3 needs
  S2's proxy to demo end-to-end — or use the **dev shortcut** (Studio → `localhost:3000`).
- **S4** turns the single-shot response into a stream; **S5** is pure frontend on top of S4.

## Conventions every session honors

- **Never publish.** The agent writes the *working* version only; humans publish. Any
  `api.request` to a publish path must be rejected (the allowlist — master plan §3 Tool surface).
- **Report honestly.** `fixed-to-working | reported | skipped | regressed | failed` — see §6.
- **Don't send whole stylesheets to the LLM.** CSS is attributed in code (S1.5); the model
  sees only the few matched rules. (No refuse-if-dirty guard — it was removed in S1; §5 step 5.)
- **Edit source, not artifacts.** SCSS source, never compiled/precompiled CSS or minified JS.
- **The contract is §8.** The `/fix` request shape and SSE events are frozen there; don't invent new fields without updating §8.
