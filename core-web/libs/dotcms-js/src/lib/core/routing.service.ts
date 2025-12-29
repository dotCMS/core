import { Observable, Subject } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-models';

import { DotRouterService } from './dot-router.service';
import { DotcmsEventsService } from './dotcms-events.service';
import { LoginService } from './login.service';

@Injectable()
export class RoutingService {
    private router = inject(DotRouterService);
    private http = inject(HttpClient);

    private _menusChange$: Subject<Menu[]> = new Subject();
    private menus: Menu[];
    private urlMenus = '/api/v1/CORE_WEB/menu';
    private portlets: Map<string, string>;
    private _currentPortletId: string;

    private _portletUrlSource$ = new Subject<string>();
    private _currentPortlet$ = new Subject<string>();

    // TODO: I think we should be able to remove the routing injection
    constructor() {
        const loginService = inject(LoginService);
        const dotcmsEventsService = inject(DotcmsEventsService);

        this.portlets = new Map();

        loginService.watchUser(this.loadMenus.bind(this));
        dotcmsEventsService
            .subscribeTo('UPDATE_PORTLET_LAYOUTS')
            .subscribe(this.loadMenus.bind(this));
    }

    get currentPortletId(): string {
        return this._currentPortletId;
    }

    get currentMenu(): Menu[] {
        return this.menus;
    }

    get menusChange$(): Observable<Menu[]> {
        return this._menusChange$.asObservable();
    }

    get portletUrl$(): Observable<string> {
        return this._portletUrlSource$.asObservable();
    }

    get firstPortlet(): string {
        const porlets = this.portlets.entries().next().value;

        return porlets ? porlets[0] : null;
    }

    public addPortletURL(portletId: string, url: string): void {
        this.portlets.set(portletId.replace(' ', '_'), url);
    }

    public getPortletURL(portletId: string): string {
        return this.portlets.get(portletId);
    }

    public goToPortlet(portletId: string): void {
        this.router.gotoPortlet(portletId);
        this._currentPortletId = portletId;
    }

    public isPortlet(url: string): boolean {
        let id = this.getPortletId(url);
        if (id.indexOf('?') >= 0) {
            id = id.substr(0, id.indexOf('?'));
        }

        return this.portlets.has(id);
    }

    public setCurrentPortlet(url: string): void {
        let id = this.getPortletId(url);
        if (id.indexOf('?') >= 0) {
            id = id.substr(0, id.indexOf('?'));
        }

        this._currentPortletId = id;
        this._currentPortlet$.next(id);
    }

    get currentPortlet$(): Observable<string> {
        return this._currentPortlet$;
    }

    public setMenus(menus: Menu[]): void {
        this.menus = menus;

        if (this.menus.length) {
            this.portlets = new Map();

            for (let i = 0; i < this.menus.length; i++) {
                const menu = this.menus[i];

                for (let k = 0; k < menu.menuItems.length; k++) {
                    const subMenuItem = menu.menuItems[k];
                    if (subMenuItem.angular) {
                        this.portlets.set(subMenuItem.id, subMenuItem.url);
                    } else {
                        subMenuItem.menuLink = '/c/' + subMenuItem.id;
                        this.portlets.set(subMenuItem.id, subMenuItem.url);
                    }
                }
            }

            this._menusChange$.next(this.menus);
        }
    }

    /**
     * Refresh the portlet displayed. with the
     * @param url portlet url
     */
    public changeRefreshPortlet(url: string): void {
        const portletId = this.getPortletId(url);
        if (portletId === this.currentPortletId) {
            this._portletUrlSource$.next(url);
        }
    }

    private loadMenus(): void {
        this.http
            .get<DotCMSResponse<Menu[]>>(this.urlMenus)
            .pipe(map((response) => response.entity))
            .subscribe({
                next: (menus) => this.setMenus(menus),
                error: (error) => this._menusChange$.error(error)
            });
    }

    private getPortletId(url: string): string {
        const urlSplit = url.split('/');

        return urlSplit[urlSplit.length - 1];
    }
}

export interface Menu {
    tabDescription: string;
    tabName: string;
    url: string;
    menuItems: MenuItem[];
}

export interface MenuItem {
    ajax: boolean;
    angular: boolean;
    id: string;
    name: string;
    url: string;
    menuLink: string;
}
