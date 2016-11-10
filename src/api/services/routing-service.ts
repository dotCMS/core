import {ApiRoot} from '../persistence/ApiRoot';
import {CoreWebService} from './core-web-service';
import {Injectable} from '@angular/core';
import {LoginService} from './login-service';
import {Observable} from 'rxjs/Rx';
import {DotcmsEventsService} from './dotcms-events-service';

import {RequestMethod, Http} from '@angular/http';
import {DotRouterService} from './dot-router-service';
import {Subject} from 'rxjs/Subject';

@Injectable()
export class RoutingService {
    private _menusChange$: Subject<Menu[]> = new Subject();
    private menus: Menu[];
    private urlMenus: string;
    private portlets: Map<string, string>;
    private _currentPortletId: string;

    // TODO: I think we should be able to remove the routing injection
    constructor(loginService: LoginService, private router: DotRouterService,
                private coreWebService: CoreWebService, dotcmsEventsService: DotcmsEventsService) {

        this.urlMenus = 'v1/CORE_WEB/menu';
        this.portlets = new Map();

        loginService.watchUser(this.loadMenus.bind(this));

        dotcmsEventsService.subscribeTo('UPDATE_PORTLET_LAYOUTS').subscribe( () => {
            this.loadMenus();
        });
    }

    get currentPortletId(): string{
        return this._currentPortletId;
    }

    get currentMenu(): Menu[] {
        return this.menus;
    }

    get menusChange$(): Observable<Menu[]> {
        return this._menusChange$.asObservable();
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
        let urlSplit = url.split('/');
        let id = urlSplit[urlSplit.length - 1];
        return this.portlets.has(id);
    }

    public setCurrentPortlet(url: string): void {
        let urlSplit = url.split('/');
        let id = urlSplit[urlSplit.length - 1];
        this._currentPortletId = id;
    }

    public setMenus(menus: Menu[]): void {
        this.menus = menus;

        if (this.menus.length) {
            this.portlets = new Map();

            for (let i = 0; i < this.menus.length; i++) {
                let menu = this.menus[i];
                for (let k = 0; k < menu.menuItems.length; k++) {
                    let subMenuItem = menu.menuItems[k];
                    if (subMenuItem.angular) {
                        subMenuItem.url = '/dotCMS' + subMenuItem.url;
                        this.portlets.set(subMenuItem.id, subMenuItem.url);
                    } else {
                        this.portlets.set(subMenuItem.id, subMenuItem.url);
                    }
                }
            }

            this._menusChange$.next(this.menus);
        }
    }

    private loadMenus(): void {
        this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: this.urlMenus,
        }).subscribe(response => {
            this.setMenus(response.entity);
        }, error => this._menusChange$.error(error));
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
}