import { Routes } from '@ngrx/router';
import { Observable } from 'rxjs/Rx';
import { Observer } from 'rxjs/Observer';
import { RuleEngineContainer } from '../../view/components/rule-engine/rule-engine.container';
import { Injectable, Inject } from '@angular/core';
import { PatternLibrary } from '../../view/components/common/pattern-library/pattern-library';
import {Subject} from 'rxjs/Subject';
import {LoginService} from './login-service';
import {CoreWebService} from './core-web-service';
import {RequestMethod, Http} from '@angular/http';
import {ApiRoot} from '../persistence/ApiRoot';
import {Router} from '@ngrx/router';

@Injectable()
export class RoutingService extends CoreWebService{

    private _menusChange$: Subject<Menu[]> = new Subject();
    private menus: Menu[];

    private urlMenus: string;

    private portlets: Map<string, string>;

    private mapComponents = {
        'RULES_ENGINE_PORTLET': RuleEngineContainer,
        'PL': PatternLibrary
    };

    constructor(apiRoot: ApiRoot, http: Http, @Inject('routes') private routes: Routes[ ],  loginService: LoginService,
                private router: Router) {
        super(apiRoot, http);

        this.urlMenus = `${apiRoot.baseUrl}api/v1/appconfiguration`;

        if (loginService.loginUser) {
            this.loadMenus();
        }

        loginService.loginUser$.subscribe(user => this.loadMenus());

        this.portlets = new Map();
    }

    get menusChange$(): Observable<Menu[]> {
        return this._menusChange$.asObservable();
    }

    public setMenus(menus: Menu[]): void {
        this.menus = menus;

        if (this.menus.length) {
            // TODO: do this more elegant
            // TODO: this is bad, we shouldn't be create the route here, a service should only return the data.
            let mainRoutes = this.routes[0];
            mainRoutes.children.slice(1, mainRoutes.children.length);

            this.menus[0].menuItems.unshift({
                ajax: false,
                angular: true,
                id: 'PL',
                name: 'Pattern Library',
                url: '/pl',
            });

            for (let i = 0; i < this.menus.length; i++) {
                let menu = this.menus[i];
                for (let k = 0; k < menu.menuItems.length; k++) {
                    let subMenuItem = menu.menuItems[k];

                    if (subMenuItem.angular) {
                        mainRoutes.children.push({
                            component: this.mapComponents[subMenuItem.id],
                            path: subMenuItem.url,
                        });
                        subMenuItem.url = 'dotCMS' + subMenuItem.url;
                    } else {
                        this.portlets.set(subMenuItem.id, subMenuItem.url);
                    }
                }
            }

            this._menusChange$.next(this.menus);
            console.log('routes', this.routes);
        }
    }

    public getPortletURL(portletId: string): string {
        return this.portlets.get(portletId);
    }

    public addPortletURL(portletId: string, url: string): void {
        this.portlets.set(portletId.replace(' ', '_'), url);
    }

    public goToPortlet(portletId: string): void {
        this.router.go(`dotCMS/portlet/${portletId.replace(' ', '_')}`);
    }

    private loadMenus(): void {
        this.requestView({
            method: RequestMethod.Get,
            url: this.urlMenus,
        }).subscribe(response => this.setMenus(response.entity.menu),
            error => this._menusChange$.error(error));
    }

    get currentMenu(): Menu[]{
        return this.menus;
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