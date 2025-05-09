import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed, getTestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotTheme } from '@dotcms/dotcms-models';
import { mockDotThemes, CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotThemesService } from './dot-themes.service';

describe('DotThemesService', () => {
    let injector: TestBed;
    let dotThemesService: DotThemesService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [{ provide: CoreWebService, useClass: CoreWebServiceMock }, DotThemesService]
        });
        injector = getTestBed();
        dotThemesService = injector.get(DotThemesService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should get Themes', () => {
        dotThemesService.get('inode').subscribe((themes: DotTheme) => {
            expect(themes).toEqual(mockDotThemes[0]);
        });

        const req = httpMock.expectOne(`v1/themes/id/inode`);
        expect(req.request.method).toBe('GET');
        req.flush({ entity: mockDotThemes[0] });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
