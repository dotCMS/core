import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/vitest';

import { DotFolderBulkDuplicateSubmitResponse } from '@dotcms/dotcms-models';

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
});
