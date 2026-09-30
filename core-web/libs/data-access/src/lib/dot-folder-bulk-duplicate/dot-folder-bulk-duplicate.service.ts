import { Observable, of, throwError } from 'rxjs';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

import {
    DotFolderBulkDuplicateSubmitResponse,
    DotFolderDuplicateActiveRun
} from '@dotcms/dotcms-models';

import { DotJobQueueService } from '../dot-job-queue/dot-job-queue.service';

/**
 * Why a duplication was refused before any run was created.
 *
 * Delete's kinds minus the overlap one: duplication carries no overlap guard, so the contract has no
 * `409` and no copy is written for it (`specs/37062-folder-copy-backend/contracts/folder-bulk-duplicate-api.md`
 * §3). Each kind has its own sentence in the shell.
 */
export type DotFolderBulkDuplicateRefusalKind =
    | 'EMPTY_SELECTION'
    | 'OVER_MAX_PATHS'
    | 'NOT_ENTITLED'
    | 'UNCLASSIFIED';

/** A refused submission, as the kind the client has copy for. */
export interface DotFolderBulkDuplicateRefusal {
    kind: DotFolderBulkDuplicateRefusalKind;
    /** The server's own sentence, for logging. Never rendered: it is not localised. */
    message?: string;
    /** Kept for logging, and for the HTTP error manager when the kind is unclassified. */
    response?: HttpErrorResponse;
}

/** The refusal body, as the REST layer's `ErrorEntity` serialises it. */
interface RefusalBody {
    errors?: { errorCode?: string; message?: string; fieldName?: string }[];
}

const BULK_DUPLICATE_URL = '/api/v1/assets/folders/_bulkduplicate';

/** The queue this feature's runs live in. */
const QUEUE_NAME = 'folderBulkDuplicate';

/** The parameters of one active run, as far as this client reads them. */
interface ActiveRunParameters {
    userId?: string;
    assetPaths?: string[];
}

/**
 * Submits a selection of folders to be duplicated through
 * `POST /api/v1/assets/folders/_bulkduplicate`.
 *
 * Each folder is duplicated in place: its duplicate lands beside it, in the same parent, under a name
 * the server derives, so no destination is ever sent. The endpoint is job-backed and answers `202`
 * immediately; the outcome arrives by push, as a `BULK_FOLDER_DUPLICATE_COMPLETED` system event, so
 * nothing here follows the run.
 */
@Injectable({
    providedIn: 'root'
})
export class DotFolderBulkDuplicateService {
    readonly #http = inject(HttpClient);
    readonly #jobQueueService = inject(DotJobQueueService);

    /**
     * Asks for every given folder to be duplicated.
     *
     * @param assetPaths Site-qualified folder paths, such as `//demo.dotcms.com/projects/alpha/`. The
     *     server collapses repeats, so the `submitted` count it answers with can be lower than what was
     *     sent.
     * @returns The accepted job's handle, or `null` for an empty selection, which is not worth a
     *     request. A refusal errors with a {@link DotFolderBulkDuplicateRefusal}, so callers switch
     *     on a kind rather than a status code.
     */
    duplicate(assetPaths: string[]): Observable<DotFolderBulkDuplicateSubmitResponse | null> {
        if (!assetPaths.length) {
            return of(null);
        }

        return this.#http
            .post<{ entity: DotFolderBulkDuplicateSubmitResponse }>(BULK_DUPLICATE_URL, {
                assetPaths
            })
            .pipe(
                map((response) => response.entity),
                catchError((response: HttpErrorResponse) =>
                    throwError(() => this.#toRefusal(response))
                )
            );
    }

    /**
     * Reads the duplicates still in progress, so a reload can put their status back.
     *
     * Filtered to runs genuinely in progress: the listing answers with every non-terminal run,
     * failed and abandoned ones included. A failure answers with no runs; nothing asked for this
     * read, so it must not surface.
     *
     * @returns the runs in progress, for every user; the caller keeps its own
     */
    readActiveRuns(): Observable<DotFolderDuplicateActiveRun[]> {
        return this.#jobQueueService.readActiveJobs<
            ActiveRunParameters,
            DotFolderDuplicateActiveRun
        >(QUEUE_NAME, (job) => ({
            id: job.id,
            userId: job.parameters?.userId,
            assetPaths: job.parameters?.assetPaths ?? []
        }));
    }

    /**
     * Maps a refused submission onto a kind the client has copy for, the way bulk folder delete's
     * service does.
     *
     * Switches on `errorCode`, not on status, because the two `400`s need different words. Where the
     * body carries no code, a `403` still reads as not entitled, and nothing guesses between the
     * `400`s: telling an author they selected nothing when they hit the ceiling sends them to the
     * wrong fix.
     */
    #toRefusal(response: HttpErrorResponse): DotFolderBulkDuplicateRefusal {
        const error = ((response.error ?? {}) as RefusalBody).errors?.[0];
        const message = error?.message;

        switch (error?.errorCode) {
            case 'EMPTY_SELECTION':
                return { kind: 'EMPTY_SELECTION', message, response };
            case 'OVER_MAX_PATHS':
                return { kind: 'OVER_MAX_PATHS', message, response };
            case 'NOT_ENTITLED':
                return { kind: 'NOT_ENTITLED', message, response };
            default:
                return {
                    kind: 403 === response.status ? 'NOT_ENTITLED' : 'UNCLASSIFIED',
                    message,
                    response
                };
        }
    }
}
