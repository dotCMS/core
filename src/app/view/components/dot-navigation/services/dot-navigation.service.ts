import { Injectable } from '@angular/core';
import { Router, NavigationEnd, Event } from '@angular/router';

import { Observable } from 'rxjs/Observable';

import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { filter, switchMap, map, flatMap, toArray, tap } from 'rxjs/operators';

import { Auth } from 'dotcms-js/core/login.service';
import { DotcmsEventsService, LoginService } from 'dotcms-js/dotcms-js';

import { DotMenu, DotMenuItem } from '../../../../shared/models/navigation';
import { DotMenuService } from '../../../../api/services/dot-menu.service';
import { DotRouterService } from '../../../../api/services/dot-router/dot-router.service';
import { DotIframeService } from '../../_common/iframe/service/dot-iframe/dot-iframe.service';

const replaceSectionsMap = {
    'edit-page': 'site-browser'
};

const replaceIdForNonMenuSection = id => {
    return replaceSectionsMap[id];
};

const setActiveItems = (id: string) => (source: Observable<DotMenu[]>) => {
    id = replaceIdForNonMenuSection(id) || id;

    return source.pipe(
        flatMap((menu: DotMenu[]) => menu),
        map((menu: DotMenu) => getActiveUpdatedMenu(menu, id)),
        toArray()
    );
};

const getActiveUpdatedMenu = (menu: DotMenu, id: string) => {
    let isActive = false;

    menu.menuItems.forEach((item: DotMenuItem) => {
        if (item.id === id) {
            item.active = true;
            isActive = true;
        } else {
            item.active = false;
        }
    });

    menu.active = menu.isOpen = isActive;
    return menu;
};

@Injectable()
export class DotNavigationService {
    _items$: BehaviorSubject<DotMenu[]> = new BehaviorSubject([]);

    constructor(
        private dotMenuService: DotMenuService,
        private dotRouterService: DotRouterService,
        private dotcmsEventsService: DotcmsEventsService,
        private loginService: LoginService,
        private dotIframeService: DotIframeService,
        private router: Router
    ) {
        this.dotMenuService.loadMenu().subscribe((menus: DotMenu[]) => {
            this.setMenu(menus);
        });

        this.onNavigationEnd()
            .pipe(
                map((event: NavigationEnd) => this.getTheUrlId(event.url)),
                switchMap((id: string) => this.dotMenuService.loadMenu().pipe(setActiveItems(id)))
            )
            .subscribe((menus: DotMenu[]) => {
                this.setMenu(menus);
            });

        this.dotcmsEventsService.subscribeTo('UPDATE_PORTLET_LAYOUTS').subscribe(() => {
            this.reloadNavigation().subscribe((menus: DotMenu[]) => {
                this.setMenu(menus);
            });
        });

        this.loginService.auth$
            .pipe(filter((auth: Auth) => !!(auth.loginAsUser || auth.user)))
            .subscribe(() => {
                this.goToFirstPortlet();
            });
    }

    get items$(): Observable<DotMenu[]> {
        return this._items$.asObservable();
    }

    /**
     * Close all the sections in the menu
     *
     * @memberof DotNavigationService
     */
    collapseMenu(): void {
        const closedMenu: DotMenu[] = this._items$.getValue().map((menu: DotMenu) => {
            menu.isOpen = false;
            return menu;
        });
        this.setMenu(closedMenu);
    }

    /**
     * Open menu section that have menulink active
     *
     * @memberof DotNavigationService
     */
    expandMenu(): void {
        const expandedMenu: DotMenu[] = this._items$.getValue().map((menu: DotMenu) => {
            let isActive = false;

            menu.menuItems.forEach((item: DotMenuItem) => {
                if (item.active) {
                    isActive = true;
                }
            });
            menu.isOpen = isActive;
            return menu;
        });
        this.setMenu(expandedMenu);
    }

    /**
     * Navigate to portlet by id
     *
     * @param {string} url
     * @memberof DotNavigationService
     */
    goTo(url: string): void {
        this.dotRouterService.gotoPortlet(url);
    }

    /**
     * Navigates to the first portlet
     *
     * @memberof DotNavigationService
     */
    goToFirstPortlet(): Promise <boolean> {
        return this.getFirstMenuLink()
            .map((link: string) => this.dotRouterService.gotoPortlet(link))
            .toPromise()
            .then((isRouted: Promise<boolean>) => {
                if (!isRouted) {
                    this.reloadIframePage();
                }
                return isRouted;
            });
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
     * Set menu open base on the id of the menulink
     *
     * @param {string} id
     * @memberof DotNavigationService
     */
    setOpen(id: string): void {
        const updatedMenu: DotMenu[] = this._items$.getValue().map((menu: DotMenu) => {
            menu.isOpen = menu.isOpen ? false : id === menu.id;
            return menu;
        });
        this.setMenu(updatedMenu);
    }

    private addMenuLinks(menu: DotMenu[]): DotMenu[] {
        return menu.map((menuGroup: DotMenu) => {
            menuGroup.menuItems.forEach((menuItem: DotMenuItem) => {
                menuItem.menuLink = menuItem.angular ? menuItem.url : this.getLegacyPortletUrl(menuItem.id);
            });
            return menuGroup;
        });
    }

    private extractFirtsMenuLink(menus: DotMenu[]): string {
        const firstMenuItem: DotMenuItem = menus[0].menuItems[0];
        return firstMenuItem.angular ? firstMenuItem.url : this.getLegacyPortletUrl(firstMenuItem.id);
    }

    private getFirstMenuLink(): Observable <string> {
        return this.dotMenuService.loadMenu().map((menus: DotMenu[]) => this.extractFirtsMenuLink(menus));
    }

    private getLegacyPortletUrl(menuItemId: string): string {
        return `/c/${menuItemId}`;
    }

    private getTheUrlId(url: string): string {
        const urlSegments: string[] = url.split('/').filter((item: string) => item.length);
        return urlSegments[0] === 'c' ? urlSegments.pop() : urlSegments[0];
    }

    private onNavigationEnd(): Observable <Event> {
        return this.router.events.filter((event: Event) => event instanceof NavigationEnd);
    }

    private reloadIframePage(): void {
        if (this.router.url.indexOf('c/') > -1) {
            this.dotIframeService.reload();
        }
    }

    private reloadNavigation(): Observable <DotMenu[] > {
        return this.dotMenuService.reloadMenu().pipe(
            setActiveItems(this.dotRouterService.currentPortlet.id),
            tap((menus: DotMenu[]) => {
                this.setMenu(menus);
            })
        );
    }

    private setMenu(menu: DotMenu[]) {
        this._items$.next(this.addMenuLinks(menu));
    }
}
