import { forkJoin, Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { take } from 'rxjs/operators';
import { DotServiceIntegration } from '@shared/models/dot-service-integration/dot-service-integration.model';
import { DotServiceIntegrationService } from '@services/dot-service-integration/dot-service-integration.service';
import { DotMessageService } from '@services/dot-messages-service';

/**
 * Returns service integrations list from the system
 *
 * @export
 * @class DotServiceIntegrationPageResolver
 * @implements {Resolve<any>}
 */
@Injectable()
export class DotServiceIntegrationConfigurationListResolver
    implements Resolve<Observable<[DotServiceIntegration, any]>> {
    constructor(
        private dotServiceIntegrationService: DotServiceIntegrationService,
        public dotMessageService: DotMessageService
    ) {}

    resolve(route: ActivatedRouteSnapshot): Observable<[DotServiceIntegration, any]> {
        const serviceKey = route.paramMap.get('serviceKey');
        const servicesConfigurations$ = this.dotServiceIntegrationService
            .getConfiguration(serviceKey)
            .pipe(take(1));
        const messages$ = this.dotMessageService
            .getMessages([
                'service.integration.configurations',
                'service.integration.no.configurations',
                'service.integration.key',
                'service.integration.add.configurations',
                'service.integration.no.configurations.message',
                'service.integration.add.configurations.button',
                'service.integration.confirmation.delete.all.button',
                'service.integration.confirmation.title',
                'service.integration.confirmation.delete.message',
                'service.integration.confirmation.delete.all.message',
                'service.integration.confirmation.accept'
            ])
            .pipe(take(1));

        return forkJoin([servicesConfigurations$, messages$]);
    }
}
