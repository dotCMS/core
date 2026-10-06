import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotSiteBrowserService } from './dot-site-browser.service';

describe('DotSiteBrowserService', () => {
    let httpTesting: HttpTestingController;
    let dotSiteBrowserService: DotSiteBrowserService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), DotSiteBrowserService]
        });
        dotSiteBrowserService = TestBed.inject(DotSiteBrowserService);
        httpTesting = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpTesting.verify();
    });

    it('should set Site Browser Selected folder', () => {
        dotSiteBrowserService.setSelectedFolder('/test').subscribe();

        const req = httpTesting.expectOne('/api/v1/browser/selectedfolder');
        expect(req.request.method).toEqual('PUT');
        expect(req.request.body).toEqual({ path: '/test' });

        req.flush({});
    });
});
