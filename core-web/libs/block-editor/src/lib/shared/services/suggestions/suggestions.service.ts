import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';

import { ContentletFilters, DEFAULT_LANG_ID } from '../../../shared';

// Hex-only segments separated by hyphens. Narrow enough to skip ordinary
// hyphenated English titles ("self-care", "White-Water Falls"), which must go
// through the regular tokenized search path instead of the identifier branch.
const UUID_LIKE = /^[0-9a-f]+(-[0-9a-f]+)+$/i;

const LUCENE_SPECIAL_CHARS = /(\+|-|&&|\|\||!|\(|\)|\{|\}|\[|\]|\^|"|~|\*|\?|:|\\|\/)/g;

const escapeLucene = (value: string): string => value.replace(LUCENE_SPECIAL_CHARS, '\\$1');

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
            .post<{ entity: DotCMSContentType[] }>(`/api/v1/contenttype/_filter`, {
                filter: {
                    types: allowedTypes,
                    query: filter
                },
                orderBy: 'name',
                direction: 'ASC',
                perPage: 40
            })
            .pipe(map((x) => x?.entity));
    }

    getContentlets({
        contentType,
        filter,
        currentLanguage,
        contentletIdentifier
    }: ContentletFilters): Observable<DotCMSContentlet[]> {
        const identifierQuery = contentletIdentifier ? `-identifier:${contentletIdentifier}` : '';
        const trimmedFilter = filter.trim();

        let searchClauses = '';
        if (UUID_LIKE.test(trimmedFilter)) {
            // Identifier/UUID branch: single mandatory clause, no wildcards or title boost.
            searchClauses = `+catchall:${escapeLucene(trimmedFilter)}`;
        } else if (trimmedFilter.length > 0) {
            // Tokenize on whitespace so multi-word queries require ALL tokens to match.
            const tokenClauses = trimmedFilter
                .split(/\s+/)
                .map((token) => `+catchall:*${escapeLucene(token)}*`)
                .join(' ');
            searchClauses = `${tokenClauses} title:"${escapeLucene(trimmedFilter)}"^15`;
        }

        const query = [
            `+contentType:${contentType}`,
            identifierQuery,
            `+languageId:${currentLanguage}`,
            `+deleted:false`,
            `+working:true`,
            searchClauses
        ]
            .filter((part) => part.length > 0)
            .join(' ');

        return this.http
            .post<{
                entity: { jsonObjectView: { contentlets: DotCMSContentlet[] } };
            }>('/api/content/_search', {
                query,
                sort: 'modDate desc',
                offset: 0,
                limit: 40
            })
            .pipe(map((x) => x?.entity?.jsonObjectView?.contentlets));
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
            .post<{
                entity: { jsonObjectView: { contentlets: DotCMSContentlet[] } };
            }>('/api/content/_search', {
                query: `+languageId:${currentLanguage} +deleted:false +working:true +(urlmap:*${link}* OR (contentType:(dotAsset OR htmlpageasset OR fileAsset) AND +path:*${link}*))`,
                sort: 'modDate desc',
                offset: 0,
                limit: 40
            })
            .pipe(map((x) => x?.entity?.jsonObjectView?.contentlets));
    }
}
