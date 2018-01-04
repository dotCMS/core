import { Component, OnInit } from '@angular/core';
import { BaseComponent } from '../../../../../../view/components/_common/_base/base-component';
import { DotMessageService } from '../../../../../../api/services/dot-messages-service';
import { FieldProperty } from '../field-properties.model';
import { PaginatorService } from '../../../../../../api/services/paginator';
import { FormGroup } from '@angular/forms';
import { Category } from '../../../shared';

/**
 * List all the categories and allow select one.
 *
 * @export
 * @class CategoriesPropertyComponent
 * @extends {BaseComponent}
 * @implements {OnInit}
 */
@Component({
    providers: [
        PaginatorService,
    ],
    selector: 'categories-property',
    templateUrl: './categories-property.component.html',
})
export class CategoriesPropertyComponent extends BaseComponent implements OnInit {
    categoriesCurrentPage: Category[];
    property: FieldProperty;
    group: FormGroup;

    constructor(public messageService: DotMessageService, private paginationService: PaginatorService) {
        super([
            'contenttypes.field.properties.category.label',
            'contenttypes.field.properties.category.error.required'
        ], messageService);
    }

    ngOnInit(): void {
        this.paginationService.url = 'v1/categories';
    }

    private getCategoriesList(filter = '', offset = 0): void {
        this.paginationService.filter = filter;
        this.paginationService.getWithOffset(offset).subscribe(items => {
            // items.splice(0) is used to return a new object and trigger the change detection in angular
            this.categoriesCurrentPage = items.splice(0);
        });
    }

    /**
     * Call when the categories global serach changed
     * @param {any} filter
     * @memberof CategoriesPropertyComponent
     */
    handleFilterChange(filter): void {
        this.getCategoriesList(filter);
    }

    /**
     * Call when the current page changed
     * @param {any} event
     * @memberof CategoriesPropertyComponent
     */
    handlePageChange(event): void {
        this.getCategoriesList(event.filter, event.first);
    }
}
