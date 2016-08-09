import {Component, Inject, ViewEncapsulation} from '@angular/core';

// Custom Components
import {LoginPageComponent} from './common/login/login-page-component';
import {MainComponent} from './common/main-component/main-component';
import { Router } from '@ngrx/router';
import {LoginService} from "../../api/services/login-service";
import {RoutingService} from "../../api/services/routing-service";


@Component({
    directives: [MainComponent, LoginPageComponent],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [],
    selector: 'app',
    styleUrls: ['app.css'],
    templateUrl: ['app.html']
})

/**
 * Display the login component or the main component if
 * there is a navigation menu set
 */
export class AppComponent {

    login: boolean= true;

    constructor(router: Router, loginService:LoginService, routingService:RoutingService) {

        let queryParams:Map = this.getQueryParams();

        if (<boolean> queryParams.get('resetPassword')){
            let token:string = queryParams.get('token');
            let userId:string = queryParams.get('userId');

            router.go(`public/resetPassword/${userId}?token=${token}`);
        }else if (!loginService.getLoginUser()) {
            router.go('public/login');
        }else {

            routingService.loadMenus().subscribe( (menu) => router.go('dotCMS'),
                error => console.error(error)
            );


        }
    }

    private getQueryParams():Map<string, string> {
        let split:string[] = window.location.search.substring(1).split('&');
        let map:Map<string, string> = new Map();

        split.forEach(param => {
            let paramSplit:string[] = param.split('=');
            map.set(paramSplit[0], paramSplit[1]);
        });

        return map;
    }

   toggleMain(change: boolean): void {
       this.login = change;
   }

}
