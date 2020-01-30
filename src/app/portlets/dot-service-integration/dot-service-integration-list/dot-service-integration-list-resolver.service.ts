import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';
import { take } from 'rxjs/operators';
import { DotServiceIntegration } from '@shared/models/dot-service-integration/dot-service-integration.model';
import { DotServiceIntegrationService } from '@services/dot-service-integration/dot-service-integration.service';

/**
 * Returns service integrations list from the system
 *
 * @export
 * @class DotServiceIntegrationListResolver
 * @implements {Resolve<DotServiceIntegration[]>}
 */
@Injectable()
export class DotServiceIntegrationListResolver implements Resolve<DotServiceIntegration[]> {
    constructor(private dotServiceIntegrationService: DotServiceIntegrationService) {}

    resolve(): Observable<DotServiceIntegration[]> {
        return this.dotServiceIntegrationService.get().pipe(take(1));
    }
}
