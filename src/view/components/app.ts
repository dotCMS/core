import {Component, Inject, ViewEncapsulation} from '@angular/core';

// Custom Components
import {LoginPageComponent} from './common/login/login-page-component';
import {MainComponent} from './common/main-component/main-component';
import { Router } from '@ngrx/router';


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
    _menuItems: Array<any>;

    constructor(@Inject('menuItems') private menuItems: Array<any>,
                 router: Router) {

        let queryParams:Map = this.getQueryParams();

        if (<boolean> queryParams.get('resetPassword')){
            let token:string = queryParams.get('token');
            let userId:string = queryParams.get('userId');

            router.go(`/login/resetPassword/${userId}?token=${token}`);
        }else if (menuItems.navigationItems.length === 0) {
            router.go('/login/login');
        }else {
            router.go('/main');
        }

        this._menuItems = menuItems;
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
