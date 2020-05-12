import { forkJoin, Observable, of } from 'rxjs';
import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { take, switchMap } from 'rxjs/operators';
import { DotApps } from '@shared/models/dot-apps/dot-apps.model';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotMessageService } from '@services/dot-messages-service';

export interface DotAppsResolverData {
    messages?: { [key: string]: string };
    app: DotApps;
}

/**
 * Returns apps list from the system
 *
 * @export
 * @class DotAppsPageResolver
 * @implements {Resolve<{[key: string]: string}>}
 */
@Injectable()
export class DotAppsConfigurationResolver implements Resolve<Observable<DotAppsResolverData>> {
    constructor(
        private dotAppsService: DotAppsService,
        public dotMessageService: DotMessageService
    ) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotAppsResolverData> {
        const appsKey = route.paramMap.get('appKey');
        const appsConfigurations$ = this.dotAppsService.getConfigurationList(appsKey).pipe(take(1));
        const messages$: Observable<{
            [key: string]: string;
        }> = this.dotMessageService
            .getMessages([
                'apps.confirmation.delete.all.button',
                'apps.confirmation.title',
                'apps.key',
                'apps.confirmation.delete.all.message',
                'apps.confirmation.accept',
                'apps.search.placeholder'
            ])
            .pipe(take(1));

        return forkJoin([appsConfigurations$, messages$]).pipe(
            switchMap(([app, messages]) => {
                return of({ messages, app });
            })
        );
    }
}
