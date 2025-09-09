import { forkJoin, Observable, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { Resolve } from '@angular/router';

import { catchError, map, take } from 'rxjs/operators';

import { DotLicenseService, PushPublishService } from '@dotcms/data-access';
import { DotEnvironment } from '@dotcms/dotcms-models';

@Injectable()
export class DotTemplateListResolver implements Resolve<[boolean, boolean]> {
    dotLicenseService = inject(DotLicenseService);
    pushPublishService = inject(PushPublishService);

    resolve(): Observable<[boolean, boolean]> {
        return forkJoin([
            this.dotLicenseService.isEnterprise().pipe(
                catchError((error) => {
                    console.error(
                        'DotTemplateListResolver: Failed to check enterprise license',
                        error
                    );
                    // Default to false if license check fails
                    return of(false);
                })
            ),
            this.pushPublishService.getEnvironments().pipe(
                map((environments: DotEnvironment[]) => !!environments.length),
                take(1),
                catchError((error) => {
                    console.error(
                        'DotTemplateListResolver: Failed to get push publish environments',
                        error
                    );
                    // Default to false if environments check fails
                    return of(false);
                })
            )
        ]);
    }
}
