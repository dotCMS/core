import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/vitest';

import { DotContentImportForm, DotImportContentType, DotImportJob } from '@dotcms/dotcms-models';

import { DotContentImportService } from './dot-content-import.service';

const IMPORT_URL = '/api/v1/content/_import';

describe('DotContentImportService', () => {
    let spectator: SpectatorHttp<DotContentImportService>;

    const createHttp = createHttpFactory(DotContentImportService);

    const FORM: DotContentImportForm = {
        contentType: 'Blog',
        language: '1',
        workflowActionId: 'publish',
        fields: ['title'],
        site: 'demo.dotcms.com'
    };
    const CSV = new File(['title\nHello'], 'blogs.csv', { type: 'text/csv' });

    beforeEach(() => {
        spectator = createHttp();
    });

    /**
     * #37896 FR-004: the import form lists only the content types the user can write to. The
     * server owns that rule and pages it, so the service only asks for a page and hands it to the
     * shared content type selector in the shape that selector reads.
     */
    describe('getWritableContentTypes', () => {
        it('asks the import endpoint for one page of a search and returns types and pagination', () => {
            const types: DotImportContentType[] = [
                { id: '1', variable: 'Blog', name: 'Blog', baseType: 'CONTENT', icon: 'article' },
                { id: '2', variable: 'News', name: 'News', baseType: 'CONTENT' }
            ];
            const pagination = { currentPage: 2, perPage: 40, totalEntries: 42 };
            let received: unknown;

            spectator.service
                .getWritableContentTypes({ page: 2, per_page: 40, filter: 'b' })
                .subscribe((value) => (received = value));

            const req = spectator.expectOne(
                `${IMPORT_URL}/contenttypes?page=2&per_page=40&filter=b`,
                HttpMethod.GET
            );
            req.flush({ entity: types, pagination });

            expect(received).toEqual({ contentTypes: types, pagination });
        });

        it('leaves out the search when there is none', () => {
            spectator.service.getWritableContentTypes({ page: 1, per_page: 40 }).subscribe();

            spectator.expectOne(`${IMPORT_URL}/contenttypes?page=1&per_page=40`, HttpMethod.GET);
        });
    });

    /**
     * #37896 FR-012: the preview is a dry run the server answers in the same request, as Content
     * Search's does — no job queue to wait on. The `form` part always names the language (leaving it
     * out turns on multilingual mode) and the Drive's site.
     */
    describe('preview', () => {
        it('posts the CSV and the form to the preview endpoint and returns the finished job', async () => {
            const finishedJob: DotImportJob = {
                id: 'preview-1',
                state: 'SUCCESS',
                result: { data: { summary: { created: 1, updated: 0, failed: 0 } } }
            };
            let job: DotImportJob | undefined;
            spectator.service.preview(CSV, FORM).subscribe((value) => (job = value));

            const req = spectator.expectOne(`${IMPORT_URL}/_preview`, HttpMethod.POST);
            const body = req.request.body as FormData;
            expect(body.get('file')).toBe(CSV);
            const form = JSON.parse(await (body.get('form') as Blob).text());
            expect(form).toEqual(FORM);
            expect(form.language).toBe('1');
            expect(form.site).toBe('demo.dotcms.com');

            req.flush({ entity: finishedJob });
            expect(job).toEqual(finishedJob);
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
