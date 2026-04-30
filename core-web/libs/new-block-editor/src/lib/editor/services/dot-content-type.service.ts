import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

export interface DotContentType {
    id: string;
    name: string;
    variable: string;
    description: string;
    baseType: string;
    icon: string;
}

interface ContentTypeFilterResponse {
    entity: DotContentType[];
}

@Injectable({ providedIn: 'root' })
export class DotContentTypeService {
    private readonly http = inject(HttpClient);

    /**
     * Fetches the content types the user can pick from in the slash sub-menu.
     *
     * @param allowedTypes Comma-separated content type variables (e.g. `"Blog,News"`) sourced
     * from the legacy `contentTypes` field variable. Empty string ⇒ no restriction (matches
     * the legacy behaviour of sending `""` through the API). The dotCMS backend treats this
     * field as an inclusive allowlist when non-empty.
     */
    fetchAll(allowedTypes = ''): Observable<DotContentType[]> {
        const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

        return this.http
            .post<ContentTypeFilterResponse>(
                '/api/v1/contenttype/_filter',
                {
                    filter: { types: allowedTypes, query: '' },
                    orderBy: 'name',
                    direction: 'ASC',
                    perPage: 40
                },
                { headers, withCredentials: true }
            )
            .pipe(map((res) => res.entity));
    }
}
