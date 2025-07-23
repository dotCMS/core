import { BehaviorSubject, Observable, Subject } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import {
    ActivatedRoute,
    Event,
    NavigationEnd,
    NavigationExtras,
    Params,
    Router
} from '@angular/router';

import { filter } from 'rxjs/operators';

import { LOGOUT_URL } from '@dotcms/dotcms-js';
import { DotAppsSite, DotNavigateToOptions, PortletNav } from '@dotcms/dotcms-models';

@Injectable()
export class DotRouterService {
    private router = inject(Router);
    private route = inject(ActivatedRoute);

    portletReload$ = new Subject();
    private _storedRedirectUrl: string;
    private _routeHistory: PortletNav = { url: '' };
    private CUSTOM_PORTLET_ID_PREFIX = 'c_';
    private _routeCanBeDeactivated = new BehaviorSubject(true);
    private _pageLeaveRequest = new Subject<void>();

    constructor() {
        this._routeHistory.url = this.router.url;
        this.router.events
            .pipe(filter((event: Event) => event instanceof NavigationEnd))
            .subscribe((event: NavigationEnd) => {
                this.routeHistory = {
                    url: event.url,
                    previousUrl: this._routeHistory.url
                };
            });
    }

    get currentSavedURL(): string {
        return this._routeHistory.url;
    }

    get previousUrl(): string {
        return this._routeHistory.previousUrl;
    }

    get currentPortlet(): PortletNav {
        return {
            url: this.router.routerState.snapshot.url,
            id: this.getPortletId(this.router.routerState.snapshot.url)
        };
    }

    set storedRedirectUrl(url: string) {
        this._storedRedirectUrl = url;
    }

    get storedRedirectUrl(): string {
        return this._storedRedirectUrl;
    }

    get routeHistory(): PortletNav {
        return this._routeHistory;
    }

    set routeHistory(value: PortletNav) {
        this._routeHistory = value;
    }

    get queryParams(): Params {
        const nav = this.router.getCurrentNavigation();

        return nav ? nav.finalUrl.queryParams : this.route.snapshot.queryParams;
    }

    get canDeactivateRoute$(): Observable<boolean> {
        return this._routeCanBeDeactivated.asObservable();
    }

    get pageLeaveRequest$() {
        return this._pageLeaveRequest.asObservable();
    }

    /**
     * Redirect to previous url
     *
     * @memberof DotRouterService
     */
    goToPreviousUrl(): void {
        this.router.navigate([this.routeHistory.previousUrl]);
    }

    /**
     * Reload the current iframe portlet;
     *
     * @param string id
     * @memberof DotRouterService
     */
    reloadCurrentPortlet(id?: string): void {
        this.portletReload$.next(id);
    }

    /**
     * Go to edit page
     *
     * @param string url
     * @param string [languageId]
     * @returns Promise<boolean>
     * @memberof DotRouterService
     */
    goToEditPage(queryParams: Params): Promise<boolean> {
        const menuId = 'edit-page';

        return this.router.navigate([`/${menuId}/content`], {
            queryParams,
            state: {
                menuId
            }
        });
    }

    /**
     * Go to edit contentlet
     *
     * @param string inode
     * @returns Promise<boolean>
     * @memberof DotRouterService
     */
    goToEditContentlet(inode: string): Promise<boolean> {
        const url = this.currentPortlet.url.split('?')[0]; // remove query params from portlet url

        return this.router.navigate([`${url}/${inode}`], {
            queryParamsHandling: 'preserve'
        }); // Preserve URL query params
    }

    /**
     * Go to edit workflow task
     *
     * @param string inode
     * @returns Promise<boolean>
     * @memberof DotRouterService
     */
    goToEditTask(inode: string): Promise<boolean> {
        return this.router.navigate([`/c/workflow/${inode}`]);
    }

    /**
     * Go to first porlet unless userEditPageRedirect is passed or storedRedirectUrl is set
     *
     * @param string [userEditPageRedirect]
     * @returns Promise<boolean>
     * @memberof DotRouterService
     */
    goToMain(userEditPageRedirect?: string): Promise<boolean> {
        return userEditPageRedirect
            ? this.goToEditPage({
                  url: userEditPageRedirect
              })
            : this.redirectMain();
    }

    /**
     * Redirects to the login page adding a busting cache parameter.
     *
     * * @param NavigationExtras navExtras
     * @memberof DotRouterService
     */
    goToLogin(navExtras?: NavigationExtras): void {
        this.router.navigate(['/public/login'], this.addCacheBusting(navExtras));
    }

    goToSiteBrowser(): void {
        this.router.navigate(['/c/site-browser']);
    }

    /**
     * Redirect to Starter page
     *
     * @memberof DotRouterService
     */
    goToStarter(): void {
        this.router.navigate(['/starter']);
    }

    /**
     * Redirect to Content page
     *
     * @memberof DotRouterService
     */
    goToContent(): void {
        this.router.navigate(['/c/content']);
    }

    /**
     * Redirect to Content page
     *
     * @memberof DotRouterService
     */
    goToCreateContent(variableName: string): void {
        this.router.navigate([`/c/content/new/${variableName}`]);
    }

    /**
     * Redirect to edit the content type
     *
     * @param {string} id
     * @param {string} portlet
     * @memberof DotRouterService
     */
    goToEditContentType(id: string, portlet: string): void {
        this.router.navigate([`/${portlet}/edit/${id}`]);
    }

    /**
     * Redirect to edit the template.
     * If the inode is paased, load a specific version of the template
     *
     * @param {string} id
     * @memberof DotRouterService
     */
    goToEditTemplate(id: string, inode?: string): void {
        this.router.navigate([
            inode ? `/templates/edit/${id}/inode/${inode}` : `/templates/edit/${id}`
        ]);
    }

    /**
     * Redirects to the create container page
     * @returns {void}
     * @memberof DotRouterService
     */
    goToCreateContainer(): void {
        this.router.navigate(['/containers/create']);
    }

    /**
     * Redirect to edit the container.
     * If the inode is passed, load a specific version of the container
     *
     * @param {string} id
     * @param {string} inode
     * @memberof DotRouterService
     */
    goToEditContainer(id: string, inode?: string): void {
        this.router.navigate([
            inode ? `/containers/edit/${id}/inode/${inode}` : `/containers/edit/${id}`
        ]);
    }

    goToURL(url: string, extras?: NavigationExtras): void {
        this.router.navigate([url], extras);
    }

    /**
     * Redirects to backend to handle the logout.
     *
     * @memberof DotRouterService
     */
    doLogOut(): void {
        this.router.navigate([LOGOUT_URL], this.addCacheBusting());
    }

    /**
     * Redirects to an App Configuration page
     *
     * @param string appKey
     * @memberof DotRouterService
     */
    goToAppsConfiguration(appKey: string) {
        this.router.navigate([`/apps/${appKey}`]);
    }

    /**
     * Redirects to create/edit configuration site page
     *
     * @param string integrationKey
     * @param DotAppsSites site
     * @memberof DotRouterService
     */
    goToUpdateAppsConfiguration(integrationKey: string, site: DotAppsSite) {
        const route =
            site && site.configured
                ? `/apps/${integrationKey}/edit/${site.id}`
                : `/apps/${integrationKey}/create/${site.id}`;
        this.router.navigate([route]);
    }

    isPublicUrl(url: string): boolean {
        return url.startsWith('/public');
    }

    isFromCoreUrl(url: string): boolean {
        return url.startsWith('/fromCore');
    }

    isRootUrl(url: string): boolean {
        return url === '/';
    }

    /**
     * Check if Portlet is based on a JSP/iframe page
     * @returns boolean
     * @memberof DotRouterService
     */
    isJSPPortlet(): boolean {
        return this.router.url.startsWith('/c/');
    }

    /**
     * Check if a URL is pointing to a JSP/iframe page
     * @returns boolean
     * @memberof DotRouterService
     */
    isJSPPortletURL(url): boolean {
        return url.startsWith('/c/');
    }

    /**
     * Check if the current route is an edit page
     *
     * @returns boolean
     * @memberof DotRouterService
     */
    isEditPage(): boolean {
        return this.currentPortlet.id === 'edit-page';
    }

    /**
     * Go to a portlet by URL
     *
     * @param {string} link
     * @param {boolean} [replaceUrl]
     * @return {*}  {Promise<boolean>}
     * @memberof DotRouterService
     */
    gotoPortlet(link: string, navigateToPorletOptions?: DotNavigateToOptions): Promise<boolean> {
        const { replaceUrl = false, queryParamsHandling = '' } = navigateToPorletOptions || {};
        const url = this.router.createUrlTree([link], { queryParamsHandling });

        return this.router.navigateByUrl(url, { replaceUrl });
    }

    goToForgotPassword(): void {
        this.router.navigate(['/public/forgotPassword']);
    }

    getPortletId(url: string): string {
        url = decodeURIComponent(url);
        if (url.indexOf('?') > 0) {
            url = url.substring(0, url.indexOf('?'));
        }

        const urlSegments = url
            .split('/')
            .filter((item) => item !== '' && item !== '#' && item !== 'c');

        return urlSegments.indexOf('add') > -1 ? urlSegments.splice(-1)[0] : urlSegments[0];
    }

    isPublicPage(): boolean {
        return this.currentPortlet.url.startsWith('/public');
    }

    /**
     * Return true if the current portlet is a custom portlet
     *
     * @returns {boolean}
     * @memberof DotRouterService
     */
    isCurrentPortletCustom(): boolean {
        return this.isCustomPortlet(this.currentPortlet.id);
    }

    /**
     * Return true if potlrtId represent a custom portlet
     *
     * @param {string} portletId
     * @returns {boolean}
     * @memberof DotRouterService
     */
    isCustomPortlet(portletId: string): boolean {
        return portletId.startsWith(this.CUSTOM_PORTLET_ID_PREFIX);
    }

    /**
     * Replace the query params received, in the URL
     *
     * @param {{ [key: string]: string | number }} params
     * @returns {void}
     * @memberof DotRouterService
     */
    replaceQueryParams(params: { [key: string]: string | number }): void {
        this.router.navigate([], {
            queryParams: params,
            queryParamsHandling: 'merge'
        });
    }

    /**
     * Only relevant for components that depend on CanDeactivateGuardService
     * @memberof DotRouterService
     */
    allowRouteDeactivation() {
        this._routeCanBeDeactivated.next(true);
    }

    /**
     * Only relevant for components that depend on CanDeactivateGuardService
     * @memberof DotRouterService
     */
    forbidRouteDeactivation() {
        this._routeCanBeDeactivated.next(false);
    }

    /**
     * Only relevant for components that depend on CanDeactivateGuardService
     * @memberof DotRouterService
     */
    requestPageLeave() {
        this._pageLeaveRequest.next();
    }

    private redirectMain(): Promise<boolean> {
        if (this.storedRedirectUrl) {
            return this.router.navigate([this.storedRedirectUrl]).then((ok: boolean) => {
                this.storedRedirectUrl = null;

                return ok;
            });
        } else {
            return this.router.navigate(['/']);
        }
    }

    private addCacheBusting(navExtras?: NavigationExtras): NavigationExtras {
        if (navExtras) {
            navExtras.queryParams['r'] = new Date().getTime();
        } else {
            navExtras = { queryParams: { r: new Date().getTime() } };
        }

        return navExtras;
    }
}
