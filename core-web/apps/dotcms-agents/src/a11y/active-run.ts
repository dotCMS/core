import type { ActiveRun, FixReport } from './contract';

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
}

export class ActiveRunRegistry {
    private readonly slots = new Map<string, Slot>();

    /** Mark a run as started for this user (replaces any prior slot). */
    start(userId: string, runId: string): void {
        this.slots.set(userId, { runId, status: 'running' });
    }

    /** Record the finished report (only if this run still owns the slot). */
    finish(userId: string, runId: string, report: FixReport): void {
        const current = this.slots.get(userId);
        if (current?.runId === runId) {
            this.slots.set(userId, { runId, status: 'done', report });
        }
    }

    /** Record a failure (only if this run still owns the slot). */
    fail(userId: string, runId: string): void {
        const current = this.slots.get(userId);
        if (current?.runId === runId) {
            this.slots.set(userId, { runId, status: 'error' });
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
