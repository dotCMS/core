import { Component, OnInit, inject } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

import { LazyLoadEvent } from 'primeng/api';

import { delay, take } from 'rxjs/operators';

import { DotMessageService, PaginatorService } from '@dotcms/data-access';
import { DotCMSContentTypeFieldCategories } from '@dotcms/dotcms-models';

import { FieldProperty } from '../field-properties.model';

/**
 * List all the categories and allow select one.
 *
 * @export
 * @class CategoriesPropertyComponent

 * @implements {OnInit}
 */
@Component({
    providers: [PaginatorService],
    selector: 'dot-categories-property',
    templateUrl: './categories-property.component.html',
    standalone: false
})
export class CategoriesPropertyComponent implements OnInit {
    private dotMessageService = inject(DotMessageService);
    paginationService = inject(PaginatorService);

    categoriesCurrentPage: DotCMSContentTypeFieldCategories[] = [];
    loading = false;
    filterValue = '';
    property: FieldProperty;
    group: UntypedFormGroup;
    placeholder: string;

    ngOnInit(): void {
        this.placeholder = !this.property.value
            ? this.dotMessageService.get('contenttypes.field.properties.category.label')
            : (this.property.value as string);
        this.paginationService.url = 'v1/categories';
        this.getCategoriesList();
    }

    /**
     * Call when the categories global serach changed
     * @param any filter
     * @memberof CategoriesPropertyComponent
     */
    handleFilterChange(filter: string): void {
        this.filterValue = filter || '';
        this.getCategoriesList(this.filterValue, 0);
    }

    /**
     * Call when the current page changed
     * @param any event
     * @memberof CategoriesPropertyComponent
     */
    handlePageChange(event): void {
        this.getCategoriesList(event.filter, event.first);
    }

    handleLazyLoad(event: LazyLoadEvent): void {
        const offset = event.first || 0;
        this.getCategoriesList(this.filterValue, offset);
    }

    private getCategoriesList(filter = '', offset = 0): void {
        this.paginationService.filter = filter;
        this.loading = true;
        this.paginationService
            .getWithOffset<DotCMSContentTypeFieldCategories[]>(offset)
            .pipe(take(1), delay(0))
            .subscribe(
                (items: DotCMSContentTypeFieldCategories[]) => {
                    this.categoriesCurrentPage = items.slice(0);
                    this.loading = false;
                },
                () => {
                    this.loading = false;
                }
            );
    }
}
