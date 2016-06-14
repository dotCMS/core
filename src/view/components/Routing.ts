import { Routes } from '@ngrx/router';

// import {ANGULAR_PORTLET3} from "./ANGULAR_PORTLET3";
// import {ANGULAR_PORTLET4} from "./ANGULAR_PORTLET4";
import {RedirectComponent} from "./RedirectComponent";
import {RuleEngineContainer} from "./rule-engine/rule-engine.container";

var $ = window['$']

export class Routing{

    private menus:any[];

    private static mapComponents = {
        // 'ANGULAR_PORTLET3': ANGULAR_PORTLET3,
        // 'ANGULAR_PORTLET4': ANGULAR_PORTLET4,
        'RuleEngineContainer': RuleEngineContainer
    };

    constructor(){

    }

    private getMenus(){

        if (this.menus){
            return Promise.resolve( this.menus );
        } else {
            return new Promise((resolve, reject) => {
                // TODO: this needs to be done with http
                $.get('/api/core_web/menu', response => {
                    this.menus = <any[]> response;
                    resolve(this.menus);
                });
            });
        }
    }

    public getRoutes(){
        return new Promise( (resolve, reject) => {
            this.getMenus().then( (menus) => {
                let routes : Routes = [];

                menus.forEach( menu => {
                    menu.menuItems.forEach( menuItem => {
                        console.log('menuItem', menuItem);
                        if (menuItem.angular) {
                            routes.push({path: menuItem.url, component: Routing.mapComponents[menuItem.id]});
                        }
                    })
                });

                routes.push({path: '/html/ng', component: RedirectComponent});

                console.log('routes', routes);
                resolve({menuItems: this.menus, routes: routes});
            });
        });
    }
}