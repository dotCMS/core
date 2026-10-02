import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import {
    DotCMSAPIResponse,
    DotCompanyAuthType,
    DotCompanyBrandingForm,
    DotCompanyConfiguration,
    DotCompanyLocaleForm,
    DotLoginLanguage,
    DotLoginUserSystemInformation
} from '@dotcms/dotcms-models';

const CONFIGURATION_URL = '/api/v1/configuration';

/**
 * Reads and saves the company-wide configuration edited in the Configuration portlet:
 * branding, outbound email, authentication type, locale and the company key.
 *
 * Every save returns the full configuration as the server stored it, so a caller can refresh
 * its state from the response without a second read. The saves are independent requests with
 * no transaction between them, and branding and authentication each rewrite the whole company
 * record, so callers must send them one after another, never in parallel.
 */
@Injectable({
    providedIn: 'root'
})
export class DotCompanyConfigurationService {
    readonly #http = inject(HttpClient);

    /**
     * Loads the current company configuration.
     *
     * @returns The configuration as stored on the server.
     */
    getConfiguration(): Observable<DotCompanyConfiguration> {
        return this.#http
            .get<DotCMSAPIResponse<DotCompanyConfiguration>>(`${CONFIGURATION_URL}/branding`)
            .pipe(map((response) => response.entity));
    }

    /**
     * Saves branding, portal URL and outbound email.
     *
     * The endpoint replaces every field: an optional value left out is cleared, so the form must
     * carry back the values the page does not edit, such as the background color.
     *
     * @param form - The complete branding form.
     * @returns The configuration after the save.
     */
    saveBranding(form: DotCompanyBrandingForm): Observable<DotCompanyConfiguration> {
        return this.#http
            .put<DotCMSAPIResponse<DotCompanyConfiguration>>(`${CONFIGURATION_URL}/branding`, form)
            .pipe(map((response) => response.entity));
    }

    /**
     * Saves how users identify themselves on the login form.
     *
     * @param authType - Email address or user id.
     * @returns The configuration after the save.
     */
    saveAuthType(authType: DotCompanyAuthType): Observable<DotCompanyConfiguration> {
        return this.#http
            .put<
                DotCMSAPIResponse<DotCompanyConfiguration>
            >(`${CONFIGURATION_URL}/authentication`, { authType })
            .pipe(map((response) => response.entity));
    }

    /**
     * Saves the system default language and time zone.
     *
     * @param form - Admin UI locale (e.g. `en_US`) and Java time zone id.
     * @returns The configuration after the save.
     */
    saveLocale(form: DotCompanyLocaleForm): Observable<DotCompanyConfiguration> {
        return this.#http
            .put<DotCMSAPIResponse<DotCompanyConfiguration>>(`${CONFIGURATION_URL}/locale`, form)
            .pipe(map((response) => response.entity));
    }

    /**
     * Generates a new company key. This runs immediately and cannot be undone: Apps secrets are
     * reset and push-publishing endpoint keys are re-encrypted with the new key.
     *
     * @returns The digest of the new key.
     */
    regenerateKey(): Observable<string> {
        return this.#http
            .post<DotCMSAPIResponse<string>>(`${CONFIGURATION_URL}/_regenerateKey`, {})
            .pipe(map((response) => response.entity));
    }

    /**
     * Queues a test email from the given sender to the logged-in user. The response only
     * confirms the email was queued; whether it was delivered arrives later as a system
     * notification.
     *
     * @param senderAndEmail - Sender in `Name <address>` form.
     */
    sendTestEmail(senderAndEmail: string): Observable<void> {
        return this.#http
            .post<DotCMSAPIResponse<string>>(`${CONFIGURATION_URL}/_validateCompanyEmail`, {
                senderAndEmail
            })
            .pipe(map(() => undefined));
    }

    /**
     * Lists the admin UI locales the system language can be set to (e.g. `en_US`, `es_ES`).
     * These are the portal locales, not content languages.
     *
     * @returns One entry per locale, with `language`, `country` and a display name.
     */
    getAdminLocales(): Observable<DotLoginLanguage[]> {
        return this.#http
            .post<DotCMSAPIResponse<DotLoginUserSystemInformation>>('/api/v1/loginform', {
                messagesKey: []
            })
            .pipe(map((response) => response.entity.languages));
    }
}
