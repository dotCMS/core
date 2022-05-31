import { Component, ViewEncapsulation } from '@angular/core';
import { LoginService, LOGOUT_URL } from '@dotcms/dotcms-js';
import { DotRouterService } from '../../../../api/services/dot-router/dot-router.service';
import { take } from 'rxjs/operators';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-log-out-container',
    template: ''
})
export class DotLogOutContainerComponent {
    constructor(loginService: LoginService, router: DotRouterService) {
        loginService.isLogin$.pipe(take(1)).subscribe((isLogin) => {
            if (isLogin) {
                window.location.href = LOGOUT_URL;
            } else {
                router.goToLogin();
            }
        });
    }
}
