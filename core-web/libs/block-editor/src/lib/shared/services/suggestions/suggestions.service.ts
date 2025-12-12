import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSContentlet, DotCMSContentType, DotCMSResponse } from '@dotcms/dotcms-models';

import { ContentletFilters, DEFAULT_LANG_ID } from '../../../shared';

@Injectable()
export class SuggestionsService {
    private readonly http = inject(HttpClient);

    get defaultHeaders() {
        const headers = new HttpHeaders();
        headers.set('Accept', '*/*').set('Content-Type', 'application/json');

        return headers;
    }

    getContentTypes(filter = '', allowedTypes = ''): Observable<DotCMSContentType[]> {
        return this.http
            .post<DotCMSResponse<DotCMSContentType[]>>(`/api/v1/contenttype/_filter`, {
                filter: {
                    types: allowedTypes,
                    query: filter
                },
                orderBy: 'name',
                direction: 'ASC',
                perPage: 40
            })
            .pipe(map((x: DotCMSResponse<DotCMSContentType[]>) => x?.entity));
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
            .post<DotCMSResponse<{ jsonObjectView: { contentlets: DotCMSContentlet[] } }>>(
                '/api/content/_search',
                {
                    query: `+contentType:${contentType} ${identifierQuery} +languageId:${currentLanguage} +deleted:false +working:true +catchall:${search} title:'${filter}'^15`,
                    sort: 'modDate desc',
                    offset: 0,
                    limit: 40
                }
            )
            .pipe(
                map(
                    (x: DotCMSResponse<{ jsonObjectView: { contentlets: DotCMSContentlet[] } }>) =>
                        x?.entity?.jsonObjectView?.contentlets
                )
            );
    }

    /**
     * Get contentlets filtered by url
     *
     * @param {{
     *         link: string;
     *         currentLanguage?: number;
     *     }} {
     *         link,
     *         currentLanguage = DEFAULT_LANG_ID
     *     }
     * @return {*}  {Observable<DotCMSContentlet[]>}
     * @memberof SuggestionsService
     */
    getContentletsByLink({
        link,
        currentLanguage = DEFAULT_LANG_ID
    }: {
        link: string;
        currentLanguage?: number;
    }): Observable<DotCMSContentlet[]> {
        return this.http
            .post<DotCMSResponse<{ jsonObjectView: { contentlets: DotCMSContentlet[] } }>>(
                '/api/content/_search',
                {
                    query: `+languageId:${currentLanguage} +deleted:false +working:true +(urlmap:*${link}* OR (contentType:(dotAsset OR htmlpageasset OR fileAsset) AND +path:*${link}*))`,
                    sort: 'modDate desc',
                    offset: 0,
                    limit: 40
                }
            )
            .pipe(
                map(
                    (x: DotCMSResponse<{ jsonObjectView: { contentlets: DotCMSContentlet[] } }>) =>
                        x?.entity?.jsonObjectView?.contentlets
                )
            );
    }
}
