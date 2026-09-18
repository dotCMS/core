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
    /**
     * The server's own sentence, for logging.
     *
     * Deliberately NOT the thing rendered. It is server-generated English, so it is not localised,
     * and the client has its own copy for each kind. Carried because a refusal with no diagnostic
     * at all is hard to chase in a log.
     */
    message?: string;
    /** Kept for logging. Never rendered. */
    response?: HttpErrorResponse;
}

/**
 * The refusal body, as the REST layer's existing `ErrorEntity` serialises it.
 *
 * Not a shape invented for this feature: it is what `ValidationException` already builds its body
 * from, which is why the backend half chose it over an ad-hoc field when it pinned these refusals
 * (`specs/37063-bulk-folder-delete-backend/contracts/bulk-delete-api.md` §1).
 */
interface RefusalBody {
    errors?: { errorCode?: string; message?: string; fieldName?: string }[];
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
     * Switches on `errorCode`, not on status: the two `400`s — an empty selection and one over the
     * maximum — need different copy and status alone cannot separate them. That gap was raised from
     * this side (dotCMS/core#37063, comment 5720585127) and settled by the backend half on
     * `ErrorEntity`, the structured-error type the REST layer already uses.
     *
     * **What the body does not carry:** a structured field naming the folder an `OVERLAPPING_RUN`
     * collided on. It appears in `message` as prose, which is server-generated English and so not
     * rendered. The client therefore reports the collision without naming which folder caused it —
     * a shortfall against FR-040, recorded rather than papered over by parsing a sentence.
     */
    #toRefusal(response: HttpErrorResponse): DotFolderBulkDeleteRefusal {
        const body = (response.error ?? {}) as RefusalBody;
        const error = body.errors?.[0];
        const message = error?.message;

        switch (error?.errorCode) {
            case 'EMPTY_SELECTION':
                return { kind: 'EMPTY_SELECTION', message, response };
            case 'OVER_MAX_PATHS':
                return { kind: 'OVER_MAX_PATHS', message, response };
            case 'NOT_ENTITLED':
                return { kind: 'NOT_ENTITLED', message, response };
            case 'OVERLAPPING_RUN':
                return { kind: 'OVERLAPPING_RUN', message, response };
            default:
                // Falls back on the status where the body carries no code — an older instance, or a
                // failure that never reached the endpoint's own error handling. Guessing between the
                // two `400`s is what this deliberately does NOT do: telling an author they selected
                // nothing when they hit a ceiling sends them looking for the wrong fix.
                return {
                    kind: 403 === response.status ? 'NOT_ENTITLED' : 'UNCLASSIFIED',
                    message,
                    response
                };
        }
    }
}
