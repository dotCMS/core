import { Injectable } from '@angular/core';
import { PlatformLocation } from '@angular/common';
import { Router, NavigationEnd } from '@angular/router';

import { Observable } from 'rxjs/Observable';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

import { Auth } from 'dotcms-js/core/login.service';
import { DotcmsEventsService } from 'dotcms-js/core/dotcms-events.service';
import { LoginService } from 'dotcms-js/dotcms-js';

import { DotMenu, DotMenuItem } from '../../../shared/models/navigation';
import { DotMenuService } from '../../../api/services/dot-menu.service';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';
import { DotIframeService } from '../_common/iframe/service/dot-iframe/dot-iframe.service';
import { filter, mergeMap } from 'rxjs/operators';

@Injectable()
export class DotNavigationService {
    items$: BehaviorSubject<DotMenu[]> = new BehaviorSubject([]);

    constructor(
        private dotMenuService: DotMenuService,
        private dotRouterService: DotRouterService,
        private dotcmsEventsService: DotcmsEventsService,
        private loginService: LoginService,
        private location: PlatformLocation,
        private dotIframeService: DotIframeService,
        private router: Router
    ) {
        this.router.events
            .filter(event => event instanceof NavigationEnd && !this.dotRouterService.isPublicPage())
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
            menuGroup.isOpen = false;
            menuGroup.menuItems.forEach((menuItem: DotMenuItem) => {
                menuItem.menuLink = menuItem.angular ? menuItem.url : this.getMenuLink(menuItem.id);
                menuGroup.isOpen =
                    menuGroup.isOpen || this.isFirstMenuActive(currentUrl, menuIndex) || this.isMenuItemCurrentUrl(currentUrl, menuItem.id);
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
        this.items$.next(this.formatMenuItems(menu));
    }
}
