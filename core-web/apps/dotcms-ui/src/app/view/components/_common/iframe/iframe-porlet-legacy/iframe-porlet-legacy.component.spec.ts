import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { BehaviorSubject, NEVER, of } from 'rxjs';
import { Mock, describe, expect, it, vi } from 'vitest';

import { signal } from '@angular/core';
import { ActivatedRoute, Navigation, Params, Router } from '@angular/router';

import {
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
import { DotLoadingIndicatorService } from '@dotcms/utils';

import { IframePortletLegacyComponent } from './iframe-porlet-legacy.component';

import { DotCustomEventHandlerService } from '../../../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';

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

    /** Mounts the portlet route on a URL with these query params. */
    const mount = (params: Params) => {
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
                        params: of({ id: 'content' }),
                        parent: { url: of([{ path: 'c' }]) },
                        snapshot: { queryParams: params, params: { id: 'content' } }
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
                mockProvider(DotMenuService, {
                    getUrlById: vi.fn().mockReturnValue(of('/c/portal/layout?p_p_id=content'))
                }),
                mockProvider(DotContentTypeService),
                mockProvider(DotLoadingIndicatorService),
                mockProvider(DotIframeService),
                mockProvider(DotEventsSocket, { on: vi.fn().mockReturnValue(NEVER) }),
                mockProvider(LoggerService),
                mockProvider(GlobalStore, { siteDetails: signal(null) })
            ]
        });
        spectator.detectChanges();
    };

    /** Lets the panel URL service, which the admin app loads on demand, be reached. */
    const settle = () => vi.dynamicImportSettled();

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
