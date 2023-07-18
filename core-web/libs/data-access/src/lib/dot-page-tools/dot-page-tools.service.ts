import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { DotPageTools } from '@dotcms/dotcms-models';

@Injectable()
export class DotPageToolsService {
    private readonly seoToolsUrl = 'assets/seo/page-tools.json';
    constructor(private http: HttpClient) {}

    /**
     * Returns the page tools from the assets file
     * @returns {Observable<DotPageTools>}
     */
    get(): Observable<DotPageTools> {
        return this.http.get<{ entity: DotPageTools }>(this.seoToolsUrl).pipe(pluck('pageTools'));
    }
}
