import {
    AbortError,
    createRuntime,
    HttpError,
    isDotCMSError,
    NetworkError,
    TimeoutError,
    type DotCMSRuntime,
    type DotCMSRuntimeConfig,
    type RequestOptions
} from '../../runtime';

/**
 * Wall-clock deadline applied to every direct `dotcms.request()` a tool makes.
 *
 * `createRuntime`'s `timeout` bounds `run()` only; `request()` is documented as having no
 * surrounding timeout of its own. Without a deadline a wedged instance hangs the tool call
 * forever — the model gets no error, no result, and no way to tell the difference from slow
 * work — and `TIMEOUT`, the one unambiguously retryable code, could never be produced.
 */
const DEFAULT_REQUEST_TIMEOUT_MS = 30_000;

/**
 * The runtime settings a tool host supplies once. Everything else a tool needs (its sandbox
 * timeout, whether it gets the `spec` global) is decided per tool, not by the host.
 */
export type ToolRuntimeConfig = Pick<
    DotCMSRuntimeConfig,
    'url' | 'token' | 'allow' | 'onCall' | 'onContextError' | 'includeStacks'
>;

/** What an individual tool asks of the runtime it is handed for one call. */
export interface ToolRuntimeOptions {
    /** Sandbox wall-clock timeout (ms) for `run()`. */
    timeout?: number;
    /** Inject the OpenAPI `spec` global into `run()`. */
    includeSpec?: boolean;
    /** Deadline (ms) for each direct `request()`. Default {@link DEFAULT_REQUEST_TIMEOUT_MS}. */
    requestTimeout?: number;
}

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
 * The tool host is misconfigured — a deployment problem, not a bad tool call.
 *
 * Carries its own `code` so it lands in {@link ToolFailure} as `CONFIGURATION` rather than
 * `UNKNOWN`, which is what lets the model tell "I called this wrong" from "this server
 * cannot serve anyone right now".
 */
export class ConfigurationError extends Error {
    readonly code = 'CONFIGURATION' as const;

    /**
     * `cause` carries the underlying error for the HOST (e.g. what a token resolver threw). It
     * is never part of the message, which is what reaches the model.
     */
    constructor(message: string, options?: { cause?: unknown }) {
        super(message);
        this.name = 'ConfigurationError';
        if (options?.cause !== undefined) {
            // Standard ES2022 `cause`, assigned defensively for older lib targets.
            (this as { cause?: unknown }).cause = options.cause;
        }
    }
}

/**
 * Build the runtime one tool call runs against. One place owns the per-request deadline —
 * so every tool constructs its runtime the same way instead of re-deriving it (and silently
 * drifting on which options they set).
 *
 * Called once per tool invocation, deliberately: the runtime owns its context cache, so a
 * fresh runtime is what lets the next call see sites, content types or languages the
 * previous call created. That is also why no session id is threaded through — a runtime
 * that lives for one call has nothing to key a cache on.
 */
export function createToolRuntime(
    config: ToolRuntimeConfig,
    opts?: ToolRuntimeOptions
): DotCMSRuntime {
    let runtime: DotCMSRuntime;
    try {
        runtime = createRuntime({
            ...config,
            timeout: opts?.timeout,
            includeSpec: opts?.includeSpec
        });
    } catch (error) {
        // `createRuntime` throws e.g. "token is required" when the host passed no token.
        // Raw, that reads to a model as a problem with ITS call — and the transfer tools' own
        // descriptions tell it "you do NOT need a dotCMS token, never go looking for them", so
        // a host misconfiguration would push it toward exactly the credential-hunting those
        // descriptions forbid. Say plainly whose problem it is.
        throw new ConfigurationError(
            `The dotCMS tools are not configured: ${errorMessage(error)}. Whoever runs this ` +
                `server or agent supplies the dotCMS URL and token, through the tools' dotCMS ` +
                `connection. This is not a problem with the call and no argument can fix it — ` +
                `report it and stop; do not look for credentials.`
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
    // A deadline hit, or a request that never got a response (refused, reset, DNS): the
    // instance may be restarting or briefly unreachable, and the same call can succeed.
    if (error instanceof TimeoutError || error instanceof NetworkError) {
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
 * Build the failure a tool returns instead of throwing.
 *
 * Owns the `[dotCMS - <operation>]` prefix convention in one place rather than as a
 * template string at each throw site, and preserves the typed detail (`code`, `status`,
 * `retryable`) that flattening to `.message` used to discard.
 *
 * Deliberately NOT a parallel error hierarchy: `formatSandboxResult` remains the layer for
 * sandbox results (`execute`/`search` already use it). This is only for failures a tool
 * call raises outside a sandbox result — a direct request, invalid arguments, a host
 * misconfiguration.
 */
export function toToolFailure(
    operation: string,
    error: unknown,
    extra?: Record<string, unknown>
): ToolFailure {
    return {
        ok: false,
        operation,
        error: `[dotCMS - ${operation}]: ${errorMessage(error)}`,
        code: errorCode(error),
        retryable: isRetryable(error),
        ...(error instanceof HttpError ? { status: error.status } : {}),
        ...extra
    };
}

/** Whether a tool result is a {@link ToolFailure} rather than the tool's normal result. */
export function isToolFailure(value: unknown): value is ToolFailure {
    return (
        typeof value === 'object' &&
        value !== null &&
        (value as { ok?: unknown }).ok === false &&
        typeof (value as { code?: unknown }).code === 'string'
    );
}
