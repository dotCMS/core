import { EnvironmentInjector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, UrlSegment } from '@angular/router';

import { DotCMSPageResolver } from './dotcms-page.resolver';

import { DOTCMS_CLIENT_TOKEN } from '../client-token/dotcms-client-token';
import { DotCMSPageAsset } from '../models';
import { PageContextService } from '../services/dotcms-context/page-context.service';
import { PageResponseMock, NavMock } from '../utils/testing.utils';

describe('DotcmsPageResolver', () => {
    const mockRouteWithoutQueryParams = {
        url: [new UrlSegment('', {})],
        queryParams: {}
    } as unknown as ActivatedRouteSnapshot;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                {
                    provide: DOTCMS_CLIENT_TOKEN,
                    useValue: {
                        page: {
                            get: () => Promise.resolve({ entity: PageResponseMock })
                        },
                        nav: {
                            get: () => Promise.resolve({ entity: NavMock })
                        }
                    }
                },
                {
                    provide: PageContextService,
                    useValue: {
                        setContext: jest.fn()
                    }
                }
            ]
        });
    });

    it('should call page and get without query params', () => {
        const client = TestBed.inject(DOTCMS_CLIENT_TOKEN);
        jest.spyOn(client.page, 'get');
        jest.spyOn(client.nav, 'get');

        runInInjectionContext(TestBed.inject(EnvironmentInjector), () =>
            DotCMSPageResolver(mockRouteWithoutQueryParams)
        );

        expect(client.page.get).toHaveBeenCalledWith({
            path: 'index',
            language_id: undefined,
            mode: undefined,
            // These options doesnt exist in PageAPIOptions, but are used in the example
            variantName: undefined,
            'com.dotmarketing.persona.id': ''
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        expect(client.nav.get).toHaveBeenCalledWith({
            path: '/',
            depth: 2,
            languageId: undefined
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);
    });

    it('should return pageAsset and nav', async () => {
        const data = runInInjectionContext(TestBed.inject(EnvironmentInjector), () =>
            DotCMSPageResolver(mockRouteWithoutQueryParams)
        );
        const result = await data;
        expect(result).toEqual({
            pageAsset: PageResponseMock as unknown as DotCMSPageAsset,
            nav: NavMock
        });
    });

    it('should call page and nav get  with query params', () => {
        const mockRouteWithQueryParams = {
            url: [new UrlSegment('blogs', {})],
            queryParams: {
                language_id: 10,
                mode: 'EDIT',
                variantName: 'experiment1',
                'com.dotmarketing.persona.id': 'persona1'
            }
        } as unknown as ActivatedRouteSnapshot;

        const client = TestBed.inject(DOTCMS_CLIENT_TOKEN);
        jest.spyOn(client.page, 'get');
        jest.spyOn(client.nav, 'get');

        runInInjectionContext(TestBed.inject(EnvironmentInjector), () =>
            DotCMSPageResolver(mockRouteWithQueryParams)
        );

        expect(client.page.get).toHaveBeenCalledWith({
            path: 'blogs',
            language_id: 10,
            mode: 'EDIT',
            variantName: 'experiment1',
            'com.dotmarketing.persona.id': 'persona1'
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        expect(client.nav.get).toHaveBeenCalledWith({
            path: '/',
            depth: 2,
            languageId: 10
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);
    });
});
