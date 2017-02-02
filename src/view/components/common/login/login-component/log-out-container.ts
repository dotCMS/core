import {Component,ViewEncapsulation} from '@angular/core';
import {LoginContainer} from './login-container';
import {HttpRequestUtils} from '../../../../../api/util/httpRequestUtils';
import {LoginService} from '../../../../../api/services/login-service';
import {DotRouterService} from '../../../../../api/services/dot-router-service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    providers: [HttpRequestUtils],
    selector: 'dot-log-out-container',
    template: `
        <dot-login-component
            [message]="message"
            [isLoginInProgress] = "isLoginInProgress"
            (login)="logInUser($event)"
            (recoverPassword)="showForgotPassword()"
            [passwordChanged]="passwordChanged"
            [resetEmailSent]="resetEmailSent"
            [resetEmail]="resetEmail"
        >
        </dot-login-component>
    `,
})
export class LogOutContainer extends LoginContainer {

    constructor ( loginService: LoginService,  router: DotRouterService, httprequestUtils: HttpRequestUtils) {
        super(loginService, router, httprequestUtils);

        loginService.isLogin$.subscribe(isLogin => {

            if (isLogin) {
                loginService.logOutUser().subscribe(() => {});
            }
        });
    }
}
