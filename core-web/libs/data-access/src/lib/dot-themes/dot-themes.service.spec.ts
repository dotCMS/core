import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotTheme } from '@dotcms/dotcms-models';

import { DotThemesService } from './dot-themes.service';

describe('DotThemesService', () => {
    let dotThemesService: DotThemesService;
    let httpMock: HttpTestingController;

    const mockThemeEntity = {
        identifier: '5b347ae0d847b6d0fc7215bf329690d4',
        inode: '5b347ae0d847b6d0fc7215bf329690d4',
        path: '/application/themes/test-1/',
        title: 'Test 1',
        themeThumbnail: null,
        name: 'test-1',
        hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d'
    };

    const expectedTheme: DotTheme = {
        identifier: '5b347ae0d847b6d0fc7215bf329690d4',
        inode: '5b347ae0d847b6d0fc7215bf329690d4',
        path: '/application/themes/test-1/',
        title: 'Test 1',
        themeThumbnail: null,
        name: 'test-1',
        hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d'
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotThemesService, provideHttpClient(), provideHttpClientTesting()]
        });
        dotThemesService = TestBed.inject(DotThemesService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should get theme by id', () => {
        dotThemesService.get('5b347ae0d847b6d0fc7215bf329690d4').subscribe((theme: DotTheme) => {
            expect(theme).toEqual(expectedTheme);
        });

        const req = httpMock.expectOne(`/api/v1/themes/id/5b347ae0d847b6d0fc7215bf329690d4`);
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: mockThemeEntity,
            errors: [],
            i18nMessagesMap: {},
            messages: [],
            pagination: null,
            permissions: []
        });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
