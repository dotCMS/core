import { Component, ViewChild } from '@angular/core';
import { DotListingDataTableComponent } from '@components/dot-listing-data-table/dot-listing-data-table.component';
import { Observable } from 'rxjs';
import { DotCategoriesListStore, DotCategoriesListState } from './store/categories-list-store';
import { DotContentState } from '@dotcms/dotcms-models';
import { DotCategory } from '@dotcms/app/shared/models/categories/dot-categories.model';

@Component({
    selector: 'dot-categories-list',
    templateUrl: './categories-list.component.html',
    styleUrls: ['./categories-list.component.scss'],
    providers: [DotCategoriesListStore]
})
export class CategoriesListComponent {
    vm$: Observable<DotCategoriesListState> = this.store.vm$;

    @ViewChild('listing', { static: false })
    listing: DotListingDataTableComponent;

    ngAfterViewInit(): void {
        this.store.updateListing(this.listing);
    }
    constructor(private store: DotCategoriesListStore) {}

    clearState() {
        this.listing.clearGlobalSearch();
    }
    getCategoryState({ live, working, deleted }: DotCategory): DotContentState {
        return { live, working, deleted, hasLiveVersion: live };
    }

    updateSelectedCategories(categories: DotCategory[]): void {
        this.store.updateSelectedCategories(categories);
    }
}
