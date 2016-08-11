import { Routes } from '@ngrx/router';
import { Observable } from 'rxjs/Rx';
import { Observer } from "rxjs/Observer";
import { RuleEngineContainer } from '../../view/components/rule-engine/rule-engine.container';
import { Injectable, Inject } from '@angular/core';
import { PatternLibrary } from '../../view/components/common/pattern-library/pattern-library';


@Injectable()
export class RoutingService{

    private menusChangeObservable:Observable<any>;
    private menusChangeObserver:Observer<any>;
    private menus:any[];

    private mapComponents = {
        'RULES_ENGINE_PORTLET': RuleEngineContainer,
        'PL': PatternLibrary
    };

    constructor(@Inject('routes') private routes: Routes[ ]){
        this.menusChangeObservable = Observable.create(observer => {
            this.menusChangeObserver = observer;

            if (this.menus){
                this.menusChangeObserver.next(this.menus);
            }
        });
    }

    public subscribeMenusChange():Observable<any>{
        return this.menusChangeObservable;
    }

    public setMenus( menus:any[] ):void{
        this.menus = menus;

        // TODO: do this more elegant
        // TODO: this is bad, we shouldn't be create the route here, a service should only return the data.
        let mainRoutes = this.routes[0];
        mainRoutes.children.slice(1, mainRoutes.children.length);

        this.menus[0].menuItems.unshift({
            ajax: false,
            angular: true
            id: "PL"
            name: "Pattern Library"
            url: "/pl"
        });

        for (let i = 0; i < this.menus.length; i++){
            let menu = this.menus[i];
            for (let k = 0; k < menu.menuItems.length; k++){
                let subMenuItem = menu.menuItems[k];

                if (subMenuItem.angular) {
                    mainRoutes.children.push({
                        component: this.mapComponents[subMenuItem.id],
                        path: subMenuItem.url,
                    });
                    subMenuItem.url = 'dotCMS' + subMenuItem.url
                }else{
                    subMenuItem.url = subMenuItem.url + '&in_frame=true&frame=detailFrame';
                }
            }
        }

        if (this.menusChangeObserver) {
            this.menusChangeObserver.next(this.menus);
        }
    }

    public getMenus():any[]{
        return this.menus;
    }

    public loadMenus():Observable<any>{
        return Observable.create( observer => {
            this.getConfig().subscribe((menuConfig) => {

                if (menuConfig.errors.length) {
                    console.log(menuConfig.errors[0].message);
                    observer.error(menuConfig.errors);

                } else {

                    this.setMenus( menuConfig.entity.menu );
                    observer.next(this.menus);
                }
            });
        });
    }

    private getConfig(): Observable<any> {

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
            oReq.open('GET', '/api/v1/appconfiguration');
            oReq.send();
        });
    }
}
