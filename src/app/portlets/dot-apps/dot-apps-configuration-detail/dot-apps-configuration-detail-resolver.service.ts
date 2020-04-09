import { Observable, forkJoin, of } from 'rxjs';
import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { take, switchMap } from 'rxjs/operators';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotMessageService } from '@services/dot-messages-service';
import { DotAppsResolverData } from '../dot-apps-configuration/dot-apps-configuration-resolver.service';

/**
 * Returns app configuration detail from the api
 *
 * @export
 * @class DotAppsConfigurationDetailResolver
 * @implements {Resolve<DotAppsResolverData>}
 */
@Injectable()
export class DotAppsConfigurationDetailResolver implements Resolve<DotAppsResolverData> {
    constructor(
        private dotAppsService: DotAppsService,
        public dotMessageService: DotMessageService
    ) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotAppsResolverData> {
        const appKey = route.paramMap.get('appKey');
        const id = route.paramMap.get('id');
        const appConfigurations$ = this.dotAppsService.getConfiguration(appKey, id).pipe(take(1));
        const messages$: Observable<{
            [key: string]: string;
        }> = this.dotMessageService
            .getMessages([
                'apps.key',
                'apps.add.property',
                'apps.form.dialog.success.header',
                'apps.form.dialog.success.message',
                'ok',
                'Cancel',
                'Save'
            ])
            .pipe(take(1));

        return forkJoin([appConfigurations$, messages$]).pipe(
            switchMap(([app, messages]) => {
                return of({ messages, app });
            })
        );
    }
}
