import { Observable } from 'rxjs';

import { inject, Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Event, NavigationEnd, Router } from '@angular/router';

import { filter, map, switchMap } from 'rxjs/operators';

import { DotIframeService, DotRouterService } from '@dotcms/data-access';
import { Auth, LoginService } from '@dotcms/dotcms-js';
import { MenuStatus } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

@Injectable()
export class DotNavigationService {
    private dotIframeService = inject(DotIframeService);
    private dotRouterService = inject(DotRouterService);
    private loginService = inject(LoginService);
    private router = inject(Router);
    private titleService = inject(Title);
    readonly #globalStore = inject(GlobalStore);
    private _appMainTitle: string;

    constructor() {
        this._appMainTitle = this.titleService.getTitle();

        // First load: the store asks the backend once and never again while it is `ready`.
        this.#globalStore.ensureLoaded().subscribe();

        // Navigating between portlets only moves the title and the highlight; it never asks for the menu.
        this.onNavigationEnd()
            .pipe(
                switchMap((event: NavigationEnd) =>
                    this.#globalStore.ensureLoaded().pipe(map(() => event))
                )
            )
            .subscribe((event: NavigationEnd) => {
                const pageTitle = this.#globalStore.getPageTitleByUrl(event.urlAfterRedirects);
                if (pageTitle) {
                    this.titleService.setTitle(`${pageTitle} - ${this._appMainTitle}`);
                }
            });

        // The backend changed the tool groups: replace the menu once. The store keeps the user's place.
        this.#globalStore
            .portletLayoutUpdated$()
            .pipe(switchMap(() => this.#reloadMenu()))
            .subscribe();

        // Login and login-as: reload the menu for that user and land on its first portlet.
        this.loginService.auth$
            .pipe(
                filter((auth: Auth) => !!(auth.loginAsUser || auth.user)),
                switchMap(() => this.#reloadMenu()),
                filter((status) => status === 'ready')
            )
            .subscribe(() => this.goToFirstPortlet());
    }

    onNavigationEnd(): Observable<NavigationEnd> {
        return this.router.events.pipe(
            filter((event: Event): event is NavigationEnd => event instanceof NavigationEnd)
        );
    }

    /**
     * Navigates to the first portlet
     *
     * @memberof DotNavigationService
     */
    goToFirstPortlet(): Promise<boolean> {
        const firstMenuItem = this.#globalStore.firstMenuItem();

        if (!firstMenuItem) {
            return Promise.resolve(false);
        }

        return this.dotRouterService
            .gotoPortlet(firstMenuItem.menuLink, {
                queryParams: { mId: firstMenuItem.parentMenuId.substring(0, 4) }
            })
            .then((isRouted: boolean) => {
                if (!isRouted) {
                    this.reloadIframePage();
                }

                return isRouted;
            });
    }

    private reloadIframePage(): void {
        if (this.router.url.indexOf('c/') > -1) {
            this.dotIframeService.reload();
        }
    }

    /**
     * Starts a menu request and emits how it ended (`ready` or `error`). The wait is attached before
     * the request starts so an answer that arrives at once is not missed.
     */
    #reloadMenu(): Observable<MenuStatus> {
        return new Observable<MenuStatus>((subscriber) => {
            const subscription = this.#globalStore.whenSettled().subscribe(subscriber);
            this.#globalStore.reload();

            return subscription;
        });
    }
}
