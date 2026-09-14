import { isHttpError } from './http';

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
    if (isHttpError(error)) {
        if (error.code === 'ECONNREFUSED') {
            return 'Connection refused - service not accepting connections yet';
        }

        if (error.code === 'ETIMEDOUT') {
            return 'Connection timeout - service too slow or not responding';
        }

        // Without these the raw code reaches the user, and "ENOTFOUND" is not a sentence.
        //
        // These stay DIAGNOSTIC — what went wrong, not what to do about it. Callers add the
        // remedy, and they know their own context; baking advice in here produced
        // "Host not found - check the address... Check the address and that the instance is
        // running." in the dotcms CLI.
        if (error.code === 'ENOTFOUND') {
            return 'Host not found (DNS lookup failed)';
        }

        if (error.code === 'ECONNRESET') {
            return 'Connection reset by the server';
        }

        if (error.code === 'CERT_HAS_EXPIRED') {
            return 'TLS certificate has expired';
        }

        if (
            error.code === 'DEPTH_ZERO_SELF_SIGNED_CERT' ||
            error.code === 'SELF_SIGNED_CERT_IN_CHAIN'
        ) {
            return 'TLS certificate is self-signed and not trusted';
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
