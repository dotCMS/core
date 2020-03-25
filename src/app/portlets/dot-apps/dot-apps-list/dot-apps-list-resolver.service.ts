import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';
import { take } from 'rxjs/operators';
import { DotApps } from '@shared/models/dot-apps/dot-apps.model';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';

/**
 * Returns apps list from the system
 *
 * @export
 * @class DotAppsListResolver
 * @implements {Resolve<DotApps[]>}
 */
@Injectable()
export class DotAppsListResolver implements Resolve<DotApps[]> {
    constructor(private dotAppsService: DotAppsService) {}

    resolve(): Observable<DotApps[]> {
        return this.dotAppsService.get().pipe(take(1));
    }
}
