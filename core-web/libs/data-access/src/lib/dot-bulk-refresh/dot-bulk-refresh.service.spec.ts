import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

import { fakeAsync, tick } from '@angular/core/testing';

import {
    DotBulkRefreshCounts,
    DotBulkRefreshJob,
    DotBulkRefreshJobState,
    DotBulkRefreshOutcome
} from '@dotcms/dotcms-models';

import {
    DOT_BULK_REFRESH_POLL_INTERVAL_MS,
    DOT_BULK_REFRESH_TIMEOUT_MS,
    DotBulkRefreshService
} from './dot-bulk-refresh.service';

describe('DotBulkRefreshService', () => {
    let spectator: SpectatorHttp<DotBulkRefreshService>;

    const createHttp = createHttpFactory(DotBulkRefreshService);

    const SUBMIT_URL = '/api/v1/content/_bulkrefresh';
    const JOB_ID = 'job-1';
    const STATUS_URL = `${SUBMIT_URL}/${JOB_ID}`;

    const counts: DotBulkRefreshCounts = {
        total: 3,
        successCount: 2,
        failedCount: 1,
        skippedCount: 0,
        versionsIndexed: 5
    };

    /**
     * A status response in the shape the server actually sends.
     *
     * The counters sit directly on `result` — OptionalJobResultSerializer flattens the job's metadata
     * map into it rather than nesting it under `metadata`. An earlier version of this fixture nested
     * them, which made the suite pass against a contract the server never emits.
     */
    const job = (
        state: DotBulkRefreshJobState,
        counts?: DotBulkRefreshCounts
    ): { entity: DotBulkRefreshJob } => ({
        entity: {
            id: JOB_ID,
            state,
            progress: counts ? 1 : 0.5,
            ...(counts ? { result: { ...counts } } : {})
        }
    });

    beforeEach(() => {
        spectator = createHttp();
    });

    /**
     * Flushes the submit call and lets the first poll fire.
     *
     * The poll loop starts with a zero-delay timer, which is still an async task — so the first GET
     * only exists after the microtask queue drains, hence the `tick(0)`.
     */
    const flushSubmit = (): void => {
        spectator.expectOne(SUBMIT_URL, HttpMethod.POST).flush({
            entity: { jobId: JOB_ID, statusUrl: STATUS_URL, submitted: 1 }
        });
        tick(0);
    };

    it('should submit the inodes without asking for per-item results', () => {
        spectator.service.refresh(['inode-1', 'inode-2']).subscribe();

        const req = spectator.expectOne(SUBMIT_URL, HttpMethod.POST);
        // A toast needs counters only. Per-item records are persisted with the job, so requesting
        // them would store an array nothing here reads.
        expect(req.request.body).toEqual({
            contentletIds: ['inode-1', 'inode-2'],
            includeItemResults: false
        });
    });

    it('should poll the status endpoint until the job is terminal and emit its counters', fakeAsync(() => {
        let emitted: DotBulkRefreshOutcome | null | undefined;
        spectator.service.refresh(['inode-1']).subscribe((result) => (emitted = result));

        flushSubmit();

        // First poll is immediate, so a job that finishes fast costs no extra wait.
        spectator.expectOne(STATUS_URL, HttpMethod.GET).flush(job('RUNNING'));
        expect(emitted).toBeUndefined();

        tick(DOT_BULK_REFRESH_POLL_INTERVAL_MS);
        spectator.expectOne(STATUS_URL, HttpMethod.GET).flush(job('SUCCESS', counts));

        expect(emitted).toEqual({ state: 'SUCCESS', counts });
    }));

    it('should stop polling once the job is terminal', fakeAsync(() => {
        spectator.service.refresh(['inode-1']).subscribe();

        flushSubmit();
        spectator.expectOne(STATUS_URL, HttpMethod.GET).flush(job('SUCCESS', counts));

        // A completed job never changes again; polling on would be pure noise against the server.
        tick(DOT_BULK_REFRESH_POLL_INTERVAL_MS * 3);
        spectator.controller.expectNone(STATUS_URL);
    }));

    it('should report a cancelled job through the same path as a successful one', fakeAsync(() => {
        let emitted: DotBulkRefreshOutcome | null | undefined;
        spectator.service.refresh(['inode-1']).subscribe((result) => (emitted = result));

        flushSubmit();

        const cancelled: DotBulkRefreshCounts = {
            total: 3,
            successCount: 1,
            failedCount: 0,
            skippedCount: 2,
            versionsIndexed: 1
        };
        spectator.expectOne(STATUS_URL, HttpMethod.GET).flush(job('CANCELED', cancelled));

        // Cancelling is an outcome, not an error: some items were reindexed and the user should be
        // told which, rather than shown a failure that hides the work that did land.
        expect(emitted).toEqual({ state: 'CANCELED', counts: cancelled });
    }));

    it('should emit null when a terminal job carries no counters', fakeAsync(() => {
        let emitted: DotBulkRefreshOutcome | null | undefined;
        spectator.service.refresh(['inode-1']).subscribe((result) => (emitted = result));

        flushSubmit();
        spectator.expectOne(STATUS_URL, HttpMethod.GET).flush(job('SUCCESS'));

        // Substituting the submitted count would claim every item succeeded; substituting zero would
        // claim a total failure. Null lets the caller report an error instead of inventing a number.
        expect(emitted).toEqual({ state: 'SUCCESS', counts: null });
    }));

    it('should error rather than poll forever when the job never finishes', fakeAsync(() => {
        let error: unknown;
        spectator.service.refresh(['inode-1']).subscribe({ error: (e) => (error = e) });

        flushSubmit();

        // A job abandoned by a dying node never reaches a terminal state, and an unbounded poll would
        // leave the toolbar claiming work is in progress for the rest of the session.
        let elapsed = 0;
        while (elapsed < DOT_BULK_REFRESH_TIMEOUT_MS + DOT_BULK_REFRESH_POLL_INTERVAL_MS) {
            spectator.controller.match(STATUS_URL).forEach((req) => {
                if (!req.cancelled) {
                    req.flush(job('RUNNING'));
                }
            });
            tick(DOT_BULK_REFRESH_POLL_INTERVAL_MS);
            elapsed += DOT_BULK_REFRESH_POLL_INTERVAL_MS;
        }

        // A bare TimeoutError has no `status`, so the shared error handler would find no handler and
        // say nothing — clearing the indicator with a stale grid and no explanation.
        expect(error).toBeTruthy();
        expect((error as { status?: number })?.status).toBe(504);
        spectator.controller
            .match(STATUS_URL)
            .forEach((req) => req.cancelled || req.flush(job('RUNNING')));
    }));

    it('should read counters off result directly, not from a nested metadata key', fakeAsync(() => {
        // This is the contract the whole feature hangs on. The server flattens the job's metadata map
        // into `result`; reading `result.metadata` yields undefined, which previously made every
        // successful reindex report as a failure. The fixture below is the real wire shape.
        let emitted: DotBulkRefreshOutcome | null | undefined;
        spectator.service.refresh(['inode-1']).subscribe((result) => (emitted = result));

        flushSubmit();
        spectator.expectOne(STATUS_URL, HttpMethod.GET).flush({
            entity: {
                id: JOB_ID,
                state: 'SUCCESS',
                progress: 1,
                result: {
                    total: 3,
                    successCount: 2,
                    failedCount: 1,
                    skippedCount: 0,
                    versionsIndexed: 5
                }
            }
        });

        expect(emitted?.counts).toEqual(counts);
    }));

    it('should surface a failed terminal state rather than just its counters', fakeAsync(() => {
        // A job that dies mid-run still carries the counters it had reached, so counters alone cannot
        // distinguish a failure from a clean run. The state has to travel with them.
        let emitted: DotBulkRefreshOutcome | null | undefined;
        spectator.service.refresh(['inode-1']).subscribe((result) => (emitted = result));

        flushSubmit();
        spectator.expectOne(STATUS_URL, HttpMethod.GET).flush(
            job('FAILED_PERMANENTLY', {
                total: 0,
                successCount: 0,
                failedCount: 0,
                skippedCount: 0,
                versionsIndexed: 0
            })
        );

        expect(emitted?.state).toBe('FAILED_PERMANENTLY');
    }));

    it('should survive a transient status failure instead of abandoning the run', fakeAsync(() => {
        // The job is unaffected by a failed poll, and a five-minute run is ~200 requests. Dying on the
        // first blip would abandon work that is still progressing server-side.
        let emitted: DotBulkRefreshOutcome | null | undefined;
        let error: unknown;
        spectator.service
            .refresh(['inode-1'])
            .subscribe({ next: (r) => (emitted = r), error: (e) => (error = e) });

        flushSubmit();
        spectator.expectOne(STATUS_URL, HttpMethod.GET).flush('boom', {
            status: 502,
            statusText: 'Bad Gateway'
        });

        tick(1000);
        spectator.expectOne(STATUS_URL, HttpMethod.GET).flush(job('SUCCESS', counts));

        expect(error).toBeUndefined();
        expect(emitted?.counts).toEqual(counts);
    }));

    it('should not cancel a status call that outlives the poll interval', fakeAsync(() => {
        // exhaustMap, not switchMap: under sustained latency a cancel-and-reissue loop means no poll
        // ever completes and the run only ends at the overall timeout.
        spectator.service.refresh(['inode-1']).subscribe();

        flushSubmit();
        const inflight = spectator.expectOne(STATUS_URL, HttpMethod.GET);

        tick(DOT_BULK_REFRESH_POLL_INTERVAL_MS * 2);
        expect(inflight.cancelled).toBe(false);

        inflight.flush(job('SUCCESS', counts));
    }));

    it('should not call the endpoint at all for an empty selection', () => {
        let emitted: DotBulkRefreshOutcome | null | undefined;
        spectator.service.refresh([]).subscribe((result) => (emitted = result));

        spectator.controller.expectNone(SUBMIT_URL);
        expect(emitted).toBeNull();
    });
});
