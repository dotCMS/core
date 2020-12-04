import { Observable, of } from 'rxjs';
import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';
import { LoginService } from 'dotcms-js';

/**
 * Returns username logged
 *
 * @export
 * @class DotStarterResolver
 * @implements {Resolve<Observable<string>>}
 */
@Injectable()
export class DotStarterResolver implements Resolve<Observable<string>> {
    constructor(private loginService: LoginService) {}

    resolve(): Observable<string> {
        return of(this.loginService.auth.user.firstName);
    }
}
