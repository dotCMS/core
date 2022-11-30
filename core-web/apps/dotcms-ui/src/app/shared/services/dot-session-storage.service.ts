import { Injectable } from '@angular/core';
import { SESSION_STORAGE_VARIATION_KEY } from '@portlets/dot-experiments/shared/models/dot-experiments-constants';

@Injectable()
export class DotSessionStorageService {
    /**
     * Set the variantId to the SessionStorage Key
     * @param {string} variationId
     */
    setVariationId(variationId: string): void {
        sessionStorage.setItem(SESSION_STORAGE_VARIATION_KEY, variationId);
    }

    /**
     * Get the variationId from the SessionStorage Key
     */
    getVariationId(): string {
        return sessionStorage.getItem(SESSION_STORAGE_VARIATION_KEY);
    }

    /**
     * Remove the variation of the SessionStorage Key
     */
    removeVariationId(): void {
        sessionStorage.removeItem(SESSION_STORAGE_VARIATION_KEY);
    }
}
