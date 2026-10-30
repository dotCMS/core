import { Observable, of } from 'rxjs';

import { inject, Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotExperimentsService } from '@dotcms/data-access';
import { DotExperiment } from '@dotcms/dotcms-models';

/**
 * Return a DotExperiment getting the experimentId from query params
 * to show data in the secondary toolbar
 *
 * @export
 * @class DotExperimentExperimentResolver
 * @implements {Resolve<DotRenderedPageState>}
 *
 */
@Injectable()
export class DotExperimentExperimentResolver implements Resolve<Observable<DotExperiment | null>> {
    private readonly dotExperimentsService = inject(DotExperimentsService);

    resolve(route: ActivatedRouteSnapshot): Observable<DotExperiment | null> {
        const { experimentId } = route.queryParams;

        if (!experimentId) {
            return of(null);
        }

        return this.dotExperimentsService
            .getById(experimentId)
            .pipe(map((result) => result ?? null));
    }
}
