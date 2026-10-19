import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/vitest';
import { describe, expect, it } from 'vitest';

import { DotActiveJobEntry } from '@dotcms/dotcms-models';

import { DotJobQueueService } from './dot-job-queue.service';

describe('DotJobQueueService', () => {
    let spectator: SpectatorHttp<DotJobQueueService>;
    const createHttp = createHttpFactory(DotJobQueueService);

    const read = () => {
        let runs: string[] | undefined;

        spectator.service
            .readActiveJobs<
                { label?: string },
                string
            >('someQueue', (job: DotActiveJobEntry<{ label?: string }>) => `${job.id}:${job.parameters?.label}`)
            .subscribe((value) => (runs = value));

        return { runs: () => runs };
    };

    it("should read the queue's active listing in one large page", () => {
        spectator = createHttp();
        read();

        const req = spectator.expectOne(
            '/api/v1/jobs/someQueue/active?pageSize=100',
            HttpMethod.GET
        );

        expect(req.request.params.get('pageSize')).toBe('100');
    });

    it('should keep only the runs still working, mapped by the caller', () => {
        // The listing answers every run not in a terminal state, failed and abandoned included.
        spectator = createHttp();
        const { runs } = read();

        spectator.expectOne('/api/v1/jobs/someQueue/active?pageSize=100', HttpMethod.GET).flush({
            entity: {
                jobs: [
                    { id: 'a', state: 'RUNNING', parameters: { label: 'one' } },
                    { id: 'b', state: 'FAILED', parameters: { label: 'two' } },
                    { id: 'c', state: 'PENDING', parameters: { label: 'three' } }
                ]
            }
        });

        expect(runs()).toEqual(['a:one', 'c:three']);
    });

    it('should answer no runs when the read fails, rather than erroring', () => {
        spectator = createHttp();
        const { runs } = read();

        spectator
            .expectOne('/api/v1/jobs/someQueue/active?pageSize=100', HttpMethod.GET)
            .flush(null, { status: 500, statusText: 'Server Error' });

        expect(runs()).toEqual([]);
    });

    it('should answer no runs when the body carries no jobs', () => {
        spectator = createHttp();
        const { runs } = read();

        spectator.expectOne('/api/v1/jobs/someQueue/active?pageSize=100', HttpMethod.GET).flush({});

        expect(runs()).toEqual([]);
    });
});
