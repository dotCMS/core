import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';

import { DotBatchOutcome, DOT_JOB_STATES, DotJob, DotJobState } from '@dotcms/dotcms-models';

import { DotJobService } from './dot-job.service';

/**
 * The run-following capability, written against the submission contract rather than against a
 * running server, so it can be built before one exists
 * (`specs/37166-bulk-file-upload/contracts/bulk-upload-api.md` §2, §3).
 *
 * Nothing here mentions uploads. Bulk upload, folder copy (#37062) and bulk delete (#37063) all
 * follow a run identically and differ only in the outcome they read out of it (FR-032), so a test
 * that needed a file name would be a test of the wrong thing.
 */
describe('DotJobService', () => {
    let spectator: SpectatorHttp<DotJobService>;

    const createHttp = createHttpFactory(DotJobService);

    const JOB_ID = 'e6d9bae8-657b-4e2f-8524-c0222db66355';
    const STATUS_URL = `/api/v1/jobs/${JOB_ID}/status`;

    const OUTCOME: DotBatchOutcome = {
        total: 3,
        processed: 3,
        successCount: 2,
        failedCount: 1,
        skippedCount: 0,
        results: [
            { key: 'brochure.pdf', status: 'SUCCESS' },
            { key: 'notes.exe', status: 'FAILED', reason: 'DISALLOWED_FILE_TYPE', message: 'diag' },
            { key: 'later.png', status: 'SKIPPED' }
        ]
    };

    const job = (over: Partial<DotJob<DotBatchOutcome>> = {}): DotJob<DotBatchOutcome> => ({
        id: JOB_ID,
        queueName: 'assetBulkUpload',
        state: 'RUNNING',
        ...over
    });

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('status', () => {
        it('should read the generic job status endpoint', () => {
            spectator.service.status(JOB_ID).subscribe();

            spectator.expectOne(STATUS_URL, HttpMethod.GET);
        });

        it('should unwrap the response entity', (done) => {
            spectator.service.status(JOB_ID).subscribe((result) => {
                expect(result).toEqual(job({ state: 'SUCCESS' }));
                done();
            });

            spectator
                .expectOne(STATUS_URL, HttpMethod.GET)
                .flush({ entity: job({ state: 'SUCCESS' }) });
        });

        it('should surface a transport failure rather than swallowing it', (done) => {
            // A run whose state cannot be read is not a run that finished cleanly. Reporting
            // nothing here would let a caller settle rows on no evidence (FR-024).
            spectator.service.status(JOB_ID).subscribe({
                error: (error) => {
                    expect(error).toBeTruthy();
                    done();
                }
            });

            spectator
                .expectOne(STATUS_URL, HttpMethod.GET)
                .flush(null, { status: 404, statusText: 'Not Found' });
        });
    });

    describe('isTerminal', () => {
        it.each([['SUCCESS'], ['FAILED_PERMANENTLY'], ['ABANDONED_PERMANENTLY'], ['CANCELED']])(
            'should treat %s as terminal',
            (state) => {
                expect(spectator.service.isTerminal(state as DotJobState)).toBe(true);
            }
        );

        it.each([['FAILED'], ['ABANDONED']])(
            'should not treat %s as terminal, because the framework retries it',
            (state) => {
                // The one that would bite: reporting a bare FAILED as final settles a run that is
                // about to be retried, and a resumed run reports the whole batch, not the attempt.
                expect(spectator.service.isTerminal(state as DotJobState)).toBe(false);
            }
        );

        it.each([['PENDING'], ['RUNNING'], ['CANCEL_REQUESTED'], ['CANCELLING']])(
            'should not treat %s as terminal',
            (state) => {
                expect(spectator.service.isTerminal(state as DotJobState)).toBe(false);
            }
        );

        it('should cover every state the queue can report', () => {
            // Guards the pair of lists against the server growing a state the client silently
            // mis-classifies as still running.
            const classified = DOT_JOB_STATES.map((state) => spectator.service.isTerminal(state));

            expect(classified.filter(Boolean)).toHaveLength(4);
        });

        it('should not treat a missing state as terminal', () => {
            expect(spectator.service.isTerminal(undefined)).toBe(false);
        });
    });

    describe('outcomeOf', () => {
        it('should read the outcome out of the job result metadata', () => {
            expect(
                spectator.service.outcomeOf(
                    job({ state: 'SUCCESS', result: { metadata: OUTCOME } })
                )
            ).toEqual(OUTCOME);
        });

        it('should return nothing for a job carrying no result', () => {
            // A finished run with no outcome is a case the caller must report as an error rather
            // than as a clean run over nothing (FR-024), so this must not invent an empty one.
            expect(spectator.service.outcomeOf(job({ state: 'SUCCESS' }))).toBeUndefined();
        });

        it('should return nothing for a result carrying no metadata', () => {
            expect(
                spectator.service.outcomeOf(job({ state: 'FAILED_PERMANENTLY', result: {} }))
            ).toBeUndefined();
        });
    });
});
