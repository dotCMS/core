import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

export const STYLE_CLASSES_FILE_URL = '/application/templates/classes.json';

/**
 * @description This service fetchs the available style classes from dotcms users
 *
 * @export
 * @class DotStyleClassesService
 */
@Injectable({
    providedIn: 'root'
})
export class DotStyleClassesService {
    constructor(private http: HttpClient) {}

    /**
     * @description This method fetchs the style classes from a file, if filePath is not provided it will fetch to "/application/templates/classes.json"
     *
     * @param {string} [filePath=STYLE_CLASSES_FILE_URL]
     * @return {*}  {Observable<object>}
     * @memberof DotStyleClassesService
     */
    getStyleClassesFromFile(filePath: string = STYLE_CLASSES_FILE_URL): Observable<object> {
        return this.http.get(filePath);
    }
}
