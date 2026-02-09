import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, ElementRef, inject, ViewChild } from '@angular/core';

import { LazyLoadEvent } from 'primeng/api';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InplaceModule } from 'primeng/inplace';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { PaginatorModule } from 'primeng/paginator';
import { Table, TableModule } from 'primeng/table';

import { DotCategory } from '@dotcms/dotcms-models';
import { DotActionMenuButtonComponent, DotMessagePipe } from '@dotcms/ui';

import { DotCategoriesListState, DotCategoriesListStore } from './store/dot-categories-list-store';

import { DotEmptyStateComponent } from '../../../view/components/_common/dot-empty-state/dot-empty-state.component';
import { DotPortletBaseComponent } from '../../../view/components/dot-portlet-base/dot-portlet-base.component';

@Component({
    selector: 'dot-categories-list',
    templateUrl: './dot-categories-list.component.html',
    styleUrls: ['./dot-categories-list.component.scss'],
    providers: [DotCategoriesListStore],
    imports: [
        CommonModule,
        DotPortletBaseComponent,
        MenuModule,
        ButtonModule,
        InputTextModule,
        TableModule,
        PaginatorModule,
        InplaceModule,
        InputNumberModule,
        DotActionMenuButtonComponent,
        DotMessagePipe,
        CheckboxModule,
        BreadcrumbModule,
        DotEmptyStateComponent
    ]
})
export class DotCategoriesListComponent {
    readonly #store = inject(DotCategoriesListStore);

    vm$: Observable<DotCategoriesListState> = this.#store.vm$;
    selectedCategories: DotCategory[] = [];
    breadCrumbHome = { icon: 'pi pi-home' };
    isContentFiltered = false;
    @ViewChild('dataTable')
    dataTable: Table;
    @ViewChild('gf')
    globalSearch: ElementRef;

    /**
     * Add category in breadcrumb
     * @param {DotCategory} category
     * @memberof DotCategoriesListComponent
     */
    addBreadCrumb(category: DotCategory) {
        this.dataTable.filter(category.inode, 'inode', null);
        this.dataTable.filter(null, 'global', null);
        this.#store.addCategoriesBreadCrumb({ label: category.categoryName, id: category.inode });
    }

    /**5
     * Update categories breadcrumb in store
     * @param {*} event
     * @memberof DotCategoriesListComponent
     */
    updateBreadCrumb(event) {
        const { item } = event;
        this.#store.updateCategoriesBreadCrumb(item);
        // for getting child categories need to pass category ID
        this.dataTable.filter(item.id || null, 'inode', null);
        this.dataTable.filter(null, 'global', null);
    }

    /**
     * Update selected categories in store
     * @memberof DotCategoriesListComponent
     */
    handleRowCheck(): void {
        this.#store.updateSelectedCategories(this.selectedCategories);
    }

    /**
     * Check if display results are filtered.
     * @memberof DotCategoriesListComponent
     */
    handleFilter(): void {
        this.isContentFiltered = Object.prototype.hasOwnProperty.call(
            this.dataTable.filters,
            'global'
        );
    }

    /**
     * get records according to pagination
     * @param {LazyLoadEvent} event
     * @memberof DotCategoriesListComponent
     */
    loadCategories(event: LazyLoadEvent) {
        if (event?.filters?.inode) {
            this.#store.getChildrenCategories(event);
        } else {
            this.#store.getCategories(event);
        }

        // for reset search field
        if (!event.globalFilter && this.globalSearch) {
            this.globalSearch.nativeElement.value = '';
        }
    }
}
