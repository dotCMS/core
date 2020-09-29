import { DotLanguagesService } from './dot-languages.service';
import { mockDotLanguage } from '../../../test/dot-language.mock';
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';

describe('DotLanguagesService', () => {
    let injector: TestBed;
    let dotLanguagesService: DotLanguagesService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotLanguagesService
            ]
        });
        injector = getTestBed();
        dotLanguagesService = injector.get(DotLanguagesService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should get Languages', () => {
        dotLanguagesService.get().subscribe((res) => {
            expect(res).toEqual([mockDotLanguage]);
        });

        const req = httpMock.expectOne('v2/languages');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: [mockDotLanguage] });
    });

    it('should get Languages by content indode', () => {
        dotLanguagesService.get('2').subscribe((res) => {
            expect(res).toEqual([mockDotLanguage]);
        });

        const req = httpMock.expectOne('v2/languages?contentInode=2');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: [mockDotLanguage] });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
