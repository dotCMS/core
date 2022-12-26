import { getTestBed, TestBed } from '@angular/core/testing';
import { CoreWebService } from '@dotcms/dotcms-js';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CoreWebServiceMock } from '@dotcms/utils-testing';
import { DotSiteBrowserService } from './dot-site-browser.service';

describe('DotSiteBrowserService', () => {
    let injector: TestBed;
    let httpMock: HttpTestingController;
    let dotSiteBrowserService: DotSiteBrowserService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotSiteBrowserService
            ]
        });
        injector = getTestBed();
        dotSiteBrowserService = injector.inject(DotSiteBrowserService);
        httpMock = injector.inject(HttpTestingController);
    });

    it('should set Site Browser Selected folder', () => {
        dotSiteBrowserService.setSelectedFolder('/test').subscribe();

        const req = httpMock.expectOne('/api/v1/browser/selectedfolder');
        expect(req.request.method).toEqual('PUT');
        expect(req.request.body).toEqual({ path: '/test' });

        req.flush({});
    });
});
