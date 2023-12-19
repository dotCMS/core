import { Observable, of, throwError } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map, pluck, switchMap } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { AiPluginResponse, DotAIImageResponse } from './dot-ai.models';

const API_ENDPOINT = '/api/v1/ai';
const API_ENDPOINT_FOR_PUBLISH = '/api/v1/workflow/actions/default/fire/PUBLISH';
const headers = new HttpHeaders({
    'Content-Type': 'application/json'
});

type ImageSize = '1024x1024' | '1024x1792' | '1792x1024';

@Injectable()
export class DotAiService {
    private http: HttpClient = inject(HttpClient);

    /**
     * Generates content by sending a HTTP POST request to the AI plugin endpoint.
     *
     * @param {string} prompt - The prompt used to generate the content.
     * @returns {Observable<string>} - An observable that emits the generated content as a string.
     *
     * @throws {string} - Throws an error message if there was an error fetching AI content.
     */
    generateContent(prompt: string): Observable<string> {
        return this.http
            .post<AiPluginResponse>(`${API_ENDPOINT}/text/generate`, JSON.stringify({ prompt }), {
                headers
            })
            .pipe(
                catchError(() => {
                    return throwError('Error fetching AI content');
                }),
                map(({ choices }) => {
                    // We only will use the first choice
                    return choices[0].message.content;
                })
            );
    }

    /**
     * Generates and publishes an image based on the given prompt and size.
     *
     * @param {string} prompt - The prompt for generating the image.
     * @param {string} size - The size of the image to be generated (default: '1024x1024').
     * @returns {Observable<DotCMSContentlet[]>} - An observable that emits an array of DotCMSContentlet objects.
     */
    public generateAndPublishImage(
        prompt: string,
        size: ImageSize = '1024x1024'
    ): Observable<DotCMSContentlet[]> {
        return this.http
            .post<DotAIImageResponse>(
                `${API_ENDPOINT}/image/generate`,
                JSON.stringify({ prompt, size }),
                {
                    headers
                }
            )
            .pipe(
                catchError(() => throwError('Error fetching AI content')),
                switchMap((response: DotAIImageResponse) => {
                    return this.createAndPublishContentlet(response);
                })
            );
    }

    /**
     * Checks if the plugin is installed by sending a test HTTP request to the API endpoint.
     * @return {Observable<boolean>} An observable that emits a boolean value indicating whether the plugin is installed (true) or not (false).
     */
    checkPluginInstallation(): Observable<boolean> {
        return this.http.get(`${API_ENDPOINT}/image/test`, { observe: 'response' }).pipe(
            map((res) => res.status === 200),
            catchError(() => {
                return of(false);
            })
        );
    }

    private createAndPublishContentlet(image: DotAIImageResponse): Observable<DotCMSContentlet[]> {
        const { response, tempFileName } = image;
        const contentlets: Partial<DotCMSContentlet>[] = [
            {
                baseType: 'dotAsset',
                asset: response,
                title: tempFileName,
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
