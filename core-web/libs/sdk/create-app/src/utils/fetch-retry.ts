import axios from 'axios';

/**
 * Retry reporting, split out so it can be rendered by whoever owns the terminal.
 *
 * `fetchWithRetry` used to `console.log` between attempts while an `ora` spinner was running.
 * A spinner owns and repaints the last line, so concurrent writes tear — which is exactly the
 * mangled retry block in the report for #37262. Handing the caller a structured report lets the
 * CLI route it through the spinner instead of fighting it, without going silent: a ten-minute
 * wait with no output is the symptom being fixed, not the fix.
 */

export interface RetryReport {
    /** 1-based, so it reads the way it prints. */
    attempt: number;
    totalAttempts: number;
    reason: string;
    nextDelayMs: number;
}

export type RetryReporter = (report: RetryReport) => void;

/**
 * One rule for what counts as a successful response.
 *
 * `fetchWithRetry` accepted any 2xx while `isDotcmsRunning` demanded exactly 200, so a 204 was
 * success to one and failure to the other and a healthy instance could be reported unreachable.
 */
export function isSuccessStatus(status: number): boolean {
    return status >= 200 && status < 300;
}

/** Turns a request failure into something worth showing a user mid-wait. */
export function describeRequestFailure(error: unknown): string {
    if (axios.isAxiosError(error)) {
        if (error.code === 'ECONNREFUSED') {
            return 'Connection refused - service not accepting connections yet';
        }

        if (error.code === 'ETIMEDOUT' || error.code === 'ECONNABORTED') {
            return 'Connection timeout - service too slow or not responding';
        }

        if (error.response) {
            return `HTTP ${error.response.status}: ${error.response.statusText}`;
        }

        return error.code || error.message;
    }

    if (error instanceof Error) {
        return error.message;
    }

    return String(error);
}

/**
 * A single line: a spinner repaints one line, and a multi-line report tears across the repaint.
 */
export function formatRetryReport({
    attempt,
    totalAttempts,
    reason,
    nextDelayMs
}: RetryReport): string {
    return `dotCMS not ready (attempt ${attempt}/${totalAttempts}) - ${reason} - retrying in ${Math.round(nextDelayMs / 1000)}s`;
}
