import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import {
    DotPushPublishFiltersService,
    DotPushPublishFilter
} from './dot-push-publish-filters.service';

describe('DotPushPublishFiltersService', () => {
    let service: DotPushPublishFiltersService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                DotPushPublishFiltersService
            ]
        });
        service = TestBed.inject(DotPushPublishFiltersService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should get push publish filters', () => {
        const mockFilters: DotPushPublishFilter[] = [
            {
                defaultFilter: true,
                key: 'test-key',
                title: 'Test Filter'
            }
        ];

        service.get().subscribe((filters: DotPushPublishFilter[]) => {
            expect(filters).toEqual(mockFilters);
        });

        const req = httpMock.expectOne('/api/v1/pushpublish/filters/');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: mockFilters });
    });
});
