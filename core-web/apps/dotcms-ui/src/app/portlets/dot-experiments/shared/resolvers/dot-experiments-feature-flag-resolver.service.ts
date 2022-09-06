import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Resolve } from '@angular/router';
import { DotPropertiesService } from '@services/dot-properties/dot-properties.service';
import { map } from 'rxjs/operators';

const FEATURED_FLAG_EXPERIMENTS = 'EXPERIMENTS_FEATURE_ENABLE';

@Injectable()
export class DotFeatureFlagResolver implements Resolve<Observable<boolean>> {
    constructor(private readonly dotConfigurationService: DotPropertiesService) {}

    resolve() {
        return this.dotConfigurationService.getKey(FEATURED_FLAG_EXPERIMENTS).pipe(
            map((result) => {
                return !!result;
            })
        );
    }
}
