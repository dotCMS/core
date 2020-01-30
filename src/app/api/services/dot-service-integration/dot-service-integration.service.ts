import { pluck, catchError, take, map } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DotServiceIntegration } from '@models/dot-service-integration/dot-service-integration.model';
import { RequestMethod } from '@angular/http';
import { CoreWebService, ResponseView } from 'dotcms-js';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

const serviceIntegrationUrl = `v1/service-integrations`;

/**
 * Provide util methods to get service integrations in the system.
 * @export
 * @class DotServiceIntegrationService
 */
@Injectable()
export class DotServiceIntegrationService {
    constructor(
        private coreWebService: CoreWebService,
        private httpErrorManagerService: DotHttpErrorManagerService
    ) {}

    /**
     * Return a list of Service Integrations.
     * @returns Observable<DotServiceIntegration[]>
     * @memberof DotServiceIntegrationService
     */
    get(): Observable<DotServiceIntegration[]> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: serviceIntegrationUrl
            })
            .pipe(
                pluck('entity'),
                catchError((error: ResponseView) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            );
    }

    /**
     * Return configurations of a specific Service Integration
     * @param {string} serviceKey
     * @returns Observable<DotServiceIntegration>
     * @memberof DotServiceIntegrationService
     */
    getConfiguration(serviceKey: string): Observable<DotServiceIntegration> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: `${serviceIntegrationUrl}/${serviceKey}`
            })
            .pipe(
                pluck('entity'),
                catchError((error: ResponseView) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            );
    }

    /**
     * Delete configuration of a specific Service Integration
     * @param {string} serviceKey
     * @param {string} hostId
     * @returns Observable<string>
     * @memberof DotServiceIntegrationService
     */
    deleteConfiguration(serviceKey: string, hostId: string): Observable<string> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Delete,
                url: `${serviceIntegrationUrl}/${serviceKey}/${hostId}`
            })
            .pipe(
                pluck('entity'),
                catchError((error: ResponseView) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            );
    }

    /**
     * Delete all configuration of a specific Service Integration
     * @param {string} serviceKey
     * @returns Observable<string>
     * @memberof DotServiceIntegrationService
     */
    deleteAllConfigurations(serviceKey: string): Observable<string> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Delete,
                url: `${serviceIntegrationUrl}/${serviceKey}`
            })
            .pipe(
                pluck('entity'),
                catchError((error: ResponseView) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            );
    }
}
