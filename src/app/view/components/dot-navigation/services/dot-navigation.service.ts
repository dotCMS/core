import { Injectable } from '@angular/core';
import { PlatformLocation } from '@angular/common';
import { Router, NavigationEnd, Event } from '@angular/router';

import { Observable } from 'rxjs/Observable';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { filter, mergeMap, take } from 'rxjs/operators';

import { Auth } from 'dotcms-js/core/login.service';
import { DotcmsEventsService, LoginService } from 'dotcms-js/dotcms-js';

import { DotMenu, DotMenuItem } from '../../../../shared/models/navigation';
import { DotMenuService } from '../../../../api/services/dot-menu.service';
import { DotRouterService } from '../../../../api/services/dot-router/dot-router.service';
import { DotIframeService } from '../../_common/iframe/service/dot-iframe/dot-iframe.service';

@Injectable()
export class DotNavigationService {
    _items$: BehaviorSubject<DotMenu[]> = new BehaviorSubject([]);

    constructor(
        private dotMenuService: DotMenuService,
        private dotRouterService: DotRouterService,
        private dotcmsEventsService: DotcmsEventsService,
        private loginService: LoginService,
        private location: PlatformLocation,
        private dotIframeService: DotIframeService,
        private router: Router
    ) {
        this.onNavigationEnd()
            .pipe(
                filter(() => !this.dotRouterService.isPublicPage()),
                take(1)
            )
            .subscribe((_event: NavigationEnd) => {
                this.dotMenuService.loadMenu().subscribe((menu: DotMenu[]) => {
                    this.setMenu(menu);
                });
            });

        this.dotcmsEventsService.subscribeTo('UPDATE_PORTLET_LAYOUTS').subscribe(() => {
            this.reloadNavigation();
        });

        this.loginService.auth$
            .pipe(
                filter((auth: Auth) => !!(auth.loginAsUser || auth.user)),
                mergeMap(() => {
                    return this.reloadNavigation().filter(() => !this.dotRouterService.previousSavedURL);
                })
            )
            .subscribe(() => {
                this.goToFirstPortlet();
            });
    }

    get items$(): Observable<DotMenu[]> {
        return this._items$.asObservable();
    }

    /**
     * Navigates to the first portlet
     *
     * @memberof DotNavigationService
     */
    goToFirstPortlet(): Promise<boolean> {
        return this.getFirstMenuLink()
            .map((link: string) => {
                return this.dotRouterService.gotoPortlet(link);
            })
            .toPromise()
            .then((isRouted: Promise<boolean>) => {
                if (!isRouted) {
                    this.reloadIframePage();
                }
                return isRouted;
            });
    }

    /**
     * Check if menu option is active
     *
     * @param {string} id
     * @returns {boolean}
     * @memberof DotNavigationService
     */
    isActive(id: string): boolean {
        return this.dotRouterService.currentPortlet.id === id;
    }

    /**
     * Emit event when navigation end
     *
     * @returns {Observable<Event>}
     * @memberof DotNavigationService
     */
    onNavigationEnd(): Observable<Event> {
        return this.router.events.filter((event: Event) => event instanceof NavigationEnd);
    }

    /**
     * Reload current portlet
     *
     * @param {string} id
     * @memberof DotNavigationService
     */
    reloadCurrentPortlet(id: string): void {
        if (this.dotRouterService.currentPortlet.id === id) {
            this.dotRouterService.reloadCurrentPortlet(id);
        }
    }

    /**
     * Reloads and return a new version of the menu and
     * decide where to go if the user do not have access to the current route.
     *
     * @returns {Observable<DotMenu[]>}
     * @memberof DotNavigationService
     */
    reloadNavigation(): Observable<DotMenu[]> {
        return this.dotMenuService.reloadMenu().do((menu: DotMenu[]) => {
            this.setMenu(menu);
        });
    }

    /**
     * Navigate to portlet
     * @param url
     */
    goTo(url: string): void {
        this.dotRouterService.gotoPortlet(url);
    }

    private reloadIframePage(): void {
        if (this.router.url.indexOf('c/') > -1) {
            this.dotIframeService.reload();
        }
    }

    private extractFirtsMenuLink(menus: DotMenu[]): string {
        const firstMenuItem: DotMenuItem = menus[0].menuItems[0];
        return firstMenuItem.angular ? firstMenuItem.url : this.getMenuLink(firstMenuItem.id);
    }

    private formatMenuItems(menu: DotMenu[]): DotMenu[] {
        const currentUrl = this.location.hash;

        return menu.map((menuGroup: DotMenu, menuIndex: number) => {
            menuGroup.menuItems.forEach((menuItem: DotMenuItem) => {
                menuItem.menuLink = menuItem.angular ? menuItem.url : this.getMenuLink(menuItem.id);
                menuGroup.isOpen =
                    menuGroup.isOpen || this.isFirstMenuActive(currentUrl, menuIndex) || this.isMenuItemCurrentUrl(currentUrl, menuItem.id);
                menuGroup.active = menuGroup.isOpen;
                menuItem.active = this.isMenuItemCurrentUrl(currentUrl, menuItem.id);
            });
            return menuGroup;
        });
    }

    private getFirstMenuLink(): Observable<string> {
        return this.dotMenuService.loadMenu().map((menus: DotMenu[]) => this.extractFirtsMenuLink(menus));
    }

    private getMenuLink(menuItemId: string): string {
        return `/c/${menuItemId}`;
    }

    private isFirstMenuActive(currentUrl: string, index: number): boolean {
        return currentUrl === '#/' && index === 0;
    }

    private isMenuItemCurrentUrl(currentUrl: string, menuItemId: string): boolean {
        return this.dotRouterService.getPortletId(currentUrl) === menuItemId;
    }

    private setMenu(menu: DotMenu[]) {
        this._items$.next(this.formatMenuItems(menu));
    }
}
