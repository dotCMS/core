import {
    createHttpFactory,
    HttpMethod,
    mockProvider,
    SpectatorHttp
} from '@openng/spectator/vitest';
import { Subject } from 'rxjs';
import { vi } from 'vitest';

import { DotContentImportForm, DotImportCompletedEvent, DotImportJob } from '@dotcms/dotcms-models';

import { DotContentImportService } from './dot-content-import.service';

import { DotEventsSocket } from '../dot-websocket/dot-events-socket.service';

const IMPORT_URL = '/api/v1/content/_import';

describe('DotContentImportService', () => {
    let spectator: SpectatorHttp<DotContentImportService>;

    let completions: Subject<DotImportCompletedEvent>;

    const createHttp = createHttpFactory({
        service: DotContentImportService,
        providers: [mockProvider(DotEventsSocket, { on: vi.fn(() => completions) })]
    });

    const FORM: DotContentImportForm = {
        contentType: 'Blog',
        language: '1',
        workflowActionId: 'publish',
        fields: ['title'],
        site: 'demo.dotcms.com'
    };
    const CSV = new File(['title\nHello'], 'blogs.csv', { type: 'text/csv' });

    beforeEach(() => {
        completions = new Subject<DotImportCompletedEvent>();
        spectator = createHttp();
    });

    /**
     * #37896 FR-012 (PR #38054 review): the preview is a job on the queue, through `/_validate`.
     * The service waits for it: the completion event ends the wait as soon as it lands, and the
     * job is also read every second in case the event is missed. The `form` part always names the
     * language (leaving it out turns on multilingual mode) and the Drive's site.
     */
    describe('preview', () => {
        const finishedJob: DotImportJob = {
            id: 'preview-1',
            state: 'SUCCESS',
            result: { data: { summary: { created: 1, updated: 0, failed: 0 } } }
        };

        const startPreview = () => {
            let job: DotImportJob | undefined;
            spectator.service.preview(CSV, FORM).subscribe((value) => (job = value));
            const req = spectator.expectOne(`${IMPORT_URL}/_validate`, HttpMethod.POST);

            return { req, received: () => job };
        };

        afterEach(() => vi.useRealTimers());

        it('queues the CSV and the form through /_validate', async () => {
            const { req } = startPreview();

            const body = req.request.body as FormData;
            expect(body.get('file')).toBe(CSV);
            const form = JSON.parse(await (body.get('form') as Blob).text());
            expect(form).toEqual(FORM);
            expect(form.language).toBe('1');
            expect(form.site).toBe('demo.dotcms.com');
        });

        it('returns the finished job as soon as its completion arrives', () => {
            const { req, received } = startPreview();
            req.flush({ entity: { jobId: 'preview-1' } });

            completions.next({ jobId: 'other-job', state: 'SUCCESS', cmd: 'preview' });
            spectator.controller.expectNone(`${IMPORT_URL}/other-job`);

            completions.next({ jobId: 'preview-1', state: 'SUCCESS', cmd: 'preview' });
            spectator.expectOne(`${IMPORT_URL}/preview-1`, HttpMethod.GET).flush({
                entity: finishedJob
            });

            expect(received()).toEqual(finishedJob);
        });

        it('reads the job every second when no completion arrives', () => {
            vi.useFakeTimers();
            const { req, received } = startPreview();
            req.flush({ entity: { jobId: 'preview-1' } });

            vi.advanceTimersByTime(1000);
            spectator.expectOne(`${IMPORT_URL}/preview-1`, HttpMethod.GET).flush({
                entity: { id: 'preview-1', state: 'RUNNING' }
            });
            expect(received()).toBeUndefined();

            vi.advanceTimersByTime(1000);
            spectator.expectOne(`${IMPORT_URL}/preview-1`, HttpMethod.GET).flush({
                entity: finishedJob
            });

            expect(received()).toEqual(finishedJob);
        });
    });

    /** #37896 US1: the import itself runs as a background job. */
    describe('importContent', () => {
        it('posts the CSV and the form to the import endpoint', async () => {
            let jobId: string | undefined;
            spectator.service
                .importContent(CSV, FORM)
                .subscribe((created) => (jobId = created.jobId));

            const req = spectator.expectOne(IMPORT_URL, HttpMethod.POST);
            const body = req.request.body as FormData;
            expect(body.get('file')).toBe(CSV);
            expect(JSON.parse(await (body.get('form') as Blob).text())).toEqual(FORM);

            req.flush({ entity: { jobId: 'job-2' } });
            expect(jobId).toBe('job-2');
        });
    });

    /**
     * PR #38054 review: the import reuses the file the preview already uploaded, by its temp file
     * id, so the CSV goes up once and the import reads exactly what was previewed.
     */
    describe('importPreviewed', () => {
        it("posts the preview's temp file id and the form, without the CSV", async () => {
            let jobId: string | undefined;
            spectator.service
                .importPreviewed('temp_123', FORM)
                .subscribe((created) => (jobId = created.jobId));

            const req = spectator.expectOne(IMPORT_URL, HttpMethod.POST);
            const body = req.request.body as FormData;
            expect(body.get('file')).toBeNull();
            expect(body.get('tempFileId')).toBe('temp_123');
            expect(JSON.parse(await (body.get('form') as Blob).text())).toEqual(FORM);

            req.flush({ entity: { jobId: 'job-3' } });
            expect(jobId).toBe('job-3');
        });
    });

    /**
     * #37896 FR-019: a running import is restored after a reload from the server's job queue, like
     * uploads are. Previews belong to an open dialog and are never restored.
     */
    describe('readActiveImports', () => {
        it('lists running imports, not previews, named by file or else by content type', () => {
            let runs: unknown;
            spectator.service.readActiveImports().subscribe((value) => (runs = value));

            spectator
                .expectOne('/api/v1/jobs/importContentlets/active?pageSize=100', HttpMethod.GET)
                .flush({
                    entity: {
                        jobs: [
                            {
                                id: 'import-1',
                                state: 'RUNNING',
                                parameters: {
                                    cmd: 'publish',
                                    userId: 'me',
                                    fileName: 'blogs.csv',
                                    contentType: 'Blog'
                                }
                            },
                            {
                                id: 'import-2',
                                state: 'PENDING',
                                parameters: { cmd: 'publish', userId: 'me', contentType: 'News' }
                            },
                            {
                                id: 'preview-1',
                                state: 'RUNNING',
                                parameters: { cmd: 'preview', userId: 'me', fileName: 'x.csv' }
                            }
                        ]
                    }
                });

            expect(runs).toEqual([
                { id: 'import-1', userId: 'me', fileName: 'blogs.csv' },
                { id: 'import-2', userId: 'me', fileName: 'News' }
            ]);
        });
    });

    /** #37896 US4: the sample CSV Content Search offers, for the picked content type. */
    describe('downloadSample', () => {
        it('asks for the sample of the content type as a file', () => {
            let blob: Blob | undefined;
            spectator.service.downloadSample('Blog').subscribe((value) => (blob = value));

            const req = spectator.expectOne(
                `${IMPORT_URL}/_sample?contentType=Blog`,
                HttpMethod.GET
            );
            expect(req.request.responseType).toBe('blob');
            req.flush(new Blob(['title\n"XXX"'], { type: 'text/csv' }));

            expect(blob).toBeInstanceOf(Blob);
        });
    });
});
