import {
    AbortError,
    createRuntime,
    HttpError,
    isDotCMSError,
    TimeoutError,
    type DotCMSRuntime,
    type RequestOptions
} from '@dotcms/ai/runtime';

/**
 * Wall-clock deadline applied to every direct `dotcms.request()` a lib tool makes.
 *
 * `createRuntime`'s `timeout` bounds `run()` only; `request()` is documented as having no
 * surrounding timeout of its own. Without a deadline a wedged instance hangs the MCP call
 * forever — the model gets no error, no result, and no way to tell the difference from slow
 * work — and `TIMEOUT`, the one unambiguously retryable code, could never be produced.
 */
const DEFAULT_REQUEST_TIMEOUT_MS = 30_000;

/**
 * Cap on any single error string handed back to the model.
 *
 * A dotCMS 5xx returns its full HTML stack-trace page, tens to hundreds of KB, and
 * `HttpError.message` embeds the body verbatim. A transfer manifest keeps one message PER
 * FAILED FILE, so 200 files against a broken instance would otherwise carry 200 copies of
 * that page in a single tool result.
 */
export const MAX_ERROR_CHARS = 2_000;

/**
 * The server is misconfigured — a deployment problem, not a bad tool call.
 *
 * Carries its own `code` so it lands in {@link ToolFailure} as `CONFIGURATION` rather than
 * `UNKNOWN`, which is what lets the model tell "I called this wrong" from "this server
 * cannot serve anyone right now".
 */
export class ConfigurationError extends Error {
    readonly code = 'CONFIGURATION' as const;

    constructor(message: string) {
        super(message);
        this.name = 'ConfigurationError';
    }
}

/**
 * Build a runtime from the MCP server's environment. One place owns the `DOTCMS_URL` /
 * `AUTH_TOKEN` reading, the default session id, the standard context-error logging, and the
 * per-request deadline — so every tool (`execute`, `search`, `download_assets`,
 * `upload_assets`) constructs the runtime the same way instead of re-deriving it (and
 * silently drifting on which options they set).
 */
export function runtimeFromEnv(
    sessionId?: string,
    opts?: { timeout?: number; includeSpec?: boolean; requestTimeout?: number }
): DotCMSRuntime {
    let runtime: DotCMSRuntime;
    try {
        runtime = createRuntime({
            url: process.env.DOTCMS_URL ?? '',
            token: process.env.AUTH_TOKEN ?? '',
            sessionId: sessionId ?? '__default__',
            timeout: opts?.timeout,
            includeSpec: opts?.includeSpec,
            onContextError: (label, error) => {
                console.error(`[context] failed to load ${label}: ${errorMessage(error)}`);
            }
        });
    } catch (error) {
        // `createRuntime` throws e.g. "token is required" when DOTCMS_URL / AUTH_TOKEN are
        // unset. Raw, that reads to a model as a problem with ITS call — and the transfer
        // tools' own descriptions tell it "you do NOT need a dotCMS token, never go looking
        // for them", so a server misconfiguration would push it toward exactly the
        // credential-hunting those descriptions forbid. Say plainly whose problem it is.
        throw new ConfigurationError(
            `The MCP server is not configured: ${errorMessage(error)}. DOTCMS_URL and ` +
                `AUTH_TOKEN are set in the MCP client's server config, by the operator. This ` +
                `is not a problem with the tool call and no argument can fix it — report it ` +
                `and stop; do not look for credentials.`
        );
    }

    const requestTimeout = opts?.requestTimeout ?? DEFAULT_REQUEST_TIMEOUT_MS;

    return {
        ...runtime,
        /**
         * `request` with a default deadline. A caller that passes its own signal keeps full
         * control and is left alone; otherwise the call is bounded and a deadline hit is
         * reported as `TIMEOUT` rather than `ABORT`, because the two mean opposite things to
         * the model — a timeout is worth retrying, a caller-initiated abort is not.
         */
        request: (options: RequestOptions, reqOpts?: { signal?: AbortSignal }) => {
            if (reqOpts?.signal) {
                return runtime.request(options, reqOpts);
            }

            const controller = new AbortController();
            const timer = setTimeout(() => controller.abort(), requestTimeout);

            return runtime
                .request(options, { signal: controller.signal })
                .catch((error: unknown) => {
                    if (controller.signal.aborted && error instanceof AbortError) {
                        throw new TimeoutError(
                            `Request ${options.method ?? 'GET'} ${options.path} exceeded the ` +
                                `${requestTimeout}ms deadline and was aborted. The instance may be ` +
                                `overloaded or wedged; this is worth retrying.`,
                            requestTimeout
                        );
                    }
                    throw error;
                })
                .finally(() => clearTimeout(timer));
        }
    };
}

/**
 * Normalize any thrown value to a message string, capped at {@link MAX_ERROR_CHARS}.
 *
 * The cap is the point: see MAX_ERROR_CHARS for why an uncapped `HttpError.message` is a
 * real problem rather than a cosmetic one.
 */
export function errorMessage(error: unknown): string {
    const raw = error instanceof Error ? error.message : String(error);

    return raw.length <= MAX_ERROR_CHARS
        ? raw
        : `${raw.slice(0, MAX_ERROR_CHARS)}… [truncated, ${raw.length} chars total]`;
}

/**
 * Whether re-issuing the same call could plausibly succeed.
 *
 * This is the single most useful thing a tool can tell the calling model, and the one it
 * cannot work out for itself: a 429 on file 3 of 200 and a permanent 403 read identically
 * once flattened to a message, so the model either abandons a transfer that would have
 * succeeded or retries one that never can.
 */
function isRetryable(error: unknown): boolean {
    if (error instanceof TimeoutError) {
        return true;
    }
    if (error instanceof HttpError) {
        // 408 Request Timeout and 429 Too Many Requests are explicitly transient; 5xx is the
        // instance failing rather than the request being wrong. Every other 4xx is the call
        // itself being wrong, and retrying it unchanged cannot help.
        return error.status === 408 || error.status === 429 || error.status >= 500;
    }

    // ABORT is caller-initiated, VALIDATION/POLICY are the call being wrong, and
    // SANDBOX/RUNTIME are bugs. None of them improve on a retry.
    return false;
}

/** Stable machine-readable code for any thrown value. */
function errorCode(error: unknown): string {
    if (isDotCMSError(error)) {
        return error.code;
    }

    return error instanceof ConfigurationError ? error.code : 'UNKNOWN';
}

/** The structured failure a tool returns instead of a bare message. */
export interface ToolFailure {
    ok: false;
    operation: string;
    error: string;
    /** Stable machine-readable code (`HTTP`, `TIMEOUT`, `POLICY`, …), or `UNKNOWN`. */
    code: string;
    /**
     * Whether retrying could help. A FIELD rather than only a type, because MCP hands the
     * model a STRING — `instanceof` is unavailable on the far side, so anything the model
     * needs to branch on has to survive JSON.
     */
    retryable: boolean;
    /** HTTP status when the failure was an HTTP error. */
    status?: number;
    [key: string]: unknown;
}

/**
 * Build the failure payload a tool handler returns.
 *
 * Owns the `[MCP Server - <operation>]` prefix convention in one place rather than as a
 * template string at each throw site, and preserves the typed detail (`code`, `status`,
 * `retryable`) that flattening to `.message` used to discard.
 *
 * Deliberately NOT a parallel error hierarchy: `formatSandboxResult` remains the layer for
 * sandbox results (`execute`/`search` already use it). This is only for the direct-request
 * tools, which have no sandbox result to format.
 */
export function toolFailure(
    operation: string,
    error: unknown,
    extra?: Record<string, unknown>
): string {
    const failure: ToolFailure = {
        ok: false,
        operation,
        error: `[MCP Server - ${operation}]: ${errorMessage(error)}`,
        code: errorCode(error),
        retryable: isRetryable(error),
        ...(error instanceof HttpError ? { status: error.status } : {}),
        ...extra
    };

    return JSON.stringify(failure, null, 2);
}
