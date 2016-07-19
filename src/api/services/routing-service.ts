import {Routes} from '@ngrx/router';
import { Observable } from 'rxjs/Rx';

import {RuleEngineContainer} from '../../view/components/rule-engine/rule-engine.container';
import {IframeLegacyComponent} from '../../view/components/common/iframe-legacy/IframeLegacyComponent';

export class RoutingService {

    private menus: Array<any>;

    private mapComponents;

    constructor() {
        this.mapComponents = {
            'RULES_ENGINE_PORTLET': RuleEngineContainer,
        };
    }

   public getRoutes(): Observable<any> {
        return Observable.create(observer => {
            this.getMenus().subscribe((navigationItems) => {
                // TODO: do this more elegant
                // TODO: this is bad, we shouldn't be create the route here, a service should only return the data.
                let routes: Routes = [];
                let mapPaths = {};
                if (navigationItems.errors.length > 0) {
                    console.log(navigationItems.errors[0].message);
                }else {
                    navigationItems.entity.forEach((item) => {
                        item.menuItems.forEach(subMenuItem => {
                            if (subMenuItem.angular) {
                                routes.push({
                                    component: this.mapComponents[subMenuItem.id],
                                    path: subMenuItem.url,
                                });
                            } else {
                                mapPaths[subMenuItem.id] = subMenuItem.url + '&in_frame=true&frame=detailFrame';
                            }
                        });
                    });
                }
                routes.push({
                    component: IframeLegacyComponent,
                    path: '/portlet/:id',
                });
                observer.next({
                    menuItems: {
                        mapPaths: mapPaths,
                        navigationItems: navigationItems.entity,
                    },
                    routes: routes,
                });
                observer.complete();
            });
        });

   }

   private getMenus(): Observable<any> {

        return Observable.create(observer => {
            let oReq = new XMLHttpRequest();

            oReq.onreadystatechange = (() => {
                if (oReq.status === 401) {
                    // if the user is not loggedIn will be here ;
                    observer.next(JSON.parse('{"errors":[],"entity":[]}'));
                    observer.complete();
                }else if (oReq.status === 400 || oReq.status === 500) {
                    console.log('Error ' + oReq.status + ': ' + oReq.statusText);
                }else if (oReq.readyState === XMLHttpRequest.DONE) {
                    observer.next(JSON.parse(oReq.response));
                    observer.complete();
                }
            });
            oReq.open('GET', '/api/v1/core_web/menu');
            oReq.send();
        });
   }

 }
