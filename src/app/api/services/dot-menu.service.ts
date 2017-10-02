import { CoreWebService } from 'dotcms-js/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Rx';
import { RequestMethod } from '@angular/http';
import { DotMenu, DotMenuItem } from '../../shared/models/navigation';

@Injectable()
export class DotMenuService {
    menu$: Observable<DotMenu[]>;

    private urlMenus = 'v1/CORE_WEB/menu';

    constructor(private coreWebService: CoreWebService) {}

    /**
     * Get the url for the iframe porlet from the menu object
     *
     * @param {string} id
     * @returns {Observable<string>}
     * @memberof DotNavigationService
     */
    getUrlById(id: string): Observable<string> {
        return this.getMenuItems()
            .filter((res: any) => !res.angular && res.id === id)
            .first()
            .pluck('url');
    }

    /**
     * Check if a portlet exist in the current loaded menu
     *
     * @param {string} url
     * @returns {Observable<boolean>}
     * @memberof DotNavigationService
     */
    isPortletInMenu(menuId: string): Observable<boolean> {
        return this.getMenuItems()
            .pluck('id')
            .map((id: string) => menuId === id)
            .filter(val => !!val)
            .defaultIfEmpty(false);
    }

    /**
     * Load and set menu from endpoint
     *
     * @returns {Observable<DotMenu[]>}
     * @memberof DotNavigationService
     */
    loadMenu(): Observable<DotMenu[]> {
        if (!this.menu$) {
            this.menu$ = this.coreWebService
                .requestView({
                    method: RequestMethod.Get,
                    url: this.urlMenus
                })
                .publishLast()
                .refCount()
                .pluck('entity');
        }

        return this.menu$;
    }

    /**
     * Clear the "cache" in the menu and reloads
     *
     * @returns {Observable<DotMenu[]>}
     * @memberof DotNavigationService
     */
    reloadMenu(): Observable<DotMenu[]> {
        this.menu$ = null;
        return this.loadMenu();
    }

    private getMenuItems(): Observable<DotMenuItem> {
        return this.loadMenu()
            .flatMap((menus: DotMenu[]) => menus)
            .flatMap((menu: DotMenu) => menu.menuItems);
    }
}
