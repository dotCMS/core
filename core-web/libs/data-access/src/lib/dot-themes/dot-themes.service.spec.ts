import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotTheme } from '@dotcms/dotcms-models';

import { DotThemesService } from './dot-themes.service';

describe('DotThemesService', () => {
    let service: DotThemesService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), DotThemesService]
        });
        service = TestBed.inject(DotThemesService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should get theme by inode', () => {
        const mockTheme: DotTheme = {
            inode: 'test-inode',
            name: 'Test Theme',
            identifier: 'test-id',
            hostId: 'test-host'
        };

        service.get('test-inode').subscribe((theme: DotTheme) => {
            expect(theme).toEqual(mockTheme);
        });

        const req = httpMock.expectOne('/api/v1/themes/id/test-inode');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: mockTheme });
    });
});
