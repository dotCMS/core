/**
 * Studio-side types for the Accessibility Studio portlet.
 *
 * The agent wire contract (FixRequest/FixReport/FixResult/FixStatus/BlastRadius/
 * ScanCount/ActiveRunStatus) is the SINGLE SOURCE OF TRUTH in
 * @dotcms/agent-contracts (shared with the Node agent). We re-export it here so
 * the portlet has one import point, and define the STUDIO-ONLY types (UI state
 * machine, picker row, live-step log, SSE event union) below.
 */
import type { FixReport, FixRequest } from '@dotcms/agent-contracts';

// Re-export the shared contract so existing portlet imports keep working.
export type {
    ActiveRunStatus,
    BlastRadius,
    FixReport,
    FixRequest,
    FixResult,
    FixStatus,
    ScanCount
} from '@dotcms/agent-contracts';

/** The agent fix request (POST /a11y/fix[/stream]) — the shared contract shape. */
export type AgentFixRequest = FixRequest;

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
    | { type: 'error'; message: string };
