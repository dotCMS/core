import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, getTestBed } from '@angular/core/testing';

import { DotESContentService, ESOrderDirection } from './dot-es-content.service';

describe('DotESContentService', () => {
    let injector: TestBed;
    let dotESContentService: DotESContentService;
    let httpMock: HttpTestingController;

    const responseData = {
        contentTook: 0,
        jsonObjectView: { contentlets: [] },
        queryTook: 1,
        resultsSize: 2
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), DotESContentService]
        });
        injector = getTestBed();
        dotESContentService = injector.inject(DotESContentService);
        httpMock = injector.inject(HttpTestingController);
    });

    it('should get Blogs with default values', () => {
        dotESContentService.get({ query: '+contentType: blog' }).subscribe((res) => {
            expect(res).toEqual(responseData);
        });

        const req = httpMock.expectOne('/api/content/_search');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(
            '{"query":"+contentType: blog   ","sort":"modDate DESC","limit":40,"offset":"0"}'
        );
        req.flush({ entity: responseData });
    });

    it('should get Blogs with custom values', () => {
        dotESContentService
            .get({
                itemsPerPage: 5,
                filter: 'test',
                lang: '2',
                offset: '10',
                sortField: 'name',
                sortOrder: ESOrderDirection.ASC,
                query: '+contentType: blog'
            })
            .subscribe((res) => {
                expect(res).toEqual(responseData);
            });

        const req = httpMock.expectOne('/api/content/_search');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(
            '{"query":"+contentType: blog   +languageId : 2   +title : test*  ","sort":"name ASC","limit":5,"offset":"10"}'
        );
        req.flush({ entity: responseData });
    });

    it('should get Blogs with filter values and wrap into single quote if it is contain space', () => {
        dotESContentService
            .get({
                filter: 'test one',
                query: '+contentType: blog'
            })
            .subscribe((res) => {
                expect(res).toEqual(responseData);
            });

        const req = httpMock.expectOne('/api/content/_search');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(
            JSON.stringify({
                query: "+contentType: blog   +title : 'test one'*  ",
                sort: 'modDate DESC',
                limit: 40,
                offset: '0'
            })
        );
        req.flush({ entity: responseData });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
