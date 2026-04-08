import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DOT_CMS_AUTH_TOKEN, DOT_CMS_BASE_URL } from './dot-cms.config';

export interface DotCmsContentType {
    id: string;
    name: string;
    variable: string;
    description: string;
    baseType: string;
    icon: string;
}

interface ContentTypeFilterResponse {
    entity: DotCmsContentType[];
}

@Injectable({ providedIn: 'root' })
export class DotCmsContentTypeService {
    private readonly http = inject(HttpClient);

    fetchAll(): Observable<DotCmsContentType[]> {
        const headers = new HttpHeaders({
            Authorization: `Bearer ${DOT_CMS_AUTH_TOKEN}`,
            'Content-Type': 'application/json'
        });

        return this.http
            .post<ContentTypeFilterResponse>(
                `${DOT_CMS_BASE_URL}/api/v1/contenttype/_filter`,
                { filter: { query: '' }, orderBy: 'name', direction: 'ASC', perPage: 40 },
                { headers }
            )
            .pipe(map((res) => res.entity));
    }
}
