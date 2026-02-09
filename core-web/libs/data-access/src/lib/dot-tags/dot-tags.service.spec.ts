import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotTagsService } from './dot-tags.service';

describe('DotTagsService', () => {
    let dotTagsService: DotTagsService;
    let httpMock: HttpTestingController;

    const mockResponse = {
        test: { label: 'test', siteId: '1', siteName: 'Site', persona: false },
        united: { label: 'united', siteId: '1', siteName: 'Site', persona: false }
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [{ provide: CoreWebService, useClass: CoreWebServiceMock }, DotTagsService]
        });
        dotTagsService = TestBed.inject(DotTagsService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should get Tags', () => {
        dotTagsService.getSuggestions().subscribe((res) => {
            expect(res).toEqual([mockResponse.test, mockResponse.united]);
        });

        const req = httpMock.expectOne('v1/tags');
        expect(req.request.method).toBe('GET');
        req.flush(mockResponse);
    });

    it('should get Tags filtered by name ', () => {
        dotTagsService.getSuggestions('test').subscribe((res) => {
            expect(res).toEqual([mockResponse.test, mockResponse.united]);
        });

        const req = httpMock.expectOne('v1/tags?name=test');
        expect(req.request.method).toBe('GET');
        req.flush(mockResponse);
    });

    afterEach(() => {
        httpMock.verify();
    });
});
