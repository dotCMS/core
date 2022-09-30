import { Component, EventEmitter, Output, ViewChild } from '@angular/core';
import { DotListingDataTableComponent } from '@components/dot-listing-data-table/dot-listing-data-table.component';
import { Observable } from 'rxjs';
import { DotCategoriesListStore, DotCategoriesListState } from './store/dot-categories-list-store';
import { DotCategory } from '@dotcms/app/shared/models/categories/dot-categories.model';
import { take } from 'rxjs/operators';
import { MenuItem } from 'primeng/api/menuitem';
@Component({
    selector: 'dot-categories-list',
    templateUrl: './dot-categories-list.component.html',
    styleUrls: ['./dot-categories-list.component.scss'],
    providers: [DotCategoriesListStore]
})
export class DotCategoriesListComponent {
    vm$: Observable<DotCategoriesListState> = this.store.vm$;
    @Output() updateCategory: EventEmitter<MenuItem> = new EventEmitter();
    @ViewChild('listing', { static: false })
    listing: DotListingDataTableComponent;

    constructor(private store: DotCategoriesListStore) {}

    /**
     * The function clears the global search of listing-data-table by calling the clearGlobalSearch() function on the listing
     * object
     */
    clearState() {
        this.listing.clearGlobalSearch();
    }

    /**
     * It set a selected category to the parent component, updates the endpoint for the listing component,
     * and then loads the first page of the listing component
     * @param {DotCategory} category - DotCategory - The category object that is being passed to the
     * function.
     */
    addBreadCrumb(category: DotCategory) {
        const getSubCategoryEndPoint = 'v1/categories/children';
        this.store.addCategoriesBreadCrumb({ label: category.categoryName, id: category.inode });
        this.updateCategory.emit({ label: category.categoryName, id: category.inode });
        this.listing.paginatorService.url = getSubCategoryEndPoint;
        this.listing.paginatorService.setExtraParams('inode', category.inode);
        this.listing.loadFirstPage();
    }

    /**
     * It updates the breadcrumb, category listing and parent component based on the selected category
     * @param event - The event that triggered the function.
     */
    async updateBreadCrumb(event) {
        const getCategoryEndPoint = 'v1/categories';
        const getSubCategoryEndPoint = 'v1/categories/children';
        const { item } = event;
        let { categoryBreadCrumb } = await this.store.categoryBreadCrumbSelector$
            .pipe(take(1))
            .toPromise();
        categoryBreadCrumb = categoryBreadCrumb.filter(
            ({ tabindex }: MenuItem) => Number(tabindex) <= Number(item.tabindex)
        );
        this.store.updateCategoriesBreadCrumb(categoryBreadCrumb);
        this.updateCategory.emit(item);
        if (item.label === 'Top') {
            this.listing.paginatorService.url = getCategoryEndPoint;
            this.listing.paginatorService.deleteExtraParams('inode');
        } else {
            this.listing.paginatorService.url = getSubCategoryEndPoint;
            this.listing.paginatorService.setExtraParams('inode', item.id);
        }

        this.listing.paginatorService.deleteExtraParams('filter');
        this.listing.loadFirstPage();
    }

    /**
     * It takes an array of categories and updates the store with the new array
     * @param {DotCategory[]} categories - DotCategory[] - An array of DotCategory objects.
     */
    updateSelectedCategories(categories: DotCategory[]): void {
        this.store.updateSelectedCategories(categories);
    }
}
