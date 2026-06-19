# S2 — dotCMS proxy + App config

> **Goal:** a dotCMS REST resource that authenticates the user, mints the short-lived JWT,
> resolves the page, and forwards to the agent service — **as a single request/response
> first** (streaming relay is S4). Plus the App config holding the agent URL/token.
> **Plan:** [§8.1–8.3 contract](../a11y-agent-plan.md), [§8.5–8.6 SSE stack + proxy caveat](../a11y-agent-plan.md), [§13 reference files](../a11y-agent-plan.md).

## Entry state
- S1 done: the agent service exposes `POST /fix` (JSON) and the §6 report schema is frozen.
- Agent service reachable at a known URL (local: `localhost:3000`).

## Tasks (in order)
1. **New REST resource** under `com.dotcms.rest.api.v1...` (e.g. `A11yAgentResource`,
   `POST /api/v1/a11y-agent/fix`). **Reuse `PageScannerResource`'s half** that:
   authenticates the backend user, and **mints the short-lived JWT** (`generateShortLivedToken`).
   Model the auth + error-mapping on `PageScannerResource.java` (§13).
2. **Resolve the page** from the picker's `{ page.identifier, languageId }` (hop 1, §8.1) →
   the resolved `{ uri, liveUrl, host, hostId, languageId }` the agent needs (hop 2, §8.2).
   Generate a `runId`.
3. **Forward to the agent** (`POST <agentUrl>/fix`) with the minted JWT as the
   `Authorization: Bearer` header and the §8.2 body. **Single request/response** — return the
   agent's §6 JSON report straight through. (The streaming relay is S4; structure
   `forwardRequest` so swapping to SSE later is contained — see §8.6.)
4. **App config** yml in `dotCMS/src/main/resources/apps/` for the agent service `url` +
   `authToken`, per-host — model on `dotPageScanner-config.yml` (§13).
5. **OpenAPI**: annotate the resource; regenerate `openapi.yaml`
   (`./mvnw compile -pl :dotcms-core -DskipTests`), commit alongside.

## Contract to honor
- Hop 1 / Hop 2 shapes = §8.1 / §8.2 exactly.
- The proxy holds the tokens; the **browser and agent never see a session token** — the
  agent gets only the minted short-lived JWT, in the header.
- Clean error statuses (no stacktraces), mirroring `PageScannerResource`.

## Scope fences (do NOT)
- **No SSE in this session** — single request/response. (S4 rewrites `forwardRequest` into
  the `EventOutput` streaming relay; just don't paint yourself into a corner — keep the
  forward step isolated.)
- No Angular. No agent-loop changes.
- Don't clone `PageScannerResource` wholesale — reuse the auth/mint half; the forward half is new.

## Definition of done
- `POST /api/v1/a11y-agent/fix { page, options }` (authenticated, same-origin) returns the
  §6 report for a real page, end-to-end through the proxy → agent → dotCMS.
- App config appears in the Apps portlet and supplies the agent URL/token.
- `openapi.yaml` regenerated and committed.
