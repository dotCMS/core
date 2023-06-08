import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { DotExperiment } from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';

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
export class DotExperimentExperimentResolver implements Resolve<DotExperiment> {
    constructor(private readonly dotExperimentsService: DotExperimentsService) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotExperiment> | null {
        const { experimentId } = route.queryParams;

        if (!experimentId) {
            return of(null);
        }

        return this.dotExperimentsService.getById(experimentId);
    }
}
