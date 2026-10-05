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
    DotSessionStorageService,
    DotSiteService
} from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';

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
    private dotSiteService = inject(DotSiteService);
    private globalStore = inject(GlobalStore);
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
     * The variant id is cleared whenever the requested portlet is rejected, since the user leaves
     * it either way.
     *
     * @param url the requested route
     * @returns true to allow the route, a UrlTree to redirect, or false when the navigation stops,
     * after going to the first portlet or because an error handler already navigated
     */
    private canAccessPortlet(url: string): Observable<boolean | UrlTree> {
        const id = this.dotRouterService.getPortletId(url);
        const checkJSPPortlet = this.dotRouterService.isJSPPortletURL(url);

        return this.dotMenuService.isPortletInMenu(id, checkJSPPortlet).pipe(
            switchMap(
                (isValidPortlet): Observable<boolean | UrlTree | null> =>
                    isValidPortlet ? of(true) : this.getContentDriveRedirect(url)
            ),
            map((result) => {
                if (result === true) {
                    return true;
                }

                this.dotSessionStorageService.removeVariantId();

                if (result === null) {
                    this.dotNavigationService.goToFirstPortlet();

                    return false;
                }

                return result;
            })
        );
    }

    /**
     * Builds the Content Drive equivalent of a Content Search or Site Browser URL, when the user
     * has Content Drive in their menu.
     *
     * Every link is redirected the same way, wherever it comes from. Content Drive opens legacy
     * editor content in its own side panel and never links to the Content Search route, so a link
     * from inside Content Drive cannot loop back into it (#37759, FR-027).
     *
     * @param url the rejected route
     * @returns the Content Drive UrlTree, false when an error handler already navigated away, or
     * null when there is no Content Drive equivalent and the user goes to the first portlet
     */
    private getContentDriveRedirect(url: string): Observable<UrlTree | false | null> {
        const urlTree = this.router.parseUrl(url);
        const replaced = this.parseReplacedUrl(urlTree);

        if (!replaced) {
            return of(null);
        }

        return this.dotMenuService
            .isPortletInMenu(CONTENT_DRIVE_PORTLET_ID)
            .pipe(
                switchMap(
                    (hasContentDrive): Observable<UrlTree | false | null> =>
                        hasContentDrive
                            ? this.getContentDriveUrl(replaced, urlTree.queryParams)
                            : of(null)
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
     *   as its `contentType` filter. The Content Types chip fills in the base type it belongs to.
     * - The Site Browser `path` param holds the folder to open (see {@link getFolderPath}).
     * - Edit links carry an inode, while Content Drive opens content by identifier and language,
     *   so the content is looked up first. If the lookup fails, the error is reported and the user
     *   lands on Content Drive.
     * - Create links pass the content type as `createContent`.
     * - Other params have no Content Drive equivalent and are dropped, including the `CD_` params
     *   older builds of Content Drive added when they sent the user to the legacy editor (#37759,
     *   FR-026).
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

        if (replaced.kind === 'listing' && replaced.portletId === 'site-browser') {
            const folder = queryParams['path'];

            return folder
                ? this.getFolderPath(folder).pipe(map((path) => toUrlTree(path ? { path } : {})))
                : of(toUrlTree({}));
        }

        if (replaced.kind === 'listing') {
            const contentType = queryParams['filter'];

            return of(toUrlTree(contentType ? { filters: `contentType:${contentType}` } : {}));
        }

        if (replaced.kind === 'create') {
            return of(
                toUrlTree(replaced.contentType ? { createContent: replaced.contentType } : {})
            );
        }

        return this.dotContentletService.getContentletByInode(replaced.inode).pipe(
            map(({ identifier, languageId }) =>
                toUrlTree({ editContent: identifier, editContentLang: languageId })
            ),
            catchError((error: HttpErrorResponse) =>
                this.dotHttpErrorManagerService
                    .handle(error)
                    .pipe(map(({ redirected }) => (redirected ? false : toUrlTree({}))))
            )
        );
    }

    /**
     * Turns a Site Browser folder into Content Drive's `path` param, which is a folder path inside
     * the current site.
     *
     * Files stored in the Site Browser (containers, templates) give their folder with the host,
     * as `//<host>/<folder path>/`. The host is stripped. When it is not the current site, the
     * guard switches to that site first: Content Drive loads again when the site changes, and it
     * reads the folder from the URL each time, so it lands on the folder in the right site. A path
     * without a host is a folder in the current site and is passed as is.
     *
     * @param folder the Site Browser folder
     * @returns the folder path in the current site, or null when its host cannot be found
     */
    private getFolderPath(folder: string): Observable<string | null> {
        const [, hostname, path = '/'] = folder.match(/^\/\/([^/]+)(\/.*)?$/) ?? [];

        if (!hostname) {
            return of(folder);
        }

        if (hostname === this.globalStore.siteDetails()?.hostname) {
            return of(path);
        }

        return this.dotSiteService.getSites({ filter: hostname }).pipe(
            map(({ sites }) => {
                const site = sites.find((site) => site.hostname === hostname);

                if (!site) {
                    return null;
                }

                this.globalStore.switchCurrentSite(site.identifier);

                return path;
            }),
            catchError(() => of(null))
        );
    }
}
