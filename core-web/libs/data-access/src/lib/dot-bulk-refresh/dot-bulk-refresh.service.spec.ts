import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

import { DotBulkRefreshSubmitResponse } from '@dotcms/dotcms-models';

import { DotBulkRefreshService } from './dot-bulk-refresh.service';

describe('DotBulkRefreshService', () => {
    let spectator: SpectatorHttp<DotBulkRefreshService>;

    const createHttp = createHttpFactory(DotBulkRefreshService);

    const SUBMIT_URL = '/api/v1/content/_bulkrefresh';

    beforeEach(() => {
        spectator = createHttp();
    });

    it('should submit the inodes without asking for per-item results', () => {
        spectator.service.refresh(['inode-1', 'inode-2']).subscribe();

        const req = spectator.expectOne(SUBMIT_URL, HttpMethod.POST);
        // Counters are all the completion event carries, and the per-item records are no longer
        // readable over REST now that the status endpoint is gone.
        expect(req.request.body).toEqual({
            contentletIds: ['inode-1', 'inode-2'],
            includeItemResults: false
        });
    });

    it('should emit the accepted job handle', () => {
        let emitted: DotBulkRefreshSubmitResponse | null | undefined;
        spectator.service.refresh(['inode-1']).subscribe((result) => (emitted = result));

        const entity: DotBulkRefreshSubmitResponse = {
            jobId: 'job-1',
            submitted: 1
        };
        spectator.expectOne(SUBMIT_URL, HttpMethod.POST).flush({ entity });

        expect(emitted).toEqual(entity);
    });

    it('should complete after the submit rather than waiting on the job', () => {
        // The whole point of the push model: nothing here follows the run. A subscriber gets one value
        // and the observable completes, so there is no interval left behind to leak or to cancel.
        let completed = false;
        spectator.service.refresh(['inode-1']).subscribe({ complete: () => (completed = true) });

        spectator.expectOne(SUBMIT_URL, HttpMethod.POST).flush({
            entity: { jobId: 'job-1', submitted: 1 }
        });

        expect(completed).toBe(true);
    });

    it('should not call the endpoint at all for an empty selection', () => {
        let emitted: DotBulkRefreshSubmitResponse | null | undefined;
        spectator.service.refresh([]).subscribe((result) => (emitted = result));

        spectator.controller.expectNone(SUBMIT_URL);
        expect(emitted).toBeNull();
    });
});
