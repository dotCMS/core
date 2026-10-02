import { Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

import { DotActiveJobEntry, isJobInProgress } from '@dotcms/dotcms-models';

/**
 * How many runs one read of a queue's active listing asks for.
 *
 * The listing pages at 20 by default and is not scoped to the reader, so other authors' runs could
 * push this author's own past the first page. One large page keeps them in view.
 */
const ACTIVE_JOBS_PAGE_SIZE = 100;

/**
 * Operations on the server's job queues that do not depend on which feature's queue it is.
 *
 * Each feature's own service (bulk folder duplicate and delete, bulk upload) keeps what is specific
 * to its queue: the submit endpoint and the refusals it maps. What every queue answers the same
 * way lives here once, so the three cannot drift apart.
 */
@Injectable({ providedIn: 'root' })
export class DotJobQueueService {
    readonly #http = inject(HttpClient);

    /**
     * Reads the runs a job queue is still working on, each mapped by the caller.
     *
     * - The listing answers every run not in a terminal state, failed and abandoned ones included,
     *   so only the runs {@link isJobInProgress} counts are kept.
     * - A failed read answers no runs rather than erroring: the author did not ask for it, and a
     *   portlet with nothing restored is what they would see without it.
     *
     * @param queue the job queue's name, as in `/api/v1/jobs/{queue}/active`
     * @param toRun maps one in-progress run to what the caller keeps
     * @returns the in-progress runs, for every user; the caller keeps its own
     */
    readActiveJobs<P, R>(queue: string, toRun: (job: DotActiveJobEntry<P>) => R): Observable<R[]> {
        return this.#http
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
}
