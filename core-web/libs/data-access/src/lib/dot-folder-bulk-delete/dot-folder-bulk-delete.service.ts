import { Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { DotFolderBulkDeleteSubmitResponse } from '@dotcms/dotcms-models';

/**
 * Why a submission was refused before any run was created.
 *
 * The four kinds must stay distinguishable because each needs its own copy — the overlap one is
 * the only refusal an ordinary author can actually provoke, and "someone is already deleting one
 * of these folders" is actionable where a generic failure is not (contract CR-02).
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

/**
 * Submission and in-flight reads for Content Drive bulk folder delete (#37063).
 *
 * NOT YET IMPLEMENTED — this is the stub the test set was written against, so the specs compile and
 * fail on behaviour rather than on missing symbols. Implemented in T015 (submit) and T042 (the
 * in-flight read) once the Red gate is confirmed.
 */
@Injectable({ providedIn: 'root' })
export class DotFolderBulkDeleteService {
    /**
     * Submit a selection of folder paths for deletion.
     *
     * Answers immediately with a run handle; the deletion itself happens in the background.
     */
    submit(_assetPaths: string[]): Observable<DotFolderBulkDeleteSubmitResponse> {
        throw new Error('DotFolderBulkDeleteService.submit is not implemented yet (T015)');
    }
}
