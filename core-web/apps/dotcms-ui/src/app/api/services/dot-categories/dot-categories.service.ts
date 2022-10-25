import { Injectable } from '@angular/core';
import { DotCategory } from '@dotcms/app/shared/models/dot-categories/dot-categories.model';
import { CoreWebService } from '@dotcms/dotcms-js';
import { Observable } from 'rxjs';
import { PaginatorService } from '../paginator';

@Injectable()
export class DotCategoriesService extends PaginatorService {
    constructor(coreWebService: CoreWebService) {
        super(coreWebService);
        this.url = 'v1/categories';
    }

    /**
     * @param {Record<string, unknown>} [filters]
     * @return {*}  {Observable<DotCategory[]>}
     * @memberof DotCategoriesService
     */
    getCategories(filters?: Record<string, unknown>): Observable<DotCategory[]> {
        if (filters?.filter) this.filter = (filters.filter as string) || '';
        if (filters?.currentPage) this.setExtraParams('page', filters.currentPage);

        return this.get();
    }
}
