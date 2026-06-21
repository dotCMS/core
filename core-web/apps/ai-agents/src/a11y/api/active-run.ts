import type { ActiveRun, FixReport } from '../domain/contract';

/**
 * Per-user active-run slot (plan §8.7). One in-flight or finished run per user,
 * keyed by the JWT subject. Backs GET /active-run so the Studio can re-attach
 * after a reload/reconnect, and enforces replace-on-re-trigger.
 *
 * S1 is single-shot (no streaming), so "re-attach" returns the finished report
 * or the running marker; the SSE resume comes in S4. Replace-at-loop-step-
 * boundary (cancel mid-run) is also deferred — S1 simply records the latest run
 * per user.
 */

interface Slot {
    runId: string;
    status: ActiveRun['status'];
    report?: FixReport;
    /** Aborts the in-flight runFix when the user calls Stop. */
    controller: AbortController;
}

export class ActiveRunRegistry {
    private readonly slots = new Map<string, Slot>();

    /**
     * Mark a run as started for this user (replaces any prior slot). Any prior
     * in-flight run for the same user is aborted first, so a re-trigger doesn't
     * leave two runs editing the same working files (Codex review §5).
     */
    start(userId: string, runId: string): AbortSignal {
        this.slots.get(userId)?.controller.abort();
        const controller = new AbortController();
        this.slots.set(userId, { runId, status: 'running', controller });
        return controller.signal;
    }

    /** The abort signal for a user's current run, or undefined if it's not this run. */
    signalFor(userId: string, runId: string): AbortSignal | undefined {
        const slot = this.slots.get(userId);
        return slot?.runId === runId ? slot.controller.signal : undefined;
    }

    /**
     * Request the user's in-flight run to stop (cooperative — runFix checks the
     * signal at safe checkpoints and returns a partial report). Returns the runId
     * that was signalled, or null if the user has no running run.
     */
    requestStop(userId: string): string | null {
        const slot = this.slots.get(userId);
        if (!slot || slot.status !== 'running') {
            return null;
        }
        slot.controller.abort();
        return slot.runId;
    }

    /** Record the finished report (only if this run still owns the slot). */
    finish(userId: string, runId: string, report: FixReport): void {
        const current = this.slots.get(userId);
        if (current?.runId === runId) {
            current.status = 'done';
            current.report = report;
        }
    }

    /** Record a failure (only if this run still owns the slot). */
    fail(userId: string, runId: string): void {
        const current = this.slots.get(userId);
        if (current?.runId === runId) {
            current.status = 'error';
        }
    }

    /** The §8.7 GET /active-run payload for a user, or null if none. */
    get(userId: string): ActiveRun | null {
        const slot = this.slots.get(userId);
        if (!slot) {
            return null;
        }
        return {
            runId: slot.runId,
            status: slot.status,
            reportSoFar: slot.report
        };
    }
}
