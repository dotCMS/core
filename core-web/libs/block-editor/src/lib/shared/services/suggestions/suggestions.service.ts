import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';

import { ContentletFilters, DEFAULT_LANG_ID } from '../../../extensions';

@Injectable()
export class SuggestionsService {
    constructor(private http: HttpClient) {}

    get defaultHeaders() {
        const headers = new HttpHeaders();
        headers.set('Accept', '*/*').set('Content-Type', 'application/json');

        return headers;
    }

    getContentTypes(filter = '', allowedTypes = ''): Observable<DotCMSContentType[]> {
        return this.http
            .post(`/api/v1/contenttype/_filter`, {
                filter: {
                    types: allowedTypes,
                    query: filter
                },
                orderBy: 'name',
                direction: 'ASC',
                perPage: 40
            })
            .pipe(pluck('entity'));
    }

    getContentlets({
        contentType,
        filter,
        currentLanguage,
        contentletIdentifier
    }: ContentletFilters): Observable<DotCMSContentlet[]> {
        const identifierQuery = contentletIdentifier ? `-identifier:${contentletIdentifier}` : '';
        const search = filter.includes('-') ? filter : `*${filter}*`;

        return this.http
            .post('/api/content/_search', {
                query: `+contentType:${contentType} ${identifierQuery} +languageId:${currentLanguage} +deleted:false +working:true +catchall:${search} title:'${filter}'^15`,
                sort: 'modDate desc',
                offset: 0,
                limit: 40
            })
            .pipe(pluck('entity', 'jsonObjectView', 'contentlets'));
    }

    /**
     * Get contentlets filtered by urlMap
     *
     * @param {*} { filter, currentLanguage = DEFAULT_LANG_ID }
     * @return {*}  {Observable<DotCMSContentlet[]>}
     * @memberof SuggestionsService
     */
    getContentletsUrlMap({
        filter,
        currentLanguage = DEFAULT_LANG_ID
    }): Observable<DotCMSContentlet[]> {
        return this.http
            .post('/api/content/_search', {
                query: `+languageId:${currentLanguage} +deleted:false +working:true +(urlmap:*${filter}* OR +(basetype:5 AND path:*${filter}*))`,
                sort: 'modDate desc',
                offset: 0,
                limit: 40
            })
            .pipe(pluck('entity', 'jsonObjectView', 'contentlets'));
    }
}
