import { Routes } from '@ngrx/router';
import { Observable } from 'rxjs/Rx'

import {RuleEngineContainer} from '../../view/components/rule-engine/rule-engine.container';
import {IframeComponent} from '../../view/components/IframeComponent';

export class RoutingService {

    private menus:any[];

    private mapComponents;

    constructor() {
        this.mapComponents = {
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
                let mapPaths = {};
                navigationItems.forEach((item) => {
                    item.menuItems.forEach(subMenuItem => {
                        if (subMenuItem.angular) {
                            routes.push({
                                path: subMenuItem.url,
                                component: this.mapComponents[subMenuItem.id]
                            });
                        } else {
                            mapPaths[subMenuItem.id] = subMenuItem.url+'&in_frame=true&frame=detailFrame';
                        }
                    })
                });
                routes.push({
                    path: '/portlet/:id',
                    component: IframeComponent,
                });
                observer.next({
                    menuItems: {
                        navigationItems: navigationItems,
                        mapPaths: mapPaths
                    },
                    routes: routes
                });
                observer.complete();
            })
        });

    }
}