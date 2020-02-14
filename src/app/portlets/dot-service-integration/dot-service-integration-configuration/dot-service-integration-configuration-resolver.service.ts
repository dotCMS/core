import { forkJoin, Observable, of } from 'rxjs';
import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { take, switchMap } from 'rxjs/operators';
import { DotServiceIntegration } from '@shared/models/dot-service-integration/dot-service-integration.model';
import { DotServiceIntegrationService } from '@services/dot-service-integration/dot-service-integration.service';
import { DotMessageService } from '@services/dot-messages-service';

export interface IntegrationResolverData {
    messages?: { [key: string]: string };
    service: DotServiceIntegration;
}

/**
 * Returns service integrations list from the system
 *
 * @export
 * @class DotServiceIntegrationPageResolver
 * @implements {Resolve<{[key: string]: string}>}
 */
@Injectable()
export class DotServiceIntegrationConfigurationResolver
    implements Resolve<Observable<IntegrationResolverData>> {
    constructor(
        private dotServiceIntegrationService: DotServiceIntegrationService,
        public dotMessageService: DotMessageService
    ) {}

    resolve(route: ActivatedRouteSnapshot): Observable<IntegrationResolverData> {
        const serviceKey = route.paramMap.get('serviceKey');
        const servicesConfigurations$ = this.dotServiceIntegrationService
            .getConfiguration(serviceKey)
            .pipe(take(1));
        const messages$: Observable<{
            [key: string]: string;
        }> = this.dotMessageService
            .getMessages([
                'service.integration.configurations',
                'service.integration.no.configurations',
                'service.integration.confirmation.delete.all.button',
                'service.integration.confirmation.title',
                'service.integration.key',
                'service.integration.confirmation.description.show.more',
                'service.integration.confirmation.description.show.less',
                'service.integration.confirmation.delete.all.message',
                'service.integration.confirmation.accept',
                'service.integration.search.placeholder'
            ])
            .pipe(take(1));

        return forkJoin([servicesConfigurations$, messages$]).pipe(
            switchMap(([integration, messages]) => {
                return of({ messages, service: integration });
            })
        );
    }
}
