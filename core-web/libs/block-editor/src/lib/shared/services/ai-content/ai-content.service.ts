import { Observable, throwError } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

interface OpenAIResponse {
    model: string;
    prompt: string;
    response: string;
}

@Injectable()
export class AiContentService {
    constructor(private http: HttpClient) {}

    getIAContent(prompt: string): Observable<string> {
        const url = '/api/ai/text/generate';
        const body = JSON.stringify({
            prompt
        });

        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });

        return this.http.post<OpenAIResponse>(url, body, { headers }).pipe(
            catchError(() => {
                return throwError('Error fetching AI content');
            }),
            map(({ response }) => {
                return response;
            })
        );
    }
}
