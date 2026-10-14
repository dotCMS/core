import { Injectable } from '@angular/core';

import { SESSION_STORAGE_VARIATION_KEY } from '@dotcms/dotcms-models';

/** Content type last opened in the Content Types editor, handed to Content Drive once (#37903). */
const LAST_CONTENT_TYPE_KEY = 'dotcms.content-drive.lastContentType';

//TODO: set a proper name for this
@Injectable({ providedIn: 'root' })
export class DotSessionStorageService {
    /**
     * Remembers the content type last opened in the Content Types editor, replacing any earlier
     * one, so the next Content Drive visit in this tab can start filtered to it.
     *
     * @param variable the content type's variable name
     */
    setLastContentType(variable: string): void {
        try {
            sessionStorage.setItem(LAST_CONTENT_TYPE_KEY, variable);
        } catch {
            // Storage can be unavailable (blocked, private mode); the handoff is a convenience
        }
    }

    /**
     * Forgets the remembered content type, for a type Content Drive can't filter by.
     */
    removeLastContentType(): void {
        try {
            sessionStorage.removeItem(LAST_CONTENT_TYPE_KEY);
        } catch {
            // Storage can be unavailable (blocked, private mode); nothing to forget then
        }
    }

    /**
     * Returns the remembered content type and forgets it, so it is used at most once.
     *
     * @returns the content type's variable name, or `null` when none is remembered
     */
    consumeLastContentType(): string | null {
        try {
            const variable = sessionStorage.getItem(LAST_CONTENT_TYPE_KEY);
            sessionStorage.removeItem(LAST_CONTENT_TYPE_KEY);

            return variable;
        } catch {
            return null;
        }
    }

    /**
     * Set the variantId to the SessionStorage Key
     *
     * @param {string} variationId
     * @memberof DotSessionStorageService
     */
    setVariationId(variationId: string): void {
        sessionStorage.setItem(SESSION_STORAGE_VARIATION_KEY, variationId);
    }

    /**
     * Get the variationId from the SessionStorage Key
     *
     * @return {*}  {(string | null)}
     * @memberof DotSessionStorageService
     */
    getVariationId(): string {
        if (typeof SESSION_STORAGE_VARIATION_KEY === 'string') {
            return sessionStorage.getItem(SESSION_STORAGE_VARIATION_KEY) || 'DEFAULT';
        }

        return 'DEFAULT';
    }

    /**
     * Remove the variation of the SessionStorage Key
     *
     * @memberof DotSessionStorageService
     */
    removeVariantId(): void {
        sessionStorage.removeItem(SESSION_STORAGE_VARIATION_KEY);
    }
}
