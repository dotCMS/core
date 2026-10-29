import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { ActivatedRoute, convertToParamMap, Params, Router } from '@angular/router';

import { DotIframeService, DotRouterService } from '@dotcms/data-access';
// The opener is the component's dependency; the app also lazy-loads the library's routes (#38303).
// eslint-disable-next-line @nx/enforce-module-boundaries
import {
    DotEditContentOpenerService,
    DotEditContentOpenOptions,
    DotEditContentRequest
} from '@dotcms/edit-content';
import { DotSidePanelRef } from '@dotcms/ui';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotEditContentDeepLinkComponent } from './dot-edit-content-deep-link.component';

/**
 * #38303 (R11) — the deep links to content (`/c/:id/:asset`, `/c/content/new/:contentType`,
 * `/pages/:asset`, `/pages/new/:contentType`) keep their paths and open the side panel over the
 * portlet they belong to. The route component only opens the panel and leaves when it closes.
 *
 * The parent portlet is the one `DotRouterService.currentPortlet` names: its id goes on the request
 * and to `reloadData`, and its URL, without the asset segments, is where closing the panel returns.
 */
describe('DotEditContentDeepLinkComponent', () => {
    let spectator: Spectator<DotEditContentDeepLinkComponent>;
    let router: { navigate: Mock };
    let reloadData: Mock;

    /** Every panel the component opened, as the fake opener returned them. */
    let refs: DotSidePanelRef[];

    const createComponent = createComponentFactory({
        component: DotEditContentDeepLinkComponent,
        detectChanges: false,
        providers: [
            mockProvider(DotEditContentOpenerService, {
                // Like the real opener: `closed` fires when the returned ref closes.
                open: vi.fn(
                    (_request: DotEditContentRequest, options: DotEditContentOpenOptions) => {
                        const ref = new DotSidePanelRef(() => undefined);
                        ref.closed.subscribe(() => options.closed?.());
                        refs.push(ref);

                        return ref;
                    }
                )
            })
        ]
    });

    /** Lets the component load the opener, which the admin app imports on demand, and open. */
    const settle = async (): Promise<void> => {
        await vi.dynamicImportSettled();
        await new Promise((resolve) => setTimeout(resolve));
    };

    /**
     * Mounts the component on a deep link and waits for it to open the panel.
     *
     * @param portlet The portlet `DotRouterService` reports as current: its id and the full URL.
     * @param params The route params (`asset`, or `contentType`).
     * @param queryParams The route query params (`folder` for a create).
     */
    const mountOn = async (
        portlet: { id: string; url: string },
        params: Params,
        queryParams: Params = {}
    ): Promise<void> => {
        mountWithoutWaiting(portlet, params, queryParams);
        await settle();
    };

    /** Mounts the component on a deep link; the panel opens once the opener has loaded. */
    const mountWithoutWaiting = (
        portlet: { id: string; url: string },
        params: Params,
        queryParams: Params = {}
    ): void => {
        refs = [];
        router = { navigate: vi.fn().mockResolvedValue(true) };
        reloadData = vi.fn();

        spectator = createComponent({
            providers: [
                { provide: Router, useValue: router },
                { provide: DotRouterService, useValue: { currentPortlet: portlet } },
                { provide: DotIframeService, useValue: { reloadData } },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            params,
                            queryParams,
                            paramMap: convertToParamMap(params),
                            queryParamMap: convertToParamMap(queryParams)
                        }
                    }
                }
            ]
        });
        (spectator.inject(DotEditContentOpenerService).open as Mock).mockClear();
        spectator.detectChanges();
    };

    /** The opener's `open`, as the mock records it. */
    const opened = () => spectator.inject(DotEditContentOpenerService).open as Mock;

    /** The request the component asked for, on its only `open`. */
    const openedRequest = (): DotEditContentRequest => {
        expect(opened().mock.calls.length, 'the component opens one panel').toBe(1);

        return opened().mock.calls[0][0] as DotEditContentRequest;
    };

    /** The options the component handed the opener, on its only `open`. */
    const openOptions = (): DotEditContentOpenOptions => {
        expect(opened().mock.calls.length, 'the component opens one panel').toBe(1);

        return opened().mock.calls[0][1] as DotEditContentOpenOptions;
    };

    describe('an existing content (/c/:id/:asset)', () => {
        beforeEach(() =>
            mountOn({ id: 'content', url: '/c/content/abc-inode' }, { asset: 'abc-inode' })
        );

        it('should open the content for editing on init, naming the parent portlet', () => {
            expect(openedRequest()).toEqual({
                mode: 'edit',
                inode: 'abc-inode',
                portletId: 'content'
            });
        });

        // The panel then closes with the route, not with the admin root.
        it('should open the panel with its own injector', () => {
            const { injector } = openOptions();

            // Compared by identity: printing an injector runs the worker out of memory.
            expect(
                injector.get(DotEditContentDeepLinkComponent, null) === spectator.component
            ).toBe(true);
        });

        it('should reload the parent portlet on every save', () => {
            openOptions().saved?.(createFakeContentlet({ inode: 'saved-inode' }));

            expect(reloadData).toHaveBeenCalledWith('content');
        });

        it('should return to the parent portlet, keeping its query params, when the panel closes', () => {
            refs[0].close();

            expect(router.navigate).toHaveBeenCalledWith(['/c/content'], {
                queryParamsHandling: 'preserve'
            });
            expect(reloadData).toHaveBeenCalledWith('content');
        });

        // Leaving the route through the menu must not be bounced back to the portlet.
        it('should close the panel when the route is left, without navigating back', () => {
            spectator.fixture.destroy();

            expect(refs[0].isClosed).toBe(true);
            expect(router.navigate.mock.calls.length).toBe(0);
        });
    });

    // The opener loads on demand: a route left before it arrives opens nothing.
    describe('a route left before the editor loads', () => {
        it('should open no panel', async () => {
            mountWithoutWaiting(
                { id: 'content', url: '/c/content/abc-inode' },
                { asset: 'abc-inode' }
            );
            spectator.fixture.destroy();

            await settle();

            expect(opened().mock.calls.length).toBe(0);
            expect(router.navigate.mock.calls.length).toBe(0);
        });
    });

    describe('a new content (/c/content/new/:contentType)', () => {
        it('should open a create of the content type on init, naming the parent portlet', async () => {
            await mountOn({ id: 'content', url: '/c/content/new/Blog' }, { contentType: 'Blog' });

            expect(openedRequest()).toEqual({
                mode: 'new',
                contentType: 'Blog',
                portletId: 'content'
            });
        });

        // Content Drive and the Site Browser pass the folder to create in as `?folder=<inode>`.
        it('should pre-select the folder the link names', async () => {
            await mountOn(
                { id: 'content', url: '/c/content/new/Blog?folder=folder-inode' },
                { contentType: 'Blog' },
                { folder: 'folder-inode' }
            );

            expect(openedRequest()).toEqual({
                mode: 'new',
                contentType: 'Blog',
                folderInode: 'folder-inode',
                portletId: 'content'
            });
        });

        it('should return to the parent portlet when the panel closes', async () => {
            await mountOn({ id: 'content', url: '/c/content/new/Blog' }, { contentType: 'Blog' });

            refs[0].close();

            expect(router.navigate).toHaveBeenCalledWith(['/c/content'], {
                queryParamsHandling: 'preserve'
            });
            expect(reloadData).toHaveBeenCalledWith('content');
        });
    });

    describe('the Pages portlet (/pages/:asset, /pages/new/:contentType)', () => {
        it('should open the page for editing, naming the Pages portlet', async () => {
            await mountOn({ id: 'pages', url: '/pages/page-inode' }, { asset: 'page-inode' });

            expect(openedRequest()).toEqual({
                mode: 'edit',
                inode: 'page-inode',
                portletId: 'pages'
            });
        });

        it('should return to the Pages portlet and reload it when the panel closes', async () => {
            await mountOn({ id: 'pages', url: '/pages/page-inode' }, { asset: 'page-inode' });

            refs[0].close();

            expect(router.navigate).toHaveBeenCalledWith(['/pages'], {
                queryParamsHandling: 'preserve'
            });
            expect(reloadData).toHaveBeenCalledWith('pages');
        });

        it('should return to the Pages portlet after a create', async () => {
            await mountOn(
                { id: 'pages', url: '/pages/new/htmlpageasset' },
                { contentType: 'htmlpageasset' }
            );

            refs[0].close();

            expect(router.navigate).toHaveBeenCalledWith(['/pages'], {
                queryParamsHandling: 'preserve'
            });
        });
    });
});
