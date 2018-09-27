import { Component, OnInit } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { FieldProperty } from '../field-properties.model';
import { PaginatorService } from '@services/paginator';
import { FormGroup } from '@angular/forms';
import { Category } from '../../../shared';

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
    categoriesCurrentPage: Category[];
    property: FieldProperty;
    group: FormGroup;
    placeholder: string;

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(
        public dotMessageService: DotMessageService,
        public paginationService: PaginatorService
    ) {}

    ngOnInit(): void {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.properties.category.label',
                'contenttypes.field.properties.category.error.required'
            ])
            .subscribe((res) => {
                this.i18nMessages = res;
            });

        this.placeholder = !this.property.value
            ? this.dotMessageService.get('contenttypes.field.properties.category.label')
            : this.property.value;
        this.paginationService.url = 'v1/categories';
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

    private getCategoriesList(filter = '', offset = 0): void {
        this.paginationService.filter = filter;
        this.paginationService.getWithOffset(offset).subscribe((items) => {
            // items.splice(0) is used to return a new object and trigger the change detection in angular
            this.categoriesCurrentPage = items.splice(0);
        });
    }
}
