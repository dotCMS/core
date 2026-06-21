import type { DotcmsGateway } from '../dotcms/dotcms-gateway';
import type { LanguageModel } from 'ai';

/**
 * Shared types for the fix loop, kept separate so both the orchestrator
 * (run-fix) and the deterministic CSS engine (css/css-fixer) depend on them
 * without importing each other (avoids a cycle).
 */

export interface RunFixCaps {
    /** Max source files the run will edit. */
    maxFiles: number;
    /** Max bytes of any single file edit. */
    maxFileBytes: number;
    /** Max violations triaged (protects against huge scans). */
    maxViolations: number;
}

export const DEFAULT_CAPS: RunFixCaps = {
    maxFiles: 20,
    maxFileBytes: 512 * 1024,
    maxViolations: 100
};

export interface RunFixDeps {
    client: DotcmsGateway;
    model?: LanguageModel; // injected for tests / provider swap
    caps?: RunFixCaps;
    /** Hook for the future SSE layer; no-op by default (plan §8.4). */
    onStep?: (phase: string, message: string) => void;
    /** Disable PASS 2 agentic research (default on). Set false to run deterministic-only. */
    research?: boolean;
    /** maxSteps for the PASS 2 research loop (default 40). */
    researchMaxSteps?: number;
    /**
     * Cooperative cancellation (Stop endpoint). When aborted, runFix stops at the
     * next SAFE checkpoint (between violations, before PASS 2) and returns a partial
     * report — fixes already saved to working stay applied. Never aborts mid-edit.
     */
    signal?: AbortSignal;
}
