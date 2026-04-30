import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

export interface DotContentlet {
    inode: string;
    identifier: string;
    title: string;
    contentType: string;
    modDate: string;
    languageId: number;
    [key: string]: unknown;
}

/** POST /api/content/_search wraps results in ResponseEntityView → SearchView. */
interface ContentSearchResponse {
    entity?: {
        contentTook?: number;
        jsonObjectView?: { contentlets?: DotContentlet[] };
        queryTook?: number;
        resultsSize?: number;
    };
}

@Injectable({ providedIn: 'root' })
export class DotContentletService {
    private readonly http = inject(HttpClient);

    /**
     * Lists the 40 most recently modified contentlets of a given type for the given language.
     * Used by the slash-menu's content-type sub-menu drill-down — the user picks a content
     * type and we list the contentlets they can embed via `dotContent`.
     */
    fetchByType(variable: string, languageId = 1): Observable<DotContentlet[]> {
        const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

        return this.http
            .post<ContentSearchResponse>(
                '/api/content/_search',
                {
                    query: `+contentType:${variable} +languageId:${languageId} +deleted:false +working:true +catchall:** title:''^15`,
                    sort: 'modDate desc',
                    offset: 0,
                    limit: 40
                },
                { headers, withCredentials: true }
            )
            .pipe(map((res) => res.entity?.jsonObjectView?.contentlets ?? []));
    }
}
