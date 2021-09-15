import { Injectable } from '@angular/core';
import { CoreWebService } from '@dotcms/dotcms-js';
import { map, pluck, take } from 'rxjs/operators';
import { Observable } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class DotPropertiesService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Get the value of specific key
     * from the dotmarketing-config.properties
     *
     * @param string key
     * @returns {Observable<string>}
     * @memberof DotPropertiesService
     */
    getKey(key: string): Observable<string> {
        return this.coreWebService
            .requestView({
                url: `/api/v1/configuration/config?keys=${key}`
            })
            .pipe(
                take(1),
                pluck('bodyJsonObject'),
                map((response) => response[key])
            );
    }

    /**
     * Get the value of specific key as a list
     * from the dotmarketing-config.properties
     *
     * @param string key
     * @returns {Observable<string[]>}
     * @memberof DotPropertiesService
     */
    getKeyAsList(key: string): Observable<string[]> {
        const finalKey = `list:${key}`;
        return this.coreWebService
            .requestView<{ [key: string]: any }>({
                url: `/api/v1/configuration/config?keys=${finalKey}`
            })
            .pipe(
                take(1),
                pluck('bodyJsonObject'),
                map((response) => response[finalKey])
            );
    }
}
