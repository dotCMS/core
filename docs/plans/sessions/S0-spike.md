# S0 — Spike: prove the loop composes

> **Goal:** with a hand-minted token, drive the full loop through `@dotcms/agentic-tools`
> once, by script, and confirm the endpoints compose. Throwaway code — no app, no UI.
> **Plan:** [master §10 Phase 0](../a11y-agent-plan.md), [§8.2 URL recipe](../a11y-agent-plan.md), [§9 risks](../a11y-agent-plan.md).

## Why this session exists
De-risk before building anything. Three things are unverified and cheap to check now; if
any fails, the plan adjusts before real code is written.

## Entry state
- A running dotCMS instance you can reach (local or demo) with the **Page Health app**
  configured (scanner `apiUrl` + `apiAuthToken`).
- A backend-user **API token** you can mint by hand (User Tools → API Tokens).
- `core-web` deps installed (`@dotcms/agentic-tools` builds).

## Tasks (in order)
1. Write a throwaway script (`scratch/` or a one-off test) that imports `createExecutor` +
   `createApiAdapter` from `@dotcms/agentic-tools` and runs JS through the sandbox with the
   hand-minted token as `authToken`.
2. **Compose the loop** via `api.request(...)`, on one real page:
   - `POST /api/v1/page-scanner/a11y/check { url: liveUrl }` → violations.
   - `GET /api/v1/page/_render-sources/{uri}?host_id=<id>` → source refs.
   - `GET /api/v2/assets?path=<a vtl from the refs>` → raw VTL.
   - `PUT /api/v2/assets/save` → write a trivial harmless edit to **working**.
   - `POST /api/v1/page-scanner/a11y/check { url: <working URL> }` → re-scan.
3. **Verify the three unknowns** and write down the answers:
   - (a) all four endpoints accept the **minted JWT** — especially `_render-sources` and
     `/api/v2/assets`, not just the scanner.
   - (b) the **working/EDIT_MODE re-scan** recipe — `https://<base>/<uri>?host_id=<id>&mode=EDIT_MODE`
     — actually renders the working edit (does the re-scan reflect the change?).
   - (c) whether **`language_id`** is needed on the URL for a multilingual page.

## Contract to honor
- Scan **through the proxy** (`/api/v1/page-scanner/a11y/check`), never the microservice directly.
- Write **working only** (`/save`), never `/publish`.

## Scope fences (do NOT)
- No Nx app, no agent loop, no LLM, no UI. This is a script that proves plumbing.
- Don't keep the code — its only output is **confirmed answers** to (a)/(b)/(c).

## Definition of done
- The five calls run green against a real instance with the minted token.
- (a), (b), (c) answered in writing (drop findings into master plan §9 / §8.2, replacing
  the "verify in spike" notes with confirmed facts).
- If (b) fails (working re-scan doesn't reflect the edit), **stop and flag** — the
  confirmation model needs rethinking before S1.
