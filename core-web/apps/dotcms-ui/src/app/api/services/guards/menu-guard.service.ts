import { Observable, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
    ActivatedRouteSnapshot,
    CanActivate,
    PRIMARY_OUTLET,
    Params,
    Router,
    RouterStateSnapshot,
    UrlTree
} from '@angular/router';

import { catchError, map, switchMap } from 'rxjs/operators';

import {
    DotContentletService,
    DotHttpErrorManagerService,
    DotRouterService,
    DotSessionStorageService
} from '@dotcms/data-access';
import { mapParamsFromEditContentlet } from '@dotcms/utils';

import { DotNavigationService } from '../../../view/components/dot-navigation/services/dot-navigation.service';
import { DotMenuService } from '../dot-menu.service';

const CONTENT_DRIVE_PORTLET_ID = 'content-drive';
const CONTENT_DRIVE_URL = `/${CONTENT_DRIVE_PORTLET_ID}`;

/**
 * Portlets whose listing Content Drive replaces. Their bare listing URL (`/c/<id>`) is redirected
 * to Content Drive when the portlet is missing from the menu.
 */
const CONTENT_DRIVE_REPLACES = ['content', 'site-browser'];

/**
 * The Content Search URLs the guard knows a Content Drive equivalent for:
 * - `listing`: `/c/content` or `/c/site-browser`
 * - `edit`: `/c/content/<inode>`
 * - `create`: `/c/content/new` and `/c/content/new/<content type variable>`
 */
type ReplacedUrl =
    | { kind: 'listing'; portletId: string }
    | { kind: 'edit'; inode: string }
    | { kind: 'create'; contentType?: string };

/**
 * Route Guard that checks if a User have access to the specified Menu portlet.
 */
@Injectable()
export class MenuGuardService implements CanActivate {
    private dotMenuService = inject(DotMenuService);
    private dotRouterService = inject(DotRouterService);
    private dotNavigationService = inject(DotNavigationService);
    private dotSessionStorageService = inject(DotSessionStorageService);
    private dotContentletService = inject(DotContentletService);
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private router = inject(Router);

    canActivate(
        _route: ActivatedRouteSnapshot,
        state: RouterStateSnapshot
    ): Observable<boolean | UrlTree> {
        return this.canAccessPortlet(state.url);
    }

    canActivateChild(
        _route: ActivatedRouteSnapshot,
        state: RouterStateSnapshot
    ): Observable<boolean | UrlTree> {
        return this.canAccessPortlet(state.url);
    }

    /**
     * Checks the requested portlet against the user's menu. When it is missing, a Content Search
     * or Site Browser URL goes to its Content Drive equivalent if the user has Content Drive;
     * anything else goes to the first portlet in the menu.
     *
     * @param url the requested route
     * @returns true to allow the route, a UrlTree to redirect, or false after navigating to the
     * first portlet
     */
    private canAccessPortlet(url: string): Observable<boolean | UrlTree> {
        const id = this.dotRouterService.getPortletId(url);
        const checkJSPPortlet = this.dotRouterService.isJSPPortletURL(url);

        return this.dotMenuService.isPortletInMenu(id, checkJSPPortlet).pipe(
            switchMap((isValidPortlet) =>
                isValidPortlet ? of(true) : this.getContentDriveRedirect(url)
            ),
            map((result) => {
                if (result === false) {
                    this.dotSessionStorageService.removeVariantId();
                    this.dotNavigationService.goToFirstPortlet();
                }

                return result;
            })
        );
    }

    /**
     * Builds the Content Drive equivalent of a Content Search or Site Browser URL, when the user
     * has Content Drive in their menu.
     *
     * Edit and create links coming from Content Drive itself are left alone: Content Drive sends
     * content with the legacy editor to those URLs, and sending them back would loop.
     *
     * @param url the rejected route
     * @returns the Content Drive UrlTree, or false when there is nothing to redirect to
     */
    private getContentDriveRedirect(url: string): Observable<UrlTree | false> {
        const urlTree = this.router.parseUrl(url);
        const replaced = this.parseReplacedUrl(urlTree);
        const isLeavingContentDrive = this.router.url.startsWith(CONTENT_DRIVE_URL);

        if (!replaced || (replaced.kind !== 'listing' && isLeavingContentDrive)) {
            return of(false);
        }

        return this.dotMenuService
            .isPortletInMenu(CONTENT_DRIVE_PORTLET_ID)
            .pipe(
                switchMap(
                    (hasContentDrive): Observable<UrlTree | false> =>
                        hasContentDrive
                            ? this.getContentDriveUrl(replaced, urlTree.queryParams)
                            : of(false)
                )
            );
    }

    /**
     * Reads which Content Search or Site Browser URL was requested.
     *
     * @param urlTree the rejected route
     * @returns the replaced URL, or null when Content Drive has no equivalent for it
     */
    private parseReplacedUrl(urlTree: UrlTree): ReplacedUrl | null {
        const [root, portletId, asset, contentType, ...rest] = (
            urlTree.root.children[PRIMARY_OUTLET]?.segments ?? []
        ).map(({ path }) => path);

        if (root !== 'c' || !CONTENT_DRIVE_REPLACES.includes(portletId) || rest.length) {
            return null;
        }

        if (!asset) {
            return { kind: 'listing', portletId };
        }

        if (portletId !== 'content') {
            return null;
        }

        if (asset === 'new') {
            return { kind: 'create', contentType };
        }

        return contentType ? null : { kind: 'edit', inode: asset };
    }

    /**
     * Builds the Content Drive URL for a replaced URL.
     *
     * - The Content Search `filter` param holds a content type variable, which Content Drive takes
     *   as its `contentType` filter.
     * - Edit links carry an inode, while Content Drive opens content by identifier and language,
     *   so the content is looked up first. If the lookup fails, the error is reported and the user
     *   lands on Content Drive.
     * - Create links pass the content type as `createContent`.
     * - Edit and create links restore the `CD_`-prefixed params Content Drive adds when it sends
     *   the user to the legacy editor, so they land back on their folder and filters. Other params
     *   have no Content Drive equivalent and are dropped.
     *
     * @param replaced the replaced URL
     * @param queryParams the rejected route's query params
     * @returns the Content Drive UrlTree, or false when the error handler already navigated away
     */
    private getContentDriveUrl(
        replaced: ReplacedUrl,
        queryParams: Params
    ): Observable<UrlTree | false> {
        const toUrlTree = (params: Params) =>
            this.router.createUrlTree([CONTENT_DRIVE_URL], { queryParams: params });

        if (replaced.kind === 'listing') {
            const contentType = replaced.portletId === 'content' ? queryParams['filter'] : null;

            return of(toUrlTree(contentType ? { filters: `contentType:${contentType}` } : {}));
        }

        const restored = mapParamsFromEditContentlet(new URLSearchParams(queryParams));

        if (replaced.kind === 'create') {
            const createContent = replaced.contentType
                ? { createContent: replaced.contentType }
                : {};

            return of(toUrlTree({ ...restored, ...createContent }));
        }

        return this.dotContentletService.getContentletByInode(replaced.inode).pipe(
            map(({ identifier, languageId }) =>
                toUrlTree({ ...restored, editContent: identifier, editContentLang: languageId })
            ),
            catchError((error: HttpErrorResponse) =>
                this.dotHttpErrorManagerService
                    .handle(error)
                    .pipe(map(({ redirected }) => (redirected ? false : toUrlTree(restored))))
            )
        );
    }
}
