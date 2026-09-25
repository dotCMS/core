import {
    DotFolderBulkDeleteCompletedEvent,
    DotFolderBulkDeleteOutcome,
    DotFolderBulkDeleteSubmitResponse,
    DotFolderDeleteAnnouncementEvent
} from '@dotcms/dotcms-models';

/**
 * Wire-shape fixtures for bulk folder delete (#37063), built from the merged contract at
 * `specs/37063-bulk-folder-delete-frontend/contracts/client-requirements.md`.
 *
 * **Why these are centralised.** The server half is specified but not yet implemented, so every
 * spec in this feature asserts against a contract rather than against something observed. One
 * source of truth means the day the real server answers differently, the divergence shows up here
 * and in the types — not as a dozen specs quietly agreeing with each other about the wrong shape.
 *
 * Keep them minimal and obviously synthetic. They are not sample data for the UI and must never be
 * imported outside a spec.
 */

/** A site-qualified folder path, in the form the shipped single delete already accepts. */
export const FOLDER_PATH_A = '//demo.dotcms.com/old-a/';
export const FOLDER_PATH_B = '//demo.dotcms.com/old-b/';
export const FOLDER_PATH_C = '//demo.dotcms.com/old-c/';

export const RUN_ID = 'e6d9bae8-657b-4e2f-8524-c0222db66355';

/** The `202` answer to a submission: a handle, and the count the **server** accepted (CR-01). */
export const SUBMIT_RESPONSE: DotFolderBulkDeleteSubmitResponse = {
    jobId: RUN_ID,
    statusUrl: `/api/v1/jobs/${RUN_ID}/status`,
    submitted: 3
};

/**
 * A mixed outcome: one deleted, one refused, one never attempted.
 *
 * Note the field names — `failedCount`, not `failCount`; `results[].key`, not `path`; and a
 * three-valued `status` rather than a boolean, because a boolean cannot express "skipped" (CR-07).
 * `message` is diagnostic and must never reach the screen (CR-04).
 */
export const MIXED_OUTCOME: DotFolderBulkDeleteOutcome = {
    total: 3,
    processed: 2,
    successCount: 1,
    failedCount: 1,
    skippedCount: 1,
    results: [
        { key: FOLDER_PATH_A, status: 'SUCCESS' },
        {
            key: FOLDER_PATH_B,
            status: 'FAILED',
            reason: 'PERMISSION_DENIED',
            message: 'diagnostic only — never displayed to the author'
        },
        { key: FOLDER_PATH_C, status: 'SKIPPED' }
    ]
};

/** Everything asked for was deleted. */
export const CLEAN_OUTCOME: DotFolderBulkDeleteOutcome = {
    total: 2,
    processed: 2,
    successCount: 2,
    failedCount: 0,
    skippedCount: 0,
    results: [
        { key: FOLDER_PATH_A, status: 'SUCCESS' },
        { key: FOLDER_PATH_B, status: 'SUCCESS' }
    ]
};

/** The completion push, scoped to the submitter (CR-08). */
export const COMPLETED_EVENT: DotFolderBulkDeleteCompletedEvent = {
    state: 'SUCCESS',
    jobId: RUN_ID,
    ...MIXED_OUTCOME
};

/** A folder entering a delete — reaches everyone who may read it (CR-12). */
export const DELETE_STARTED_EVENT: DotFolderDeleteAnnouncementEvent = {
    jobId: RUN_ID,
    path: FOLDER_PATH_A
};

/** A folder leaving a delete, success or failure alike (CR-12). */
export const DELETE_FINISHED_EVENT: DotFolderDeleteAnnouncementEvent = {
    jobId: RUN_ID,
    path: FOLDER_PATH_A
};

/**
 * In-flight runs as the queue's *active* listing returns them — deliberately including a run that
 * has already **failed** and one the abandonment sweep has marked.
 *
 * This is the CR-10 trap in fixture form: a client that marks folders from this list without
 * filtering on `state` reports `old-b` and `old-c` as being deleted when neither is. Any spec
 * covering the load-time read should assert that only `old-a` ends up marked.
 */
export const ACTIVE_RUNS_RESPONSE = [
    { jobId: RUN_ID, state: 'RUNNING' as const, assetPaths: [FOLDER_PATH_A] },
    { jobId: 'failed-run', state: 'FAILED' as const, assetPaths: [FOLDER_PATH_B] },
    { jobId: 'abandoned-run', state: 'ABANDONED' as const, assetPaths: [FOLDER_PATH_C] }
];
