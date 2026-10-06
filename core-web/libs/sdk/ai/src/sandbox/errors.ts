/**
 * The single typed error hierarchy for `@dotcms/ai`.
 *
 * Both verbs of the runtime — `request()` (direct) and `run()` (sandboxed) — surface
 * the *same* error types, because both route through one shared request core. The
 * model-facing string an MCP tool builds is *formatting on top of* these errors, not a
 * separate error model.
 *
 * Every error carries a stable, machine-readable `code` and a `toJSON()` so it survives
 * the worker→host serialization boundary (a thrown error becomes `{ name, message, stack }`
 * by default; these types preserve `code` and the structured detail too).
 */

export type DotCMSErrorCode =
    | 'VALIDATION' // adapter input failed its schema / request options invalid
    | 'POLICY' // allow-list / policy rejected the call before it reached the wire
    | 'HTTP' // dotCMS returned a non-2xx response
    | 'TIMEOUT' // sandbox wall-clock (or a per-call timeout) elapsed
    | 'ABORT' // the call was aborted (e.g. the sandbox timed out mid-flight)
    | 'SANDBOX' // the model-authored code threw while executing
    | 'RUNTIME'; // anything else originating in the runtime itself

/** Serializable shape every DotCMSError flattens to (and that crosses the worker boundary). */
export interface SerializedDotCMSError {
    name: string;
    code: DotCMSErrorCode;
    message: string;
    /**
     * Present only when `includeStack` is enabled on the runtime. Host stack traces can
     * contain absolute host paths, so they are withheld from model-authored code by default.
     */
    stack?: string;
    /** Type-specific structured detail (HTTP status + body, validation issues, etc.). */
    detail?: Record<string, unknown>;
}

export abstract class DotCMSError extends Error {
    abstract readonly code: DotCMSErrorCode;

    constructor(message: string, options?: { cause?: unknown }) {
        super(message);
        this.name = new.target.name;
        if (options?.cause !== undefined) {
            // Standard ES2022 `cause`, but assigned defensively for older lib targets.
            (this as { cause?: unknown }).cause = options.cause;
        }
        // Restore the prototype chain so `instanceof` works after transpilation to ES5/ES2017.
        Object.setPrototypeOf(this, new.target.prototype);
    }

    /** Type-specific structured detail; overridden by subclasses that carry data. */
    detail(): Record<string, unknown> | undefined {
        return undefined;
    }

    toJSON(): SerializedDotCMSError {
        return {
            name: this.name,
            code: this.code,
            message: this.message,
            stack: this.stack,
            detail: this.detail()
        };
    }
}

/** Adapter input failed validation, or request options were malformed. */
export class ValidationError extends DotCMSError {
    readonly code = 'VALIDATION' as const;
    /** Field-level issues, when the source was a Zod (or similar) schema. */
    readonly issues?: unknown[];

    constructor(message: string, issues?: unknown[]) {
        super(message);
        this.issues = issues;
    }

    detail(): Record<string, unknown> | undefined {
        return this.issues ? { issues: this.issues } : undefined;
    }
}

/** The allow-list / policy rejected the call before it reached the network. */
export class PolicyError extends DotCMSError {
    readonly code = 'POLICY' as const;
    readonly method: string;
    readonly path: string;

    constructor(message: string, method: string, path: string) {
        super(message);
        this.method = method;
        this.path = path;
    }

    detail(): Record<string, unknown> {
        return { method: this.method, path: this.path };
    }
}

/** dotCMS responded with a non-2xx status. Carries the status and the (text) body. */
export class HttpError extends DotCMSError {
    readonly code = 'HTTP' as const;
    readonly status: number;
    readonly statusText: string;
    readonly body: string;

    constructor(status: number, statusText: string, body: string) {
        super(`HTTP ${status} ${statusText}: ${body}`);
        this.status = status;
        this.statusText = statusText;
        this.body = body;
    }

    detail(): Record<string, unknown> {
        return { status: this.status, statusText: this.statusText, body: this.body };
    }
}

/** A timeout elapsed (sandbox wall-clock, per-adapter-call, or context load). */
export class TimeoutError extends DotCMSError {
    readonly code = 'TIMEOUT' as const;
    readonly timeoutMs?: number;

    constructor(message: string, timeoutMs?: number) {
        super(message);
        this.timeoutMs = timeoutMs;
    }

    detail(): Record<string, unknown> | undefined {
        return this.timeoutMs === undefined ? undefined : { timeoutMs: this.timeoutMs };
    }
}

/** The call was aborted in-flight (typically because the sandbox timed out). */
export class AbortError extends DotCMSError {
    readonly code = 'ABORT' as const;
}

/** Model-authored code threw while executing inside the sandbox. */
export class SandboxError extends DotCMSError {
    readonly code = 'SANDBOX' as const;
    /** The original error's name (e.g. "TypeError") as seen inside the sandbox. */
    readonly originalName?: string;

    constructor(message: string, originalName?: string, stack?: string) {
        super(message);
        this.originalName = originalName;
        if (stack) this.stack = stack;
    }

    detail(): Record<string, unknown> | undefined {
        return this.originalName ? { originalName: this.originalName } : undefined;
    }
}

/** Catch-all for runtime-internal failures that don't fit a more specific type. */
export class RuntimeError extends DotCMSError {
    readonly code = 'RUNTIME' as const;
}

/** Type guard for any error in the hierarchy. */
export function isDotCMSError(value: unknown): value is DotCMSError {
    return value instanceof DotCMSError;
}

/**
 * Normalize any thrown value into a serializable error shape. Used at the worker→host
 * boundary and when formatting a result, so callers always get a consistent structure
 * regardless of what was thrown.
 */
export function serializeError(value: unknown): SerializedDotCMSError {
    if (isDotCMSError(value)) {
        return value.toJSON();
    }
    if (value instanceof Error) {
        // Preserve a code already set on a plain Error (e.g. Node system errors, or an
        // adapter that tags its own); fall back to RUNTIME.
        const existingCode = (value as { code?: unknown }).code;
        return {
            name: value.name,
            code: (typeof existingCode === 'string' ? existingCode : 'RUNTIME') as DotCMSErrorCode,
            message: value.message,
            stack: value.stack
        };
    }
    return { name: 'Error', code: 'RUNTIME', message: String(value) };
}
