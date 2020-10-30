import { pluck, catchError, take, map } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
    DotApps,
    DotAppsExportConfiguration,
    DotAppsSaveData
} from '@models/dot-apps/dot-apps.model';
import { CoreWebService } from 'dotcms-js';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { HttpErrorResponse } from '@angular/common/http';
import { getDownloadLink } from '@shared/dot-utils';

const appsUrl = `v1/apps`;

/**
 * Provide util methods to get apps in the system.
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
     * Return a list of apps.
     * @param {string} filter
     * @returns Observable<DotApps[]>
     * @memberof DotAppsService
     */
    get(filter?: string): Observable<DotApps[]> {
        const url = filter ? `${appsUrl}?filter=${filter}` : appsUrl;
        return this.coreWebService
            .requestView<DotApps[]>({
                url
            })
            .pipe(
                pluck('entity'),
                catchError((error: HttpErrorResponse) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            );
    }

    /**
     * Return a list of configurations of a specific Apps
     * @param {string} appKey
     * @returns Observable<DotApps>
     * @memberof DotAppsService
     */
    getConfigurationList(appKey: string): Observable<DotApps> {
        return this.coreWebService
            .requestView({
                url: `${appsUrl}/${appKey}`
            })
            .pipe(
                pluck('entity'),
                catchError((error: HttpErrorResponse) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            );
    }

    /**
     * Return a detail configuration of a specific App
     * @param {string} appKey
     * @param {string} id
     * @returns Observable<DotApps>
     * @memberof DotAppsService
     */
    getConfiguration(appKey: string, id: string): Observable<DotApps> {
        return this.coreWebService
            .requestView({
                url: `${appsUrl}/${appKey}/${id}`
            })
            .pipe(
                pluck('entity'),
                catchError((error: HttpErrorResponse) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            );
    }

    /**
     * Saves a detail configuration of a specific Service Integration
     * @param {string} appKey
     * @param {string} id
     * @param {DotAppsSaveData} params
     * @returns Observable<string>
     * @memberof DotAppsService
     */
    saveSiteConfiguration(appKey: string, id: string, params: DotAppsSaveData): Observable<string> {
        return this.coreWebService
            .requestView({
                body: {
                    ...params
                },
                method: 'POST',
                url: `${appsUrl}/${appKey}/${id}`
            })
            .pipe(
                pluck('entity'),
                catchError((error: HttpErrorResponse) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            );
    }

    /**
     * Export configuration(s) of a Service Integration
     * @param {DotAppsExportConfiguration} conf
     * @returns Promise<string>
     * @memberof DotAppsService
     */
    exportConfiguration(conf: DotAppsExportConfiguration): Promise<string> {
        let fileName = '';

        return fetch(`/api/${appsUrl}/export`, {
            method: 'POST',
            cache: 'no-cache',
            headers: {
                'Content-Type': 'application/json'
            },

            body: JSON.stringify(conf)
        })
            .then((res: Response) => {
                const key = 'filename=';
                const contentDisposition = res.headers.get('content-disposition');
                fileName = contentDisposition.slice(contentDisposition.indexOf(key) + key.length);
                return res.blob();
            })
            .then((blob: Blob) => {
                getDownloadLink(blob, fileName).click();
                return '';
            })
            .catch((error) => {
                return error.message;
            });
    }

    /**
     * Delete configuration of a specific Service Integration
     * @param {string} appKey
     * @param {string} hostId
     * @returns Observable<string>
     * @memberof DotAppsService
     */
    deleteConfiguration(appKey: string, hostId: string): Observable<string> {
        return this.coreWebService
            .requestView({
                method: 'DELETE',
                url: `${appsUrl}/${appKey}/${hostId}`
            })
            .pipe(
                pluck('entity'),
                catchError((error: HttpErrorResponse) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            );
    }

    /**
     * Delete all configuration of a specific Service Integration
     * @param {string} appKey
     * @returns Observable<string>
     * @memberof DotAppsService
     */
    deleteAllConfigurations(appKey: string): Observable<string> {
        return this.coreWebService
            .requestView({
                method: 'DELETE',
                url: `${appsUrl}/${appKey}`
            })
            .pipe(
                pluck('entity'),
                catchError((error: HttpErrorResponse) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            );
    }
}
