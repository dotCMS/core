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
    });

    describe('getBundleAssets', () => {
        it('hits /api/bundle/{bundleId}/assets with limit=-1', () => {
            const mockAssets: BundleAssetView[] = [
                { id: 'a1', title: 'Asset 1', type: 'contentlet' }
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
});
