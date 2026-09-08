/**
 * The job queue's own vocabulary, as the client sees it over
 * `GET /api/v1/jobs/{jobId}/status`.
 *
 * Deliberately free of any feature's shapes: bulk upload, folder copy (#37062) and bulk delete
 * (#37063) all follow a run the same way, and only the outcome they read out of it differs
 * (frontend FR-032).
 */
export const DOT_JOB_STATES = [
    'PENDING',
    'RUNNING',
    'SUCCESS',
    'FAILED',
    'FAILED_PERMANENTLY',
    'ABANDONED',
    'ABANDONED_PERMANENTLY',
    'CANCEL_REQUESTED',
    'CANCELLING',
    'CANCELED'
] as const;

export type DotJobState = (typeof DOT_JOB_STATES)[number];

/**
 * The states a run cannot leave, so the only ones a caller may settle on.
 *
 * `FAILED` and `ABANDONED` are **not** here: the framework documents both as retryable, and their
 * `_PERMANENTLY` counterparts as the point at which it gives up. Treating a bare `FAILED` as final
 * would report a run that is about to be retried as over, and a resumed run reports the whole batch
 * rather than the last attempt.
 */
export const DOT_JOB_TERMINAL_STATES = [
    'SUCCESS',
    'FAILED_PERMANENTLY',
    'ABANDONED_PERMANENTLY',
    'CANCELED'
] as const satisfies readonly DotJobState[];

export type DotJobTerminalState = (typeof DOT_JOB_TERMINAL_STATES)[number];

/**
 * A batch run's outcome, read from a finished job's `result.metadata`.
 *
 * This is the shared batch-outcome shape the server generalised from bulk refresh (backend FR-018),
 * which is why nothing here names files: `results[].key` carries a file name for an upload and a
 * folder path for #37062 / #37063.
 */
export interface DotBatchOutcome<TReason extends string = string> {
    total: number;
    /**
     * Items **attempted**, across every attempt. Skipped items were never attempted, so this is
     * `successCount + failedCount` and not `total`.
     */
    processed: number;
    successCount: number;
    failedCount: number;
    /** Never attempted, because the run was cancelled before reaching them. */
    skippedCount: number;
    results?: DotBatchItemResult<TReason>[];
}

/** One item's outcome within a batch. */
export interface DotBatchItemResult<TReason extends string = string> {
    /** Generic on purpose: a file name for an upload, a folder path elsewhere. */
    key: string;
    status: 'SUCCESS' | 'FAILED' | 'SKIPPED';
    /** Present only on `FAILED`. This is what a client maps to product copy. */
    reason?: TReason;
    /** Diagnostic, for logs. Never shown to a user. */
    message?: string;
}

/**
 * Why a run failed, as the queue records it.
 *
 * Every field is required on the server's immutable, so all of them are present whenever an
 * `errorDetail` is. None of it is shown to a user: `message` is diagnostic and `stackTrace` plainly
 * so (FR-030), which is what the reason codes on {@link DotBatchItemResult} exist to replace.
 */
export interface DotJobErrorDetail {
    message: string;
    exceptionClass: string;
    stackTrace: string;
    /** ISO-8601. A `LocalDateTime` on the server, so it carries no zone. */
    timestamp: string;
    processingStage: string;
}

/**
 * What a finished run recorded.
 *
 * Both halves are optional on the server and independently so: a run can fail with a detail and no
 * outcome, or succeed with an outcome and no detail.
 */
export interface DotJobResult<TOutcome = DotBatchOutcome> {
    errorDetail?: DotJobErrorDetail;
    /** Whatever the processor chose to record. For a batch run, a {@link DotBatchOutcome}. */
    metadata?: TOutcome;
}

/**
 * A job as `GET /api/v1/jobs/{jobId}/status` reports it.
 *
 * `progressTracker` is deliberately absent: it is the server's own instrument, and `progress` is
 * the value derived from it that a caller actually reads.
 */
export interface DotJob<TOutcome = DotBatchOutcome> {
    id: string;
    queueName: string;
    state: DotJobState;
    /** `0`–`1`. Absent until the processor reports one. */
    progress?: number;
    retryCount?: number;
    /** The node running it, when one holds it. */
    executionNode?: string;
    /** ISO-8601 timestamps, absent until the run reaches each point. */
    createdAt?: string;
    startedAt?: string;
    updatedAt?: string;
    completedAt?: string;
    /** What the run was submitted with. Immutable once created, so never a place to read progress. */
    parameters?: Record<string, unknown>;
    result?: DotJobResult<TOutcome>;
}
