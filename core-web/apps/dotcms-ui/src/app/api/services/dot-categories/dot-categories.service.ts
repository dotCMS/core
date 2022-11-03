import { Injectable } from '@angular/core';
import { DotCategory } from '@dotcms/app/shared/models/dot-categories/dot-categories.model';
import { CoreWebService } from '@dotcms/dotcms-js';
import { LazyLoadEvent } from 'primeng/api';
import { Observable } from 'rxjs';
import { OrderDirection, PaginatorService } from '../paginator';

@Injectable()
export class DotCategoriesService extends PaginatorService {
    constructor(coreWebService: CoreWebService) {
        super(coreWebService);
        this.url = 'v1/categories';
    }

    /**
     * Get categories according to pagination and search
     * @param {LazyLoadEvent} [filters]
     * @return {*}  {Observable<DotCategory[]>}
     * @memberof DotCategoriesService
     */
    getCategories(filters?: LazyLoadEvent): Observable<DotCategory[]> {
        const { sortField, sortOrder } = filters;
        const page = parseInt(String(filters.first / this.paginationPerPage), 10) + 1;
        this.filter = filters?.filters?.global?.value || '';
        this.sortField = sortField;
        this.sortOrder = sortOrder === 1 ? OrderDirection.ASC : OrderDirection.DESC;

        return this.getPage(page);
    }
}
