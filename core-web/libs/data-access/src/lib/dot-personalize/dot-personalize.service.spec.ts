import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed, getTestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotPersonalizeService } from './dot-personalize.service';

import { DotSessionStorageService } from '../dot-session-storage/dot-session-storage.service';

describe('DotPersonalizeService', () => {
    let injector: TestBed;
    let dotPersonalizeService: DotPersonalizeService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotSessionStorageService,
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

        const req = httpMock.expectOne('/api/v1/personalization/pagepersonas?variantName=DEFAULT');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({ pageId: 'a', personaTag: 'b' });
    });

    it('should despersonalized', () => {
        const pageId = 'a';
        const personaTag = 'b';
        dotPersonalizeService.despersonalized(pageId, personaTag).subscribe();

        const req = httpMock.expectOne(
            `/api/v1/personalization/pagepersonas/page/${pageId}/personalization/${personaTag}?variantName=DEFAULT`
        );
        expect(req.request.method).toBe('DELETE');
    });

    afterEach(() => {
        httpMock.verify();
    });
});
