import { DotThemesService } from './dot-themes.service';
import { mockDotThemes } from '../../../test/dot-themes.mock';
import { DotTheme } from '../../../portlets/dot-edit-page/shared/models/dot-theme.model';
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';

describe('DotThemesService', () => {
    let injector: TestBed;
    let dotThemesService: DotThemesService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotThemesService
            ]
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
