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

    describe('getThemes', () => {
        const mockThemesResponse = {
            entity: [
                {
                    identifier: '5b347ae0d847b6d0fc7215bf329690d4',
                    inode: '5b347ae0d847b6d0fc7215bf329690d4',
                    path: '/application/themes/test-1/',
                    title: 'Test 1',
                    themeThumbnail: null,
                    name: 'test-1',
                    hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d'
                },
                {
                    identifier: '6c458bf1e958c7e1gd8326cg430801e5',
                    inode: '6c458bf1e958c7e1gd8326cg430801e5',
                    path: '/application/themes/test-2/',
                    title: 'Test 2',
                    themeThumbnail: null,
                    name: 'test-2',
                    hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d'
                }
            ],
            pagination: {
                currentPage: 1,
                perPage: 10,
                totalEntries: 502
            },
            errors: [],
            i18nMessagesMap: {},
            messages: [],
            permissions: []
        };

        it('should get themes with default parameters', () => {
            dotThemesService.getThemes().subscribe((result) => {
                expect(result.themes).toEqual(mockThemesResponse.entity);
                expect(result.pagination).toEqual(mockThemesResponse.pagination);
            });

            const req = httpMock.expectOne((request) => {
                return (
                    request.url === '/api/v1/themes' &&
                    request.method === 'GET' &&
                    request.params.get('hostId') === '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d' &&
                    request.params.get('page') === '1' &&
                    request.params.get('per_page') === '10' &&
                    request.params.get('direction') === 'ASC'
                );
            });

            req.flush(mockThemesResponse);
        });

        it('should get themes with custom pagination parameters', () => {
            dotThemesService
                .getThemes({
                    page: 2,
                    per_page: 20,
                    direction: 'DESC'
                })
                .subscribe((result) => {
                    expect(result.themes).toEqual(mockThemesResponse.entity);
                    expect(result.pagination).toEqual(mockThemesResponse.pagination);
                });

            const req = httpMock.expectOne((request) => {
                return (
                    request.url === '/api/v1/themes' &&
                    request.method === 'GET' &&
                    request.params.get('hostId') === '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d' &&
                    request.params.get('page') === '2' &&
                    request.params.get('per_page') === '20' &&
                    request.params.get('direction') === 'DESC'
                );
            });

            req.flush(mockThemesResponse);
        });

        it('should get themes with search parameter', () => {
            dotThemesService
                .getThemes({
                    searchParam: 'test'
                })
                .subscribe((result) => {
                    expect(result.themes).toEqual(mockThemesResponse.entity);
                    expect(result.pagination).toEqual(mockThemesResponse.pagination);
                });

            const req = httpMock.expectOne((request) => {
                return (
                    request.url === '/api/v1/themes' &&
                    request.method === 'GET' &&
                    request.params.get('hostId') === '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d' &&
                    request.params.get('page') === '1' &&
                    request.params.get('per_page') === '10' &&
                    request.params.get('direction') === 'ASC' &&
                    request.params.get('searchParam') === 'test'
                );
            });

            req.flush(mockThemesResponse);
        });

        it('should get themes with custom hostId', () => {
            const customHostId = 'custom-host-id';
            dotThemesService
                .getThemes({
                    hostId: customHostId
                })
                .subscribe((result) => {
                    expect(result.themes).toEqual(mockThemesResponse.entity);
                    expect(result.pagination).toEqual(mockThemesResponse.pagination);
                });

            const req = httpMock.expectOne((request) => {
                return (
                    request.url === '/api/v1/themes' &&
                    request.method === 'GET' &&
                    request.params.get('hostId') === customHostId
                );
            });

            req.flush(mockThemesResponse);
        });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
