import { Routes } from '@ngrx/router';
import { Observable } from 'rxjs/Rx'

import {ANGULAR_PORTLET3} from '../../view/components/ANGULAR_PORTLET3';
import {ANGULAR_PORTLET4} from '../../view/components/ANGULAR_PORTLET4';
import {RedirectComponent} from '../../view/components/RedirectComponent';
import {RuleEngineContainer} from '../../view/components/rule-engine/rule-engine.container';

export class RoutingService {

    private menus:any[];

    private mapComponents;

    constructor() {
        this.mapComponents = {
            'ANGULAR_PORTLET3': ANGULAR_PORTLET3,
            'ANGULAR_PORTLET4': ANGULAR_PORTLET4,
            'RULES_ENGINE_PORTLET': RuleEngineContainer
        };
    }

    private getMenus() {

        return Observable.create(observer => {
            let oReq = new XMLHttpRequest();

            oReq.onreadystatechange = function() {
                if (oReq.readyState === XMLHttpRequest.DONE) {
                    observer.next(JSON.parse(oReq.response));
                    observer.complete();
                }
            }
            oReq.open('GET', '/api/core_web/menu');
            oReq.send();
        });
    }

    public getRoutes() {
        return Observable.create(observer => {
            this.getMenus().subscribe((navigationItems) => {
                // TODO: do this more elegant
                let routes : Routes = [];
                navigationItems.forEach((item) => {
                    item.menuItems.forEach(subMenuItem => {
                        if (subMenuItem.angular) {
                            routes.push({
                                path: subMenuItem.url,
                                component: this.mapComponents[subMenuItem.id]
                            });
                        }
                    })
                });
                routes.push({
                    path: '/html/ng/p/',
                    component: RedirectComponent
                });
                observer.next({
                    menuItems: navigationItems,
                    routes: routes
                });
                observer.complete();
            })
        });

    }
}