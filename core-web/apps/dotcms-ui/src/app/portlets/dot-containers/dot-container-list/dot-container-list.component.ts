import { Component, ViewChild } from '@angular/core';
import { DotListingDataTableComponent } from '@components/dot-listing-data-table/dot-listing-data-table.component';
import { DotContainer } from '@models/container/dot-container.model';
import { DotContentState } from '@dotcms/dotcms-models';
import { DotContainerListStore } from '@portlets/dot-containers/dot-container-list/store/dot-container-list.store';

@Component({
    selector: 'dot-container-list',
    templateUrl: './dot-container-list.component.html',
    styleUrls: ['./dot-container-list.component.scss'],
    providers: [DotContainerListStore]
})
export class DotContainerListComponent {
    vm$ = this.store.vm$;

    @ViewChild('listing', { static: false })
    listing: DotListingDataTableComponent;

    constructor(private store: DotContainerListStore) {}

    /**
     * get the attributes that define the state of a container.
     * @param {DotContainer} { live, working, deleted, hasLiveVersion}
     * @returns DotContentState
     * @memberof ContainerListComponent
     */
    getContainerState({ live, working, deleted }: DotContainer): DotContentState {
        return { live, working, deleted, hasLiveVersion: live };
    }

    /**
     * Handle filter for hide / show archive containers
     * @param {boolean} checked
     *
     * @memberof ContainerListComponent
     */
    handleArchivedFilter(checked: boolean): void {
        checked
            ? this.listing.paginatorService.setExtraParams('archive', checked)
            : this.listing.paginatorService.deleteExtraParams('archive');
        this.listing.loadFirstPage();
    }

    /**
     * Keep updated the selected containers in the grid
     * @param {DotContainer[]} containers
     *
     * @memberof ContainerListComponent
     */
    updateSelectedContainers(containers: DotContainer[]): void {
        this.store.updateSelectedContainers(containers);
    }

    /**
     * Reset bundle state to null
     *
     * @memberof ContainerListComponent
     */
    resetBundleIdentifier(): void {
        this.store.updateBundleIdentifier(null);
    }
}
