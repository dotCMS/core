import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { BehaviorSubject, EMPTY, NEVER, of, Subject } from 'rxjs';
import { Mock, describe, expect, it, vi } from 'vitest';

import { NO_ERRORS_SCHEMA, signal } from '@angular/core';
import { ActivatedRoute, Navigation, Params, Router } from '@angular/router';

import {
    DotContentChangeService,
    DotContentTypeService,
    DotEventsSocket,
    DotIframeService,
    DotRouterService
} from '@dotcms/data-access';
import { LoggerService } from '@dotcms/dotcms-js';
// The service that owns the panel params; the app also lazy-loads the library's routes (#38303).
// eslint-disable-next-line @nx/enforce-module-boundaries
import { DotPanelUrlStateService } from '@dotcms/edit-content';
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

const IDENTIFIER = '3f0c8e5a-7b2d-4c1e-9a6f-0d4b8e2c1a57';
const FOLDER_INODE = '7c2e9b14-5a3d-4f6e-8b1c-2d9a0e4f6b38';

/**
 * A legacy (JSP) portlet keeps its open panel in its URL like every other surface, and opens what a
 * URL names: on load, after a deep link redirected here, or after any router navigation that
 * changes the params (#38303, FR-049). The panel params arrive on the router's `queryParams`;
 * `Location` writes, which is how the panel URL service writes them, never reach it.
 */
describe('IframePortletLegacyComponent — the panel in the URL', () => {
    let spectator: Spectator<IframePortletLegacyComponent>;
    let queryParams$: BehaviorSubject<Params>;
    let openContent: Mock;
    let clear: Mock;
    let refuse: Mock;

    const createComponent = createComponentFactory({
        component: IframePortletLegacyComponent,
        // The iframe and the license notice have specs of their own.
        overrideComponents: [
            [IframePortletLegacyComponent, { set: { imports: [], template: '' } }]
        ],
        detectChanges: false
    });

    /**
     * Mounts the portlet route on a URL with these query params; `task` mounts the Workflow
     * portlet with a task detail shown (`/c/workflow/<task>`).
     */
    const mount = (params: Params, { task = false } = {}) => {
        queryParams$ = new BehaviorSubject<Params>(params);
        openContent = vi.fn();
        clear = vi.fn();
        refuse = vi.fn();

        spectator = createComponent({
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: of({ canAccessPortlet: true }),
                        queryParams: queryParams$,
                        params: of({ id: task ? 'workflow' : 'content' }),
                        parent: { url: of([{ path: 'c' }]) },
                        snapshot: {
                            queryParams: params,
                            params: { id: task ? 'workflow' : 'content' }
                        },
                        firstChild: task ? { snapshot: { params: { asset: 'task-1' } } } : null
                    }
                },
                mockProvider(DotCustomEventHandlerService, { openContent, handle: vi.fn() }),
                {
                    provide: DotPanelUrlStateService,
                    useValue: { clear, refuse, register: vi.fn() }
                },
                mockProvider(DotRouterService, {
                    portletReload$: NEVER,
                    isJSPPortlet: vi.fn().mockReturnValue(true),
                    currentPortlet: { id: 'content', url: '/c/content' }
                }),
                mockProvider(DotContentTypeService),
                mockProvider(DotLoadingIndicatorService),
                mockProvider(DotIframeService),
                mockProvider(DotEventsSocket, { on: vi.fn().mockReturnValue(NEVER) }),
                mockProvider(LoggerService),
                mockProvider(GlobalStore, {
                    siteDetails: signal(null),
                    loadFreshPortletUrl: vi
                        .fn()
                        .mockReturnValue(of('/c/portal/layout?p_p_id=content'))
                })
            ]
        });
        spectator.detectChanges();
    };

    /** Lets the panel URL service, which the admin app loads on demand, be reached. */
    const settle = () => vi.dynamicImportSettled();

    /**
     * #38303, T174: every save and delete in a side panel, at any depth (a relationship's "New"
     * inside the panel a row opened), reloads the portlet's list, for as long as the portlet is
     * shown. The panel itself only reloads it on its close.
     */
    it.each([
        ['a save', 'saved', 0],
        ['a save in a stacked panel', 'saved', 1],
        ['a delete', 'deleted', 0]
    ] as const)('reloads the portlet on %s in any side panel', async (_label, kind, depth) => {
        mount({ p_p_id: 'content' });
        await settle();

        spectator
            .inject(DotContentChangeService)
            .notify({ kind, contentlet: { inode: 'saved-inode' } as never, depth });

        expect(spectator.inject(DotIframeService).reloadData).toHaveBeenCalledTimes(1);
        expect(spectator.inject(DotIframeService).reloadData).toHaveBeenCalledWith('content');
    });

    it('opens the version a URL names by identifier and language', async () => {
        mount({ editContent: IDENTIFIER, editContentLang: '2' });
        await settle();

        expect(openContent).toHaveBeenCalledTimes(1);
        expect(openContent).toHaveBeenCalledWith({
            mode: 'edit',
            identifier: IDENTIFIER,
            languageId: 2
        });
    });

    it('opens an edit by identifier alone when the URL names no language', async () => {
        mount({ editContent: IDENTIFIER });
        await settle();

        expect(openContent).toHaveBeenCalledWith({ mode: 'edit', identifier: IDENTIFIER });
    });

    // A create deep link redirects here with the folder it was given (`/c/:id/new/:contentType`).
    it('opens a create of the content type, in the folder the URL names', async () => {
        mount({ createContent: 'Blog', folder: FOLDER_INODE });
        await settle();

        expect(openContent).toHaveBeenCalledTimes(1);
        expect(openContent).toHaveBeenCalledWith({
            mode: 'new',
            contentType: 'Blog',
            folderInode: FOLDER_INODE
        });
    });

    it('opens a create without a folder when the URL names none', async () => {
        mount({ createContent: 'Blog' });
        await settle();

        expect(openContent).toHaveBeenCalledWith({ mode: 'new', contentType: 'Blog' });
    });

    // `folder` reaches the legacy create screen, so only an inode-shaped value is used.
    it('ignores a folder that is not an inode', async () => {
        mount({ createContent: 'Blog', folder: '../x' });
        await settle();

        expect(openContent).toHaveBeenCalledWith({ mode: 'new', contentType: 'Blog' });
    });

    // The router emits the same params again on a navigation that changes something else.
    it('opens nothing when the same params arrive again', async () => {
        mount({ editContent: IDENTIFIER, editContentLang: '2' });
        await settle();

        queryParams$.next({ editContent: IDENTIFIER, editContentLang: '2', mId: 'a1b2' });
        await settle();

        expect(openContent).toHaveBeenCalledTimes(1);
    });

    /**
     * Back and Forward are the panel URL service's (FR-048): the router replays a history entry
     * that may name a panel still open, which opening again would duplicate. A later navigation of
     * the app's own still opens what it names.
     */
    it("opens nothing for a navigation the browser's Back or Forward started", async () => {
        mount({});
        await settle();
        const currentNavigation = vi.spyOn(spectator.inject(Router), 'currentNavigation');

        currentNavigation.mockReturnValue({ trigger: 'popstate' } as Navigation);
        queryParams$.next({ editContent: IDENTIFIER, editContentLang: '1' });
        await settle();

        expect(openContent).not.toHaveBeenCalled();

        currentNavigation.mockReturnValue({ trigger: 'imperative' } as Navigation);
        queryParams$.next({ editContent: IDENTIFIER, editContentLang: '1', mId: 'a1b2' });
        await settle();

        expect(openContent).toHaveBeenCalledTimes(1);
    });

    /**
     * A portlet a Back or Forward shows again reads its URL once that navigation has ended, when
     * the router no longer reports it as current: the URL is still the panel URL service's, which
     * drops the params of a panel the author left (#38303, review R4).
     */
    it('opens nothing for a URL a Back or Forward showed, read after that navigation ended', async () => {
        const lastSuccessfulNavigation = vi
            .spyOn(
                Router.prototype as unknown as { lastSuccessfulNavigation: unknown },
                'lastSuccessfulNavigation',
                'get'
            )
            .mockReturnValue(signal({ trigger: 'popstate' } as Navigation));

        mount({ editContent: IDENTIFIER, editContentLang: '2' });
        await settle();

        expect(openContent).not.toHaveBeenCalled();
        lastSuccessfulNavigation.mockRestore();
    });

    it('opens what a later navigation names', async () => {
        mount({});
        await settle();

        queryParams$.next({ editContent: IDENTIFIER, editContentLang: '1' });
        await settle();

        expect(openContent).toHaveBeenCalledTimes(1);
        expect(openContent).toHaveBeenCalledWith({
            mode: 'edit',
            identifier: IDENTIFIER,
            languageId: 1
        });
    });

    /**
     * A task detail opens what the URL names itself, once its dialog has shown, so the panel
     * always ends up above the dialog (stack review L7-6).
     */
    it('leaves a URL naming a panel to the task detail it shows', async () => {
        mount({ editContent: IDENTIFIER, editContentLang: '2' }, { task: true });
        await settle();

        expect(openContent).not.toHaveBeenCalled();
    });

    it('opens nothing for a URL that names no panel', async () => {
        mount({ p_p_id: 'content' });
        await settle();

        expect(openContent).not.toHaveBeenCalled();
        expect(clear).not.toHaveBeenCalled();
        expect(refuse).not.toHaveBeenCalled();
    });

    // FR-049, T172: a link the author cannot have meant opens nothing, tells them, and loses its
    // params with the create's folder.
    it.each([
        [
            'names two panels',
            { editContent: IDENTIFIER, createContent: 'Blog', folder: FOLDER_INODE }
        ],
        ['names content by a malformed identifier', { editContent: 'wrong-identifier' }]
    ])('refuses a URL that %s, with the folder', async (_label, params) => {
        mount(params);
        await settle();

        expect(openContent).not.toHaveBeenCalled();
        expect(refuse).toHaveBeenCalledTimes(1);
        expect(refuse).toHaveBeenCalledWith(['folder']);
    });
});
