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
            service
                .getThemes({ hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d' })
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
                    request.params.get('direction') === 'ASC'
                );
            });

            req.flush(mockThemesResponse);
        });

        it('should get themes with custom pagination parameters', () => {
            service
                .getThemes({
                    hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d',
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
            service
                .getThemes({
                    hostId: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d',
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
            service
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

    it('should get theme by inode', () => {
        const mockTheme: DotTheme = {
            inode: 'test-inode',
            name: 'Test Theme',
            identifier: 'test-id',
            title: 'Test Theme',
            themeThumbnail: '',
            path: '/application/themes/test/',
            hostId: 'test-host',
            host: {
                hostName: 'test-host',
                inode: 'test-inode',
                identifier: 'test-id'
            }
        };

        service.get('test-inode').subscribe((theme: DotTheme) => {
            expect(theme).toEqual(mockTheme);
        });

        const req = httpMock.expectOne('/api/v1/themes/id/test-inode');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: mockTheme });
    });
});
