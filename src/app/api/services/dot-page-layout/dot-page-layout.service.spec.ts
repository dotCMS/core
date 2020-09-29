import { TestBed, getTestBed } from '@angular/core/testing';
import { DotPageLayoutService } from './dot-page-layout.service';
import { mockDotLayout } from '../../../test/dot-page-render.mock';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';

describe('DotPageLayoutService', () => {
    let injector: TestBed;
    let dotPageLayoutService: DotPageLayoutService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotPageLayoutService
            ]
        });
        injector = getTestBed();
        dotPageLayoutService = injector.get(DotPageLayoutService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should post data and return an entity', () => {
        const mockResponse = {
            entity: [
                Object.assign({}, mockDotLayout, {
                    iDate: 1495670226000,
                    identifier: '1234-id-7890-entifier',
                    modDate: 1495670226000
                })
            ]
        };

        dotPageLayoutService
            .save('test38923-82393842-23823', mockDotLayout)
            .subscribe((result: any) => {
                expect(result.params).toEqual(mockResponse.entity);
            });

        const req = httpMock.expectOne('v1/page/test38923-82393842-23823/layout');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toBe(mockDotLayout);
        req.flush(mockResponse);
    });

    afterEach(() => {
        httpMock.verify();
    });
});
