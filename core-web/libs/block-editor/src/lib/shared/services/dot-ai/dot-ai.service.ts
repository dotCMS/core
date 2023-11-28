import { Observable, throwError } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { catchError, map, pluck } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

interface OpenAIResponse {
    model: string;
    prompt: string;
    response: string;
}
@Injectable()
export class DotAiService {
    constructor(private http: HttpClient) {}

    generateContent(prompt: string): Observable<string> {
        const url = '/api/v1/ai/text/generate';
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

    generateImage(prompt: string) {
        const url = '/api/v1/ai/image/generate';
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

    createAndPublishContentlet(fileId: string): Observable<DotCMSContentlet[]> {
        const contentlets = [
            {
                contentType: 'dotAsset',
                asset: fileId,
                hostFolder: '',
                indexPolicy: 'WAIT_FOR'
            }
        ];

        return this.http
            .post(
                '/api/v1/workflow/actions/default/fire/PUBLISH',
                JSON.stringify({ contentlets }),
                {
                    headers: {
                        Origin: window.location.hostname,
                        'Content-Type': 'application/json;charset=UTF-8'
                    }
                }
            )
            .pipe(
                pluck('entity', 'results'),
                catchError((error) => throwError(error))
            ) as Observable<DotCMSContentlet[]>;
    }
}
