import { PlatformLocation } from '@angular/common';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { DotMenu, DotMenuItem } from '../../../shared/models/navigation';
import { DotMenuService } from '../../../api/services/dot-menu.service';
import { DotRouterService } from '../../../api/services/dot-router-service';
import { LoginService } from 'dotcms-js/dotcms-js';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { DotcmsEventsService } from 'dotcms-js/core/dotcms-events.service';
import { Auth } from 'dotcms-js/core/login.service';

@Injectable()
export class DotNavigationService {
    items$: BehaviorSubject<DotMenu[]> = new BehaviorSubject([]);

    constructor(
        private dotMenuService: DotMenuService,
        private dotRouterService: DotRouterService,
        private dotcmsEventsService: DotcmsEventsService,
        private loginService: LoginService,
        private location: PlatformLocation
    ) {
        this.dotMenuService.loadMenu().subscribe((menu: DotMenu[]) => {
            this.setMenu(menu);
        });

        this.dotcmsEventsService.subscribeTo('UPDATE_PORTLET_LAYOUTS').subscribe(() => {
            this.reloadNavigation();
        });

        this.location.onPopState((popStateEvent: any) => {
            if (this.isHashHome(popStateEvent.target.location.hash)) {
                this.goToFirstPortlet(true);
            }
        });

        /*
            When the browser refresh the auth$ triggers for the "first time" and because of that
            on page reload we are doing 2 requests to menu enpoint.
        */
        this.loginService.auth$.subscribe((auth: Auth) => {
            if (auth.loginAsUser || auth.user) {
                this.reloadNavigation();
            }
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
     * Reloads and return a new version of the menu
     *
     * @returns {Observable<DotMenu[]>}
     * @memberof DotNavigationService
     */
    reloadNavigation(): void {
        this.dotMenuService.reloadMenu().subscribe((menu: DotMenu[]) => {
            this.dotMenuService
                .isPortletInMenu(
                    this.dotRouterService.currentPortlet.id ||
                    this.dotRouterService.getPortletId(this.location.hash)
                )
                .subscribe((isPortletInMenu: boolean) => {
                    if (!isPortletInMenu) {
                        this.goToFirstPortlet().then(res => {
                            this.setMenu(menu);
                        });
                    } else {
                        this.setMenu(menu);
                    }
                });
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
        return this.dotMenuService
            .loadMenu()
            .map((menus: DotMenu[]) => this.extractFirtsMenuLink(menus));
    }

    private getMenuLink(menuItemId: string): string {
        return `/c/${menuItemId}`;
    }

    private isFirstMenuActive(currentUrl: string, index: number): boolean {
        return currentUrl === '#/' && index === 0;
    }

    private isHashHome(hash: string): boolean {
        return hash === '#/c' || hash === '#/c/' || hash === '' || hash === '#/';
    }

    private isMenuItemCurrentUrl(currentUrl: string, menuItemId: string): boolean {
        return this.dotRouterService.getPortletId(currentUrl) === menuItemId;
    }

    private setMenu(menu: DotMenu[]) {
        this.items$.next(this.formatMenuItems(menu));
    }
}
