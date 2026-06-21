import { z } from 'zod';

/**
 * The a11y-fix agent contract — the seam the proxy (S2) and Studio (S3) build
 * against, and the SINGLE SOURCE OF TRUTH shared by the Node agent
 * (apps/ai-agents) and the Angular Studio portlet. Schemas are zod (the agent
 * validates requests against them); type-only consumers use the `z.infer` exports.
 *
 * LOCKED in S1. The request shape MUST NOT change when SSE streaming is added
 * (only the response media type flips json → text/event-stream). The report
 * shape is the §6 output contract; its `status` values are the vocabulary the
 * Studio renders. See docs/plans/a11y-agent-plan.md §5/§6/§8.2.
 */

// ── Request (proxy → agent, plan §8.2) ──────────────────────────────────────
// The proxy resolves identifier → liveUrl/uri/host/hostId so the agent never
// re-resolves URL construction. The agent builds scan URLs by string assembly:
//   live    = `${dotcmsBaseUrl}${uri}?host_id=${hostId}`
//   re-scan = `${live}&mode=EDIT_MODE` (+ `language_id` when multilingual)

export const PageRefSchema = z.object({
    identifier: z.string().min(1),
    uri: z.string().min(1),
    liveUrl: z.url(),
    host: z.string().min(1),
    hostId: z.string().min(1),
    languageId: z.number().int().positive()
});

export const FixOptionsSchema = z
    .object({
        // §3 per-run opt-out: when true, the agent fixes only VTL and reports
        // CSS contrast instead of editing it (no visual changes).
        skipCss: z.boolean().default(false)
    })
    .default({ skipCss: false });

export const FixRequestSchema = z.object({
    runId: z.string().min(1), // proxy-generated; idempotency/reconnect (§8.7)
    dotcmsBaseUrl: z.url(), // the agent's api.request base
    page: PageRefSchema,
    options: FixOptionsSchema
});

export type PageRef = z.infer<typeof PageRefSchema>;
export type FixOptions = z.infer<typeof FixOptionsSchema>;
export type FixRequest = z.infer<typeof FixRequestSchema>;

// ── Report (agent → proxy → Studio, plan §6) ────────────────────────────────
// One entry per triaged violation. `status` is the locked vocabulary:
//   fixed-to-working — minimal diff saved to the working version (never published)
//   reported         — not auto-fixable in v1; carries guidance (content field,
//                       third-party, JS-injected, ambiguous attribution, skipCss)
//   skipped          — refuse-if-dirty: working already differed from live (§5)
//   regressed        — re-scan proved the edit made things worse; auto-reverted
//   failed           — the save/operation did not apply (e.g. 0 bytes persisted)

export const FixStatusSchema = z.enum([
    'fixed-to-working',
    'reported',
    'skipped',
    'regressed',
    'failed'
]);

export type FixStatus = z.infer<typeof FixStatusSchema>;

// Blast radius of a CSS edit — surfaced so the human sees scope before publish (§9).
export const BlastRadiusSchema = z.enum(['element-scoped', 'shared-rule', 'token']);

export type BlastRadius = z.infer<typeof BlastRadiusSchema>;

export const ScanCountSchema = z.object({
    violations: z.number().int().nonnegative()
});

export type ScanCount = z.infer<typeof ScanCountSchema>;

export const FixResultSchema = z.object({
    ruleId: z.string().min(1), // axe rule, e.g. "image-alt", "color-contrast"
    status: FixStatusSchema,
    file: z.string().optional(), // host-qualified asset path, when a file was touched
    identifier: z.string().optional(), // dotCMS asset identifier
    diff: z.string().optional(), // the applied minimal diff (fixed-to-working)
    blastRadius: BlastRadiusSchema.optional(), // CSS edits only
    review: z.string().optional(), // human-facing scope note, e.g. "affects .btn site-wide"
    reverted: z.boolean().optional(), // true on regressed (auto-revert, §5 step 6)
    reason: z.string().optional() // why skipped/reported/regressed/failed
});

export type FixResult = z.infer<typeof FixResultSchema>;

export const FixReportSchema = z.object({
    runId: z.string().min(1), // echoes the request (§8.2)
    page: z.object({
        uri: z.string().min(1),
        host: z.string().min(1),
        languageId: z.number().int().positive()
    }),
    // before/after are the EDIT_MODE-vs-EDIT_MODE deltas (§8.2 S0 note) — same
    // editor chrome on both sides cancels out the ~48 phantom violations.
    scan: z.object({
        before: ScanCountSchema,
        after: ScanCountSchema
    }),
    results: z.array(FixResultSchema),
    publishRequired: z.literal(true) // human publishes from the Studio
});

export type FixReport = z.infer<typeof FixReportSchema>;

// ── Active-run slot (plan §8.7) ─────────────────────────────────────────────
// GET /active-run → the calling user's in-flight or finished run, or null.

export const ActiveRunStatusSchema = z.enum(['running', 'done', 'error']);

export const ActiveRunSchema = z.object({
    runId: z.string().min(1),
    status: ActiveRunStatusSchema,
    reportSoFar: FixReportSchema.partial().optional()
});

export type ActiveRunStatus = z.infer<typeof ActiveRunStatusSchema>;
export type ActiveRun = z.infer<typeof ActiveRunSchema>;
