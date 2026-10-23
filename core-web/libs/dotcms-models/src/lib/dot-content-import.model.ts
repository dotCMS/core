import { DotJobState } from './dot-job.model';

/**
 * The `form` part of a content import request (#37896): `POST /api/v1/content/_import`, and its
 * preview `/_validate`, queued as a job.
 *
 * `language` is always sent by Content Drive: leaving it out turns on multilingual mode on the
 * server, which is what `'-1'` asks for explicitly. `site` is the site the Drive is browsing; the
 * server resolves in-row references (File and Image values) against it instead of the session's site.
 */
export interface DotContentImportForm {
    /** Content type id or variable. */
    contentType: string;
    /** Language id, or `'-1'` for a multilingual file. */
    language: string;
    workflowActionId: string;
    /** Key fields, as field ids or variables. */
    fields?: string[];
    /** Site id or hostname. */
    site: string;
}

/** Why rows land where they do in an import's destination summary. */
export type DotImportDestinationSource = 'CSV' | 'CONTENT_TYPE_DEFAULT' | 'EXISTING';

/**
 * How many rows an import places in one site/folder.
 *
 * `CSV` comes from the row's site/folder column, `CONTENT_TYPE_DEFAULT` is new content in a CSV with
 * no such column (the content type's own site/folder), and `EXISTING` is matched content that keeps
 * its place. Rows with errors are not counted.
 */
export interface DotImportDestination {
    /** `//host/folder/path/`; a folder of `/` is the site root. */
    path: string;
    rows: number;
    source: DotImportDestinationSource;
}

/** One info, warning or error line of an import result. */
export interface DotImportValidationMessage {
    code?: string;
    message: string;
    /** CSV line number, when the message is about a line. */
    row?: number;
    field?: string;
    value?: string;
}

/**
 * The result of a preview or an import, as `GET /api/v1/content/_import/{jobId}` returns it in
 * `entity.result`.
 *
 * In a preview, `data.summary.created` and `.updated` are the counts that *would* be created and
 * updated. `data.processed.parsedRows` includes the header line.
 */
export interface DotImportResult {
    type?: 'PREVIEW' | 'PUBLISH';
    contentTypeName?: string;
    contentTypeVariableName?: string;
    keyFields?: string[];
    file?: {
        totalRows?: number;
        headers?: {
            valid?: string[];
            invalid?: string[];
            missing?: string[];
        };
    };
    data?: {
        processed?: { parsedRows?: number; failedRows?: number };
        summary?: { created?: number; updated?: number; failed?: number };
    };
    info?: DotImportValidationMessage[];
    warning?: DotImportValidationMessage[];
    error?: DotImportValidationMessage[];
    destinations?: DotImportDestination[];
    stoppedOnErrorAtLine?: number;
}

/** What creating an import or preview job answers with. */
export interface DotImportJobCreated {
    jobId: string;
    statusUrl?: string;
}

/** An import or preview job, as `GET /api/v1/content/_import/{jobId}` returns it. */
export interface DotImportJob {
    id: string;
    state: DotJobState;
    parameters?: DotImportJobParameters;
    result?: DotImportResult;
}

/** The job parameters Content Drive reads from an import job. */
export interface DotImportJobParameters {
    cmd?: 'preview' | 'publish';
    userId?: string;
    contentType?: string;
    /** Original name of the uploaded CSV, when the server recorded it. */
    fileName?: string;
    /** The stored CSV, which an import can reuse instead of uploading it again. */
    tempFileId?: string;
}

/**
 * The payload of an `IMPORT_CONTENTLETS_COMPLETED` system event.
 *
 * Counts are absent when the job ended without a result (for example a failure before any row was
 * read); `message` carries the job's first error when it failed.
 */
export interface DotImportCompletedEvent {
    jobId: string;
    state: DotJobState;
    cmd: 'preview' | 'publish';
    created?: number;
    updated?: number;
    failed?: number;
    message?: string;
}
