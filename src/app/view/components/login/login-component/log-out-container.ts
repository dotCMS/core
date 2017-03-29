import {Component, ViewEncapsulation} from '@angular/core';
import {LoginContainer} from './login-container';
import {LoginService} from '../../../../api/services/login-service';
import {DotRouterService} from '../../../../api/services/dot-router-service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-log-out-container',
    template: ''
})
export class LogOutContainer {

    constructor ( loginService: LoginService,  router: DotRouterService) {

        loginService.isLogin$.subscribe(isLogin => {

            if (isLogin) {
                loginService.logOutUser().subscribe(() => {
                });
            }else {
                router.goToLogin();
            }
        });
    }
}
