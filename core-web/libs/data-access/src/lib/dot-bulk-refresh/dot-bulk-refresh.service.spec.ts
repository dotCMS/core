import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

import { fakeAsync, tick } from '@angular/core/testing';

import { DotBulkRefreshCounts, DotBulkRefreshJob } from '@dotcms/dotcms-models';

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

    const job = (
        state: string,
        metadata?: DotBulkRefreshCounts
    ): { entity: DotBulkRefreshJob } => ({
        entity: {
            id: JOB_ID,
            state,
            progress: metadata ? 1 : 0.5,
            ...(metadata ? { result: { metadata } } : {})
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
        let emitted: DotBulkRefreshCounts | null | undefined;
        spectator.service.refresh(['inode-1']).subscribe((result) => (emitted = result));

        flushSubmit();

        // First poll is immediate, so a job that finishes fast costs no extra wait.
        spectator.expectOne(STATUS_URL, HttpMethod.GET).flush(job('RUNNING'));
        expect(emitted).toBeUndefined();

        tick(DOT_BULK_REFRESH_POLL_INTERVAL_MS);
        spectator.expectOne(STATUS_URL, HttpMethod.GET).flush(job('SUCCESS', counts));

        expect(emitted).toEqual(counts);
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
        let emitted: DotBulkRefreshCounts | null | undefined;
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
        expect(emitted).toEqual(cancelled);
    }));

    it('should emit null when a terminal job carries no counters', fakeAsync(() => {
        let emitted: DotBulkRefreshCounts | null | undefined;
        spectator.service.refresh(['inode-1']).subscribe((result) => (emitted = result));

        flushSubmit();
        spectator.expectOne(STATUS_URL, HttpMethod.GET).flush(job('SUCCESS'));

        // Substituting the submitted count would claim every item succeeded; substituting zero would
        // claim a total failure. Null lets the caller report an error instead of inventing a number.
        expect(emitted).toBeNull();
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

        expect(error).toBeTruthy();
        spectator.controller
            .match(STATUS_URL)
            .forEach((req) => req.cancelled || req.flush(job('RUNNING')));
    }));

    it('should not call the endpoint at all for an empty selection', () => {
        let emitted: DotBulkRefreshCounts | null | undefined;
        spectator.service.refresh([]).subscribe((result) => (emitted = result));

        spectator.controller.expectNone(SUBMIT_URL);
        expect(emitted).toBeNull();
    });
});
