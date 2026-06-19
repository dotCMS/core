# S1 — Headless agent loop (`POST /fix` → JSON report)

> **Goal:** the agent service exists and runs the full loop as a single-shot run, returning
> the §6 JSON report. No streaming, no UI. This is the spine — its report schema is the
> contract S2 and S3 build against, so **lock the schema in this session**.
> **Plan:** [§5 loop](../a11y-agent-plan.md), [§6 report](../a11y-agent-plan.md), [§3 Tool surface](../a11y-agent-plan.md), [§8.2 contract](../a11y-agent-plan.md).

## Entry state
- S0 done: endpoints + minted JWT + the EDIT_MODE re-scan recipe are confirmed.
- An `ANTHROPIC_API_KEY` available to the dev environment.

## Tasks (in order)
1. **Scaffold** `core-web/apps/a11y-agent` (Nx app, Node/TS, sibling to `mcp-server`). Wire
   the Vercel AI SDK (`ai`) + `@ai-sdk/anthropic`. Import the `@dotcms/agentic-tools`
   executor + `createApiAdapter`.
2. **The loop** as a single-shot function `runFix({ runId, dotcmsBaseUrl, page, options }, token)`:
   SCAN → LOCATE → TRIAGE+ATTRIBUTE → READ → FIX-TO-WORKING → RE-SCAN(working) → REPORT.
   Implement the loop guards from §5:
   - **refuse-if-dirty** before any write (skip + report if working ≠ live).
   - **attribution evidence gate**: confirm the read file contains the offending markup
     before editing; else report, don't guess-edit.
   - **don't fabricate semantic content**; contrast = nudge existing color to WCAG AA, else report.
   - **auto-revert on regression**: if the re-scan proves a fix made things worse, revert
     that asset to its prior version → `regressed (reverted)`.
3. **Path allowlist** in the adapter interceptor (§3 stage B): permit only
   `page-scanner/a11y/check`, `_render-sources` GET, `/api/v2/assets` GET + `/save`;
   hard-reject everything else (publish/delete/workflow/config + non-dotCMS hosts).
4. **`maxSteps` + per-run caps** (max files/bytes/calls) — pick starting numbers; this is
   also where the open Q1 cap-behavior gets its first answer.
5. **Lock the §6 report schema + write tests for it.** This is the deliverable that unblocks
   S2/S3 — freeze it. Statuses: `fixed-to-working | reported | skipped | regressed | failed`.
6. Expose `POST /fix` (plain JSON response, no SSE). Token read from the
   `Authorization: Bearer` header (§8.2).
7. Add the **per-user active-run slot** + `GET /active-run` (§8.7) — `Map<userId, run>`,
   replace-at-loop-step-boundary on re-trigger.

## Contract to honor
- Request shape = §8.2 exactly (`runId`, `dotcmsBaseUrl`, `page{...}`, `options`).
- Response = §6 report exactly. **The request shape must not change when S4 adds streaming.**

## Scope fences (do NOT)
- No SSE yet — single JSON response only.
- No dotCMS Java changes (that's S2). The agent is reached directly for now (dev/test).
- No Angular.
- Don't widen the allowlist beyond the four loop operations.

## Definition of done
- `POST /fix` against a real instance returns a valid §6 report for a real page, with at
  least one `fixed-to-working` and the triage statuses exercised.
- refuse-if-dirty, auto-revert, and the publish-path rejection each have a test.
- Report schema is committed with tests; S2/S3 can code against it.
