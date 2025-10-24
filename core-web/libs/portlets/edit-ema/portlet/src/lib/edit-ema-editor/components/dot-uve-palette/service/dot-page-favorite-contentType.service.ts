import { Injectable, inject } from '@angular/core';

import { DotLocalstorageService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

import {
    DotFavoriteContentTypeParams,
    DotFavoriteContentTypeResponse
} from '../../../../shared/models';

const FAVORITE_CONTENT_TYPES_KEY_PREFIX = 'dot-favorite-content-types';

/**
 * Service to manage favorite content types using localStorage.
 * Provides functionality to save, retrieve, search, and paginate content types.
 * Favorites are stored per page using the page URL/ID.
 *
 * @export
 * @class DotPageFavoriteContentTypeService
 */
@Injectable()
export class DotPageFavoriteContentTypeService {
    private localStorageService = inject(DotLocalstorageService);

    /**
     * Generate a unique localStorage key for a specific page.
     *
     * @private
     * @param {string} pagePathOrId - The page URL or ID
     * @returns {string} The generated localStorage key
     * @memberof DotPageFavoriteContentTypeService
     */
    private getStorageKey(pagePathOrId: string): string {
        return `${FAVORITE_CONTENT_TYPES_KEY_PREFIX}:${pagePathOrId}`;
    }

    /**
     * Save an array of content types to localStorage for a specific page.
     *
     * @param {string} pagePathOrId - The page URL or ID
     * @param {DotCMSContentType[]} contentTypes - The content types to save
     * @memberof DotPageFavoriteContentTypeService
     */
    save(pagePathOrId: string, contentTypes: DotCMSContentType[]): void {
        this.localStorageService.setItem<DotCMSContentType[]>(
            this.getStorageKey(pagePathOrId),
            contentTypes
        );
    }

    /**
     * Add a content type to favorites for a specific page.
     *
     * @param {string} pagePathOrId - The page URL or ID
     * @param {DotCMSContentType} contentType - The content type to add
     * @memberof DotPageFavoriteContentTypeService
     */
    add(pagePathOrId: string, contentType: DotCMSContentType): DotCMSContentType[] {
        const favorites = this.getAll(pagePathOrId);
        const exists = favorites.some((ct) => ct.id === contentType.id);

        if (!exists) {
            favorites.push(contentType);
            this.save(pagePathOrId, favorites);
        }

        return favorites;
    }

    /**
     * Remove a content type from favorites by ID for a specific page.
     *
     * @param {string} pagePathOrId - The page URL or ID
     * @param {string} contentTypeId - The ID of the content type to remove
     * @memberof DotPageFavoriteContentTypeService
     */
    remove(pagePathOrId: string, contentTypeId: string): DotCMSContentType[] {
        const favorites = this.getAll(pagePathOrId);
        const filtered = favorites.filter((ct) => ct.id !== contentTypeId);
        this.save(pagePathOrId, filtered);

        return filtered;
    }

    /**
     * Check if a content type is in favorites for a specific page.
     *
     * @param {string} pagePathOrId - The page URL or ID
     * @param {string} contentTypeId - The ID of the content type to check
     * @returns {boolean} True if the content type is in favorites
     * @memberof DotPageFavoriteContentTypeService
     */
    isFavorite(pagePathOrId: string, contentTypeId: string): boolean {
        const favorites = this.getAll(pagePathOrId);

        return favorites.some((ct) => ct.id === contentTypeId);
    }

    /**
     * Get all favorite content types from localStorage for a specific page.
     *
     * @param {string} pagePathOrId - The page URL or ID
     * @returns {DotCMSContentType[]} Array of favorite content types
     * @memberof DotPageFavoriteContentTypeService
     */
    getAll(pagePathOrId: string): DotCMSContentType[] {
        return (
            this.localStorageService.getItem<DotCMSContentType[]>(
                this.getStorageKey(pagePathOrId)
            ) || []
        );
    }

    /**
     * Get favorite content types with search and pagination support for a specific page.
     *
     * @param {string} pagePathOrId - The page URL or ID
     * @param {DotFavoriteContentTypeParams} [params={}] - Search and pagination parameters
     * @returns {DotFavoriteContentTypeResponse} Filtered and paginated content types with pagination info
     * @memberof DotPageFavoriteContentTypeService
     */
    get(
        pagePathOrId: string,
        params: DotFavoriteContentTypeParams = {}
    ): DotFavoriteContentTypeResponse {
        let contentTypes = this.getAll(pagePathOrId);
        const totalEntries = contentTypes.length;

        // Apply filter
        if (params.filter) {
            const filterLower = params.filter.toLowerCase();
            contentTypes = contentTypes.filter((ct) => ct.name.toLowerCase().includes(filterLower));
        }

        // Apply sorting
        const orderby = params.orderby || 'name';
        const direction = params.direction || 'ASC';

        contentTypes.sort((a, b) => {
            let compareValue = 0;

            if (orderby === 'name') {
                compareValue = a.name.localeCompare(b.name);
            }

            return direction === 'ASC' ? compareValue : -compareValue;
        });

        // Calculate pagination
        const page = params.page || 1;
        const perPage = params.per_page || 20;
        const startIndex = (page - 1) * perPage;
        const endIndex = startIndex + perPage;

        // Apply pagination
        const paginatedContentTypes = contentTypes.slice(startIndex, endIndex);

        return {
            contenttypes: paginatedContentTypes,
            pagination: {
                currentPage: page,
                perPage: perPage,
                totalEntries: totalEntries
            }
        };
    }

    /**
     * Clear all favorite content types from localStorage for a specific page.
     *
     * @param {string} pagePathOrId - The page URL or ID
     * @memberof DotPageFavoriteContentTypeService
     */
    clear(pagePathOrId: string): void {
        this.localStorageService.removeItem(this.getStorageKey(pagePathOrId));
    }
}
