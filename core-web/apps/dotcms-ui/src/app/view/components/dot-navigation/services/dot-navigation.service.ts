import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Event, NavigationEnd, Router } from '@angular/router';

import { filter, map, switchMap, take, tap } from 'rxjs/operators';

import { DotIframeService, DotRouterService } from '@dotcms/data-access';
import { Auth, DotcmsEventsService, LoginService } from '@dotcms/dotcms-js';
import { DotMenu } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotMenuService } from '../../../../api/services/dot-menu.service';

@Injectable()
export class DotNavigationService {
    private dotIframeService = inject(DotIframeService);
    private dotMenuService = inject(DotMenuService);
    private dotRouterService = inject(DotRouterService);
    private dotcmsEventsService = inject(DotcmsEventsService);
    private loginService = inject(LoginService);
    private router = inject(Router);
    private titleService = inject(Title);
    readonly #globalStore = inject(GlobalStore);
    private _appMainTitle: string;

    constructor() {
        this._appMainTitle = this.titleService.getTitle();

        // Load initial menu - store handles menu link processing and entity transformation
        this.dotMenuService.loadMenu().subscribe((menus: DotMenu[]) => {
            this.#globalStore.loadMenu(menus);

            if (this.dotRouterService.currentPortlet.id) {
                this.#globalStore.setActiveMenu({
                    portletId: this.dotRouterService.currentPortlet.id,
                    shortParentMenuId: this.dotRouterService.queryParams['mId'],
                    bookmark: true,
                    breadcrumbs: this.#globalStore.breadcrumbs()
                });
            }
        });

        // Handle navigation end events
        this.onNavigationEnd()
            .pipe(
                switchMap((event: NavigationEnd) => {
                    return this.dotMenuService.loadMenu().pipe(
                        tap(() => {
                            const pageTitle = this.#globalStore.getPageTitleByUrl(
                                event.urlAfterRedirects
                            );
                            if (pageTitle) {
                                this.titleService.setTitle(`${pageTitle} - ${this._appMainTitle}`);
                            }

                            // validation for bookmark links with missing or invalid mId query param SHOULD MBE REMOVED SOON
                            const queryParamsValid =
                                !this.dotRouterService.queryParams['mId'] &&
                                Object.keys(this.dotRouterService.queryParams).length === 0;

                            if (this.dotRouterService.currentPortlet.id) {
                                this.#globalStore.setActiveMenu({
                                    portletId: this.dotRouterService.currentPortlet.id,
                                    shortParentMenuId: this.dotRouterService.queryParams['mId'],
                                    bookmark: queryParamsValid,
                                    breadcrumbs: this.#globalStore.breadcrumbs()
                                });
                            }
                        }),
                        map(() => true)
                    );
                }),
                filter((menu) => !!menu)
            )
            .subscribe();

        // Handle portlet layout updates
        this.dotcmsEventsService.subscribeTo('UPDATE_PORTLET_LAYOUTS').subscribe(() => {
            this.dotMenuService
                .reloadMenu()
                .pipe(take(1))
                .subscribe((menus: DotMenu[]) => {
                    this.#globalStore.loadMenu(menus);

                    if (this.dotRouterService.currentPortlet.id) {
                        this.#globalStore.setActiveMenu({
                            portletId: this.dotRouterService.currentPortlet.id,
                            shortParentMenuId: this.dotRouterService.queryParams['mId']
                        });
                    }
                });
        });

        // Handle login/auth changes
        this.loginService.auth$
            .pipe(
                filter((auth: Auth) => !!(auth.loginAsUser || auth.user)),
                switchMap(() => this.dotMenuService.reloadMenu())
            )
            .subscribe((menus: DotMenu[]) => {
                this.#globalStore.loadMenu(menus);
                this.goToFirstPortlet();
            });
    }

    onNavigationEnd(): Observable<Event> {
        return this.router.events.pipe(filter((event: Event) => event instanceof NavigationEnd));
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
}
