import { Component, ViewEncapsulation } from '@angular/core';

import { take } from 'rxjs/operators';

import { DotRouterService } from '@dotcms/data-access';
import { LoginService, LOGOUT_URL } from '@dotcms/dotcms-js';

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
