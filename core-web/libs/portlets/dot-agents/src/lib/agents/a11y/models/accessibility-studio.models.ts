import { AgentChangedFile, AgentStreamEvent } from '@dotcms/dotcms-models';

// ── Agent wire contract (plan §5/§6) ────────────────────────────────────────

export type FixStatus = 'fixed-to-working' | 'reported' | 'skipped' | 'regressed' | 'failed';

/**
 * Statuses that mean a violation was genuinely left unresolved.
 *
 * `reported` is NOT one of them: a run makes two passes (deterministic, then agentic),
 * and `reported` marks a violation the first pass handed to the second — an
 * intermediate handoff, not an outcome. The agentic pass usually goes on to fix it.
 * Counting `reported` as unresolved reported work that was actually done.
 */
export const NEEDS_ATTENTION_STATUSES: ReadonlyArray<FixStatus> = [
    'skipped',
    'regressed',
    'failed'
];

/**
 * Rule id the agent uses for its research pass. Not a fix — it's the step where the
 * agent worked out how to fix something (or why it couldn't), so it carries a `reason`
 * narrative and no `diff`. Kept out of the fixed list; the files it touched appear in
 * the diff view via the report's `changedFiles`.
 */
export const RESEARCH_RULE_ID = 'agentic-research';

export type BlastRadius = 'element-scoped' | 'shared-rule' | 'token';

export interface ScanCount {
    violations: number;
}

export interface FixResult {
    ruleId: string;
    status: FixStatus;
    file?: string;
    identifier?: string;
    diff?: string;
    blastRadius?: BlastRadius;
    review?: string;
    reverted?: boolean;
    reason?: string;
}

export interface FixReport {
    runId: string;
    page: { uri: string; host: string; languageId: number };
    scan: { before: ScanCount; after: ScanCount };
    results: FixResult[];
    /** Files the agent changed in the working version (path + identifier). */
    changedFiles: AgentChangedFile[];
    /** True when the working fixes still need publishing to reach the live site. */
    publishRequired: boolean;
    /** Terminal status the agent reports on the `done` frame. */
    status?: string;
}

/**
 * The Studio → proxy request body (POST /api/v1/agents/a11y/fix[/stream], plan §8.1).
 * Simpler than the full FixRequest — the Java proxy resolves the page and builds
 * the complete agent payload (FixRequest) before forwarding.
 */
export interface AgentFixRequest {
    /** dotCMS content identifier of the page to fix. */
    identifier: string;
    /** Language id (default: 1). */
    languageId: number;
    /** When true the agent fixes only VTL and reports CSS contrast (plan §3). */
    skipCss: boolean;
}

/**
 * The run screen's state machine. Drives which action block renders.
 * ready → scanning → scanned → fixing → done → published
 *
 * (The page list is a separate route with its own store — see A11yPageListStore — so
 * there is no `page-list` phase here.)
 */
export type StudioPhase = 'ready' | 'scanning' | 'scanned' | 'fixing' | 'done' | 'published';

/** A page row in the page list, projected from a DotCMSContentlet. */
export interface StudioPageRow {
    identifier: string;
    title: string;
    /** Page URI / path, e.g. "/about-us". */
    path: string;
    /** Content type / base type label, e.g. "htmlpageasset", "Blog". */
    type: string;
    languageId: number;
    /** Host identifier — used as `host_id` to disambiguate the page render. */
    hostId: string;
    hostName: string;
    /** Human-formatted last-edited date. */
    modDate: string;
    modUserName: string;
    live: boolean;
}

// ── Agent streaming (SSE) ───────────────────────────────────────────────────

/**
 * Coarse phase tag on each streamed `step` event, emitted by the agent loop.
 * Mirrors the agent's onStep phases (run-fix/tools): carried in the generic
 * step's `meta.phase`, the presenter maps it to an icon for the activity log.
 */
export type StudioStepPhase = 'scan' | 'locate' | 'read' | 'fix' | 'rescan';

/**
 * The a11y agent's stream: the generic {@link AgentStreamEvent} specialized to
 * the a11y terminal payload. `done`/`aborted` carry a {@link FixReport} (the
 * `aborted` one is partial — fixes already applied are kept).
 */
export type A11yAgentStreamEvent = AgentStreamEvent<FixReport>;
