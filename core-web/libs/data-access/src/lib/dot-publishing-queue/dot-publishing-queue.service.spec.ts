import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import {
    BundleAssetView,
    IN_PROGRESS_STATUSES,
    PublishAuditStatus,
    PublishingJobsResponse,
    READY_STATUSES
} from '@dotcms/dotcms-models';

import { DotPublishingQueueService } from './dot-publishing-queue.service';

describe('DotPublishingQueueService', () => {
    let service: DotPublishingQueueService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), DotPublishingQueueService]
        });
        service = TestBed.inject(DotPublishingQueueService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    describe('listPublishingJobs', () => {
        it('joins statuses with comma and forwards pagination params', () => {
            const mockResponse: PublishingJobsResponse = {
                entity: [],
                pagination: { currentPage: 1, perPage: 10, totalEntries: 0 }
            };

            service
                .listPublishingJobs({
                    statuses: READY_STATUSES,
                    page: 2,
                    perPage: 25,
                    filter: 'bundle-x'
                })
                .subscribe((response) => {
                    expect(response).toEqual(mockResponse);
                });

            const req = httpMock.expectOne((request) => request.url === '/api/v1/publishing');
            expect(req.request.method).toBe('GET');
            expect(req.request.params.get('status')).toBe(
                `${PublishAuditStatus.WAITING_FOR_PUBLISHING},${PublishAuditStatus.BUNDLE_REQUESTED}`
            );
            expect(req.request.params.get('page')).toBe('2');
            expect(req.request.params.get('per_page')).toBe('25');
            expect(req.request.params.get('filter')).toBe('bundle-x');
            req.flush(mockResponse);
        });

        it('omits optional params when not provided', () => {
            service.listPublishingJobs({ statuses: IN_PROGRESS_STATUSES }).subscribe();

            const req = httpMock.expectOne((request) => request.url === '/api/v1/publishing');
            expect(req.request.params.has('page')).toBe(false);
            expect(req.request.params.has('per_page')).toBe(false);
            expect(req.request.params.has('filter')).toBe(false);
            req.flush({ entity: [], pagination: { currentPage: 1, perPage: 50, totalEntries: 0 } });
        });

        it('omits filter when empty string', () => {
            service.listPublishingJobs({ statuses: READY_STATUSES, filter: '' }).subscribe();

            const req = httpMock.expectOne((request) => request.url === '/api/v1/publishing');
            expect(req.request.params.has('filter')).toBe(false);
            req.flush({ entity: [], pagination: { currentPage: 1, perPage: 50, totalEntries: 0 } });
        });

        it('omits the status param when statuses is undefined (BE returns all)', () => {
            service.listPublishingJobs({}).subscribe();

            const req = httpMock.expectOne((request) => request.url === '/api/v1/publishing');
            expect(req.request.params.has('status')).toBe(false);
            req.flush({ entity: [], pagination: { currentPage: 1, perPage: 50, totalEntries: 0 } });
        });

        it('omits the status param when statuses is an empty array', () => {
            service.listPublishingJobs({ statuses: [] }).subscribe();

            const req = httpMock.expectOne((request) => request.url === '/api/v1/publishing');
            expect(req.request.params.has('status')).toBe(false);
            req.flush({ entity: [], pagination: { currentPage: 1, perPage: 50, totalEntries: 0 } });
        });
    });

    describe('getBundleAssets', () => {
        it('hits /api/bundle/{bundleId}/assets with limit=-1', () => {
            const mockAssets: BundleAssetView[] = [
                { asset: 'a1', title: 'Asset 1', type: 'contentlet' }
            ];

            service.getBundleAssets('bundle-123').subscribe((assets) => {
                expect(assets).toEqual(mockAssets);
            });

            const req = httpMock.expectOne(
                (request) => request.url === '/api/bundle/bundle-123/assets'
            );
            expect(req.request.method).toBe('GET');
            expect(req.request.params.get('limit')).toBe('-1');
            req.flush(mockAssets);
        });
    });

    describe('removeAssetsFromBundle', () => {
        it('DELETEs /api/v1/bundles/{bundleId}/assets with assetIds body and unwraps entity', () => {
            const results = [
                { assetId: 'a1', success: true, message: 'ok' },
                { assetId: 'a2', success: true, message: 'ok' }
            ];

            service.removeAssetsFromBundle('bundle-123', ['a1', 'a2']).subscribe((response) => {
                expect(response).toEqual(results);
            });

            const req = httpMock.expectOne(
                (request) => request.url === '/api/v1/bundles/bundle-123/assets'
            );
            expect(req.request.method).toBe('DELETE');
            expect(req.request.body).toEqual({ assetIds: ['a1', 'a2'] });
            req.flush({ entity: results });
        });
    });

    describe('deleteBundles', () => {
        it('DELETEs /api/bundle/ids with { identifiers } body', () => {
            service.deleteBundles(['b1', 'b2', 'b3']).subscribe();

            const req = httpMock.expectOne((r) => r.url === '/api/bundle/ids');
            expect(req.request.method).toBe('DELETE');
            expect(req.request.body).toEqual({ identifiers: ['b1', 'b2', 'b3'] });
            req.flush({ entity: 'Removing bundles in a separated process' });
        });
    });

    describe('purgeBundles', () => {
        it('DELETEs /api/v1/publishing/purge with no status param when statuses is omitted', () => {
            service.purgeBundles().subscribe();

            const req = httpMock.expectOne((r) => r.url === '/api/v1/publishing/purge');
            expect(req.request.method).toBe('DELETE');
            expect(req.request.params.has('status')).toBe(false);
            req.flush({ entity: { message: 'Purge started' } });
        });

        it('DELETEs /api/v1/publishing/purge with comma-joined status param when statuses are provided', () => {
            service
                .purgeBundles([
                    PublishAuditStatus.SUCCESS,
                    PublishAuditStatus.SUCCESS_WITH_WARNINGS
                ])
                .subscribe();

            const req = httpMock.expectOne((r) => r.url === '/api/v1/publishing/purge');
            expect(req.request.method).toBe('DELETE');
            expect(req.request.params.get('status')).toBe('SUCCESS,SUCCESS_WITH_WARNINGS');
            req.flush({ entity: { message: 'Purge started' } });
        });

        it('omits the status param when statuses is an empty array', () => {
            service.purgeBundles([]).subscribe();
            const req = httpMock.expectOne((r) => r.url === '/api/v1/publishing/purge');
            expect(req.request.params.has('status')).toBe(false);
            req.flush({ entity: { message: 'Purge started' } });
        });
    });

    describe('getUnsendBundles', () => {
        it('hits /api/bundle/getunsendbundles/userid/{userId} with name + start + count', () => {
            const mockResponse = {
                identifier: 'id',
                label: 'name',
                items: [{ id: 'b1', name: 'My Bundle' }],
                numRows: 1
            };

            service.getUnsendBundles('dotcms.org.1', '*term*', 0, 50).subscribe((response) => {
                expect(response).toEqual(mockResponse);
            });

            const req = httpMock.expectOne(
                (request) => request.url === '/api/bundle/getunsendbundles/userid/dotcms.org.1'
            );
            expect(req.request.method).toBe('GET');
            expect(req.request.params.get('name')).toBe('*term*');
            expect(req.request.params.get('start')).toBe('0');
            expect(req.request.params.get('count')).toBe('50');
            req.flush(mockResponse);
        });

        it('falls back to wildcard when filter is empty', () => {
            service.getUnsendBundles('u1', '').subscribe();
            const req = httpMock.expectOne(
                (request) => request.url === '/api/bundle/getunsendbundles/userid/u1'
            );
            expect(req.request.params.get('name')).toBe('*');
            req.flush({ identifier: 'id', label: 'name', items: [], numRows: 0 });
        });
    });

    describe('download URL builders', () => {
        it('getBundleDownloadUrl returns the /api/bundle/_download path', () => {
            expect(service.getBundleDownloadUrl('b-1')).toBe('/api/bundle/_download/b-1');
        });

        it('getBundleManifestUrl returns the /api/bundle/{id}/manifest path', () => {
            expect(service.getBundleManifestUrl('b-1')).toBe('/api/bundle/b-1/manifest');
        });

        it('URL-encodes special characters in the bundleId (defense-in-depth)', () => {
            // BE currently only issues bundle ids matching [a-zA-Z0-9_-]+, but the
            // encoding keeps the path safe if that ever changes.
            expect(service.getBundleDownloadUrl('a/b c')).toBe('/api/bundle/_download/a%2Fb%20c');
            expect(service.getBundleManifestUrl('a/b c')).toBe('/api/bundle/a%2Fb%20c/manifest');
        });
    });

    // These probes mirror the legacy JSP's file-existence checks because the
    // current detail response does not expose `hasBundle` / `hasManifest`. See
    // the service-level docs on `probeBundleDownload` for the full why.
    describe('probeBundleDownload', () => {
        it('issues a HEAD against /api/bundle/_download/{id} and maps 200 → true', () => {
            let result: boolean | undefined;
            service.probeBundleDownload('b-1').subscribe((value) => (result = value));

            const req = httpMock.expectOne('/api/bundle/_download/b-1');
            expect(req.request.method).toBe('HEAD');
            req.flush(null, { status: 200, statusText: 'OK' });

            expect(result).toBe(true);
        });

        it('maps non-2xx (file purged) to false via catchError', () => {
            let result: boolean | undefined;
            service.probeBundleDownload('b-1').subscribe((value) => (result = value));

            const req = httpMock.expectOne('/api/bundle/_download/b-1');
            req.flush(null, { status: 404, statusText: 'Not Found' });

            expect(result).toBe(false);
        });
    });

    describe('probeBundleManifest', () => {
        it('issues a HEAD against /api/bundle/{id}/manifest and maps 200 → true', () => {
            let result: boolean | undefined;
            service.probeBundleManifest('b-1').subscribe((value) => (result = value));

            const req = httpMock.expectOne('/api/bundle/b-1/manifest');
            expect(req.request.method).toBe('HEAD');
            req.flush(null, { status: 200, statusText: 'OK' });

            expect(result).toBe(true);
        });

        it('maps non-2xx (no manifest / archive missing) to false', () => {
            let result: boolean | undefined;
            service.probeBundleManifest('b-1').subscribe((value) => (result = value));

            const req = httpMock.expectOne('/api/bundle/b-1/manifest');
            req.flush(null, { status: 404, statusText: 'Not Found' });

            expect(result).toBe(false);
        });
    });

    // Mirrors the legacy DotDownloadBundleDialogComponent payload byte-for-byte
    // so users get the same .tar.gz from the inline menu and the global modal.
    describe('generateBundle', () => {
        it('POSTs { bundleId, operation, filterKey } to /api/bundle/_generate', () => {
            service.generateBundle('b-1', '0', 'ForcePush.yml').subscribe();

            const req = httpMock.expectOne('/api/bundle/_generate');
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toEqual({
                bundleId: 'b-1',
                operation: '0',
                filterKey: 'ForcePush.yml'
            });
            req.flush(new Blob(['x']), {
                status: 200,
                statusText: 'OK',
                headers: { 'content-disposition': 'attachment; filename=b-1.tar.gz' }
            });
        });

        it('emits { blob, filename } parsed from content-disposition', (done) => {
            service.generateBundle('b-1', '0', 'ForcePush.yml').subscribe(({ blob, filename }) => {
                expect(blob).toBeInstanceOf(Blob);
                expect(filename).toBe('b-1.tar.gz');
                done();
            });

            const req = httpMock.expectOne('/api/bundle/_generate');
            req.flush(new Blob(['x']), {
                status: 200,
                statusText: 'OK',
                headers: { 'content-disposition': 'attachment; filename=b-1.tar.gz' }
            });
        });

        it('strips surrounding quotes from the filename', (done) => {
            service.generateBundle('b-1', '0', 'ForcePush.yml').subscribe(({ filename }) => {
                expect(filename).toBe('b-1.tar.gz');
                done();
            });

            const req = httpMock.expectOne('/api/bundle/_generate');
            req.flush(new Blob(['x']), {
                status: 200,
                statusText: 'OK',
                headers: { 'content-disposition': 'attachment; filename="b-1.tar.gz"' }
            });
        });

        it('returns an empty filename when content-disposition is missing (still emits the blob)', (done) => {
            service.generateBundle('b-1', '1', '').subscribe(({ blob, filename }) => {
                expect(blob).toBeInstanceOf(Blob);
                expect(filename).toBe('');
                done();
            });

            const req = httpMock.expectOne('/api/bundle/_generate');
            req.flush(new Blob(['x']), { status: 200, statusText: 'OK' });
        });

        it('passes empty filterKey for unpublish operation', () => {
            service.generateBundle('b-1', '1', '').subscribe();

            const req = httpMock.expectOne('/api/bundle/_generate');
            expect(req.request.body).toEqual({
                bundleId: 'b-1',
                operation: '1',
                filterKey: ''
            });
            req.flush(new Blob(['x']), { status: 200, statusText: 'OK' });
        });
    });
});
