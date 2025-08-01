import { Component, OnInit, inject } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

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
    templateUrl: './categories-property.component.html'
})
export class CategoriesPropertyComponent implements OnInit {
    private dotMessageService = inject(DotMessageService);
    paginationService = inject(PaginatorService);

    categoriesCurrentPage: DotCMSContentTypeFieldCategories[];
    property: FieldProperty;
    group: UntypedFormGroup;
    placeholder: string;

    ngOnInit(): void {
        this.placeholder = !this.property.value
            ? this.dotMessageService.get('contenttypes.field.properties.category.label')
            : (this.property.value as string);
        this.paginationService.url = 'v1/categories';
    }

    /**
     * Call when the categories global serach changed
     * @param any filter
     * @memberof CategoriesPropertyComponent
     */
    handleFilterChange(filter): void {
        this.getCategoriesList(filter);
    }

    /**
     * Call when the current page changed
     * @param any event
     * @memberof CategoriesPropertyComponent
     */
    handlePageChange(event): void {
        this.getCategoriesList(event.filter, event.first);
    }

    private getCategoriesList(filter = '', offset = 0): void {
        this.paginationService.filter = filter;
        this.paginationService
            .getWithOffset<DotCMSContentTypeFieldCategories[]>(offset)
            .subscribe((items: DotCMSContentTypeFieldCategories[]) => {
                // items.splice(0) is used to return a new object and trigger the change detection in angular
                this.categoriesCurrentPage = items.splice(0);
            });
    }
}
