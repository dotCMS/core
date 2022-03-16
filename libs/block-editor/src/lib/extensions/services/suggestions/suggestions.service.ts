import { Injectable } from '@angular/core';

import { Observable } from 'rxjs';
import { pluck } from 'rxjs/operators';

import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';
// eslint-disable-next-line max-len
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Injectable()
export class SuggestionsService {

    constructor(private http: HttpClient) {}

    get defaultHeaders() {
        const headers = new HttpHeaders();
        headers.set('Accept', '*/*').set('Content-Type', 'application/json');
        return headers;
    }

    getContentTypes(filter = ''): Observable<DotCMSContentType[]> {
        return this.http
            .get(`/api/v1/contenttype?filter=${filter}&orderby=name&direction=ASC&per_page=40`, {
                headers: this.defaultHeaders
            })
            .pipe(pluck('entity'));
    }

    getContentlets(contentType = ''): Observable<DotCMSContentlet[]> {
        return this.http
            .post('/api/content/_search', {
                query: `+contentType:${contentType} +languageId:1 +deleted:false +working:true`,
                sort: 'modDate desc',
                offset: 0
            })
            .pipe(pluck('entity', 'jsonObjectView', 'contentlets'));
    }
}
