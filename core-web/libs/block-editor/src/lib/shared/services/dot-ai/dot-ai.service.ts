import { Observable, throwError } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { catchError, map, pluck, switchMap } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

interface OpenAIResponse {
    model: string;
    prompt: string;
    response: string;
}

const API_ENDPOINT = '/api/v1/ai';
const API_ENDPOINT_FOR_PUBLISH = '/api/v1/workflow/actions/default/fire/PUBLISH';
const headers = new HttpHeaders({
    'Content-Type': 'application/json'
});

@Injectable()
export class DotAiService {

    constructor(private http: HttpClient) {}

    generateContent(prompt: string): Observable<string> {
        return this.http
            .post<OpenAIResponse>(`${API_ENDPOINT}/text/generate`, JSON.stringify({ prompt }), {
                headers
            })
            .pipe(
                catchError(() => {
                    return throwError('Error fetching AI content');
                }),
                map(({ response }) => {
                    return response;
                })
            );
    }

    generateAndPublishImage(prompt: string): Observable<DotCMSContentlet[]> {
        return this.http
            .post<OpenAIResponse>(`${API_ENDPOINT}/image/generate`, JSON.stringify({ prompt }), {
                headers
            })
            .pipe(
                catchError(() => throwError('Error fetching AI content')),
                pluck('response'),
                switchMap((tempFileName: string) => {
                    return this.createAndPublishContentlet(tempFileName);
                })
            );
    }

    createAndPublishContentlet(fileId: string): Observable<DotCMSContentlet[]> {
        const contentlets = [
            {
                baseType: 'dotAsset',
                asset: fileId,
                title: 'Test1 name.png',
                hostFolder: '',
                indexPolicy: 'WAIT_FOR'
            }
        ];

        return this.http
            .post(`${API_ENDPOINT_FOR_PUBLISH}`, JSON.stringify({ contentlets }), {
                headers
            })
            .pipe(
                pluck('entity', 'results'),
                catchError((error) => throwError(error))
            ) as Observable<DotCMSContentlet[]>;
    }
}
