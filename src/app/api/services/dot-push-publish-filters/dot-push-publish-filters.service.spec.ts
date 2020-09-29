import {
    DotPushPublishFiltersService,
    DotPushPublishFilter
} from './dot-push-publish-filters.service';
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';

describe('DotPushPublishFiltersService', () => {
    let injector: TestBed;
    let dotPushPublishFiltersService: DotPushPublishFiltersService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotPushPublishFiltersService
            ]
        });
        injector = getTestBed();
        dotPushPublishFiltersService = injector.get(DotPushPublishFiltersService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should get hit pp filters url', () => {
        dotPushPublishFiltersService.get().subscribe();

        const req = httpMock.expectOne('/api/v1/pushpublish/filters/');
        expect(req.request.method).toBe('GET');
    });

    it('should return entity', () => {
        dotPushPublishFiltersService.get().subscribe((res: DotPushPublishFilter[]) => {
            expect(res).toEqual([
                {
                    defaultFilter: true,
                    key: 'some.yml',
                    title: 'Hello World'
                }
            ]);
        });

        const req = httpMock.expectOne('/api/v1/pushpublish/filters/');
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: [
                {
                    defaultFilter: true,
                    key: 'some.yml',
                    title: 'Hello World'
                }
            ]
        });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
