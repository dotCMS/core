import { Observable, of, throwError } from 'rxjs';

import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, switchMap } from 'rxjs/operators';

import { DOT_AUTH_TOKEN, DOT_BASE_URL } from './dot.config';

const AI_ENDPOINT = '/api/v1/ai';
const PUBLISH_ENDPOINT = '/api/v1/workflow/actions/default/fire/PUBLISH';

interface AiTextChoice {
    message: { content: string };
}

interface AiTextResponse {
    choices?: AiTextChoice[];
    error?: { message: string };
}

interface AiImageResponse {
    response: string;
    tempFileName: string;
    revised_prompt?: string;
    originalPrompt?: string;
    url?: string;
}

export interface DotAiImageContent extends AiImageResponse {
    contentlet: Record<string, unknown>;
}

interface AiPluginConfigResponse {
    providerConfig?: unknown;
}

/**
 * Talks to the dotCMS AI plugin endpoints (text generation, image generation, plugin status).
 * Re-implementation of `libs/data-access/src/lib/dot-ai/dot-ai.service.ts` so the new editor
 * library stays self-contained (consumable outside dotcms-ui without pulling data-access).
 */
@Injectable({ providedIn: 'root' })
export class DotAiService {
    private readonly http = inject(HttpClient);

    private readonly headers = new HttpHeaders({
        Authorization: `Bearer ${DOT_AUTH_TOKEN}`,
        'Content-Type': 'application/json'
    });

    /**
     * POSTs the prompt to the AI plugin's text endpoint and emits the first generated string.
     */
    generateContent(prompt: string): Observable<string> {
        return this.http
            .post<AiTextResponse>(
                `${DOT_BASE_URL}${AI_ENDPOINT}/text/generate`,
                JSON.stringify({ prompt }),
                { headers: this.headers, observe: 'response' }
            )
            .pipe(
                map((res) => {
                    if (res?.body?.error) throw new Error(res.body.error.message);
                    const choices = res?.body?.choices;
                    if (!choices || choices.length === 0) throw new Error('No content returned');
                    return choices[0].message.content;
                }),
                catchError((err) =>
                    throwError(() => (err instanceof HttpErrorResponse ? err.statusText : err))
                )
            );
    }

    /**
     * Generates an image and publishes it as a dotCMS contentlet (base type `dotAsset`).
     * Stub for Phase 2 — kept here so the service contract matches the legacy lib.
     */
    generateAndPublishImage(prompt: string, size = '1024x1024'): Observable<DotAiImageContent> {
        return this.http
            .post<AiImageResponse>(
                `${DOT_BASE_URL}${AI_ENDPOINT}/image/generate`,
                JSON.stringify({ prompt, size }),
                { headers: this.headers }
            )
            .pipe(
                catchError(() => throwError(() => 'AI image generation failed')),
                switchMap((res) => this.publishGeneratedImage(res))
            );
    }

    /**
     * GETs the AI plugin config endpoint. True when the plugin is installed and configured.
     * Used to gate the slash-menu "Ask AI" entry.
     */
    checkPluginInstallation(): Observable<boolean> {
        return this.http
            .get<AiPluginConfigResponse>(`${DOT_BASE_URL}${AI_ENDPOINT}/completions/config`, {
                headers: this.headers,
                observe: 'response'
            })
            .pipe(
                map((res) => res.status === 200 && !!res.body?.providerConfig),
                catchError(() => of(false))
            );
    }

    private publishGeneratedImage(aiResponse: AiImageResponse): Observable<DotAiImageContent> {
        const contentlets = [
            {
                baseType: 'dotAsset',
                asset: aiResponse.response,
                title: aiResponse.tempFileName,
                hostFolder: '',
                indexPolicy: 'WAIT_FOR'
            }
        ];

        return this.http
            .post<{
                entity: { results: { [key: string]: Record<string, unknown> }[] };
            }>(`${DOT_BASE_URL}${PUBLISH_ENDPOINT}`, JSON.stringify({ contentlets }), { headers: this.headers })
            .pipe(
                map((res) => res?.entity?.results ?? []),
                map((results) => {
                    if (results.length === 0) throw new Error('Publish returned no results');
                    const first = results[0];
                    const contentlet = { ...(Object.values(first)[0] as Record<string, unknown>) };
                    if (contentlet['errorMessage']) throw new Error('Could not publish the image');
                    return { contentlet, ...aiResponse };
                }),
                catchError(() => throwError(() => 'AI image publish failed'))
            );
    }
}
