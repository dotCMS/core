import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { BehaviorSubject, EMPTY, of, Subject } from 'rxjs';
import { vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import {
    DotContentTypeService,
    DotEventsSocket,
    DotIframeService,
    DotRouterService
} from '@dotcms/data-access';
import { LoggerService } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';
import { DotNotLicenseComponent } from '@dotcms/ui';
import { DotLoadingIndicatorService } from '@dotcms/utils';

import { IframePortletLegacyComponent } from './iframe-porlet-legacy.component';

import { DotCustomEventHandlerService } from '../../../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { IframeComponent } from '../iframe-component/iframe.component';

/**
 * Characterization of how the legacy (JSP/Dojo) iframe host chooses its `src` (issue #38078).
 * Everything is observed at the HTTP boundary: the menu response a test flushes is the only source
 * of the portlet URL, so the same cases hold whether the URL comes from a service cache or from the
 * navigation store. They pin today's behavior and must keep passing through the consolidation.
 */
describe('IframePortletLegacyComponent', () => {
    const MENU_URL = '/api/v1/menu';

    const menuWith = (jspUrl: string) => ({
        entity: [
            {
                id: 'aaaa1111-0000-4000-8000-000000000001',
                name: 'Site',
                tabName: 'Site',
                tabIcon: 'icon',
                url: '/pages',
                menuItems: [
                    { id: 'pages', label: 'Pages', url: '/pages', angular: true, ajax: false },
                    {
                        id: 'publishing-queue',
                        label: 'Publishing Queue',
                        url: jspUrl,
                        angular: false,
                        ajax: false
                    }
                ]
            }
        ]
    });

    let spectator: Spectator<IframePortletLegacyComponent>;
    let httpMock: HttpTestingController;
    let portletReload$: Subject<string>;
    let params$: BehaviorSubject<{ id: string }>;
    let queryParams$: BehaviorSubject<{ url?: string }>;
    let parentUrl$: BehaviorSubject<{ path: string }[]>;
    let contentTypeService: DotContentTypeService;

    const createComponent = createComponentFactory({
        component: IframePortletLegacyComponent,
        detectChanges: false,
        overrideComponents: [
            [
                IframePortletLegacyComponent,
                {
                    remove: { imports: [IframeComponent, DotNotLicenseComponent] },
                    add: { schemas: [NO_ERRORS_SCHEMA] }
                }
            ]
        ],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            DotMenuService,
            mockProvider(DotContentTypeService, {
                getUrlById: vi.fn().mockReturnValue(of('/add/contentlet/url'))
            }),
            mockProvider(DotLoadingIndicatorService),
            mockProvider(DotCustomEventHandlerService),
            mockProvider(LoggerService),
            mockProvider(DotIframeService),
            mockProvider(DotEventsSocket, { on: () => EMPTY }),
            mockProvider(GlobalStore, { siteDetails: signal(null) })
        ]
    });

    const open = (route: { id: string; parent?: string; url?: string }) => {
        params$ = new BehaviorSubject({ id: route.id });
        queryParams$ = new BehaviorSubject<{ url?: string }>(route.url ? { url: route.url } : {});
        parentUrl$ = new BehaviorSubject([{ path: route.parent ?? 'c' }]);
        portletReload$ = new Subject<string>();

        spectator = createComponent({
            providers: [
                {
                    provide: DotRouterService,
                    useValue: { portletReload$, isJSPPortlet: () => true }
                },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: of({ canAccessPortlet: true }),
                        params: params$,
                        queryParams: queryParams$,
                        parent: { url: parentUrl$ }
                    }
                }
            ]
        });
        httpMock = spectator.inject(HttpTestingController);
        contentTypeService = spectator.inject(DotContentTypeService);
        spectator.detectChanges();
    };

    afterEach(() => {
        httpMock.verify();
    });

    it('sets the iframe src from the portlet url when a JSP portlet opens', () => {
        open({ id: 'publishing-queue' });

        httpMock.expectOne(MENU_URL).flush(menuWith('/c/portal/layout?p_l_id=1&r=1000'));

        expect(spectator.component.url.getValue()).toBe('/c/portal/layout?p_l_id=1&r=1000');
    });

    it('never takes the iframe url from an Angular item, which navigates with its menuLink', () => {
        open({ id: 'pages' });

        httpMock.expectOne(MENU_URL).flush(menuWith('/jsp?r=1'));

        expect(spectator.component.url.getValue()).toBe('');
    });

    it('sets the src from the ?url= query param without asking for the menu', () => {
        open({ id: 'publishing-queue', url: '/html/edit?inode=77' });

        httpMock.expectNone(MENU_URL);
        expect(spectator.component.url.getValue()).toBe('/html/edit?inode=77');
    });

    it('asks the contentlet service, not the menu, on the add route', () => {
        open({ id: 'blog', parent: 'add' });

        httpMock.expectNone(MENU_URL);
        expect(contentTypeService.getUrlById).toHaveBeenCalledWith('blog');
        expect(spectator.component.url.getValue()).toBe('/add/contentlet/url');
    });

    it('shows nothing and asks for nothing when the user cannot access the portlet', () => {
        const route = {
            data: of({ canAccessPortlet: false }),
            params: of({ id: 'publishing-queue' }),
            queryParams: of({}),
            parent: { url: of([{ path: 'c' }]) }
        };
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        spectator = createComponent({
            providers: [
                {
                    provide: DotRouterService,
                    useValue: { portletReload$: new Subject<string>(), isJSPPortlet: () => true }
                },
                { provide: ActivatedRoute, useValue: route }
            ]
        });
        httpMock = spectator.inject(HttpTestingController);
        spectator.detectChanges();

        httpMock.expectNone(MENU_URL);
        expect(spectator.component.canAccessPortlet).toBe(false);
        expect(spectator.component.url.getValue()).toBe('');
    });
});
