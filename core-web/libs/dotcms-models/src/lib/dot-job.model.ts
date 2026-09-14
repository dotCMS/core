import { DotBulkUploadFailureReason } from './dot-content-drive.model';

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
    /**
     * `true` when this run was a resubmission of a batch that had already succeeded.
     *
     * The counts cannot tell the two apart. Under the collision branch a retry that worked collides
     * on every file, so it reads as "everything failed" — and an author told that deletes and
     * re-uploads files that were already there, which is worse than being offered no retry at all.
     */
    duplicateSubmission?: boolean;
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
 * Where a batch of files should land.
 *
 * Exactly one of the two, never both: the workflow API this sits in front of already separates a
 * site id from a folder id and carries disambiguation messaging because callers confuse them, so
 * the contract states the intent with two fields rather than overloading one
 * (`contracts/bulk-upload-api.md` §1).
 */
export interface DotBulkUploadTarget {
    folderId?: string;
    siteId?: string;
}

/** The JSON `form` part of a bulk upload submission. Field names are binding on both halves. */
export interface DotBulkUploadForm extends DotBulkUploadTarget {
    baseType: 'DOTASSET' | 'FILEASSET';
    /**
     * The batch's total size, declared so the server can refuse an over-ceiling batch *before*
     * reading the body.
     *
     * Advisory, never the enforcement point: the authoritative total is accumulated while the
     * content is read. Omitting it only forfeits the early refusal, so the author would upload
     * gigabytes before being told no.
     */
    totalSizeBytes?: number;
}

/** The `202` answer to a bulk upload: a handle, not an outcome. */
export interface DotBulkUploadSubmitResponse {
    jobId: string;
    /** Absolute enough to follow on its own, so the queue name is not the client's to hardcode. */
    statusUrl: string;
    /**
     * File parts the **server** read into the batch, and the number a client displays.
     *
     * It equals the `total` the outcome later reports, so the first screen and the last agree by
     * construction. It normally matches what the author chose: a part the per-file ceiling refused
     * still counts, because it is carried into the batch as that file's own failure rather than
     * dropped. Where it does not match, parts were lost between the browser and the server, and a
     * client rendering its own count would show a figure no later screen confirms.
     *
     * Optional because an instance older than the field answers without it, not because it is
     * discretionary. Absent, the caller's own count is the honest fallback.
     */
    submitted?: number;
}

/**
 * The payload of a `BULK_UPLOAD_COMPLETED` system event.
 *
 * Pushed over the websocket the admin UI already holds when a batch settles, scoped to whoever
 * submitted it — so a client receives runs it never started, from another tab, another window or a
 * Login-As session, and `jobId` is how it tells its own from somebody else's.
 *
 * The counters and `results` are optional for the same reason the bulk refresh event's are: a job
 * that finished without recording an outcome carries only `state`, and a caller must treat that as
 * a failure rather than as a clean run over nothing, which is what all-zero counters would look
 * like.
 */
export interface DotBulkUploadCompletedEvent extends Partial<
    DotBatchOutcome<DotBulkUploadFailureReason>
> {
    state: DotJobState;
    jobId?: string;
}

/**
 * What {@link DotBulkUploadSubmitResponse}'s request emits on the way to answering.
 *
 * A union rather than a progress callback, so the compiler makes a caller acknowledge that this
 * request has a lifecycle: bytes go out for a while, and only then is the batch accepted. A callback
 * would let a caller subscribe and quietly ignore half of what happens.
 */
export type DotBulkUploadEvent =
    | {
          kind: 'progress';
          /** Bytes handed to the socket so far. */
          loaded: number;
          /**
           * Bytes in the whole multipart body: every file part with its headers and boundaries,
           * plus the JSON `form` part. So it is larger than the summed file sizes, and larger than
           * the `totalSizeBytes` declared in the form — two different numbers for two different
           * jobs, and not to be conflated.
           *
           * Absent when the browser cannot compute a length, which is why a caller must be able to
           * fall back to reporting activity without a position.
           */
          total?: number;
      }
    | { kind: 'accepted'; handle: DotBulkUploadSubmitResponse };
