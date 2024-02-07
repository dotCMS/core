import { Injectable } from '@angular/core';
import { CanActivate } from '@angular/router';

import { DotRouterService } from '@dotcms/data-access';

/**
 * Route Guard the only function is to redirect to the Main Portlet.
 */
@Injectable()
export class DefaultGuardService implements CanActivate {
    constructor(private router: DotRouterService) {}

    canActivate(): boolean {
        this.router.goToMain();

        return true;
    }
}
