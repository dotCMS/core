import { Observable, of, timer } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { last, map, switchMap, takeWhile, timeout } from 'rxjs/operators';

import {
    DOT_BULK_REFRESH_TERMINAL_STATES,
    DotBulkRefreshCounts,
    DotBulkRefreshJob,
    DotBulkRefreshJobState,
    DotBulkRefreshSubmitResponse
} from '@dotcms/dotcms-models';

/** How often the job is polled while it runs. */
export const DOT_BULK_REFRESH_POLL_INTERVAL_MS = 1500;

/**
 * How long a run is followed before giving up on it.
 *
 * Not a cancellation — the job keeps going server-side. This only bounds how long the UI claims to
 * know what is happening, because a job on a node that dies mid-run never reaches a terminal state and
 * an unbounded poll would leave the toolbar reporting work in progress for the rest of the session.
 */
export const DOT_BULK_REFRESH_TIMEOUT_MS = 5 * 60 * 1000;

const BULK_REFRESH_URL = '/api/v1/content/_bulkrefresh';

/**
 * Reindexes a selection of contentlets through `POST /api/v1/content/_bulkrefresh`.
 *
 * The endpoint is job-backed: the submit call only accepts the work and answers `202` with a job id, so
 * this service submits and then polls the status endpoint until the job settles, emitting the final
 * counters once. Callers get a single-emission observable and never see the job machinery.
 *
 * Progress while the run is in flight is deliberately not surfaced: the status endpoint reports a
 * `progress` float but no live counters, so there is nothing item-wise to report until the end.
 */
@Injectable({
    providedIn: 'root'
})
export class DotBulkRefreshService {
    readonly #http = inject(HttpClient);

    /**
     * Reindexes every given contentlet and emits the final counters.
     *
     * @param inodes Contentlet inodes. The server collapses them by identifier, so the reported
     *     `total` can be lower than what was sent.
     * @returns The terminal counters, or `null` when the selection was empty or the finished job
     *     carried none — in which case there is no honest count to report and the caller should treat
     *     it as a failure rather than substitute one.
     */
    refresh(inodes: string[]): Observable<DotBulkRefreshCounts | null> {
        if (!inodes.length) {
            return of(null);
        }

        return this.#submit(inodes).pipe(switchMap(({ jobId }) => this.#awaitCompletion(jobId)));
    }

    #submit(inodes: string[]): Observable<DotBulkRefreshSubmitResponse> {
        return this.#http
            .post<{
                entity: DotBulkRefreshSubmitResponse;
            }>(BULK_REFRESH_URL, {
                contentletIds: inodes,
                // Counters are all a toast needs, and the per-item records are persisted with the job.
                includeItemResults: false
            })
            .pipe(map((response) => response.entity));
    }

    /**
     * Polls the job until it settles.
     *
     * The first poll fires immediately so a fast job is not held back by the interval, and polling
     * stops on the terminal value itself rather than one tick later.
     */
    #awaitCompletion(jobId: string): Observable<DotBulkRefreshCounts | null> {
        return timer(0, DOT_BULK_REFRESH_POLL_INTERVAL_MS).pipe(
            switchMap(() => this.#status(jobId)),
            takeWhile((job) => !this.#isTerminal(job.state), true),
            last(),
            // Bounds the whole run, not the gap between polls: `last()` emits once, at the end.
            timeout(DOT_BULK_REFRESH_TIMEOUT_MS),
            map((job) => job.result?.metadata ?? null)
        );
    }

    #status(jobId: string): Observable<DotBulkRefreshJob> {
        return this.#http
            .get<{ entity: DotBulkRefreshJob }>(`${BULK_REFRESH_URL}/${jobId}`)
            .pipe(map((response) => response.entity));
    }

    /**
     * Whether the job has settled. A cancelled or failed job is settled too — the caller reports what
     * happened rather than waiting for a state that will never come.
     */
    #isTerminal(state: DotBulkRefreshJobState): boolean {
        return (DOT_BULK_REFRESH_TERMINAL_STATES as readonly string[]).includes(state);
    }
}
