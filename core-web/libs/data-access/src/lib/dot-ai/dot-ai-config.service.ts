import { Observable, of } from 'rxjs';

import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

import {
    DotAiProviderConfig,
    DotAiProviderMetadata,
    DotAiResolvedConfig,
    DotAiTestConnectionResult
} from '@dotcms/dotcms-models';

import { AI_API_ENDPOINT, AI_REDACTION_FAILED_SENTINEL } from './dot-ai.constants';

interface ResponseEntityView<T> {
    entity: T;
}

interface RawCompletionsConfig {
    configHost: string;
    settings?: Record<string, string>;
    /** Omitted entirely by the backend when blank — that absence is the "not configured" signal. */
    providerConfig?: string;
}

const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

/**
 * dotAI **configuration** administration: reading and writing the app config, listing
 * providers, and testing a provider connection.
 *
 * Split out of the former `DotAiService`, which mixed configuration, text generation, image
 * generation and a workflow fire in one class. Its content-side half now lives in
 * `DotAiContentService`.
 */
@Injectable({ providedIn: 'root' })
export class DotAiConfigService {
    #http: HttpClient = inject(HttpClient);

    /**
     * Checks if the plugin is installed and properly configured.
     *
     * @return {Observable<boolean>} whether a provider config is present.
     */
    checkPluginInstallation(): Observable<boolean> {
        return this.#http
            .get<DotAiProviderConfig>(`${AI_API_ENDPOINT}/completions/config`, {
                observe: 'response'
            })
            .pipe(
                map((res) => res.status === 200 && !!res?.body?.providerConfig),
                catchError(() => of(false))
            );
    }

    getConfig(siteId?: string): Observable<DotAiProviderConfig> {
        const params = siteId ? new HttpParams().set('siteId', siteId) : undefined;

        return this.#http.get<DotAiProviderConfig>(`${AI_API_ENDPOINT}/completions/config`, {
            params
        });
    }

    saveConfig(json: string, siteId?: string): Observable<DotAiProviderConfig> {
        const params = siteId ? new HttpParams().set('siteId', siteId) : undefined;

        return this.#http.put<DotAiProviderConfig>(`${AI_API_ENDPOINT}/completions/config`, json, {
            headers,
            params
        });
    }

    /**
     * Lists capability and field metadata for every registered dotAI provider, so the
     * configuration form can render dynamic provider/field UI without hardcoding provider
     * knowledge. A new backend provider appears here automatically.
     */
    getProviders(): Observable<DotAiProviderMetadata[]> {
        return this.#http
            .get<ResponseEntityView<DotAiProviderMetadata[]>>(`${AI_API_ENDPOINT}/providers`)
            .pipe(map((response) => response.entity));
    }

    /**
     * Tests a provider configuration for one capability by asking the backend to build the
     * provider client and issue a minimal real request against it. Masked credential fields
     * (`"*****"`) in `config` are resolved server-side against the value already stored for
     * `siteId` — the real secret never has to round-trip through the browser.
     */
    testConnection(
        capability: string,
        config: Record<string, unknown>,
        siteId?: string
    ): Observable<DotAiTestConnectionResult> {
        const params = siteId ? new HttpParams().set('siteId', siteId) : undefined;

        return this.#http
            .post<
                ResponseEntityView<DotAiTestConnectionResult>
            >(`${AI_API_ENDPOINT}/providers/test/${capability}`, JSON.stringify(config), { headers, params })
            .pipe(map((response) => response.entity));
    }

    /**
     * The dotAI configuration as the portlet needs it, converted **once** here.
     *
     * The raw response needs three things doing to it, and this is the only place any of them
     * may happen — `providerConfig` used to be parsed independently in the legacy portlet, the
     * app-config screen and the image-prompt component:
     *
     * 1. `providerConfig` is a JSON **string**, and is omitted when blank. Its presence is
     *    what `isConfigured` means.
     * 2. `chat.model` inside it is a comma-separated fallback list whose first entry is the
     *    default model.
     * 3. When redaction fails the server sends a literal sentinel that is not JSON.
     *
     * None of these may throw: a malformed config must still render the screen, because the
     * screen is how you diagnose a malformed config.
     */
    getResolvedConfig(siteId?: string): Observable<DotAiResolvedConfig> {
        const params = siteId ? new HttpParams().set('siteId', siteId) : undefined;

        return this.#http
            .get<RawCompletionsConfig>(`${AI_API_ENDPOINT}/completions/config`, { params })
            .pipe(map((raw) => this.#toResolvedConfig(raw)));
    }

    #toResolvedConfig(raw: RawCompletionsConfig): DotAiResolvedConfig {
        const base = {
            configHost: raw?.configHost ?? '',
            settings: raw?.settings ?? {}
        };

        const rawProviderConfig = raw?.providerConfig;

        if (!rawProviderConfig) {
            return {
                ...base,
                providerConfig: null,
                chatModels: [],
                isConfigured: false,
                redactionFailed: false
            };
        }

        if (rawProviderConfig.trim() === AI_REDACTION_FAILED_SENTINEL) {
            return {
                ...base,
                providerConfig: null,
                chatModels: [],
                isConfigured: true,
                redactionFailed: true
            };
        }

        const providerConfig = this.#safeParse(rawProviderConfig);

        return {
            ...base,
            providerConfig,
            chatModels: this.#toChatModels(providerConfig),
            isConfigured: true,
            redactionFailed: false
        };
    }

    #safeParse(value: string): Record<string, unknown> | null {
        try {
            const parsed = JSON.parse(value);

            return parsed && typeof parsed === 'object' ? parsed : null;
        } catch {
            return null;
        }
    }

    #toChatModels(providerConfig: Record<string, unknown> | null): string[] {
        const chat = providerConfig?.['chat'] as { model?: string } | undefined;

        return (chat?.model ?? '')
            .split(',')
            .map((model) => model.trim())
            .filter(Boolean);
    }
}
