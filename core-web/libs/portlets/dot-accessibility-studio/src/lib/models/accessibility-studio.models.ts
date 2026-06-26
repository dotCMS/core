// ── Agent wire contract (plan §5/§6) ────────────────────────────────────────

export type FixStatus = 'fixed-to-working' | 'reported' | 'skipped' | 'regressed' | 'failed';

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
    changedFiles: string[];
    publishRequired: true;
}

export type ActiveRunStatus = 'running' | 'done' | 'error';

/**
 * The Studio → proxy request body (POST /api/v1/a11y-agent/fix[/stream], plan §8.1).
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
 * The Studio state machine. Drives which screen + action block renders.
 * picker → ready → scanning → scanned → fixing → done → published
 */
export type StudioPhase =
    | 'picker'
    | 'ready'
    | 'scanning'
    | 'scanned'
    | 'fixing'
    | 'done'
    | 'published';

/** A page row in the picker, projected from a DotCMSContentlet. */
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
 * Mirrors the agent's onStep phases (run-fix/tools): the Studio maps these to an
 * icon + tone for the live activity log.
 */
export type StudioStepPhase = 'scan' | 'locate' | 'read' | 'fix' | 'rescan';

/** One live activity-log entry, built from an SSE `step` event. */
export interface StudioStep {
    /** Monotonic id for @for tracking + entry animation. */
    id: number;
    phase: StudioStepPhase;
    message: string;
}

/** Discriminated union of the parsed SSE events the agent emits. */
export type AgentStreamEvent =
    | { type: 'step'; phase: StudioStepPhase; message: string }
    | { type: 'done'; report: FixReport }
    // Terminal event when the user stopped the run — carries the PARTIAL report
    // (fixes already applied are kept). Same payload as `done`.
    | { type: 'aborted'; report: FixReport }
    | { type: 'error'; message: string };
