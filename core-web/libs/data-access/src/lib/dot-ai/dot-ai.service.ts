import { Observable, of, throwError } from 'rxjs';

import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map, pluck, switchMap } from 'rxjs/operators';

import {
    DotCMSContentlet,
    AiPluginResponse,
    DotAICompletionsConfig,
    DotAIImageContent,
    DotAIImageOrientation,
    DotAIImageResponse
} from '@dotcms/dotcms-models';

export const AI_PLUGIN_KEY = {
    NOT_SET: 'NOT SET'
};
export const API_ENDPOINT = '/api/v1/ai';
export const API_ENDPOINT_FOR_PUBLISH = '/api/v1/workflow/actions/default/fire/PUBLISH';

const headers = new HttpHeaders({
    'Content-Type': 'application/json'
});

@Injectable()
export class DotAiService {
    #http: HttpClient = inject(HttpClient);

    /**
     * Generates content by sending a HTTP POST request to the AI plugin endpoint.
     *
     * @param {string} prompt - The prompt used to generate the content.
     * @returns {Observable<string>} - An observable that emits the generated content as a string.
     *
     * @throws {string} - Throws an error message if there was an error fetching AI content.
     */
    generateContent(prompt: string): Observable<string> {
        return this.#http
            .post<AiPluginResponse>(`${API_ENDPOINT}/text/generate`, JSON.stringify({ prompt }), {
                headers,
                observe: 'response'
            })
            .pipe(
                map((response) => {
                    // If the response is 200 and the body come with an error, we throw an error
                    if (response?.body?.error) {
                        throw new Error(response.body.error.message);
                    }

                    const choices = response?.body?.choices;
                    if (!choices || choices.length === 0) {
                        throw new Error(
                            'block-editor.extension.ai-image.api-error.no-choice-returned'
                        );
                    }

                    // We only use the first choice
                    return choices[0].message.content;
                }),
                catchError((error) => {
                    if (error instanceof HttpErrorResponse) {
                        return throwError(error.statusText);
                    }

                    return throwError(error);
                })
            );
    }

    /**
     * Generates and publishes an image based on the given prompt and size.
     *
     * @param {string} prompt - The prompt for generating the image.
     * @param {string} size - The size of the image to be generated (default: '1024x1024').
     * @returns {Observable<DotAIImageContent>} - An observable that emits an array of DotCMSContentlet objects.
     */
    public generateAndPublishImage(
        prompt: string,
        size: string = DotAIImageOrientation.HORIZONTAL
    ): Observable<DotAIImageContent> {
        return this.#http
            .post<DotAIImageResponse>(
                `${API_ENDPOINT}/image/generate`,
                JSON.stringify({ prompt, size }),
                {
                    headers
                }
            )
            .pipe(
                catchError(() =>
                    throwError('block-editor.extension.ai-image.api-error.missing-token')
                ),
                switchMap((response: DotAIImageResponse) => {
                    return this.createAndPublishContentlet(response);
                })
            );
    }

    /**
     * Checks if the plugin is installed and properly configured.
     *
     * @return {Observable<boolean>} An observable that emits a boolean value indicating if the plugin is installed and properly configured.
     */
    checkPluginInstallation(): Observable<boolean> {
        return this.#http
            .get<DotAICompletionsConfig>(`${API_ENDPOINT}/completions/config`, {
                observe: 'response'
            })
            .pipe(
                map((res) => res.status === 200 && res?.body?.apiKey !== AI_PLUGIN_KEY.NOT_SET),
                catchError(() => {
                    return of(false);
                })
            );
    }

    createAndPublishContentlet(aiResponse: DotAIImageResponse): Observable<DotAIImageContent> {
        const { response, tempFileName } = aiResponse;
        const contentlets: Partial<DotCMSContentlet>[] = [
            {
                baseType: 'dotAsset',
                asset: response,
                title: tempFileName,
                hostFolder: '',
                indexPolicy: 'WAIT_FOR'
            }
        ];

        return this.#http
            .post<{ entity: { results: DotCMSContentlet[] } }>(
                `${API_ENDPOINT_FOR_PUBLISH}`,
                JSON.stringify({ contentlets }),
                {
                    headers
                }
            )
            .pipe(
                pluck('entity', 'results'),
                map((contentlets: DotCMSContentlet[]) => {
                    if (contentlets.length === 0) {
                        throw new Error('contentlets is empty.');
                    }

                    const item = contentlets[0];
                    const values = Object.values(item);

                    const contentlet = { ...values[0] };

                    // under errorMessage is how the backend returns an error.
                    if (contentlet?.errorMessage) {
                        throw new Error('Could not publish the image.');
                    }

                    return {
                        contentlet,
                        ...aiResponse
                    };
                }),
                catchError(() =>
                    throwError(
                        'block-editor.extension.ai-image.api-error.error-publishing-ai-image'
                    )
                )
            );
    }
}
