import { getTestBed, TestBed } from '@angular/core/testing';
import { CoreWebService } from '@dotcms/dotcms-js';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';

import { DotSiteBrowserService } from '@services/dot-site-browser/dot-site-browser.service';

describe('DotSiteBrowserService', () => {
    let injector: TestBed;
    let coreWebService: CoreWebService;
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
        dotSiteBrowserService = injector.get(DotSiteBrowserService);
        coreWebService = injector.get(CoreWebService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should set Site Browser Selected folder', () => {
        dotSiteBrowserService.setSelectedFolder('/test').subscribe(() => {});

        const req = httpMock.expectOne('/api/v1/browser/selectedfolder');
        expect(req.request.method).toEqual('PUT');
        expect(req.request.body).toEqual({ path: '/test' });

        req.flush({});
    });
});
