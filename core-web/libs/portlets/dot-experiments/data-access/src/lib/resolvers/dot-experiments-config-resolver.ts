import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { DotPropertiesService } from '@dotcms/data-access';

@Injectable()
export class DotExperimentsConfigResolver implements Resolve<Record<string, string> | null> {
    constructor(private readonly dotConfigurationService: DotPropertiesService) {}

    resolve(route: ActivatedRouteSnapshot): Observable<Record<string, string>> | null {
        return this.dotConfigurationService.getKeys(route.data['experimentsConfigProps']);
    }
}
