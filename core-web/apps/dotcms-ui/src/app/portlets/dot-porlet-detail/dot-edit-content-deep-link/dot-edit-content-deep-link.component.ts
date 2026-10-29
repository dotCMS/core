import { Component, Injector, OnDestroy, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { DotIframeService, DotRouterService } from '@dotcms/data-access';
import type { DotEditContentRequest } from '@dotcms/edit-content';
import type { DotSidePanelRef } from '@dotcms/ui';

import { openEditContentPanel } from '../../../api/util/open-edit-content-panel';

/**
 * A deep link to content (`/c/:id/:asset`, `/c/:id/new/:contentType`, `/pages/:asset`,
 * `/pages/new/:contentType`) opens the side panel over the portlet it belongs to (#38303, R11). The
 * links keep their paths, so bookmarks and the menu's redirects keep working; the route only opens
 * the panel and goes back to the portlet when it closes.
 *
 * The portlet is the one `DotRouterService.currentPortlet` names: its id goes on the request and to
 * the iframe reload, and its URL without the asset segments is where closing the panel returns.
 */
@Component({
    selector: 'dot-edit-content-deep-link',
    template: ''
})
export class DotEditContentDeepLinkComponent implements OnInit, OnDestroy {
    readonly #route = inject(ActivatedRoute);
    readonly #router = inject(Router);
    readonly #dotRouterService = inject(DotRouterService);
    readonly #dotIframeService = inject(DotIframeService);
    readonly #injector = inject(Injector);

    /** The panel this route opened, once the editor has loaded. */
    #panel: DotSidePanelRef | null = null;

    /** Set when the route is left, so the panel closing with it does not navigate back. */
    #leaving = false;

    /**
     * Opens the panel for the route: an edit for `:asset`, a create for `:contentType` (in the
     * folder `?folder` names). Every save reloads the portlet; closing the panel returns to it.
     */
    ngOnInit(): void {
        const { id: portletId, url } = this.#dotRouterService.currentPortlet;
        const returnUrl = parentPortletUrl(url);
        const reload = () => {
            if (portletId) {
                this.#dotIframeService.reloadData(portletId);
            }
        };

        void openEditContentPanel(
            { ...this.#request(), portletId },
            {
                injector: this.#injector,
                saved: reload,
                closed: () => {
                    if (this.#leaving) {
                        return;
                    }

                    this.#router.navigate([returnUrl], { queryParamsHandling: 'preserve' });
                    reload();
                }
            }
        ).then((panel) => (this.#panel = panel));
    }

    /**
     * The route is being left (the author went elsewhere): the panel goes with it, without sending
     * the author back to the portlet. A panel still loading is never opened.
     */
    ngOnDestroy(): void {
        this.#leaving = true;
        this.#panel?.close();
    }

    /** What the route names: an inode to edit, or a content type to create. */
    #request(): DotEditContentRequest {
        const { asset, contentType } = this.#route.snapshot.params;
        const folderInode = this.#route.snapshot.queryParams['folder'];

        return asset
            ? { mode: 'edit', inode: asset }
            : { mode: 'new', contentType, ...(folderInode ? { folderInode } : {}) };
    }
}

/**
 * The portlet a deep link belongs to: its URL without the query and without the asset segments
 * (`/c/content/abc` → `/c/content`, `/pages/new/Blog?folder=x` → `/pages`).
 *
 * @param url The deep link's full URL.
 */
function parentPortletUrl(url: string): string {
    const segments = url.split('?')[0].split('/');
    const createIndex = segments.lastIndexOf('new');

    return segments.slice(0, createIndex > 0 ? createIndex : -1).join('/');
}
