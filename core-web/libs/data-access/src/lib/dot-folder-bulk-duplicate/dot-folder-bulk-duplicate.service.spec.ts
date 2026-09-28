import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/vitest';

import {
    DotFolderBulkDuplicateSubmitResponse,
    DotFolderDuplicateActiveRun
} from '@dotcms/dotcms-models';

import {
    DotFolderBulkDuplicateRefusal,
    DotFolderBulkDuplicateService
} from './dot-folder-bulk-duplicate.service';

describe('DotFolderBulkDuplicateService', () => {
    let spectator: SpectatorHttp<DotFolderBulkDuplicateService>;

    const createHttp = createHttpFactory(DotFolderBulkDuplicateService);

    const SUBMIT_URL = '/api/v1/assets/folders/_bulkduplicate';

    beforeEach(() => {
        spectator = createHttp();
    });

    it('should submit the folder paths as they were given', () => {
        spectator.service
            .duplicate(['//demo.dotcms.com/projects/alpha/', '//demo.dotcms.com/projects/beta/'])
            .subscribe();

        const req = spectator.expectOne(SUBMIT_URL, HttpMethod.POST);
        // No destination travels with the request: every duplicate lands beside its source.
        expect(req.request.body).toEqual({
            assetPaths: ['//demo.dotcms.com/projects/alpha/', '//demo.dotcms.com/projects/beta/']
        });
    });

    it('should emit the accepted job handle', () => {
        let emitted: DotFolderBulkDuplicateSubmitResponse | null | undefined;
        spectator.service
            .duplicate(['//demo.dotcms.com/projects/alpha/'])
            .subscribe((result) => (emitted = result));

        const entity: DotFolderBulkDuplicateSubmitResponse = {
            jobId: 'job-1',
            statusUrl: '/api/v1/jobs/job-1/status',
            submitted: 1
        };
        spectator.expectOne(SUBMIT_URL, HttpMethod.POST).flush({ entity });

        expect(emitted).toEqual(entity);
    });

    it('should complete after the submit rather than following the run', () => {
        // The outcome arrives on the pushed completion event, so the submit observable ends here.
        let completed = false;
        spectator.service
            .duplicate(['//demo.dotcms.com/projects/alpha/'])
            .subscribe({ complete: () => (completed = true) });

        spectator.expectOne(SUBMIT_URL, HttpMethod.POST).flush({
            entity: { jobId: 'job-1', statusUrl: '/api/v1/jobs/job-1/status', submitted: 1 }
        });

        expect(completed).toBe(true);
    });

    it('should not call the endpoint at all for an empty selection', () => {
        let emitted: DotFolderBulkDuplicateSubmitResponse | null | undefined;
        spectator.service.duplicate([]).subscribe((result) => (emitted = result));

        spectator.controller.expectNone(SUBMIT_URL);
        expect(emitted).toBeNull();
    });

    /**
     * Refusals before any run exists, told apart the way bulk folder delete tells its own apart
     * (developer decision, 2026-09-28: match delete). The caller switches on a kind rather than a
     * status, because the two `400`s need different words.
     */
    describe('refusals', () => {
        const refuse = (status: number, errorCode?: string) => {
            let refusal: DotFolderBulkDuplicateRefusal | undefined;
            spectator.service
                .duplicate(['//demo.dotcms.com/projects/alpha/'])
                .subscribe({ error: (error) => (refusal = error) });

            spectator
                .expectOne(SUBMIT_URL, HttpMethod.POST)
                .flush(errorCode ? { errors: [{ errorCode, message: 'server prose' }] } : {}, {
                    status,
                    statusText: 'Refused'
                });

            return refusal;
        };

        it.each([
            [400, 'EMPTY_SELECTION'],
            [400, 'OVER_MAX_PATHS'],
            [403, 'NOT_ENTITLED']
        ])('should report a %s %s refusal as that kind', (status, code) => {
            expect(refuse(status, code)?.kind).toBe(code);
        });

        it("should keep the server's sentence for logging, not for the author", () => {
            expect(refuse(400, 'OVER_MAX_PATHS')?.message).toBe('server prose');
        });

        it('should read a 403 with no code as not entitled', () => {
            expect(refuse(403)?.kind).toBe('NOT_ENTITLED');
        });

        it('should not guess between the two 400s when the body carries no code', () => {
            // Telling an author they selected nothing when they hit the ceiling sends them to the
            // wrong fix.
            expect(refuse(400)?.kind).toBe('UNCLASSIFIED');
        });

        it('should not recognise an overlap refusal, which duplication cannot produce', () => {
            // The contract has no overlap guard for duplication, so no copy exists for one.
            expect(refuse(409, 'OVERLAPPING_RUN')?.kind).toBe('UNCLASSIFIED');
        });
    });

    /**
     * The runs still going when Content Drive opens, so a reload can put their status back
     * (#37062, FR-015 as amended). Filtered to what is genuinely in progress, as delete's is.
     */
    describe('readActiveRuns', () => {
        const ACTIVE_URL = '/api/v1/jobs/folderBulkDuplicate/active';

        const flushJobs = (jobs: unknown[]): void => {
            spectator.expectOne(ACTIVE_URL, HttpMethod.GET).flush({ entity: { jobs } });
        };

        it("should read the queue's active runs", () => {
            spectator.service.readActiveRuns().subscribe();

            spectator.expectOne(ACTIVE_URL, HttpMethod.GET);
        });

        it('should keep only runs that are genuinely in progress', () => {
            // The listing returns every non-terminal run, failed and abandoned ones included.
            let runs: DotFolderDuplicateActiveRun[] | undefined;
            spectator.service.readActiveRuns().subscribe((result) => (runs = result));

            flushJobs([
                { id: 'running', state: 'RUNNING', parameters: { assetPaths: ['//x/a/'] } },
                { id: 'pending', state: 'PENDING', parameters: { assetPaths: ['//x/b/'] } },
                { id: 'failed', state: 'FAILED', parameters: { assetPaths: ['//x/c/'] } },
                { id: 'done', state: 'SUCCESS', parameters: { assetPaths: ['//x/d/'] } }
            ]);

            expect(runs?.map((run) => run.id)).toEqual(['running', 'pending']);
        });

        it('should unpack who submitted each run and which folders it duplicates', () => {
            let runs: DotFolderDuplicateActiveRun[] | undefined;
            spectator.service.readActiveRuns().subscribe((result) => (runs = result));

            flushJobs([
                {
                    id: 'job-1',
                    state: 'RUNNING',
                    parameters: { userId: 'dotcms.org.1', assetPaths: ['//x/a/', '//x/b/'] }
                }
            ]);

            expect(runs).toEqual([
                { id: 'job-1', userId: 'dotcms.org.1', assetPaths: ['//x/a/', '//x/b/'] }
            ]);
        });

        it('should answer with no runs when the listing cannot be read', () => {
            // Nothing asked for this read, so a failure must leave the portlet as it is.
            let runs: DotFolderDuplicateActiveRun[] | undefined;
            spectator.service.readActiveRuns().subscribe((result) => (runs = result));

            spectator
                .expectOne(ACTIVE_URL, HttpMethod.GET)
                .flush('boom', { status: 500, statusText: 'Server Error' });

            expect(runs).toEqual([]);
        });
    });
});
