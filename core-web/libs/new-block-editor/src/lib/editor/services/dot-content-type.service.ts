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

    fetchAll(): Observable<DotContentType[]> {
        const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

        return this.http
            .post<ContentTypeFilterResponse>(
                '/api/v1/contenttype/_filter',
                { filter: { query: '' }, orderBy: 'name', direction: 'ASC', perPage: 40 },
                { headers, withCredentials: true }
            )
            .pipe(map((res) => res.entity));
    }
}
