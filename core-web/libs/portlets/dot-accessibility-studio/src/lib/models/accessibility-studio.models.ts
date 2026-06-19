/**
 * Studio-side types for the Accessibility Studio portlet.
 *
 * The `Fix*` types mirror the agent's locked §6 output contract
 * (apps/dotcms-agents/src/a11y/contract.ts). That file lives in a Node app and
 * is not importable from Angular, so the shapes are duplicated here and MUST be
 * kept in sync with the agent contract. See docs/plans/a11y-agent-plan.md §6/§8.
 */

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
    hostName: string;
    /** Human-formatted last-edited date. */
    modDate: string;
    modUserName: string;
    live: boolean;
}

// ── Agent §6 report mirror ──────────────────────────────────────────────────

/**
 * Per-violation status — the locked vocabulary the Studio renders:
 *   fixed-to-working — minimal diff saved to the working version (never published)
 *   reported         — not auto-fixable in v1; carries guidance
 *   skipped          — the loop chose not to act (e.g. cap reached)
 *   regressed        — re-scan proved the edit made things worse; auto-reverted
 *   failed           — the save/operation did not apply
 */
export type FixStatus = 'fixed-to-working' | 'reported' | 'skipped' | 'regressed' | 'failed';

/** Blast radius of a CSS edit — surfaced so the human sees scope before publish. */
export type BlastRadius = 'element-scoped' | 'shared-rule' | 'token';

export interface ScanCount {
    violations: number;
}

export interface FixResult {
    /** axe rule, e.g. "image-alt", "color-contrast". */
    ruleId: string;
    status: FixStatus;
    /** host-qualified asset path, when a file was touched. */
    file?: string;
    /** dotCMS asset identifier. */
    identifier?: string;
    /** the applied minimal diff (fixed-to-working). */
    diff?: string;
    /** CSS edits only. */
    blastRadius?: BlastRadius;
    /** human-facing scope note, e.g. "affects .btn site-wide". */
    review?: string;
    /** true on regressed (auto-revert). */
    reverted?: boolean;
    /** why skipped/reported/regressed/failed. */
    reason?: string;
}

export interface FixReport {
    /** echoes the request runId. */
    runId: string;
    page: {
        uri: string;
        host: string;
        languageId: number;
    };
    /**
     * before/after are the EDIT_MODE-vs-EDIT_MODE deltas — same editor chrome on
     * both sides cancels out the phantom violations.
     */
    scan: {
        before: ScanCount;
        after: ScanCount;
    };
    results: FixResult[];
    /** human publishes from the Studio. */
    publishRequired: true;
}

/** The active-run slot status (GET /active-run). */
export type ActiveRunStatus = 'running' | 'done' | 'error';
