import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

const API_URL = 'https://cdn.dotcms.dev/api/v1/ai/search';

const API_TOKEN =
    'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcGk1MjhiN2E5ZS1iMDEwLTQ5NzUtOGE2Ny0wNmU2ZGFiY2IwYTQiLCJ4bW9kIjoxNzIzODIwMjkxMDAwLCJuYmYiOjE3MjM4MjAyOTEsImlzcyI6ImNvcnBzaXRlcy1oZWFkbGVzcyIsImxhYmVsIjoiZm9yIHNlYXJjaCBjaGF0Ym90IiwiZXhwIjoxODE4Mzg4ODAwLCJpYXQiOjE3MjM4MjAyOTEsImp0aSI6IjYyZmM5Zjk5LTdlYTktNGNhYy05MzBhLWU0NzQxZDU2Nzg1NiJ9.xDYRWbR0geBzyR4UM0w-rOGRmj-mISTVqp0-FIqoFbA';

interface DocSearchResult {
    documentation: string;
}

interface DocSearchResponse {
    dotCMSResults: DocSearchResult[];
}

@Injectable({
    providedIn: 'root'
})
export class HashbrownDocSearchService {
    private readonly http = inject(HttpClient);

    search(query: string): Observable<string[]> {
        const headers = new HttpHeaders({
            'Content-Type': 'application/json',
            Authorization: `Bearer ${API_TOKEN}`
        });

        const body = {
            model: 'gpt-4o',
            indexName: 'default',
            prompt: query,
            operator: 'cosine',
            threshold: '.25',
            searchLimit: 1
        };

        return this.http.post<DocSearchResponse>(API_URL, body, { headers }).pipe(
            map((response) => {
                const first = response.dotCMSResults?.[0]?.documentation;

                return first ? [first] : [];
            })
        );
    }
}
