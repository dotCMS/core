import { Routes } from '@ngrx/router';
import { Observable } from 'rxjs/Rx'

// import {ANGULAR_PORTLET3} from "./ANGULAR_PORTLET3";
// import {ANGULAR_PORTLET4} from "./ANGULAR_PORTLET4";
import {RedirectComponent} from "../../view/components/RedirectComponent";
import {RuleEngineContainer} from "../../view/components/rule-engine/rule-engine.container";

var $ = window['$'];

export class RoutingService {

    private menus:any[];

    private mapComponents;

    constructor() {
        this.mapComponents = {
            // 'ANGULAR_PORTLET3': ANGULAR_PORTLET3,
            // 'ANGULAR_PORTLET4': ANGULAR_PORTLET4,
            'RuleEngineContainer': RuleEngineContainer
        };
    }

    private getMenus() {

        return Observable.create(observer => {
            $.get('/api/core_web/menu', response => {
                this.menus = <any[]> response;
                observer.next(this.menus);
                observer.complete();
            });
        });
    }

    public getRoutes() {
        return Observable.create(observer => {
            this.getMenus().subscribe((items) => {
                // TODO: do this more elegant
                let routes : Routes = [];
                items.forEach((menu) => {
                    menu.menuItems.forEach(menuItem => {
                        if (menuItem.angular) {
                            routes.push({
                                path: menuItem.url,
                                component: this.mapComponents[menuItem.id]
                            });
                        }
                    })
                });
                routes.push({
                    path: '/html/ng',
                    component: RedirectComponent
                });
                observer.next({
                    menuItems: this.menus,
                    routes: routes
                });
                observer.complete();
            })
        });

    }
}