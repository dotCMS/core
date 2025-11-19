import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Event, NavigationEnd, Router } from '@angular/router';

import { filter, map, switchMap, take, tap } from 'rxjs/operators';

import { DotIframeService, DotRouterService } from '@dotcms/data-access';
import { Auth, DotcmsEventsService, LoginService } from '@dotcms/dotcms-js';
import { DotMenu, DotMenuItem } from '@dotcms/dotcms-models';
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

            this.#globalStore.setActiveMenu(
                this.dotRouterService.currentPortlet.id,
                this.router.getCurrentNavigation()?.extras?.state?.menuId ||
                    window.history?.state?.menuId
            );
        });

        // Handle navigation end events
        this.onNavigationEnd()
            .pipe(
                switchMap((event: NavigationEnd) => {
                    return this.dotMenuService.loadMenu().pipe(
                        tap((menu: DotMenu[]) => {
                            const pageTitle = this.getPageCurrentTitle(event.url, menu);
                            if (pageTitle) {
                                this.titleService.setTitle(`${pageTitle} - ${this._appMainTitle}`);
                            }

                            // Load menu and set active item based on current portlet and parent context
                            this.#globalStore.setActiveMenu(
                                this.dotRouterService.currentPortlet.id,
                                this.router.getCurrentNavigation().extras?.state?.menuId
                            );
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
                    this.#globalStore.setActiveMenu(
                        this.dotRouterService.currentPortlet.id,
                        this.router.getCurrentNavigation()?.extras?.state?.menuId,
                        menus
                    );
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
        return this.getFirstMenuLink()
            .pipe(
                map((link: string) => {
                    return this.dotRouterService.gotoPortlet(
                        link,
                        {},
                        this.router.getCurrentNavigation()?.extras.state?.menuId
                    );
                })
            )
            .toPromise()
            .then((isRouted: Promise<boolean>) => {
                if (!isRouted) {
                    this.reloadIframePage();
                }

                return isRouted;
            });
    }

    private extractFirtsMenuLink(menus: DotMenu[]): string {
        const firstMenuItem: DotMenuItem = menus[0].menuItems[0];

        return (
            firstMenuItem.menuLink ||
            (firstMenuItem.angular ? firstMenuItem.url : this.getLegacyPortletUrl(firstMenuItem.id))
        );
    }

    private getFirstMenuLink(): Observable<string> {
        return this.dotMenuService
            .loadMenu()
            .pipe(map((menus: DotMenu[]) => this.extractFirtsMenuLink(menus)));
    }

    private getLegacyPortletUrl(menuItemId: string): string {
        return `/c/${menuItemId}`;
    }

    private reloadIframePage(): void {
        if (this.router.url.indexOf('c/') > -1) {
            this.dotIframeService.reload();
        }
    }

    private getPageCurrentTitle(url: string, menu: DotMenu[]): string {
        let title = '';
        // Load menu to ensure entities are up to date
        this.#globalStore.loadMenu(menu);

        // Use flattened menu items from store (already processed)
        const flattedMenu = this.#globalStore
            .flattenMenuItems()
            .reduce((a, { label, menuLink }) => ({ ...a, [menuLink]: label }), {});

        Object.entries(flattedMenu).forEach(([menuLink, label]: [string, string]) => {
            title = url.indexOf(menuLink) >= 0 ? label : title;
        });

        return title;
    }
}
