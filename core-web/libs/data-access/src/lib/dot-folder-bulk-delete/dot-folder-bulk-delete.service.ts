import { Observable, of, throwError } from 'rxjs';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

import {
    DotFolderBulkDeleteSubmitResponse,
    DotFolderDeleteActiveRun,
    isJobInProgress
} from '@dotcms/dotcms-models';

/**
 * Why a submission was refused before any run was created.
 *
 * The kinds must stay distinguishable because each needs its own copy — the overlap one is the only
 * refusal an ordinary author can actually provoke, and "someone is already deleting one of these
 * folders" is actionable where a generic failure is not (contract CR-02).
 */
export interface DotFolderBulkDeleteRefusal {
    kind:
        | 'EMPTY_SELECTION'
        | 'OVER_MAX_PATHS'
        | 'NOT_ENTITLED'
        | 'OVERLAPPING_RUN'
        | 'UNCLASSIFIED';
    /** On `OVERLAPPING_RUN`, the folder already being deleted. Never who is deleting it. */
    path?: string;
    /** On `OVER_MAX_PATHS`, the ceiling the server advertised with the refusal. */
    maxPaths?: number;
    /** Kept for logging. Never rendered. */
    response?: HttpErrorResponse;
}

/** Shape of the error body the endpoint is expected to answer refusals with. See the note below. */
interface RefusalBody {
    error?: string;
    path?: string;
    maxPaths?: number;
}

const SUBMIT_URL = '/api/v1/assets/folders/_bulkdelete';

/** The queue this feature's runs live in. */
const QUEUE_NAME = 'folderBulkDelete';

const ACTIVE_URL = `/api/v1/jobs/${QUEUE_NAME}/active`;

/**
 * Submission for Content Drive bulk folder delete (#37063).
 *
 * **Written against a contract, not against a running server.** The server half is specified and
 * merged but not yet implemented, so the shapes here come from
 * `specs/37063-bulk-folder-delete-frontend/contracts/client-requirements.md`. That feature's
 * `quickstart.md` carries the first-contact checklist for the day the real endpoint answers.
 */
@Injectable({ providedIn: 'root' })
export class DotFolderBulkDeleteService {
    readonly #http = inject(HttpClient);

    /**
     * Submit a selection of folder paths for deletion.
     *
     * Answers immediately with a run handle; the deletion happens in the background. The paths go
     * exactly as given — this service never filters a selection, because a selection the author may
     * only partly delete is submitted whole and the refusals come back per path (FR-004a).
     *
     * Errors with a {@link DotFolderBulkDeleteRefusal} rather than the raw response, so callers
     * switch on a kind instead of re-deriving one from a status code.
     */
    submit(assetPaths: string[]): Observable<DotFolderBulkDeleteSubmitResponse> {
        return this.#http
            .post<{
                entity: DotFolderBulkDeleteSubmitResponse;
            }>(SUBMIT_URL, { assetPaths })
            .pipe(
                map((response) => response.entity),
                catchError((response: HttpErrorResponse) =>
                    throwError(() => this.#toRefusal(response))
                )
            );
    }

    /**
     * The folders every genuinely in-flight delete is working on.
     *
     * Answers with an empty list rather than erroring: see the `catchError` below.
     */
    readActiveRuns(): Observable<DotFolderDeleteActiveRun[]> {
        return this.#http.get<{ entity?: { jobs?: DotFolderDeleteActiveRun[] } }>(ACTIVE_URL).pipe(
            map((response) => response?.entity?.jobs ?? []),
            // THE filter. The endpoint is called "active" but answers with every run in a
            // NON-TERMINAL state, failed and abandoned ones included. Without this, folders
            // whose delete already failed are reported as still being deleted, and they stay
            // that way until the framework moves the run on (contract CR-10).
            map((jobs) => jobs.filter((job) => isJobInProgress(job?.state))),
            // Never surfaced to the author: they did not ask for this read, and a listing that
            // renders unmarked is exactly the behaviour Content Drive has today. Marking
            // degrades; the portlet does not (FR-022, SC-010).
            catchError(() => of([] as DotFolderDeleteActiveRun[]))
        );
    }

    /**
     * Maps a refused submission onto a kind the client has copy for.
     *
     * **The two `400`s are the awkward part**, and the contract does not yet settle them: an empty
     * selection and an over-maximum selection both answer `400`, and they need different messages —
     * one is "you selected nothing", the other has to name the limit. This reads an `error` code out
     * of the body to tell them apart, which is the client's *assumption*, raised with the server
     * half at dotCMS/core#37063 (comment 5720585127).
     *
     * If the server settles on a different shape, this method is the only thing that changes: the
     * kinds the rest of the client switches on are stable either way.
     */
    #toRefusal(response: HttpErrorResponse): DotFolderBulkDeleteRefusal {
        const body = (response.error ?? {}) as RefusalBody;

        switch (response.status) {
            case 400:
                // Falls through to UNCLASSIFIED when the body names neither, rather than guessing
                // one: telling an author they selected nothing when they hit a ceiling would send
                // them looking for the wrong fix.
                if (body.error === 'OVER_MAX_PATHS') {
                    return { kind: 'OVER_MAX_PATHS', maxPaths: body.maxPaths, response };
                }

                if (body.error === 'EMPTY_SELECTION') {
                    return { kind: 'EMPTY_SELECTION', response };
                }

                return { kind: 'UNCLASSIFIED', response };

            case 403:
                // Status alone is enough — nothing else here answers 403.
                return { kind: 'NOT_ENTITLED', response };

            case 409:
                // The folder, never the other submitter: the refused author cannot see that run at
                // all, so naming who started it would leak it (FR-040, backend FR-029a).
                return { kind: 'OVERLAPPING_RUN', path: body.path, response };

            default:
                return { kind: 'UNCLASSIFIED', response };
        }
    }
}
