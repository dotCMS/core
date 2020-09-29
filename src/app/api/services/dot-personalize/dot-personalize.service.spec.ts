import { DotPersonalizeService } from './dot-personalize.service';
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';

describe('DotPersonalizeService', () => {
    let injector: TestBed;
    let dotPersonalizeService: DotPersonalizeService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotPersonalizeService
            ]
        });
        injector = getTestBed();
        dotPersonalizeService = injector.get(DotPersonalizeService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should set Personalized', () => {
        dotPersonalizeService.personalized('a', 'b').subscribe();

        const req = httpMock.expectOne('/api/v1/personalization/pagepersonas');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({ pageId: 'a', personaTag: 'b' });
    });

    it('should despersonalized', () => {
        const pageId = 'a';
        const personaTag = 'b';
        dotPersonalizeService.despersonalized(pageId, personaTag).subscribe();

        const req = httpMock.expectOne(
            `/api/v1/personalization/pagepersonas/page/${pageId}/personalization/${personaTag}`
        );
        expect(req.request.method).toBe('DELETE');
    });

    afterEach(() => {
        httpMock.verify();
    });
});
