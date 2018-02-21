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
import { DotRouterService } from '../../../api/services/dot-router-service';

@Injectable()
export class DotNavigationService {
    items$: BehaviorSubject<DotMenu[]> = new BehaviorSubject([]);

    constructor(
        private dotMenuService: DotMenuService,
        private dotRouterService: DotRouterService,
        private dotcmsEventsService: DotcmsEventsService,
        private loginService: LoginService,
        private location: PlatformLocation,
        private router: Router
    ) {
        this.router.events
            .filter((event) => event instanceof NavigationEnd && !this.dotRouterService.isPublicPage())
            .take(1)
            .subscribe((_event: NavigationEnd) => {
                this.dotMenuService.loadMenu().subscribe((menu: DotMenu[]) => {
                    this.setMenu(menu);
                });
            });

        this.dotcmsEventsService.subscribeTo('UPDATE_PORTLET_LAYOUTS').subscribe(() => {
            this.reloadNavigation();
        });

        this.loginService.auth$.filter((auth: Auth) => !!(auth.loginAsUser || auth.user)).subscribe((auth: Auth) => {
            this.reloadNavigation().subscribe((isPortletInMenu: boolean) => {
                const isUserRedirect = auth.user['editModeUrl'] || this.dotRouterService.previousSavedURL;

                if (isUserRedirect) {
                    this.userCustomRedirect(auth.user['editModeUrl']);
                } else if (!isPortletInMenu) {
                    this.goToFirstPortlet();
                }
            });
        });
    }

    /**
     * Navigates to the first portlet
     *
     * @memberof DotNavigationService
     */
    goToFirstPortlet(replaceUrl?: boolean): Promise<boolean> {
        return this.getFirstMenuLink()
            .map((link: string) => {
                return this.dotRouterService.gotoPortlet(link, replaceUrl);
            })
            .toPromise()
            .then((isRouted: Promise<boolean>) => isRouted);
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
    reloadNavigation(): Observable<boolean> {
        return this.dotMenuService.reloadMenu().mergeMap((menu: DotMenu[]) => {
            this.setMenu(menu);

            return this.dotMenuService.isPortletInMenu(
                this.dotRouterService.currentPortlet.id || this.dotRouterService.getPortletId(this.location.hash)
            );
        });
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

                if (
                    this.isFirstMenuActive(currentUrl, menuIndex) ||
                    this.isMenuItemCurrentUrl(currentUrl, menuItem.id)
                ) {
                    menuGroup.isOpen = true;
                }
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

    private userCustomRedirect(editModeUrl?: string): void {
        if (editModeUrl) {
            this.dotRouterService.gotoPortlet(`edit-page/content?url=${editModeUrl}`, true);
        } else if (this.dotRouterService.previousSavedURL) {
            this.dotRouterService.gotoPortlet(this.dotRouterService.previousSavedURL, true).then((res: boolean) => {
                this.dotRouterService.previousSavedURL = res ? null : this.dotRouterService.previousSavedURL;
            });
        }
    }
}
