import { Routes } from '@ngrx/router';
import { Observable } from 'rxjs/Rx'

import {RuleEngineContainer} from '../../view/components/rule-engine/rule-engine.container';
import {IframeLegacyComponent} from '../../view/components/common/iframe-legacy/IframeLegacyComponent';

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
                if(oReq.status == 500){
                    //if the user is not loggedIn will be here ;
                    observer.next(JSON.parse("[]"));
                    observer.complete();
                }else if (oReq.readyState === XMLHttpRequest.DONE) {
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
                // TODO: this is bad, we shouldn't be create the route here, a service should only return the data.
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
                            mapPaths[subMenuItem.id] = subMenuItem.url + '&in_frame=true&frame=detailFrame';
                        }
                    })
                });
                routes.push({
                    path: '/portlet/:id',
                    component: IframeLegacyComponent,
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