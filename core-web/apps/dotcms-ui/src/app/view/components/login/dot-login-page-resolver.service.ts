import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { Resolve } from '@angular/router';

import { DotLoginInformation } from '@dotcms/dotcms-models';

import { DotLoginPageStateService } from './shared/services/dot-login-page-state.service';

/**
 *
 * @export
 * @class DotLoginPageResolver
 * @implements {Resolve<any>}
 */
@Injectable()
export class DotLoginPageResolver implements Resolve<DotLoginInformation> {
    private dotLoginPageStateService = inject(DotLoginPageStateService);

    resolve(): Observable<DotLoginInformation> {
        return this.dotLoginPageStateService.set('');
    }
}
