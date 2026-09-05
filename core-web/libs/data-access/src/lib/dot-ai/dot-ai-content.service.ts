import { Observable, throwError } from 'rxjs';

import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map, switchMap } from 'rxjs/operators';

import {
    DotCMSContentlet,
    AiPluginResponse,
    DotAIImageContent,
    DotAIImageResponse,
    DEFAULT_IMAGE_SIZE
} from '@dotcms/dotcms-models';

import { AI_API_ENDPOINT, API_ENDPOINT_FOR_PUBLISH } from './dot-ai.constants';

const headers = new HttpHeaders({
    'Content-Type': 'application/json'
});

/**
 * dotAI **content generation**: text, images, and publishing a generated image as a dotAsset.
 *
 * Split out of the former `DotAiService`; its configuration half now lives in
 * `DotAiConfigService`.
 */
@Injectable({ providedIn: 'root' })
export class DotAiContentService {
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
            .post<AiPluginResponse>(
                `${AI_API_ENDPOINT}/text/generate`,
                JSON.stringify({ prompt }),
                {
                    headers,
                    observe: 'response'
                }
            )
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
                        return throwError(() => error.statusText);
                    }

                    return throwError(() => error);
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
        size = DEFAULT_IMAGE_SIZE
    ): Observable<DotAIImageContent> {
        return this.generateImage(prompt, size).pipe(
            switchMap((response: DotAIImageResponse) => {
                return this.createAndPublishContentlet(response);
            })
        );
    }

    /**
     * Generates an image **without publishing it**.
     *
     * Extracted from `generateAndPublishImage`, which chains straight into
     * `createAndPublishContentlet` and therefore publishes a live dotAsset for every
     * generation — including ones the user goes on to discard. The dotAI portlet needs
     * Generate and Save to be separate, deliberate actions, so it composes this with
     * `createAndPublishContentlet` itself.
     *
     * @param {string} prompt - the prompt for generating the image.
     * @param {string} size - the size of the image to generate.
     * @returns {Observable<DotAIImageResponse>} the raw generation response.
     */
    generateImage(prompt: string, size = DEFAULT_IMAGE_SIZE): Observable<DotAIImageResponse> {
        return this.#http
            .post<DotAIImageResponse>(
                `${AI_API_ENDPOINT}/image/generate`,
                JSON.stringify({ prompt, size }),
                {
                    headers
                }
            )
            .pipe(
                catchError((error: HttpErrorResponse) => {
                    const body = error?.error;
                    const message =
                        body?.error?.message ??
                        (typeof body?.error === 'string' ? body.error : null) ??
                        body?.message;

                    return throwError(
                        () => message ?? 'block-editor.extension.ai-image.api-error.missing-token'
                    );
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
                map((x) => x?.entity?.results),
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
                        () => 'block-editor.extension.ai-image.api-error.error-publishing-ai-image'
                    )
                )
            );
    }
}
