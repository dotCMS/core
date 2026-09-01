/**
 * Contract spec for retry reporting (tasks T040/T042, dotCMS #37262, AC-009).
 *
 * THE BUG. `fetchWithRetry` calls `console.log` directly between attempts
 * (`src/utils/index.ts`, in the catch block) while an `ora` spinner is running. A spinner owns
 * the last terminal line and repaints it; anything else writing to stdout at the same time gets
 * shredded. That is the mangled block in the original report:
 *
 *     ✔ dotCMS containers started successfully.
 *     ⏳ dotCMS not ready (attempt 1/60) - ECONNRESET - Retrying in 5s...
 *
 * The fix is not to silence the retries — the user needs them, a ten-minute wait with no output
 * is the very thing being fixed — but to hand them to the caller, who owns the spinner and can
 * render them without fighting it.
 *
 * API PINNED
 *   export interface RetryReport {
 *       attempt: number;      // 1-based
 *       totalAttempts: number;
 *       reason: string;       // human-readable cause of THIS attempt's failure
 *       nextDelayMs: number;
 *   }
 *   export type RetryReporter = (report: RetryReport) => void;
 *   export function formatRetryReport(report: RetryReport): string;
 *   export function describeRequestFailure(error: unknown): string;
 *
 * `fetchWithRetry` takes an optional `onRetry: RetryReporter`. When omitted it stays silent —
 * silence is the correct default for a library function; the CLI supplies a reporter that routes
 * through its spinner.
 *
 * ALSO PINNED HERE — the status contract. `fetchWithRetry` accepts any 2xx
 * (`validateStatus: status >= 200 && status < 300`) but `isDotcmsRunning` then demands exactly
 * `res.status === 200`. A 204 is therefore success to one and failure to the other. One rule:
 * any 2xx is success.
 */

import { describeRequestFailure, formatRetryReport, isSuccessStatus } from './fetch-retry';

describe('describeRequestFailure', () => {
    function axiosError(extra: Record<string, unknown>) {
        return Object.assign(new Error('request failed'), { isAxiosError: true, ...extra });
    }

    it('names a refused connection in words the user can act on', () => {
        expect(describeRequestFailure(axiosError({ code: 'ECONNREFUSED' }))).toMatch(/refus/i);
    });

    it('names a timeout', () => {
        expect(describeRequestFailure(axiosError({ code: 'ETIMEDOUT' }))).toMatch(/timeout/i);
    });

    it('reports an HTTP status when the server did answer', () => {
        const described = describeRequestFailure(
            axiosError({ response: { status: 503, statusText: 'Service Unavailable' } })
        );

        expect(described).toContain('503');
    });

    it('falls back to something printable for a non-axios failure', () => {
        expect(describeRequestFailure(new Error('boom'))).toContain('boom');
        expect(typeof describeRequestFailure('plain string')).toBe('string');
    });
});

describe('formatRetryReport', () => {
    const report = {
        attempt: 3,
        totalAttempts: 60,
        reason: 'Connection refused',
        nextDelayMs: 5000
    };

    it('reports progress through the budget, so a long wait is legible', () => {
        const line = formatRetryReport(report);

        expect(line).toContain('3');
        expect(line).toContain('60');
    });

    it('includes the reason and the next delay', () => {
        const line = formatRetryReport(report);

        expect(line).toContain('Connection refused');
        expect(line).toMatch(/5\s*s/);
    });

    it('returns a single line — a spinner repaints one line, so a multi-line report tears', () => {
        expect(formatRetryReport(report)).not.toContain('\n');
    });
});

describe('isSuccessStatus — one rule for both callers', () => {
    it.each([200, 201, 202, 204, 299])('treats %i as success', (status) => {
        expect(isSuccessStatus(status)).toBe(true);
    });

    it.each([199, 300, 400, 403, 500, 503])('treats %i as failure', (status) => {
        expect(isSuccessStatus(status)).toBe(false);
    });

    /**
     * The mismatch this exists to close: `fetchWithRetry` resolved on any 2xx, then
     * `isDotcmsRunning` rejected anything that was not exactly 200. A 204 slipped through the
     * first and was refused by the second, so the CLI could report a healthy instance as
     * unreachable.
     */
    it('accepts 204, which the old `status === 200` check rejected', () => {
        expect(isSuccessStatus(204)).toBe(true);
    });
});
