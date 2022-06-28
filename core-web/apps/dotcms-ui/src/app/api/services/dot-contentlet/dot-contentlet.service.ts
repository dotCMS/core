import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { pluck, take } from 'rxjs/operators';
import { CoreWebService } from '@dotcms/dotcms-js';

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
}
