import { forkJoin, Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { DotPropertiesService } from '@dotcms/data-access';
import { DotExperimentResolver } from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';

/**
 * Return a DotExperiment getting the experimentId from query params
 * and config properties from data
 * to show data in the secondary toolbar
 *
 * @export
 * @class DotExperimentExperimentResolver
 * @implements {Resolve<DotExperimentResolver>}
 *
 */
@Injectable()
export class DotExperimentExperimentResolver implements Resolve<DotExperimentResolver> {
    constructor(
        private readonly dotExperimentsService: DotExperimentsService,
        private readonly dotConfigurationService: DotPropertiesService
    ) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotExperimentResolver> | null {
        const { experimentId } = route.queryParams;

        //
        // if (!experimentId) {
        //     return of(null);
        // }

        return forkJoin({
            experiment: experimentId ? this.dotExperimentsService.getById(experimentId) : of(null),
            configProps: this.dotConfigurationService.getKeys(route.data.experimentsConfigProps)
        });
    }
}
