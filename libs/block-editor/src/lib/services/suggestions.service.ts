import { Injectable } from '@angular/core';

import { Observable } from 'rxjs';
import { pluck } from 'rxjs/operators';

import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';
import { CoreWebService } from '@dotcms/dotcms-js';

@Injectable()
export class SuggestionsService {
    constructor(private coreWebSerice: CoreWebService) {}

    getContentTypes(filter = ''): Observable<DotCMSContentType[]> {
        return this.coreWebSerice
            .requestView({
                url: `/api/v1/contenttype?filter=${filter}&orderby=modDate&direction=DESC&per_page=40`
            })
            .pipe(pluck('entity'));
    }

    getContentlets(contentType = ''): Observable<DotCMSContentlet[]> {
        return this.coreWebSerice
            .requestView({
                // eslint-disable-next-line max-len
                url: `/api/content/render/false/query/+contentType:${contentType}%20+languageId:1%20+deleted:false%20+working:true/orderby/modDate%20desc`
            })
            .pipe(pluck('contentlets'));
    }
}
