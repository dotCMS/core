import { Observable } from 'rxjs';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, take } from 'rxjs/operators';

import {
    DotApp,
    DotAppsExportConfiguration,
    DotAppsImportConfiguration,
    DotAppsSaveData
} from '@dotcms/dotcms-models';
import { getDownloadLink } from '@dotcms/utils';

import { DotHttpErrorManagerService } from '../dot-http-error-manager/dot-http-error-manager.service';

interface DotAppsResponse<T> {
    entity: T;
    errors: string[];
}

const API_URL = '/api/v1/apps';

/**
 * Provide util methods to get apps in the system.
 * @export
 * @class DotAppsService
 */
@Injectable()
export class DotAppsService {
    private readonly http = inject(HttpClient);
    private readonly httpErrorManagerService = inject(DotHttpErrorManagerService);

    /**
     * Return a list of apps.
     * @param {string} filter
     * @returns Observable<DotApps[]>
     * @memberof DotAppsService
     */
    get(filter?: string): Observable<DotApp[] | null> {
        const url = filter ? `${API_URL}?filter=${filter}` : API_URL;

        return this.http.get<DotAppsResponse<DotApp[]>>(url).pipe(
            map((response) => response.entity),
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
    getConfigurationList(appKey: string): Observable<DotApp | null> {
        return this.http.get<DotAppsResponse<DotApp>>(`${API_URL}/${appKey}`).pipe(
            map((response) => response.entity),
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
    getConfiguration(appKey: string, id: string): Observable<DotApp | null> {
        return this.http.get<DotAppsResponse<DotApp>>(`${API_URL}/${appKey}/${id}`).pipe(
            map((response) => response.entity),
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
    saveSiteConfiguration(
        appKey: string,
        id: string,
        params: DotAppsSaveData
    ): Observable<string | null> {
        return this.http.post<DotAppsResponse<string>>(`${API_URL}/${appKey}/${id}`, params).pipe(
            map((response) => response.entity),
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
    exportConfiguration(conf: DotAppsExportConfiguration): Promise<string | null> {
        let fileName = '';

        return fetch(`${API_URL}/export`, {
            method: 'POST',
            cache: 'no-cache',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(conf)
        })
            .then((res: Response) => {
                const message = res.headers.get('error-message');
                if (message) {
                    throw new Error(message);
                }

                const key = 'filename=';
                const contentDisposition = res.headers.get('content-disposition');
                fileName =
                    contentDisposition?.slice(contentDisposition?.indexOf(key) + key.length) ||
                    'Unknown';

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
     * Import configuration(s) of a Service Integration
     * @param {DotAppsImportConfiguration} conf
     * @returns Observable<string>
     * @memberof DotAppsService
     */
    importConfiguration(conf: DotAppsImportConfiguration): Observable<string> {
        const formData = new FormData();
        formData.append('json', JSON.stringify(conf.json));
        formData.append('file', conf.file);

        return this.http.post<DotAppsResponse<string>>(`${API_URL}/import`, formData).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => {
                return this.httpErrorManagerService.handle(error).pipe(
                    take(1),
                    map((err) => err.status.toString())
                );
            })
        );
    }

    /**
     * Delete configuration of a specific Service Integration
     * @param {string} appKey
     * @param {string} hostId
     * @returns Observable<string>
     * @memberof DotAppsService
     */
    deleteConfiguration(appKey: string, hostId: string): Observable<string | null> {
        return this.http.delete<DotAppsResponse<string>>(`${API_URL}/${appKey}/${hostId}`).pipe(
            map((response) => response.entity),
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
    deleteAllConfigurations(appKey: string): Observable<string | null> {
        return this.http.delete<DotAppsResponse<string>>(`${API_URL}/${appKey}`).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => {
                return this.httpErrorManagerService.handle(error).pipe(
                    take(1),
                    map(() => null)
                );
            })
        );
    }
}
