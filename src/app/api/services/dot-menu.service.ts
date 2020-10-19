import {
    filter,
    refCount,
    defaultIfEmpty,
    map,
    pluck,
    find,
    mergeMap,
    first,
    publishLast
} from 'rxjs/operators';
import { CoreWebService } from 'dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DotMenu, DotMenuItem } from '@models/navigation';

@Injectable()
export class DotMenuService {
    menu$: Observable<DotMenu[]>;

    private urlMenus = 'v1/menu';

    constructor(private coreWebService: CoreWebService) {}

    /**
     * Get the url for the iframe porlet from the menu object
     *
     * @param string id
     * @returns Observable<string>
     * @memberof DotMenuService
     */
    getUrlById(id: string): Observable<string> {
        return this.getMenuItems().pipe(
            filter((res: any) => !res.angular && res.id === id),
            first(),
            pluck('url')
        );
    }

    /**
     * Check if a portlet exist in the current loaded menu
     *
     * @param string url
     * @returns Observable<boolean>
     * @memberof DotMenuService
     */
    isPortletInMenu(menuId: string): Observable<boolean> {
        return this.getMenuItems().pipe(
            pluck('id'),
            map((id: string) => menuId === id),
            filter((val) => !!val),
            defaultIfEmpty(false)
        );
    }

    /**
     * Load and set menu from endpoint
     *
     * @returns Observable<DotMenu[]>
     * @memberof DotMenuService
     */
    loadMenu(): Observable<DotMenu[]> {
        if (!this.menu$) {
            this.menu$ = this.coreWebService
                .requestView({
                    url: this.urlMenus
                })
                .pipe(
                    publishLast(),
                    refCount(),
                    pluck('entity')
                );
        }

        return this.menu$;
    }

    /**
     * Clear the "cache" in the menu and reloads
     *
     * @returns Observable<DotMenu[]>
     * @memberof DotMenuService
     */
    reloadMenu(): Observable<DotMenu[]> {
        this.menu$ = null;
        return this.loadMenu();
    }

    /**
     * Return the DotMenu's ID of a Portlet
     * @param portletId MenuItems id
     */
    getDotMenuId(portletId: string): Observable<string> {
        return this.loadMenu().pipe(
            mergeMap((menus: DotMenu[]) => menus),
            find((menu: DotMenu) => menu.menuItems.some((menuItem) => menuItem.id === portletId)),
            map((menu: DotMenu) => menu.id)
        );
    }

    private getMenuItems(): Observable<DotMenuItem> {
        return this.loadMenu().pipe(
            mergeMap((menus: DotMenu[]) => menus),
            mergeMap((menu: DotMenu) => menu.menuItems)
        );
    }
}
