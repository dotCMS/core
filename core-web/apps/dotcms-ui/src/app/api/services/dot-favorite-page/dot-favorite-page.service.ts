import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { DotESContentService, ESOrderDirection } from '@dotcms/data-access';
import { ESContent } from '@dotcms/dotcms-models';

const FAVORITE_PAGES_ES_QUERY = `+contentType:dotFavoritePage +deleted:false +working:true`;

/**
 * Provide method to get Dot Favorite Pages
 * @export
 * @class DotFavoritePageService
 */
@Injectable()
export class DotFavoritePageService {
    constructor(private dotESContentService: DotESContentService) {}

    /**
     * Return a list of DotFavoritePage.
     * @param {string} filter
     * @returns Observable<ESContent>
     * @memberof DotFavoritePageService
     */
    get(params: {
        limit: number;
        userId: string;
        identifier?: string;
        url?: string;
        offset?: string;
        sortField?: string;
        sortOrder?: ESOrderDirection | string;
    }): Observable<ESContent> {
        const { limit, userId, identifier, url, offset, sortField, sortOrder } = params;

        let extraQueryParams = '';
        if (identifier) {
            extraQueryParams = `+identifier:${identifier}`;
        } else if (url) {
            extraQueryParams = `+DotFavoritePage.url_dotraw:${url}`;
        }

        return this.dotESContentService.get({
            itemsPerPage: limit || 5,
            offset: offset || '0',
            query: `${FAVORITE_PAGES_ES_QUERY} +owner:${userId} ${extraQueryParams}`,
            sortField: sortField || 'dotFavoritePage.order',
            sortOrder: sortOrder || ESOrderDirection.ASC
        });
    }
}
