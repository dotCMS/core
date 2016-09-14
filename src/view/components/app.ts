import {Component, Inject, ViewEncapsulation} from '@angular/core';
import {DotcmsConfig} from '../../api/services/system/dotcms-config';
import {LoginPageComponent} from './common/login/login-page-component';
import {LoginService} from '../../api/services/login-service';
import {MainComponent} from './common/main-component/main-component';
import {Router} from '@ngrx/router';
import {RoutingService} from '../../api/services/routing-service';

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

    constructor(private router: Router, private loginService: LoginService, private routingService: RoutingService,
                @Inject('dotcmsConfig') private dotcmsConfig: DotcmsConfig) {

    }

    ngOnInit(): void {
        let queryParams: Map = this.getQueryParams();

        if (<boolean> queryParams.get('resetPassword')) {
            let token: string = queryParams.get('token');
            this.router.go(`public/resetPassword?token=${token}`);
        } else if (this.dotcmsConfig.configParams.user) {
            this.routingService.setMenus(this.dotcmsConfig.configParams.menu);

            this.loginService.setLogInUser(this.dotcmsConfig.configParams.loginAsUser || this.dotcmsConfig.configParams.user);
            this.router.go('dotCMS');
        } else {
            this.router.go('public/login');
        }
    }

    toggleMain(change: boolean): void {
        this.login = change;
    }

    private getQueryParams(): Map<string, string> {
        let split: string[] = window.location.search.substring(1).split('&');
        let map: Map<string, string> = new Map();

        split.forEach(param => {
            let paramSplit: string[] = param.split('=');
            map.set(paramSplit[0], paramSplit[1]);
        });

        return map;
    }
}
