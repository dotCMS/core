import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    DotBatchOutcome,
    DOT_JOB_TERMINAL_STATES,
    DotJob,
    DotJobState
} from '@dotcms/dotcms-models';

const JOBS_URL = '/api/v1/jobs';

/**
 * Follows a job-queue run: read its state and progress, tell whether it is over, and read what it
 * recorded.
 *
 * Feature-agnostic on purpose (frontend FR-032). Bulk upload, folder copy (#37062) and bulk delete
 * (#37063) all follow a run identically and differ only in the outcome they read out of it, so
 * `TOutcome` is a parameter rather than a union of every feature's shape. Submitting is *not* here:
 * each feature posts to its own endpoint, and a generic `submit(url, body)` would add a layer
 * without adding a decision.
 *
 * Nothing here polls. A caller reads status when it has reason to; completion is pushed over the
 * websocket the admin UI already holds, which is what the per-feature completion events exist for.
 */
@Injectable({ providedIn: 'root' })
export class DotJobService {
    readonly #http = inject(HttpClient);

    /**
     * Reads a run's current state.
     *
     * Transport failures are left to propagate. A run whose state cannot be read is not a run that
     * finished cleanly, and swallowing that would let a caller settle rows on no evidence
     * (FR-024).
     */
    status<TOutcome = DotBatchOutcome>(jobId: string): Observable<DotJob<TOutcome>> {
        return this.#http
            .get<{ entity: DotJob<TOutcome> }>(`${JOBS_URL}/${jobId}/status`)
            .pipe(map((response) => response.entity));
    }

    /**
     * Whether a run is over, meaning it will not be retried.
     *
     * `FAILED` and `ABANDONED` answer `false`: the queue documents both as retryable and their
     * `_PERMANENTLY` counterparts as where it gives up. Settling on a bare `FAILED` reports a run
     * that is about to be retried as finished, and the retry then reports the whole batch again.
     */
    isTerminal(state: DotJobState | undefined): boolean {
        return !!state && (DOT_JOB_TERMINAL_STATES as readonly DotJobState[]).includes(state);
    }

    /**
     * What the run recorded, or nothing.
     *
     * Nothing is a real answer and must stay distinguishable from an empty outcome: a finished run
     * carrying no outcome is an error for the caller to report, not a clean run over zero items
     * (FR-024).
     */
    outcomeOf<TOutcome = DotBatchOutcome>(job: DotJob<TOutcome>): TOutcome | undefined {
        return job.result?.metadata;
    }
}
