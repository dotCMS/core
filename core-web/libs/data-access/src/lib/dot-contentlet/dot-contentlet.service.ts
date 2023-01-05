import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { pluck, take } from 'rxjs/operators';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Injectable()
export class DotContentletService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Get the Contentlet versions by language.
     *
     * @param string identifier
     * @param string language
     * @returns Observable<DotCMSContentlet[]>
     * @memberof DotContentletService
     */
    getContentletVersions(identifier: string, language: string): Observable<DotCMSContentlet[]> {
        return this.coreWebService
            .requestView({
                url: `/api/v1/content/versions?identifier=${identifier}&groupByLang=1`
            })
            .pipe(take(1), pluck('entity', 'versions', language));
    }

    /**
     * Get Contentlet permissions data
     * @param string identifier
     * @returns Observable<{ [key: string]: string[] }
     * @memberof DotContentletService
     */
    getContentletPermissions(identifier: string): Observable<{ [key: string]: string[] }> {
        return this.coreWebService
            .requestView({
                url: `v1/permissions/_bycontent/_groupbytype?contentletId=${identifier}`
            })
            .pipe(take(1), pluck('entity'));
    }
}
