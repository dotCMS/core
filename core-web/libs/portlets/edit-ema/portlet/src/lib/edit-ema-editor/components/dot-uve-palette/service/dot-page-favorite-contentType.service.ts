import { Injectable, inject } from '@angular/core';

import { DotLocalstorageService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

const FAVORITE_CONTENT_TYPES_KEY = 'dot-favorite-content-types';

/**
 * Service to manage favorite content types using localStorage.
 *
 * @export
 * @class DotPageFavoriteContentTypeService
 */
@Injectable({
    providedIn: 'root'
})
export class DotPageFavoriteContentTypeService {
    private localStorageService = inject(DotLocalstorageService);

    /**
     * Add one or more content types to favorites.
     * Prevents duplicates by checking existing favorites.
     *
     * @param {DotCMSContentType | DotCMSContentType[]} contentTypes - The content type(s) to add
     * @returns {DotCMSContentType[]} Updated array of favorite content types
     * @memberof DotPageFavoriteContentTypeService
     */
    add(contentTypes: DotCMSContentType | DotCMSContentType[]): DotCMSContentType[] {
        const favorites = this.getAll();
        const typesToAdd = Array.isArray(contentTypes) ? contentTypes : [contentTypes];

        typesToAdd.forEach((contentType) => {
            const exists = favorites.some((ct) => ct.id === contentType.id);
            if (!exists) {
                favorites.push(contentType);
            }
        });

        this.localStorageService.setItem<DotCMSContentType[]>(
            FAVORITE_CONTENT_TYPES_KEY,
            favorites
        );

        return favorites;
    }

    /**
     * Remove a content type from favorites by ID.
     *
     * @param {string} contentTypeId - The ID of the content type to remove
     * @returns {DotCMSContentType[]} Updated array of favorite content types
     * @memberof DotPageFavoriteContentTypeService
     */
    remove(contentTypeId: string): DotCMSContentType[] {
        const favorites = this.getAll();
        const filtered = favorites.filter((ct) => ct.id !== contentTypeId);

        this.localStorageService.setItem<DotCMSContentType[]>(FAVORITE_CONTENT_TYPES_KEY, filtered);

        return filtered;
    }

    /**
     * Set/replace all favorite content types with the provided array.
     *
     * @param {DotCMSContentType[]} contentTypes - The content types to set as favorites
     * @returns {DotCMSContentType[]} The saved array of favorite content types
     * @memberof DotPageFavoriteContentTypeService
     */
    set(contentTypes: DotCMSContentType[]): DotCMSContentType[] {
        this.localStorageService.setItem<DotCMSContentType[]>(
            FAVORITE_CONTENT_TYPES_KEY,
            contentTypes
        );

        return contentTypes;
    }

    /**
     * Get all favorite content types from localStorage.
     *
     * @returns {DotCMSContentType[]} Array of favorite content types
     * @memberof DotPageFavoriteContentTypeService
     */
    getAll(): DotCMSContentType[] {
        return (
            this.localStorageService.getItem<DotCMSContentType[]>(FAVORITE_CONTENT_TYPES_KEY) || []
        );
    }

    /**
     * Check if a content type is in favorites.
     *
     * @param {string} contentTypeId - The ID of the content type to check
     * @returns {boolean} True if the content type is in favorites
     * @memberof DotPageFavoriteContentTypeService
     */
    isFavorite(contentTypeId: string): boolean {
        const favorites = this.getAll();

        return favorites.some((ct) => ct.id === contentTypeId);
    }

    /**
     * Clear all favorite content types from localStorage.
     *
     * @memberof DotPageFavoriteContentTypeService
     */
    clear(): void {
        this.localStorageService.removeItem(FAVORITE_CONTENT_TYPES_KEY);
    }
}
