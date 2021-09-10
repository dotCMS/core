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
        headers
            .set('Accept', '*/*')
            .set('Content-Type', 'application/json');
        return headers;
    }

    getContentTypes(filter = ''): Observable<DotCMSContentType[]> {
        return this.http
            .get(`/api/v1/contenttype?filter=${filter}&orderby=modDate&direction=DESC&per_page=40`, {
                headers: this.defaultHeaders
            })
            .pipe(pluck('entity'));
    }

    getContentlets(contentType = ''): Observable<DotCMSContentlet[]> {
        return this.http
            // eslint-disable-next-line max-len
            .get(`/api/content/render/false/query/+contentType:${contentType}%20+languageId:1%20+deleted:false%20+working:true/orderby/modDate%20desc`, {
                headers: this.defaultHeaders
            })
            .pipe(pluck('contentlets'));
    }

}
