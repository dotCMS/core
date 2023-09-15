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
export class AiContentService {
    private lastUsedPrompt: string | null = null;
    private lastContentResponse: string | null = null;

    private lastImagePrompt: string | null = null;
    private lastImageResponse: string | null = null;

    constructor(private http: HttpClient) {}

    getLastUsedPrompt(): string | null {
        return this.lastUsedPrompt;
    }

    getLastContentResponse(): string | null {
        return this.lastContentResponse;
    }

    getIAContent(prompt: string): Observable<string> {
        this.lastUsedPrompt = prompt;

        const url = 'http://localhost:8081/api/ai/text/generate';
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
                this.lastContentResponse = response;

                return response;
            })
        );
    }

    getAIImage(prompt: string) {
        const url = 'http://localhost:8081/api/ai/image/generate';
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
                this.lastImageResponse = response;

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
                'http://localhost:8081/api/v1/workflow/actions/default/fire/PUBLISH',
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
