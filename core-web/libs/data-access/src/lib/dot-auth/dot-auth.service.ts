import { Observable } from 'rxjs';

import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    DotAuthConfigPayload,
    DotAuthConfigView,
    DotAuthDiscoveryView,
    DotAuthHeadlessPayload,
    DotAuthSitesView,
    DotCMSAPIResponse
} from '@dotcms/dotcms-models';

/**
 * Client for the dotAuth REST surface at /api/v1/dotauth.
 */
@Injectable({
    providedIn: 'root'
})
export class DotAuthService {
    readonly #http = inject(HttpClient);
    readonly #base = '/api/v1/dotauth/sites';

    /** List all sites with their SITE_OVERRIDE / INHERITED / NOT_CONFIGURED status plus the SYSTEM_HOST flag. */
    listSites(): Observable<DotAuthSitesView> {
        return this.#http
            .get<DotCMSAPIResponse<DotAuthSitesView>>(this.#base)
            .pipe(map((response) => response.entity));
    }

    /** Fetch the config for a site. Use hostId "SYSTEM_HOST" to hit the global default. */
    getConfig(hostId: string): Observable<DotAuthConfigView> {
        return this.#http
            .get<DotCMSAPIResponse<DotAuthConfigView>>(`${this.#base}/${hostId}`)
            .pipe(map((response) => response.entity));
    }

    /** Upsert the config for a site (or SYSTEM_HOST). clientSecret = "****" preserves the stored value. */
    saveConfig(hostId: string, payload: DotAuthConfigPayload): Observable<void> {
        return this.#http
            .put<DotCMSAPIResponse<string>>(`${this.#base}/${hostId}`, payload)
            .pipe(map(() => undefined));
    }

    /** Clear the site's SSO + headless rows. On SYSTEM_HOST this removes the global default. */
    clearConfig(hostId: string): Observable<void> {
        return this.#http.delete<void>(`${this.#base}/${hostId}`);
    }

    /** Save system-level headless token-exchange config. */
    saveHeadlessConfig(payload: DotAuthHeadlessPayload): Observable<void> {
        return this.#http
            .put<DotCMSAPIResponse<string>>('/api/v1/dotauth/headless', payload)
            .pipe(map(() => undefined));
    }

    /** Clear the system-level headless config. SSO config is not affected. */
    clearHeadlessConfig(): Observable<void> {
        return this.#http.delete<void>('/api/v1/dotauth/headless');
    }

    discoverOidc(url: string): Observable<DotAuthDiscoveryView> {
        return this.#http
            .post<DotCMSAPIResponse<DotAuthDiscoveryView>>('/api/v1/dotauth/discover/oidc', { url })
            .pipe(map((response) => response.entity));
    }

    fetchSamlMetadata(url: string): Observable<string> {
        return this.#http
            .post<
                DotCMSAPIResponse<{ xml: string }>
            >('/api/v1/dotauth/fetch/saml-metadata', { url })
            .pipe(map((response) => response.entity.xml));
    }

    downloadSamlMetadata(hostId: string): void {
        window.open(`/api/v1/dotauth/saml/metadata/${hostId}`, '_blank');
    }

    revokeAllSessionRefs(): Observable<void> {
        return this.#http
            .post<DotCMSAPIResponse<string>>('/api/v1/dotauth/sessionrefs/revoke', {})
            .pipe(map(() => undefined));
    }

    exportBundle(password: string): Promise<string | null> {
        return new Promise((resolve) => {
            this.#http
                .post(
                    '/api/v1/dotauth/export',
                    { password },
                    {
                        responseType: 'blob',
                        observe: 'response'
                    }
                )
                .subscribe({
                    next: (resp: HttpResponse<Blob>) => {
                        const contentDisposition = resp.headers.get('content-disposition');
                        const filenameMatch = contentDisposition?.match(/filename="?([^"]+)"?/);
                        const fileName = filenameMatch?.[1] || 'dotauth-appSecrets.export';

                        const url = URL.createObjectURL(resp.body!);
                        const anchor = document.createElement('a');
                        anchor.href = url;
                        anchor.download = fileName;
                        anchor.click();
                        URL.revokeObjectURL(url);

                        resolve(null);
                    },
                    error: (error) => resolve(error?.message ?? 'Export failed')
                });
        });
    }

    importBundle(password: string, file: File): Observable<void> {
        const formData = new FormData();
        formData.append('json', JSON.stringify({ password }));
        formData.append('file', file);

        return this.#http
            .post<DotCMSAPIResponse<{ imported: number }>>('/api/v1/dotauth/import', formData)
            .pipe(map(() => undefined));
    }
}
