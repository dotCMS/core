import { AfterViewInit, Component, ViewChild } from '@angular/core';
import { DotListingDataTableComponent } from '@components/dot-listing-data-table/dot-listing-data-table.component';
import { Observable } from 'rxjs';
import { DotCategoriesListStore, DotCategoriesListState } from './store/categories-list-store';
import { DotContentState } from '@dotcms/dotcms-models';
import { DotCategory } from '@dotcms/app/shared/models/categories/dot-categories.model';
import { take } from 'rxjs/operators';
import { MenuItem } from 'primeng/api/menuitem';
@Component({
    selector: 'dot-categories-list',
    templateUrl: './categories-list.component.html',
    styleUrls: ['./categories-list.component.scss'],
    providers: [DotCategoriesListStore]
})
export class CategoriesListComponent implements AfterViewInit {
    vm$: Observable<DotCategoriesListState> = this.store.vm$;

    @ViewChild('listing', { static: false })
    listing: DotListingDataTableComponent;

    constructor(private store: DotCategoriesListStore) {}

    ngAfterViewInit(): void {
        this.store.updateListing(this.listing);
    }

    clearState() {
        this.listing.clearGlobalSearch();
    }
    getCategoryState({ live, working, deleted }: DotCategory): DotContentState {
        return { live, working, deleted, hasLiveVersion: live };
    }

    addBreadCrumb(category: DotCategory) {
        const getSubCategoryEndPoint = 'v1/categories/children';
        this.store.updateCategoryEndPoint(getSubCategoryEndPoint);
        this.store.addCategoriesBreadCrumb({ label: category.categoryName, id: category.inode });
        this.listing.paginatorService.url = getSubCategoryEndPoint;
        this.listing.paginatorService.setExtraParams('inode', category.inode);
        this.listing.loadFirstPage();
    }

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
        if (item.label === 'Top') {
            this.store.updateCategoryEndPoint(getCategoryEndPoint);
            this.listing.paginatorService.url = getCategoryEndPoint;
            this.listing.paginatorService.deleteExtraParams('inode');
        } else {
            this.listing.paginatorService.url = getSubCategoryEndPoint;
            this.listing.paginatorService.setExtraParams('inode', item.id);
        }

        this.listing.paginatorService.deleteExtraParams('filter');
        this.listing.loadFirstPage();
    }

    updateSelectedCategories(categories: DotCategory[]): void {
        this.store.updateSelectedCategories(categories);
    }
}
