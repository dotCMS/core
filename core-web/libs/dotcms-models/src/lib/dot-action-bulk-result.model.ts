/**
 * Expected response when hit the endpoint: /api/v1/workflow/contentlet/actions/bulk/fire
 * And bulk endpoints in templates
 *
 * @interface
 */
export interface DotActionBulkResult {
    skippedCount: number;
    successCount: number;
    fails: DotBulkFailItem[];
    action?: string;
}

/**
 * Response of `POST /api/v1/workflow/actions/default/fire/{systemAction}`, the multi-contentlet
 * system-action endpoint.
 *
 * Streamed rather than assembled, which is why the shape differs from {@link DotActionBulkResult}:
 * one entry per contentlet keyed by its id, then a summary. A per-item failure does **not** fail the
 * request — the status stays 200 and the item is counted in {@link summary.failCount} — so the
 * summary is the only honest source of what actually happened.
 */
export interface DotFireDefaultActionResult {
    /** One entry per contentlet, keyed by identifier. A failed item holds an error payload. */
    results: Record<string, unknown>[];
    summary: {
        /** Number of contentlets the request was asked to act on. */
        affected: number;
        successCount: number;
        failCount: number;
        /** Server-side duration in ms. */
        time: number;
    };
}

// optional attrs because api is not consistent
export interface DotBulkFailItem {
    errorMessage: string;
    element?: string;
    inode?: string;
    description?: string;
}
