# S4 — Streaming (agent SSE emission + proxy relay)

> **Goal:** turn the single-shot run into a live stream. The agent emits SSE step events;
> the proxy relays them frame-by-frame; the Studio consumes them. Same request shape as
> before — only the response media type changes, so this is additive, not a rewrite.
> **Plan:** [§8.3–8.6 streaming + stack + proxy caveat](../a11y-agent-plan.md), [§8.4 event shape](../a11y-agent-plan.md), [§13 ContentImportResource](../a11y-agent-plan.md).

## Entry state
- S1/S2/S3 done: the loop, proxy, and Studio all work over plain request/response.

## Tasks (in order)
1. **Freeze the streaming protocol** (§8.4) before emitting: lock the event shapes —
   `step` / `score` / `violation` / `done` (carries the §6 report) / `error`. The agent,
   proxy, and Studio all code against these.
2. **Agent service:** switch the loop from "compute then return JSON" to **emitting events
   as it progresses** (`step` per phase, `score` as the count drops, `violation` per result,
   `done` with the full §6 report at the end). Keep a non-stream JSON fallback if cheap.
3. **Proxy: rewrite `forwardRequest` as a streaming relay** (§8.6 — NOT a clone of
   `PageScannerResource`): consume the agent stream with `BodyHandlers.ofInputStream()` /
   `ofLines()`, return `EventOutput` with `@Produces(SseFeature.SERVER_SENT_EVENTS)`,
   relay frames as they arrive. **Model on `ContentImportResource`** (§13), which already
   does exactly this in production. Confirm `GZIPFilter` stays unregistered for this path (§8.5).
4. **Studio: SSE consumer** — replace the S3 spinner with an `EventSource` that drives the
   step log live. On reconnect, re-attach via `GET /active-run` (§8.7) rather than starting fresh.

## Contract to honor
- Request shape unchanged from §8.2 — this session only changes the **response** to
  `text/event-stream`. No new request fields.
- `done` event carries the **same §6 report** S3 already renders — so S3's report view keeps
  working unchanged.
- SSE must **stream, not buffer** (the proxy relays frames; don't collect-then-send).

## Scope fences (do NOT)
- No new report fields, no loop logic changes (that was S1).
- No score widget / overlays / toggle yet — that's S5. This session just gets the *events
  flowing* and the step log live; the fancy consumers come next.

## Definition of done
- Running a page in the Studio shows steps appearing **live** as the agent works, ending in
  the same rendered report.
- Killing the SSE connection mid-run and reloading re-attaches to the in-flight run.
- Verified the proxy relays incrementally (frames arrive during the run, not all at the end).
