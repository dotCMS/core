import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { defaultIfEmpty, filter, find, map, mergeMap, pluck, shareReplay } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-js';
import { DotMenu, DotMenuItem } from '@dotcms/dotcms-models';

@Injectable()
export class DotMenuService {
    menu$: Observable<DotMenu[]>;

    private urlMenus = '/api/v1/menu';
    private readonly http = inject(HttpClient);

    /**
     * Get the url for the iframe porlet from the menu object
     *
     * @param string id
     * @returns Observable<string>
     * @memberof DotMenuService
     */
    getUrlById(id: string): Observable<string> {
        return this.getMenuItems().pipe(
            filter((res: DotMenuItem) => !res.angular && res.id === id),
            pluck('url'),
            defaultIfEmpty('')
        );
    }

    /**
     * Check if a portlet exist in the current loaded menu
     *
     * @param string url
     * @returns Observable<boolean>
     * @memberof DotMenuService
     */
    isPortletInMenu(menuId: string, checkJSPPortlet = false): Observable<boolean> {
        return this.getMenuItems().pipe(
            map(({ id, angular }) => {
                const idMatch = id === menuId;
                // Check if the JSP Portlet is in the menu and hasn't been migrated to Angular
                if (checkJSPPortlet) {
                    return idMatch && !angular;
                }

                return idMatch;
            }),
            filter((val) => !!val),
            defaultIfEmpty(false)
        );
    }

    /**
     * Load and set menu from endpoint
     * @param boolean force
     * @returns Observable<DotMenu[]>
     * @memberof DotMenuService
     */
    loadMenu(force = false): Observable<DotMenu[]> {
        if (!this.menu$ || force) {
            this.menu$ = this.http
                .get<DotCMSResponse<DotMenu>>(this.urlMenus)
                .pipe(shareReplay({ bufferSize: 1, refCount: true }), pluck('entity'));
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
