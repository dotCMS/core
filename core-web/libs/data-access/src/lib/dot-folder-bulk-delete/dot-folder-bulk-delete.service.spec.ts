import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/vitest';

import { HttpErrorResponse } from '@angular/common/http';

import { DotFolderBulkDeleteSubmitResponse, DotFolderDeleteActiveRun } from '@dotcms/dotcms-models';

import {
    DotFolderBulkDeleteRefusal,
    DotFolderBulkDeleteService
} from './dot-folder-bulk-delete.service';

/**
 * Submission half of bulk folder delete (#37063).
 *
 * The server half is specified but not yet implemented, so every expectation here is written
 * against the merged contract
 * (`specs/37063-bulk-folder-delete-frontend/contracts/client-requirements.md`) rather than against
 * anything observed. The first-contact checklist in that feature's `quickstart.md` lists what to
 * re-verify the day the real endpoint answers.
 */
describe('DotFolderBulkDeleteService', () => {
    let spectator: SpectatorHttp<DotFolderBulkDeleteService>;

    const createHttp = createHttpFactory(DotFolderBulkDeleteService);

    const SUBMIT_URL = '/api/v1/assets/folders/_bulkdelete';

    const PATH_A = '//demo.dotcms.com/old-a/';
    const PATH_B = '//demo.dotcms.com/old-b/';

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('submit', () => {
        it('should send every selected path, unfiltered', () => {
            spectator.service.submit([PATH_A, PATH_B]).subscribe();

            const req = spectator.expectOne(SUBMIT_URL, HttpMethod.POST);

            // The client never drops a folder from the submission on its own reading of rights: a
            // selection the author may only partly delete goes whole and the refusals come back per
            // path (FR-004a). Silently shrinking a destructive action is the worse failure.
            expect(req.request.body).toEqual({ assetPaths: [PATH_A, PATH_B] });
        });

        it('should emit the accepted run handle', () => {
            let emitted: DotFolderBulkDeleteSubmitResponse | undefined;
            spectator.service.submit([PATH_A]).subscribe((result) => (emitted = result));

            spectator.expectOne(SUBMIT_URL, HttpMethod.POST).flush({
                entity: {
                    jobId: 'run-1',
                    statusUrl: '/api/v1/jobs/run-1/status',
                    submitted: 1
                }
            });

            expect(emitted).toEqual({
                jobId: 'run-1',
                statusUrl: '/api/v1/jobs/run-1/status',
                submitted: 1
            });
        });

        it('should carry the count the SERVER accepted, not the count submitted', () => {
            // The two disagree whenever the server drops a duplicate or a nested path. Everything
            // downstream — what gets marked, what the report counts — uses the server's number, so
            // the first screen and the last agree by construction (CR-01, CR-03).
            let emitted: DotFolderBulkDeleteSubmitResponse | undefined;
            spectator.service.submit([PATH_A, PATH_B]).subscribe((result) => (emitted = result));

            spectator.expectOne(SUBMIT_URL, HttpMethod.POST).flush({
                entity: { jobId: 'run-1', statusUrl: '/api/v1/jobs/run-1/status', submitted: 1 }
            });

            expect(emitted?.submitted).toBe(1);
        });
    });

    describe('refusals', () => {
        /**
         * The four refusals must stay distinguishable, because each needs different copy (CR-02).
         * The overlap one is the only refusal an ordinary author can actually provoke.
         */
        const refusalFor = (
            status: number,
            body: unknown
        ): DotFolderBulkDeleteRefusal | undefined => {
            let refusal: DotFolderBulkDeleteRefusal | undefined;

            spectator.service.submit([PATH_A]).subscribe({
                error: (error: DotFolderBulkDeleteRefusal) => (refusal = error)
            });

            spectator
                .expectOne(SUBMIT_URL, HttpMethod.POST)
                .flush(body, { status, statusText: 'refused' });

            return refusal;
        };

        it('should report an over-maximum submission as its own refusal', () => {
            const refusal = refusalFor(400, { error: 'OVER_MAX_PATHS', maxPaths: 100 });

            expect(refusal?.kind).toBe('OVER_MAX_PATHS');
        });

        it('should report an empty submission as its own refusal', () => {
            const refusal = refusalFor(400, { error: 'EMPTY_SELECTION' });

            expect(refusal?.kind).toBe('EMPTY_SELECTION');
        });

        it('should report a caller with no entitlement as its own refusal', () => {
            const refusal = refusalFor(403, {});

            expect(refusal?.kind).toBe('NOT_ENTITLED');
        });

        it('should report an overlapping run as its own refusal, naming the folder', () => {
            const refusal = refusalFor(409, { error: 'OVERLAPPING_RUN', path: PATH_A });

            expect(refusal?.kind).toBe('OVERLAPPING_RUN');
            // Named so the copy can say which folder is already being deleted. It must never carry
            // who started the other run: the refused author cannot see that run at all, and naming
            // them would leak it (FR-040, backend FR-029a).
            expect(refusal?.path).toBe(PATH_A);
            expect(JSON.stringify(refusal)).not.toContain('user');
        });

        it('should fall back to an unclassified refusal rather than swallowing an unknown status', () => {
            const refusal = refusalFor(500, {});

            expect(refusal?.kind).toBe('UNCLASSIFIED');
        });

        it('should keep the underlying response available for logging', () => {
            const refusal = refusalFor(403, {});

            // Diagnostics belong in the console, never on screen — the same rule that keeps the
            // server's per-path `message` out of the UI (CR-04).
            expect(refusal?.response).toBeInstanceOf(HttpErrorResponse);
        });
    });

    describe('readActiveRuns', () => {
        const ACTIVE_URL = '/api/v1/jobs/folderBulkDelete/active';

        const flushJobs = (jobs: unknown[]): void => {
            spectator.expectOne(ACTIVE_URL, HttpMethod.GET).flush({ entity: { jobs } });
        };

        it("should read the queue's active runs", () => {
            spectator.service.readActiveRuns().subscribe();

            spectator.expectOne(ACTIVE_URL, HttpMethod.GET);
        });

        it('should keep only runs that are genuinely in progress', () => {
            // THE trap in this feature (CR-10). The listing returns every NON-TERMINAL run — failed
            // and abandoned included — so reading it unfiltered marks folders whose delete already
            // failed, and they stay marked until the framework moves them on. The symptom reads in
            // QA as "sometimes folders stay marked forever", which looks like a client defect.
            let runs: DotFolderDeleteActiveRun[] | undefined;
            spectator.service.readActiveRuns().subscribe((result) => (runs = result));

            flushJobs([
                { id: 'running', state: 'RUNNING', parameters: { assetPaths: [PATH_A] } },
                { id: 'pending', state: 'PENDING', parameters: { assetPaths: [PATH_B] } },
                { id: 'failed', state: 'FAILED', parameters: { assetPaths: ['//x/failed/'] } },
                { id: 'abandoned', state: 'ABANDONED', parameters: { assetPaths: ['//x/gone/'] } },
                { id: 'done', state: 'SUCCESS', parameters: { assetPaths: ['//x/done/'] } }
            ]);

            expect(runs?.map((run) => run.id)).toEqual(['running', 'pending']);
        });

        it('should treat a cancelling run as still in progress', () => {
            // The folders are still being deleted while the cancellation takes effect between them.
            let runs: DotFolderDeleteActiveRun[] | undefined;
            spectator.service.readActiveRuns().subscribe((result) => (runs = result));

            flushJobs([{ id: 'c', state: 'CANCELLING', parameters: { assetPaths: [PATH_A] } }]);

            expect(runs?.length).toBe(1);
        });

        it('should answer with nothing rather than failing when the listing is empty', () => {
            let runs: DotFolderDeleteActiveRun[] | undefined;
            spectator.service.readActiveRuns().subscribe((result) => (runs = result));

            spectator.expectOne(ACTIVE_URL, HttpMethod.GET).flush({ entity: { jobs: [] } });

            expect(runs).toEqual([]);
        });

        it('should degrade to nothing in flight when the read fails', () => {
            // Never an error the author sees: they did not ask for this, and a listing that renders
            // unmarked is the behaviour Content Drive has today (FR-022, SC-010).
            let runs: DotFolderDeleteActiveRun[] | undefined;
            let errored = false;
            spectator.service.readActiveRuns().subscribe({
                next: (result) => (runs = result),
                error: () => (errored = true)
            });

            spectator
                .expectOne(ACTIVE_URL, HttpMethod.GET)
                .flush({}, { status: 500, statusText: 'boom' });

            expect(errored).toBe(false);
            expect(runs).toEqual([]);
        });
    });
});
