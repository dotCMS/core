export interface DotVelocityPlaygroundForm {
    velocity: string;
}

export type DotVelocityResponseContentType = 'json' | 'xml' | 'plaintext';

export interface DotVelocityPlaygroundResponse {
    body: string;
    contentType: DotVelocityResponseContentType;
    elapsedMs: number;
    /** Non-fatal warnings parsed from the `X-Dot-Velocity-Warnings` response header. */
    warnings: VelocityWarning[];
}

/**
 * A single Velocity error as returned by `POST /api/vtl/dynamic` with a `400`.
 * Mirrors the backend `VelocityErrorView`. `message` is a concise one-liner;
 * `detail` carries the full engine output (for parse errors, the exhaustive
 * "was expecting one of …" token list). `line`/`column`/`templateName` are only
 * present when Velocity reports a position.
 */
export interface VelocityError {
    message: string;
    errorType?: string;
    templateName?: string;
    line?: number;
    column?: number;
    detail?: string;
}

/**
 * A non-fatal Velocity warning (undefined reference, null method result).
 * Mirrors the backend `VelocityWarningView`.
 */
export interface VelocityWarning {
    type: 'UNDEFINED_REFERENCE' | 'NULL_METHOD_RESULT' | 'INVALID_METHOD' | 'NULL_SET';
    message: string;
    reference?: string;
    line?: number;
    column?: number;
}

/**
 * The `{ "errors": [...], "warnings": [...] }` body the backend returns with a
 * `400` when the submitted Velocity fails to parse or evaluate.
 */
export interface VelocityErrorResponse {
    errors: VelocityError[];
    warnings?: VelocityWarning[];
}

/**
 * Normalized error the store exposes to the view. `structured` carries the
 * parsed Velocity error detail when the backend returned the structured `400`
 * contract; `message` is always populated (an i18n key or a raw string) so the
 * banner has something to show even for unstructured/infra failures. `warnings`
 * carries any non-fatal issues reported alongside the error.
 */
export interface DotVelocityPlaygroundError {
    message: string;
    structured: VelocityError | null;
    warnings: VelocityWarning[];
}
