import { pluck, catchError, take, map } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DotApps } from '@models/dot-apps/dot-apps.model';
import { RequestMethod } from '@angular/http';
import { CoreWebService, ResponseView } from 'dotcms-js';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

const serviceIntegrationUrl = `v1/apps`;

/**
 * Provide util methods to get service integrations in the system.
 * @export
 * @class DotAppsService
 */
@Injectable()
export class DotAppsService {
    constructor(
        private coreWebService: CoreWebService,
        private httpErrorManagerService: DotHttpErrorManagerService
    ) {}

    /**
     * Return a list of Service Integrations.
     * @returns Observable<DotApps[]>
     * @memberof DotAppsService
     */
    get(): Observable<DotApps[]> {
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
     * @returns Observable<DotApps>
     * @memberof DotAppsService
     */
    getConfiguration(serviceKey: string): Observable<DotApps> {
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
     * @memberof DotAppsService
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
     * @memberof DotAppsService
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
