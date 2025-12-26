import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

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
            providers: [provideHttpClient(), provideHttpClientTesting(), DotTagsService]
        });
        dotTagsService = TestBed.inject(DotTagsService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should get Tags', () => {
        dotTagsService.getSuggestions().subscribe((res) => {
            expect(res).toEqual([mockResponse.test, mockResponse.united]);
        });

        const req = httpMock.expectOne('/api/v1/tags');
        expect(req.request.method).toBe('GET');
        req.flush(mockResponse);
    });

    it('should get Tags filtered by name ', () => {
        dotTagsService.getSuggestions('test').subscribe((res) => {
            expect(res).toEqual([mockResponse.test, mockResponse.united]);
        });

        const req = httpMock.expectOne('/api/v1/tags?name=test');
        expect(req.request.method).toBe('GET');
        req.flush(mockResponse);
    });

    afterEach(() => {
        httpMock.verify();
    });
});
