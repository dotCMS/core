/* eslint-disable @typescript-eslint/no-explicit-any */

import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed, getTestBed } from '@angular/core/testing';

import { DotSessionStorageService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock, mockDotLayout } from '@dotcms/utils-testing';

import { DotPageLayoutService } from './dot-page-layout.service';

describe('DotPageLayoutService', () => {
    let injector: TestBed;
    let dotPageLayoutService: DotPageLayoutService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotPageLayoutService,
                DotSessionStorageService
            ]
        });
        injector = getTestBed();
        dotPageLayoutService = injector.get(DotPageLayoutService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should post data and return an entity', () => {
        const mockResponse = {
            entity: [
                Object.assign({}, mockDotLayout(), {
                    iDate: 1495670226000,
                    identifier: '1234-id-7890-entifier',
                    modDate: 1495670226000
                })
            ]
        };

        dotPageLayoutService
            .save('test38923-82393842-23823', mockDotLayout())
            .subscribe((result: any) => {
                expect(result.params).toEqual(mockResponse.entity);
            });

        const req = httpMock.expectOne(
            'v1/page/test38923-82393842-23823/layout?variantName=DEFAULT'
        );
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(mockDotLayout());
        req.flush(mockResponse);
    });

    it('should post data and return an entity with variantName', () => {
        const mockResponse = {
            entity: [
                Object.assign({}, mockDotLayout(), {
                    iDate: 1495670226000,
                    identifier: '1234-id-7890-entifier',
                    modDate: 1495670226000
                })
            ]
        };

        sessionStorage.setItem('variantName', 'variantTesting');

        dotPageLayoutService
            .save('test38923-82393842-23823', mockDotLayout())
            .subscribe((result: any) => {
                expect(result.params).toEqual(mockResponse.entity);
            });

        const req = httpMock.expectOne(
            'v1/page/test38923-82393842-23823/layout?variantName=variantTesting'
        );
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(mockDotLayout());
        req.flush(mockResponse);
    });

    afterEach(() => {
        httpMock.verify();
    });
});
