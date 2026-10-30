import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { BehaviorSubject, EMPTY, of, Subject } from 'rxjs';
import { vi } from 'vitest';

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
import { IframeComponent } from '../iframe-component/iframe.component';

/**
 * How the legacy (JSP/Dojo) iframe host chooses its `src` (issue #38078).
 *
 * A legacy portlet url carries a timestamp (`r=<currentTimeMillis>`) that changes on every request, so
 * the menu snapshot cannot hold it: the host asks the store for the url when the portlet opens and
 * every time it is reloaded. The store reads it with a request of its own and never touches the
 * snapshot or the highlight; that is the store's contract and is tested there.
 */
describe('IframePortletLegacyComponent', () => {
    let spectator: Spectator<IframePortletLegacyComponent>;
    let portletReload$: Subject<string>;
    let params$: BehaviorSubject<{ id: string }>;
    let queryParams$: BehaviorSubject<{ url?: string }>;
    let parentUrl$: BehaviorSubject<{ path: string }[]>;
    let contentTypeService: DotContentTypeService;
    let globalStore: { loadFreshPortletUrl: ReturnType<typeof vi.fn> };

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
            mockProvider(DotContentTypeService, {
                getUrlById: vi.fn().mockReturnValue(of('/add/contentlet/url'))
            }),
            mockProvider(DotLoadingIndicatorService),
            mockProvider(DotCustomEventHandlerService),
            mockProvider(LoggerService),
            mockProvider(DotIframeService),
            mockProvider(DotEventsSocket, { on: () => EMPTY }),
            mockProvider(GlobalStore, {
                siteDetails: signal(null),
                loadFreshPortletUrl: vi.fn()
            })
        ]
    });

    // The mocks are created once per factory, so their call counts must not leak between tests.
    beforeEach(() => vi.clearAllMocks());

    /**
     * Opens the host on a route. `answers` are what the store returns, one per request for a url,
     * like the fresh `r=` timestamp a real request brings.
     */
    const open = (
        route: { id: string; parent?: string; url?: string; access?: boolean },
        answers: string[] = []
    ) => {
        params$ = new BehaviorSubject({ id: route.id });
        queryParams$ = new BehaviorSubject<{ url?: string }>(route.url ? { url: route.url } : {});
        parentUrl$ = new BehaviorSubject([{ path: route.parent ?? 'c' }]);
        portletReload$ = new Subject<string>();

        spectator = createComponent({
            detectChanges: false,
            providers: [
                {
                    provide: DotRouterService,
                    useValue: { portletReload$, isJSPPortlet: () => true }
                },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: of({ canAccessPortlet: route.access ?? true }),
                        params: params$,
                        queryParams: queryParams$,
                        parent: { url: parentUrl$ }
                    }
                }
            ]
        });
        contentTypeService = spectator.inject(DotContentTypeService);
        globalStore = spectator.inject(GlobalStore) as unknown as typeof globalStore;
        const pending = [...answers];
        globalStore.loadFreshPortletUrl.mockImplementation(() => of(pending.shift() ?? ''));
        spectator.detectChanges();
    };

    describe('opening a legacy portlet', () => {
        it('sets the iframe src from a url the store reads at open time', () => {
            open({ id: 'publishing-queue' }, ['/c/portal/layout?p_l_id=1&r=1000']);

            expect(globalStore.loadFreshPortletUrl).toHaveBeenCalledWith('publishing-queue');
            expect(globalStore.loadFreshPortletUrl).toHaveBeenCalledTimes(1);
            expect(spectator.component.url.getValue()).toBe('/c/portal/layout?p_l_id=1&r=1000');
        });

        it('asks again, and takes the new url, every time the portlet opens', () => {
            open({ id: 'publishing-queue' }, [
                '/c/portal/layout?p_l_id=1&r=1000',
                '/c/portal/layout?p_l_id=1&r=2000'
            ]);

            params$.next({ id: 'publishing-queue' });

            expect(globalStore.loadFreshPortletUrl).toHaveBeenCalledTimes(2);
            expect(spectator.component.url.getValue()).toBe('/c/portal/layout?p_l_id=1&r=2000');
        });

        describe('when the answers come back out of order', () => {
            /** Makes every url request wait until the test answers it. */
            const holdRequests = (): Subject<string>[] => {
                const requests: Subject<string>[] = [];
                globalStore.loadFreshPortletUrl.mockImplementation(() => {
                    const request = new Subject<string>();
                    requests.push(request);

                    return request;
                });

                return requests;
            };

            it('keeps the url of the portlet the user is on when the previous one answers late', () => {
                open({ id: 'portlet-a' }, ['/c/a']);
                const requests = holdRequests();

                params$.next({ id: 'portlet-a' });
                params$.next({ id: 'portlet-b' });
                requests[1].next('/c/b');
                requests[0].next('/c/a-late');

                expect(spectator.component.url.getValue()).toBe('/c/b');
            });

            it('takes no answer once the host is gone', () => {
                open({ id: 'portlet-a' }, ['/c/a']);
                const requests = holdRequests();
                params$.next({ id: 'portlet-b' });

                spectator.fixture.destroy();
                requests[0].next('/c/b-after-destroy');

                expect(spectator.component.url.getValue()).toBe('/c/a');
            });
        });

        it('reads a fresh url when the portlet is reloaded', () => {
            open({ id: 'publishing-queue' }, [
                '/c/portal/layout?p_l_id=1&r=1000',
                '/c/portal/layout?p_l_id=1&r=2000'
            ]);

            portletReload$.next('publishing-queue');

            expect(globalStore.loadFreshPortletUrl).toHaveBeenCalledTimes(2);
            expect(globalStore.loadFreshPortletUrl).toHaveBeenLastCalledWith('publishing-queue');
            expect(spectator.component.url.getValue()).toBe('/c/portal/layout?p_l_id=1&r=2000');
        });

        it('keeps the url it has when it is reloaded because the site changed', () => {
            open({ id: 'publishing-queue' }, ['/c/portal/layout?p_l_id=1&r=1000']);

            spectator.component.reloadIframePortlet();

            expect(globalStore.loadFreshPortletUrl).toHaveBeenCalledTimes(1);
            expect(spectator.component.url.getValue()).toBe('/c/portal/layout?p_l_id=1&r=1000');
        });
    });

    describe('routes that do not use the portlet url', () => {
        it('sets the src from the ?url= query param without asking the store', () => {
            open({ id: 'publishing-queue', url: '/html/edit?inode=77' });

            expect(globalStore.loadFreshPortletUrl).not.toHaveBeenCalled();
            expect(spectator.component.url.getValue()).toBe('/html/edit?inode=77');
        });

        it('asks the contentlet service, not the store, on the add route', () => {
            open({ id: 'blog', parent: 'add' });

            expect(globalStore.loadFreshPortletUrl).not.toHaveBeenCalled();
            expect(contentTypeService.getUrlById).toHaveBeenCalledWith('blog');
            expect(spectator.component.url.getValue()).toBe('/add/contentlet/url');
        });

        it('shows nothing and asks for nothing when the user cannot access the portlet', () => {
            open({ id: 'publishing-queue', access: false });

            expect(globalStore.loadFreshPortletUrl).not.toHaveBeenCalled();
            expect(spectator.component.canAccessPortlet).toBe(false);
            expect(spectator.component.url.getValue()).toBe('');
        });

        it('gets an empty url for an Angular portlet, which navigates with its menuLink', () => {
            open({ id: 'pages' }, ['']);

            expect(spectator.component.url.getValue()).toBe('');
        });
    });
});
