import { Observable, of, throwError, timer } from 'rxjs';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, exhaustMap, last, map, retry, takeWhile, timeout } from 'rxjs/operators';

import {
    DOT_BULK_REFRESH_TERMINAL_STATES,
    DotBulkRefreshCounts,
    DotBulkRefreshJob,
    DotBulkRefreshJobState,
    DotBulkRefreshOutcome,
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

/** Consecutive failures of a single status poll tolerated before the run is abandoned. */
export const DOT_BULK_REFRESH_POLL_RETRIES = 3;

const BULK_REFRESH_URL = '/api/v1/content/_bulkrefresh';

/**
 * Reindexes a selection of contentlets through `POST /api/v1/content/_bulkrefresh`.
 *
 * The endpoint is job-backed: the submit call only accepts the work and answers `202` with a job id, so
 * this service submits and then polls the status endpoint until the job settles, emitting the outcome
 * once. Callers get a single-emission observable and never see the job machinery.
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
     * Reindexes every given contentlet and emits how the job settled.
     *
     * @param inodes Contentlet inodes. The server collapses them by identifier, so the reported
     *     `total` can be lower than what was sent.
     * @returns The terminal state and its counters. `counts` is `null` when the selection was empty or
     *     the finished job carried none — there is no honest count to report in that case, and the
     *     caller must not substitute one.
     */
    refresh(inodes: string[]): Observable<DotBulkRefreshOutcome | null> {
        if (!inodes.length) {
            return of(null);
        }

        return this.#submit(inodes).pipe(exhaustMap(({ jobId }) => this.#awaitCompletion(jobId)));
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
    #awaitCompletion(jobId: string): Observable<DotBulkRefreshOutcome> {
        return timer(0, DOT_BULK_REFRESH_POLL_INTERVAL_MS)
            .pipe(
                // exhaustMap, not switchMap: a status call slower than the interval must be allowed to
                // finish. Cancelling and reissuing it would mean that under sustained latency no poll
                // ever completes, nothing is ever emitted, and only the overall timeout ends the run.
                exhaustMap(() =>
                    this.#status(jobId).pipe(
                        // One blip over a five-minute run is ~200 requests' worth of exposure. The job
                        // is unaffected by a failed poll, so retrying beats abandoning it — but only
                        // for failures that asking again can fix. A 401, 403 or 404 will answer the
                        // same way three times over, and a 404 specifically means the job id is wrong,
                        // which the caller should hear at once rather than three seconds late.
                        retry({
                            count: DOT_BULK_REFRESH_POLL_RETRIES,
                            delay: (error) =>
                                this.#isRetryable(error) ? timer(1000) : throwError(() => error)
                        })
                    )
                ),
                takeWhile((job) => !this.#isTerminal(job.state), true),
                // Bounds the whole run, not the gap between polls: `last()` emits once, at the end.
                last(),
                timeout(DOT_BULK_REFRESH_TIMEOUT_MS)
            )
            .pipe(
                // A bare TimeoutError has no `status`, so the shared HTTP error handler finds no
                // handler for it and says nothing at all — the indicator would clear silently and
                // leave a stale grid. Give it a shape that handler understands.
                catchError((error) =>
                    throwError(() =>
                        error?.name === 'TimeoutError'
                            ? new HttpErrorResponse({
                                  status: 504,
                                  statusText:
                                      'Stopped waiting for the reindex job; it may still be running'
                              })
                            : error
                    )
                ),
                map((job) => ({
                    state: job.state,
                    // `result` IS the counters: the server flattens the job's metadata map straight
                    // into it rather than nesting it under `metadata`.
                    counts: job.result ? this.#toCounts(job.result) : null
                }))
            );
    }

    #toCounts(result: NonNullable<DotBulkRefreshJob['result']>): DotBulkRefreshCounts {
        return {
            total: result.total,
            successCount: result.successCount,
            failedCount: result.failedCount,
            skippedCount: result.skippedCount,
            versionsIndexed: result.versionsIndexed
        };
    }

    #status(jobId: string): Observable<DotBulkRefreshJob> {
        return this.#http
            .get<{ entity: DotBulkRefreshJob }>(`${BULK_REFRESH_URL}/${jobId}`)
            .pipe(map((response) => response.entity));
    }

    /**
     * Whether a failed poll is worth repeating: server faults and transport failures only.
     *
     * Status 0 covers the network-level failures (DNS, connection reset, CORS) that HttpClient reports
     * without a status code.
     */
    #isRetryable(error: unknown): boolean {
        const status = (error as { status?: number })?.status;

        return undefined === status || 0 === status || status >= 500;
    }

    /**
     * Whether the job has settled. A cancelled or failed job is settled too — the caller decides what
     * each terminal state means rather than this waiting for one that will never come.
     */
    #isTerminal(state: DotBulkRefreshJobState): boolean {
        return (DOT_BULK_REFRESH_TERMINAL_STATES as readonly string[]).includes(state);
    }
}
