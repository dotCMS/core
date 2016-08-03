import {Routes} from '@ngrx/router';
import { Observable } from 'rxjs/Rx';

import {RuleEngineContainer} from '../../view/components/rule-engine/rule-engine.container';
import {IframeLegacyComponent} from '../../view/components/common/iframe-legacy/IframeLegacyComponent';
import {LoginComponent} from "../../view/components/common/login/login-component/login-component";
import {LoginPageComponent} from "../../view/components/common/login/login-page-component";
import {FogotPasswordComponent} from "../../view/components/common/login/fogot-password-component/fogot-password-component";
import {MainComponent} from "../../view/components/common/main-component/main-component";
import {ResetPasswordComponent} from "../../view/components/common/login/reset-password-component/reset-password-component";
import {FogotPasswordContainer} from "../../view/components/common/login/fogot-password-component/fogot-password-container";
import {LoginContainer} from "../../view/components/common/login/login-component/login-container";
import {ResetPasswordContainer} from "../../view/components/common/login/reset-password-component/reset-password-container";

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
                let loginRoutes =  this.getLoginRoutes();
                let mainRoutes = { path: '/main',
                    component: MainComponent,
                    children: []
                };
                let routes: Routes = [ mainRoutes, loginRoutes ];
                if (navigationItems.errors.length > 0) {
                    console.log(navigationItems.errors[0].message);
                }else {
                    navigationItems.entity.forEach((item) => {
                        item.menuItems.forEach(subMenuItem => {
                            if (subMenuItem.angular) {
                                mainRoutes.children.push({
                                    component: this.mapComponents[subMenuItem.id],
                                    path: subMenuItem.url,
                                });
                            } else {
                                mainRoutes.children[subMenuItem.id] = subMenuItem.url + '&in_frame=true&frame=detailFrame';
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
                        mapPaths: mainRoutes,
                        navigationItems: navigationItems.entity,
                    },
                    routes: routes,
                });
                observer.complete();
            });
        });

   }

    private getLoginRoutes():any {
        return {
            path: '/login',
            component: LoginPageComponent,
            children: [
                {
                    path: 'fogotPassword',
                    component: FogotPasswordContainer
                },
                {
                    path: 'login',
                    component: LoginContainer
                },
                {
                    path: 'resetPassword/:userId',
                    component: ResetPasswordContainer
                }
            ]
        };
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
