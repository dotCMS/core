import { Resolve } from '@angular/router';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DotLoginInformation } from '@models/dot-login';
import { DotLoginPageStateService } from '@components/login/shared/services/dot-login-page-state.service';

/**
 *
 * @export
 * @class DotLoginPageResolver
 * @implements {Resolve<any>}
 */
@Injectable()
export class DotLoginPageResolver implements Resolve<DotLoginInformation> {
    constructor(private dotLoginPageStateService: DotLoginPageStateService) {}

    resolve(): Observable<DotLoginInformation> {
        return this.dotLoginPageStateService.set('');
    }
}
