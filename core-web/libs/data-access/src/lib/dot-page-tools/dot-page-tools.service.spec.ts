import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed, getTestBed } from '@angular/core/testing';

import { mockPageTools } from '@dotcms/utils-testing';

import { DotPageToolsService } from './dot-page-tools.service';

describe('DotPageToolsService', () => {
    let injector: TestBed;
    let dotPageToolsService: DotPageToolsService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [DotPageToolsService]
        });
        injector = getTestBed();
        dotPageToolsService = injector.inject(DotPageToolsService);
        httpMock = injector.inject(HttpTestingController);
    });

    it('should get Page Tools', (done) => {
        const url = 'assets/seo/page-tools.json';

        dotPageToolsService.get().subscribe((result) => {
            expect(result).toEqual(mockPageTools.pageTools);
            done();
        });

        const req = httpMock.expectOne(url);
        expect(req.request.method).toBe('GET');

        req.flush(mockPageTools);
    });

    afterEach(() => {
        httpMock.verify();
    });
});
