import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotPageTool, DotPageTools } from '@dotcms/dotcms-models';

@Service({ autoProvided: false })
export class DotPageToolsService {
    private http = inject(HttpClient);

    private readonly seoToolsUrl = 'assets/seo/page-tools.json';

    /**
     * Returns the page tools from the assets file
     * @returns {Observable<DotPageTool[]>}
     */
    get(): Observable<DotPageTool[]> {
        return this.http.get<DotPageTools>(this.seoToolsUrl).pipe(map((x) => x?.pageTools));
    }
}
