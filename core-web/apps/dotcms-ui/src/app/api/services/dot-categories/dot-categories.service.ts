import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { LazyLoadEvent } from 'primeng/api';

import { OrderDirection, PaginatorService } from '@dotcms/data-access';
import { DotCategory } from '@dotcms/dotcms-models';

export const CATEGORY_API_URL = 'v1/categories';

export const CATEGORY_CHILDREN_API_URL = 'v1/categories/children';

@Injectable()
export class DotCategoriesService extends PaginatorService {
    constructor() {
        super();
        this.url = CATEGORY_API_URL;
    }

    updatePaginationService(event: LazyLoadEvent) {
        const { sortField, sortOrder, filters } = event;
        this.setExtraParams('inode', filters?.inode?.value || null);
        this.filter = event?.filters?.global?.value || '';
        this.sortField = sortField;
        this.sortOrder = sortOrder === 1 ? OrderDirection.ASC : OrderDirection.DESC;
    }

    /**
     * Get categories according to pagination and search
     * @param {LazyLoadEvent} [event]
     * @return {*}  {Observable<DotCategory[]>}
     * @memberof DotCategoriesService
     */
    getCategories(event: LazyLoadEvent): Observable<DotCategory[]> {
        this.url = CATEGORY_API_URL;
        this.updatePaginationService(event);
        const page = parseInt(String(event.first / this.paginationPerPage), 10) + 1;

        return this.getPage(page);
    }

    /**
     * Get children categories according to pagination and search
     * @param {LazyLoadEvent} [event]
     * @return {*}  {Observable<DotCategory[]>}
     * @memberof DotCategoriesService
     */
    getChildrenCategories(event: LazyLoadEvent): Observable<DotCategory[]> {
        this.url = CATEGORY_CHILDREN_API_URL;
        this.updatePaginationService(event);
        const page = parseInt(String(event.first / this.paginationPerPage), 10) + 1;

        return this.getPage(page);
    }
}
