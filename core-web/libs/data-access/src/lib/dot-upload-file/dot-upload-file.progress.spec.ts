import { mockProvider } from '@openng/spectator/jest';

import { XhrFactory } from '@angular/common';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

import { DotBulkUploadEvent, DotBulkUploadForm } from '@dotcms/dotcms-models';

import { DotUploadFileService } from './dot-upload-file.service';

import { DotWorkflowActionsFireService } from '../dot-workflow-actions-fire/dot-workflow-actions-fire.service';

/**
 * That a batch actually reports bytes sent, through the whole chain rather than a mock of it.
 *
 * The other spec in this folder uses `HttpTestingController`, which is transport-agnostic: it
 * verifies the request and lets a test flush whatever events it likes, so it passed happily while
 * the running app reported no progress at all. What it cannot see is the backend, and the backend
 * was the bug — Angular 22 defaults to `FetchBackend`, which has no `xhr.upload` and therefore no
 * upload progress, so `reportProgress: true` reported nothing on the way up.
 *
 * So this spec wires the real `HttpXhrBackend` via `withXhr()` over a fake `XMLHttpRequest` and
 * drives the events a browser would fire. Everything between the service and the transport is the
 * production article: if Angular stops forwarding upload progress, or the service stops asking for
 * it, or someone drops `withXhr` from the app, this fails.
 */
describe('DotUploadFileService — upload progress', () => {
    /** The parts of `XMLHttpRequest` Angular's XHR backend actually touches. */
    class FakeXhr {
        static last: FakeXhr;

        readonly upload = new EventTarget();
        readonly #listeners = new Map<string, ((event: Event) => void)[]>();

        status = 200;
        statusText = 'OK';
        responseType = '';
        responseText = '';
        response: unknown = '';
        withCredentials = false;

        constructor() {
            FakeXhr.last = this;
        }

        open(): void {
            /* nothing to record: the other spec already asserts the URL and method */
        }
        setRequestHeader(): void {
            /* headers are not what this spec is about */
        }
        getAllResponseHeaders(): string {
            return 'content-type: application/json';
        }
        getResponseHeader(): string {
            return 'application/json';
        }
        abort(): void {
            /* teardown path, unused here */
        }

        addEventListener(type: string, listener: (event: Event) => void): void {
            this.#listeners.set(type, [...(this.#listeners.get(type) ?? []), listener]);
        }
        removeEventListener(): void {
            /* nothing to do */
        }

        send(): void {
            /* the test drives the lifecycle explicitly, so sending does nothing */
        }

        /** Fires what a browser fires while the body goes out. */
        emitUploadProgress(loaded: number, total: number): void {
            const event = new ProgressEvent('progress', {
                lengthComputable: true,
                loaded,
                total
            });
            this.upload.dispatchEvent(event);
        }

        /** Completes the request with a JSON body. */
        emitLoad(body: unknown): void {
            this.responseText = JSON.stringify(body);
            this.response = this.responseText;
            (this.#listeners.get('load') ?? []).forEach((listener) => listener(new Event('load')));
        }
    }

    const form: DotBulkUploadForm = { baseType: 'DOTASSET', folderId: 'folder-1' };
    const file = (name: string, size: number) =>
        new File([new Uint8Array(size)], name, { type: 'image/png' });

    let service: DotUploadFileService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(withXhr()),
                DotUploadFileService,
                mockProvider(DotWorkflowActionsFireService),
                {
                    provide: XhrFactory,
                    useValue: { build: () => new FakeXhr() as unknown as XMLHttpRequest }
                }
            ]
        });

        service = TestBed.inject(DotUploadFileService);
    });

    it('should report bytes sent while the batch uploads', () => {
        const events: DotBulkUploadEvent[] = [];

        service
            .uploadFilesByBaseType([file('a.png', 4), file('b.png', 6)], form)
            .subscribe((event) => events.push(event));

        FakeXhr.last.emitUploadProgress(2_500_000, 10_000_000);
        FakeXhr.last.emitUploadProgress(7_500_000, 10_000_000);

        expect(events).toEqual([
            { kind: 'progress', loaded: 2_500_000, total: 10_000_000 },
            { kind: 'progress', loaded: 7_500_000, total: 10_000_000 }
        ]);
    });

    it('should hand over the accepted run once the body is through', () => {
        const events: DotBulkUploadEvent[] = [];

        service
            .uploadFilesByBaseType([file('a.png', 4)], form)
            .subscribe((event) => events.push(event));

        FakeXhr.last.emitUploadProgress(5_000, 10_000);
        FakeXhr.last.emitLoad({
            entity: { jobId: 'job-1', statusUrl: '/api/v1/jobs/job-1/status' }
        });

        expect(events).toEqual([
            { kind: 'progress', loaded: 5_000, total: 10_000 },
            {
                kind: 'accepted',
                handle: { jobId: 'job-1', statusUrl: '/api/v1/jobs/job-1/status' }
            }
        ]);
    });
});
