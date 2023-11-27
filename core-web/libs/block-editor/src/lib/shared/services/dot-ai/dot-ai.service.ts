import { Observable, of, throwError } from 'rxjs';

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

@Injectable()
export class DotAiService {
    // TODO: Work on progress
    private lastUsedPrompt: string | null = null;
    private lastImagePrompt: string | null = null;
    private lastContentResponse: string | null = null;

    constructor(private http: HttpClient) {}

    // TODO: Work on progress
    getLastUsedPrompt(): string | null {
        return this.lastUsedPrompt;
    }

    // TODO: Work on progress
    getLastContentResponse(): string | null {
        return this.lastContentResponse;
    }

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

    generateAndPublishImage(prompt: string): Observable<DotCMSContentlet[]> {
        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });

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

    getLatestContent() {
        return this.lastContentResponse;
    }

    // TODO: Work on progress
    getNewContent(contentType: string): Observable<string> {
        if (contentType === 'aiContent') {
            return this.generateContent(this.lastUsedPrompt);
        }

        if (contentType === 'dotImage') {
            // return this.generateImage(this.lastImagePrompt);
        }

        return of('');
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

        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });

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
