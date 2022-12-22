import { Component, OnDestroy, ViewChild } from '@angular/core';
import { DotListingDataTableComponent } from '@components/dot-listing-data-table/dot-listing-data-table.component';
import {
    DotActionBulkResult,
    DotBulkFailItem,
    DotContentState,
    DotContainer
} from '@dotcms/dotcms-models';
import { DotContainerListStore } from '@portlets/dot-containers/container-list/store/dot-container-list.store';
import { DotActionMenuItem } from '@models/dot-action-menu/dot-action-menu-item.model';

import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import { DotBulkInformationComponent } from '@components/_common/dot-bulk-information/dot-bulk-information.component';
import { DotMessageService } from '@dotcms/data-access';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DialogService } from 'primeng/dynamicdialog';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { DotRouterService } from '@services/dot-router/dot-router.service';

@Component({
    selector: 'dot-container-list',
    templateUrl: './container-list.component.html',
    styleUrls: ['./container-list.component.scss'],
    providers: [DotContainerListStore]
})
export class ContainerListComponent implements OnDestroy {
    vm$ = this.store.vm$;
    notify$ = this.store.notify$;

    @ViewChild('listing', { static: false })
    listing: DotListingDataTableComponent;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private store: DotContainerListStore,
        private dotMessageService: DotMessageService,
        private dotMessageDisplayService: DotMessageDisplayService,
        private dialogService: DialogService,
        private dotRouterService: DotRouterService
    ) {
        this.notify$.pipe(takeUntil(this.destroy$)).subscribe(({ payload, message, failsInfo }) => {
            this.notifyResult(payload, failsInfo, message);
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Change base type to the selected one
     * @param {string} value
     * @memberof ContainerListComponent
     */
    changeBaseTypeSelector(value: string) {
        value !== ''
            ? this.listing.paginatorService.setExtraParams('type', value)
            : this.listing.paginatorService.deleteExtraParams('type');
        this.listing.loadFirstPage();
    }

    /**
     * Get the attributes that define the state of a container.
     * @param {DotContainer} { live, working, deleted, hasLiveVersion}
     * @returns DotContentState
     * @memberof ContainerListComponent
     */
    getContainerState({ live, working, deleted }: DotContainer): DotContentState {
        return { live, working, deleted, hasLiveVersion: live };
    }

    /**
     * Get the clicked container row and redirect to edit container page.
     * @param {DotContainer} container
     * @memberof ContainerListComponent
     */
    handleRowClick(container: DotContainer) {
        this.store.editContainer(container);
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
        const filterContainers = containers.filter(
            (container: DotContainer) => container.identifier !== 'SYSTEM_CONTAINER'
        );
        this.store.updateSelectedContainers(filterContainers);
    }

    /**
     * Reset bundle state to null
     *
     * @memberof ContainerListComponent
     */
    resetBundleIdentifier(): void {
        this.store.updateBundleIdentifier(null);
    }

    setContainerActions(container: DotContainer): DotActionMenuItem[] {
        return this.store.getContainerActions(container);
    }

    /**
     * Return a list of containers with disableInteraction in system items.
     * @param {DotContainer[]} containers
     * @returns DotContainer[]
     * @memberof DotContainerListComponent
     */
    getContainersWithDisabledEntities(containers: DotContainer[]): DotContainer[] {
        return containers.map((container) => {
            container.disableInteraction =
                container.identifier.includes('/') || container.identifier === 'SYSTEM_CONTAINER';

            return container;
        });
    }

    private notifyResult(
        response: DotActionBulkResult | DotContainer,
        failsInfo: DotBulkFailItem[],
        message: string
    ): void {
        if ('fails' in response && failsInfo?.length) {
            this.showErrorDialog({
                ...response,
                fails: failsInfo,
                action: message
            });
        } else if (message) {
            this.showToastNotification(message);
        }

        this.listing?.clearSelection();
        this.listing?.loadCurrentPage();
    }

    private showToastNotification(message: string): void {
        this.dotMessageDisplayService.push({
            life: 3000,
            message: message,
            severity: DotMessageSeverity.SUCCESS,
            type: DotMessageType.SIMPLE_MESSAGE
        });
    }

    private showErrorDialog(result: DotActionBulkResult): void {
        this.dialogService.open(DotBulkInformationComponent, {
            header: this.dotMessageService.get('Results'),
            width: '40rem',
            contentStyle: { 'max-height': '500px', overflow: 'auto' },
            baseZIndex: 10000,
            data: result
        });
    }
}
