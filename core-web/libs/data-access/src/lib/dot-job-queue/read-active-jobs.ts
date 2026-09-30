import { Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';

import { catchError, map } from 'rxjs/operators';

import { DotJobState, isJobInProgress } from '@dotcms/dotcms-models';

/**
 * How many runs one read of a queue's active listing asks for.
 *
 * The listing pages at 20 by default and is not scoped to the reader, so other authors' runs could
 * push this author's own past the first page. One large page keeps them in view.
 */
export const ACTIVE_JOBS_PAGE_SIZE = 100;

/** One run as a queue's active listing returns it, with the queue's own parameters. */
export interface DotActiveJobEntry<P> {
    id: string;
    state: DotJobState;
    parameters?: P;
}

/**
 * Reads the runs a job queue is still working on, each mapped by the caller.
 *
 * Every queue that restores in-flight state on load reads its listing the same way, so it lives
 * here once rather than in each service:
 * - the listing answers every run not in a terminal state, failed and abandoned ones included, so
 *   only the runs {@link isJobInProgress} counts are kept;
 * - a failed read answers no runs rather than erroring: the author did not ask for it, and a
 *   portlet with nothing restored is what they would see without it.
 *
 * @param http the client to read with
 * @param queue the job queue's name, as in `/api/v1/jobs/{queue}/active`
 * @param toRun maps one in-progress run to what the caller keeps
 * @returns the in-progress runs, for every user; the caller keeps its own
 */
export function readActiveJobs<P, R>(
    http: HttpClient,
    queue: string,
    toRun: (job: DotActiveJobEntry<P>) => R
): Observable<R[]> {
    return http
        .get<{ entity?: { jobs?: DotActiveJobEntry<P>[] } }>(`/api/v1/jobs/${queue}/active`, {
            params: { pageSize: ACTIVE_JOBS_PAGE_SIZE }
        })
        .pipe(
            map((response) =>
                (response?.entity?.jobs ?? []).filter((job) => isJobInProgress(job?.state))
            ),
            map((jobs) => jobs.map(toRun)),
            catchError(() => of([] as R[]))
        );
}
