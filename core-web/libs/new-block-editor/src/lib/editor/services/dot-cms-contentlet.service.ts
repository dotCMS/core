import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DOT_CMS_AUTH_TOKEN, DOT_CMS_BASE_URL } from './dot-cms.config';

export interface DotCmsContentlet {
    inode: string;
    identifier: string;
    title: string;
    contentType: string;
    modDate: string;
    [key: string]: unknown;
}

/** POST /api/content/_search wraps results in ResponseEntityView → SearchView. */
interface ContentSearchResponse {
    entity?: {
        contentTook?: number;
        jsonObjectView?: { contentlets?: DotCmsContentlet[] };
        queryTook?: number;
        resultsSize?: number;
    };
}

@Injectable({ providedIn: 'root' })
export class DotCmsContentletService {
    private readonly http = inject(HttpClient);

    fetchByType(variable: string): Observable<DotCmsContentlet[]> {
        const headers = new HttpHeaders({
            Authorization: `Bearer ${DOT_CMS_AUTH_TOKEN}`,
            'Content-Type': 'application/json'
        });

        return this.http
            .post<ContentSearchResponse>(
                `${DOT_CMS_BASE_URL}/api/content/_search`,
                {
                    query: `+contentType:${variable} +languageId:1 +deleted:false +working:true +catchall:** title:''^15`,
                    sort: 'modDate desc',
                    offset: 0,
                    limit: 40
                },
                { headers }
            )
            .pipe(map((res) => res.entity?.jsonObjectView?.contentlets ?? []));
    }
}
